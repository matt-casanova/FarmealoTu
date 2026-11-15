"""Base agrícola de referencia: cada número tiene fuente o es null con motivo (plan 3.3 y 3.5)."""
import copy
import json

import pytest

from engine import reference as R


@pytest.fixture(scope="module")
def crops_doc():
    return json.loads((R.REFERENCE_DIR / "crops.json").read_text(encoding="utf-8"))


def test_reference_loads_without_problems():
    ref = R.load_reference()
    assert {"soy", "maize", "sorghum", "sunflower", "wheat", "chia", "fallow", "cover"} <= set(ref["crops"])
    assert list(ref["rules"]) == [f"R{i}" for i in range(1, 8)]


def test_every_number_has_source_or_is_null_with_reason(crops_doc):
    assert R.validate_crops(crops_doc) == []
    params = []

    def collect(obj):
        if R.is_param(obj):
            params.append(obj)
        elif isinstance(obj, dict):
            for v in obj.values():
                collect(v)
        elif isinstance(obj, list):
            for v in obj:
                collect(v)
    collect(crops_doc["crops"])
    assert params
    for p in params:
        if p["value"] is None:
            assert p["evidence_status"] == "unavailable" and p["note"].strip()
        else:
            assert p["source"] and p["url"] and isinstance(p["year"], int)
            assert p["evidence_status"] in {"documented", "derived", "assumption"}


def test_validator_rejects_number_without_source(crops_doc):
    bad = copy.deepcopy(crops_doc)
    bad["crops"][0]["kc_mid"] = 1.15                                    # número suelto
    assert any("número sin fuente" in p for p in R.validate_crops(bad))
    bad2 = copy.deepcopy(crops_doc)
    bad2["crops"][0]["kc_mid"]["source"] = None                         # valor sin fuente
    assert any("sin source/url" in p for p in R.validate_crops(bad2))
    bad3 = copy.deepcopy(crops_doc)
    bad3["crops"][0]["kc_mid"].update(value=None, evidence_status="documented")  # null que no dice «unavailable»
    assert any("value null" in p for p in R.validate_crops(bad3))


def test_unverified_values_are_flagged_not_hidden(crops_doc):
    """Lo transcrito de memoria queda con verified_at = null y la advertencia en note (nada se da por verificado)."""
    soy = next(c for c in crops_doc["crops"] if c["code"] == "soy")
    assert soy["kc_mid"]["value"] == 1.15 and soy["kc_mid"]["verified_at"] is None
    assert "VERIFICAR" in soy["kc_mid"]["note"]


def test_no_crop_specific_value_is_invented(crops_doc):
    by = {c["code"]: c for c in crops_doc["crops"]}
    for code in ("soy", "maize", "sorghum", "sunflower", "wheat", "chia"):
        assert by[code]["heat_threshold_c"]["value"] is None          # sin umbral de calor con fuente
        assert by[code]["drought_tolerance"]["value"] is None         # EcoCrop no consultado
    assert by["chia"]["kc_mid"]["value"] is None and by["chia"]["priority"] is False
    assert by["cover"]["kc_mid"]["value"] is None                      # cobertura sin especie → fuera
    assert by["soy"]["fixes_n"]["value"] is True and "crédito fijo" in by["soy"]["fixes_n"]["note"]


def test_rules_have_text_effect_and_rationale():
    rules = R.load_reference()["rules"]
    for r in rules.values():
        assert r["text"] and r["effect"] and r["rationale"]
    assert "inundación" in rules["R7"]["limitation_text"]
    assert rules["R6"]["effect"] == "qualitative_only"
