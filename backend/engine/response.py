"""Arma la respuesta de POST /api/rotations/compare (contrato de docs/UI_BACKEND_CONTRACT.md, sección 3).

Todos los textos se generan con plantillas a partir de los números y las reglas reales: no hay texto aleatorio.
"""
from __future__ import annotations

from datetime import date

import numpy as np

from analytics import indicators as I
from analytics import uncertainty as U
from engine.recommendation import (CRITERIA, CRITERIA_ES, MONTHS_ES, PROFILE, Climate, Evaluation, build_climate,
                                   evaluate, mcda, select)
from engine.reference import load_reference, param, value
from engine.rules import CROP_ES, NO_CROP, SEASON_ES, Feasibility, Sequence, farmer_sequence, feasible_sequences
from pipeline import power as P

ECONOMY = "No se evalúan precios ni costos."
NO_SOIL = "Falta un análisis de suelo de tu campo."
NO_SMAP_NDVI = "SMAP y NDVI se ven en el mapa, pero no entran en el puntaje."
GENERIC_HEAT = "El calor usa un umbral general, no uno propio del cultivo."
UNVERIFIED_FAO = "Valores de cultivo: tablas generales de FAO-56."
RELATIVE = "Los puntajes (0–100) comparan opciones; no son probabilidad de éxito."
SCALE = P.SCALE_LABEL


def _r(x: float | None, nd: int = 1) -> float | None:
    return None if x is None else round(float(x), nd)


def _months_text(months: list[int]) -> str:
    if not months:
        return "sin dato"
    if len(months) == 1:
        return MONTHS_ES[months[0]]
    return f"{MONTHS_ES[months[0]]} a {MONTHS_ES[months[-1]]}"


def _name(code: str | None) -> str:
    return CROP_ES.get(code, code) if code else "sin dato"


# =========================================================================== bloques de la tarjeta
def _crop_risk_facts(ev: Evaluation, climate: Climate) -> list[dict]:
    """Hechos de exposición por cultivo, ordenados del más expuesto al menos expuesto."""
    thr = param("dry_spell_threshold_days")
    facts = []
    for code, info in ev.risk_detail.items():
        cc = info["climate"]
        valid = int(np.sum(~np.isnan(cc.dry_flag)))
        k_dry = int(np.nansum(cc.dry_flag))
        k_rain = int(np.nansum(cc.rain_flag))
        valid_rain = int(np.sum(~np.isnan(cc.rain_flag)))
        heat = float(np.nanmean(cc.heat_frac)) if not np.all(np.isnan(cc.heat_frac)) else None
        facts.append({
            "code": code, "season": info["season"], "k_dry": k_dry, "n": valid, "k_rain": k_rain, "n_rain": valid_rain,
            "heat_pct": None if heat is None else round(100 * heat, 1), "crit": _months_text(cc.crit_months),
            "harvest": _months_text(cc.harvest_months), "thr": thr,
            "spell_mean": None if np.all(np.isnan(cc.dry_spell)) else round(float(np.nanmean(cc.dry_spell)), 1),
        })
    return sorted(facts, key=lambda f: (f["k_dry"] / f["n"] if f["n"] else 0), reverse=True)


def _criteria(ev: Evaluation, climate: Climate, irrigation: str) -> dict:
    pt = ev.point()
    out = {}
    if pt["water"] is None:
        out["water"] = {"score": None, "detail": ev.reasons_missing.get("water", "Sin datos."), "unit": "%",
                        "status": "insufficient"}
    else:
        detail = (f"La lluvia efectiva cubre en promedio el {pt['water']:.0f} % de la demanda de agua de los cultivos "
                  f"({climate.period_label}).")
        if irrigation == "irrigated":
            detail += " El riego no se suma como agua gratuita: el puntaje mide solo lo que aporta la lluvia."
        out["water"] = {"score": _r(pt["water"]), "detail": detail, "unit": "%", "status": "ok"}
    if pt["soil"] is None:
        out["soil"] = {"score": None, "detail": ev.reasons_missing.get("soil", "Sin datos."),
                       "unit": "puntos (0–100)", "status": "insufficient"}
    else:
        ref = load_reference()["crops"]
        residues = ", ".join(f"{_name(c)} {_residue_es(value(ref[c], 'residue_class'))}" for _, c in ev.seq.crops())
        fam = len(ev.soil_parts["families"])
        detail = (f"Suelo cubierto {ev.covered_months:g} de 12 meses (cultivo o rastrojo); residuo: {residues}; "
                  f"{fam} {'familia' if fam == 1 else 'familias'} de cultivos.")
        if ev.soil_parts["penalty"]:
            detail += f" Repite el mismo cultivo: −{ev.soil_parts['penalty']} puntos."
        out["soil"] = {"score": _r(pt["soil"]), "detail": detail, "unit": "puntos (0–100)", "status": "ok"}
    if pt["risk"] is None:
        out["risk"] = {"score": None, "detail": ev.reasons_missing.get("risk", "Sin datos."),
                       "unit": "puntos (0–100)", "status": "insufficient"}
    else:
        parts = [f"{_name(f['code'])}: racha seca de más de {f['thr']} días en la floración ({f['crit']}) en "
                 f"{f['k_dry']} de {f['n']} años" for f in _crop_risk_facts(ev, climate)]
        out["risk"] = {"score": _r(pt["risk"]), "detail": "Más alto = menos exposición histórica. " + "; ".join(parts)
                       + ".", "unit": "puntos (0–100)", "status": "ok"}
    return out


def _residue_es(cls: str | None) -> str:
    return {"high": "alto", "medium": "medio", "low": "bajo"}.get(cls, "sin dato")


def _soil_why(ev: Evaluation, e0: Evaluation) -> str:
    """El componente de S_suelo que más explica la diferencia con la rotación actual."""
    a, b = ev.soil_parts, e0.soil_parts
    ref = load_reference()["crops"]
    contrib = {
        "C": 100 * param("soil_weight_cover") * (a["C"] - b["C"]),
        "R": 100 * param("soil_weight_residue") * (a["R"] - b["R"]),
        "D": 100 * param("soil_weight_diversity") * (a["D"] - b["D"]),
        "P": -(a["penalty"] - b["penalty"]),
    }
    k = max(contrib, key=lambda x: abs(contrib[x]))
    if k == "C":
        return f"cubre el suelo {ev.covered_months:g} de 12 meses (tu rotación: {e0.covered_months:g})"
    if k == "R":
        res = lambda e: ", ".join(f"{_name(c)} {_residue_es(value(ref[c], 'residue_class'))}" for _, c in e.seq.crops())
        return f"deja {'más' if contrib['R'] > 0 else 'menos'} rastrojo (residuo: {res(ev)}; tu rotación: {res(e0)})"
    if k == "D":
        return (f"alterna {len(a['families'])} familias de cultivos (tu rotación: {len(b['families'])})")
    return "no repite el mismo cultivo" if contrib["P"] > 0 else "repite el mismo cultivo en las dos campañas"


def _risk_why(ev: Evaluation, e0: Evaluation, climate: Climate) -> str:
    fa, fb = _crop_risk_facts(ev, climate), _crop_risk_facts(e0, climate)
    if not fa or not fb:
        return "cambia la exposición a rachas secas, calor y lluvia intensa en las etapas sensibles"
    a, b = fa[0], fb[0]
    return (f"su etapa más expuesta es la floración de {_name(a['code'])} ({a['crit']}), con rachas secas largas en "
            f"{a['k_dry']} de {a['n']} años (tu rotación: {_name(b['code'])}, {b['k_dry']} de {b['n']})")


def _compare_sentence(k: str, ev: Evaluation, e0: Evaluation, climate: Climate) -> str | None:
    a, b = ev.point()[k], e0.point()[k]
    if a is None or b is None:
        return None
    diff = a - b
    if abs(diff) < 1:
        return f"Igual que tu rotación en {CRITERIA_ES[k]} ({a:.0f} vs {b:.0f})."
    why = {
        "soil": lambda: _soil_why(ev, e0),
        "water": lambda: f"la lluvia efectiva cubre el {a:.0f} % de la demanda (tu rotación: {b:.0f} %)",
        "risk": lambda: _risk_why(ev, e0, climate),
    }[k]()
    word = "Mejor" if diff > 0 else "Peor"
    return f"{word} en {CRITERIA_ES[k]} ({a:.0f} vs {b:.0f} de tu rotación) porque {why}."


def _reasons(eid: str, ev: Evaluation, e0: Evaluation, climate: Climate, farmer_issues: list[str]) -> list[str]:
    reasons: list[str] = []
    if eid == "E0":
        reasons.append(f"Es la rotación que nos contaste: {ev.seq.label() if ev.seq.complete else 'sin ubicar'}.")
        reasons += farmer_issues
        if ev.covered_months is not None:
            reasons.append(f"Tu rotación actual cubre el suelo {ev.covered_months:g} de 12 meses.")
        facts = _crop_risk_facts(ev, climate)
        if facts and facts[0]["n"]:
            f = facts[0]
            reasons.append(f"En {f['crit']} ({_name(f['code'])} en floración) queda más expuesta a rachas secas: "
                           f"{f['k_dry']} de {f['n']} años con más de {f['thr']} días seguidos sin lluvia útil.")
        return reasons
    reasons.append({
        "E1": "Es la opción factible con el puntaje de suelo más alto (sin contar tu rotación).",
        "E2": "Es la opción factible con el puntaje de agua más alto (sin contar las ya propuestas).",
        "E3": "Es la opción más pareja entre las que quedan (sin repetir tu rotación ni las ya propuestas): su "
              "criterio más bajo es el más alto. No es la mejor en nada, pero tampoco es débil en ningún criterio.",
    }[eid])
    reasons.append(f"Secuencia: {ev.seq.label()}.")
    sentences = [s for k in ("soil", "water", "risk") if (s := _compare_sentence(k, ev, e0, climate))]
    if sentences and all(s.startswith("Igual") for s in sentences):
        reasons.append("Con los datos disponibles puntúa igual que tu rotación: con los parámetros de FAO-56 que "
                       "tenemos, las dos secuencias tienen el mismo ciclo, la misma clase de residuo y las mismas "
                       "familias. La diferencia real hay que verla con tu técnico.")
    reasons += sentences
    return reasons


def _tradeoffs(ev: Evaluation, e0: Evaluation, climate: Climate) -> dict | None:
    pt = ev.point()
    avail = {k: v for k, v in pt.items() if v is not None}
    if not avail:
        return None
    best, worst = max(avail, key=avail.get), min(avail, key=avail.get)
    facts = _crop_risk_facts(ev, climate)
    text = {
        "soil": f"Protege el suelo: queda cubierto {ev.covered_months:g} de 12 meses.",
        "risk": "Menor exposición histórica a rachas secas, calor y lluvia intensa en las etapas sensibles.",
        "water": f"La lluvia efectiva cubre el {pt['water']:.0f} % de lo que piden los cultivos."
        if pt["water"] is not None else "",
    }
    top = facts[0] if facts else None
    weak = {
        "soil": f"El suelo queda sin cultivo ni rastrojo unos {round(12 - ev.covered_months, 1):g} meses al año."
        if ev.covered_months is not None else "",
        "risk": (f"Su punto más débil es el riesgo: la floración de {_name(top['code'])} ({top['crit']}) cae en la "
                 f"época de rachas secas largas.") if top else "Su punto más débil es el riesgo climático.",
        "water": f"La lluvia efectiva cubre solo el {pt['water']:.0f} % de lo que piden los cultivos."
        if pt["water"] is not None else "",
    }
    if ev.seq.winter in NO_CROP:
        weak["soil"] = "En invierno el lote queda en barbecho: sin cultivo y con poco rastrojo."
    main_risk = None
    if facts:
        f = facts[0]
        main_risk = (f"{_name(f['code']).capitalize()} en floración ({f['crit']}): en {f['k_dry']} de {f['n']} años "
                     f"hubo más de {f['thr']} días seguidos sin lluvia útil (NASA POWER, contexto regional).")
        if f["k_rain"]:
            main_risk += (f" Lluvia intensa en la cosecha ({f['harvest']}) en {f['k_rain']} de {f['n_rain']} años "
                          f"(exposición, no pronóstico de inundación).")
    return {"advantage": text[best], "disadvantage": weak[worst] if best != worst else
            "Ningún criterio se destaca por debajo de los demás.", "main_risk": main_risk}


def _missing(ev: Evaluation, partial: bool, farmer_issues: list[str], eid: str) -> list[str]:
    items = []
    for k in CRITERIA:
        if k in ev.reasons_missing:
            items.append(f"Puntaje de {CRITERIA_ES[k]} insuficiente: {ev.reasons_missing[k]}")
    if partial and any(k in ev.reasons_missing for k in CRITERIA):
        items.append("Puntaje parcial: falta un criterio.")
    if eid == "E0":
        items += farmer_issues
    items += [NO_SOIL, NO_SMAP_NDVI, ECONOMY]
    return items


def _questions(ev: Evaluation, flags: list[dict]) -> list[str]:
    ref = load_reference()["crops"]
    qs = []
    for f in flags:
        if f["flag"] == "high_demand_winter_rainfed":
            qs.append(f"¿Alcanza el agua de invierno para el {_name(ev.seq.winter)} sin riego en tu campo?")
    for _, c in ev.seq.crops():
        if value(ref[c], "residue_class") == "low":
            qs.append(f"¿Se puede dejar más rastrojo o una cobertura después de la {_name(c)}?")
            break
    if ev.seq.winter in NO_CROP:
        qs.append("¿Conviene dejar el lote en barbecho en invierno o sembrar un cultivo que cubra el suelo?")
    if any(value(ref[c], "fixes_n") for _, c in ev.seq.crops()):
        qs.append("¿Cuánto nitrógeno deja la soya en tu suelo? No le sumamos un valor fijo: hay que medirlo.")
    qs.append("¿Qué análisis de suelo conviene hacer antes de decidir?")
    return qs[:3]


def _evidence(ev: Evaluation, climate: Climate) -> list[dict]:
    out = []
    for f in _crop_risk_facts(ev, climate):
        name = _name(f["code"])
        base = {"period": climate.period_label, "scale": SCALE}
        if f["n"]:
            out.append({**base, "indicator": f"dry_spell_critical_{f['code']}",
                        "name": f"Años con racha seca > {f['thr']} días en la floración de {name} ({f['crit']})",
                        "value": round(100 * f["k_dry"] / f["n"], 1), "unit": "% de años",
                        "source": "NASA POWER PRECTOTCORR", "evidence_status": "derived"})
        if f["spell_mean"] is not None:
            out.append({**base, "indicator": f"dry_spell_mean_{f['code']}",
                        "name": f"Racha seca máxima media en la floración de {name}", "value": f["spell_mean"],
                        "unit": "días", "source": "NASA POWER PRECTOTCORR", "evidence_status": "derived"})
        if f["heat_pct"] is not None:
            out.append({**base, "indicator": f"heat_days_critical_{f['code']}",
                        "name": f"Días de calor en la floración de {name} (umbral genérico P90)",
                        "value": f["heat_pct"], "unit": "% de días", "source": "NASA POWER T2M_MAX",
                        "evidence_status": "derived"})
        if f["n_rain"]:
            out.append({**base, "indicator": f"heavy_rain_harvest_{f['code']}",
                        "name": f"Años con lluvia intensa (> P95) en la cosecha de {name} ({f['harvest']})",
                        "value": round(100 * f["k_rain"] / f["n_rain"], 1), "unit": "% de años",
                        "source": "NASA POWER PRECTOTCORR", "evidence_status": "derived"})
    return out


# =========================================================================== respuesta completa
def _alerts(ind: dict) -> list[dict]:
    alerts = []
    thr = param("alert_rain_anomaly_pct")
    an = next((i for i in ind["indicators"] if i["id"] == "rain_anomaly"), None)
    if an and an.get("value") is not None:
        v = an["value"]
        level = "high" if abs(v) >= thr["high"] else "medium" if abs(v) >= thr["medium"] else "low"
        word = "por encima" if v > 0 else "por debajo"
        alerts.append({"message": f"La lluvia de este período está {word} de lo habitual ({v:+.0f} %, {an['period']}, "
                                  f"NASA POWER, contexto regional).",
                       "level": level, "date": ind["last_observation_date"], "source": an.get("source"),
                       "evidence_status": an["evidence_status"]})
    dry = next((i for i in ind["indicators"] if i["id"] == "dry_spell_max"), None)
    if dry and dry.get("value") is not None and (dry.get("reference") or {}).get("value") is not None:
        ref = dry["reference"]["value"]
        level = "medium" if dry["value"] > ref else "low"
        alerts.append({"message": f"Racha seca más larga del período: {dry['value']} días (media de las mismas "
                                  f"fechas 2015–2025: {ref} días).",
                       "level": level, "date": ind["last_observation_date"], "source": dry.get("source"),
                       "evidence_status": dry["evidence_status"]})
    return alerts


def build_compare_response(req, df, ind_recent: dict, nasa_status: dict, climate: Climate | None = None) -> dict:
    climate = climate or build_climate(df)
    weights = req.priorities.normalized()
    irrigation = req.constraints.irrigation
    feas: Feasibility = feasible_sequences(list(req.constraints.excluded_crops), irrigation)
    e0_seq, farmer_issues = farmer_sequence(list(req.current_rotation))
    e0 = evaluate(e0_seq, climate)
    if e0_seq.complete and e0_seq.key not in {s.key for s in feas.feasible}:
        why = next((x["reason"] for x in feas.excluded if x["sequence"] == e0_seq.key), None)
        if why:
            farmer_issues.append(f"Tu rotación no pasa los filtros de factibilidad ({why}); se evalúa igual, con "
                                 f"las mismas fórmulas, como punto de comparación.")
    candidates = [evaluate(s, climate) for s in feas.feasible]
    chosen, selection_notes = select(e0, candidates)

    water_for_all = all(ev.point()["water"] is None for ev in [e0, *candidates])
    boot = U.bootstrap({eid: U.StrategyInputs(ev.soil, ev.risk_years, ev.water_years) for eid, ev in chosen.items()},
                       weights, climate.n)
    strategies, scores = [], {}
    for eid, ev in chosen.items():
        point, partial, used = mcda(ev.point(), weights)
        b = boot["per_strategy"][eid]
        score = b["p50"] if b["p50"] is not None else point          # plan 3.8: score mediano del bootstrap
        scores[eid] = score
        flags = feas.flags.get(ev.seq.key, [])
        missing_es = [CRITERIA_ES[k] for k in CRITERIA if ev.point()[k] is None]
        level, reason = U.confidence(b["rank_stability"], missing_es, soil_info=False)
        uncertainty = None if b["p50"] is None else {
            "p10": _r(b["p10"]), "p50": _r(b["p50"]), "p90": _r(b["p90"]),
            "rank_stability": round(b["rank_stability"], 3),
            "rank_basis": "puntaje parcial (" + " y ".join(CRITERIA_ES[k] for k in used) + ")" if partial
            else "puntaje completo",
            "prob_better_than_current": None if eid == "E0" else _r(boot["pairwise"].get(f"{eid}>E0"), 3),
            "confidence_level": level, "confidence_reason": reason,
            "n_iterations": boot["n_iter"], "seed": boot["seed"], "note": U.MESSAGE,
        }
        strategies.append({
            "id": eid, "name": PROFILE[eid],
            "sequence": ev.seq.as_api() if ev.seq.complete else list(req.current_rotation),
            "score": _r(score), "score_point": _r(point), "partial_score": partial,
            "weights_used": {k: round(v, 4) for k, v in used.items()},
            "criteria": _criteria(ev, climate, irrigation),
            "uncertainty": uncertainty,
            "tradeoffs": _tradeoffs(ev, e0, climate),
            "reasons": _reasons(eid, ev, e0, climate, farmer_issues),
            "evidence": _evidence(ev, climate),
            "missing_information": _missing(ev, partial, farmer_issues, eid),
            "agronomist_questions": _questions(ev, flags),
            "flags": flags,
        })

    if water_for_all:
        recommended = None
        ranking_note = ("Sin puntaje de agua para ninguna opción (falta la ET0 FAO-56) no hay ranking global: las "
                        "opciones se comparan solo por suelo y riesgo, de forma cualitativa.")
    else:
        ranked = sorted(((s, eid) for eid, s in scores.items() if s is not None), key=lambda t: (-t[0], t[1]))
        recommended = ranked[0][1] if ranked else None
        ranking_note = None

    by_rule: dict[str, int] = {}
    for x in feas.excluded:
        by_rule[x["rule_id"]] = by_rule.get(x["rule_id"], 0) + 1
    rule_names = {"R1": "estación o ciclo incompatible (R1)", "PARAMS": "parámetros sin fuente",
                  "FARMER": "excluidos por vos"}
    excl_text = "; ".join(f"{n} por {rule_names.get(r, r)}" for r, n in sorted(by_rule.items()))
    limitations = [load_reference()["rules"]["R7"]["limitation_text"], RELATIVE]
    if ranking_note:
        limitations.append(ranking_note)
    limitations += selection_notes
    limitations.append(f"Combinaciones verano × invierno: {len(feas.feasible)} factibles de "
                       f"{len(feas.feasible) + len(feas.excluded)}.")
    limitations += [U.MESSAGE, UNVERIFIED_FAO]
    if water_for_all:
        limitations.append("La estabilidad del ranking se calcula sobre el puntaje parcial (suelo y riesgo): sirve "
                           "para ver qué tan firme es la comparación, no es un orden recomendado.")
    limitations += ["Datos de NASA POWER: zona de ~50 km, no tu campo.", GENERIC_HEAT]

    alerts = _alerts(ind_recent)
    if climate.et0 is None:
        alerts.append({"message": "Falta la evapotranspiración de referencia (ET0 FAO-56): no se puede comparar cuánta "
                                  "agua pide cada opción, así que no hay un orden recomendado.",
                       "level": "medium", "date": date.today().isoformat(), "source": "FarmealoTu",
                       "evidence_status": "unavailable"})

    ref = load_reference()
    return {
        "site": {"lat": req.site.lat, "lon": req.site.lon, "label": ind_recent["site"]["label"]},
        "data_status": {
            "last_observation_date": nasa_status.get("last_observation_date"),
            "last_successful_download": nasa_status.get("last_download"),
            "source": "NASA POWER (caché)",
            "engine": "ready" if climate.et0 is not None else "partial",
            "engine_detail": [] if climate.et0 is not None else
            ["S_agua pendiente: falta la ET0 FAO-56 (physics.py)"],
            "weights_received": weights,
            "years_used": climate.period_label,
            "uncertainty": {
                "method": "bootstrap de años agrícolas completos (nov→oct) con reemplazo",
                "n_iterations": boot["n_iter"], "seed": boot["seed"], "years_per_iteration": boot["n_years"],
                "pairwise_prob_better": {k: round(v, 3) for k, v in boot["pairwise"].items()},
                "note": U.MESSAGE,
            },
            "et0": {"status": "available" if climate.et0 is not None else "unavailable",
                    "reason": climate.et0_reason},
            "assumptions": {k: {"value": v["value"], "note": v["note"], "evidence_status": v["evidence_status"]}
                            for k, v in ref["params"].items()},
            "feasibility": {
                "evaluated": len(feas.feasible) + len(feas.excluded),
                "feasible": [s.label() for s in feas.feasible],
                "excluded": feas.excluded,
            },
        },
        "alerts": alerts,
        "strategies": strategies,
        "recommended_strategy_id": recommended,
        "limitations": limitations,
    }
