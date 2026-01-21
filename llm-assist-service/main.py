from fastapi import FastAPI
from pydantic import BaseModel
from typing import Dict, List, Optional, Any


app = FastAPI(title="LLM Assist Service", version="1.0.0")


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


# -----------------------------
# Response Models
# -----------------------------
class FollowupQuestion(BaseModel):
    field: str
    question: str


class PartialData(BaseModel):
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: Optional[str] = None
    durationHours: Optional[int] = None
    services: Optional[List[str]] = None
    resourceArns: Optional[List[str]] = None
    actionGroups: Optional[List[str]] = None


class InterpretResponse(BaseModel):
    needFollowup: bool
    followupQuestion: Optional[FollowupQuestion] = None
    partialData: Optional[PartialData] = None

    # only present when needFollowup=false (complete)
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
# Helper: check missing fields
# -----------------------------
MANDATORY_FIELDS = [
    "requesterEmail",
    "awsAccount",
    "reason",
    "services",
    "resourceArns",
    "durationHours",
]


def get_missing_fields(data: dict) -> List[str]:
    missing = []
    for field in MANDATORY_FIELDS:
        value = data.get(field)

        # treat empty list / None / empty string as missing
        if value is None:
            missing.append(field)
        elif isinstance(value, str) and value.strip() == "":
            missing.append(field)
        elif isinstance(value, list) and len(value) == 0:
            missing.append(field)

    return missing


# -----------------------------
# Endpoint: Interpret (MOCK Day 1 but Contract-aligned)
# -----------------------------
@app.post("/api/v1/llm/interpret", response_model=InterpretResponse, response_model_exclude_none=True)
def interpret(req: InterpretRequest):
    """
    Day-1 implementation:
    - Contract-aligned JSON
    - Mock mapping: if reason contains "S3", choose S3
    - Ask follow-up until mandatory fields exist (backend will re-call resolve endpoint later)
    """

    # 1) Decide services/actionGroups (mock rules)
    services: List[str] = []
    action_groups: List[str] = []

    reason_lower = req.reason.lower()

    if "s3" in reason_lower and "S3" in req.allowedServices:
        services = ["S3"]
        allowed_s3_groups = req.allowedActionGroups.get("S3", [])

        if "UPLOAD_OBJECTS" in allowed_s3_groups:
            action_groups.append("UPLOAD_OBJECTS")
        if "READ_OBJECTS" in allowed_s3_groups:
            action_groups.append("READ_OBJECTS")

    # 2) Build current structured data (resourceArns unknown on Day-1)
    structured = {
        "requesterEmail": req.requesterEmail,
        "awsAccount": req.awsAccount,
        "reason": req.reason,
        "durationHours": req.durationHours,
        "services": services,
        "resourceArns": None,  # unknown until user answers follow-up
        "actionGroups": action_groups,
    }

    # 3) Check missing mandatory fields
    missing = get_missing_fields(structured)

    # 4) If missing -> ask ONE follow-up question
    if missing:
        field = missing[0]

        question_map = {
            "requesterEmail": "Please provide your requester email.",
            "awsAccount": "Please provide the AWS account ID.",
            "services": "Please confirm which AWS service(s) you need access to.",
            "resourceArns": "Please provide the AWS resource ARN(s) you need access to.",
            "durationHours": "Please provide the access duration in hours.",
            "reason": "Please provide the reason for access."
        }

        return InterpretResponse(
            needFollowup=True,
            followupQuestion=FollowupQuestion(
                field=field,
                question=question_map.get(field, f"Please provide {field}.")
            ),
            partialData=PartialData(
                requesterEmail=req.requesterEmail,
                awsAccount=req.awsAccount,
                reason=req.reason,
                durationHours=req.durationHours,
                services=services if services else None,
                actionGroups=action_groups if action_groups else None,
            )
        )

    # 5) If complete -> return full payload
    return InterpretResponse(
        needFollowup=False,
        requesterEmail=req.requesterEmail,
        awsAccount=req.awsAccount,
        reason=req.reason,
        durationHours=req.durationHours,
        services=services,
        resourceArns=["arn:aws:s3:::example-bucket/*"],
        actionGroups=action_groups
    )

