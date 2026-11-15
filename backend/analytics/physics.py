"""ET0 de referencia FAO-56 Penman–Monteith (Allen et al., 1998, ecuación 6), diaria.

    ET0 = [0.408·Δ·(Rn − G) + γ·900/(T + 273)·u2·(es − ea)] / [Δ + γ·(1 + 0.34·u2)]      (mm/día)

Entradas diarias (NASA POWER, caché local; esta función nunca llama a NASA):
    T2M_MAX, T2M_MIN (°C) · RH2M (%) · WS2M (m/s, a 2 m) · ALLSKY_SFC_SW_DWN (Rs, MJ/m²/día)
    + latitud y elevación del punto (metadatos de POWER).

Decisiones (todas de FAO-56, capítulo 3):
- T media = (Tmax + Tmin)/2 (ec. 9); es = [e°(Tmax) + e°(Tmin)]/2 (ec. 12).
- ea con la humedad relativa MEDIA (ec. 19), porque POWER no da RHmax/RHmin. Si se pasan, se usa la ec. 17.
- Rso = (0.75 + 2·10⁻⁵·z)·Ra (ec. 37); Rns con albedo 0.23 (ec. 38); Rnl por ec. 39 con 0.3 ≤ Rs/Rso ≤ 1
  (el piso 0.3 es de ASCE-EWRI 2005, la versión estandarizada de la misma ecuación).
- G = 0 a escala diaria (ec. 42). Presión por elevación (ec. 7).
- Radiación faltante (POWER publica Rs con días de atraso y a veces deja un día suelto vacío): se estima con el
  método que FAO-56 indica para ese caso, Hargreaves (ec. 50): Rs = kRs·√(Tmax − Tmin)·Ra, kRs = 0.16 (interior).
  Solo si hay temperaturas; los días estimados quedan listados en `serie.attrs["rs_estimated_dates"]`.
- Un día con temperatura, humedad o viento faltante da NaN (nunca se rellena). Negativos (rarísimos) → 0.

Validación: tests/test_physics.py reproduce el ejemplo 18 de FAO-56 (Uccle, 6-jul: ET0 = 3.9 mm/día) y la Ra del
ejemplo 8, y compara la serie de Cuatro Cañadas con una implementación independiente (pyet), si está instalada.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

ALBEDO = 0.23            # cultivo de referencia hipotético (FAO-56)
KRS_INTERIOR = 0.16      # coeficiente de Hargreaves para radiación faltante, sitio interior (FAO-56, ec. 50)
SIGMA = 4.903e-9         # Stefan-Boltzmann, MJ K⁻⁴ m⁻² día⁻¹
GSC = 0.0820             # constante solar, MJ m⁻² min⁻¹


def sat_vapour_pressure(t_c):
    """e°(T) en kPa (ec. 11)."""
    return 0.6108 * np.exp(17.27 * t_c / (t_c + 237.3))


def slope_vapour_curve(t_c):
    """Δ en kPa/°C (ec. 13)."""
    return 4098.0 * sat_vapour_pressure(t_c) / (t_c + 237.3) ** 2


def psychrometric_constant(elevation_m: float) -> float:
    """γ en kPa/°C a partir de la presión por elevación (ec. 7 y 8)."""
    p = 101.3 * ((293.0 - 0.0065 * elevation_m) / 293.0) ** 5.26
    return 0.665e-3 * p


def extraterrestrial_radiation(lat_deg: float, doy):
    """Ra en MJ/m²/día (ec. 21–25). Latitud en grados (sur negativa), doy = día del año."""
    phi = np.radians(lat_deg)
    j = np.asarray(doy, dtype=float)
    dr = 1 + 0.033 * np.cos(2 * np.pi / 365 * j)
    delta = 0.409 * np.sin(2 * np.pi / 365 * j - 1.39)
    ws = np.arccos(np.clip(-np.tan(phi) * np.tan(delta), -1.0, 1.0))
    return 24 * 60 / np.pi * GSC * dr * (ws * np.sin(phi) * np.sin(delta) + np.cos(phi) * np.cos(delta) * np.sin(ws))


def et0_pm(tmax, tmin, u2, rs, lat_deg: float, elevation_m: float, doy, rh_mean=None, rh_max=None, rh_min=None):
    """ET0 FAO-56 PM diaria (mm/día), vectorizada. Usa RHmax/RHmin (ec. 17) si se dan; si no, RH media (ec. 19)."""
    tmax, tmin, u2, rs = (np.asarray(x, dtype=float) for x in (tmax, tmin, u2, rs))
    tmean = (tmax + tmin) / 2
    es = (sat_vapour_pressure(tmax) + sat_vapour_pressure(tmin)) / 2
    if rh_max is not None and rh_min is not None:
        ea = (sat_vapour_pressure(tmin) * np.asarray(rh_max, float) / 100
              + sat_vapour_pressure(tmax) * np.asarray(rh_min, float) / 100) / 2
    else:
        ea = np.asarray(rh_mean, dtype=float) / 100 * es
    delta = slope_vapour_curve(tmean)
    gamma = psychrometric_constant(elevation_m)
    ra = extraterrestrial_radiation(lat_deg, doy)
    rso = (0.75 + 2e-5 * elevation_m) * ra
    rns = (1 - ALBEDO) * rs
    # Rs/Rso acotado a [0.3, 1.0]: FAO-56 pide ≤ 1.0; el piso 0.3 es de ASCE-EWRI (2005) y evita que, en días muy
    # nublados (Rs/Rso < 0.26), la radiación de onda larga neta salga negativa (el cielo «aportando» energía).
    rel = np.clip(np.where(rso > 0, rs / rso, np.nan), 0.3, 1.0)
    rnl = (SIGMA * ((tmax + 273.16) ** 4 + (tmin + 273.16) ** 4) / 2
           * (0.34 - 0.14 * np.sqrt(ea)) * (1.35 * rel - 0.35))
    rn = rns - rnl
    g = 0.0
    num = 0.408 * delta * (rn - g) + gamma * 900.0 / (tmean + 273.0) * u2 * (es - ea)
    den = delta + gamma * (1 + 0.34 * u2)
    return np.maximum(num / den, 0.0)


def et0_fao56_daily(df: pd.DataFrame, elevation_m: float | None) -> pd.Series:
    """Serie diaria de ET0 (mm/día) para el DataFrame de NASA POWER (índice = fecha). NaN si falta algún dato."""
    loc = (df.attrs.get("meta") or {}).get("location") or {}
    lat = loc.get("lat")
    if lat is None:
        raise ValueError("falta la latitud del punto en los metadatos de POWER")
    if elevation_m is None:
        elevation_m = loc.get("elevation_m")
    if elevation_m is None:
        raise ValueError("falta la elevación del punto en los metadatos de POWER")
    need = ["T2M_MAX", "T2M_MIN", "RH2M", "WS2M", "ALLSKY_SFC_SW_DWN"]
    missing_cols = [c for c in need if c not in df.columns]
    if missing_cols:
        raise ValueError(f"faltan columnas de POWER: {missing_cols}")
    rs = df["ALLSKY_SFC_SW_DWN"].astype(float).copy()
    temps_ok = df["T2M_MAX"].notna() & df["T2M_MIN"].notna()
    fill = rs.isna() & temps_ok
    if fill.any():                                      # FAO-56 ec. 50 (Hargreaves) solo para Rs faltante
        ra = extraterrestrial_radiation(float(lat), df.index.day_of_year)
        est = KRS_INTERIOR * np.sqrt((df["T2M_MAX"] - df["T2M_MIN"]).clip(lower=0)) * ra
        rs[fill] = est[fill.to_numpy()]
    et0 = et0_pm(df["T2M_MAX"], df["T2M_MIN"], df["WS2M"], rs, float(lat), float(elevation_m),
                 df.index.day_of_year, rh_mean=df["RH2M"])
    out = pd.Series(et0, index=df.index, name="ET0_mm")
    incomplete = df[["T2M_MAX", "T2M_MIN", "RH2M", "WS2M"]].isna().any(axis=1) | rs.isna()
    out[incomplete] = np.nan                            # día incompleto = faltante, nunca relleno
    out.attrs["rs_estimated_dates"] = [d.date().isoformat() for d in df.index[fill & ~incomplete]]
    out.attrs["rs_method_missing"] = "FAO-56 ec. 50 (Hargreaves), kRs = 0.16"
    return out
