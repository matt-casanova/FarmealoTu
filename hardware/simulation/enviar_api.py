"""Envía lecturas del GEMELO DIGITAL (simulación) a la API de FarmealoTu.

Uso desde la simulación (Python con `requests`; funciona con VPython local o en Jupyter):

    from enviar_api import enviar_lectura

    enviar_lectura(
        api_base="http://192.168.43.10:8000",      # IP de la PC de Alvin en el hotspot (o la URL del túnel)
        scenario="S2",                              # S1 base · S2 sequía · S3 exceso de lluvia · S4 riego A · S5 fallas
        zonas=[{"id": "A", "vwc": 0.12, "temp_c": 24.6},
               {"id": "B", "vwc": 0.21, "temp_c": 24.1},
               {"id": "C", "vwc": 0.33, "temp_c": 23.8}],
        aire={"t_c": 27.3, "rh_pct": 55.0},         # opcional (DHT11 simulado)
        luz_pct=62.0,                                # opcional (índice relativo 0–100)
        sim_time="2025-12-20T14:00:00Z",             # opcional: hora DENTRO de la simulación
    )

Reglas del contrato (shared/sensor_reading.schema.json):
- vwc es FRACCIÓN 0–1 (0.21 = 21 %), no porcentaje. Temperaturas en °C. Humedad del aire 0–100.
- Si un valor no existe en la simulación, páselo como None (nunca un número de relleno).
- No se manda `ts`: el servidor pone la hora de llegada, así las lecturas simuladas no se mezclan en el tiempo con
  las del nodo físico. La hora simulada viaja en `sim_time`.
- Las lecturas quedan rotuladas source = "simulated" (la app muestra la insignia SIMULADO).
"""
from __future__ import annotations

import requests

NODE_ID = "farmealotu-gemelo-01"
ENDPOINT = "/api/field-node/readings"


def armar_lectura(scenario: str | None, zonas: list[dict], aire: dict | None = None, luz_pct: float | None = None,
                  depth_cm: float | None = 7.0, sim_time: str | None = None, flags: list[str] | None = None) -> dict:
    lectura = {
        "schema_version": "1.0",
        "node_id": NODE_ID,
        "source": "simulated",
        "scenario": scenario,
        "internal": {"depth_cm": depth_cm, "zones": [
            {"id": z["id"], "soil_moisture_vwc": z.get("vwc"), "soil_temp_c": z.get("temp_c"),
             "flags": list(z.get("flags", []))} for z in zonas]},
        "external": {
            "dht11": None if aire is None else {"t_c": aire.get("t_c"), "rh_pct": aire.get("rh_pct")},
            "ldr": None if luz_pct is None else {"light_pct": luz_pct},
            "bme680": None,
        },
        "calibration": {"moisture_method": "modelo_balance_hidrico", "date": None},
        "quality_flags": list(flags or []),
    }
    if sim_time:
        lectura["sim_time"] = sim_time
    return lectura


def enviar_lectura(api_base: str, scenario: str | None, zonas: list[dict], aire: dict | None = None,
                   luz_pct: float | None = None, depth_cm: float | None = 7.0, sim_time: str | None = None,
                   flags: list[str] | None = None, timeout: float = 10) -> bool:
    """POST de una lectura. Devuelve True si quedó guardada (HTTP 201). Nunca corta la simulación por un error."""
    url = api_base.rstrip("/") + ENDPOINT
    lectura = armar_lectura(scenario, zonas, aire, luz_pct, depth_cm, sim_time, flags)
    try:
        r = requests.post(url, json=lectura, timeout=timeout)
    except requests.RequestException as exc:
        print(f"[enviar_api] sin conexión con {url}: {exc}")
        return False
    if r.status_code == 201:
        return True
    print(f"[enviar_api] HTTP {r.status_code}: {r.text[:300]}")   # 422 = algún valor fuera de rango o mal formado
    return False


if __name__ == "__main__":   # prueba rápida: python enviar_api.py http://<IP>:8000
    import sys
    base = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000"
    ok = enviar_lectura(base, "S1", [{"id": "A", "vwc": 0.12, "temp_c": 24.6}], aire={"t_c": 27.0, "rh_pct": 55.0},
                        luz_pct=60.0, sim_time="2025-12-20T14:00:00Z")
    print("guardada" if ok else "NO guardada")
