# PROMPT — Claude Code · Motor de recomendación FarmealoTu (parte de Matías)

> **Para Matías:** abrí una terminal, creá tu carpeta de trabajo (paso 1 de la sección 1), entrá en ella, ejecutá `claude` y escribí:
> **«Lee `PROMPT_Claude_Code_Matias_Motor.md` y sigue sus instrucciones. Trabaja solo en `matias/motor`. Empieza por la Fase 0.»**
>
> Estado al 14-nov-2026, ~21:30 (revisado por Alvin en GitHub):
> - ✅ **Datos NASA (Alvin):** pipeline POWER con caché, indicadores, grilla mensual, `/api/nasa/status`, `/api/map/layers` (GIBS). Están en `origin/alvin/main-esqueleto`.
> - ✅ **Integración Android ↔ NASA (Matías):** rama `origin/integracion_nasa`. No se toca en esta tarea.
> - ❌ **Falta todo el motor:** JSON de referencia, parser, `rules.py`, `recommendation.py`, `uncertainty.py`, el `POST /api/rotations/compare` real y las pruebas B–E. Hoy la API responde con un **stub honesto** (solo E0, sin puntajes).
> - ⏳ **ET0 FAO-56:** depende de `analytics/physics.py` de Christian, que todavía no llegó.
> - ⏰ **Congelación de funciones a la 01:00.** Lo primero es un `compare` real (P0); después la incertidumbre; después las pruebas.

---

## 0. Misión

Eres un **ingeniero backend senior (Python 3.11+, FastAPI, pandas/NumPy)**. Vas a implementar el **motor de recomendación determinista y explicable** de FarmealoTu. El motor compara rotaciones de 12 meses (campaña de verano + campaña de invierno) para un agricultor de **Cuatro Cañadas, Santa Cruz, Bolivia**, usando:

- los indicadores NASA POWER que ya calcula el backend de Alvin;
- una base agrícola con fuentes;
- las prioridades del agricultor.

El motor reemplaza el stub de `POST /api/rotations/compare`. La app Android de Kendra ya está terminada y espera el contrato de la sección 4.

---

## 1. Rama y carpeta (hazlo antes de cualquier otra cosa)

1. El código base está en `origin/alvin/main-esqueleto`. Trabaja en una **rama nueva** dentro de un **worktree aparte**, así no tocas la carpeta Android:
   ```powershell
   git fetch origin
   git worktree add ..\FarmealoTu-motor -b matias/motor origin/alvin/main-esqueleto
   cd ..\FarmealoTu-motor
   ```
   Si `matias/motor` ya existe, usa `git worktree add ..\FarmealoTu-motor matias/motor`.
2. Verifica la rama:
   ```powershell
   git rev-parse --show-toplevel; git branch --show-current; git status --short
   ```
   Tiene que decir exactamente `matias/motor`. Si dice otra cosa, **detente** y explica cómo corregirlo.
3. Prepara el entorno y comprueba que todo está en verde **antes** de cambiar nada:
   ```powershell
   cd backend
   python -m venv .venv
   .\.venv\Scripts\python.exe -m pip install -r requirements.txt
   .\.venv\Scripts\python.exe -m pytest -q
   ```
   Anota cuántos tests pasan. Si alguno falla antes de tus cambios, dilo; no lo borres.

### Reglas de Git
- Commits pequeños con mensajes claros (`feat(engine): …`, `test(engine): …`, `data(reference): …`).
- Puedes hacer `push` de `matias/motor`. **No** hagas `merge`, `push` ni PR a `master`, `backend`, `ui_kendra*`, `integracion_nasa` ni `alvin/*` sin que Matías lo apruebe y Alvin esté avisado.
- No borres ni reescribas el trabajo de Alvin (`pipeline/`, `analytics/indicators.py`, `routers/nasa.py`, `field_node.py`, `context.py`, `monitor.py`, `tools/`). Si necesitas algo distinto de esos módulos, **impórtalo**. Si de verdad hay que cambiarlos, explica por qué y pregunta primero.

---

## 2. Reglas de producto (no negociables)

1. **Nunca inventes datos.** Ni NASA, ni agronómicos, ni citas o fuentes. Todo parámetro de un cultivo (Kc, ciclo, raíz, residuo, tolerancia, umbral de calor) lleva fuente **verificable**. Si no puedes citarla con seguridad, el valor queda en `null`, se explica el motivo y se marca `evidence_status: "unavailable"`. Si es una decisión de diseño, se marca `"assumption"`. Las cifras que ya figuren en el plan como «Supuesto» se rotulan `assumption`.
2. **Motor determinista y explicable:** reglas + MCDA + bootstrap. **Nada de ML ni de LLM** para calcular ni para recomendar. Las explicaciones se arman con plantillas a partir de los números y las reglas reales, nunca con texto aleatorio.
3. **No dupliques ni recalcules lo que ya hace el backend NASA.** Usa `analytics/indicators.py` (sección 3).
4. **Sin llamadas a NASA en una petición.** El motor lee la caché local (`load_daily_frame()`) y nunca descarga.
5. **`null` no es 0.** Un criterio que no se puede calcular queda «insuficiente», y se aplica la política de faltantes (sección 5.4).
6. **No reimplementes ET0** por tu cuenta (es de Christian). Ver sección 5.5.
7. **Escala honesta:** todo dato POWER es «contexto regional (~50 km)», nunca «tu campo».
8. Marca **FarmealoTu**. Código y comentarios en inglés o español, como el resto del backend; textos para el usuario en **español sencillo**.

---

## 3. Lo que ya existe y debes reutilizar

Lee estos archivos completos antes de diseñar:

| Archivo | Qué te da |
|---|---|
| `backend/app/schemas.py` | `CompareRequest` (site, current_rotation, priorities, constraints, notes), `Priorities.normalized()` y `CropCode` = `soy, maize, sorghum, sunflower, wheat, fallow, chia, cover`. **No cambies el request.** |
| `backend/app/routers/rotations_stub.py` | El stub actual: validación de ±0.25° → 422, forma de `data_status`, `alerts`, `evidence` y `limitations`. Úsalo como plantilla de la respuesta. |
| `backend/app/main.py` | Línea `app.include_router(rotations_stub.router)`: la cambias por tu router. Actualiza también `/api/health` (`"engine": "pending"` → `"ready"`, o `"partial"` si falta incertidumbre). |
| `backend/analytics/indicators.py` | Ver la lista de abajo. |
| `backend/app/routers/nasa.py` | `_df()` / `_daily_frame_cached` (DataFrame en caché), `_indicators_cached`, `MAX_DISTANCE_DEG = 0.25`. |
| `backend/pipeline/status.py` | `build_nasa_status()` → `last_observation_date` y `last_download` para `data_status`. |
| `backend/app/routers/context.py` | `/api/context/cuatro-canadas` y `/api/sources` **ya están hechos** (contexto documentado del plan). No los rehagas; si tu `agricultural_context.json` agrega fuentes, intégralas ahí. |
| `backend/tests/test_api_skeleton.py` | `test_compare_is_honest_stub` va a dejar de valer: **reemplázalo** por tests del motor real. No toques los demás tests. |
| `docs/contratos.md`, `docs/ejecucion_backend.md` | Contratos y forma de correr el backend. |
| `FarmealoTu_Plan_de_Proyecto.pdf` (raíz) | Secciones **3.5 a 3.8** (motor, MCDA, bootstrap, tarjetas), **4.4** (parser), **7** (ficha de Matías) y **11** (pruebas). Es la fuente de verdad. |

Funciones de `analytics/indicators.py`:
- `load_daily_frame()`: serie diaria 2015 → hoy, con NaN donde falta un dato. Columnas: `T2M`, `T2M_MAX`, `T2M_MIN`, `PRECTOTCORR`, `RH2M`, `WS2M`, `ALLSKY_SFC_SW_DWN` y, si vino, `GWETROOT`.
- `HIST_YEARS = 2015–2025`, `DRY_THRESHOLD_MM = 1.0`.
- `max_dry_spell`, `count_heat_days`, `heat_thresholds_p90`, `heavy_rain_thresholds_p95`, `count_heavy_rain_days`, `effective_rain_month`, `effective_rain_period` y `_window(df, start, end)`.
- `et0_series(df)`: devuelve `(None, motivo)` mientras no exista `analytics/physics.py`. Cuando exista, devuelve la ET0 diaria.
- `campaign_indicators(df, et0)`: una fila por campaña. Verano 1 nov → 30 abr, invierno 1 may → 31 oct; estas ventanas son un supuesto declarado en `CAMPAIGN_DEFINITION`.
- `yearly_indicators(df, et0)`: **una fila por año agrícola completo** (nov → oct) con columnas `verano_*` e `invierno_*`. Es la base pensada para el bootstrap.
- `monthly_inputs(df, et0)`: P, Pef y ET0 por mes y año. El motor aplica Kc → ETc sobre esto.

---

## 4. Contrato que espera la app Android (`docs/UI_BACKEND_CONTRACT.md` en `origin/integracion_nasa`)

Léelo: `git show origin/integracion_nasa:docs/UI_BACKEND_CONTRACT.md`. Resumen de lo que debes devolver:

```jsonc
{
  "site": {"lat": -17.2374, "lon": -62.2427, "label": "…"},
  "data_status": {"last_observation_date": "AAAA-MM-DD", "last_successful_download": "…", "source": "NASA POWER (caché)",
                  "engine": "ready", "weights_received": {"water": 0.4, "soil": 0.35, "risk": 0.25}},
  "alerts": [{"message": "…", "level": "low|medium|high", "date": "AAAA-MM-DD", "source": "…", "evidence_status": "…"}],
  "strategies": [{
    "id": "E0", "name": "Tu rotación actual",
    "sequence": [{"season": "summer", "crop": "soy"}, {"season": "winter", "crop": "sorghum"}],
    "score": <0–100 o null>, "partial_score": <bool>,
    "criteria": {"water": {"score": <0–100|null>, "detail": "…", "unit": "%", "status": "ok|insufficient"},
                 "soil":  {…}, "risk": {…}},
    "uncertainty": {"p10": …, "p50": …, "p90": …, "rank_stability": <0–1>,
                    "confidence_level": "low|medium", "confidence_reason": "…"},
    "tradeoffs": {"advantage": "…", "disadvantage": "…", "main_risk": "…"},
    "reasons": ["…"],
    "evidence": [{"indicator": "…", "value": …, "unit": "…", "source": "NASA POWER …", "period": "…",
                  "scale": "contexto regional (~50 km)", "evidence_status": "…"}],
    "missing_information": ["…"],
    "agronomist_questions": ["…"]
  }],
  "recommended_strategy_id": "E0…E3 o null",
  "limitations": ["…"]
}
```

Puntos del contrato que debes **decidir y documentar en `docs/contratos.md`**. La lista viene de la sección 9 del contrato de la app:
- **Orden de `current_rotation`:** Android envía `[cultivo_actual, cultivo_anterior]`. Interprétalo así, conviértelo a verano/invierno con `crops.json` (en qué campaña va cada cultivo) y documenta la regla. Si es ambiguo (los dos son de la misma campaña), dilo en `missing_information`; no adivines.
- **`sequence`:** usa objetos con `season` (`summer` / `winter`), que es la forma preferida por la app.
- **`rank_stability`:** fracción de 0 a 1.
- **`confidence_level`:** máximo `medium` con datos solo regionales, según la política 3.7 del plan.
- Cada `alert` lleva `date`.
- Menos de 3 estrategias defendibles: devuelve las que haya y explica en `limitations` qué filtro eliminó el resto. **Nunca rellenes.**

El request **no cambia**. La validación de ±0.25° y su 422 se conservan igual que en el stub.

---

## 5. Qué implementar (según el plan, sección 3.5–3.8)

### 5.1 Base de referencia — `backend/reference/`
- **`crops.json`:** soya, sorgo, maíz, girasol y trigo; chía como cultivo conocido sin prioridad; barbecho y cobertura solo con parámetros que tengan fuente. Campos por cultivo:
  - `family`, `seasons` (campañas candidatas), `cycle_days`;
  - `kc_ini`, `kc_mid`, `kc_end` (FAO-56, Tabla 12: cita la tabla) y la duración de las etapas;
  - `root_depth_m` (FAO-56), `residue_class` (`high` / `medium` / `low`, con fuente), `drought_tolerance` (EcoCrop);
  - `fixes_n` (sí/no, **sin crédito fijo de N**), `heat_threshold_c` (o `null`);
  - `critical_stage` (meses o días de floración y llenado), `harvest_window`, `synonyms`;
  - los 5 campos de procedencia de cada dato (`source`, `url`, `year`, `evidence_status`, `note`).
- **`rotation_rules.json`:** las reglas 1–7 de la tabla 3.5 del plan, cada una con `id`, `text`, `effect` y `rationale`. Regla 7: el mensaje fijo de que cambiar la rotación no elimina el riesgo de inundación va en `limitations`.
- **`agricultural_context.json`:** solo si agrega algo a lo que ya está en `context.py`. No dupliques.
- Valida al cargar: cada número tiene `source` o es `null` con motivo. Si encuentras un número sin fuente, el test falla.

### 5.2 Parser de cultivos — `backend/engine/parser.py`
Diccionario de sinónimos de `crops.json`:
- `soya`, `soja`, `soy` → `soy`
- `maíz`, `maiz` → `maize`
- `sorgo` → `sorghum`
- `girasol` → `sunflower`
- `trigo` → `wheat`
- `barbecho`, `descanso` → `fallow`

Ignora mayúsculas y tildes. Un término desconocido devuelve «no reconocido» y **nunca adivina**. Pruébalo con la frase del plan: «Estoy sembrando soya y antes tenía sorgo. Este año tengo poca agua y quiero evitar pérdidas.»

### 5.3 Reglas y factibilidad — `backend/engine/rules.py`
- Candidatos: todas las secuencias de 12 meses (verano + invierno) armadas con los cultivos de `crops.json`.
- Filtros, cada uno con su motivo registrado:
  - estación compatible;
  - ciclo dentro de la ventana;
  - cultivos excluidos por el agricultor (`constraints.excluded_crops`);
  - parámetros conocidos;
  - agua: alta demanda en invierno con `rainfed` → marcar o excluir, con motivo.
- Devuelve `feasible` y `excluded`, con `[{sequence, rule_id, reason}]` para cada exclusión.

### 5.4 Puntuación y selección — `backend/engine/recommendation.py`

**Subpuntuaciones (0–100)**

- **S_agua** = 100 · Σ(meses con cultivo) min(Pef, ETc) / Σ ETc
  - ETc = Kc(etapa) × ET0.
  - Pef sale de `monthly_inputs`.
  - Con riego `irrigated` se informa aparte y no se suma como agua gratuita.
  - Sin Kc o sin ET0 → `insufficient`.
- **S_suelo** = 100 · (0.4·C + 0.3·R + 0.3·D) − penalización por repetición
  - C = meses cubiertos (cultivo o rastrojo) / 12.
  - R = clase de residuo: alto 1 · medio 0.5 · bajo 0.2.
  - D = (familias distintas − 1) / (n.º de campañas − 1).
  - Los subpesos, las clases y el valor de la penalización son `assumption`: decláralos en la respuesta.
  - La cobertura también consume agua y resta en S_agua (regla 5).
- **S_riesgo** = 100 · (1 − E), donde E es el promedio de los componentes disponibles:
  - p_seca: fracción de años con una racha seca mayor al umbral en la etapa crítica. El umbral de 15 días es `assumption` configurable.
  - p_calor: fracción de días de calor en floración. Con un umbral genérico (`heat_thresholds_p90`), el componente pesa la mitad y se rotula.
  - p_lluvia: fracción de años con al menos 1 día por encima del P95 en la ventana de cosecha.

**MCDA**
- Score = w_agua·S_agua + w_suelo·S_suelo + w_riesgo·S_riesgo, con los pesos de `priorities.normalized()`.
- **Política de faltantes:** el peso de un criterio faltante se reparte proporcionalmente entre los válidos, con `partial_score: true` y el criterio anotado en `missing_information`. **Si S_agua falta para todas las estrategias, no hay ranking global:** `recommended_strategy_id: null` y solo comparación cualitativa.

**Selección (las secuencias deben ser distintas entre sí)**
- **E0:** la rotación del agricultor, evaluada con las mismas fórmulas aunque no pase los filtros. En ese caso se dice por qué.
- **E1 Cuidar suelo:** la de máxima S_suelo; si empatan, gana la de mayor S_agua.
- **E2 Cuidar agua:** la de máxima S_agua; si empatan, gana la de mayor S_riesgo.
- **E3 Equilibrada:** la de maximin de las tres subpuntuaciones.
- El ranking con los pesos del agricultor da `recommended_strategy_id`.

**Textos**
- `reasons`, `tradeoffs`, `missing_information` y `agronomist_questions` se generan desde los números y las reglas, por ejemplo: «Tu rotación actual cubre [N] meses el suelo; en [mes] queda más expuesta a rachas secas.»
- La economía va solo como criterio cualitativo: «sin datos de precios; consultá con tu técnico».
- `evidence`: los indicadores NASA que usó cada estrategia, con el `evidence_status` que entrega `indicators.py`. **No cambies etiquetas para que la UI se vea más llena.** Si el módulo 04 de la app queda vacío por eso, anótalo como pendiente para Kendra.

### 5.5 ET0 (bloqueo conocido)
- Si existe `backend/analytics/physics.py` con `et0_fao56_daily(df, elevation_m)` (de Christian), `et0_series` ya lo usa y no tienes que hacer nada más.
- Si **no** existe: no lo implementes por tu cuenta. S_agua queda `insufficient` y se aplica la política de faltantes (scores parciales con suelo y riesgo, `recommended_strategy_id: null`). Deja el motor listo para que, cuando llegue ET0, todo funcione sin cambios.
- Si a las **23:00** sigue sin estar, **avisa a Alvin y a Christian**. Alvin decide si se integra un ET0 FAO-56 provisional y quién lo hace.

### 5.6 Incertidumbre — `backend/analytics/uncertainty.py`
- Bootstrap de **200 iteraciones**, `seed = 42`. Remuestrea **años agrícolas completos** de `yearly_indicators` / `monthly_inputs`: 2015–2025, 11 años con reemplazo por iteración, nunca días sueltos.
- En cada iteración recalcula S_agua, S_suelo, S_riesgo y el Score de cada estrategia elegida.
- Salidas por estrategia: `p10`, `p50`, `p90` y `rank_stability` (fracción de iteraciones en que queda primera). Agrega `P(A > B)` por pares en `data_status` o en un campo nuevo; la app ignora los campos que no conoce.
- `confidence_level`:
  - `low` si la estabilidad es < 0.5, falta un criterio o no hay información de suelo;
  - si no, `medium` (máximo con datos regionales), con `confidence_reason` en una frase.
- **Respuesta en menos de 3 s:** vectoriza con NumPy y cachea por hash del request. Si no alcanza, precalcula para el punto piloto (plan B).
- Mensaje obligatorio: «P10–P90 es la variabilidad del modelo bajo remuestreo histórico; no es una garantía de rendimiento ni un pronóstico.»

### 5.7 Router — `backend/app/routers/rotations.py`
- `POST /api/rotations/compare` con `CompareRequest`; mismo prefijo y tag que el stub.
- En `main.py`, reemplaza `rotations_stub` por `rotations`. Deja `rotations_stub.py` en el repo y no lo borres: Alvin decide después.
- Si el motor falla internamente, responde 500 con un `detail` claro en español. **Nunca** caigas silenciosamente a datos de ejemplo.

---

## 6. Fases y orden (por la hora)

| Fase | Hasta | Entregable |
|---|---|---|
| **0. Inspección** | 21:50 | Rama verificada, tests base en verde y lectura de las secciones 3 y 4. Plan corto de archivos. **Muéstraselo a Matías antes de seguir.** |
| **1. Referencia + parser** | 22:30 | `crops.json` y `rotation_rules.json` con fuentes, parser y sus tests. |
| **2. Motor sin incertidumbre (P0)** | 23:30 | `rules.py`, `recommendation.py` y el router. `compare` real que devuelve E0 a E3 (o menos, explicado). Commit y aviso a Kendra para que pruebe la app en modo real. |
| **3. Incertidumbre** | 00:30 | `uncertainty.py` integrado, por debajo de 3 s. |
| **4. Pruebas y documentación** | 01:00 | Pruebas B–E en verde, `docs/contratos.md` actualizado y `pytest -q` completo en verde. |

**Si se atrasa:** reduce a E0, E1 y E2 bien hechas antes que cuatro a medias. Si algo va a tomar más de 30 min, avisa y propone una versión más chica.

---

## 7. Pruebas obligatorias (`backend/tests/test_engine.py` y `test_api_compare.py`)

- **B:** la rotación actual más al menos 2 alternativas, con diferencias explicadas en `reasons` y `tradeoffs`.
- **C:** subir el peso del agua recalcula de forma reproducible (la misma entrada da la misma salida). El orden **puede** cambiar, pero no se fuerza.
- **D:** sin suelo, NDVI ni SMAP la respuesta sigue siendo 200 y declara los vacíos en `missing_information`. Sin ET0, la respuesta trae `partial_score` y `recommended_strategy_id: null`.
- **E:** el bootstrap devuelve `p10 ≤ p50 ≤ p90` y `0 ≤ rank_stability ≤ 1`, es reproducible con `seed = 42` y responde en menos de 3 s.
- **Datos:** cada número de `crops.json` tiene `source` o es `null` con motivo.
- **Contrato:** las claves de nivel superior y las de cada estrategia coinciden con la sección 4; fuera de ±0.25° → 422.
- **Parser:** los sinónimos, las tildes, un término desconocido («no reconocido») y la frase del plan.
- No borres tests existentes. Si uno de Alvin falla por tu cambio, arregla tu código, no el test (salvo `test_compare_is_honest_stub`, que se reemplaza).

Prueba manual final:
```powershell
cd backend
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
# en otra terminal:
$body = '{"site":{"lat":-17.2374,"lon":-62.2427},"current_rotation":["soy","sorghum"],"priorities":{"water":0.55,"soil":0.25,"risk":0.2},"constraints":{"irrigation":"rainfed","excluded_crops":[]}}'
Measure-Command { Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/rotations/compare -ContentType 'application/json' -Body $body } | Select-Object TotalSeconds
Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/rotations/compare -ContentType 'application/json' -Body $body | ConvertTo-Json -Depth 8
```

---

## 8. Entrega final

1. La lista de commits en `matias/motor` y `git status`.
2. Los resultados reales de `pytest` (cuántos pasan y cuántos fallan) y el tiempo medido de `compare`. **No afirmes nada que no ejecutaste.**
3. Un ejemplo real de la respuesta de `compare` para el punto piloto.
4. Qué parámetros quedaron `null` o `assumption` y por qué.
5. El estado de ET0: integrado o pendiente, y cómo afecta a S_agua y al ranking.
6. Lo que Kendra o Alvin tienen que saber: cambios en `docs/contratos.md`, campos nuevos y cualquier diferencia con `UI_BACKEND_CONTRACT.md`.
7. Cómo integrarlo: «`matias/motor` está lista para que Alvin la revise y la integre», **sin hacer el merge tú**.
