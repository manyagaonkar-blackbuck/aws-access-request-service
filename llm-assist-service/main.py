from fastapi import FastAPI
from pydantic import BaseModel
from typing import Dict, List, Optional


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
    # Always present
    needFollowup: bool

    # Present only when follow-up is required
    followupQuestion: Optional[FollowupQuestion] = None
    partialData: Optional[PartialData] = None

    # Present only when request is complete
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: Optional[str] = None
    services: Optional[List[str]] = None
    resourceArns: Optional[List[str]] = None
    durationHours: Optional[int] = None
    actionGroups: Optional[List[str]] = None


# -----------------------------
# NEW: Resolve Follow-up Request Model (Day 2)
# -----------------------------
class ResolveFollowupRequest(BaseModel):
    requestId: str
    followupAnswer: str
    partialData: PartialData
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


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


def build_followup_question(field: str) -> str:
    question_map = {
        "requesterEmail": "Please provide the requester email.",
        "awsAccount": "Please provide the AWS account ID.",
        "reason": "Please provide the reason for access.",
        "services": "Please confirm which AWS service(s) you need access to.",
        "resourceArns": "Please provide the AWS resource ARN(s) you need access to.",
        "durationHours": "Please provide the access duration in hours."
    }
    return question_map.get(field, f"Please provide {field}.")


# -----------------------------
# Endpoint 1: Interpret (Contract-aligned mock)
# -----------------------------
@app.post(
    "/api/v1/llm/interpret",
    response_model=InterpretResponse,
    response_model_exclude_none=True
)
def interpret(req: InterpretRequest):
    """
    Day-1/Day-1.5 version:
    - Still MOCK logic (no real LLM call)
    - But response JSON is contract-aligned
    - Only chooses from allowedServices + allowedActionGroups
    - Asks follow-up until mandatory fields are satisfied
    """

    # 1) Decide services/action groups (mock)
    services: List[str] = []
    action_groups: List[str] = []

    reason_lower = req.reason.lower()

    # Example mapping: if reason mentions S3, suggest S3
    if "s3" in reason_lower and "S3" in req.allowedServices:
        services = ["S3"]
        allowed_s3_groups = req.allowedActionGroups.get("S3", [])

        if "UPLOAD_OBJECTS" in allowed_s3_groups:
            action_groups.append("UPLOAD_OBJECTS")
        if "READ_OBJECTS" in allowed_s3_groups:
            action_groups.append("READ_OBJECTS")

    # 2) Build structured output (resourceArns unknown today)
    structured = {
        "requesterEmail": req.requesterEmail,
        "awsAccount": req.awsAccount,
        "reason": req.reason,
        "durationHours": req.durationHours,
        "services": services,
        "resourceArns": None,  # unknown at interpret stage unless already provided
        "actionGroups": action_groups,
    }

    # 3) Validate mandatory fields
    missing_fields = get_missing_fields(structured)

    # 4) Follow-up mode
    if missing_fields:
        field = missing_fields[0]

        return InterpretResponse(
            needFollowup=True,
            followupQuestion=FollowupQuestion(
                field=field,
                question=build_followup_question(field)
            ),
            partialData=PartialData(
                requesterEmail=req.requesterEmail,
                awsAccount=req.awsAccount,
                reason=req.reason,
                durationHours=req.durationHours,
                services=services if services else None,
                actionGroups=action_groups if action_groups else None
            )
        )

    # 5) Complete mode (only if everything present)
    return InterpretResponse(
        needFollowup=False,
        requesterEmail=req.requesterEmail,
        awsAccount=req.awsAccount,
        reason=req.reason,
        services=services,
        resourceArns=["arn:aws:s3:::example-bucket/*"],  # mock placeholder
        durationHours=req.durationHours,
        actionGroups=action_groups
    )


# -----------------------------
# Endpoint 2: Resolve Follow-up (Day 2)
# -----------------------------
@app.post(
    "/api/v1/llm/resolve-followup",
    response_model=InterpretResponse,
    response_model_exclude_none=True
)
def resolve_followup(req: ResolveFollowupRequest):
    """
    Day-2 version:
    - Backend sends followupAnswer + partialData
    - We fill missing fields based on answer (still MOCK parsing)
    - If still missing mandatory fields -> ask next follow-up
    - Else -> return complete payload
    """

    # Start from partialData already collected
    updated = {
        "requesterEmail": req.partialData.requesterEmail,
        "awsAccount": req.partialData.awsAccount,
        "reason": req.partialData.reason,
        "durationHours": req.partialData.durationHours,
        "services": req.partialData.services or [],
        "resourceArns": req.partialData.resourceArns,
        "actionGroups": req.partialData.actionGroups or [],
    }

    answer = req.followupAnswer.strip()

    # -----------------------------
    # MOCK parsing rules (temporary)
    # -----------------------------
    # If answer contains ARN -> assume it is resourceArns
    if "arn:" in answer.lower():
        updated["resourceArns"] = [answer]

    # If answer is numeric -> assume it is durationHours
    if answer.isdigit():
        updated["durationHours"] = int(answer)

    # If user typed a service name in answer (very basic)
    # Example: "S3" -> set services=["S3"] if allowed
    if answer.upper() in req.allowedServices:
        updated["services"] = [answer.upper()]

        # if we set service, choose at least one allowed action group (example)
        groups = req.allowedActionGroups.get(answer.upper(), [])
        if groups and not updated["actionGroups"]:
            updated["actionGroups"] = [groups[0]]

    # Check mandatory fields again
    missing_fields = get_missing_fields(updated)

    if missing_fields:
        field = missing_fields[0]

        return InterpretResponse(
            needFollowup=True,
            followupQuestion=FollowupQuestion(
                field=field,
                question=build_followup_question(field)
            ),
            partialData=PartialData(
                requesterEmail=updated["requesterEmail"],
                awsAccount=updated["awsAccount"],
                reason=updated["reason"],
                durationHours=updated["durationHours"],
                services=updated["services"] if updated["services"] else None,
                resourceArns=updated["resourceArns"],
                actionGroups=updated["actionGroups"] if updated["actionGroups"] else None,
            )
        )

    # Return complete response
    return InterpretResponse(
        needFollowup=False,
        requesterEmail=updated["requesterEmail"],
        awsAccount=updated["awsAccount"],
        reason=updated["reason"],
        services=updated["services"],
        resourceArns=updated["resourceArns"],
        durationHours=updated["durationHours"],
        actionGroups=updated["actionGroups"]
    )

