"""POST /api/rotations/compare — motor de recomendación real (Matías).

Reemplaza a rotations_stub.py (que queda en el repo). Lee la caché NASA POWER: ninguna petición llama a NASA.
Si el motor falla, responde 500 con un detalle en español; nunca cae a datos de ejemplo.
"""
from __future__ import annotations

import copy
import logging
import threading
from functools import lru_cache

from fastapi import APIRouter, HTTPException

from analytics import indicators as I
from app.routers.nasa import MAX_DISTANCE_DEG, _df
from app.schemas import CompareRequest
from engine.recommendation import Climate, build_climate
from engine.reference import ReferenceDataError, load_reference
from engine.response import build_compare_response
from pipeline import power as P
from pipeline.status import build_nasa_status

router = APIRouter(prefix="/api/rotations", tags=["rotaciones"])
log = logging.getLogger(__name__)


def data_signature(df) -> tuple:
    """Identidad de la caché NASA cargada: filas, última fecha y fecha de descarga. Si no cambia, la
    climatología y los indicadores tampoco: no hace falta recalcularlos aunque nasa._df() se recargue."""
    power = df.attrs.get("power", {})
    parts = power.get("parts", {})
    downloaded = max((c.get("downloaded_at") or "") for c in parts.values()) if parts else None
    return len(df), str(df.index.max()), power.get("last_valid_date"), downloaded


_DATA: dict[tuple, tuple] = {}   # firma → (df, climatología, indicadores recientes); cambia solo si cambia la caché
_DATA_LOCK = threading.Lock()


def _data():
    df = _df()
    sig = data_signature(df)
    if sig not in _DATA:
        with _DATA_LOCK:         # el endpoint corre en varios hilos: se construye una sola vez
            if sig not in _DATA:
                _DATA[sig] = (df, build_climate(df), I.build_indicators_response("recent", None, df=df))
    return sig, _DATA[sig]


def climate() -> Climate:
    return _data()[1][1]


@lru_cache(maxsize=256)
def _cached_response(req_json: str, sig: tuple) -> dict:
    """Respuesta cacheada por hash del request (JSON canónico) y por la firma de los datos.
    Los errores no se cachean (lru_cache no guarda excepciones)."""
    req = CompareRequest.model_validate_json(req_json)
    load_reference()
    df, clim, ind = _DATA[sig]
    return build_compare_response(req, df, ind, build_nasa_status(), clim)


def warm_up() -> None:
    """Precalcula la climatología y una comparación con el request por defecto (se llama al arrancar)."""
    sig, _ = _data()
    _cached_response(CompareRequest(current_rotation=["soy", "sorghum"]).model_dump_json(), sig)


@router.post("/compare", summary="Compara tu rotación con alternativas (agua, suelo y riesgo) con datos NASA POWER")
def compare(req: CompareRequest):
    if abs(req.site.lat - P.PILOT_LAT) > MAX_DISTANCE_DEG or abs(req.site.lon - P.PILOT_LON) > MAX_DISTANCE_DEG:
        raise HTTPException(status_code=422, detail=[{
            "loc": ["body", "site"], "type": "value_error",
            "msg": f"Fuera del área piloto: solo hay datos para ±{MAX_DISTANCE_DEG}° del punto de referencia "
                   f"de Cuatro Cañadas ({P.PILOT_LAT}, {P.PILOT_LON})."}])
    try:
        sig, _ = _data()
        return copy.deepcopy(_cached_response(req.model_dump_json(), sig))
    except ReferenceDataError as exc:
        log.exception("Base de referencia inválida")
        raise HTTPException(status_code=500, detail=f"La base agrícola de referencia tiene errores: {exc}") from exc
    except Exception as exc:  # noqa: BLE001 — se informa con claridad; no se inventa una respuesta
        log.exception("Fallo del motor de recomendación")
        raise HTTPException(status_code=500, detail=f"El motor de recomendación falló al calcular la comparación "
                                                    f"({type(exc).__name__}: {exc}). No se muestran datos de "
                                                    f"ejemplo en su lugar.") from exc
