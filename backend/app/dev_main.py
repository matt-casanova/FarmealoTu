"""App FastAPI TEMPORAL de Alvin para probar sus endpoints mientras no exista el main.py de Matías.

    cd backend; .\\.venv\\Scripts\\Activate.ps1
    uvicorn app.dev_main:app --reload --host 0.0.0.0 --port 8000      →  http://localhost:8000/docs

Cuando Matías suba app/main.py (con `app.include_router(nasa.router)`), este archivo se borra.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware

from app.routers import nasa

app = FastAPI(title="FarmealoTu API (dev de Alvin: solo endpoints NASA)", version="0.1.0-dev")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["GET"], allow_headers=["*"])
# /api/indicators pesa ~70 KB en JSON; con gzip baja mucho (OkHttp lo descomprime solo). Matías: copiá esto a main.py
app.add_middleware(GZipMiddleware, minimum_size=1000)
app.include_router(nasa.router)


@app.get("/api/health")
def health():
    return {"status": "ok", "app": "dev_main (temporal)", "note": "main.py de Matías pendiente"}
