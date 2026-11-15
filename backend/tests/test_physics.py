"""ET0 FAO-56 Penman–Monteith (analytics/physics.py) contra los ejemplos resueltos de FAO-56 y la serie real."""
import numpy as np
import pandas as pd
import pytest

from analytics import indicators as I
from analytics import physics as PH


def test_ra_ejemplo_8_fao56():
    # FAO-56, ejemplo 8: 20° S, 3 de septiembre (J = 246) → Ra = 32.2 MJ/m²/día
    assert PH.extraterrestrial_radiation(-20.0, 246) == pytest.approx(32.2, abs=0.1)


def test_et0_ejemplo_18_fao56_uccle():
    # FAO-56, ejemplo 18: Uccle (Bruselas), 50°48' N, 100 m, 6 de julio (J = 187).
    # Tmax 21.5, Tmin 12.3, RHmax 84, RHmin 63, u2 = 2.078 m/s, Rs = 22.07 MJ/m²/día → ET0 = 3.9 mm/día.
    et0 = PH.et0_pm(21.5, 12.3, 2.078, 22.07, lat_deg=50.80, elevation_m=100, doy=187, rh_max=84, rh_min=63)
    assert float(et0) == pytest.approx(3.9, abs=0.05)
    # pasos intermedios publicados en el ejemplo
    assert PH.psychrometric_constant(100) == pytest.approx(0.0666, abs=0.0005)
    assert PH.extraterrestrial_radiation(50.80, 187) == pytest.approx(41.09, abs=0.1)


def test_dia_incompleto_es_nan():
    idx = pd.date_range("2026-01-01", periods=3, freq="D")
    df = pd.DataFrame({"T2M_MAX": [33, 34, 35], "T2M_MIN": [22, 23, np.nan], "RH2M": [70, 65, 60],
                       "WS2M": [1.5, 2.0, 1.8], "ALLSKY_SFC_SW_DWN": [22, np.nan, 24]}, index=idx)
    df.attrs["meta"] = {"location": {"lat": -17.237, "elevation_m": 276.15}}
    s = PH.et0_fao56_daily(df, None)
    assert np.isfinite(s.iloc[0]) and s.iloc[0] > 0
    assert np.isfinite(s.iloc[1])                        # falta solo la radiación: FAO-56 ec. 50 (Hargreaves)
    assert s.attrs["rs_estimated_dates"] == ["2026-01-02"]
    assert np.isnan(s.iloc[2])                           # falta Tmin: no se puede estimar → faltante


def test_serie_real_cuatro_canadas():
    df = I.load_daily_frame()
    et0, reason = I.et0_series(df)
    assert reason is None and et0 is not None
    valid = et0.dropna()
    assert len(valid) > 4000
    assert 0.2 < valid.min() and valid.max() < 12          # rango físico plausible (mín. real 0.42: surazo nublado)
    assert 3.0 < valid.mean() < 6.0                         # media diaria anual típica de llanura tropical
    # sin radiación pero con temperaturas: estimado (FAO-56 ec. 50) y listado; sin temperaturas: faltante
    est = set(et0.attrs["rs_estimated_dates"])
    no_rs = df.index[df["ALLSKY_SFC_SW_DWN"].isna()]
    assert est and all(d.date().isoformat() in est for d in no_rs if df.loc[d, ["T2M_MAX", "T2M_MIN"]].notna().all())
    assert et0[df["T2M_MAX"].isna()].isna().all()


def test_compara_con_pyet_si_esta_instalado():
    pyet = pytest.importorskip("pyet")
    df = I.load_daily_frame().dropna(subset=["T2M_MAX", "T2M_MIN", "RH2M", "WS2M", "ALLSKY_SFC_SW_DWN"])
    loc = df.attrs["meta"]["location"]
    ours = PH.et0_fao56_daily(df, loc["elevation_m"])
    ref = pyet.pm_fao56((df["T2M_MAX"] + df["T2M_MIN"]) / 2, df["WS2M"], rs=df["ALLSKY_SFC_SW_DWN"],
                        tmax=df["T2M_MAX"], tmin=df["T2M_MIN"], rh=df["RH2M"], elevation=loc["elevation_m"],
                        lat=np.radians(loc["lat"]))
    diff = (ours - ref).abs()
    assert diff.mean() < 0.01 and diff.max() < 0.05
