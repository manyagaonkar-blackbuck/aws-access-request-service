## LLM → Backend Contract (Day-2 Locked)

LLM responsibilities:
- Interpret user intent
- Suggest services + action groups
- Ask follow-up questions if mandatory fields missing
- Return deterministic JSON only

LLM must NOT:
- Auto-approve requests
- Change request status
- Call backend APIs

Backend responsibilities:
- Orchestrate LLM calls
- Validate mandatory fields
- Persist request only when complete
- Enforce approval workflow

Mandatory fields:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours
