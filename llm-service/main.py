from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI
import os
import json

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI()

class LlmRequest(BaseModel):
    text: str

@app.post("/interpret")
def interpret(req: LlmRequest):
    prompt = f"""
Convert this access request into JSON.
Return ONLY valid JSON.

Text:
{req.text}

Format:
{{
  "services": ["S3"],
  "durationHours": 24,
  "reason": "..."
}}
"""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "user", "content": prompt}
            ],
            temperature=0
        )

        raw = response.choices[0].message.content
        return json.loads(raw)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
