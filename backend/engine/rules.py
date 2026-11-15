"""Candidatos de 12 meses y filtros de factibilidad (plan 3.5, regla R1).

Una secuencia es (cultivo de verano, cultivo de invierno). Cada exclusión guarda la regla y el motivo.
También convierte `current_rotation` ([actual, anterior], lo que envía la app) a verano/invierno.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from engine.reference import load_reference, param, value

SEASONS = ("summer", "winter")
SEASON_ES = {"summer": "verano", "winter": "invierno"}
# Días de cada ventana de campaña (crops.json → campaigns): verano 1 nov–30 abr, invierno 1 may–31 oct.
CAMPAIGN_DAYS = {"summer": 181, "winter": 184}
CROP_ES = {"soy": "soya", "maize": "maíz", "sorghum": "sorgo", "sunflower": "girasol", "wheat": "trigo",
           "fallow": "barbecho", "chia": "chía", "cover": "cobertura"}
NO_CROP = {"fallow"}
REQUIRED_PARAMS = {"kc_ini": "Kc inicial (FAO-56)", "kc_mid": "Kc medio (FAO-56)", "kc_end": "Kc final (FAO-56)",
                   "stage_days": "duración de etapas (FAO-56)", "cycle_days": "duración del ciclo",
                   "residue_class": "clase de residuo"}


@dataclass(frozen=True)
class Sequence:
    summer: str | None
    winter: str | None

    @property
    def key(self) -> str:
        return f"{self.summer or '?'}>{self.winter or '?'}"

    @property
    def complete(self) -> bool:
        return self.summer is not None and self.winter is not None

    def crops(self) -> list[tuple[str, str]]:
        """[(season, code)] de los cultivos reales (sin barbecho ni campañas vacías)."""
        return [(s, c) for s, c in (("summer", self.summer), ("winter", self.winter)) if c and c not in NO_CROP]

    def label(self) -> str:
        parts = [f"{CROP_ES.get(c, c) if c else 'sin dato'} ({SEASON_ES[s]})"
                 for s, c in (("summer", self.summer), ("winter", self.winter))]
        return " → ".join(parts)

    def as_api(self) -> list[dict]:
        return [{"season": s, "crop": c} for s, c in (("summer", self.summer), ("winter", self.winter))]


@dataclass
class Feasibility:
    feasible: list[Sequence] = field(default_factory=list)
    excluded: list[dict] = field(default_factory=list)          # {sequence, rule_id, reason}
    flags: dict[str, list[dict]] = field(default_factory=dict)  # key → [{rule_id, flag, text}]


def crop_seasons(code: str) -> list[str] | None:
    crop = load_reference()["crops"].get(code)
    return value(crop, "seasons") if crop else None


def check_crop(code: str, season: str, excluded_by_farmer: set[str]) -> tuple[str, str] | None:
    """Primer filtro que falla para `code` en `season`: (rule_id, motivo). None si pasa."""
    name = CROP_ES.get(code, code)
    if code in excluded_by_farmer:
        return "FARMER", f"Excluiste {name} en tus restricciones."
    seasons = crop_seasons(code)
    if not seasons:
        return "R1", f"{name.capitalize()}: no hay una campaña con fuente para este cultivo."
    if season not in seasons:
        return "R1", (f"{name.capitalize()} no es un cultivo de {SEASON_ES[season]} en Cuatro Cañadas "
                      f"(campañas con fuente: {', '.join(SEASON_ES[s] for s in seasons)}).")
    if code in NO_CROP:
        return None
    crop = load_reference()["crops"][code]
    missing = [label for k, label in REQUIRED_PARAMS.items() if value(crop, k) is None]
    if missing:
        return "PARAMS", f"{name.capitalize()}: faltan parámetros con fuente ({', '.join(missing)})."
    cycle = value(crop, "cycle_days")
    if cycle > CAMPAIGN_DAYS[season]:
        return "R1", (f"{name.capitalize()}: su ciclo ({cycle} días) no cabe en la campaña de "
                      f"{SEASON_ES[season]} ({CAMPAIGN_DAYS[season]} días).")
    return None


def water_flags(seq: Sequence, irrigation: str) -> list[dict]:
    """R1 (agua): en secano, un cultivo de invierno de alta demanda relativa se MARCA (no se excluye)."""
    if irrigation == "irrigated" or not seq.winter or seq.winter in NO_CROP:
        return []
    crop = load_reference()["crops"].get(seq.winter)
    kc_mid = value(crop, "kc_mid") if crop else None
    if kc_mid is None or kc_mid < param("high_demand_kc_mid"):
        return []
    name = CROP_ES.get(seq.winter, seq.winter)
    riego = "sin riego" if irrigation == "rainfed" else "sin riego confirmado"
    return [{"rule_id": "R1", "flag": "high_demand_winter_rainfed",
             "text": f"El {name} tiene una demanda de agua relativamente alta (Kc medio {kc_mid}, FAO-56) y se "
                     f"siembra en invierno, la época seca, {riego}."}]


def feasible_sequences(excluded_crops: list[str], irrigation: str) -> Feasibility:
    """Todas las combinaciones verano × invierno de crops.json, filtradas con motivo."""
    codes = list(load_reference()["crops"])
    farmer_excl = set(excluded_crops)
    out = Feasibility()
    for s in codes:
        for w in codes:
            seq = Sequence(s, w)
            fail = check_crop(s, "summer", farmer_excl) or check_crop(w, "winter", farmer_excl)
            if fail:
                out.excluded.append({"sequence": seq.key, "label": seq.label(), "rule_id": fail[0], "reason": fail[1]})
                continue
            if not seq.crops():
                out.excluded.append({"sequence": seq.key, "label": seq.label(), "rule_id": "R1",
                                     "reason": "Sin ningún cultivo en el año."})
                continue
            out.feasible.append(seq)
            flags = water_flags(seq, irrigation)
            if flags:
                out.flags[seq.key] = flags
    return out


def farmer_sequence(current_rotation: list[str]) -> tuple[Sequence, list[str]]:
    """[actual, anterior] → Sequence(verano, invierno) + avisos.

    Regla documentada en docs/contratos.md: cada cultivo va a la campaña que tiene en crops.json. Si los dos
    son de la misma campaña (o falta la campaña), no se adivina: la secuencia queda incompleta y se avisa.
    """
    issues: list[str] = []
    actual = current_rotation[0]
    previous = current_rotation[1] if len(current_rotation) > 1 else None
    if len(current_rotation) > 2:
        rest = ", ".join(CROP_ES.get(c, c) for c in current_rotation[2:])
        issues.append(f"Solo se usan el cultivo actual y el anterior; no se evalúan: {rest}.")
    sa, sp = crop_seasons(actual), (crop_seasons(previous) if previous else None)
    name_a, name_p = CROP_ES.get(actual, actual), CROP_ES.get(previous, previous)
    if previous is None:
        if sa and len(sa) == 1:
            seq = Sequence(actual, None) if sa[0] == "summer" else Sequence(None, actual)
        else:
            seq = Sequence(None, None)
            issues.append(f"No sabemos en qué campaña va {name_a}.")
        issues.append("No nos contaste el cultivo anterior: la otra campaña queda sin dato.")
        return seq, issues
    options = []
    if sa and sp:
        if "summer" in sa and "winter" in sp:
            options.append(Sequence(actual, previous))
        if "winter" in sa and "summer" in sp:
            options.append(Sequence(previous, actual))
    if len(options) == 1:
        return options[0], issues
    if not sa or not sp:
        unknown = name_a if not sa else name_p
        issues.append(f"No hay una campaña con fuente para {unknown}: no podemos ubicar tu rotación en el año.")
    elif len(options) == 0:
        both = SEASON_ES[sa[0]] if sa == sp and len(sa) == 1 else "la misma campaña"
        issues.append(f"Tu cultivo actual ({name_a}) y el anterior ({name_p}) son de {both}: no sabemos cómo se "
                      f"reparten en el año. Revisalo con tu técnico.")
    else:
        issues.append(f"{name_a.capitalize()} y {name_p} pueden ir en las dos campañas: confirmá cuál va en verano.")
    return Sequence(None, None), issues
