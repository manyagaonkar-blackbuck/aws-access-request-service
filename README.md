# aws-access-request-service
## Day-2 Status
- Backend orchestrates LLM calls
- Follow-up flow supported
- Approval flow unchanged
- LLM integration contract enforced
- System demo-ready with mock LLM

## Access Approval Flow

1. User creates access request
2. Manager approves or rejects
3. DevOps approves or rejects
4. IAM policy and AWS CLI are available ONLY after DevOps approval
5. Access expires automatically after durationHours

## Status Rules

- CREATED → MANAGER_APPROVED / MANAGER_REJECTED
- MANAGER_APPROVED → DEVOPS_APPROVED / DEVOPS_REJECTED
- DEVOPS_APPROVED → EXPIRED (automatic)

## Security Guarantees

- No IAM policy before DevOps approval
- No AWS CLI command before DevOps approval
- Expired requests are blocked everywhere
- No auto approval
- No status bypass

## Outputs

- IAM Policy JSON
- Downloadable policy file
- AWS CLI command
- Status endpoint for dashboards
