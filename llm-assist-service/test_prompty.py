import json
import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    raise RuntimeError("OPENAI_API_KEY not found in .env")

client = OpenAI(api_key=api_key)

MODEL = "gpt-4o-mini"


ALLOWED_SERVICES = ["S3"]
ALLOWED_ACTION_GROUPS = {
    "S3": ["UPLOAD_OBJECTS", "READ_OBJECTS"]
}

MANDATORY_FIELDS = [
    "requesterEmail",
    "awsAccount",
    "reason",
    "services",
    "resourceArns",
    "durationHours",
]


def call_llm(system_prompt: str, user_prompt: str) -> dict:
    resp = client.chat.completions.create(
        model=MODEL,
        messages=[
            {"role": "system", "content": system_prompt.strip()},
            {"role": "user", "content": user_prompt.strip()},
        ],
        temperature=0.2,
    )

    raw = resp.choices[0].message.content.strip()
    return json.loads(raw)


def ask_followup_loop(initial_input: dict):
    """
    This simulates:
    interpret -> followup -> resolve-followup -> followup -> ...
    """
    partial_data = {
        "requesterEmail": initial_input.get("requesterEmail"),
        "awsAccount": initial_input.get("awsAccount"),
        "reason": initial_input.get("reason"),
        "durationHours": initial_input.get("durationHours"),
        "services": None,
        "resourceArns": None,
        "actionGroups": None,
    }

    while True:
        system_prompt = """
You are an AWS Access Request Interpreter.
Return ONLY JSON. No markdown.

Mandatory fields:
- requesterEmail
- awsAccount
- reason
- services
- resourceArns
- durationHours

Rules:
- Choose services ONLY from allowedServices.
- Choose actionGroups ONLY from allowedActionGroups.
- Infer services/actionGroups from reason if possible.
- If still missing any mandatory field, ask ONE follow-up question at a time.

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
Current Data (partialData):
{json.dumps(partial_data)}

allowedServices:
{json.dumps(ALLOWED_SERVICES)}

allowedActionGroups:
{json.dumps(ALLOWED_ACTION_GROUPS)}
"""

        out = call_llm(system_prompt, user_prompt)

        # guardrail: always keep needFollowup
        if "needFollowup" not in out:
            out["needFollowup"] = False

        print("\n✅ LLM Output:\n", json.dumps(out, indent=2))

        if out.get("needFollowup") is False:
            print("\n🎉 DONE! Final completed request JSON above.")
            return out

        # followup mode
        fq = out.get("followupQuestion", {})
        field = fq.get("field")
        question = fq.get("question")

        if not field:
            print("\n❌ LLM did not provide followup field properly.")
            return out

        print(f"\n❓ FOLLOW-UP QUESTION: {question}")
        answer = input(f"Enter value for {field}: ").strip()

        # Update partial_data based on the follow-up field
        if field == "services":
            # accept comma-separated values
            partial_data["services"] = [x.strip() for x in answer.split(",") if x.strip()]
        elif field == "resourceArns":
            # accept comma-separated arns
            partial_data["resourceArns"] = [x.strip() for x in answer.split(",") if x.strip()]
        elif field == "durationHours":
            try:
                partial_data["durationHours"] = int(answer)
            except:
                partial_data["durationHours"] = answer
        else:
            partial_data[field] = answer

        # keep partialData updated with whatever LLM returned
        if "partialData" in out and isinstance(out["partialData"], dict):
            # merge LLM partial updates
            for k, v in out["partialData"].items():
                if v is not None:
                    partial_data[k] = v


if __name__ == "__main__":
    print("✅ Interactive Follow-up Tester (OpenAI)")

    requester_email = input("Requester Email: ").strip()
    aws_account = input("AWS Account (12 digits): ").strip()
    reason = input("Reason: ").strip()
    duration = input("Duration Hours (example 24): ").strip()

    try:
        duration_hours = int(duration)
    except:
        duration_hours = None

    initial = {
        "requesterEmail": requester_email,
        "awsAccount": aws_account,
        "reason": reason,
        "durationHours": duration_hours,
    }

    ask_followup_loop(initial)

