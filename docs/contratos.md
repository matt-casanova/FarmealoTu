# Contratos de la API — endpoints de datos NASA (Alvin)

> Estado: **v0, borrador para acordar** (Alvin · Matías · Kendra). Cambios solo por acuerdo, anotados en `docs/decisiones.md`.
> Los endpoints viven en `backend/app/routers/nasa.py` (`APIRouter`, prefijo `/api`) y Matías los incluye en `main.py`.
> **Todas las respuestas se leen de la caché**: ninguna petición llama a NASA en vivo.

## Convenciones comunes

| Campo | Significado |
|---|---|
| `evidence_status` | `documented` (dato NASA tal cual) · `derived` (calculado por nosotros) · `assumption` (supuesto declarado) · `farmer_provided` · `unavailable` |
| `value: null` | Dato no disponible. Siempre viene acompañado de `evidence_status: "unavailable"` y `reason`. Nunca se rellena |
| Fechas | ISO `YYYY-MM-DD`. Las de POWER están en hora solar local (`time_standard: "LST"`) |
| `scale` | Escala espacial. Para POWER: `"contexto regional (~50 km, celda POWER ~0.5° × 0.625°)"`. **Nunca** «tu campo» |
| Números | Siempre con `unit`. Los porcentajes van en 0–100 |

---

## `GET /api/indicators`

**Parámetros (query):**

| Parámetro | Tipo | Defecto | Nota |
|---|---|---|---|
| `lat`, `lon` | float | punto piloto −17.2374, −62.2427 | En el MVP solo hay caché para el punto piloto; otro punto → 404 con mensaje |
| `period` | string | `recent` | `recent` (2026-01-01 → última fecha válida) · `campaign:verano-2025-26` · `campaign:invierno-2025` · `YYYY-MM-DD/YYYY-MM-DD` |
| `heat_threshold_c` | float | — | Umbral de calor de un cultivo **con fuente** (lo manda el motor). Sin él se usa el P90 histórico del mes |

**Respuesta 200 (ejemplo con valores ilustrativos):**

```json
{
  "site": {"lat": -17.2374, "lon": -62.2427, "label": "Cuatro Cañadas (punto de referencia)", "elevation_m": 276.15},
  "source": {
    "name": "NASA POWER", "product": "Daily API", "api_version": "v2.10.3", "community": "AG",
    "scale": "contexto regional (~50 km, celda POWER ~0.5° × 0.625°)", "time_standard": "LST",
    "downloaded_at": "2026-11-14T17:03:06+00:00", "from_cache": true, "warnings": []
  },
  "data_window": {"historical": {"start": "2015-01-01", "end": "2025-12-31"},
                  "recent": {"start": "2026-01-01", "last_valid_date": "2026-11-09"}},
  "period": {"key": "recent", "start": "2026-01-01", "end": "2026-11-09", "label": "1 ene – 9 nov 2026"},
  "quality": {"n_days": 313, "n_valid_days": 313, "missing_pct": 0.0},
  "indicators": [
    {
      "id": "rain_total", "name": "Lluvia acumulada", "value": 812.4, "unit": "mm",
      "evidence_status": "derived", "source": "NASA POWER PRECTOTCORR",
      "method": "suma de la lluvia diaria del período",
      "reference": {"value": 1015.2, "unit": "mm", "label": "media 2015–2025, mismas fechas"}
    },
    {
      "id": "rain_anomaly", "name": "Anomalía de lluvia", "value": -20.0, "unit": "%",
      "evidence_status": "derived", "source": "NASA POWER PRECTOTCORR",
      "method": "100·(P_actual − P̄_hist)/P̄_hist, P̄ = media 2015–2025 de las mismas fechas",
      "defined": true
    },
    {
      "id": "dry_spell_max", "name": "Racha seca máxima", "value": 23, "unit": "días",
      "evidence_status": "derived", "source": "NASA POWER PRECTOTCORR",
      "method": "máximo de días consecutivos con P < 1 mm/día", "params": {"threshold_mm_day": 1.0},
      "reference": {"value": 17.8, "unit": "días", "label": "media 2015–2025, mismas fechas"}
    },
    {
      "id": "heat_days", "name": "Días de calor", "value": 41, "unit": "días",
      "evidence_status": "derived", "source": "NASA POWER T2M_MAX",
      "method": "días con Tmáx > P90 histórico (2015–2025) del mismo mes",
      "threshold_kind": "general_p90",
      "note": "Indicador meteorológico general: no afirma daño a un cultivo"
    },
    {
      "id": "heavy_rain_days", "name": "Días de lluvia intensa", "value": 6, "unit": "días",
      "evidence_status": "derived", "source": "NASA POWER PRECTOTCORR",
      "method": "días con P > P95 de los días húmedos (≥ 1 mm) del mismo mes, 2015–2025",
      "note": "Exposición meteorológica; no es una predicción de inundación"
    },
    {
      "id": "et0_total", "name": "Evapotranspiración de referencia (ET0)", "value": null, "unit": "mm",
      "evidence_status": "unavailable", "reason": "physics.py (FAO-56) todavía no integrado"
    },
    {
      "id": "effective_rain_total", "name": "Lluvia efectiva", "value": 590.1, "unit": "mm",
      "evidence_status": "assumption", "source": "NASA POWER PRECTOTCORR",
      "method": "mensual: 0.8P − 25 si P > 75 mm; 0.6P − 10 si P ≤ 75 mm; mínimo 0 (FAO, Brouwer y Heibloem 1986)"
    }
  ],
  "monthly_grid": {
    "years": [2015, 2016, "...", 2026], "months": [1, 2, "...", 12],
    "fields": {
      "rain_mm": "mm", "rain_anomaly_pct": "%", "dry_spell_max_days": "días",
      "heat_days": "días", "heavy_rain_days": "días", "effective_rain_mm": "mm", "et0_mm": "mm"
    },
    "cells": [
      {"year": 2015, "month": 1, "rain_mm": 210.3, "rain_anomaly_pct": 12.5, "dry_spell_max_days": 6,
       "heat_days": 2, "heavy_rain_days": 1, "effective_rain_mm": 143.2, "et0_mm": null,
       "valid_days": 31, "complete": true}
    ]
  },
  "campaigns": [
    {"id": "verano-2025-26", "label": "Verano 2025-26", "start": "2025-11-01", "end": "2026-04-30",
     "rain_mm": 690.0, "rain_anomaly_pct": -8.1, "dry_spell_max_days": 19, "heat_days": 22,
     "heavy_rain_days": 9, "complete": true}
  ],
  "campaign_definition": {
    "verano": "1 nov → 30 abr", "invierno": "1 may → 31 oct", "evidence_status": "assumption",
    "note": "Ventanas aproximadas del calendario de Santa Cruz; el motor usa las de crops.json cuando tengan fuente"
  },
  "limitations": [
    "NASA POWER es una celda de ~50 km: contexto regional, no una medición de la finca.",
    "Los días de calor usan un umbral meteorológico general salvo que se indique un umbral de cultivo con fuente."
  ]
}
```

- `monthly_grid.cells`: una celda por (año, mes) desde 2015-01 hasta el último mes con datos. Si un mes está incompleto: `complete: false`.
- La anomalía de una celda compara contra la media 2015–2025 del mismo mes. Si esa media es 0, el valor es `null` (`defined: false`).

### Funciones Python para el motor (Matías), en `analytics/indicators.py`

```python
load_daily_frame() -> pd.DataFrame                    # índice fecha; columnas POWER (NaN = faltante)
yearly_indicators(df) -> pd.DataFrame                 # una fila por AÑO AGRÍCOLA (nov→oct): para el bootstrap
campaign_indicators(df) -> pd.DataFrame               # una fila por campaña (verano/invierno)
monthly_inputs(df) -> pd.DataFrame                    # por mes: P, Pef, ET0 (si hay physics.py)
```

---

## `GET /api/nasa/status`

**Respuesta 200:**

```json
{
  "generated_at": "2026-11-14T18:00:00+00:00",
  "reference_date": "2026-11-14",
  "sources": [
    {
      "id": "power_daily", "name": "NASA POWER · diario", "status": "available",
      "product_version": "v2.10.3", "last_successful_download": "2026-11-14T17:03:06+00:00",
      "last_observation_date": "2026-11-09", "coverage": {"start": "2015-01-01", "end": "2026-11-14"},
      "from_cache": true, "scale": "contexto regional (~50 km)",
      "parameters": ["T2M", "T2M_MAX", "T2M_MIN", "PRECTOTCORR", "RH2M", "WS2M", "ALLSKY_SFC_SW_DWN"],
      "notes": ["POWER publica con unos días de atraso"]
    },
    {"id": "power_gwetroot", "name": "NASA POWER · humedad de la zona radicular (GWETROOT)", "status": "available",
     "last_observation_date": "2025-12-31", "from_cache": true, "scale": "contexto regional (~50 km)"},
    {"id": "power_hourly", "name": "NASA POWER · horario (forzante del gemelo digital)", "status": "partial",
     "notes": ["Radiación horaria no publicada aún para oct–nov 2026"]},
    {"id": "gibs", "name": "NASA GIBS (teselas del visor)", "status": "unavailable", "reason": "pendiente de verificación (paso 8)"},
    {"id": "smap_l4", "name": "SMAP L4 SPL4SMGP v8", "status": "unavailable", "reason": "P2: sin archivos descargados"},
    {"id": "gpm_imerg", "name": "GPM IMERG", "status": "unavailable", "reason": "P2: se usa PRECTOTCORR de POWER"},
    {"id": "ndvi", "name": "NDVI MODIS/VIIRS", "status": "unavailable", "reason": "sin serie descargada"}
  ]
}
```

`status`: `available` · `partial` · `regional` · `unavailable`.

---

## `GET /api/map/layers` (Paso 8, pendiente)

Se definirá tras verificar el GetCapabilities de GIBS. Forma prevista: una entrada por capa (`rain`, `soil_moisture`, `heat`, `vegetation`, `water`) con `status` (`available | regional | unavailable`), `source`, `date`, `scale`, `legend` y `tile_url_template` **solo si está verificada** (si no, `null`).
