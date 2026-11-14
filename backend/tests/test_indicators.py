"""Pruebas de analytics/indicators.py: series sintéticas de resultado conocido + integración con la caché real."""
import json
import math

import numpy as np
import pandas as pd
import pytest

from analytics import indicators as I


# --------------------------------------------------------------------------- sintéticas
def test_max_dry_spell_known():
    p = [0, 0, 5, 0, 0, 0, 0.9, 1.0, 0, 0]
    assert I.max_dry_spell(p) == 4          # 0,0,0,0.9 → 4; 1.0 no es seco (< 1 mm)
    assert I.max_dry_spell(p, threshold=2.0) == 7  # 0,0,0,0.9,1.0,0,0
    assert I.max_dry_spell([3, 4]) == 0


def test_max_dry_spell_nan_breaks_and_all_nan():
    assert I.max_dry_spell([0, 0, float("nan"), 0]) == 2
    assert I.max_dry_spell([float("nan")] * 3) is None


def test_anomaly_pct_and_zero_mean():
    assert I.anomaly_pct(80, 100) == -20.0
    assert I.anomaly_pct(150, 100) == 50.0
    assert I.anomaly_pct(10, 0) is None      # P̄ = 0 → no definido (nunca divide por cero)
    assert I.anomaly_pct(None, 100) is None
    assert I.anomaly_pct(10, float("nan")) is None


@pytest.mark.parametrize("p,expected", [(100, 55.0), (75, 35.0), (50, 20.0), (10, 0.0), (0, 0.0), (200, 135.0)])
def test_effective_rain_fao(p, expected):
    assert I.effective_rain_month(p) == expected


def _synthetic(years=range(2015, 2026), rain=2.0, tmax=30.0):
    idx = pd.date_range(f"{min(years)}-01-01", f"{max(years)}-12-31", freq="D")
    return pd.DataFrame({"PRECTOTCORR": rain, "T2M_MAX": tmax}, index=idx)


def test_heavy_rain_and_heat_counts_synthetic():
    df = _synthetic()
    # un día extremo por enero de cada año → P95 de enero queda por debajo de 50
    for y in range(2015, 2026):
        df.loc[f"{y}-01-15", "PRECTOTCORR"] = 50.0
        df.loc[f"{y}-01-16", "T2M_MAX"] = 40.0
    p95, p90 = I.heavy_rain_thresholds_p95(df), I.heat_thresholds_p90(df)
    jan = df.loc["2020-01"]
    assert I.count_heavy_rain_days(jan["PRECTOTCORR"], p95) == 1
    assert I.count_heat_days(jan["T2M_MAX"], p90) == 1
    assert I.count_heat_days(jan["T2M_MAX"], 35.0) == 1   # umbral de cultivo explícito


def test_heavy_rain_dry_month_no_threshold():
    df = _synthetic(rain=0.0)                # nunca hay días húmedos → P95 indefinido
    p95 = I.heavy_rain_thresholds_p95(df)
    assert I.count_heavy_rain_days(df.loc["2020-07"]["PRECTOTCORR"], p95) == 0


def test_monthly_grid_and_campaigns_synthetic():
    df = _synthetic()
    g = I.monthly_grid(df)
    assert g["years"] == list(range(2015, 2026)) and len(g["cells"]) == 11 * 12
    c = g["cells"][0]
    assert c["rain_mm"] == 62.0 and c["rain_anomaly_pct"] == 0.0 and c["complete"]
    assert c["effective_rain_mm"] == 27.2    # 0.6·62 − 10
    camps = I.campaign_indicators(df)
    full = camps[camps["complete"]]
    assert set(full["season"]) == {"verano", "invierno"}
    years = I.yearly_indicators(df)
    assert list(years["agro_year"]) == [f"{y}-{str(y + 1)[2:]}" for y in range(2015, 2025)]  # 10 años nov→oct
    assert {"verano_rain_mm", "invierno_rain_mm"} <= set(years.columns)


def test_monthly_grid_zero_climatology_gives_null_anomaly():
    df = _synthetic(rain=0.0)
    cell = I.monthly_grid(df)["cells"][5]
    assert cell["rain_mm"] == 0.0 and cell["rain_anomaly_pct"] is None


# --------------------------------------------------------------------------- caché real
@pytest.fixture(scope="module")
def real_df():
    return I.load_daily_frame()


@pytest.fixture(scope="module")
def response(real_df):
    return I.build_indicators_response("recent", df=real_df)


def test_response_contract_shape(response):
    for k in ("site", "source", "data_window", "period", "quality", "indicators", "monthly_grid",
              "campaigns", "campaign_definition", "limitations"):
        assert k in response
    assert response["site"]["elevation_m"] == pytest.approx(276.15)
    assert response["period"]["start"] == "2026-01-01"
    assert "regional" in response["source"]["scale"]
    json.dumps(response)  # serializable (sin numpy ni NaN)
    assert "NaN" not in json.dumps(response)


def test_every_indicator_traceable(response):
    allowed = {"documented", "derived", "assumption", "farmer_provided", "unavailable"}
    for ind in response["indicators"]:
        assert ind["evidence_status"] in allowed and ind["unit"]
        if ind["value"] is None:
            assert ind["evidence_status"] == "unavailable" and ind.get("reason")
        else:
            assert ind["source"].startswith("NASA POWER") and ind["method"]


def test_real_values_plausible(response):
    v = {i["id"]: i["value"] for i in response["indicators"]}
    assert 200 < v["rain_total"] < 2500
    assert 0 <= v["dry_spell_max"] < 120
    assert v["effective_rain_total"] <= v["rain_total"]
    assert response["indicators"][3]["threshold_kind"] == "general_p90"


def test_real_grid_and_years(real_df, response):
    cells = response["monthly_grid"]["cells"]
    assert cells[0]["year"] == 2015 and cells[-1]["year"] == 2026
    assert all(c["rain_anomaly_pct"] is None for c in cells if not c["complete"])
    years = I.yearly_indicators(real_df)
    # nov-2015 → oct-2026: 11 años agrícolas completos (2015-16 … 2025-26)
    assert len(years) == 11 and years["agro_year"].iloc[0] == "2015-16"
    assert years["agro_year"].iloc[-1] == "2025-26"


def test_s2_drought_window_is_dry(real_df):
    """Coherencia con el forzante de Christian: la ventana S2 tuvo menos lluvia que lo habitual."""
    r = I.build_indicators_response("2025-12-17/2026-01-30", df=real_df)
    an = next(i for i in r["indicators"] if i["id"] == "rain_anomaly")["value"]
    assert an is not None and an < -20


def test_campaign_period_and_bad_period(real_df):
    r = I.build_indicators_response("campaign:verano-2025-26", df=real_df)
    assert r["period"]["start"] == "2025-11-01" and r["period"]["end"] == "2026-04-30"
    with pytest.raises(ValueError):
        I.build_indicators_response("campaign:otono-1999", df=real_df)
    with pytest.raises(ValueError):
        I.build_indicators_response("ayer", df=real_df)


def test_crop_heat_threshold(real_df):
    r = I.build_indicators_response("recent", heat_threshold_c=35.0, df=real_df)
    h = next(i for i in r["indicators"] if i["id"] == "heat_days")
    assert h["threshold_kind"] == "crop" and "35.0" in h["method"] and "note" not in h
