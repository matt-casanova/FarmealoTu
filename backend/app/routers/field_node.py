"""Nodo de campo (contrato v1, plan 9.2): recepción y lectura de mediciones (sim / Wokwi / físico).

ESQUELETO de Alvin: implementación real y simple (SQLite). Matías es el dueño y puede reemplazarla.
Reglas: el `source` se guarda y se devuelve siempre (la app lo muestra como insignia); los datos del nodo
NO alimentan el motor de recomendación en el MVP.
"""
from __future__ import annotations

import json
import os
import sqlite3
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Literal, Optional

from fastapi import APIRouter, Query, status

from app.schemas import SensorReading, StoredReading
from pipeline.status import power_context

router = APIRouter(prefix="/api/field-node", tags=["nodo de campo"])

DB_PATH = Path(os.environ.get("FARMEALOTU_FIELD_DB",
                              Path(__file__).resolve().parents[2] / "data" / "field_node.sqlite"))
_lock = threading.Lock()
_db: dict[str, sqlite3.Connection] = {}


def _conn() -> sqlite3.Connection:
    """Una conexión por archivo, reutilizada (protegida por _lock). `with c:` hace commit/rollback."""
    key = str(DB_PATH)
    if key in _db:
        return _db[key]
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    c = sqlite3.connect(DB_PATH, check_same_thread=False)
    _db[key] = c
    c.execute("""CREATE TABLE IF NOT EXISTS readings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        node_id TEXT NOT NULL, source TEXT NOT NULL, scenario TEXT,
        ts TEXT NOT NULL, ts_from_server INTEGER NOT NULL,
        received_at TEXT NOT NULL, payload TEXT NOT NULL)""")
    c.execute("CREATE INDEX IF NOT EXISTS ix_readings_ts ON readings(node_id, source, ts)")
    return c


def _now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


@router.post("/readings", status_code=status.HTTP_201_CREATED, response_model=StoredReading,
             summary="Recibe una lectura del nodo (ESP32 físico, Wokwi o gemelo digital)")
def post_reading(reading: SensorReading) -> StoredReading:
    received = _now()
    from_server = reading.ts is None
    ts = received if from_server else reading.ts.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    payload = reading.model_dump(mode="json")
    payload["ts"] = ts
    with _lock, _conn() as c:
        cur = c.execute("INSERT INTO readings(node_id, source, scenario, ts, ts_from_server, received_at, payload) "
                        "VALUES (?,?,?,?,?,?,?)",
                        (reading.node_id, reading.source, reading.scenario, ts, int(from_server), received,
                         json.dumps(payload, ensure_ascii=False)))
        rid = cur.lastrowid
    return StoredReading(stored=True, id=rid, received_at=received, ts_from_server=from_server)


@router.get("/readings", summary="Serie de lecturas (la app separa por `source`)")
def get_readings(
    node_id: Optional[str] = None,
    source: Optional[Literal["simulated", "wokwi", "physical"]] = None,
    since: Optional[datetime] = Query(None, description="ISO-8601; solo lecturas con ts posterior"),
    limit: int = Query(2000, ge=1, le=20000, description="máximo de lecturas (las más recientes)"),
):
    q, args = "SELECT id, ts, ts_from_server, received_at, payload FROM readings WHERE 1=1", []
    if node_id:
        q += " AND node_id = ?"; args.append(node_id)
    if source:
        q += " AND source = ?"; args.append(source)
    if since:
        q += " AND ts > ?"; args.append(since.astimezone(timezone.utc).isoformat().replace("+00:00", "Z"))
    q += " ORDER BY ts DESC, id DESC LIMIT ?"; args.append(limit)
    with _lock, _conn() as c:
        rows = c.execute(q, args).fetchall()
    readings = []
    for rid, ts, from_server, received, payload in reversed(rows):  # cronológico ascendente
        r = json.loads(payload)
        r.update({"id": rid, "received_at": received, "ts_from_server": bool(from_server)})
        readings.append(r)
    return {"count": len(readings), "readings": readings,
            "note": "Datos del nodo: prototipo de laboratorio o simulación según `source`. No alimentan el motor."}


@router.get("/status", summary="Último contacto del nodo + contexto regional NASA POWER")
def get_status(node_id: Optional[str] = None):
    q, args = "SELECT node_id, source, received_at, payload FROM readings", []
    if node_id:
        q += " WHERE node_id = ?"; args.append(node_id)
    q += " ORDER BY received_at DESC, id DESC LIMIT 1"
    with _lock, _conn() as c:
        last = c.execute(q, args).fetchone()
        counts = dict(c.execute("SELECT source, COUNT(*) FROM readings GROUP BY source").fetchall())
    ctx = power_context(datetime.now(timezone.utc).date().isoformat())
    if not last:
        return {"node_id": node_id, "status": "no_data", "last_contact": None, "counts_by_source": counts,
                "power_context": ctx}
    nid, src, received, payload = last
    p = json.loads(payload)
    return {"node_id": nid, "status": "ok", "last_contact": received, "source": src,
            "calibration": p.get("calibration"), "quality_flags": p.get("quality_flags", []),
            "counts_by_source": counts, "power_context": ctx}
