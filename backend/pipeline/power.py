"""Cliente NASA POWER (diario y horario) con validación, caché y respaldo.

Reglas de RotaSat / FarmealoTu (ver plan, secciones 0, 3.3 y 3.4):
- El navegador / app Android nunca llama a NASA: solo este backend.
- −999 (fill_value de POWER) → None. Nunca se promedia.
- Se conservan unidades, versión de la API, fuentes, estándar de tiempo y elevación.
- La caché guarda la respuesta CRUDA de POWER + metadatos de descarga, así el JSON
  recibido siempre se puede inspeccionar (prueba A).
- Si NASA no responde, se reutiliza la última descarga válida y se avisa con su fecha.

Uso por línea de comandos (desde backend/):
    python -m pipeline.power download      # histórico + reciente + prueba GWETROOT
"""
from __future__ import annotations

import hashlib
import json
import time
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any, Iterable

import requests

DAILY_URL = "https://power.larc.nasa.gov/api/temporal/daily/point"
HOURLY_URL = "https://power.larc.nasa.gov/api/temporal/hourly/point"
DOCS_URL = "https://power.larc.nasa.gov/docs/services/api/temporal/daily/"

# Punto de referencia de Cuatro Cañadas (no es una finca real ni el centroide oficial).
PILOT_LAT = -17.2374
PILOT_LON = -62.2427

CORE_DAILY_PARAMS = ["T2M", "T2M_MAX", "T2M_MIN", "PRECTOTCORR", "RH2M", "WS2M", "ALLSKY_SFC_SW_DWN"]
OPTIONAL_DAILY_PARAMS = ["GWETROOT"]  # se prueba aparte: si no viene, queda "unavailable"

HIST_START, HIST_END = "2015-01-01", "2025-12-31"
RECENT_START, RECENT_END = "2026-01-01", "2026-11-14"  # fecha de referencia del proyecto

CACHE_DIR = Path(__file__).resolve().parents[1] / "data" / "cache"
SCALE_LABEL = "contexto regional (~50 km, celda POWER ~0.5° × 0.625°)"
RETRY_STATUS = {429, 500, 502, 503, 504}


class PowerError(RuntimeError):
    """Error de NASA POWER sin caché de respaldo disponible."""


# --------------------------------------------------------------------------- utilidades
def _yyyymmdd(d: str | date) -> str:
    if isinstance(d, date):
        return d.strftime("%Y%m%d")
    s = str(d).replace("-", "")
    datetime.strptime(s, "%Y%m%d")  # valida
    return s


def _iso_from_key(key: str) -> str:
    """'20250101' → '2025-01-01'; '2025010113' → '2025-01-01T13:00'."""
    if len(key) == 8:
        return f"{key[:4]}-{key[4:6]}-{key[6:8]}"
    if len(key) == 10:
        return f"{key[:4]}-{key[4:6]}-{key[6:8]}T{key[8:10]}:00"
    raise ValueError(f"Clave de fecha POWER inesperada: {key!r}")


def _now_utc() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def _cache_path(kind: str, lat: float, lon: float, start: str, end: str, params: list[str]) -> Path:
    ph = hashlib.sha1(",".join(sorted(params)).encode()).hexdigest()[:8]
    name = f"power_{kind}_{lat:.4f}_{lon:.4f}_{start}_{end}_{ph}.json"
    return CACHE_DIR / name


# --------------------------------------------------------------------------- red
def _request(url: str, params: dict[str, Any], timeout: float = 60, retries: int = 3,
             backoff: float = 2.0, session: requests.Session | None = None) -> dict:
    """GET con reintentos limitados para errores temporales. Valida HTTP y estructura."""
    http = session or requests
    last_exc: Exception | None = None
    for attempt in range(1, retries + 1):
        try:
            resp = http.get(url, params=params, timeout=timeout)
            if resp.status_code in RETRY_STATUS:
                raise requests.HTTPError(f"HTTP {resp.status_code} (temporal)", response=resp)
            if resp.status_code != 200:
                # 4xx no temporales: no se reintenta
                raise PowerError(f"NASA POWER respondió HTTP {resp.status_code}: {resp.text[:300]}")
            raw = resp.json()
            validate_raw(raw)
            return raw
        except PowerError:
            raise
        except (requests.RequestException, ValueError) as exc:
            last_exc = exc
            if attempt < retries:
                time.sleep(backoff ** attempt)
    raise PowerError(f"NASA POWER no respondió tras {retries} intentos: {last_exc}")


def validate_raw(raw: Any) -> None:
    """Comprueba la estructura mínima del JSON de POWER."""
    if not isinstance(raw, dict):
        raise ValueError("Respuesta POWER no es un objeto JSON")
    props = raw.get("properties")
    if not isinstance(props, dict) or not isinstance(props.get("parameter"), dict):
        raise ValueError("Respuesta POWER sin properties.parameter")
    if not props["parameter"]:
        raise ValueError("properties.parameter vacío")


# --------------------------------------------------------------------------- parseo
def parse_power_json(raw: dict) -> dict:
    """Normaliza la respuesta cruda: fechas ISO, variables alineadas, −999 → None."""
    validate_raw(raw)
    header = raw.get("header", {}) or {}
    fill = header.get("fill_value", -999.0)
    param_block: dict[str, dict[str, float]] = raw["properties"]["parameter"]
    meta_params = raw.get("parameters", {}) or {}
    coords = (raw.get("geometry") or {}).get("coordinates") or [None, None, None]

    keys = sorted({k for series in param_block.values() for k in series})
    records: list[dict[str, Any]] = []
    missing = {p: 0 for p in param_block}
    for k in keys:
        row: dict[str, Any] = {"date": _iso_from_key(k)}
        for p, series in param_block.items():
            v = series.get(k)
            if v is None or v == fill or (isinstance(v, (int, float)) and v <= -998.9):
                row[p] = None
                missing[p] += 1
            else:
                row[p] = float(v)
        records.append(row)

    return {
        "source": "NASA POWER",
        "api": header.get("api", {}),
        "title": header.get("title"),
        "upstream_sources": header.get("sources", []),
        "time_standard": header.get("time_standard"),
        "fill_value": fill,
        "location": {
            "lon": coords[0],
            "lat": coords[1],
            "elevation_m": coords[2] if len(coords) > 2 else None,
        },
        "units": {p: (meta_params.get(p) or {}).get("units") for p in param_block},
        "longnames": {p: (meta_params.get(p) or {}).get("longname") for p in param_block},
        "parameters": list(param_block.keys()),
        "records": records,
        "n_records": len(records),
        "missing_count": missing,
        "messages": raw.get("messages", []),
        "scale": SCALE_LABEL,
        "evidence_status": "documented",
    }


def last_valid_date(data: dict, params: Iterable[str] | None = None) -> str | None:
    """Última fecha en la que TODAS las variables pedidas tienen valor (POWER suele atrasarse días)."""
    ps = list(params or data["parameters"])
    for row in reversed(data["records"]):
        if all(row.get(p) is not None for p in ps):
            return row["date"]
    return None


# --------------------------------------------------------------------------- caché
def _write_cache(path: Path, raw: dict, meta: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps({"_rotasat_meta": meta, "response": raw}, ensure_ascii=False), encoding="utf-8")
    tmp.replace(path)


def _read_cache(path: Path) -> tuple[dict, dict]:
    blob = json.loads(path.read_text(encoding="utf-8"))
    return blob["response"], blob["_rotasat_meta"]


def _fallback_cache(kind: str, lat: float, lon: float, params: list[str]) -> Path | None:
    """Última descarga válida del mismo tipo/punto/parámetros (aunque cubra otro rango)."""
    ph = hashlib.sha1(",".join(sorted(params)).encode()).hexdigest()[:8]
    cands = sorted(CACHE_DIR.glob(f"power_{kind}_{lat:.4f}_{lon:.4f}_*_{ph}.json"),
                   key=lambda p: p.stat().st_mtime, reverse=True)
    return cands[0] if cands else None


def list_cache() -> list[dict]:
    """Metadatos de todas las descargas en caché (para /api/nasa/status)."""
    out = []
    for p in sorted(CACHE_DIR.glob("power_*.json")):
        try:
            _, meta = _read_cache(p)
            out.append({"file": p.name, **meta})
        except (OSError, ValueError, KeyError):
            out.append({"file": p.name, "error": "caché ilegible"})
    return out


# --------------------------------------------------------------------------- API pública
def _fetch(kind: str, url: str, lat: float, lon: float, start: str | date, end: str | date,
           parameters: list[str], community: str, refresh: bool, timeout: float,
           retries: int, session: requests.Session | None) -> dict:
    s, e = _yyyymmdd(start), _yyyymmdd(end)
    params_list = list(parameters)
    path = _cache_path(kind, lat, lon, s, e, params_list)
    query = {"parameters": ",".join(params_list), "community": community,
             "longitude": lon, "latitude": lat, "start": s, "end": e, "format": "JSON"}

    if path.exists() and not refresh:
        raw, meta = _read_cache(path)
        return {"data": parse_power_json(raw),
                "cache": {"used": True, "file": path.name, "downloaded_at": meta.get("downloaded_at"),
                          "stale_fallback": False, "warning": None},
                "request": meta.get("request")}

    try:
        raw = _request(url, query, timeout=timeout, retries=retries, session=session)
    except PowerError as exc:
        fb = path if path.exists() else _fallback_cache(kind, lat, lon, params_list)
        if fb is None:
            raise
        raw, meta = _read_cache(fb)
        return {"data": parse_power_json(raw),
                "cache": {"used": True, "file": fb.name, "downloaded_at": meta.get("downloaded_at"),
                          "stale_fallback": True,
                          "warning": f"NASA POWER no respondió ({exc}). Se usa la descarga del "
                                     f"{meta.get('downloaded_at')} ({fb.name})."},
                "request": meta.get("request")}

    meta = {"downloaded_at": _now_utc(), "request": {"url": url, "query": query},
            "kind": kind, "lat": lat, "lon": lon, "start": s, "end": e, "parameters": params_list,
            "docs": DOCS_URL}
    data = parse_power_json(raw)
    meta["last_valid_date"] = last_valid_date(data)
    meta["api_version"] = (data.get("api") or {}).get("version")
    _write_cache(path, raw, meta)
    return {"data": data,
            "cache": {"used": False, "file": path.name, "downloaded_at": meta["downloaded_at"],
                      "stale_fallback": False, "warning": None},
            "request": meta["request"]}


def fetch_power_daily(lat: float, lon: float, start: str | date, end: str | date,
                      parameters: list[str] | None = None, community: str = "AG",
                      refresh: bool = False, timeout: float = 60, retries: int = 3,
                      session: requests.Session | None = None) -> dict:
    """Serie diaria de NASA POWER para un punto, normalizada y en caché.

    Devuelve {"data": {...records, units, location...}, "cache": {...}, "request": {...}}.
    """
    return _fetch("daily", DAILY_URL, lat, lon, start, end, parameters or CORE_DAILY_PARAMS,
                  community, refresh, timeout, retries, session)


def fetch_power_hourly(lat: float, lon: float, start: str | date, end: str | date,
                       parameters: list[str], community: str = "AG", refresh: bool = False,
                       timeout: float = 120, retries: int = 3,
                       session: requests.Session | None = None) -> dict:
    """Serie horaria de NASA POWER (para el forzante del gemelo digital)."""
    return _fetch("hourly", HOURLY_URL, lat, lon, start, end, parameters,
                  community, refresh, timeout, retries, session)


def probe_optional_daily(lat: float, lon: float, start: str, end: str,
                         param: str = "GWETROOT", refresh: bool = False) -> dict:
    """Prueba un parámetro opcional sin romper nada si no está disponible."""
    try:
        res = fetch_power_daily(lat, lon, start, end, parameters=[param], refresh=refresh)
    except PowerError as exc:
        return {"parameter": param, "available": False, "evidence_status": "unavailable",
                "reason": str(exc)}
    d = res["data"]
    n_valid = d["n_records"] - d["missing_count"].get(param, d["n_records"])
    if param not in d["parameters"] or n_valid == 0:
        return {"parameter": param, "available": False, "evidence_status": "unavailable",
                "reason": "POWER no devolvió valores válidos para este parámetro"}
    return {"parameter": param, "available": True, "evidence_status": "documented",
            "n_valid": n_valid, "units": d["units"].get(param), "cache_file": res["cache"]["file"],
            "last_valid_date": last_valid_date(d, [param])}


def load_pilot_daily(refresh: bool = False) -> dict:
    """Histórico + reciente del punto piloto, concatenados (lo que usan los indicadores)."""
    hist = fetch_power_daily(PILOT_LAT, PILOT_LON, HIST_START, HIST_END, refresh=refresh)
    rec = fetch_power_daily(PILOT_LAT, PILOT_LON, RECENT_START, RECENT_END, refresh=refresh)
    data = dict(hist["data"])
    data["records"] = hist["data"]["records"] + rec["data"]["records"]
    data["n_records"] = len(data["records"])
    data["missing_count"] = {p: hist["data"]["missing_count"].get(p, 0) + rec["data"]["missing_count"].get(p, 0)
                             for p in data["parameters"]}
    return {"data": data, "parts": {"historical": hist["cache"], "recent": rec["cache"]},
            "last_valid_date": last_valid_date(rec["data"]),
            "warnings": [w for w in (hist["cache"]["warning"], rec["cache"]["warning"]) if w]}


# --------------------------------------------------------------------------- CLI
def _cli_download(refresh: bool) -> None:
    print(f"Punto piloto: lat {PILOT_LAT}, lon {PILOT_LON}")
    for label, s, e in (("histórico", HIST_START, HIST_END), ("reciente", RECENT_START, RECENT_END)):
        res = fetch_power_daily(PILOT_LAT, PILOT_LON, s, e, refresh=refresh)
        d = res["data"]
        print(f"- {label} {s}→{e}: {d['n_records']} días · faltantes {d['missing_count']} · "
              f"última fecha válida {last_valid_date(d)} · elevación {d['location']['elevation_m']} m · "
              f"caché {'(ya existía)' if res['cache']['used'] else '(nueva)'} {res['cache']['file']}")
        if res["cache"]["warning"]:
            print("  ⚠", res["cache"]["warning"])
    print("- GWETROOT:", json.dumps(probe_optional_daily(PILOT_LAT, PILOT_LON, HIST_START, HIST_END,
                                                         refresh=refresh), ensure_ascii=False))


if __name__ == "__main__":
    import sys
    if len(sys.argv) >= 2 and sys.argv[1] == "download":
        _cli_download(refresh="--refresh" in sys.argv)
    else:
        print(__doc__)
