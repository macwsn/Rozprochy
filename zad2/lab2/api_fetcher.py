import asyncio
import logging
import httpx
from config import REQUEST_TIMEOUT, PUBLIC_APIS

logger = logging.getLogger(__name__)

async def _execute_plan(plan: dict, client: httpx.AsyncClient) -> dict:
    api_id   = plan["api_id"]
    api_name = plan.get("api_name", PUBLIC_APIS.get(api_id, {}).get("name", api_id))
    url      = plan["url"]
    params   = plan.get("params") or {}
    headers  = plan.get("headers") or {}

    logger.info("→ Calling [%s]  GET %s  params=%s", api_name, url, params)

    try:
        resp = await client.request(method=plan.get("method", "GET"),url=url,params=params,headers=headers,timeout=REQUEST_TIMEOUT,)
        resp.raise_for_status()
        json_data = resp.json()
        logger.info("OK [%s]  status=%s  data=%s", api_name, resp.status_code, str(json_data)[:400])
        return {
            "api_id":     api_id,
            "api_name":   api_name,
            "url_called": str(resp.url),
            "status":     resp.status_code,
            "data":       json_data,
        }
    except httpx.TimeoutException:
        logger.warning("FAIL [%s]  timeout", api_name)
        return {"api_id": api_id, "api_name": api_name, "url_called": url, "error": "Request timed out"}
    except httpx.HTTPStatusError as exc:
        logger.warning("FAIL [%s]  HTTP %s", api_name, exc.response.status_code)
        return {"api_id": api_id, "api_name": api_name, "url_called": url, "error": f"HTTP {exc.response.status_code}"}
    except Exception as exc:
        logger.exception("FAIL [%s]  unexpected error", api_name)
        return {"api_id": api_id, "api_name": api_name, "url_called": url, "error": str(exc)}

async def fetch_all(plans: list[dict], client: httpx.AsyncClient) -> list[dict]:
    logger.info("Firing %d API requests concurrently", len(plans))
    return await asyncio.gather(*[_execute_plan(p, client) for p in plans])