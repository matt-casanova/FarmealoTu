"""POST /api/rotations/compare — RESPUESTA PROVISIONAL HONESTA (esqueleto de Alvin).

El motor de recomendación es de Matías y todavía no existe. Para no bloquear a la app:
- se valida el request con el esquema real (CompareRequest);
- se devuelve SOLO la rotación actual (E0) con evidencia NASA POWER REAL del período reciente;
- NO se inventan estrategias E1–E3, puntajes ni ranking: `score` y `criteria` = null,
  `recommended_strategy_id` = null (la app muestra «Información insuficiente para ordenar opciones»).

Cuando Matías tenga engine/recommendation.py, este router se reemplaza por el suyo.
"""
from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.routers.nasa import _indicators_cached, MAX_DISTANCE_DEG
from app.schemas import CompareRequest
from pipeline import power as P
from pipeline.status import build_nasa_status

import time

router = APIRouter(prefix="/api/rotations", tags=["rotaciones"])

CROP_ES = {"soy": "soya", "maize": "maíz", "sorghum": "sorgo", "sunflower": "girasol", "wheat": "trigo",
           "fallow": "barbecho", "chia": "chía", "cover": "cobertura"}
ENGINE_PENDING = ("MOTOR PENDIENTE: el motor de recomendación (Matías) todavía no está integrado. Esta respuesta "
                  "solo describe tu rotación actual con datos NASA; no compara ni ordena opciones.")


@router.post("/compare", summary="(PROVISIONAL) Describe la rotación actual con evidencia NASA; motor pendiente")
def compare(req: CompareRequest):
    if abs(req.site.lat - P.PILOT_LAT) > MAX_DISTANCE_DEG or abs(req.site.lon - P.PILOT_LON) > MAX_DISTANCE_DEG:
        raise HTTPException(status_code=422, detail=[{
            "loc": ["body", "site"], "type": "value_error",
            "msg": f"Fuera del área piloto: solo hay datos para ±{MAX_DISTANCE_DEG}° del punto de referencia "
                   f"de Cuatro Cañadas ({P.PILOT_LAT}, {P.PILOT_LON})."}])
    ind = _indicators_cached("recent", None, int(time.time() // 600))
    status = build_nasa_status()
    evidence = [{
        "indicator": i["id"], "name": i["name"], "value": i["value"], "unit": i["unit"],
        "source": i.get("source") or "NASA POWER", "period": i.get("period"), "scale": i.get("scale"),
        "evidence_status": i["evidence_status"],
    } for i in ind["indicators"]]
    seq_text = " → ".join(CROP_ES.get(c, c) for c in req.current_rotation)
    e0 = {
        "id": "E0", "name": "Tu rotación actual", "sequence": req.current_rotation,
        "score": None, "partial_score": True,
        "criteria": {"water": None, "soil": None, "risk": None},
        "uncertainty": None,
        "tradeoffs": None,
        "reasons": [f"Es la rotación que nos contaste ({seq_text}). Se describe con datos NASA; todavía no se compara."],
        "evidence": evidence,
        "missing_information": [
            "Motor de recomendación pendiente: sin puntajes de agua, suelo ni riesgo",
            "Sin análisis de suelo de tu campo",
            "ET0 (FAO-56) pendiente de integrar",
        ],
        "agronomist_questions": [
            "¿Qué cultivo de invierno se adapta mejor a mi disponibilidad de agua este año?",
            "¿Conviene dejar más rastrojo o una cobertura en el período en que el suelo queda desnudo?",
        ],
    }
    return {
        "site": {"lat": req.site.lat, "lon": req.site.lon, "label": ind["site"]["label"]},
        "data_status": {"last_observation_date": status["last_observation_date"],
                        "last_successful_download": status["last_download"], "source": "NASA POWER (caché)",
                        "engine": "pending", "weights_received": req.priorities.normalized()},
        "alerts": [{"message": ENGINE_PENDING, "level": "high", "date": ind["last_observation_date"],
                    "source": "FarmealoTu", "evidence_status": "unavailable"}],
        "strategies": [e0],
        "recommended_strategy_id": None,
        "limitations": [ENGINE_PENDING] + ind["limitations"],
    }
