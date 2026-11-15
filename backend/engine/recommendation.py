"""Puntuación y selección de estrategias (plan 3.5, 3.6 y 3.8). Determinista: reglas + MCDA, sin ML ni LLM.

Subpuntajes (0–100):
    S_agua   = 100 · Σ min(Pef, ETc) / Σ ETc            (meses con cultivo; necesita Kc y ET0)
    S_suelo  = 100 · (0.4·C + 0.3·R + 0.3·D) − penalización por repetición
    S_riesgo = 100 · (1 − E), E = promedio ponderado de p_seca, p_calor (×0.5 con umbral genérico) y p_lluvia

Todo lo que depende del clima se calcula POR AÑO AGRÍCOLA (nov→oct) y se guarda como arreglo: el valor puntual
es el promedio de los años y el bootstrap (analytics/uncertainty.py) remuestrea esos mismos arreglos.
Los textos salen de plantillas llenadas con los números reales.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np
import pandas as pd

from analytics import indicators as I
from engine.reference import load_reference, param, value
from engine.rules import CAMPAIGN_DAYS, CROP_ES, NO_CROP, SEASON_ES, Sequence
from pipeline import power as P

CRITERIA = ("water", "soil", "risk")
CRITERIA_ES = {"water": "agua", "soil": "suelo", "risk": "riesgo"}
MONTHS_ES = ["", "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
             "noviembre", "diciembre"]
PROFILE = {
    "E0": "Tu rotación actual",
    "E1": "Cuidar suelo",
    "E2": "Cuidar agua",
    "E3": "Equilibrada",
}


# =========================================================================== clima por año agrícola
@dataclass
class CropClimate:
    """Exposición histórica de un cultivo en una campaña, un valor por año agrícola (NaN = sin dato)."""
    dry_flag: np.ndarray          # 1 si la racha seca máxima en la etapa crítica supera el umbral
    dry_spell: np.ndarray         # racha seca máxima (días) en la etapa crítica
    heat_frac: np.ndarray         # fracción de días de calor (Tmáx > P90 del mes) en la etapa crítica
    rain_flag: np.ndarray         # 1 si hubo ≥ 1 día > P95 en la ventana de cosecha
    water: np.ndarray | None      # (Σ min(Pef, ETc), Σ ETc) por año → shape (n, 2); None sin ET0
    crit_months: list[int]
    harvest_months: list[int]
    cycle_days: int


@dataclass
class Climate:
    df: pd.DataFrame
    years: list[int]                         # año de inicio de cada año agrícola (nov Y → oct Y+1)
    p90: pd.Series
    p95: pd.Series
    et0: pd.Series | None
    et0_reason: str | None
    pef: dict[tuple[int, int], float]
    _cache: dict = field(default_factory=dict)

    @property
    def n(self) -> int:
        return len(self.years)

    @property
    def period_label(self) -> str:
        return f"{self.years[0]}-{str(self.years[0] + 1)[2:]} → {self.years[-1]}-{str(self.years[-1] + 1)[2:]} " \
               f"({self.n} años agrícolas)"

    def sow_date(self, season: str, year: int) -> pd.Timestamp:
        return pd.Timestamp(f"{year}-11-01") if season == "summer" else pd.Timestamp(f"{year + 1}-05-01")

    def crop(self, code: str, season: str) -> CropClimate:
        key = (code, season)
        if key not in self._cache:
            self._cache[key] = self._compute_crop(code, season)
        return self._cache[key]

    def _compute_crop(self, code: str, season: str) -> CropClimate:
        crop = load_reference()["crops"][code]
        st = value(crop, "stage_days")
        ini, dev, mid, late = st["ini"], st["dev"], st["mid"], st["late"]
        cycle = ini + dev + mid + late
        thr = param("dry_spell_threshold_days")
        kc = _kc_curve(value(crop, "kc_ini"), value(crop, "kc_mid"), value(crop, "kc_end"), ini, dev, mid, late)
        dry_flag, dry_spell, heat, rain, water = [], [], [], [], []
        crit_months, harvest_months = set(), set()
        for y in self.years:
            sow = self.sow_date(season, y)
            c0, c1 = sow + pd.Timedelta(days=ini + dev), sow + pd.Timedelta(days=ini + dev + mid - 1)
            h0, h1 = sow + pd.Timedelta(days=ini + dev + mid), sow + pd.Timedelta(days=cycle - 1)
            crit, harv = I._window(self.df, c0, c1), I._window(self.df, h0, h1)
            crit_months.update(crit.index.month)
            harvest_months.update(harv.index.month)
            spell = I.max_dry_spell(crit["PRECTOTCORR"])
            dry_spell.append(np.nan if spell is None else spell)
            dry_flag.append(np.nan if spell is None else float(spell > thr))
            n_t = int(crit["T2M_MAX"].notna().sum())
            hd = I.count_heat_days(crit["T2M_MAX"], self.p90)
            heat.append(np.nan if hd is None or n_t == 0 else hd / n_t)
            hr = I.count_heavy_rain_days(harv["PRECTOTCORR"], self.p95)
            rain.append(np.nan if hr is None else float(hr >= 1))
            water.append(self._water_year(sow, kc) if self.et0 is not None else (np.nan, np.nan))
        return CropClimate(
            dry_flag=np.array(dry_flag), dry_spell=np.array(dry_spell), heat_frac=np.array(heat),
            rain_flag=np.array(rain), water=np.array(water) if self.et0 is not None else None,
            crit_months=sorted(crit_months, key=_agro_month_order), harvest_months=sorted(harvest_months,
                                                                                            key=_agro_month_order),
            cycle_days=cycle)

    def _water_year(self, sow: pd.Timestamp, kc: np.ndarray) -> tuple[float, float]:
        """(Σ min(Pef, ETc), Σ ETc) del ciclo. Pef mensual × fracción del mes con cultivo (supuesto declarado)."""
        days = pd.date_range(sow, periods=len(kc), freq="D")
        et0 = self.et0.reindex(days)
        if et0.isna().any():
            return np.nan, np.nan
        etc = pd.Series(kc * et0.to_numpy(), index=days)
        covered, demand = 0.0, 0.0
        for (yy, mm), etc_m in etc.groupby([etc.index.year, etc.index.month]):
            pef_m = self.pef.get((yy, mm))
            if pef_m is None or (isinstance(pef_m, float) and math.isnan(pef_m)):
                return np.nan, np.nan
            frac = len(etc_m) / pd.Period(f"{yy}-{mm:02d}").days_in_month
            covered += min(pef_m * frac, float(etc_m.sum()))
            demand += float(etc_m.sum())
        return covered, demand


def _agro_month_order(m: int) -> int:
    return (m - 11) % 12  # noviembre primero


def _kc_curve(kc_ini, kc_mid, kc_end, ini, dev, mid, late) -> np.ndarray:
    """Curva diaria de Kc de FAO-56 (constante, rampa, constante, rampa)."""
    return np.concatenate([
        np.full(ini, kc_ini),
        np.linspace(kc_ini, kc_mid, dev, endpoint=False),
        np.full(mid, kc_mid),
        np.linspace(kc_mid, kc_end, late),
    ])


def build_climate(df: pd.DataFrame) -> Climate:
    yearly = I.yearly_indicators(df)
    years = [int(s[:4]) for s in yearly["start"]]
    et0, reason = I.et0_series(df)
    pef = {(int(r.year), int(r.month)): r.effective_rain_mm for r in I.monthly_inputs(df).itertuples()}
    return Climate(df=df, years=years, p90=I.heat_thresholds_p90(df), p95=I.heavy_rain_thresholds_p95(df),
                   et0=et0, et0_reason=reason, pef=pef)


# =========================================================================== evaluación de una secuencia
@dataclass
class Evaluation:
    seq: Sequence
    soil: float | None
    soil_parts: dict
    risk_years: np.ndarray | None     # S_riesgo por año
    water_years: np.ndarray | None    # (cubierto, demanda) por año, para S_agua
    reasons_missing: dict[str, str]   # criterio → por qué falta
    covered_months: float | None
    risk_detail: dict

    def point(self) -> dict[str, float | None]:
        return {"water": water_score(self.water_years), "soil": self.soil, "risk": _nanmean(self.risk_years)}


def _nanmean(a: np.ndarray | None) -> float | None:
    if a is None or np.all(np.isnan(a)):
        return None
    return float(np.nanmean(a))


def water_score(water_years: np.ndarray | None, idx: np.ndarray | None = None) -> float | None:
    """S_agua = 100 · Σ min(Pef, ETc) / Σ ETc sobre los años (idx = años remuestreados)."""
    if water_years is None:
        return None
    w = water_years if idx is None else water_years[idx]
    if np.isnan(w).any() or w[:, 1].sum() <= 0:
        return None
    return float(100 * w[:, 0].sum() / w[:, 1].sum())


def _cover_fraction(seq: Sequence) -> float:
    """Fracción del año con suelo cubierto (cultivo o rastrojo). Día 0 = 1 de noviembre."""
    residue_days = param("residue_cover_days")
    covered = np.zeros(2 * 365, dtype=bool)
    starts = {"summer": 0, "winter": CAMPAIGN_DAYS["summer"]}
    for rep in (0, 365):
        for season, code in seq.crops():
            crop = load_reference()["crops"][code]
            sow = rep + starts[season]
            harvest = sow + value(crop, "cycle_days")
            end = min(harvest + residue_days[value(crop, "residue_class")], 2 * 365)
            covered[sow:end] = True
    return float(covered[365:].mean())


def evaluate(seq: Sequence, climate: Climate) -> Evaluation:
    missing: dict[str, str] = {}
    crops = seq.crops()
    ref = load_reference()["crops"]
    if not seq.complete:
        why = "No pudimos ubicar tu rotación en verano e invierno, así que no se puede puntuar."
        return Evaluation(seq, None, {}, None, None, {c: why for c in CRITERIA}, None, {})
    params_ok = all(all(value(ref[c], k) is not None for k in ("kc_ini", "kc_mid", "kc_end", "stage_days",
                                                               "residue_class")) for _, c in crops)
    if not params_ok:
        why = "Faltan parámetros con fuente (Kc, etapas o residuo) para algún cultivo de la secuencia."
        return Evaluation(seq, None, {}, None, None, {c: why for c in CRITERIA}, None, {})

    # ---- suelo
    fam = {value(ref[c], "family") for _, c in crops}
    c_frac = _cover_fraction(seq)
    r_vals = [param("residue_value")[value(ref[c], "residue_class")] for _, c in crops]
    r = float(np.mean(r_vals))
    d = (len(fam) - 1) / (len(CAMPAIGN_DAYS) - 1)
    repeated = seq.summer == seq.winter and seq.summer not in NO_CROP
    penalty = param("repetition_penalty_points") if repeated else 0
    soil = 100 * (param("soil_weight_cover") * c_frac + param("soil_weight_residue") * r
                  + param("soil_weight_diversity") * d) - penalty
    soil = float(min(100.0, max(0.0, soil)))
    soil_parts = {"C": round(c_frac, 3), "R": round(r, 3), "D": round(d, 3), "penalty": penalty,
                  "families": sorted(fam)}

    # ---- riesgo (por año)
    gw = param("generic_heat_weight")
    comps = {"dry": [], "heat": [], "rain": []}
    detail = {}
    for season, code in crops:
        cc = climate.crop(code, season)
        comps["dry"].append(cc.dry_flag)
        comps["heat"].append(cc.heat_frac)
        comps["rain"].append(cc.rain_flag)
        detail[code] = {"season": season, "climate": cc}
    means = {k: np.nanmean(np.vstack(v), axis=0) if v else None for k, v in comps.items()}
    weights = {"dry": 1.0, "heat": gw, "rain": 1.0}
    stack = np.vstack([means[k] * weights[k] for k in weights])
    wsum = np.vstack([np.where(np.isnan(means[k]), 0.0, weights[k]) for k in weights]).sum(axis=0)
    with np.errstate(invalid="ignore", divide="ignore"):
        e = np.nansum(stack, axis=0) / wsum
    risk_years = np.where(wsum > 0, 100 * (1 - e), np.nan)
    if np.all(np.isnan(risk_years)):
        missing["risk"] = "Sin datos de lluvia o temperatura en las etapas del cultivo."
        risk_years = None

    # ---- agua (por año)
    water_years = None
    if climate.et0 is None:
        missing["water"] = (f"Falta la evapotranspiración de referencia (ET0 FAO-56): {climate.et0_reason}. "
                            "Sin ET0 no se puede calcular la demanda de agua del cultivo.")
    else:
        water_years = np.sum([climate.crop(c, s).water for s, c in crops], axis=0)
        if water_score(water_years) is None:
            missing["water"] = "ET0 o lluvia efectiva incompletas en algún año."
            water_years = None
    return Evaluation(seq, soil, soil_parts, risk_years, water_years, missing, round(c_frac * 12, 1), detail)


# =========================================================================== MCDA
def mcda(point: dict[str, float | None], weights: dict[str, float]) -> tuple[float | None, bool, dict]:
    """Score con política de faltantes: el peso de un criterio faltante se reparte entre los válidos."""
    avail = {k: v for k, v in point.items() if v is not None}
    if not avail:
        return None, True, {}
    total = sum(weights[k] for k in avail)
    if total <= 0:
        return None, True, {}
    used = {k: weights[k] / total for k in avail}
    return float(sum(used[k] * avail[k] for k in avail)), len(avail) < len(CRITERIA), used


# =========================================================================== selección E0–E3
def select(e0: Evaluation, candidates: list[Evaluation]) -> tuple[dict[str, Evaluation], list[str]]:
    """E1 máx suelo (desempate agua) · E2 máx agua (desempate riesgo) · E3 maximin. Secuencias distintas."""
    chosen: dict[str, Evaluation] = {"E0": e0}
    used = {e0.seq.key} if e0.seq.complete else set()
    notes: list[str] = []
    neg = -math.inf

    def pick(eid: str, key, eligible=lambda ev: True, why_none: str = "") -> None:
        pool = [ev for ev in candidates if ev.seq.key not in used and eligible(ev)]
        if not pool:
            notes.append(f"No se propone «{PROFILE[eid]}» ({eid}): {why_none}")
            return
        best = max(pool, key=lambda ev: (key(ev), _neg_key(ev.seq.key)))
        chosen[eid] = best
        used.add(best.seq.key)

    pick("E1", lambda ev: (ev.point()["soil"] if ev.point()["soil"] is not None else neg,
                           ev.point()["water"] if ev.point()["water"] is not None else neg,
                           ev.point()["risk"] if ev.point()["risk"] is not None else neg),
         lambda ev: ev.point()["soil"] is not None,
         "ninguna otra secuencia factible tiene puntaje de suelo.")
    pick("E2", lambda ev: (ev.point()["water"], ev.point()["risk"] if ev.point()["risk"] is not None else neg),
         lambda ev: ev.point()["water"] is not None,
         "el puntaje de agua no se puede calcular (falta la ET0 FAO-56), así que no hay forma honesta de elegir "
         "la opción que menos agua pide.")
    pick("E3", lambda ev: (min(v for v in ev.point().values() if v is not None),),
         lambda ev: any(v is not None for v in ev.point().values()),
         "no quedan secuencias factibles distintas de las ya elegidas.")
    return chosen, notes


def _neg_key(key: str) -> tuple:
    """Desempate determinista: a igualdad de puntajes gana el orden alfabético de la secuencia."""
    return tuple(-ord(ch) for ch in key)
