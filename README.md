# FarmealoTu · RotaSat

> **README momentáneo** (14-nov-2026, durante la hackatón). Describe el estado **real** de cada parte: qué funciona,
> qué es provisional y qué falta. Se reemplaza por la versión final (en inglés) antes del envío.

**NASA Space Apps Challenge 2026 · Reto «Field Shift: Adapting Farms with NASA Data».**
Equipo FarmealoTu: Alvin, Matías y Kendra (app) · Vanesa, Christian y Mateo (prototipo y simulación).

FarmealoTu es una **app Android de apoyo a la decisión**. Ayuda a agricultores de **Cuatro Cañadas (Santa Cruz,
Bolivia)** a comparar estrategias de rotación de cultivos usando datos de observación de la Tierra de la NASA,
información agrícola local y sus propias prioridades: cuidar el agua, el suelo o reducir riesgos. La herramienta
**apoya, no reemplaza**, el criterio del agricultor y del agrónomo. NASA aporta los datos; FarmealoTu los
interpreta con su propio motor, que es determinista y explicable.

El plan completo está en [`FarmealoTu_Plan_de_Proyecto.pdf`](FarmealoTu_Plan_de_Proyecto.pdf).

---

## Estado actual

| Parte | Estado | Dónde |
|---|---|---|
| Datos NASA POWER (diario 2015-01-01 → 2026-11-14, en caché) | ✅ Real | `backend/pipeline/power.py`, `backend/data/cache/` |
| Indicadores agroclimáticos (racha seca, anomalía de lluvia, días de calor, lluvia intensa, lluvia efectiva) | ✅ Real | `backend/analytics/indicators.py` |
| `GET /api/indicators`, `GET /api/nasa/status` | ✅ Real | `backend/app/routers/nasa.py` |
| `GET /api/map/layers`: 4 capas NASA GIBS verificadas (IMERG, SMAP L4, VIIRS temperatura y NDVI) | ✅ Real | `backend/pipeline/gibs.py` |
| Forzante horario NASA POWER para el gemelo digital (escenarios S1, S2, S3) | ✅ Real | `hardware/simulation/forcing/` |
| Nodo de campo: `POST/GET /api/field-node/readings`, `/status` (contrato v1, SQLite) | ✅ Real (simple) | `backend/app/routers/field_node.py` |
| Contexto de Cuatro Cañadas y catálogo de fuentes | ✅ Información documentada | `backend/app/routers/context.py` |
| Base agrícola con fuente por dato (`crops.json`, `rotation_rules.json`, `engine_params.json`) y parser de sinónimos | ✅ Real (Matías). Kc, etapas y raíz de FAO-56 cotejados contra el texto de FAO salvo el Kc inicial; falta el cotejo de una persona (`verified_at`) | `backend/reference/`, `backend/engine/parser.py` |
| Motor de recomendación: factibilidad, S_suelo, S_riesgo, MCDA con política de faltantes, E0–E3 | ✅ Real (Matías) | `backend/engine/` |
| Incertidumbre: bootstrap de 200 años agrícolas remuestreados (seed 42), P10/P50/P90, estabilidad del ranking | ✅ Real (Matías) | `backend/analytics/uncertainty.py` |
| `POST /api/rotations/compare` | ✅ Completo: E0–E3 con agua, suelo y riesgo, P10–P90 y recomendación según los pesos del agricultor | `backend/app/routers/rotations.py` |
| ET0 FAO-56 Penman–Monteith diaria desde NASA POWER | ✅ Real. Validada con el ejemplo 18 de FAO-56 (3.9 mm/día) y día por día contra `pyet` (diferencia 0.0). Rs/Rso acotado a 0.3–1 (ASCE-EWRI); los pocos días sin radiación publicada se estiman con FAO-56 ec. 50 y quedan listados | `backend/analytics/physics.py` |
| Nodo de campo físico (ESP32: humedad capacitiva, DS18B20, DHT11, LDR) y gemelo digital (simulación) | ✅ Enviando a la API en vivo; la app los muestra con su insignia FÍSICO / SIMULADO | `hardware/` |
| App Android (Kotlin + Jetpack Compose) | ✅ UI pulida + integración con la API real (Kendra y Matías). 72 pruebas unitarias | `app/` |
| Gemelo digital, firmware ESP32 / Wokwi, prototipo físico | ⏳ En desarrollo (Christian, Mateo, Vanesa) | `hardware/` |

### Ramas

| Rama | Contenido |
|---|---|
| **`master`** | **Rama principal.** App Android, backend completo (datos NASA, API, nodo de campo, motor), documentación, forzante del gemelo digital y plan en PDF |
| `alvin/datos-nasa`, `alvin/main-esqueleto`, `matias/motor` | Ramas de trabajo del backend, ya integradas en `master` (históricas) |
| `ui_kendra`, `ui_kendra_pos`, `integracion_nasa` | Ramas de trabajo de la app, ya integradas en `master` (históricas) |
| `backend` | Sin uso |

---

## Arquitectura

```
NASA POWER (diario/horario) ─┐
NASA GIBS (WMTS EPSG:3857) ──┼─▶ pipeline/ (descarga, validación, caché) ─▶ analytics/ (indicadores, ET0)
Base agrícola local* ────────┘                                                     │
                                                                                   ▼
App Android (Compose) ◀── HTTPS ── FastAPI /api/* ◀── engine/* (reglas + MCDA) + analytics/uncertainty.py (bootstrap)
        ▲                                  ▲
        └─ «Datos de terreno» ◀────────────┴── POST /api/field-node/readings ◀── gemelo digital · Wokwi · ESP32
```
Reglas que no se negocian:
- La app **nunca** llama a NASA; la API **nunca** llama a NASA durante una petición, porque todo sale de la caché.
- Todo valor lleva fuente, fecha, unidad, escala y `evidence_status` (`documented | derived | assumption | farmer_provided | unavailable`).
- Nada se inventa: lo que falta es `null` con su motivo.

---

## Cómo correrlo (Windows PowerShell)

### Backend

```powershell
git switch master                      # rama principal (NASA + nodo + motor)
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- Documentación interactiva: http://localhost:8000/docs
- Pruebas: `python -m pytest -q` (117 pruebas; ninguna llama a NASA).
- Al arrancar, el servidor precalienta la climatología del motor (unos segundos). Después, cada `compare` responde en ~0.1 s.
- Refrescar datos (solo antes de la demo, no durante):
  ```powershell
  python -m pipeline.power download --refresh   # NASA POWER diario + GWETROOT
  python -m pipeline.forcing --refresh          # CSV horario para el gemelo digital
  python -m pipeline.gibs verify                # re-verifica las capas GIBS
  ```

### Túnel público (para el teléfono físico y para Wokwi)

Ver [`docs/ejecucion_backend.md`](docs/ejecucion_backend.md). Resumen:
`cloudflared tunnel --url http://localhost:8000` imprime una URL `https://….trycloudflare.com`. **Esa URL cambia cada vez
que se reinicia el túnel.**

### App Android

1. Abrir la raíz del repo en Android Studio (rama `master`).
2. En `local.properties` (no se versiona):
   ```properties
   farmealotu.apiBaseUrl=http://10.0.2.2:8000/      # emulador → backend en la PC
   # teléfono físico: farmealotu.apiBaseUrl=https://<url-del-tunel>/
   farmealotu.demoMode=false                         # true = datos de ejemplo rotulados «DEMO»
   ```
3. `.\gradlew.bat assembleDebug`.

---

## API (estado actual)

| Endpoint | Estado |
|---|---|
| `GET /api/health` | ✅ |
| `GET /api/indicators?lat&lon&period&heat_threshold_c` | ✅ `period` = `recent` · `campaign:verano-2025-26` · `AAAA-MM-DD/AAAA-MM-DD`. Puntos a más de 0.25° del piloto → 404 |
| `GET /api/nasa/status` | ✅ |
| `GET /api/map/layers` | ✅ |
| `POST /api/field-node/readings` · `GET /api/field-node/readings` · `GET /api/field-node/status` | ✅ |
| `GET /api/context/cuatro-canadas` · `GET /api/sources` | ✅ |
| `POST /api/rotations/compare` | ✅ motor completo (E0–E3, agua/suelo/riesgo, P10–P90, recomendación). Fuera de ±0.25° → 422. Ejemplo real: [`docs/ejemplos/compare_punto_piloto.json`](docs/ejemplos/compare_punto_piloto.json) |

Contratos JSON: [`docs/contratos.md`](docs/contratos.md) (API) y [`docs/UI_BACKEND_CONTRACT.md`](docs/UI_BACKEND_CONTRACT.md)
(lo que espera la app). Contrato del nodo: [`shared/sensor_reading.schema.json`](shared/sensor_reading.schema.json).

---

## Datos NASA usados

| Producto | Uso | Escala |
|---|---|---|
| **NASA POWER** Daily API v2.10 (community AG): T2M, T2M_MAX, T2M_MIN, PRECTOTCORR, RH2M, WS2M, ALLSKY_SFC_SW_DWN, GWETROOT | Indicadores agroclimáticos, contexto regional para «Datos de terreno» | Celda ~0.5° × 0.625° (~50 km) |
| **NASA POWER** Hourly API: T2M, RH2M, WS2M, PRECTOTCORR, ALLSKY_SFC_SW_DWN, PS, T2MDEW | Forzante del gemelo digital (S1 línea base, S2 sequía dic-2025/ene-2026, S3 exceso de lluvia feb–mar-2026) | ~50 km, horario (LST) |
| **NASA GIBS** WMTS: `IMERG_Precipitation_Rate`, `SMAP_L4_Analyzed_Root_Zone_Soil_Moisture`, `VIIRS_NOAA20_Land_Surface_Temp_Day`, `VIIRS_NOAA20_NDVI_8Day` | Capas del visor «Explorar mi zona» | ~10 km · ~9 km · ~1 km · ~500 m |

Punto de referencia: **−17.2374, −62.2427** (Cuatro Cañadas). Elevación según POWER: 276.15 m.

**Datos reales destacados:**
- Entre el 1-ene y el 11-nov-2026 llovió **+24 %** respecto de la media 2015–2025 de las mismas fechas.
- La ventana más seca de dic-2025/ene-2026 tuvo **−32 %** de lluvia.
- La ventana más lluviosa de feb–mar-2026 tuvo **+135 %**, lo que coincide con lo reportado por ANAPO.

## Otras fuentes

FAO-56 (ET0 y Kc) · FAO, Brouwer y Heibloem 1986 (lluvia efectiva, supuesto declarado) · MDPyEP *Santa Cruz Industrial y
Productiva 2024* (producción 2023) · ANAPO (prácticas y campañas) · El Deber (inundación de mar-2025). El catálogo completo
con enlaces está en `GET /api/sources`.

---

## Limitaciones (se declaran también en la app)

- NASA POWER es una celda de ~50 km: es **contexto regional**, no una medición de la finca. SMAP (~9 km) es una estimación
  de modelo con asimilación, no un sensor enterrado.
- Los días de calor usan un umbral meteorológico general (P90 del mes) salvo que se indique un umbral de cultivo con fuente.
  Por sí solos no afirman daño a un cultivo.
- La lluvia intensa es exposición meteorológica: no predice inundaciones fluviales. Cambiar la rotación no elimina ese riesgo.
- La lluvia efectiva usa un método empírico FAO (supuesto declarado).
- Las puntuaciones dependen de reglas y pesos; los pesos iniciales (0.40/0.35/0.25) y los subpesos del suelo son
  preferencias de producto (`assumption`). Son relativas entre opciones, no probabilidades.
- P10–P90 es la variabilidad del modelo bajo remuestreo histórico, no una garantía ni un pronóstico. Con datos solo
  regionales, la confianza máxima es «media».
- Los parámetros de cultivo vienen de tablas genéricas de FAO-56 (sin ajuste climático local) y la siembra se supone
  al inicio de cada campaña. Soya→sorgo y soya→maíz tienen el mismo puntaje de suelo y riesgo, porque FAO-56 les
  asigna las mismas etapas: solo el agua (Kc × ET0) las distingue.
- La ET0 se calcula con datos regionales de NASA POWER (~50 km), no con mediciones del campo.
- No se predicen rendimientos ni precios. No se sustituyen el análisis de suelo ni la visita técnica.
- El nodo de campo es un prototipo de laboratorio: no valida estadísticamente al satélite y no alimenta el motor.

## Uso de IA

Se usó **Claude Code (Anthropic)** como asistente de programación y documentación: pipeline NASA, indicadores, API, capas
GIBS, motor, pruebas, la UI Android y este README. El equipo revisó y probó el código (pruebas automáticas y verificación contra datos reales).
**No se usa IA ni LLM para calcular recomendaciones.** El motor es determinista y explicable. Detalle en
[`docs/uso_ia.md`](docs/uso_ia.md).
