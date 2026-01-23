from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI
import os
import json

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI(title="LLM Assist Service")

class LlmRequest(BaseModel):
    text: str

@app.get("/health")
def health():
    return {"status": "UP"}

@app.post("/interpret")
def interpret(req: LlmRequest):
    prompt = f"""
Convert this AWS access request into JSON.
Return ONLY valid JSON. No explanations.

Text:
{req.text}

JSON format:
{{
  "services": ["S3"],
  "durationHours": 24,
  "reason": "..."
}}
"""

    response = client.responses.create(
        model="gpt-4.1-mini",
        input=prompt,
        temperature=0
    )

    raw = response.output_text

    # ensure backend always receives JSON
    return json.loads(raw)
