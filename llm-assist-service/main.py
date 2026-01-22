import json
import os
from typing import Dict, List, Optional, Any, Tuple

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
from openai import OpenAI


# -----------------------------
# Env + OpenAI Client
# -----------------------------
load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError("OPENAI_API_KEY missing. Add it in llm-assist-service/.env")

client = OpenAI(api_key=OPENAI_API_KEY)

app = FastAPI(title="LLM Assist Service", version="2.4.0")


# -----------------------------
# Request Models
# -----------------------------
class InterpretRequest(BaseModel):
    requestId: str
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: str
    durationHours: Optional[int] = None
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


class ResolveFollowupRequest(BaseModel):
    requestId: str
    followupAnswer: str
    partialData: Dict[str, Any]
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


class SuggestRequest(BaseModel):
    reason: str
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


# -----------------------------
# Response Models
# -----------------------------
class FollowupQuestion(BaseModel):
    field: str
    question: str


class SuggestResponse(BaseModel):
    services: List[str]
    actionGroups: List[str]
    suggestion: str


class InterpretResponse(BaseModel):
    # Always present
    needFollowup: bool

    # Present only in follow-up mode
    followupQuestion: Optional[FollowupQuestion] = None
    partialData: Optional[Dict[str, Any]] = None

    # Present only in complete mode
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: Optional[str] = None
    services: Optional[List[str]] = None
    resourceArns: Optional[List[str]] = None
    durationHours: Optional[int] = None
    actionGroups: Optional[List[str]] = None


# -----------------------------
# Health Check
# -----------------------------
@app.get("/health")
def health():
    return {"status": "UP"}


# -----------------------------
# Helpers
# -----------------------------
MANDATORY_FIELDS = [
    "requesterEmail",
    "awsAccount",
    "reason",
    "services",
    "resourceArns",
    "durationHours",
]


def safe_json_parse(text: str) -> dict:
    return json.loads(text.strip())


def call_openai_json(system_prompt: str, user_prompt: str) -> dict:
    """
    Calls OpenAI and forces JSON output.
    Retries once if invalid JSON.
    """
    model = "gpt-4o-mini"

    def _call(extra_instruction: Optional[str] = None) -> str:
        messages = [{"role": "system", "content": system_prompt}]
        if extra_instruction:
            messages.append({"role": "system", "content": extra_instruction})
        messages.append({"role": "user", "content": user_prompt})

        resp = client.chat.completions.create(
            model=model,
            messages=messages,
            temperature=0.2,
        )
        return resp.choices[0].message.content

    raw = _call()
    try:
        return safe_json_parse(raw)
    except Exception:
        raw2 = _call(
            "Your previous output was invalid JSON. Return ONLY valid JSON. No markdown, no explanation."
        )
        return safe_json_parse(raw2)


def enforce_need_followup(out: dict) -> dict:
    """
    BULLETPROOF: FastAPI response model requires needFollowup ALWAYS.
    If LLM forgets it, we set default needFollowup=false.
    """
    if "needFollowup" not in out:
        out["needFollowup"] = False
    return out


def enforce_allowed_values(
    services: List[str],
    action_groups: List[str],
    allowed_services: List[str],
    allowed_action_groups: Dict[str, List[str]]
) -> Tuple[List[str], List[str]]:
    """
    Ensure LLM output stays within allowed services and action groups.
    """
    services = [s for s in services if s in allowed_services]

    allowed_all_groups = set()
    for svc, groups in allowed_action_groups.items():
        for g in groups:
            allowed_all_groups.add(g)

    action_groups = [g for g in action_groups if g in allowed_all_groups]
    return services, action_groups


def get_missing_fields(data: dict) -> List[str]:
    """
    Contract mandatory fields check.
    Treat empty list / empty string as missing.
    """
    missing = []
    for field in MANDATORY_FIELDS:
        value = data.get(field)

        if value is None:
            missing.append(field)
        elif isinstance(value, str) and value.strip() == "":
            missing.append(field)
        elif isinstance(value, list) and len(value) == 0:
            missing.append(field)

    return missing


def make_followup(field: str, partial: Dict[str, Any]) -> dict:
    question_map = {
        "requesterEmail": "Please provide the requester email.",
        "awsAccount": "Please provide the AWS account ID.",
        "reason": "Please provide the reason for access.",
        "services": "Please confirm which AWS service(s) you need access to.",
        "resourceArns": "Please provide the AWS resource ARN(s) you need access to.",
        "durationHours": "Please provide the access duration in hours."
    }

    return {
        "needFollowup": True,
        "followupQuestion": {
            "field": field,
            "question": question_map.get(field, f"Please provide {field}.")
        },
        "partialData": partial
    }


# -----------------------------
# Endpoint: Suggest (LLM)
# -----------------------------
@app.post(
    "/api/v1/llm/suggest",
    response_model=SuggestResponse,
    response_model_exclude_none=True
)
def suggest(req: SuggestRequest):
    system_prompt = """
You are an AWS permission suggestion assistant.
Return ONLY JSON. No markdown.

Rules:
- Suggest only minimal and safe permissions (never admin/full access).
- Choose services ONLY from allowedServices.
- Choose actionGroups ONLY from allowedActionGroups.

Return format:
{
  "services": [...],
  "actionGroups": [...],
  "suggestion": "short sentence"
}
"""

    user_prompt = f"""
Reason:
{req.reason}

allowedServices:
{req.allowedServices}

allowedActionGroups:
{req.allowedActionGroups}
"""

    try:
        out = call_openai_json(system_prompt, user_prompt)

        services = out.get("services", [])
        action_groups = out.get("actionGroups", [])
        suggestion_text = out.get("suggestion", "Suggested permissions based on the request reason.")

        services, action_groups = enforce_allowed_values(
            services, action_groups, req.allowedServices, req.allowedActionGroups
        )

        return SuggestResponse(
            services=services,
            actionGroups=action_groups,
            suggestion=suggestion_text
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Suggest LLM error: {str(e)}")


# -----------------------------
# Endpoint: Interpret (LLM)
# -----------------------------
@app.post(
    "/api/v1/llm/interpret",
    response_model=InterpretResponse,
    response_model_exclude_none=True
)
def interpret(req: InterpretRequest):
    system_prompt = """
You are an AWS Access Request Interpreter.
Return ONLY JSON. No markdown.

Mandatory fields for completion:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

Rules:
- Infer services and actionGroups from the reason if possible.
- Choose services ONLY from allowedServices.
- Choose actionGroups ONLY from allowedActionGroups.
- Ask ONE follow-up question at a time when mandatory data is missing.

Follow-up format:
{
  "needFollowup": true,
  "followupQuestion": { "field": "<missingField>", "question": "<question>" },
  "partialData": { ... }
}

Complete format:
{
  "needFollowup": false,
  "requesterEmail": "...",
  "awsAccount": "...",
  "reason": "...",
  "services": [...],
  "resourceArns": [...],
  "durationHours": 24,
  "actionGroups": [...]
}
"""

    user_prompt = f"""
requestId: {req.requestId}

requesterEmail: {req.requesterEmail}
awsAccount: {req.awsAccount}
reason: {req.reason}
durationHours: {req.durationHours}

allowedServices:
{req.allowedServices}

allowedActionGroups:
{req.allowedActionGroups}
"""

    try:
        out = call_openai_json(system_prompt, user_prompt)
        out = enforce_need_followup(out)

        if out.get("needFollowup") is False:
            services = out.get("services", [])
            action_groups = out.get("actionGroups", [])

            services, action_groups = enforce_allowed_values(
                services, action_groups, req.allowedServices, req.allowedActionGroups
            )
            out["services"] = services
            out["actionGroups"] = action_groups

            missing = get_missing_fields(out)
            if missing:
                field = missing[0]
                partial = {
                    "requesterEmail": req.requesterEmail,
                    "awsAccount": req.awsAccount,
                    "reason": req.reason,
                    "durationHours": req.durationHours,
                    "services": out.get("services", []),
                    "actionGroups": out.get("actionGroups", []),
                    "resourceArns": out.get("resourceArns", None),
                }
                return make_followup(field, partial)

        # FINAL GUARANTEE
        out = enforce_need_followup(out)
        return out

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Interpret LLM error: {str(e)}")


# -----------------------------
# Endpoint: Resolve Follow-up (LLM)
# -----------------------------
@app.post(
    "/api/v1/llm/resolve-followup",
    response_model=InterpretResponse,
    response_model_exclude_none=True
)
def resolve_followup(req: ResolveFollowupRequest):
    system_prompt = """
You are an AWS Access Request Follow-up Resolver.
Return ONLY JSON. No markdown.

You will receive:
- partialData (already known values)
- followupAnswer (new user answer)

Task:
- merge followupAnswer into partialData
- if still missing mandatory fields, ask NEXT follow-up question (one at a time)
- else return complete JSON

Mandatory fields:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

Output format same as /interpret.
"""

    user_prompt = f"""
requestId: {req.requestId}

partialData:
{req.partialData}

followupAnswer:
{req.followupAnswer}

allowedServices:
{req.allowedServices}

allowedActionGroups:
{req.allowedActionGroups}
"""

    try:
        out = call_openai_json(system_prompt, user_prompt)
        out = enforce_need_followup(out)

        if out.get("needFollowup") is False:
            services = out.get("services", [])
            action_groups = out.get("actionGroups", [])

            services, action_groups = enforce_allowed_values(
                services, action_groups, req.allowedServices, req.allowedActionGroups
            )
            out["services"] = services
            out["actionGroups"] = action_groups

            missing = get_missing_fields(out)
            if missing:
                field = missing[0]
                partial = req.partialData.copy()
                partial["services"] = out.get("services", partial.get("services", []))
                partial["actionGroups"] = out.get("actionGroups", partial.get("actionGroups", []))
                partial["resourceArns"] = out.get("resourceArns", partial.get("resourceArns", None))
                partial["durationHours"] = out.get("durationHours", partial.get("durationHours", None))
                return make_followup(field, partial)

        # FINAL GUARANTEE
        out = enforce_need_followup(out)
        return out

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Resolve-followup LLM error: {str(e)}")

