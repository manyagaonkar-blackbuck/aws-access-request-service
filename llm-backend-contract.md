===========================================================
LLM ASSIST SERVICE ↔ SPRING BOOT BACKEND
API INTEGRATION CONTRACT (FINAL)
===========================================================

1) PURPOSE
----------
The LLM Assist Service is responsible ONLY for:
- Understanding the employee's "reason" text
- Suggesting AWS services + action groups
- Asking follow-up questions if mandatory details are missing
- Producing deterministic JSON output (no extra text)

The LLM Assist Service is NOT responsible for:
- Approval flow
- Status transitions
- Database storage
- IAM policy generation
- Audit logs

Approval workflow is owned by the Spring Boot Backend.

-----------------------------------------------------------

2) INTEGRATION MODEL
--------------------
Protocol: REST over HTTP
Format: JSON (Content-Type: application/json)

Caller: Spring Boot Backend (Orchestrator)
Callee: LLM Assist Service

Important: LLM service DOES NOT call backend directly.
Backend always calls LLM and backend decides whether to proceed.

-----------------------------------------------------------

3) BASE URL
-----------
Local:
- http://localhost:9000

Deployed:
- https://llm-assist.internal.company

-----------------------------------------------------------

4) REQUIRED HEADERS
-------------------
Backend -> LLM headers:

- Content-Type: application/json
- X-Correlation-Id: <uuid>            (required for request tracing)
- Authorization: Bearer <token>       (optional depending on deployment)

-----------------------------------------------------------

5) MANDATORY FIELDS RULE (CRITICAL)
-----------------------------------
For a request to be considered COMPLETE, the LLM MUST provide ALL of these:

- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

If ANY mandatory field is missing:
- LLM MUST return needFollowup=true
- LLM MUST ask follow-up question(s)
- LLM MUST NOT return needFollowup=false until all mandatory fields exist

-----------------------------------------------------------

6) ENDPOINT #1 - INTERPRET INITIAL REQUEST
------------------------------------------
Method: POST
Path: /api/v1/llm/interpret

6.1 Request JSON (Backend -> LLM)
--------------------------------
{
  "requestId": "AR-1029",
  "requesterEmail": "user@company.com",
  "awsAccount": "123456789012",
  "reason": "Need access to upload logs to S3",
  "durationHours": 24,
  "allowedServices": ["S3", "EC2", "CloudWatch"],
  "allowedActionGroups": {
    "S3": ["READ_OBJECTS", "UPLOAD_OBJECTS", "DELETE_OBJECTS", "LIST_BUCKET"],
    "EC2": ["READ_INSTANCES", "START_STOP_INSTANCES"],
    "CloudWatch": ["READ_LOGS", "WRITE_LOGS"]
  }
}

Notes:
- requestId is used for tracking/debugging.
- allowedServices and allowedActionGroups are constraints.
- LLM MUST NOT output services or actionGroups outside allowed lists.

6.2 Response JSON Case A - Follow-up Required
---------------------------------------------
HTTP 200 OK

{
  "needFollowup": true,
  "followupQuestions": [
    {
      "field": "resourceArns",
      "question": "Please provide the AWS resource ARN(s) you need access to (example: arn:aws:s3:::my-bucket/*)."
    }
  ],
  "partialData": {
    "services": ["S3"],
    "actionGroups": ["UPLOAD_OBJECTS"]
  }
}

Rules:
- LLM asks only ONE follow-up at a time (preferred)
- LLM must specify which field is missing via "field"
- partialData is optional but recommended

6.3 Response JSON Case B - Completed (Ready for approval flow)
--------------------------------------------------------------
HTTP 200 OK

{
  "needFollowup": false,
  "requesterEmail": "user@company.com",
  "awsAccount": "123456789012",
  "reason": "Need access to upload logs to S3",
  "services": ["S3"],
  "resourceArns": ["arn:aws:s3:::logs-bucket/logs/*"],
  "durationHours": 24,
  "actionGroups": ["UPLOAD_OBJECTS", "READ_OBJECTS"]
}

-----------------------------------------------------------

7) ENDPOINT #2 - RESOLVE FOLLOW-UP ANSWER
-----------------------------------------
Method: POST
Path: /api/v1/llm/resolve-followup

7.1 Request JSON (Backend -> LLM)
---------------------------------
{
  "requestId": "AR-1029",
  "reason": "Need access to upload logs to S3",
  "previousSuggestion": {
    "services": ["S3"],
    "actionGroups": ["UPLOAD_OBJECTS"]
  },
  "followupAnswer": "Bucket ARN is arn:aws:s3:::logs-bucket and prefix logs/*",
  "allowedServices": ["S3", "EC2", "CloudWatch"],
  "allowedActionGroups": {
    "S3": ["READ_OBJECTS", "UPLOAD_OBJECTS", "DELETE_OBJECTS", "LIST_BUCKET"]
  }
}

7.2 Response JSON (must be COMPLETE)
------------------------------------
HTTP 200 OK

{
  "needFollowup": false,
  "requesterEmail": "user@company.com",
  "awsAccount": "123456789012",
  "reason": "Need access to upload logs to S3",
  "services": ["S3"],
  "resourceArns": ["arn:aws:s3:::logs-bucket/logs/*"],
  "durationHours": 24,
  "actionGroups": ["UPLOAD_OBJECTS", "READ_OBJECTS"]
}

-----------------------------------------------------------

8) STATUS CODES / ERROR CONTRACT
--------------------------------
8.1 200 OK
- LLM successfully processed request
- May return follow-up OR complete response

8.2 400 BAD_REQUEST
- Backend sent invalid JSON or missing required keys

Example:
{
  "errorCode": "INVALID_INPUT",
  "message": "reason is required"
}

8.3 422 UNPROCESSABLE_ENTITY
- LLM understood input but cannot map intent to allowed services/action groups

Example:
{
  "errorCode": "UNSUPPORTED_REQUEST",
  "message": "Requested access does not match allowedActionGroups"
}

8.4 500 INTERNAL_SERVER_ERROR
- LLM provider failure / crash / unexpected error

Example:
{
  "errorCode": "LLM_FAILURE",
  "message": "Unable to generate deterministic JSON output"
}

-----------------------------------------------------------

9) TIMEOUT / RETRY POLICY (INTEGRATION)
---------------------------------------
Backend -> LLM timeout: 3 to 8 seconds recommended

Retry rules (backend side):
- Retry once if:
  - Timeout
  - 5xx errors

No retry if:
- 400 errors
- 422 errors

-----------------------------------------------------------

10) IDEMPOTENCY / CONSISTENCY
-----------------------------
- Backend sends requestId for tracking
- LLM output must be deterministic in format
- LLM must avoid random changes in field names/structure

-----------------------------------------------------------

11) OUTPUT RULES (STRICT)
-------------------------
LLM MUST:
- Return JSON only
- No explanations
- No markdown formatting
- No extra text outside JSON

LLM MUST NOT:
- Invent services/actionGroups outside allowed lists
- Mark response as complete if mandatory fields missing

-----------------------------------------------------------

12) OWNERSHIP CLARITY (FINAL)
-----------------------------
LLM Service owns:
- Intent extraction from reason
- Follow-up question generation
- Final structured output JSON

Spring Boot Backend owns:
- Saving request in DB
- Approval flow (Manager approval + DevOps approval)
- Status transitions
- IAM policy and CLI generation
- Audit logging

===========================================================
END OF CONTRACT
===========================================================

