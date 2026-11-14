"""Endpoints de datos NASA (Alvin). Matías los incluye en main.py:

    from app.routers import nasa
    app.include_router(nasa.router)

Reglas: todo se lee de la caché (ninguna petición llama a NASA); contrato en docs/contratos.md.
"""
from __future__ import annotations

import math
import time
from functools import lru_cache

from fastapi import APIRouter, HTTPException, Query

from analytics import indicators as I
from pipeline import gibs as G
from pipeline import power as P
from pipeline import status as S

router = APIRouter(prefix="/api", tags=["nasa"])

MAX_DISTANCE_DEG = 0.25  # ~27 km: orden del tamaño de una celda POWER


@lru_cache(maxsize=1)
def _daily_frame_cached(_bucket: int):
    return I.load_daily_frame()


def _df():
    # se recarga como máximo cada 10 min (por si se actualiza la caché sin reiniciar el servidor)
    return _daily_frame_cached(int(time.time() // 600))


@lru_cache(maxsize=64)
def _indicators_cached(period: str, heat: float | None, _bucket: int) -> dict:
    return I.build_indicators_response(period, heat, df=_df())


def _km(lat1, lon1, lat2, lon2) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


@router.get("/indicators", summary="Indicadores agroclimáticos NASA POWER (desde caché)")
def get_indicators(
    lat: float = Query(P.PILOT_LAT, ge=-90, le=90),
    lon: float = Query(P.PILOT_LON, ge=-180, le=180),
    period: str = Query("recent", description="recent · campaign:verano-2025-26 · YYYY-MM-DD/YYYY-MM-DD"),
    heat_threshold_c: float | None = Query(None, description="Umbral de calor de un cultivo CON fuente (°C)"),
):
    if abs(lat - P.PILOT_LAT) > MAX_DISTANCE_DEG or abs(lon - P.PILOT_LON) > MAX_DISTANCE_DEG:
        raise HTTPException(status_code=404, detail=(
            f"Solo hay datos NASA precalculados alrededor del punto de referencia de Cuatro Cañadas "
            f"({P.PILOT_LAT}, {P.PILOT_LON}, ±{MAX_DISTANCE_DEG}°). No se consulta NASA en vivo."))
    try:
        resp = _indicators_cached(period, heat_threshold_c, int(time.time() // 600))
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    resp = dict(resp)
    dist = round(_km(lat, lon, P.PILOT_LAT, P.PILOT_LON), 1)
    resp["site"] = {**resp["site"], "requested": {"lat": lat, "lon": lon}, "distance_km": dist}
    if dist > 1:
        resp["source"] = {**resp["source"], "warnings": resp["source"]["warnings"] + [
            f"Tu punto está a {dist} km del punto de referencia: se muestran los datos de la celda POWER "
            f"del punto de referencia (contexto regional)."]}
    return resp


@router.get("/nasa/status", summary="Disponibilidad y frescura de cada fuente NASA")
def get_nasa_status():
    return S.build_nasa_status()


@router.get("/map/layers", summary="Capas del visor (NASA GIBS verificadas + balance hídrico)")
def get_map_layers():
    return G.build_map_layers()
