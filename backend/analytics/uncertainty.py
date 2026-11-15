"""Incertidumbre por bootstrap de años climáticos (plan 3.7).

- Se remuestrean AÑOS AGRÍCOLAS COMPLETOS (nov→oct) con reemplazo: n años por iteración, nunca días sueltos.
- 200 iteraciones con semilla fija (seed = 42): el resultado es reproducible.
- En cada iteración se recalculan S_agua, S_riesgo (S_suelo no depende del clima) y el Score de cada estrategia
  con los mismos pesos y la misma política de faltantes que el valor puntual.
- Salidas: P10, P50, P90 por estrategia, estabilidad del ranking (fracción de iteraciones en que queda primera)
  y P(A > B) por pares.

Todo está vectorizado con NumPy sobre los arreglos por año que arma engine/recommendation.py: no se vuelve a leer
la serie diaria, así que el costo es de milisegundos.
"""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np

N_ITER = 200
SEED = 42
MESSAGE = "P10–P90 muestra cuánto varía el puntaje según el año; no es una garantía de rendimiento ni un pronóstico."


@dataclass(frozen=True)
class StrategyInputs:
    """Lo que el bootstrap necesita de una estrategia (todo por año agrícola)."""
    soil: float | None
    risk_years: np.ndarray | None          # shape (n,)
    water_years: np.ndarray | None         # shape (n, 2): (Σ min(Pef, ETc), Σ ETc)


def resample_indices(n_years: int, n_iter: int = N_ITER, seed: int = SEED) -> np.ndarray:
    """Índices de años remuestreados con reemplazo: shape (n_iter, n_years)."""
    return np.random.default_rng(seed).integers(0, n_years, size=(n_iter, n_years))


def _criteria_boot(s: StrategyInputs, idx: np.ndarray) -> dict[str, np.ndarray | None]:
    n_iter = idx.shape[0]
    out: dict[str, np.ndarray | None] = {"soil": None, "risk": None, "water": None}
    if s.soil is not None:
        out["soil"] = np.full(n_iter, float(s.soil))
    if s.risk_years is not None and not np.all(np.isnan(s.risk_years)):
        with np.errstate(invalid="ignore"):
            out["risk"] = np.nanmean(s.risk_years[idx], axis=1)
    if s.water_years is not None and not np.isnan(s.water_years).any():
        w = s.water_years[idx]                         # (n_iter, n_years, 2)
        dem = w[..., 1].sum(axis=1)
        with np.errstate(invalid="ignore", divide="ignore"):
            out["water"] = np.where(dem > 0, 100 * w[..., 0].sum(axis=1) / dem, np.nan)
    return out


def score_samples(s: StrategyInputs, weights: dict[str, float], idx: np.ndarray) -> np.ndarray | None:
    """Score por iteración con la política de faltantes (peso de lo que falta repartido entre lo disponible)."""
    crit = {k: v for k, v in _criteria_boot(s, idx).items() if v is not None}
    total = sum(weights[k] for k in crit)
    if not crit or total <= 0:
        return None
    return sum((weights[k] / total) * crit[k] for k in crit)


def bootstrap(strategies: dict[str, StrategyInputs], weights: dict[str, float], n_years: int,
              n_iter: int = N_ITER, seed: int = SEED) -> dict:
    """{"per_strategy": {id: {p10, p50, p90, rank_stability}}, "pairwise": {"A>B": P}, "n_iter", "seed"}."""
    idx = resample_indices(n_years, n_iter, seed)
    samples = {sid: score_samples(s, weights, idx) for sid, s in strategies.items()}
    ranked = [sid for sid, v in samples.items() if v is not None]
    per: dict[str, dict] = {}
    if ranked:
        mat = np.vstack([samples[sid] for sid in ranked])          # (n_strat, n_iter)
        # Empate en una iteración: la «victoria» se reparte entre las empatadas (no se premia a ninguna).
        is_top = np.isclose(mat, mat.max(axis=0), rtol=0, atol=1e-9)
        wins = (is_top / is_top.sum(axis=0)).sum(axis=1) / n_iter
    for sid, v in samples.items():
        if v is None:
            per[sid] = {"p10": None, "p50": None, "p90": None, "rank_stability": None}
            continue
        p10, p50, p90 = np.percentile(v, [10, 50, 90])
        per[sid] = {"p10": float(p10), "p50": float(p50), "p90": float(p90),
                    "rank_stability": float(wins[ranked.index(sid)])}
    pairwise = {}
    for a in ranked:
        for b in ranked:
            if a != b:
                pairwise[f"{a}>{b}"] = float(np.mean(samples[a] > samples[b]))
    return {"per_strategy": per, "pairwise": pairwise, "n_iter": n_iter, "seed": seed, "n_years": n_years}


def confidence(rank_stability: float | None, missing_criteria: list[str], soil_info: bool) -> tuple[str, str]:
    """Política 3.7: máximo «medium» con datos solo regionales; «low» con estabilidad < 0.5, un criterio
    faltante o sin información de suelo."""
    causes = []
    if rank_stability is None:
        causes.append("no hay puntaje para comparar")
    elif rank_stability < 0.5:
        causes.append(f"queda primera solo en el {100 * rank_stability:.0f} % de los remuestreos (menos de la mitad)")
    if missing_criteria:
        causes.append("falta el criterio de " + " y ".join(missing_criteria))
    if not soil_info:
        causes.append("no hay análisis de suelo de tu campo")
    if causes:
        return "low", "Confianza baja: " + "; ".join(causes) + "."
    return "medium", ("Confianza media: es el máximo con datos solo regionales (NASA POWER, ~50 km) y parámetros "
                      "de cultivo genéricos.")
