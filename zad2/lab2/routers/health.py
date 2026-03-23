from fastapi import APIRouter
from pydantic import BaseModel
from time import time

router = APIRouter()
_start_time = time()

class HealthResponse(BaseModel):
    status: str
    uptime_seconds: float
    version: str

@router.get("/health",summary="Service health check",response_model=HealthResponse)

async def health():return HealthResponse(status="ok",uptime_seconds=round(time() - _start_time, 2),version="1.0.0")