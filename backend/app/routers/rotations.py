"""POST /api/rotations/compare — motor de recomendación real (Matías).

Reemplaza a rotations_stub.py (que queda en el repo). Lee la caché NASA POWER: ninguna petición llama a NASA.
Si el motor falla, responde 500 con un detalle en español; nunca cae a datos de ejemplo.
"""
from __future__ import annotations

import logging
import time
from functools import lru_cache

from fastapi import APIRouter, HTTPException

from app.routers.nasa import MAX_DISTANCE_DEG, _df, _indicators_cached
from app.schemas import CompareRequest
from engine.recommendation import Climate, build_climate
from engine.reference import ReferenceDataError, load_reference
from engine.response import build_compare_response
from pipeline import power as P
from pipeline.status import build_nasa_status

router = APIRouter(prefix="/api/rotations", tags=["rotaciones"])
log = logging.getLogger(__name__)


@lru_cache(maxsize=2)
def _climate_cached(_bucket: int) -> Climate:
    return build_climate(_df())


def climate() -> Climate:
    """Climatología por año agrícola, recalculada como máximo cada 10 min (igual que nasa._df)."""
    return _climate_cached(int(time.time() // 600))


@router.post("/compare", summary="Compara tu rotación con alternativas (agua, suelo y riesgo) con datos NASA POWER")
def compare(req: CompareRequest):
    if abs(req.site.lat - P.PILOT_LAT) > MAX_DISTANCE_DEG or abs(req.site.lon - P.PILOT_LON) > MAX_DISTANCE_DEG:
        raise HTTPException(status_code=422, detail=[{
            "loc": ["body", "site"], "type": "value_error",
            "msg": f"Fuera del área piloto: solo hay datos para ±{MAX_DISTANCE_DEG}° del punto de referencia "
                   f"de Cuatro Cañadas ({P.PILOT_LAT}, {P.PILOT_LON})."}])
    try:
        load_reference()
        ind = _indicators_cached("recent", None, int(time.time() // 600))
        return build_compare_response(req, _df(), ind, build_nasa_status(), climate())
    except ReferenceDataError as exc:
        log.exception("Base de referencia inválida")
        raise HTTPException(status_code=500, detail=f"La base agrícola de referencia tiene errores: {exc}") from exc
    except Exception as exc:  # noqa: BLE001 — se informa con claridad; no se inventa una respuesta
        log.exception("Fallo del motor de recomendación")
        raise HTTPException(status_code=500, detail=f"El motor de recomendación falló al calcular la comparación "
                                                    f"({type(exc).__name__}: {exc}). No se muestran datos de "
                                                    f"ejemplo en su lugar.") from exc
