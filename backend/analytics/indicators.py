"""Indicadores agroclimáticos derivados de NASA POWER (plan, sección 3.4; contrato en docs/contratos.md).

Todo valor sale con unidad, fuente, período, escala y evidence_status.
Regla: nunca se divide por cero, nunca se rellena un faltante, nunca se afirma daño a un cultivo
con un umbral genérico.

Funciones para el motor (Matías):
    load_daily_frame()            -> DataFrame diario (índice fecha, NaN = faltante)
    yearly_indicators(df)         -> 1 fila por AÑO AGRÍCOLA nov→oct (para el bootstrap)
    campaign_indicators(df)       -> 1 fila por campaña (verano nov→abr / invierno may→oct)
    monthly_inputs(df)            -> por mes: P, Pef, ET0 (si physics.py está disponible)
Para la API:
    build_indicators_response(period="recent", heat_threshold_c=None) -> dict (contrato)
"""
from __future__ import annotations

import math
from datetime import date
from typing import Any, Callable

import numpy as np
import pandas as pd

from pipeline import power as P

HIST_YEARS = list(range(2015, 2026))          # climatología de referencia 2015–2025
DRY_THRESHOLD_MM = 1.0                        # P < 1 mm/día = día seco (configurable)
WET_DAY_MM = 1.0                              # día húmedo para el P95
SOURCE_P = "NASA POWER PRECTOTCORR"
SOURCE_TMAX = "NASA POWER T2M_MAX"
CAMPAIGN_DEFINITION = {
    "verano": "1 nov → 30 abr", "invierno": "1 may → 31 oct", "evidence_status": "assumption",
    "note": "Ventanas aproximadas del calendario de Santa Cruz; el motor usa las de crops.json cuando tengan fuente",
}
LIMITATIONS = [
    "NASA POWER es una celda de ~50 km: contexto regional, no una medición de la finca.",
    "Los días de calor usan un umbral meteorológico general salvo que se indique un umbral de cultivo con fuente.",
    "La lluvia intensa es exposición meteorológica: no predice inundaciones fluviales.",
    "La lluvia efectiva usa un método empírico FAO (supuesto declarado).",
]


# =========================================================================== carga
def load_daily_frame() -> pd.DataFrame:
    """Serie diaria del punto piloto (histórico + reciente) desde la caché. NaN = faltante."""
    res = P.load_pilot_daily()
    df = pd.DataFrame(res["data"]["records"])
    df["date"] = pd.to_datetime(df["date"])
    df = df.set_index("date").sort_index()
    df = df[~df.index.duplicated(keep="last")].astype(float)
    df.attrs["power"] = {k: v for k, v in res.items() if k != "data"}
    df.attrs["meta"] = {k: res["data"][k] for k in ("api", "units", "location", "time_standard", "scale",
                                                     "upstream_sources")}
    return df


# =========================================================================== fórmulas puras
def max_dry_spell(precip: pd.Series | list, threshold: float = DRY_THRESHOLD_MM) -> int | None:
    """Máximo de días consecutivos con P < threshold. Un día faltante corta la racha (conservador).
    Devuelve None si no hay ningún dato válido."""
    s = pd.Series(precip, dtype=float)
    if s.notna().sum() == 0:
        return None
    best = run = 0
    for v in s:
        if not math.isnan(v) and v < threshold:
            run += 1
            best = max(best, run)
        else:
            run = 0
    return best


def anomaly_pct(actual: float | None, hist_mean: float | None) -> float | None:
    """100·(actual − media)/media. None si la media es 0 o falta (nunca se divide por cero)."""
    if actual is None or hist_mean is None or _isnan(actual) or _isnan(hist_mean) or hist_mean == 0:
        return None
    return round(100.0 * (actual - hist_mean) / hist_mean, 1)


def effective_rain_month(p_month_mm: float | None) -> float | None:
    """Lluvia efectiva mensual (FAO, Brouwer y Heibloem 1986): 0.8P−25 si P>75; 0.6P−10 si P≤75; mín 0."""
    if p_month_mm is None or _isnan(p_month_mm):
        return None
    pe = 0.8 * p_month_mm - 25 if p_month_mm > 75 else 0.6 * p_month_mm - 10
    return round(max(0.0, pe), 1)


def heat_thresholds_p90(df: pd.DataFrame) -> pd.Series:
    """P90 de Tmáx por mes calendario (1–12) en 2015–2025."""
    h = df[df.index.year.isin(HIST_YEARS)]["T2M_MAX"].dropna()
    return h.groupby(h.index.month).quantile(0.9)


def heavy_rain_thresholds_p95(df: pd.DataFrame) -> pd.Series:
    """P95 de la lluvia de los días húmedos (≥ 1 mm) por mes calendario en 2015–2025."""
    p = df[df.index.year.isin(HIST_YEARS)]["PRECTOTCORR"].dropna()
    wet = p[p >= WET_DAY_MM]
    return wet.groupby(wet.index.month).quantile(0.95)


def count_heat_days(tmax: pd.Series, thresholds: pd.Series | float) -> int | None:
    t = tmax.dropna()
    if t.empty:
        return None
    thr = thresholds if isinstance(thresholds, (int, float)) else t.index.month.map(thresholds).to_numpy(dtype=float)
    return int((t.to_numpy() > thr).sum())


def count_heavy_rain_days(precip: pd.Series, p95: pd.Series) -> int | None:
    p = precip.dropna()
    if p.empty:
        return None
    thr = p.index.month.map(p95).to_numpy(dtype=float)  # NaN si el mes no tiene días húmedos
    vals = p.to_numpy()
    with np.errstate(invalid="ignore"):
        return int(((vals >= WET_DAY_MM) & (vals > thr)).sum())


def effective_rain_period(precip: pd.Series) -> float | None:
    """Suma de la lluvia efectiva calculada sobre cada mes calendario del período."""
    p = precip.dropna()
    if p.empty:
        return None
    monthly = p.groupby([p.index.year, p.index.month]).sum()
    return round(sum(effective_rain_month(v) for v in monthly), 1)


# =========================================================================== ET0 (physics.py de Christian)
def et0_series(df: pd.DataFrame) -> tuple[pd.Series | None, str | None]:
    """ET0 diaria (mm/día) si analytics/physics.py existe; si no, (None, motivo)."""
    try:
        from analytics.physics import et0_fao56_daily  # type: ignore
    except ImportError:
        return None, "physics.py (ET0 FAO-56 de Christian) todavía no integrado"
    elev = (df.attrs.get("meta") or {}).get("location", {}).get("elevation_m")
    try:
        return et0_fao56_daily(df, elev), None
    except Exception as exc:  # noqa: BLE001 — se informa, no se rompe la API
        return None, f"ET0 no calculable: {exc}"


# =========================================================================== agregaciones
def _isnan(x: Any) -> bool:
    return isinstance(x, float) and math.isnan(x)


def _r(x: Any, nd: int = 1) -> Any:
    if x is None or _isnan(x):
        return None
    return round(float(x), nd)


def _window(df: pd.DataFrame, start: date | pd.Timestamp, end: date | pd.Timestamp) -> pd.DataFrame:
    return df.loc[pd.Timestamp(start):pd.Timestamp(end)]


def _shift_year(d: pd.Timestamp, years: int) -> pd.Timestamp:
    try:
        return d.replace(year=d.year + years)
    except ValueError:  # 29-feb → 28-feb
        return d.replace(year=d.year + years, day=28)


def same_dates_history(df: pd.DataFrame, start, end, fn: Callable[[pd.DataFrame], Any]) -> list[float]:
    """Aplica fn a la misma ventana calendario en cada año 2015–2025 (solo ventanas completas)."""
    s, e = pd.Timestamp(start), pd.Timestamp(end)
    out = []
    for y in HIST_YEARS:
        off = y - s.year
        ws, we = _shift_year(s, off), _shift_year(e, off)
        if ws < df.index.min() or we > pd.Timestamp(f"{HIST_YEARS[-1]}-12-31"):
            continue
        w = _window(df, ws, we)
        if len(w) and w["PRECTOTCORR"].notna().all():
            v = fn(w)
            if v is not None:
                out.append(float(v))
    return out


def _block_stats(w: pd.DataFrame, p90: pd.Series, p95: pd.Series, heat_thr: float | None = None,
                 et0: pd.Series | None = None) -> dict:
    p = w["PRECTOTCORR"]
    rain = p.sum(min_count=1)
    d = {
        "rain_mm": _r(rain),
        "dry_spell_max_days": max_dry_spell(p),
        "heat_days": count_heat_days(w["T2M_MAX"], heat_thr if heat_thr is not None else p90),
        "heavy_rain_days": count_heavy_rain_days(p, p95),
        "effective_rain_mm": effective_rain_period(p),
        "et0_mm": None,
        "valid_days": int(w[["PRECTOTCORR", "T2M_MAX"]].notna().all(axis=1).sum()),
        "n_days": int(len(w)),
    }
    if et0 is not None:
        e = et0.reindex(w.index)
        d["et0_mm"] = _r(e.sum(min_count=1)) if e.notna().all() else None
    return d


def monthly_grid(df: pd.DataFrame, et0: pd.Series | None = None) -> dict:
    """Grilla meses × años para el mapa de calor (módulo Riesgos)."""
    p90, p95 = heat_thresholds_p90(df), heavy_rain_thresholds_p95(df)
    hist = df[df.index.year.isin(HIST_YEARS)]["PRECTOTCORR"]
    full_months = hist.groupby([hist.index.year, hist.index.month]).agg(["sum", "count"])
    clim = {}
    for m in range(1, 13):
        vals = [r["sum"] for (y, mm), r in full_months.iterrows()
                if mm == m and r["count"] == pd.Period(f"{y}-{mm:02d}").days_in_month]
        clim[m] = float(np.mean(vals)) if vals else None

    last_valid = df.dropna(subset=["PRECTOTCORR", "T2M_MAX"]).index.max()
    cells = []
    for (y, m), w in df.groupby([df.index.year, df.index.month]):
        if pd.Timestamp(year=y, month=m, day=1) > last_valid:
            continue
        st = _block_stats(w, p90, p95, et0=et0)
        complete = st["valid_days"] == pd.Period(f"{y}-{m:02d}").days_in_month
        st["rain_anomaly_pct"] = anomaly_pct(st["rain_mm"], clim[m]) if complete else None
        cells.append({"year": int(y), "month": int(m), **{k: v for k, v in st.items() if k != "n_days"},
                      "complete": bool(complete)})
    years = sorted({c["year"] for c in cells})
    return {
        "years": years, "months": list(range(1, 13)),
        "fields": {"rain_mm": "mm", "rain_anomaly_pct": "%", "dry_spell_max_days": "días", "heat_days": "días",
                   "heavy_rain_days": "días", "effective_rain_mm": "mm", "et0_mm": "mm"},
        "notes": {"rain_anomaly_pct": "vs. media 2015–2025 del mismo mes; null si el mes está incompleto",
                  "dry_spell_max_days": "racha dentro del mes (se reinicia cada mes)",
                  "heat_days": "Tmáx > P90 histórico del mes (indicador meteorológico general)"},
        "cells": cells,
    }


def _campaign_windows(df: pd.DataFrame) -> list[tuple[str, str, pd.Timestamp, pd.Timestamp]]:
    out = []
    for y in range(df.index.min().year - 1, df.index.max().year + 1):
        out.append((f"invierno-{y}", f"Invierno {y}", pd.Timestamp(f"{y}-05-01"), pd.Timestamp(f"{y}-10-31")))
        out.append((f"verano-{y}-{str(y + 1)[2:]}", f"Verano {y}-{str(y + 1)[2:]}",
                    pd.Timestamp(f"{y}-11-01"), pd.Timestamp(f"{y + 1}-04-30")))
    return sorted(out, key=lambda t: t[2])


def campaign_indicators(df: pd.DataFrame, et0: pd.Series | None = None) -> pd.DataFrame:
    """Una fila por campaña con datos (verano nov→abr, invierno may→oct). `complete` si cubre todos los días."""
    p90, p95 = heat_thresholds_p90(df), heavy_rain_thresholds_p95(df)
    rows = []
    for cid, label, s, e in _campaign_windows(df):
        w = _window(df, s, e)
        if w.empty or w["PRECTOTCORR"].notna().sum() == 0:
            continue
        st = _block_stats(w, p90, p95, et0=et0)
        expected = (e - s).days + 1
        complete = st["valid_days"] == expected
        hist = same_dates_history(df, s, e, lambda x: x["PRECTOTCORR"].sum()) if complete else []
        rows.append({"id": cid, "label": label, "season": cid.split("-")[0], "start": s.date().isoformat(),
                     "end": e.date().isoformat(), **{k: v for k, v in st.items() if k != "n_days"},
                     "rain_anomaly_pct": anomaly_pct(st["rain_mm"], float(np.mean(hist)) if hist else None),
                     "complete": bool(complete)})
    return pd.DataFrame(rows)


def yearly_indicators(df: pd.DataFrame, et0: pd.Series | None = None) -> pd.DataFrame:
    """Una fila por año agrícola nov(Y)→oct(Y+1), solo años COMPLETOS. Base del bootstrap (Matías).
    Incluye columnas por campaña: verano_* e invierno_*."""
    camps = campaign_indicators(df, et0).set_index("id")
    p90, p95 = heat_thresholds_p90(df), heavy_rain_thresholds_p95(df)
    rows = []
    for y in range(df.index.min().year, df.index.max().year + 1):
        s, e = pd.Timestamp(f"{y}-11-01"), pd.Timestamp(f"{y + 1}-10-31")
        w = _window(df, s, e)
        if len(w) != (e - s).days + 1 or w[["PRECTOTCORR", "T2M_MAX"]].isna().any().any():
            continue
        row = {"agro_year": f"{y}-{str(y + 1)[2:]}", "start": s.date().isoformat(), "end": e.date().isoformat(),
               **{k: v for k, v in _block_stats(w, p90, p95, et0=et0).items() if k != "n_days"}}
        for season, cid in (("verano", f"verano-{y}-{str(y + 1)[2:]}"), ("invierno", f"invierno-{y + 1}")):
            if cid in camps.index:
                c = camps.loc[cid]
                for k in ("rain_mm", "dry_spell_max_days", "heat_days", "heavy_rain_days", "effective_rain_mm", "et0_mm"):
                    row[f"{season}_{k}"] = c[k]
        rows.append(row)
    return pd.DataFrame(rows)


def monthly_inputs(df: pd.DataFrame, et0: pd.Series | None = None) -> pd.DataFrame:
    """Insumos del balance hídrico por mes: P, Pef (supuesto FAO) y ET0. Matías aplica Kc → ETc."""
    g = monthly_grid(df, et0)
    return pd.DataFrame([{"year": c["year"], "month": c["month"], "rain_mm": c["rain_mm"],
                          "effective_rain_mm": c["effective_rain_mm"], "et0_mm": c["et0_mm"],
                          "complete": c["complete"]} for c in g["cells"]])


# =========================================================================== respuesta API
def _ind(id_, name, value, unit, status, source, method, **extra) -> dict:
    d = {"id": id_, "name": name, "value": value, "unit": unit, "evidence_status": status,
         "source": source, "method": method, "scale": P.SCALE_LABEL}
    if value is None and status != "unavailable":
        d["evidence_status"] = "unavailable"
        d.setdefault("reason", "sin datos válidos en el período")
    d.update({k: v for k, v in extra.items() if v is not None})
    return d


def resolve_period(df: pd.DataFrame, period: str) -> tuple[pd.Timestamp, pd.Timestamp, str, str]:
    last_valid = df.dropna(subset=["PRECTOTCORR", "T2M_MAX"]).index.max()
    if period in ("", "recent", None):
        s, e = pd.Timestamp(P.RECENT_START), last_valid
        return s, e, "recent", f"{s.date()} → {e.date()}"
    if period.startswith("campaign:"):
        cid = period.split(":", 1)[1]
        for c, label, s, e in _campaign_windows(df):
            if c == cid:
                return s, min(e, last_valid), period, label
        raise ValueError(f"Campaña desconocida: {cid} (ej.: verano-2025-26, invierno-2025)")
    if "/" in period:
        a, b = period.split("/", 1)
        s, e = pd.Timestamp(a), min(pd.Timestamp(b), last_valid)
        if s > e:
            raise ValueError("Período vacío o posterior a la última fecha con datos")
        return s, e, period, f"{s.date()} → {e.date()}"
    raise ValueError("period debe ser 'recent', 'campaign:<id>' o 'YYYY-MM-DD/YYYY-MM-DD'")


def period_indicators(df: pd.DataFrame, start, end, heat_threshold_c: float | None = None,
                      et0: pd.Series | None = None, et0_reason: str | None = None) -> list[dict]:
    w = _window(df, start, end)
    p90, p95 = heat_thresholds_p90(df), heavy_rain_thresholds_p95(df)
    st = _block_stats(w, p90, p95, heat_thr=heat_threshold_c, et0=et0)
    hist_rain = same_dates_history(df, start, end, lambda x: x["PRECTOTCORR"].sum())
    hist_dry = same_dates_history(df, start, end, lambda x: max_dry_spell(x["PRECTOTCORR"]))
    mean_rain = float(np.mean(hist_rain)) if hist_rain else None
    ref_label = f"media 2015–2025, mismas fechas (n={len(hist_rain)} años)"
    an = anomaly_pct(st["rain_mm"], mean_rain)
    crop_thr = heat_threshold_c is not None
    out = [
        _ind("rain_total", "Lluvia acumulada", st["rain_mm"], "mm", "derived", SOURCE_P,
             "suma de la lluvia diaria del período",
             reference={"value": _r(mean_rain), "unit": "mm", "label": ref_label} if mean_rain is not None else None),
        _ind("rain_anomaly", "Anomalía de lluvia", an, "%", "derived", SOURCE_P,
             "100·(P_actual − P̄_hist)/P̄_hist, P̄ = media 2015–2025 de las mismas fechas",
             defined=an is not None,
             reason=None if an is not None else "media histórica 0 o no disponible: anomalía no definida"),
        _ind("dry_spell_max", "Racha seca máxima", st["dry_spell_max_days"], "días", "derived", SOURCE_P,
             f"máximo de días consecutivos con P < {DRY_THRESHOLD_MM} mm/día",
             params={"threshold_mm_day": DRY_THRESHOLD_MM},
             reference={"value": _r(np.mean(hist_dry)), "unit": "días", "label": ref_label} if hist_dry else None),
        _ind("heat_days", "Días de calor", st["heat_days"], "días", "derived", SOURCE_TMAX,
             f"días con Tmáx > {heat_threshold_c} °C (umbral del cultivo)" if crop_thr
             else "días con Tmáx > P90 histórico (2015–2025) del mismo mes",
             threshold_kind="crop" if crop_thr else "general_p90",
             note=None if crop_thr else "Indicador meteorológico general: no afirma daño a un cultivo"),
        _ind("heavy_rain_days", "Días de lluvia intensa", st["heavy_rain_days"], "días", "derived", SOURCE_P,
             "días con P > P95 de los días húmedos (≥ 1 mm) del mismo mes, 2015–2025",
             note="Exposición meteorológica; no es una predicción de inundación"),
        _ind("effective_rain_total", "Lluvia efectiva", st["effective_rain_mm"], "mm", "assumption", SOURCE_P,
             "mensual: 0.8P − 25 si P > 75 mm; 0.6P − 10 si P ≤ 75 mm; mínimo 0 (FAO, Brouwer y Heibloem 1986)"),
    ]
    if st["et0_mm"] is not None:
        out.append(_ind("et0_total", "Evapotranspiración de referencia (ET0)", st["et0_mm"], "mm", "derived",
                        "NASA POWER + FAO-56 Penman–Monteith (physics.py)", "suma de ET0 diaria FAO-56"))
    else:
        out.append({"id": "et0_total", "name": "Evapotranspiración de referencia (ET0)", "value": None,
                    "unit": "mm", "evidence_status": "unavailable",
                    "reason": et0_reason or "ET0 incompleta en el período", "scale": P.SCALE_LABEL})
    return out


def build_indicators_response(period: str = "recent", heat_threshold_c: float | None = None,
                              df: pd.DataFrame | None = None) -> dict:
    df = df if df is not None else load_daily_frame()
    et0, et0_reason = et0_series(df)
    s, e, key, label = resolve_period(df, period)
    meta, pw = df.attrs.get("meta", {}), df.attrs.get("power", {})
    w = _window(df, s, e)
    n_valid = int(w[["PRECTOTCORR", "T2M_MAX"]].notna().all(axis=1).sum())
    parts = pw.get("parts", {})
    camps = campaign_indicators(df, et0)
    return {
        "site": {"lat": P.PILOT_LAT, "lon": P.PILOT_LON, "label": "Cuatro Cañadas (punto de referencia)",
                 "elevation_m": (meta.get("location") or {}).get("elevation_m")},
        "source": {"name": "NASA POWER", "product": "Daily API", "api_version": (meta.get("api") or {}).get("version"),
                   "community": "AG", "scale": P.SCALE_LABEL, "time_standard": meta.get("time_standard"),
                   "downloaded_at": max((c.get("downloaded_at") or "") for c in parts.values()) if parts else None,
                   "from_cache": all(c.get("used") for c in parts.values()) if parts else True,
                   "warnings": pw.get("warnings", [])},
        "data_window": {"historical": {"start": P.HIST_START, "end": P.HIST_END},
                        "recent": {"start": P.RECENT_START, "last_valid_date": pw.get("last_valid_date")}},
        "period": {"key": key, "start": s.date().isoformat(), "end": e.date().isoformat(), "label": label},
        "quality": {"n_days": int(len(w)), "n_valid_days": n_valid,
                    "missing_pct": round(100 * (1 - n_valid / len(w)), 1) if len(w) else None},
        "indicators": period_indicators(df, s, e, heat_threshold_c, et0, et0_reason),
        "monthly_grid": monthly_grid(df, et0),
        "campaigns": camps.replace({np.nan: None}).to_dict(orient="records"),
        "campaign_definition": CAMPAIGN_DEFINITION,
        "limitations": LIMITATIONS,
    }
