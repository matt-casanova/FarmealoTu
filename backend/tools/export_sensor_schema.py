"""Genera shared/sensor_reading.schema.json desde el modelo Pydantic (una sola fuente de verdad).

Uso (desde backend/):  python -m tools.export_sensor_schema
Christian (export.py) y Mateo (validación de la salida serie) validan contra ese archivo con `jsonschema`.
"""
import json
from pathlib import Path

from app.schemas import SensorReading

OUT = Path(__file__).resolve().parents[2] / "shared" / "sensor_reading.schema.json"

if __name__ == "__main__":
    schema = SensorReading.model_json_schema()
    schema["$schema"] = "https://json-schema.org/draft/2020-12/schema"
    schema["$id"] = "https://github.com/matt-casanova/FarmealoTu/shared/sensor_reading.schema.json"
    schema["title"] = "FarmealoTu · lectura del nodo de campo v1 (plan 9.2)"
    schema["description"] = ("Grupos: internal = sensores en la tierra (humedad capacitiva v1.2, DS18B20); "
                             "external = fuera de la tierra (BME680, DHT11, LDR). Unidades en el nombre del campo. "
                             "Datos faltantes: null + bandera, nunca un valor inventado. "
                             "Regla adicional (no expresable aquí): scenario solo si source = 'simulated'.")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(schema, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"escrito {OUT}")
