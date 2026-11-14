"""Esquemas Pydantic de la API FarmealoTu.

ESQUELETO de Alvin (rama alvin/main-esqueleto) mientras Matías no puede trabajar. Matías es el dueño:
puede conservarlo, cambiarlo o reemplazarlo. Fuentes de las formas:
- Nodo de campo: contrato v1, plan sección 9.2 (= shared/sensor_reading.schema.json).
- /rotations/compare: plan 4.8 y docs/UI_BACKEND_CONTRACT.md (lo que envía la app de Kendra).
"""
from __future__ import annotations

from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

EvidenceStatus = Literal["documented", "derived", "assumption", "farmer_provided", "unavailable"]
CropCode = Literal["soy", "maize", "sorghum", "sunflower", "wheat", "fallow", "chia", "cover"]


# =========================================================================== nodo de campo (v1)
class Zone(BaseModel):
    model_config = ConfigDict(extra="allow")
    id: str = Field(..., min_length=1, max_length=8, examples=["A"])
    soil_moisture_vwc: Optional[float] = Field(None, ge=0, le=1, description="m³/m³ (fracción 0–1)")
    soil_moisture_raw: Optional[int] = Field(None, ge=0, le=65535)
    soil_temp_c: Optional[float] = Field(None, ge=-40, le=85)
    flags: list[str] = Field(default_factory=list)


class Internal(BaseModel):
    model_config = ConfigDict(extra="allow")
    depth_cm: Optional[float] = Field(None, ge=0, le=200)
    zones: list[Zone] = Field(default_factory=list, max_length=12)


class Bme680(BaseModel):
    model_config = ConfigDict(extra="allow")
    t_c: Optional[float] = Field(None, ge=-40, le=85)
    rh_pct: Optional[float] = Field(None, ge=0, le=100)
    p_hpa: Optional[float] = Field(None, ge=300, le=1100)
    gas_kohm: Optional[float] = Field(None, ge=0)


class Dht11(BaseModel):
    model_config = ConfigDict(extra="allow")
    t_c: Optional[float] = Field(None, ge=-20, le=60)
    rh_pct: Optional[float] = Field(None, ge=0, le=100)


class Ldr(BaseModel):
    model_config = ConfigDict(extra="allow")
    raw: Optional[int] = Field(None, ge=0, le=65535)
    mv: Optional[float] = Field(None, ge=0, le=5000)
    light_pct: Optional[float] = Field(None, ge=0, le=100)
    r_fixed_ohm: Optional[float] = Field(None, gt=0)


class External(BaseModel):
    model_config = ConfigDict(extra="allow")
    bme680: Optional[Bme680] = None
    dht11: Optional[Dht11] = None
    ldr: Optional[Ldr] = None


class Calibration(BaseModel):
    model_config = ConfigDict(extra="allow")
    moisture_method: Optional[str] = Field(None, examples=["gravimetric_3pt"])
    date: Optional[str] = None
    ds18b20_offsets_c: Optional[list[float]] = None


class SensorReading(BaseModel):
    """Lectura del nodo de campo, contrato v1 (plan 9.2): grupos `internal` (en la tierra) y `external`."""
    model_config = ConfigDict(extra="allow", json_schema_extra={"examples": [{
        "schema_version": "1.0", "node_id": "rotasat-node-01", "source": "simulated", "scenario": "S4",
        "ts": "2026-11-14T20:15:00Z",
        "internal": {"depth_cm": 7, "zones": [{"id": "A", "soil_moisture_vwc": 0.11, "soil_moisture_raw": 2710,
                                               "soil_temp_c": 24.6, "flags": []}]},
        "external": {"bme680": {"t_c": 26.1, "rh_pct": 48.2, "p_hpa": 965.0, "gas_kohm": 85.4},
                     "dht11": {"t_c": 26.0, "rh_pct": 50.0},
                     "ldr": {"raw": 1830, "mv": 1480, "light_pct": 45.0, "r_fixed_ohm": 2200}},
        "calibration": {"moisture_method": "gravimetric_3pt", "date": "2026-11-14", "ds18b20_offsets_c": [0.1, -0.2, 0.0]},
        "quality_flags": []}]})

    schema_version: str = Field("1.0", pattern=r"^1\.\d+$")
    node_id: str = Field(..., min_length=1, max_length=64)
    source: Literal["simulated", "wokwi", "physical"]
    scenario: Optional[str] = Field(None, max_length=16)
    ts: Optional[datetime] = Field(None, description="UTC ISO-8601 (NTP). Si falta, se usa received_at")
    internal: Internal = Field(default_factory=Internal)
    external: External = Field(default_factory=External)
    calibration: Optional[Calibration] = None
    quality_flags: list[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def _scenario_only_simulated(self):
        if self.scenario and self.source != "simulated":
            raise ValueError("scenario solo aplica cuando source = 'simulated'")
        return self


class StoredReading(BaseModel):
    stored: bool
    id: int
    received_at: str
    ts_from_server: bool


# =========================================================================== /rotations/compare
class Site(BaseModel):
    lat: float = Field(-17.2374, ge=-90, le=90)
    lon: float = Field(-62.2427, ge=-180, le=180)


class Priorities(BaseModel):
    water: float = Field(0.40, ge=0, le=1)
    soil: float = Field(0.35, ge=0, le=1)
    risk: float = Field(0.25, ge=0, le=1)

    @model_validator(mode="after")
    def _sum_positive(self):
        if self.water + self.soil + self.risk <= 0:
            raise ValueError("Las prioridades no pueden ser todas 0")
        return self

    def normalized(self) -> dict[str, float]:
        s = self.water + self.soil + self.risk
        return {k: round(v / s, 4) for k, v in (("water", self.water), ("soil", self.soil), ("risk", self.risk))}


class Constraints(BaseModel):
    irrigation: Literal["irrigated", "rainfed", "partial", "unknown"] = "unknown"
    excluded_crops: list[CropCode] = Field(default_factory=list)


class CompareRequest(BaseModel):
    model_config = ConfigDict(json_schema_extra={"examples": [{
        "site": {"lat": -17.2374, "lon": -62.2427}, "current_rotation": ["soy", "sorghum"],
        "priorities": {"water": 0.40, "soil": 0.35, "risk": 0.25},
        "constraints": {"irrigation": "rainfed", "excluded_crops": []},
        "notes": "Mi terreno suele secarse en invierno"}]})

    site: Site = Field(default_factory=Site)
    current_rotation: list[CropCode] = Field(..., min_length=1, max_length=4)
    priorities: Priorities = Field(default_factory=Priorities)
    constraints: Constraints = Field(default_factory=Constraints)
    notes: Optional[str] = Field(None, max_length=2000)

    @field_validator("current_rotation")
    @classmethod
    def _not_all_fallow(cls, v):
        if all(c == "fallow" for c in v):
            raise ValueError("La rotación actual necesita al menos un cultivo")
        return v
