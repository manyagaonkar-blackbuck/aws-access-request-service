## LLM → Backend Contract

### Mode
LLM will directly call backend REST APIs .

### Mandatory Fields (LLM MUST send all)
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

If any mandatory field is missing, LLM must ask follow-up questions
and MUST NOT call backend.

### LLM MUST NOT
- Auto-approve requests
- Bypass manager approval
- Bypass devops approval
- Directly modify request status
- Write to database

### Backend Responsibilities
- Validate request payload
- Create access request with status = CREATED
- Enforce approval workflow:
  CREATED → MANAGER_APPROVED → DEVOPS_APPROVED
- Reject invalid state transitions
- Act as final source of truth
