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

app = FastAPI(title="LLM Assist Service", version="3.4.0")


# -----------------------------
# Request Models
# -----------------------------
class SuggestRequest(BaseModel):
    reason: str
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


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
    needFollowup: bool

    followupQuestion: Optional[FollowupQuestion] = None
    partialData: Optional[Dict[str, Any]] = None

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
# Contract Rules
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
    Hard requirement:
    - LLM returns ONLY JSON
    - retry once if invalid JSON
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
            temperature=0.0,  # deterministic
        )
        return resp.choices[0].message.content

    raw = _call()
    try:
        return safe_json_parse(raw)
    except Exception:
        raw2 = _call(
            "INVALID JSON. Return ONLY valid JSON. No markdown. No explanation. Output MUST be JSON object."
        )
        return safe_json_parse(raw2)


def enforce_allowed_values(
    services: List[str],
    action_groups: List[str],
    allowed_services: List[str],
    allowed_action_groups: Dict[str, List[str]]
) -> Tuple[List[str], List[str]]:
    services = [s for s in services if s in allowed_services]

    allowed_group_set = set()
    for svc, groups in allowed_action_groups.items():
        for g in groups:
            allowed_group_set.add(g)

    action_groups = [g for g in action_groups if g in allowed_group_set]
    return services, action_groups


def enforce_need_followup(out: dict) -> dict:
    if "needFollowup" not in out:
        out["needFollowup"] = False
    return out


def get_missing_fields(data: dict) -> List[str]:
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
    q = {
        "requesterEmail": "Please provide the requester email.",
        "awsAccount": "Please provide the AWS account ID (12 digits).",
        "reason": "Please provide the reason for access.",
        "services": "Please confirm which AWS service(s) you need access to.",
        "actionGroups": (
            "Please specify what actions you need in the service "
            "(example for S3: READ_OBJECTS / UPLOAD_OBJECTS / DELETE_OBJECTS / LIST_BUCKET / MANAGE_BUCKET)."
        ),
        "resourceArns": "Please provide the AWS resource ARN(s) you need access to.",
        "durationHours": "Please provide the access duration in hours.",
    }
    return {
        "needFollowup": True,
        "followupQuestion": {"field": field, "question": q.get(field, f"Please provide {field}.")},
        "partialData": partial
    }


def is_vague_reason(reason: str) -> bool:
    """
    Simple heuristic:
    If reason is too generic, we ask user to specify actionGroups first.
    """
    r = (reason or "").strip().lower()

    vague_phrases = [
        "need access to s3",
        "need s3 access",
        "need access to bucket",
        "need access to s3 bucket",
        "need access to s3 for project",
        "need access for my project",
        "need s3 permissions",
        "need s3 permission",
        "s3 bucket access",
        "bucket access",
        "project work",
        "work on s3"
    ]

    # if any vague phrase is present -> vague
    return any(v in r for v in vague_phrases)


# -----------------------------
# Endpoint: /suggest ✅ USP output
# -----------------------------
@app.post("/api/v1/llm/suggest", response_model=SuggestResponse)
def suggest(req: SuggestRequest):
    """
    reason -> services + actionGroups suggestion (impressive manager-level message)
    """

    system_prompt = """
You are an AWS access request suggestion assistant for an internal DevOps access system.

Return ONLY JSON in EXACT format:
{
  "services": ["S3"],
  "actionGroups": ["READ_OBJECTS", "LIST_BUCKET"],
  "suggestion": "..."
}

STRICT RULES:
- Choose services ONLY from allowedServices
- Choose actionGroups ONLY from allowedActionGroups
- Output MUST follow least privilege (minimum permissions)
- DO NOT output extra keys
- DO NOT output markdown
- suggestion MUST be unique, detailed, and informative (3–6 lines max)
- suggestion MUST NOT ONLY ask for ARN/duration (those will be collected in follow-up)

S3 Action Groups meaning:
- READ_OBJECTS: download/read objects
- UPLOAD_OBJECTS: upload/put objects
- DELETE_OBJECTS: delete objects
- LIST_BUCKET: list objects in bucket
- MANAGE_BUCKET: create/delete bucket, bucket policy, versioning

IMPORTANT:
- You can return MULTIPLE actionGroups if required.
- Reading/downloading -> READ_OBJECTS + LIST_BUCKET
- Uploading logs -> UPLOAD_OBJECTS + LIST_BUCKET
- Deleting -> DELETE_OBJECTS + LIST_BUCKET
- Creating bucket/policy/versioning -> MANAGE_BUCKET

Suggestion writing format (MANDATORY):
The suggestion MUST include these sections in plain text (single string):
1) "Intent:"
2) "Permission scope:"
3) "Safer Alternative:"
4) "Notes:"

You MAY include ARN examples briefly:
- arn:aws:s3:::bucket-name/*
- arn:aws:s3:::bucket-name/prefix/*
"""

    user_prompt = f"""
reason:
{req.reason}

allowedServices:
{req.allowedServices}

allowedActionGroups:
{req.allowedActionGroups}
"""

    try:
        out = call_openai_json(system_prompt, user_prompt)

        services = out.get("services", []) or []
        action_groups = out.get("actionGroups", []) or []
        suggestion_text = out.get("suggestion", "Suggested minimal permissions based on the reason.")

        services, action_groups = enforce_allowed_values(
            services, action_groups, req.allowedServices, req.allowedActionGroups
        )

        return SuggestResponse(
            services=services,
            actionGroups=action_groups,
            suggestion=suggestion_text
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Suggest error: {str(e)}")


# -----------------------------
# Endpoint: /interpret ✅ Contract output ONLY
# -----------------------------
@app.post("/api/v1/llm/interpret", response_model=InterpretResponse, response_model_exclude_none=True)
def interpret(req: InterpretRequest):
    system_prompt = """
You are an AWS Access Request Interpreter for a DevOps access approval system.

Return ONLY JSON. No markdown. No explanation.

Mandatory fields for completion:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

Rules:
- Infer services and actionGroups from reason if possible.
- Choose services ONLY from allowedServices.
- Choose actionGroups ONLY from allowedActionGroups.
- Ask ONE follow-up question at a time until all mandatory fields are present.

S3 Action Groups meaning:
- READ_OBJECTS: download/read objects
- UPLOAD_OBJECTS: upload/put objects
- DELETE_OBJECTS: delete objects
- LIST_BUCKET: list objects in bucket
- MANAGE_BUCKET: create/delete bucket, bucket policy, versioning

IMPORTANT:
You can return MULTIPLE actionGroups when required:
- READ_OBJECTS + LIST_BUCKET (download/read)
- UPLOAD_OBJECTS + LIST_BUCKET (upload logs)
- DELETE_OBJECTS + LIST_BUCKET (cleanup/removal)
- MANAGE_BUCKET (bucket creation/policy)

Follow-up response format:
{
  "needFollowup": true,
  "followupQuestion": { "field": "<missingField>", "question": "<question>" },
  "partialData": {
    "requesterEmail": "...",
    "awsAccount": "...",
    "reason": "...",
    "durationHours": 24,
    "services": ["S3"],
    "actionGroups": ["READ_OBJECTS", "LIST_BUCKET"],
    "resourceArns": []
  }
}

Complete response format:
{
  "needFollowup": false,
  "requesterEmail": "...",
  "awsAccount": "...",
  "reason": "...",
  "services": ["S3"],
  "resourceArns": ["arn:aws:s3:::bucket/prefix/*"],
  "durationHours": 24,
  "actionGroups": ["READ_OBJECTS", "LIST_BUCKET"]
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

        services = out.get("services", []) or []
        action_groups = out.get("actionGroups", []) or []

        services, action_groups = enforce_allowed_values(
            services, action_groups, req.allowedServices, req.allowedActionGroups
        )

        out["services"] = services
        out["actionGroups"] = action_groups

        # ✅ NEW: if reason is vague -> ask actionGroups first (before ARN)
        if is_vague_reason(req.reason):
            if not out.get("actionGroups") or len(out.get("actionGroups", [])) == 0:
                partial = {
                    "requesterEmail": req.requesterEmail,
                    "awsAccount": req.awsAccount,
                    "reason": req.reason,
                    "durationHours": req.durationHours,
                    "services": out.get("services", []),
                    "actionGroups": out.get("actionGroups", []),
                    "resourceArns": out.get("resourceArns", []),
                }
                return make_followup("actionGroups", partial)

        # Existing mandatory follow-up logic
        if out.get("needFollowup") is False:
            missing = get_missing_fields(out)
            if missing:
                field = missing[0]
                partial = {
                    "requesterEmail": out.get("requesterEmail", req.requesterEmail),
                    "awsAccount": out.get("awsAccount", req.awsAccount),
                    "reason": out.get("reason", req.reason),
                    "durationHours": out.get("durationHours", req.durationHours),
                    "services": out.get("services", []),
                    "actionGroups": out.get("actionGroups", []),
                    "resourceArns": out.get("resourceArns", []),
                }
                return make_followup(field, partial)

        return out

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Interpret error: {str(e)}")


# -----------------------------
# Endpoint: /resolve-followup ✅ Follow-up loop
# -----------------------------
@app.post("/api/v1/llm/resolve-followup", response_model=InterpretResponse, response_model_exclude_none=True)
def resolve_followup(req: ResolveFollowupRequest):
    system_prompt = """
You are an AWS Access Request Follow-up Resolver.

Return ONLY JSON. No markdown. No explanation.

Mandatory fields:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

Rules:
- Merge followupAnswer into partialData.
- If mandatory fields still missing, ask NEXT follow-up question (one at a time).
- Else return needFollowup=false with full JSON.

S3 Action Groups meaning:
- READ_OBJECTS: download/read objects
- UPLOAD_OBJECTS: upload/put objects
- DELETE_OBJECTS: delete objects
- LIST_BUCKET: list objects in bucket
- MANAGE_BUCKET: create/delete bucket, bucket policy, versioning

IMPORTANT:
You can return MULTIPLE actionGroups when required:
- READ_OBJECTS + LIST_BUCKET
- UPLOAD_OBJECTS + LIST_BUCKET
- DELETE_OBJECTS + LIST_BUCKET
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

        services = out.get("services", []) or req.partialData.get("services", []) or []
        action_groups = out.get("actionGroups", []) or req.partialData.get("actionGroups", []) or []

        services, action_groups = enforce_allowed_values(
            services, action_groups, req.allowedServices, req.allowedActionGroups
        )

        out["services"] = services
        out["actionGroups"] = action_groups

        if out.get("needFollowup") is False:
            missing = get_missing_fields(out)
            if missing:
                field = missing[0]
                partial = req.partialData.copy()
                partial["services"] = out.get("services", partial.get("services", []))
                partial["actionGroups"] = out.get("actionGroups", partial.get("actionGroups", []))
                partial["resourceArns"] = out.get("resourceArns", partial.get("resourceArns", []))
                partial["durationHours"] = out.get("durationHours", partial.get("durationHours", None))
                return make_followup(field, partial)

        return out

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Resolve-followup error: {str(e)}")