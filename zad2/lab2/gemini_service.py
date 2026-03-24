import json
import logging
import httpx

from config import GEMINI_API_URL, PUBLIC_APIS

logger = logging.getLogger(__name__)

async def _call_gemini(prompt: str, client: httpx.AsyncClient) -> str:
    payload = {"contents": [{"parts": [{"text": prompt}]}],"generationConfig": {"temperature": 0.2, "maxOutputTokens": 2048},}
    resp = await client.post(GEMINI_API_URL, json=payload, timeout=25)
    resp.raise_for_status()
    data = resp.json()
    return data["candidates"][0]["content"]["parts"][0]["text"].strip()

def _strip_fences(raw: str) -> str:
    raw = raw.strip()
    if raw.startswith("```"): raw = raw.split("\n", 1)[-1]
    if raw.endswith("```"):raw = raw.rsplit("```", 1)[0]
    return raw.strip()

def _catalog_text() -> str:
    lines = []
    for key, meta in PUBLIC_APIS.items():
        topics = ", ".join(meta["example_topics"])
        lines.append(f'  ID="{key}"  name="{meta["name"]}"  base_url="{meta["base_url"]}"  topics=[{topics}]')
    return "\n".join(lines)


async def select_and_plan(query: str, client: httpx.AsyncClient) -> list[dict]:
    catalog = _catalog_text()
    prompt = f"""You are an API orchestrator. Given a user question and a catalog of public REST APIs,
choose 2 to 3 APIs most relevant to the question and generate the exact HTTP request for each.

Respond ONLY with a valid JSON array — no markdown, no explanation.
Each element must have: "api_id", "api_name", "url", "method" (always "GET"), "params" (object), "headers" (object).

Hints:
- Numbers API url: http://numbersapi.com/{{number}}/trivia
- Bored API url: https://bored-api.appbrewery.com/random
- Useless Facts url: https://uselessfacts.jsph.pl/api/v2/facts/random  params: {{"language":"en"}}
- Cat Facts url: https://catfact.ninja/facts  params: {{"limit":3}}
- Dog CEO url: https://dog.ceo/api/breeds/image/random/3
- IP-API url: http://ip-api.com/json/8.8.8.8
- Open Trivia params: {{"amount":3,"type":"multiple"}}
- jService url: https://jservice.io/api/random  params: {{"count":3}}
- REST Countries url: https://restcountries.com/v3.1/name/{{country_name}}
- Open Library params: {{"q":"keyword","limit":5}}
- TheMealDB / TheCocktailDB params: {{"s":"keyword"}}

User question: {query}

API Catalog:
{catalog}
"""
    raw = await _call_gemini(prompt, client)
    plans: list[dict] = json.loads(_strip_fences(raw))
    valid = [p for p in plans if p.get("api_id") in PUBLIC_APIS and p.get("url")]
    if not valid:
        raise ValueError(f"Gemini returned no valid plans: {raw[:200]}")
    return valid[:3]


async def synthesize_answer(query: str, api_results: list[dict], client: httpx.AsyncClient) -> str:
    results_text = json.dumps(api_results, ensure_ascii=False, indent=2)
    prompt = f"""You are a helpful assistant. A user asked:
"{query}"

Data collected from public APIs:
{results_text}

Write a clear, concise answer in plain text with simple markdown (bold, bullet points).
Do not mention API names or JSON structure. Speak directly to the user.
"""
    return await _call_gemini(prompt, client)