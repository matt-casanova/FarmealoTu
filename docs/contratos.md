# Contratos de la API — endpoints de datos NASA (Alvin)

> Estado: **v1 implementado** en `master` y alineado con los mappers de la app (`DtoMappers.kt`, `app/`).
> Cambios solo por acuerdo, anotados en `docs/decisiones.md`. **Si un ejemplo de abajo choca con la sección «v1 (implementado)», manda esta última.**
>
> ## v1 (implementado): diferencias respecto del borrador v0
>
> - **Nivel superior:** `last_observation_date` (último día con lluvia y Tmáx válidas). `period` es **texto** (p. ej. `"2026-01-01 → 2026-11-11"`) y el detalle va en `period_detail: {key, start, end, label}`.
> - **Cada indicador** trae además: `period` (texto), `quality: {valid_days, n_days, missing_pct}` y `description` (método + referencia + nota, listo para mostrar).
> - **`monthly_grid`** pasa a tener **una serie por indicador** (la forma propuesta en `docs/UI_BACKEND_CONTRACT.md` de Kendra):
>   ```json
>   {"years": [2015, …, 2026], "months": [1, …, 12],
>    "indicators": [{"id": "rain_anomaly", "name": "Anomalía de lluvia", "unit": "%", "source": "NASA POWER PRECTOTCORR",
>                    "scale": "contexto regional (~50 km, …)", "evidence_status": "derived", "description": "…",
>                    "cells": [{"year": 2015, "month": 1, "value": 12.5, "valid_days": 31, "quality": "ok"}]}]}
>   ```
>   - `id` de las series: `rain_total`, `rain_anomaly`, `dry_spell_max`, `heat_days`, `heavy_rain_days`, `effective_rain_total` y `et0_total`. Esta última solo aparece cuando exista `physics.py`; **una serie sin ningún valor se omite**.
>   - `quality` vale `ok` (mes completo) o `incomplete`. En los meses incompletos, `rain_anomaly` es `null`.
> - **Ubicación:** solo hay caché del punto de referencia. Con `lat`/`lon` a ≤ 0.25° (unos 27 km) se responden los datos del punto de referencia, junto con `site.requested`, `site.distance_km` y un aviso en `source.warnings`. Más lejos → **404** con `detail`. Nunca se consulta NASA en vivo.
> - **`period` inválido → 422** con `detail`.
> - `/api/nasa/status` trae además, en el nivel superior, `last_observation_date` y `last_download`.
> - **Contexto POWER para «Datos de terreno»:** `pipeline.status.power_context(day)` devuelve `{date, t2m_c, rh2m_pct, source, scale, evidence_status, label, note?}`. **Matías** lo incluye como `power_context` dentro de `GET /api/field-node/status`, que es lo que pidió Kendra.
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

## `GET /api/map/layers` (implementado, paso 8)

Lee `backend/data/cache/gibs_layers.json`, que genera `python -m pipeline.gibs verify`. **No llama a NASA en la petición.** Una capa se publica solo si:
1. su identificador está en el GetCapabilities oficial **EPSG:3857** de GIBS;
2. una **tesela real sobre Cuatro Cañadas** respondió con un PNG válido.

| `id` | Capa GIBS (verificada 14-nov-2026) | Fecha | Zoom máx. | Leyenda oficial |
|---|---|---|---|---|
| `rain` | `IMERG_Precipitation_Rate` | 2026-11-13 | 6 | mm/h |
| `soil_moisture` | `SMAP_L4_Analyzed_Root_Zone_Soil_Moisture` | 2026-11-10 | 6 | m³/m³ |
| `heat` | `VIIRS_NOAA20_Land_Surface_Temp_Day` | 2026-11-13 (el 14 no tenía dato en el punto) | 7 | °C (convertido de K) |
| `vegetation` | `VIIRS_NOAA20_NDVI_8Day` | 2026-11-13 | 8 | NDVI |
| `water` | — balance derivado | — | — | `unavailable` hasta tener la ET0 |

```json
{"verified_at": "2026-11-14T…", "attribution": "Imagery: NASA GIBS …", "layers": [
  {"id": "vegetation", "name": "Vegetación", "status": "available", "evidence_status": "documented",
   "source": "NASA GIBS · VIIRS NOAA-20 — NDVI (8 días móviles)", "gibs_layer": "VIIRS_NOAA20_NDVI_8Day",
   "date": "2026-11-13", "resolution": "~500 m", "scale": "regional (~500 m)",
   "legend": [{"label": "0.2", "color": "#B19883"}, {"label": "0.8", "color": "#056400"}],
   "legend_title": "Vegetation Indices", "unit": null, "legend_url": "https://gibs.earthdata.nasa.gov/legends/MODIS_NDVI_H.svg",
   "interpretation": "…", "explanation": "…",
   "tile_url": "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_NOAA20_NDVI_8Day/default/2026-11-13/GoogleMapsCompatible_Level8/{TileMatrix}/{TileRow}/{TileCol}.png",
   "tile_url_zxy": "…/GoogleMapsCompatible_Level8/{z}/{y}/{x}.png",
   "projection": "EPSG:3857 (GoogleMapsCompatible)", "min_zoom": 0, "max_zoom": 8,
   "attribution": "Imagery: NASA GIBS …", "at_point": {"has_data": true, "note": null}},
  {"id": "water", "status": "unavailable", "reason": "Requiere la ET0 FAO-56 …", "tile_url": null}
]}
```

- **Orden de las teselas en GIBS: `{TileMatrix}/{TileRow}/{TileCol}` = `z/y/x`** (fila antes que columna). `tile_url_zxy` ya viene en ese orden.
- **No pedir zoom mayor que `max_zoom`.** Por encima, GIBS no tiene teselas: el mapa debe escalar la última.
- `at_point.has_data = false` significa que el punto de referencia no tiene color ese día. En la lluvia eso indica que no llovió; en las demás capas, nubes o falta de pasada. `note` lo explica en español.

---

## `POST /api/rotations/compare` — motor real (Matías)

Reemplaza al stub (`rotations_stub.py` queda en el repo; Alvin decide si se borra). El **request no cambia**
(`app/schemas.py` → `CompareRequest`). Fuera de ±0.25° del punto piloto → **422** con `detail`, igual que el stub.
Si el motor falla: **500** con `detail` en español; nunca se responde con datos de ejemplo.

### Decisiones de contrato (sección 9 de `docs/UI_BACKEND_CONTRACT.md`)

| Punto | Decisión |
|---|---|
| Orden de `current_rotation` | `[cultivo_actual, cultivo_anterior]`. Cada cultivo va a la campaña que tiene en `reference/crops.json` (`seasons`). Si los dos son de la misma campaña, o uno no tiene campaña con fuente, **no se adivina**: E0 vuelve con `score: null`, `sequence` como lista simple y el motivo en `reasons` y `missing_information`. Solo se usan los dos primeros elementos; el resto se avisa. |
| `sequence` | Objetos `{"season": "summer"\|"winter", "crop": "<código>"}` (la forma preferida por la app). Solo un E0 que no se pudo ubicar en el año viene como lista simple de códigos. |
| `score` | **P50 del bootstrap** (plan 3.8, «score mediano»), 0–100, o `null`. `score_point` (campo nuevo) es el valor de los 11 años reales, sin remuestrear. |
| `rank_stability` | **Fracción 0–1** de las 200 iteraciones en que la estrategia queda primera. Un empate se reparte entre las empatadas. |
| `confidence_level` | `low` o `medium`; **nunca `high`** (política 3.7: datos solo regionales). `low` si la estabilidad es < 0.5, falta un criterio o no hay análisis de suelo (hoy siempre: el request no trae análisis de suelo). |
| `alerts[].date` | Siempre presente: la última observación válida de NASA POWER, o la fecha del día en los avisos del sistema (falta de ET0). |
| Menos de 3 estrategias | Se devuelven las que haya. `limitations` dice qué perfil no se propone y por qué, y cuántas combinaciones descartó cada filtro. Nunca se rellena. |

### Estrategias

| id | Perfil | Regla de selección (entre las factibles; secuencias distintas) |
|---|---|---|
| E0 | Tu rotación actual | La del agricultor, evaluada con las mismas fórmulas aunque no pase los filtros (se dice por qué). |
| E1 | Cuidar suelo | Máxima S_suelo; desempate por S_agua, luego S_riesgo, luego orden alfabético de la secuencia. |
| E2 | Cuidar agua | Máxima S_agua; desempate por S_riesgo. **Sin ET0 no se propone.** |
| E3 | Equilibrada | Maximin de los subpuntajes disponibles entre las que quedan. |

`recommended_strategy_id`: la de mayor `score` con los pesos del agricultor. **Si S_agua falta para todas (hoy: falta la ET0) es
`null`** y la comparación es solo cualitativa (suelo y riesgo).

### Campos que agrega el motor respecto de `UI_BACKEND_CONTRACT.md` (la app ignora los que no conoce)

```jsonc
{
  "data_status": {
    "last_observation_date": "2026-11-09", "last_successful_download": "…", "source": "NASA POWER (caché)",
    "engine": "ready",                         // "partial" si faltara la ET0 (analytics/physics.py)
    "weights_received": {"water": 0.55, "soil": 0.25, "risk": 0.2},
    "years_used": "2015-16 → 2025-26 (11 años agrícolas)",
    "et0": {"status": "available", "reason": null},   // FAO-56 Penman–Monteith desde NASA POWER
    "uncertainty": {"method": "bootstrap de años agrícolas completos (nov→oct) con reemplazo", "n_iterations": 200,
                    "seed": 42, "years_per_iteration": 11, "pairwise_prob_better": {"E1>E0": 0.0}, "note": "…"},
    "assumptions": {"soil_weight_cover": {"value": 0.4, "note": "…", "evidence_status": "assumption"}},
    "feasibility": {"evaluated": 64, "feasible": ["soya (verano) → maíz (invierno)"],
                    "excluded": [{"sequence": "soy>chia", "label": "…", "rule_id": "PARAMS", "reason": "…"}]}
  },
  "strategies": [{
    "id": "E0", "name": "Tu rotación actual",
    "sequence": [{"season": "summer", "crop": "soy"}, {"season": "winter", "crop": "sorghum"}],
    "score": 76.4, "score_point": 76.4, "partial_score": true,
    "weights_used": {"soil": 0.5556, "risk": 0.4444},   // pesos tras repartir el del criterio faltante
    "criteria": {
      "water": {"score": null, "detail": "Falta la ET0 …", "unit": "%", "status": "insufficient"},
      "soil":  {"score": 80.8, "detail": "Suelo cubierto 9.8 de 12 meses …", "unit": "puntos (0–100)", "status": "ok"},
      "risk":  {"score": 70.9, "detail": "Más alto = menos exposición histórica. …", "unit": "puntos (0–100)", "status": "ok"}
    },
    "uncertainty": {"p10": 74.0, "p50": 76.4, "p90": 78.8, "rank_stability": 0.5,
                    "rank_basis": "puntaje parcial (suelo y riesgo)", "prob_better_than_current": null,
                    "confidence_level": "low", "confidence_reason": "Confianza baja: …",
                    "n_iterations": 200, "seed": 42, "note": "P10–P90 es la variabilidad del modelo …"},
    "tradeoffs": {"advantage": "…", "disadvantage": "…", "main_risk": "…"},
    "reasons": ["…"],
    "evidence": [{"indicator": "dry_spell_critical_sorghum", "name": "…", "value": 81.8, "unit": "% de años",
                  "source": "NASA POWER PRECTOTCORR", "period": "…", "scale": "contexto regional (~50 km, …)",
                  "evidence_status": "derived"}],
    "missing_information": ["…"], "agronomist_questions": ["…"],
    "flags": [{"rule_id": "R1", "flag": "high_demand_winter_rainfed", "text": "…"}]
  }]
}
```

### Fórmulas (plan 3.6) y supuestos

- **S_agua** = 100 · Σ min(Pef, ETc) / Σ ETc en los días con cultivo; ETc = Kc(día) · ET0, con la curva de Kc de FAO-56. Pef mensual
  (`monthly_inputs`) × fracción del mes con cultivo (supuesto). Con riego `irrigated`, el riego **no** se suma. Sin ET0 → `insufficient`.
  ⚠️ Como solo cuenta los meses con cultivo, una secuencia con barbecho en invierno tiende a ganar en agua. Es el comportamiento de la
  fórmula del plan, y el suelo de esa secuencia cae (C y D bajos). Conviene revisarlo cuando llegue la ET0.
- **S_suelo** = 100 · (0.4·C + 0.3·R + 0.3·D) − penalización. C = días cubiertos (cultivo o rastrojo) / 365; el rastrojo cubre
  90/60/30 días según su clase alta/media/baja (supuesto); R = promedio de las clases (1 / 0.5 / 0.2); D = familias distintas − 1;
  penalización de 10 puntos por repetir cultivo (supuesto).
- **S_riesgo** = 100 · (1 − E) por año agrícola. E = promedio ponderado de: racha seca > 15 días en la etapa media de FAO-56
  (floración y llenado); fracción de días > P90 del mes en esa etapa (peso 0.5: umbral genérico); ≥ 1 día > P95 en la etapa final
  (cosecha). En una secuencia, cada componente es el promedio de sus cultivos.
- Calendario: siembra al inicio de la ventana de campaña (1 nov / 1 may, supuesto de `CAMPAIGN_DEFINITION`) + etapas de la Tabla 11 de FAO-56.
- Todos los supuestos están en `backend/reference/engine_params.json` y se publican en `data_status.assumptions`.

### Base agrícola (`backend/reference/`)

- `crops.json`: cada dato con `source`, `url`, `year`, `evidence_status`, `note` y `verified_at`. **Los valores de FAO-56 (Kc, etapas,
  raíz) tienen `verified_at: null`: se transcribieron de memoria y hay que cotejarlos con las tablas antes de presentar.** Umbral de calor
  y tolerancia a sequía (EcoCrop): `null` con motivo. Campañas: ANAPO (verano: soya; invierno: sorgo, maíz, girasol, trigo). Chía en
  invierno como supuesto y fuera de candidatos (sin Kc). Cobertura fuera de candidatos (sin especie ni Kc).
- `rotation_rules.json`: reglas R1–R7 (plan 3.5) con texto, efecto y fundamento. R7 da el texto fijo de `limitations`.
- `engine/reference.py` rechaza al cargar cualquier número sin fuente o `null` sin motivo (`tests/test_reference.py`).
- `engine/parser.py`: sinónimos de `crops.json`, sin tildes ni mayúsculas; término desconocido → «no reconocido», nunca se adivina.

### Rendimiento

Al arrancar, el servidor precalienta la climatología por año agrícola y los indicadores recientes. Se cachean por firma de la caché
NASA (filas, última fecha, fecha de descarga), no por reloj; la respuesta se cachea por hash del request. Medido con uvicorn el
14-nov-2026: 0.21–0.28 s por comparación.

### Pendientes para el equipo

- **Christian / Alvin:** `analytics/physics.py` con `et0_fao56_daily(df, elevation_m)`. Con eso, sin cambiar el motor, se activan S_agua,
  E2, el ranking y `engine: "ready"` (probado con una ET0 sintética en `tests/test_engine.py::test_engine_ready_when_et0_arrives`).
- **Kendra (sin cambios necesarios):** la evidencia del motor es `derived` (calculada por FarmealoTu con datos NASA POWER), igual que en
  `indicators.py`, y no se reetiquetó. En el módulo 04 de la app, «Qué viene de NASA» (filtra `documented`) dice que no hay
  observaciones NASA marcadas, y la sección «Qué calculó FarmealoTu» lista esa evidencia con fuente, período y escala. Verificado en un
  teléfono Android contra el motor real el 14-nov-2026.
- **Alvin:** la fuente GRIN (familias botánicas) de `crops.json` no está en `/api/sources` (`context.py`); no se tocó ese archivo.
- **Quien presente:** cotejar los valores de FAO-56 de `crops.json` y completar `verified_at`.
