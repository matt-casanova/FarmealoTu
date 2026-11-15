"""Pruebas del esqueleto: nodo de campo (contrato v1), compare provisional, contexto y fuentes."""
import json
from pathlib import Path

import jsonschema
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.routers import field_node

SCHEMA = json.loads((Path(__file__).resolve().parents[2] / "shared" / "sensor_reading.schema.json")
                    .read_text(encoding="utf-8"))

READING = {
    "schema_version": "1.0", "node_id": "rotasat-node-01", "source": "simulated", "scenario": "S4",
    "ts": "2026-11-14T20:15:00Z",
    "internal": {"depth_cm": 7, "zones": [
        {"id": "A", "soil_moisture_vwc": 0.11, "soil_moisture_raw": 2710, "soil_temp_c": 24.6, "flags": []},
        {"id": "B", "soil_moisture_vwc": 0.19, "soil_moisture_raw": 2290, "soil_temp_c": 23.9, "flags": []},
        {"id": "C", "soil_moisture_vwc": None, "soil_moisture_raw": None, "soil_temp_c": 23.1,
         "flags": ["moisture_uncalibrated"]}]},
    "external": {"bme680": {"t_c": 26.1, "rh_pct": 48.2, "p_hpa": 965.0, "gas_kohm": 85.4},
                 "dht11": {"t_c": 26.0, "rh_pct": 50.0},
                 "ldr": {"raw": 1830, "mv": 1480, "light_pct": 45.0, "r_fixed_ohm": 2200}},
    "calibration": {"moisture_method": "gravimetric_3pt", "date": "2026-11-14", "ds18b20_offsets_c": [0.1, -0.2, 0.0]},
    "quality_flags": [],
}


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(field_node, "DB_PATH", tmp_path / "field.sqlite")
    return TestClient(app)


def test_shared_schema_accepts_plan_example():
    jsonschema.validate(READING, SCHEMA)
    bad = {**READING, "source": "inventado"}
    with pytest.raises(jsonschema.ValidationError):
        jsonschema.validate(bad, SCHEMA)


def test_post_and_get_readings(client):
    r = client.post("/api/field-node/readings", json=READING)
    assert r.status_code == 201 and r.json()["stored"] and not r.json()["ts_from_server"]
    no_ts = {**READING, "source": "wokwi", "scenario": None}
    no_ts.pop("ts")
    r2 = client.post("/api/field-node/readings", json=no_ts)
    assert r2.status_code == 201 and r2.json()["ts_from_server"]
    body = client.get("/api/field-node/readings").json()
    assert body["count"] == 2 and {x["source"] for x in body["readings"]} == {"simulated", "wokwi"}
    zone_c = body["readings"][0]["internal"]["zones"][2]
    assert zone_c["soil_moisture_vwc"] is None and "moisture_uncalibrated" in zone_c["flags"]  # null no se vuelve 0
    only_sim = client.get("/api/field-node/readings", params={"source": "simulated"}).json()
    assert only_sim["count"] == 1 and only_sim["readings"][0]["scenario"] == "S4"


@pytest.mark.parametrize("patch,field", [
    ({"source": "real"}, "source"),
    ({"source": "physical", "scenario": "S4"}, None),             # scenario solo con simulated
    ({"internal": {"zones": [{"id": "A", "soil_moisture_vwc": 35}]}}, "soil_moisture_vwc"),  # % en vez de fracción
])
def test_invalid_readings_422(client, patch, field):
    r = client.post("/api/field-node/readings", json={**READING, **patch})
    assert r.status_code == 422
    if field:
        assert field in json.dumps(r.json()["detail"])


def test_field_status_with_power_context(client):
    empty = client.get("/api/field-node/status").json()
    assert empty["status"] == "no_data" and empty["power_context"]["source"].startswith("NASA POWER")
    client.post("/api/field-node/readings", json=READING)
    st = client.get("/api/field-node/status").json()
    assert st["status"] == "ok" and st["source"] == "simulated" and st["last_contact"]
    assert st["calibration"]["moisture_method"] == "gravimetric_3pt"
    assert "regional" in st["power_context"]["scale"]


def test_compare_uses_real_engine(client):
    """Reemplaza a test_compare_is_honest_stub: el router ya no es el stub (las pruebas B–E están en
    test_api_compare.py y test_engine.py)."""
    req = {"site": {"lat": -17.2374, "lon": -62.2427}, "current_rotation": ["soy", "sorghum"],
           "priorities": {"water": 0.55, "soil": 0.25, "risk": 0.2},
           "constraints": {"irrigation": "rainfed", "excluded_crops": []}, "notes": "poca agua"}
    body = client.post("/api/rotations/compare", json=req).json()
    for k in ("site", "data_status", "alerts", "strategies", "recommended_strategy_id", "limitations"):
        assert k in body
    assert body["strategies"][0]["id"] == "E0" and len(body["strategies"]) >= 3
    assert body["data_status"]["engine"] in ("partial", "ready")
    assert all(a["date"] for a in body["alerts"])


@pytest.mark.parametrize("req", [
    {"current_rotation": []},
    {"current_rotation": ["soja"]},                                    # código inválido (la app manda 'soy')
    {"current_rotation": ["fallow"]},
    {"current_rotation": ["soy"], "priorities": {"water": 0, "soil": 0, "risk": 0}},
    {"current_rotation": ["soy"], "constraints": {"irrigation": "a veces"}},
    {"current_rotation": ["soy"], "site": {"lat": -15.0, "lon": -62.2}},
])
def test_compare_validation_422(client, req):
    assert client.post("/api/rotations/compare", json=req).status_code == 422


def test_context_and_sources(client):
    ctx = client.get("/api/context/cuatro-canadas").json()
    soy = ctx["produccion_2023"]["cultivos"][0]
    assert soy["codigo"] == "soy" and soy["superficie_ha"] == 318083 and soy["porcentaje_superficie"] == 69.4
    assert ctx["riesgos_historicos"]["items"][0]["source_url"].startswith("https://")
    src = client.get("/api/sources").json()["sources"]
    assert all(s["url"].startswith("https://") and s["title"] for s in src)
    assert any(s["id"] == "nasa_power" for s in src)


def test_monitor_page(client):
    r = client.get("/monitor")
    assert r.status_code == 200 and "text/html" in r.headers["content-type"]
    assert "/api/field-node/readings" in r.text and "/api/nasa/status" in r.text


def test_demo_sender_reading_is_valid_and_labeled():
    from tools.demo_sender import NODE_ID, reading
    rd = reading(3)
    jsonschema.validate(rd, SCHEMA)
    assert rd["node_id"] == NODE_ID and rd["source"] == "simulated" and "prueba_manual" in rd["quality_flags"]


def test_openapi_lists_all_endpoints(client):
    paths = client.get("/openapi.json").json()["paths"]
    for p in ("/api/health", "/api/indicators", "/api/nasa/status", "/api/rotations/compare",
              "/api/field-node/readings", "/api/field-node/status", "/api/context/cuatro-canadas", "/api/sources"):
        assert p in paths
