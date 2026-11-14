"""Emisor de lecturas de PRUEBA para ver la base del nodo actualizándose (no son mediciones).

Las lecturas van rotuladas: node_id="prueba-alvin", source="simulated", scenario="PRUEBA", quality_flags=["prueba_manual"].
Los valores son una onda sintética simple, NO el gemelo digital de Christian.

Uso (desde backend/, con el servidor corriendo):
    python -m tools.demo_sender                       # una lectura cada 3 s, contra http://127.0.0.1:8000
    python -m tools.demo_sender --every 1 --count 20
    python -m tools.demo_sender --api https://<tunel>.trycloudflare.com
    python -m tools.demo_sender --clean               # borra TODAS las lecturas de prueba de la base local
"""
from __future__ import annotations

import argparse
import math
import sqlite3
import time
from datetime import datetime, timezone

import requests

NODE_ID = "prueba-alvin"


def reading(i: int) -> dict:
    t = i / 10.0
    return {
        "schema_version": "1.0", "node_id": NODE_ID, "source": "simulated", "scenario": "PRUEBA",
        "ts": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "internal": {"depth_cm": 7, "zones": [
            {"id": "A", "soil_moisture_vwc": round(0.10 + 0.02 * math.sin(t), 3), "soil_temp_c": round(25 + 2 * math.sin(t), 1), "flags": []},
            {"id": "B", "soil_moisture_vwc": round(0.20 + 0.02 * math.sin(t + 1), 3), "soil_temp_c": round(24 + 1.5 * math.sin(t), 1), "flags": []},
            {"id": "C", "soil_moisture_vwc": round(0.32 + 0.02 * math.sin(t + 2), 3), "soil_temp_c": round(23 + 1 * math.sin(t), 1), "flags": []},
        ]},
        "external": {"bme680": {"t_c": round(27 + 2 * math.sin(t), 1), "rh_pct": round(50 - 5 * math.sin(t), 1), "p_hpa": 980.0, "gas_kohm": 80.0},
                     "dht11": {"t_c": round(27 + 2 * math.sin(t)), "rh_pct": round(52 - 5 * math.sin(t))},
                     "ldr": {"raw": 1800, "mv": 1450, "light_pct": round(50 + 30 * math.sin(t), 1), "r_fixed_ohm": 2200}},
        "calibration": {"moisture_method": "prueba_sin_calibrar", "date": None},
        "quality_flags": ["prueba_manual"],
    }


def clean() -> None:
    from app.routers.field_node import DB_PATH
    c = sqlite3.connect(DB_PATH)
    n = c.execute("DELETE FROM readings WHERE node_id = ?", (NODE_ID,)).rowcount
    c.commit()
    print(f"borradas {n} lecturas de prueba ({NODE_ID}) de {DB_PATH}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--api", default="http://127.0.0.1:8000")
    ap.add_argument("--every", type=float, default=3.0, help="segundos entre lecturas")
    ap.add_argument("--count", type=int, default=0, help="0 = sin fin (Ctrl+C para cortar)")
    ap.add_argument("--clean", action="store_true")
    a = ap.parse_args()
    if a.clean:
        clean()
        return
    url = a.api.rstrip("/") + "/api/field-node/readings"
    i = 0
    print(f"enviando lecturas de PRUEBA a {url} (Ctrl+C para cortar; luego --clean para borrarlas)")
    try:
        while a.count == 0 or i < a.count:
            r = requests.post(url, json=reading(i), timeout=15)
            print(f"  #{i + 1} → HTTP {r.status_code} {r.json() if r.ok else r.text[:200]}")
            i += 1
            time.sleep(a.every)
    except KeyboardInterrupt:
        print("cortado")


if __name__ == "__main__":
    main()
