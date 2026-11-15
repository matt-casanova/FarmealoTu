"""API FarmealoTu (FastAPI). Backend principal (rama master).

    cd backend; .\\.venv\\Scripts\\Activate.ps1
    uvicorn app.main:app --host 0.0.0.0 --port 8000        →  http://localhost:8000/docs

Estado de cada parte:
- /api/health, /api/nasa/status, /api/indicators, /api/map/layers   → reales (Alvin: NASA POWER + GIBS, en caché)
- /api/field-node/*                                                → real y simple (SQLite), contrato v1
- /api/context/cuatro-canadas, /api/sources                        → información documentada del plan
- /api/rotations/compare                                           → motor real (Matías): reglas + MCDA + bootstrap;
                                                                     el agua queda insuficiente hasta integrar la ET0
"""
import importlib.util
import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware

from app.routers import context, field_node, monitor, nasa, rotations


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Precalienta la climatología del motor: la primera comparación responde en menos de 3 s.
    try:
        rotations.warm_up()
    except Exception:  # noqa: BLE001 — si falla, el endpoint lo informará con un 500 claro
        logging.getLogger(__name__).exception("No se pudo precalentar el motor")
    yield


app = FastAPI(
    lifespan=lifespan,
    title="FarmealoTu API",
    version="0.3.0",
    description="Apoyo a la decisión de rotación de cultivos en Cuatro Cañadas con datos NASA. "
                "Apoya, no reemplaza, el criterio del agricultor y del agrónomo. "
                "NASA provee datos; FarmealoTu los interpreta con su propio motor.",
)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["GET", "POST"], allow_headers=["*"])
app.add_middleware(GZipMiddleware, minimum_size=1000)

app.include_router(nasa.router)
app.include_router(field_node.router)
app.include_router(context.router)
app.include_router(rotations.router)       # motor de recomendación (Matías)
app.include_router(monitor.router)         # /monitor: página de desarrollo para ver la API y el nodo en vivo


@app.get("/api/health", tags=["sistema"])
def health():
    # «ready» cuando existe la ET0 FAO-56 (analytics/physics.py); sin ella S_agua queda insuficiente.
    engine = "ready" if importlib.util.find_spec("analytics.physics") else "partial"
    return {"status": "ok", "version": app.version, "engine": engine,
            "time": datetime.now(timezone.utc).replace(microsecond=0).isoformat()}
