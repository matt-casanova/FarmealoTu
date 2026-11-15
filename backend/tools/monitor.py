"""Monitor de terminal: muestra en vivo las lecturas del nodo que llegan a la API.

Uso (desde backend/, con el servidor corriendo):
    python -m tools.monitor                              # http://127.0.0.1:8000, cada 2 s
    python -m tools.monitor --api https://<tunel>.trycloudflare.com --every 5
    python -m tools.monitor --source physical            # solo el nodo físico
Ctrl+C para salir. No escribe nada: solo lee GET /api/field-node/readings.
"""
from __future__ import annotations

import argparse
import functools
import time

import requests

print = functools.partial(print, flush=True)  # noqa: A001 — que se vea al instante aunque la salida esté redirigida

BADGE = {"simulated": "SIMULADO", "wokwi": "WOKWI", "physical": "FÍSICO"}


def _f(v, fmt="{:.1f}", none="—"):
    return none if v is None else fmt.format(v)


def line(r: dict) -> str:
    zones = {z["id"]: z for z in (r.get("internal") or {}).get("zones", [])}
    def hum(z: dict) -> str:  # % calibrado si existe; si no, el valor crudo del ADC (sin calibrar)
        if z.get("soil_moisture_vwc") is not None:
            return _f(z["soil_moisture_vwc"] * 100, "{:4.1f}%")
        return f"crudo {z['soil_moisture_raw']}" if z.get("soil_moisture_raw") is not None else "—"
    soil = "  ".join(f"{zid}:{hum(z)}/{_f(z.get('soil_temp_c'), '{:4.1f}°')}" for zid, z in sorted(zones.items()))
    ext = r.get("external") or {}
    bme, dht, ldr = ext.get("bme680") or {}, ext.get("dht11") or {}, ext.get("ldr") or {}
    air = f"BME {_f(bme.get('t_c'))}°C {_f(bme.get('rh_pct'), '{:.0f}')}%  DHT {_f(dht.get('t_c'), '{:.0f}')}°C  luz {_f(ldr.get('light_pct'), '{:.0f}')}%"
    flags = ",".join(r.get("quality_flags") or [])
    return (f"{r['ts'][11:19]}  {BADGE.get(r['source'], r['source']):<8} {r['node_id'][:14]:<14} "
            f"{soil}  |  {air}" + (f"  [{flags}]" if flags else ""))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--api", default="http://127.0.0.1:8000")
    ap.add_argument("--every", type=float, default=2.0)
    ap.add_argument("--source", choices=["simulated", "wokwi", "physical"])
    ap.add_argument("--last", type=int, default=10, help="cuántas lecturas previas mostrar al iniciar")
    a = ap.parse_args()
    base = a.api.rstrip("/")
    params = {"source": a.source} if a.source else {}
    print(f"Monitor del nodo · {base} · cada {a.every}s · Ctrl+C para salir")
    print("hora      origen   nodo           zonas (humedad % / temp. suelo)            |  aire y luz")
    seen: set[int] = set()
    first = True
    try:
        while True:
            try:
                body = requests.get(f"{base}/api/field-node/readings", params={**params, "limit": 200}, timeout=15).json()
                rows = body["readings"]
                new = [r for r in rows if r["id"] not in seen]
                if first:
                    new = new[-a.last:]
                    if not rows:
                        print("  (todavía no hay lecturas: esperando…)")
                for r in new:
                    print(line(r))
                seen.update(r["id"] for r in rows)
                first = False
            except requests.RequestException as exc:
                print(f"  ⚠ API no responde: {exc}")
            time.sleep(a.every)
    except KeyboardInterrupt:
        print("\nmonitor cerrado")


if __name__ == "__main__":
    main()
