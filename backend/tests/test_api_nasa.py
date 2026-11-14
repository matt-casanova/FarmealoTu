"""Pruebas de los endpoints NASA (router de Alvin) con TestClient. No llaman a NASA."""
import time

import pytest
from fastapi.testclient import TestClient

from app.main import app
from pipeline import power as P

client = TestClient(app)


@pytest.fixture(autouse=True)
def no_network(monkeypatch):
    """Si algún endpoint intentara llamar a NASA, la prueba falla."""
    def boom(*a, **k):
        raise AssertionError("un endpoint intentó llamar a NASA en vivo")
    monkeypatch.setattr(P.requests, "get", boom)


def test_health():
    assert client.get("/api/health").json()["status"] == "ok"


def test_indicators_default_fast_and_traceable():
    client.get("/api/indicators")  # calienta la caché en memoria
    t0 = time.perf_counter()
    r = client.get("/api/indicators")
    assert r.status_code == 200 and time.perf_counter() - t0 < 1.0
    body = r.json()
    assert body["site"]["distance_km"] == 0.0 and body["last_observation_date"]
    assert {i["id"] for i in body["indicators"]} >= {"rain_total", "rain_anomaly", "dry_spell_max", "heat_days",
                                                    "heavy_rain_days", "effective_rain_total", "et0_total"}
    assert body["monthly_grid"]["indicators"][0]["cells"]


def test_indicators_campaign_and_heat_threshold():
    r = client.get("/api/indicators", params={"period": "campaign:invierno-2025", "heat_threshold_c": 35})
    assert r.status_code == 200
    body = r.json()
    assert body["period_detail"]["start"] == "2025-05-01"
    heat = next(i for i in body["indicators"] if i["id"] == "heat_days")
    assert heat["threshold_kind"] == "crop"


def test_indicators_nearby_point_warns_far_point_404():
    near = client.get("/api/indicators", params={"lat": -17.10, "lon": -62.30}).json()
    assert 10 < near["site"]["distance_km"] < 30
    assert any("km del punto de referencia" in w for w in near["source"]["warnings"])
    far = client.get("/api/indicators", params={"lat": -16.0, "lon": -62.2427})
    assert far.status_code == 404 and "No se consulta NASA en vivo" in far.json()["detail"]


def test_indicators_bad_period_422():
    r = client.get("/api/indicators", params={"period": "mañana"})
    assert r.status_code == 422


def test_nasa_status():
    body = client.get("/api/nasa/status").json()
    assert body["last_observation_date"] and body["last_download"]
    src = {s["id"]: s for s in body["sources"]}
    assert src["power_daily"]["status"] == "available" and src["power_daily"]["from_cache"]
    assert src["power_daily"]["coverage"]["start"] == "2015-01-01"
    assert src["power_gwetroot"]["status"] == "available"
    assert src["power_hourly"]["status"] in ("available", "partial")
    for opt in ("gibs", "smap_l4", "gpm_imerg", "ndvi"):
        assert src[opt]["status"] == "unavailable" and src[opt]["reason"]


def test_power_context_for_field_data():
    from pipeline.status import power_context
    c = power_context("2026-11-14")
    assert c["t2m_c"] is not None and c["date"] <= "2026-11-14" and "regional" in c["scale"]
    assert "note" in c  # el 14-nov aún no está publicado → se aclara
