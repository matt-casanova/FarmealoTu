"""Forzante horario NASA POWER para el gemelo digital de Christian (escenarios S1, S2, S3).

Selección de períodos (reproducible, a partir de la serie DIARIA ya en caché):
- S1: últimos 14 días completos con todas las variables horarias válidas (línea base reciente).
- S2: ventana de 45 días con MENOS lluvia dentro de dic-2025 → ene-2026 (sequía reportada por ANAPO).
- S3: ventana de 45 días con MÁS lluvia dentro de feb-2026 → mar-2026 (exceso de lluvia reportado).

Salida: hardware/simulation/forcing/power_hourly_S{1,2,3}.csv (+ forcing_metadata.json).
Cada CSV empieza con líneas de comentario '#' (fuente, período, estándar de tiempo, unidades);
leer con pandas.read_csv(path, comment="#").

Uso (desde backend/):  python -m pipeline.forcing
"""
from __future__ import annotations

import csv
import json
from datetime import date, timedelta
from pathlib import Path

from pipeline import power as P

OUT_DIR = Path(__file__).resolve().parents[2] / "hardware" / "simulation" / "forcing"
HOURLY_PARAMS = ["T2M", "RH2M", "WS2M", "PRECTOTCORR", "ALLSKY_SFC_SW_DWN", "PS", "T2MDEW"]
MJ_H_TO_W_M2 = 1e6 / 3600.0  # 1 MJ/m²/h = 277.78 W/m²

# columna CSV → (parámetro POWER, unidad esperada)
COLUMNS = [
    ("T2M_C", "T2M", "C"),
    ("RH2M_pct", "RH2M", "%"),
    ("WS2M_m_s", "WS2M", "m/s"),
    ("PRECTOTCORR_mm_h", "PRECTOTCORR", "mm/hour"),
    ("ALLSKY_SFC_SW_DWN_MJ_m2_h", "ALLSKY_SFC_SW_DWN", "MJ/hr"),
    ("PS_kPa", "PS", "kPa"),
    ("T2MDEW_C", "T2MDEW", "C"),
]


def _daily_precip() -> list[tuple[date, float | None]]:
    d = P.load_pilot_daily()["data"]
    return [(date.fromisoformat(r["date"]), r["PRECTOTCORR"]) for r in d["records"]]


def _window(series, lo: date, hi: date, length: int, mode: str) -> tuple[date, date, float]:
    pts = [(d, p) for d, p in series if lo <= d <= hi]
    best = None
    for i in range(len(pts) - length + 1):
        win = pts[i:i + length]
        if any(p is None for _, p in win):
            continue
        tot = sum(p for _, p in win)
        if best is None or (tot < best[2] if mode == "min" else tot > best[2]):
            best = (win[0][0], win[-1][0], tot)
    if best is None:
        raise RuntimeError(f"Sin ventana válida de {length} días entre {lo} y {hi}")
    return best


def select_periods() -> dict:
    series = _daily_precip()
    s2 = _window(series, date(2025, 12, 1), date(2026, 1, 31), 45, "min")
    s3 = _window(series, date(2026, 2, 1), date(2026, 3, 31), 45, "max")
    hist_mean = {}
    for key, (a, b, _) in (("S2", s2), ("S3", s3)):
        # lluvia media 2015–2025 del mismo calendario (para dar contexto, no para el CSV)
        vals = []
        for y in range(2015, 2025):
            ya, yb = a.replace(year=y), b.replace(year=y + (b.year - a.year))
            vals.append(sum(p for d, p in series if ya <= d <= yb and p is not None))
        hist_mean[key] = round(sum(vals) / len(vals), 1)
    return {
        "S2": {"start": s2[0], "end": s2[1], "precip_mm": round(s2[2], 1), "hist_mean_mm": hist_mean["S2"],
               "rule": "ventana de 45 días con menor lluvia total entre 2025-12-01 y 2026-01-31"},
        "S3": {"start": s3[0], "end": s3[1], "precip_mm": round(s3[2], 1), "hist_mean_mm": hist_mean["S3"],
               "rule": "ventana de 45 días con mayor lluvia total entre 2026-02-01 y 2026-03-31"},
    }


MET_PARAMS = [p for p in HOURLY_PARAMS if p != "ALLSKY_SFC_SW_DWN"]


def _complete_days(records, params=None) -> list[str]:
    """Fechas (YYYY-MM-DD) con las 24 horas y todas las variables `params` válidas."""
    params = params or HOURLY_PARAMS
    by_day: dict[str, list] = {}
    for r in records:
        by_day.setdefault(r["date"][:10], []).append(r)
    return sorted(d for d, rows in by_day.items()
                  if len(rows) == 24 and all(r.get(p) is not None for r in rows for p in params))


def _daily_radiation() -> dict[str, float | None]:
    d = P.load_pilot_daily()["data"]
    return {r["date"]: r["ALLSKY_SFC_SW_DWN"] for r in d["records"]}


def _write_csv(name: str, records, res: dict, info: dict) -> dict:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    data = res["data"]
    units = data["units"]
    for col, p, expected in COLUMNS:
        if units.get(p) != expected:
            raise RuntimeError(f"Unidad inesperada para {p}: {units.get(p)!r} (esperada {expected!r})")
    path = OUT_DIR / f"power_hourly_{name}.csv"
    n_missing = 0
    with path.open("w", newline="", encoding="utf-8") as f:
        f.write(f"# FarmealoTu · forzante horario {name} · {info['description']}\n")
        f.write(f"# Fuente: NASA POWER Hourly API {data['api'].get('version')} (community=AG) · "
                f"orígenes {', '.join(data['upstream_sources'])} · evidence_status=documented\n")
        f.write(f"# Punto: lat {P.PILOT_LAT}, lon {P.PILOT_LON} (Cuatro Cañadas, referencia; celda POWER "
                f"lat {data['location']['lat']}, lon {data['location']['lon']}, elevación {data['location']['elevation_m']} m)\n")
        f.write(f"# Escala: {P.SCALE_LABEL}. NO es una medición de una finca.\n")
        f.write(f"# Tiempo: datetime_lst = {data['time_standard']} (hora solar local, ≈ UTC−4.15 h en esta longitud); "
                f"cada fila = hora que empieza en ese instante\n")
        f.write(f"# Período: {info['start']} → {info['end']} · regla: {info['rule']}\n")
        f.write(f"# Descargado: {res['cache']['downloaded_at']} · caché backend/data/cache/{res['cache']['file']}\n")
        f.write("# ALLSKY_SFC_SW_DWN_W_m2 es DERIVADO (MJ/m²/h × 277.78). Celdas vacías = dato faltante (−999 de POWER)\n")
        f.write("# ALLSKY_SFC_SW_DWN_daily_MJ_m2_day = radiación DIARIA documentada de POWER (mismo valor en las "
                "24 h del día), para desagregar si falta la horaria\n")
        rad_cov = sum(1 for r in records if r.get("ALLSKY_SFC_SW_DWN") is not None)
        if rad_cov < len(records):
            f.write(f"# AVISO: radiación HORARIA disponible en {rad_cov}/{len(records)} horas (atraso de CERES en POWER). "
                    "Si se desagrega desde la diaria, rotular la serie como «desagregado».\n")
        daily_rad = _daily_radiation()
        w = csv.writer(f)
        w.writerow(["datetime_lst"] + [c for c, _, _ in COLUMNS]
                   + ["ALLSKY_SFC_SW_DWN_W_m2", "ALLSKY_SFC_SW_DWN_daily_MJ_m2_day"])
        for r in records:
            row = [r["date"]]
            for _, p, _ in COLUMNS:
                v = r.get(p)
                if v is None:
                    n_missing += 1
                row.append("" if v is None else v)
            rad = r.get("ALLSKY_SFC_SW_DWN")
            row.append("" if rad is None else round(rad * MJ_H_TO_W_M2, 1))
            dr = daily_rad.get(r["date"][:10])
            row.append("" if dr is None else dr)
            w.writerow(row)
    return {"file": str(path.relative_to(OUT_DIR.parents[2])).replace("\\", "/"), "rows": len(records),
            "missing_values": n_missing, "hourly_radiation_hours": rad_cov,
            **{k: str(v) for k, v in info.items()}}


def build(refresh: bool = False) -> dict:
    periods = select_periods()
    meta = {"source": "NASA POWER Hourly API", "lat": P.PILOT_LAT, "lon": P.PILOT_LON,
            "time_standard": "LST", "units": {c: u for c, _, u in COLUMNS} | {"ALLSKY_SFC_SW_DWN_W_m2": "W/m^2 (derivado)",
                                                       "ALLSKY_SFC_SW_DWN_daily_MJ_m2_day": "MJ/m^2/day (POWER diario)"},
            "scenarios": {}}

    # S1: últimos 14 días completos
    rec = P.fetch_power_hourly(P.PILOT_LAT, P.PILOT_LON, "2026-10-01", P.RECENT_END, HOURLY_PARAMS, refresh=refresh)
    days = _complete_days(rec["data"]["records"], MET_PARAMS)[-14:]
    if len(days) < 14:
        raise RuntimeError("No hay 14 días horarios completos recientes")
    recs = [r for r in rec["data"]["records"] if r["date"][:10] in set(days)]
    meta["scenarios"]["S1"] = _write_csv("S1", recs, rec, {
        "description": "línea base reciente", "start": days[0], "end": days[-1],
        "rule": "últimos 14 días con las 24 h y todas las variables meteorológicas válidas "
                "(radiación horaria: ver aviso)"})

    for key, desc in (("S2", "sequía dic-2025 → ene-2026"), ("S3", "exceso de lluvia feb–mar-2026")):
        pr = periods[key]
        res = P.fetch_power_hourly(P.PILOT_LAT, P.PILOT_LON, pr["start"], pr["end"], HOURLY_PARAMS, refresh=refresh)
        meta["scenarios"][key] = _write_csv(key, res["data"]["records"], res, {
            "description": desc, "start": pr["start"], "end": pr["end"], "rule": pr["rule"]})
        meta["scenarios"][key].update({"precip_daily_total_mm": pr["precip_mm"],
                                       "precip_same_dates_mean_2015_2024_mm": pr["hist_mean_mm"]})

    (OUT_DIR / "forcing_metadata.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    return meta


if __name__ == "__main__":
    import sys
    print(json.dumps(build(refresh="--refresh" in sys.argv), ensure_ascii=False, indent=2))
