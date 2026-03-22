from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from routers import query, health

app = FastAPI(title="API Explorer",description="Gemini managed API",version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["GET", "POST"], allow_headers=["Content-Type"])
app.include_router(query.router, prefix="/api/v1", tags=["Query"])
app.include_router(health.router, prefix="/api/v1", tags=["Health"])
app.mount("/static", StaticFiles(directory="static"), name="static")

@app.get("/", include_in_schema=False)
async def root(): return FileResponse("static/index.html")