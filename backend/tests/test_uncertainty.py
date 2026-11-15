"""Prueba E del plan: bootstrap por años completos (200 iteraciones, seed = 42), rangos, estabilidad y tiempo."""
import time

import numpy as np
import pytest
from fastapi.testclient import TestClient

from analytics import uncertainty as U
from app.main import app
from app.routers import rotations as RT

BASE = {"site": {"lat": -17.2374, "lon": -62.2427}, "current_rotation": ["soy", "sorghum"],
        "priorities": {"water": 0.55, "soil": 0.25, "risk": 0.2},
        "constraints": {"irrigation": "rainfed", "excluded_crops": []}}


@pytest.fixture(scope="module")
def client():
    RT.warm_up()                                    # lo mismo que hace el arranque del servidor (lifespan)
    return TestClient(app)


def test_resample_whole_years_reproducible():
    idx = U.resample_indices(11)
    assert idx.shape == (200, 11) and idx.min() >= 0 and idx.max() <= 10   # años enteros, 11 por iteración
    assert np.array_equal(idx, U.resample_indices(11))                      # seed = 42 → reproducible
    assert not np.array_equal(idx, U.resample_indices(11, seed=7))


def test_bootstrap_math_and_ties():
    rng = np.random.default_rng(0)
    risk = rng.uniform(40, 90, 11)
    a = U.StrategyInputs(soil=70.0, risk_years=risk, water_years=None)
    b = U.StrategyInputs(soil=70.0, risk_years=risk.copy(), water_years=None)   # idéntica a «a»
    c = U.StrategyInputs(soil=10.0, risk_years=risk - 30, water_years=None)    # claramente peor
    out = U.bootstrap({"E0": a, "E1": b, "E3": c}, {"water": 0.4, "soil": 0.35, "risk": 0.25}, 11)
    per = out["per_strategy"]
    for v in per.values():
        assert v["p10"] <= v["p50"] <= v["p90"] and 0 <= v["rank_stability"] <= 1
    assert per["E0"]["rank_stability"] == pytest.approx(0.5) == per["E1"]["rank_stability"]  # empate repartido
    assert per["E3"]["rank_stability"] == 0
    assert sum(v["rank_stability"] for v in per.values()) == pytest.approx(1.0)
    assert out["pairwise"]["E0>E3"] == 1.0 and out["pairwise"]["E0>E1"] == 0.0
    assert out["n_iter"] == 200 and out["seed"] == 42


def test_water_bootstrap_uses_resampled_years():
    w = np.array([[50.0, 100.0]] * 5 + [[100.0, 100.0]] * 6)
    s = U.StrategyInputs(soil=None, risk_years=None, water_years=w)
    out = U.bootstrap({"E0": s}, {"water": 1.0, "soil": 0.0, "risk": 0.0}, 11)["per_strategy"]["E0"]
    assert 50 <= out["p10"] <= out["p50"] <= out["p90"] <= 100


def test_confidence_policy():
    assert U.confidence(0.8, [], soil_info=True)[0] == "medium"              # máximo con datos regionales
    assert U.confidence(0.4, [], soil_info=True)[0] == "low"                 # estabilidad < 0.5
    assert U.confidence(0.9, ["agua"], soil_info=True)[0] == "low"           # criterio faltante
    level, reason = U.confidence(0.9, [], soil_info=False)                   # sin análisis de suelo
    assert level == "low" and "suelo" in reason
    assert all(U.confidence(x, [], soil_info=True)[0] != "high" for x in (0.0, 0.5, 1.0))


def test_E_api_ranges_reproducible_and_fast(client):
    t = time.perf_counter()
    r = client.post("/api/rotations/compare", json={**BASE, "priorities": {"water": 0.3, "soil": 0.3, "risk": 0.4}})
    elapsed = time.perf_counter() - t
    assert r.status_code == 200 and elapsed < 3.0                            # request nuevo, datos precalentados
    body = r.json()
    for s in body["strategies"]:
        u = s["uncertainty"]
        assert u["p10"] <= u["p50"] <= u["p90"] and 0 <= u["rank_stability"] <= 1
        assert u["confidence_level"] in ("low", "medium") and u["confidence_reason"]
        assert u["n_iterations"] == 200 and u["seed"] == 42
        assert s["score"] == u["p50"]                                       # plan 3.8: score mediano
    assert abs(sum(s["uncertainty"]["rank_stability"] for s in body["strategies"]) - 1) < 0.01
    assert any("no es una garantía de rendimiento ni un pronóstico" in x for x in body["limitations"])
    pw = body["data_status"]["uncertainty"]["pairwise_prob_better"]
    assert all(0 <= v <= 1 for v in pw.values()) and "E1>E0" in pw
    again = client.post("/api/rotations/compare", json={**BASE, "priorities": {"water": 0.3, "soil": 0.3, "risk": 0.4}})
    assert again.json()["strategies"] == body["strategies"]                  # reproducible


def test_without_et0_confidence_is_low(monkeypatch):
    from tests.test_api_compare import _body_without_et0
    body = _body_without_et0(monkeypatch)
    for s in body["strategies"]:
        assert s["uncertainty"]["confidence_level"] == "low"
        assert "agua" in s["uncertainty"]["confidence_reason"]


def test_with_real_et0_water_is_not_a_missing_criterion(client):
    body = client.post("/api/rotations/compare", json=BASE).json()
    for s in body["strategies"]:
        assert "falta el criterio de agua" not in s["uncertainty"]["confidence_reason"]
        assert s["uncertainty"]["confidence_level"] in ("low", "medium")       # nunca «high» con datos regionales
