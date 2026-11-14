"""Prueba A (plan, sección 11.1): conexión NASA POWER.

La ingesta trae valores reales del punto piloto, conserva fecha, unidades y fuente,
los guarda en caché y el JSON recibido se puede inspeccionar. Sin −999.

Por defecto usa la CACHÉ versionada (no depende de la red en la demo).
Para forzar una llamada real:  $env:FARMEALOTU_LIVE = "1"; pytest -q tests/test_power.py
"""
import json
import os

import pytest
import requests

from pipeline import power as P

LIVE = os.environ.get("FARMEALOTU_LIVE") == "1"
EXPECTED_UNITS = {"T2M": "C", "T2M_MAX": "C", "T2M_MIN": "C", "PRECTOTCORR": "mm/day",
                  "RH2M": "%", "WS2M": "m/s", "ALLSKY_SFC_SW_DWN": "MJ/m^2/day"}


@pytest.fixture(scope="module")
def hist():
    return P.fetch_power_daily(P.PILOT_LAT, P.PILOT_LON, P.HIST_START, P.HIST_END, refresh=LIVE)


@pytest.fixture(scope="module")
def recent():
    return P.fetch_power_daily(P.PILOT_LAT, P.PILOT_LON, P.RECENT_START, P.RECENT_END, refresh=LIVE)


def test_raw_json_is_inspectable(hist):
    """El JSON crudo de POWER está guardado tal cual y tiene la estructura esperada."""
    path = P.CACHE_DIR / hist["cache"]["file"]
    blob = json.loads(path.read_text(encoding="utf-8"))
    raw, meta = blob["response"], blob["_rotasat_meta"]
    P.validate_raw(raw)
    assert set(EXPECTED_UNITS) <= set(raw["properties"]["parameter"])
    assert meta["downloaded_at"] and meta["request"]["url"] == P.DAILY_URL
    assert meta["request"]["query"]["community"] == "AG"


def test_units_source_and_location(hist):
    d = hist["data"]
    assert d["source"] == "NASA POWER" and d["evidence_status"] == "documented"
    assert d["units"] == EXPECTED_UNITS
    assert d["time_standard"] == "LST"
    assert d["location"]["elevation_m"] is not None
    assert abs(d["location"]["lat"] - P.PILOT_LAT) < 0.01 and abs(d["location"]["lon"] - P.PILOT_LON) < 0.01
    assert "regional" in d["scale"]


def test_dates_iso_aligned_and_complete(hist):
    recs = hist["data"]["records"]
    assert recs[0]["date"] == "2015-01-01" and recs[-1]["date"] == "2025-12-31"
    assert len(recs) == 4018  # 11 años, incluye 2016, 2020 y 2024 bisiestos
    assert all(set(r) == {"date", *EXPECTED_UNITS} for r in recs)


def test_no_fill_values_anywhere(hist, recent):
    for res in (hist, recent):
        for r in res["data"]["records"]:
            for k, v in r.items():
                if k != "date":
                    assert v is None or v > -998, f"quedó un fill value en {r['date']} {k}"


def test_real_plausible_values(hist):
    """Valores físicamente plausibles para Cuatro Cañadas (no inventados ni en otra unidad)."""
    recs = hist["data"]["records"]
    tmax = [r["T2M_MAX"] for r in recs if r["T2M_MAX"] is not None]
    rain = [r["PRECTOTCORR"] for r in recs if r["PRECTOTCORR"] is not None]
    assert 25 < sum(tmax) / len(tmax) < 35
    assert 600 < sum(rain) / 11 < 2000  # lluvia anual media en mm


def test_recent_last_valid_date_detected(recent):
    lv = P.last_valid_date(recent["data"])
    assert lv is not None and "2026-10-01" <= lv <= "2026-11-14"


def test_fill_value_parsing_unit():
    raw = {"header": {"fill_value": -999.0, "time_standard": "LST"},
           "geometry": {"coordinates": [-62.2, -17.2, 276.0]},
           "parameters": {"T2M": {"units": "C"}},
           "properties": {"parameter": {"T2M": {"20260101": 25.0, "20260102": -999.0}}}}
    d = P.parse_power_json(raw)
    assert d["records"] == [{"date": "2026-01-01", "T2M": 25.0}, {"date": "2026-01-02", "T2M": None}]
    assert d["missing_count"] == {"T2M": 1}


def test_fallback_to_cache_when_nasa_down(monkeypatch):
    class Down:
        def get(self, *a, **k):
            raise requests.ConnectionError("sin red (simulado)")
    monkeypatch.setattr(P.time, "sleep", lambda s: None)
    res = P.fetch_power_daily(P.PILOT_LAT, P.PILOT_LON, P.RECENT_START, P.RECENT_END,
                              refresh=True, session=Down())
    assert res["cache"]["used"] and res["cache"]["stale_fallback"]
    assert "no respondió" in res["cache"]["warning"] and res["cache"]["downloaded_at"]


def test_invalid_structure_rejected():
    with pytest.raises(ValueError):
        P.validate_raw({"properties": {}})
