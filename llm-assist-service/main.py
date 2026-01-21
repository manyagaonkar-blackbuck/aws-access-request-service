from fastapi import FastAPI
from pydantic import BaseModel
from typing import Dict, List, Optional, Any

app = FastAPI(title="LLM Assist Service", version="1.0.0")


# -----------------------------
# Request/Response Models
# -----------------------------

class InterpretRequest(BaseModel):
    requestId: str
    requesterEmail: Optional[str] = None
    awsAccount: Optional[str] = None
    reason: str
    durationHours: Optional[int] = None
    allowedServices: List[str]
    allowedActionGroups: Dict[str, List[str]]


class FollowupQuestion(BaseModel):
    field: str
    question: str


class InterpretResponse(BaseModel):
    needFollowup: bool
    followupQuestion: Optional[FollowupQuestion] = None
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
# Main Endpoint (MOCK Day-1)
# -----------------------------
@app.post("/api/v1/llm/interpret", response_model=InterpretResponse)
def interpret(req: InterpretRequest):
    # Day-1 mock logic (no real LLM yet)

    # Example: If reason mentions S3, we assume service S3 is needed
    services = []
    action_groups = []

    reason_lower = req.reason.lower()

    if "s3" in reason_lower:
        if "S3" in req.allowedServices:
            services = ["S3"]
            # choose action groups only from allowedActionGroups
            possible = req.allowedActionGroups.get("S3", [])
            if "UPLOAD_OBJECTS" in possible:
                action_groups.append("UPLOAD_OBJECTS")
            if "READ_OBJECTS" in possible:
                action_groups.append("READ_OBJECTS")

    # Mandatory fields check for completion
    # requesterEmail, awsAccount, reason, services, resourceArns, durationHours
    missing_fields = []

    if not req.requesterEmail:
        missing_fields.append("requesterEmail")
    if not req.awsAccount:
        missing_fields.append("awsAccount")
    if not req.reason:
        missing_fields.append("reason")
    if not services:
        missing_fields.append("services")
    if not req.durationHours:
        missing_fields.append("durationHours")

    # resourceArns is always unknown on Day-1 mock, so we ask for it
    missing_fields.append("resourceArns")

    # Ask ONE follow-up at a time (follow-up loop will happen in Day 2+)
    if missing_fields:
        return InterpretResponse(
            needFollowup=True,
            followupQuestion=FollowupQuestion(
                field=missing_fields[0],
                question=f"Please provide {missing_fields[0]}."
            ),
            requesterEmail=req.requesterEmail,
            awsAccount=req.awsAccount,
            reason=req.reason,
            services=services,
            durationHours=req.durationHours,
            actionGroups=action_groups
        )

    # If all mandatory fields exist (rare in mock), return complete
    return InterpretResponse(
        needFollowup=False,
        requesterEmail=req.requesterEmail,
        awsAccount=req.awsAccount,
        reason=req.reason,
        services=services,
        resourceArns=["arn:aws:s3:::example-bucket/*"],
        durationHours=req.durationHours,
        actionGroups=action_groups
    )

