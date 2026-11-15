"""POST /api/rotations/compare: contrato de la app (docs/UI_BACKEND_CONTRACT.md) y pruebas B, C y D del plan."""
import pytest
from fastapi.testclient import TestClient

from app.main import app

TOP_KEYS = {"site", "data_status", "alerts", "strategies", "recommended_strategy_id", "limitations"}
STRATEGY_KEYS = {"id", "name", "sequence", "score", "partial_score", "criteria", "uncertainty", "tradeoffs",
                 "reasons", "evidence", "missing_information", "agronomist_questions"}
BASE = {"site": {"lat": -17.2374, "lon": -62.2427}, "current_rotation": ["soy", "sorghum"],
        "priorities": {"water": 0.55, "soil": 0.25, "risk": 0.2},
        "constraints": {"irrigation": "rainfed", "excluded_crops": []}}


@pytest.fixture(scope="module")
def client():
    return TestClient(app)


def post(client, **patch):
    return client.post("/api/rotations/compare", json={**BASE, **patch})


def test_contract_keys(client):
    r = post(client)
    assert r.status_code == 200
    body = r.json()
    assert TOP_KEYS <= set(body)
    for k in ("last_observation_date", "last_successful_download", "source", "engine", "weights_received"):
        assert k in body["data_status"]
    for s in body["strategies"]:
        assert STRATEGY_KEYS <= set(s)
        assert set(s["criteria"]) == {"water", "soil", "risk"}
        for c in s["criteria"].values():
            assert {"score", "detail", "unit", "status"} <= set(c) and c["status"] in ("ok", "insufficient")
            assert c["score"] is None or 0 <= c["score"] <= 100
        assert all(set(x) == {"season", "crop"} for x in s["sequence"])
    for a in body["alerts"]:
        assert a["date"] and a["level"] in ("low", "medium", "high") and a["message"]
    assert any("inundación" in x for x in body["limitations"])        # regla R7


def test_outside_pilot_area_is_422(client):
    assert post(client, site={"lat": -16.9, "lon": -62.2427}).status_code == 422


def test_B_current_plus_two_alternatives_explained(client):
    body = post(client).json()
    ids = [s["id"] for s in body["strategies"]]
    assert ids[0] == "E0" and len(ids) >= 3
    seqs = [tuple((x["season"], x["crop"]) for x in s["sequence"]) for s in body["strategies"]]
    assert len(seqs) == len(set(seqs))
    for s in body["strategies"][1:]:
        assert s["score"] is not None and s["reasons"] and s["tradeoffs"]["advantage"]
        assert any("tu rotación" in r for r in s["reasons"])           # diferencias con la actual, con números
        assert s["tradeoffs"]["main_risk"] and "NASA POWER" in s["tradeoffs"]["main_risk"]
        assert s["evidence"] and all(e["source"].startswith("NASA POWER") and e["scale"] for e in s["evidence"])


def test_C_more_water_weight_recalculates_reproducibly(client):
    a = post(client).json()
    b = post(client).json()
    assert a["strategies"] == b["strategies"]                          # misma entrada → misma salida
    heavy = post(client, priorities={"water": 0.9, "soil": 0.05, "risk": 0.05}).json()
    assert heavy["data_status"]["weights_received"] == {"water": 0.9, "soil": 0.05, "risk": 0.05}
    assert heavy["strategies"] == post(client, priorities={"water": 0.9, "soil": 0.05, "risk": 0.05}).json()["strategies"]
    soil_heavy = post(client, priorities={"water": 0.1, "soil": 0.8, "risk": 0.1}).json()
    e0_a = next(s for s in a["strategies"] if s["id"] == "E0")
    e0_s = next(s for s in soil_heavy["strategies"] if s["id"] == "E0")
    assert e0_a["weights_used"] != e0_s["weights_used"]               # los pesos se aplican de verdad
    assert e0_a["criteria"] == e0_s["criteria"]                       # los subpuntajes no dependen de los pesos


def test_D_missing_data_declared_with_real_et0(client):
    body = post(client).json()
    ids = [s["id"] for s in body["strategies"]]
    assert "E2" in ids and body["recommended_strategy_id"] in ids     # con ET0 real: «Cuidar agua» y ranking
    assert body["data_status"]["engine"] == "ready"
    for s in body["strategies"]:
        assert s["criteria"]["water"]["status"] == "ok" and s["partial_score"] is False
        text = " ".join(s["missing_information"])
        assert "análisis de suelo" in text and "SMAP" in text and "NDVI" in text   # los vacíos se siguen declarando


def _body_without_et0(monkeypatch):
    """Respuesta del motor con la ET0 ausente (simulada), sin tocar la caché del servidor."""
    from analytics import indicators as I
    from app.schemas import CompareRequest
    from engine.recommendation import build_climate
    from engine.response import build_compare_response
    monkeypatch.setattr(I, "et0_series", lambda d: (None, "sin ET0 (simulado en la prueba)"))
    df = I.load_daily_frame()
    ind = I.build_indicators_response("recent", None, df=df)
    return build_compare_response(CompareRequest(**BASE), df, ind,
                                  {"last_observation_date": "x", "last_download": "y"}, build_climate(df))


def test_D_missing_data_declared_and_no_ranking_without_et0(monkeypatch):
    body = _body_without_et0(monkeypatch)
    assert body["recommended_strategy_id"] is None                     # sin S_agua para todas: sin ranking
    for s in body["strategies"]:
        assert s["partial_score"] is True and s["criteria"]["water"]["status"] == "insufficient"
        text = " ".join(s["missing_information"])
        assert "análisis de suelo" in text and "SMAP" in text and "NDVI" in text and "ET0" in text
    assert any("ranking" in x for x in body["limitations"])
    assert all(s["id"] != "E2" for s in body["strategies"])            # «Cuidar agua» no se inventa
    assert any("E2" in x for x in body["limitations"])


def test_ambiguous_rotation_is_declared_not_guessed(client):
    body = post(client, current_rotation=["sorghum", "wheat"]).json()
    e0 = body["strategies"][0]
    assert e0["score"] is None and e0["sequence"] == ["sorghum", "wheat"]
    assert any("invierno" in m for m in e0["missing_information"])


def test_rotation_failing_filters_is_still_evaluated(client):
    body = post(client, constraints={"irrigation": "rainfed", "excluded_crops": ["sorghum"]}).json()
    e0 = body["strategies"][0]
    assert e0["score"] is not None                                     # se evalúa igual
    assert any("no pasa los filtros" in r for r in e0["reasons"])
    assert all(s["sequence"][1]["crop"] != "sorghum" for s in body["strategies"][1:])


def test_no_feasible_alternative_is_explained(client):
    body = post(client, constraints={"irrigation": "rainfed", "excluded_crops": ["soy"]}).json()
    assert [s["id"] for s in body["strategies"]] == ["E0"]
    assert any("E1" in x for x in body["limitations"]) and any("0 factibles" in x for x in body["limitations"])
