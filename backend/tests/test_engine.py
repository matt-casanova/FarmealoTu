"""Motor de recomendación: reglas, conversión de la rotación, subpuntajes, MCDA y selección (plan 3.5–3.8)."""
import numpy as np
import pytest

from analytics import indicators as I
from engine import recommendation as RC
from engine.rules import Sequence, farmer_sequence, feasible_sequences


@pytest.fixture(scope="module")
def climate():
    return RC.build_climate(I.load_daily_frame())


# ---------------------------------------------------------------- reglas y factibilidad
def test_feasible_sequences_and_reasons():
    f = feasible_sequences([], "rainfed")
    keys = {s.key for s in f.feasible}
    assert keys == {"soy>maize", "soy>sorghum", "soy>sunflower", "soy>wheat", "soy>fallow"}
    assert len(f.feasible) + len(f.excluded) == 64
    by_key = {x["sequence"]: x for x in f.excluded}
    assert by_key["soy>chia"]["rule_id"] == "PARAMS"                 # chía sin Kc con fuente
    assert by_key["sorghum>soy"]["rule_id"] == "R1"                  # sorgo no es de verano
    assert by_key["soy>cover"]["rule_id"] == "R1" and "campaña" in by_key["soy>cover"]["reason"]
    assert all(x["reason"] for x in f.excluded)


def test_farmer_exclusions_are_recorded():
    f = feasible_sequences(["maize", "wheat"], "rainfed")
    assert {s.key for s in f.feasible} == {"soy>sorghum", "soy>sunflower", "soy>fallow"}
    assert any(x["sequence"] == "soy>maize" and x["rule_id"] == "FARMER" for x in f.excluded)


def test_excluding_soy_leaves_no_summer_crop():
    f = feasible_sequences(["soy"], "rainfed")
    assert f.feasible == []                                           # no se rellena: se explica


def test_rainfed_high_demand_winter_is_flagged_not_excluded():
    f = feasible_sequences([], "rainfed")
    assert "soy>maize" in f.flags and f.flags["soy>maize"][0]["rule_id"] == "R1"
    assert "soy>sorghum" not in f.flags                               # Kc medio 1.05 < 1.15
    assert feasible_sequences([], "irrigated").flags == {}


@pytest.mark.parametrize("rotation,summer,winter,issues", [
    (["soy", "sorghum"], "soy", "sorghum", 0),       # [actual, anterior]: actual de verano
    (["sorghum", "soy"], "soy", "sorghum", 0),       # actual de invierno
    (["soy", "fallow"], "soy", "fallow", 0),
    (["sorghum", "wheat"], None, None, 1),           # los dos de invierno → no se adivina
    (["soy"], "soy", None, 1),                       # sin anterior
    (["soy", "cover"], None, None, 1),               # cobertura sin campaña con fuente
])
def test_farmer_sequence(rotation, summer, winter, issues):
    seq, msgs = farmer_sequence(rotation)
    assert (seq.summer, seq.winter) == (summer, winter)
    assert len(msgs) == issues


# ---------------------------------------------------------------- subpuntajes
def test_scores_are_in_range_and_deterministic(climate):
    a = RC.evaluate(Sequence("soy", "sorghum"), climate).point()
    b = RC.evaluate(Sequence("soy", "sorghum"), climate).point()
    assert a == b
    assert 0 <= a["soil"] <= 100 and 0 <= a["risk"] <= 100


def test_soil_formula_components(climate):
    ev = RC.evaluate(Sequence("soy", "sorghum"), climate)
    p = ev.soil_parts
    expected = 100 * (0.4 * p["C"] + 0.3 * p["R"] + 0.3 * p["D"])
    assert ev.soil == pytest.approx(expected, abs=0.1)
    assert p["D"] == 1.0 and p["R"] == pytest.approx((0.2 + 1.0) / 2)   # soya bajo, sorgo alto
    fallow = RC.evaluate(Sequence("soy", "fallow"), climate)
    assert fallow.soil_parts["D"] == 0.0 and fallow.soil < ev.soil      # barbecho: menos cobertura y diversidad


def test_repetition_penalty(climate, monkeypatch):
    from engine import reference as R
    base = RC.evaluate(Sequence("soy", "sorghum"), climate)
    # simulamos que el sorgo también fuera de verano para evaluar la repetición sorgo → sorgo
    rep = RC.evaluate(Sequence("sorghum", "sorghum"), climate)
    assert rep.soil_parts["penalty"] == R.param("repetition_penalty_points")
    assert rep.soil_parts["D"] == 0.0
    assert base.soil_parts["penalty"] == 0


def test_risk_uses_years_of_critical_stage(climate):
    ev = RC.evaluate(Sequence("soy", "sorghum"), climate)
    assert ev.risk_years.shape == (climate.n,) and climate.n == 11
    cc = climate.crop("sorghum", "winter")
    assert cc.crit_months and set(cc.crit_months) <= set(range(1, 13))
    assert np.nanmax(cc.dry_flag) <= 1 and np.nanmin(cc.dry_flag) >= 0


def test_water_is_insufficient_without_et0(climate):
    assert climate.et0 is None                                        # physics.py todavía no existe
    ev = RC.evaluate(Sequence("soy", "sorghum"), climate)
    assert ev.point()["water"] is None and "ET0" in ev.reasons_missing["water"]


def test_water_score_formula():
    w = np.array([[50.0, 100.0], [100.0, 100.0]])                     # (min(Pef, ETc), ETc) por año
    assert RC.water_score(w) == pytest.approx(75.0)
    assert RC.water_score(w, np.array([1, 1])) == pytest.approx(100.0)
    assert RC.water_score(np.array([[np.nan, 1.0]])) is None


# ---------------------------------------------------------------- MCDA y faltantes
def test_mcda_redistributes_missing_weight():
    score, partial, used = RC.mcda({"water": None, "soil": 80.0, "risk": 60.0},
                                   {"water": 0.5, "soil": 0.25, "risk": 0.25})
    assert partial and used == {"soil": 0.5, "risk": 0.5} and score == pytest.approx(70.0)
    full, partial2, _ = RC.mcda({"water": 50.0, "soil": 80.0, "risk": 60.0}, {"water": 0.4, "soil": 0.35, "risk": 0.25})
    assert not partial2 and full == pytest.approx(0.4 * 50 + 0.35 * 80 + 0.25 * 60)
    assert RC.mcda({"water": None, "soil": None, "risk": None}, {"water": 1, "soil": 0, "risk": 0})[0] is None


def test_selection_distinct_and_no_water_profile_without_et0(climate):
    e0 = RC.evaluate(Sequence("soy", "sorghum"), climate)
    cands = [RC.evaluate(s, climate) for s in feasible_sequences([], "rainfed").feasible]
    chosen, notes = RC.select(e0, cands)
    keys = [ev.seq.key for ev in chosen.values()]
    assert len(keys) == len(set(keys))                                # secuencias distintas
    assert "E2" not in chosen and any("E2" in n for n in notes)       # sin ET0 no se elige «Cuidar agua»
    soil_e1 = chosen["E1"].point()["soil"]
    assert all(soil_e1 >= ev.point()["soil"] for ev in cands if ev.seq.key != e0.seq.key)
