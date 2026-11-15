"""Carga y valida la base agrícola de referencia (backend/reference/*.json).

Regla de producto: ningún número sin fuente. Cada dato es un objeto
{value, source, url, year, evidence_status, note, verified_at}. Si value es null, evidence_status
debe ser «unavailable» y note debe decir por qué. Un número suelto fuera de esa forma es un error.
"""
from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

REFERENCE_DIR = Path(__file__).resolve().parents[1] / "reference"
EVIDENCE = {"documented", "derived", "assumption", "farmer_provided", "unavailable"}
PROVENANCE = ("source", "url", "year", "evidence_status", "note", "verified_at")
# Campos que necesita el motor para un cultivo candidato (los demás pueden faltar).
REQUIRED_CROP_FIELDS = ("family", "seasons", "fixes_n", "residue_class", "drought_tolerance", "heat_threshold_c",
                        "kc_ini", "kc_mid", "kc_end", "stage_days", "cycle_days", "root_depth_m",
                        "critical_stage", "harvest_window", "synonyms")


class ReferenceDataError(ValueError):
    """La base de referencia no cumple la regla «número con fuente o null con motivo»."""

    def __init__(self, problems: list[str]):
        self.problems = problems
        super().__init__("Base de referencia inválida:\n- " + "\n- ".join(problems))


def is_param(obj: Any) -> bool:
    return isinstance(obj, dict) and "value" in obj and "evidence_status" in obj


def _is_number(x: Any) -> bool:
    return isinstance(x, (int, float)) and not isinstance(x, bool)


def _contains_number(x: Any) -> bool:
    if _is_number(x):
        return True
    if isinstance(x, dict):
        return any(_contains_number(v) for v in x.values())
    if isinstance(x, list):
        return any(_contains_number(v) for v in x)
    return False


def _check_param(path: str, prm: dict, problems: list[str]) -> None:
    missing = [k for k in PROVENANCE if k not in prm]
    if missing:
        problems.append(f"{path}: faltan campos de procedencia {missing}")
    status = prm.get("evidence_status")
    if status not in EVIDENCE:
        problems.append(f"{path}: evidence_status inválido ({status!r})")
    if not (isinstance(prm.get("note"), str) and prm["note"].strip()):
        problems.append(f"{path}: falta note")
    if prm.get("value") is None:
        if status != "unavailable":
            problems.append(f"{path}: value null exige evidence_status 'unavailable'")
    else:
        if not prm.get("source") or not prm.get("url"):
            problems.append(f"{path}: valor sin source/url")
        if not _is_number(prm.get("year")):
            problems.append(f"{path}: valor sin year")
        if status == "unavailable":
            problems.append(f"{path}: un valor presente no puede ser 'unavailable'")


def _walk(path: str, obj: Any, problems: list[str]) -> None:
    """Recorre un cultivo: todo número debe vivir dentro del `value` (o `year`) de un parámetro."""
    if is_param(obj):
        _check_param(path, obj, problems)
        extra = {k: v for k, v in obj.items() if k not in ("value", "year", *PROVENANCE, "unit")}
        for k, v in extra.items():
            if _contains_number(v):
                problems.append(f"{path}.{k}: número fuera de 'value'")
        return
    if _is_number(obj):
        problems.append(f"{path}: número sin fuente ({obj})")
    elif isinstance(obj, dict):
        for k, v in obj.items():
            _walk(f"{path}.{k}", v, problems)
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            _walk(f"{path}[{i}]", v, problems)


def validate_crops(doc: dict) -> list[str]:
    problems: list[str] = []
    seen_codes: set[str] = set()
    for crop in doc.get("crops", []):
        code = crop.get("code")
        if not code or code in seen_codes:
            problems.append(f"código de cultivo vacío o repetido: {code!r}")
        seen_codes.add(code)
        for k, v in crop.items():
            _walk(f"crops.{code}.{k}", v, problems)
        if crop.get("kind") is None:  # cultivo propiamente dicho
            for f in REQUIRED_CROP_FIELDS:
                if f not in crop:
                    problems.append(f"crops.{code}: falta el campo {f}")
    for name, camp in doc.get("campaigns", {}).items():
        if camp.get("evidence_status") not in EVIDENCE or not camp.get("note"):
            problems.append(f"campaigns.{name}: falta evidence_status o note")
    return problems


def validate_rules(doc: dict) -> list[str]:
    problems = []
    ids = [r.get("id") for r in doc.get("rules", [])]
    if ids != [f"R{i}" for i in range(1, 8)]:
        problems.append(f"rotation_rules: se esperaban R1–R7 en orden, hay {ids}")
    for r in doc.get("rules", []):
        for k in ("id", "text", "effect", "rationale"):
            if not r.get(k):
                problems.append(f"rotation_rules.{r.get('id')}: falta {k}")
    return problems


def validate_params(doc: dict) -> list[str]:
    problems: list[str] = []
    for k, prm in doc.get("params", {}).items():
        if not is_param(prm):
            problems.append(f"engine_params.{k}: no tiene la forma {{value, source, …}}")
            continue
        _walk(f"engine_params.{k}", prm, problems)
        if prm.get("evidence_status") != "assumption":
            problems.append(f"engine_params.{k}: los parámetros de diseño deben rotularse 'assumption'")
    return problems


def _read(name: str) -> dict:
    return json.loads((REFERENCE_DIR / name).read_text(encoding="utf-8"))


@lru_cache(maxsize=1)
def load_reference() -> dict:
    """Devuelve {"crops", "campaigns", "rules", "params"} o lanza ReferenceDataError."""
    crops_doc, rules_doc, params_doc = _read("crops.json"), _read("rotation_rules.json"), _read("engine_params.json")
    problems = validate_crops(crops_doc) + validate_rules(rules_doc) + validate_params(params_doc)
    if problems:
        raise ReferenceDataError(problems)
    return {
        "crops": {c["code"]: c for c in crops_doc["crops"]},
        "campaigns": crops_doc["campaigns"],
        "rules": {r["id"]: r for r in rules_doc["rules"]},
        "rules_source": {k: rules_doc[k] for k in ("source", "url", "year")},
        "params": params_doc["params"],
    }


def param(name: str) -> Any:
    """Valor de un parámetro de diseño de engine_params.json."""
    return load_reference()["params"][name]["value"]


def value(crop: dict, field: str) -> Any:
    """Valor de un parámetro del cultivo (None si falta o es null)."""
    prm = crop.get(field)
    return prm.get("value") if is_param(prm) else None
