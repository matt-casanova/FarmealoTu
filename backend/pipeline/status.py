"""Estado de las fuentes NASA (GET /api/nasa/status) y contexto POWER para «Datos de terreno».

Todo se lee de la caché local: ninguna función de este módulo llama a NASA.
"""
from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from pipeline import power as P

FORCING_META = Path(__file__).resolve().parents[2] / "hardware" / "simulation" / "forcing" / "forcing_metadata.json"
REGIONAL = "contexto regional (~50 km)"


def _now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def _power_daily_entry(caches: list[dict]) -> dict:
    core = sorted([c for c in caches if c.get("kind") == "daily" and c.get("parameters") == P.CORE_DAILY_PARAMS],
                  key=lambda c: c.get("start", ""))
    if not core:
        return {"id": "power_daily", "name": "NASA POWER · diario", "status": "unavailable",
                "reason": "sin descargas en caché: ejecutar `python -m pipeline.power download`"}
    last_obs = max((c.get("last_valid_date") or "") for c in core) or None
    return {
        "id": "power_daily", "name": "NASA POWER · diario", "status": "available",
        "product_version": next((c.get("api_version") for c in reversed(core) if c.get("api_version")), None),
        "last_successful_download": max(c["downloaded_at"] for c in core),
        "last_observation_date": last_obs,
        "coverage": {"start": f"{core[0]['start'][:4]}-{core[0]['start'][4:6]}-{core[0]['start'][6:]}",
                     "end": f"{core[-1]['end'][:4]}-{core[-1]['end'][4:6]}-{core[-1]['end'][6:]}"},
        "from_cache": True, "scale": REGIONAL, "parameters": P.CORE_DAILY_PARAMS,
        "evidence_status": "documented",
        "notes": ["POWER publica con unos días de atraso",
                  "last_observation_date = último día con TODAS las variables (la radiación llega más tarde)"],
        "cache_files": [c["file"] for c in core],
    }


def _gwetroot_entry(caches: list[dict]) -> dict:
    g = [c for c in caches if c.get("kind") == "daily" and c.get("parameters") == ["GWETROOT"]]
    if not g:
        return {"id": "power_gwetroot", "name": "NASA POWER · humedad de la zona radicular (GWETROOT)",
                "status": "unavailable", "reason": "no descargado"}
    last = max(g, key=lambda c: c.get("last_valid_date") or "")
    return {"id": "power_gwetroot", "name": "NASA POWER · humedad de la zona radicular (GWETROOT)",
            "status": "available", "last_successful_download": last["downloaded_at"],
            "last_observation_date": last.get("last_valid_date"), "from_cache": True, "scale": REGIONAL,
            "evidence_status": "documented",
            "notes": ["Humedad relativa modelada (0–1): solo contexto regional, no un sensor en la finca"]}


def _hourly_entry(caches: list[dict]) -> dict:
    h = [c for c in caches if c.get("kind") == "hourly"]
    if not h:
        return {"id": "power_hourly", "name": "NASA POWER · horario (forzante del gemelo digital)",
                "status": "unavailable", "reason": "no descargado"}
    entry = {"id": "power_hourly", "name": "NASA POWER · horario (forzante del gemelo digital)",
             "status": "available", "last_successful_download": max(c["downloaded_at"] for c in h),
             "from_cache": True, "scale": REGIONAL, "evidence_status": "documented", "notes": []}
    if FORCING_META.exists():
        meta = json.loads(FORCING_META.read_text(encoding="utf-8"))
        scen = meta.get("scenarios", {})
        entry["scenarios"] = {k: {"start": v.get("start"), "end": v.get("end"), "rows": v.get("rows"),
                                  "hourly_radiation_hours": v.get("hourly_radiation_hours")} for k, v in scen.items()}
        if any(v.get("hourly_radiation_hours", 1) < v.get("rows", 0) for v in scen.values()):
            entry["status"] = "partial"
            entry["notes"].append("Radiación horaria aún no publicada para el período más reciente (S1)")
    return entry


def _gibs_entry() -> dict:
    from pipeline import gibs as G
    v = G.load_verified()
    base = {"id": "gibs", "name": "NASA GIBS (teselas del visor)"}
    if not v:
        return {**base, "status": "unavailable", "reason": "capas no verificadas (python -m pipeline.gibs verify)"}
    ok = [L for L in v["layers"] if L.get("verified")]
    return {**base, "status": "available" if ok else "unavailable", "last_successful_download": v["verified_at"],
            "verified_layers": [{"id": L["id"], "gibs_layer": L["gibs_id"], "date": L.get("date")} for L in ok],
            "from_cache": True, "evidence_status": "documented",
            "notes": ["Identificadores verificados en GetCapabilities EPSG:3857 + tesela de prueba sobre Cuatro Cañadas"]}


def build_nasa_status() -> dict:
    caches = [c for c in P.list_cache() if "error" not in c]
    daily = _power_daily_entry(caches)
    return {
        "generated_at": _now(),
        "reference_date": P.RECENT_END,
        "last_observation_date": daily.get("last_observation_date"),
        "last_download": daily.get("last_successful_download"),
        "sources": [
            daily,
            _gwetroot_entry(caches),
            _hourly_entry(caches),
            _gibs_entry(),
            {"id": "smap_l4", "name": "SMAP L4 SPL4SMGP v8", "status": "unavailable",
             "reason": "P2: sin archivos descargados (requiere Earthdata Login)"},
            {"id": "gpm_imerg", "name": "GPM IMERG", "status": "unavailable",
             "reason": "P2: la lluvia se toma de PRECTOTCORR de NASA POWER"},
            {"id": "ndvi", "name": "NDVI MODIS/VIIRS", "status": "unavailable", "reason": "sin serie descargada"},
        ],
    }


def power_context(day: str | None = None) -> dict:
    """T2M y RH2M de POWER para un día (o el último con datos), para la banda «Dato regional» de
    «Datos de terreno». Forma pedida por la app: {date, t2m_c, rh2m_pct, source, scale, evidence_status}."""
    data = P.load_pilot_daily()["data"]
    rows = [r for r in data["records"] if r["T2M"] is not None and r["RH2M"] is not None]
    if day:
        rows = [r for r in rows if r["date"] <= day[:10]]
    if not rows:
        return {"date": day, "t2m_c": None, "rh2m_pct": None, "source": "NASA POWER",
                "evidence_status": "unavailable", "reason": "sin dato POWER para esa fecha"}
    r = rows[-1]
    out = {"date": r["date"], "t2m_c": r["T2M"], "rh2m_pct": r["RH2M"], "source": "NASA POWER (diario, media)",
           "scale": REGIONAL, "evidence_status": "documented",
           "label": "Dato regional (~50 km): contexto, no tu campo"}
    if day and r["date"] != day[:10]:
        out["note"] = f"POWER aún no publica {day[:10]}; se muestra el último día disponible"
    return out
