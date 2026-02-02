import json
import requests

BASE_URL = "http://localhost:9000"


def pretty(obj):
    print(json.dumps(obj, indent=2))


def main():
    print("\n✅ Testing main.py APIs (FastAPI LLM Assist Service)")
    print("Make sure server is running:")
    print("uvicorn main:app --host 0.0.0.0 --port 9000 --reload\n")

    reason = input("Enter reason: ").strip()
    if not reason:
        print("❌ Reason cannot be empty")
        return

    requester_email = input("Requester Email (optional, press enter to skip): ").strip() or None
    aws_account = input("AWS Account (optional, press enter to skip): ").strip() or None
    duration = input("Duration Hours (optional, press enter to skip): ").strip()

    duration_hours = None
    if duration:
        try:
            duration_hours = int(duration)
        except:
            duration_hours = None

    # --------------------------
    # 1) CALL /suggest
    # --------------------------
    suggest_payload = {
        "reason": reason,
        "allowedServices": ["S3", "EC2"],
        "allowedActionGroups": {
            "S3": ["UPLOAD_OBJECTS", "READ_OBJECTS", "DELETE_OBJECTS"],
            "EC2": ["READ_INSTANCES"]
        }
    }

    print("\n==============================")
    print("🚀 Calling /suggest")
    print("==============================")
    r = requests.post(f"{BASE_URL}/api/v1/llm/suggest", json=suggest_payload)
    suggest_out = r.json()
    pretty(suggest_out)

    # --------------------------
    # 2) CALL /interpret
    # --------------------------
    interpret_payload = {
        "requestId": "AR-TEST-LOOP-1",
        "requesterEmail": requester_email,
        "awsAccount": aws_account,
        "reason": reason,
        "durationHours": duration_hours,
        "allowedServices": ["S3", "EC2"],
        "allowedActionGroups": {
            "S3": ["UPLOAD_OBJECTS", "READ_OBJECTS", "DELETE_OBJECTS"],
            "EC2": ["READ_INSTANCES"]
        }
    }

    print("\n==============================")
    print("🚀 Calling /interpret")
    print("==============================")
    r2 = requests.post(f"{BASE_URL}/api/v1/llm/interpret", json=interpret_payload)
    out = r2.json()
    pretty(out)

    # --------------------------
    # 3) FOLLOW-UP LOOP
    # --------------------------
    while out.get("needFollowup") is True:
        fq = out.get("followupQuestion", {})
        field = fq.get("field")
        question = fq.get("question")

        print("\n==============================")
        print("❓ Follow-up Required")
        print("==============================")
        print(f"Field: {field}")
        print(f"Question: {question}")

        answer = input(f"Enter answer for {field}: ").strip()

        resolve_payload = {
            "requestId": "AR-TEST-LOOP-1",
            "followupAnswer": answer,
            "partialData": out.get("partialData", {}),
            "allowedServices": ["S3", "EC2"],
            "allowedActionGroups": {
                "S3": ["UPLOAD_OBJECTS", "READ_OBJECTS", "DELETE_OBJECTS"],
                "EC2": ["READ_INSTANCES"]
            }
        }

        print("\n🔁 Calling /resolve-followup ...")
        r3 = requests.post(f"{BASE_URL}/api/v1/llm/resolve-followup", json=resolve_payload)
        out = r3.json()
        pretty(out)

    print("\n==============================")
    print("✅ FINAL COMPLETE JSON ✅")
    print("==============================")
    pretty(out)


if __name__ == "__main__":
    main()

