import os
from pathlib import Path
from typing import List, Optional, Dict, Any

from fastapi import FastAPI
from pydantic import BaseModel
from dotenv import load_dotenv

from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.messages import SystemMessage, HumanMessage


# --------------------------------------------------
# Env
# --------------------------------------------------
load_dotenv(dotenv_path=Path(__file__).parent / ".env")

GEMINI_API_KEY = os.getenv("GOOGLE_API_KEY")
if not GEMINI_API_KEY:
    raise RuntimeError("GOOGLE_API_KEY missing in .env")

llm = ChatGoogleGenerativeAI(
    model="gemini-3-flash-preview",
    temperature=0.3,
    google_api_key=GEMINI_API_KEY,
)

app = FastAPI(title="LLM Assist Service", version="4.3.3")


# --------------------------------------------------
# Models
# --------------------------------------------------
class InterpretRequest(BaseModel):
    requestId: str
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: str
    services: List[str]
    actionGroups: List[str]
    resourceArns: List[str]
    durationHours: Optional[int] = None


class ResolveFollowupRequest(BaseModel):
    requestId: str
    followupAnswer: str
    partialData: Dict[str, Any]


class FollowupQuestion(BaseModel):
    question: str


class InterpretResponse(BaseModel):
    needFollowup: bool
    followupQuestion: FollowupQuestion
    partialData: Dict[str, Any]


# --------------------------------------------------
# Health
# --------------------------------------------------
@app.get("/health")
def health():
    return {"status": "UP"}


# --------------------------------------------------
# Gemini helpers
# --------------------------------------------------
def extract_text(resp) -> str:
    content = resp.content

    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        texts = []
        for part in content:
            if hasattr(part, "text") and isinstance(part.text, str):
                texts.append(part.text)
            elif isinstance(part, dict) and "text" in part:
                texts.append(part["text"])
        return " ".join(texts).strip()

    return str(content).strip()


def ask_llm_followup(context: str) -> str:
    system_prompt = """
You are a clarification assistant for an AWS access request system.

Helpful guidance:
- Some actions may logically require additional actions or services.
- If a commonly required action or service is missing, ask about it.
- Do NOT assume; always ask.

Your task:
- Ask exactly ONE follow-up question to clarify missing, unclear, or commonly required information.

Rules:
- ONE question only
- Output ONLY the question text
- No explanations
- No JSON
- No greetings or closings
""".strip()

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=context),
    ]

    resp = llm.invoke(messages)
    text = extract_text(resp)

    text = text.strip().split("\n")[0]
    if "?" in text:
        text = text[: text.index("?") + 1]

    return text


# --------------------------------------------------
# Backend authority logic
# --------------------------------------------------
def normalize_s3_arns(arn: str) -> List[str]:
    """
    Given bucket ARN or object ARN, return BOTH.
    """
    if arn.endswith("/*"):
        bucket_arn = arn[:-2]
        return [bucket_arn, arn]

    if arn.startswith("arn:aws:s3:::"):
        return [arn, f"{arn}/*"]

    return []


def apply_followup_answer(partial: Dict[str, Any], answer: str):
    answer = answer.strip()
    answer_lower = answer.lower()

    # duration
    if "hour" in answer_lower:
        nums = [int(s) for s in answer_lower.split() if s.isdigit()]
        if nums:
            partial["durationHours"] = nums[0]
        return

    # s3 arn
    if answer.startswith("arn:aws:s3:::"):
        partial["resourceArns"] = normalize_s3_arns(answer)
        return


def is_complete(partial: Dict[str, Any]) -> bool:
    return (
        bool(partial.get("resourceArns")) and
        bool(partial.get("durationHours"))
    )


# --------------------------------------------------
# /interpret
# --------------------------------------------------
@app.post("/api/v1/llm/interpret", response_model=InterpretResponse)
def interpret(req: InterpretRequest):

    context = f"""
Reason:
{req.reason}

Services:
{req.services}

Actions:
{req.actionGroups}

Resources:
{req.resourceArns}

Duration:
{req.durationHours}
""".strip()

    question = ask_llm_followup(context)

    return InterpretResponse(
        needFollowup=True,
        followupQuestion=FollowupQuestion(question=question),
        partialData=req.dict(),
    )


# --------------------------------------------------
# /resolve-followup
# --------------------------------------------------
@app.post("/api/v1/llm/resolve-followup", response_model=InterpretResponse)
def resolve_followup(req: ResolveFollowupRequest):

    partial = req.partialData.copy()

    # 🔥 APPLY ANSWER INTO STATE FIRST
    apply_followup_answer(partial, req.followupAnswer)

    # keep for audit/debug
    partial["lastAnswer"] = req.followupAnswer

    # ✅ STOP IF COMPLETE (NO LLM CALL)
    if is_complete(partial):
        return InterpretResponse(
            needFollowup=False,
            followupQuestion=FollowupQuestion(question=""),
            partialData=partial,
        )

    # ❓ Ask LLM only if backend cannot infer
    context = f"""
Current request:
{partial}
""".strip()

    question = ask_llm_followup(context)

    return InterpretResponse(
        needFollowup=True,
        followupQuestion=FollowupQuestion(question=question),
        partialData=partial,
    )
