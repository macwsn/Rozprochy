import logging
import time

import httpx
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field, field_validator
from config import PUBLIC_APIS
from gemini_service import select_and_plan, synthesize_answer
from api_fetcher import fetch_all

logger = logging.getLogger(__name__)
router = APIRouter()
class QueryRequest(BaseModel):
    query: str = Field(..., min_length=3, max_length=500)

    @field_validator("query")
    @classmethod
    def sanitize(cls, v: str) -> str:
        for bad in ["<script", "javascript:", "onerror"]:
            if bad.lower() in v.lower(): raise ValueError("Query contains forbidden content.")
        return v.strip()

class ApiUsed(BaseModel):
    id: str
    name: str
    url_called: str = ""
    raw_data: dict | list | None = None

class QueryResponse(BaseModel):
    query: str
    apis_used: list[ApiUsed]
    answer: str
    processing_time_ms: int


@router.get("/apis", summary="List all APIs in the catalog")
async def list_apis():
    return [{"id": k, "name": v["name"], "description": v["description"], "topics": v["example_topics"]} for k, v in PUBLIC_APIS.items()]


@router.post("/query", response_model=QueryResponse, summary="Ask a question")
async def query_endpoint(body: QueryRequest):
    logger.info("Query received: %r", body.query)
    start = time.monotonic()
    async with httpx.AsyncClient() as client:
        try:
            plans = await select_and_plan(body.query, client)
            logger.info("Gemini selected APIs: %s", [p["api_id"] for p in plans])
        except Exception as exc:
            logger.exception("Gemini planning failed")
            raise HTTPException(503, detail=f"Gemini planning error: {exc}") from exc
        try:
            api_results = await fetch_all(plans, client)
        except Exception as exc:
            logger.exception("fetch_all failed")
            raise HTTPException(502, detail=f"Upstream fetch error: {exc}") from exc
        try:
            answer = await synthesize_answer(body.query, api_results, client)
        except Exception as exc:
            logger.exception("Gemini synthesis failed")
            raise HTTPException(503, detail=f"Gemini synthesis error: {exc}") from exc

    elapsed = int((time.monotonic() - start) * 1000)
    logger.info("Query done in %dms", elapsed)

    return QueryResponse(query=body.query,apis_used=[ApiUsed(id=r["api_id"],name=r.get("api_name", r["api_id"]),
                            url_called=r.get("url_called", ""),raw_data=r.get("data"),)
            for r in api_results], answer=answer,processing_time_ms=elapsed, )