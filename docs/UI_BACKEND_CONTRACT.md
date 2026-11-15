# Contrato UI Android ↔ Backend FastAPI — FarmealoTu

Qué espera la app Android (rama `ui_kendra`) de la API, qué está confirmado y qué falta confirmar.
Este archivo **no modifica el backend**: es la lista de conciliación para cuando exista `/docs` u `openapi.json`.

- **Fuente del contrato hoy:** `FarmealoTu_Plan_de_Proyecto.pdf` (secciones 3.8, 4.8 y 9.2).
- **Estado del backend al escribir esto (14-nov-2026):** la rama `origin/backend` no contiene código de API
  (mismo contenido que `master`) y no había servidor accesible. **Nada de lo de abajo fue probado contra la API real.**
- **Código que hay que tocar al conciliar:** `app/src/main/java/com/example/farmealotu/data/model/ApiDtos.kt`,
  `DtoMappers.kt` y `data/remote/FarmealoTuApi.kt`. Las pantallas no conocen el JSON.

Leyenda: ✅ definido en el PDF · 🟡 el PDF no fija la forma: la app acepta varias · ❓ supuesto de la UI, **pendiente de confirmar**.

---

## 1. Reglas generales que aplica la app

| Regla | Detalle |
|---|---|
| `null` no es 0 | Todo campo de respuesta es opcional. Lo ausente o `null` se muestra «sin dato / no disponible / dato insuficiente». |
| Campos nuevos | Se ignoran (`ignoreUnknownKeys`). Agregar campos no rompe la app. |
| Android no calcula | No recalcula `S_agua`, `S_suelo`, `S_riesgo`, `score`, ranking ni P10–P90. Solo ordena tarjetas por el `score` recibido. |
| Pesos | Se normalizan a suma 1 (4 decimales) y se envían al mismo `POST /api/rotations/compare`. Un cambio de pesos = una nueva llamada, con botón «Actualizar comparación». |
| Sin llamadas a NASA | La app solo habla con este backend (y con el servidor de teselas del mapa base). |
| Errores | 422/400 → se muestran los campos de `detail` (formato FastAPI). 5xx, timeout (8 s conexión / 25 s lectura) y caída de red tienen mensaje propio y botón «Reintentar». |
| Fechas | ISO-8601 (`2026-11-14` o `2026-11-14T20:15:00Z`). Otro formato se muestra tal cual. |

## 2. Endpoints

| Método y ruta | Uso en Android | Estado |
|---|---|---|
| `GET /api/health` | Tarjeta «Conexión» del inicio | ✅ ruta · 🟡 cuerpo |
| `GET /api/context/cuatro-canadas` | «Fuentes y límites» → contexto histórico | ✅ ruta · 🟡 cuerpo |
| `GET /api/nasa/status` | «Datos actualizados al: …» | ✅ ruta · 🟡 cuerpo |
| `GET /api/indicators?lat&lon&period` | Módulo 03 (tarjetas + mapa de calor) | ✅ ruta · 🟡 cuerpo |
| `POST /api/rotations/compare` | Resultado y módulos 01, 02 y 04 | ✅ ruta y campos · 🟡 formas internas |
| `GET /api/map/layers` | «Explorar mi zona» | ✅ ruta · 🟡 cuerpo |
| `GET /api/sources` | «Fuentes y límites» | ✅ ruta · 🟡 cuerpo |
| `GET /api/field-node/readings?node_id&source&since` | «Datos de terreno» | ✅ ruta y lectura v1 · 🟡 envoltorio |
| `GET /api/field-node/status` | «Datos de terreno» | ✅ ruta · 🟡 cuerpo |
| `POST /api/field-node/readings` | **No lo usa Android** (nodo / simulador / Wokwi) | — |

Android llama a `GET /api/field-node/readings` **sin parámetros** y separa por `source` en pantalla. ❓ Si el backend
exige `node_id` o limita por defecto la cantidad de lecturas, avisar.

## 3. `POST /api/rotations/compare`

### Request (✅ forma exacta del PDF)

```json
{
  "site": { "lat": -17.2374, "lon": -62.2427 },
  "current_rotation": ["soy", "sorghum"],
  "priorities": { "water": 0.55, "soil": 0.25, "risk": 0.2 },
  "constraints": { "irrigation": "rainfed", "excluded_crops": [] },
  "notes": "Mi terreno suele secarse en invierno"
}
```

| Campo | Qué manda Android | Estado |
|---|---|---|
| `site` | Punto piloto por defecto; el agricultor puede cambiarlo dentro de ±0.5° (caja de la UI) | ❓ el backend debe validar el alcance real y responder 422 si está fuera |
| `current_rotation` | `[cultivo_actual, cultivo_anterior]` | ❓ **confirmar el orden** (¿actual→anterior o verano→invierno?) |
| Códigos de cultivo | `soy`, `maize`, `sorghum`, `sunflower`, `wheat`, `fallow` | ✅ (PDF 4.4) |
| `priorities` | Suma 1. Partida según «¿Qué te preocupa más?» | ✅ `0.40/0.35/0.25` y «Falta de agua → agua 0.55» · ❓ resto (ver abajo) |
| `constraints.irrigation` | `irrigated` (Sí) · `rainfed` (No) · `partial` (A veces) · `unknown` (No sé) | ✅ `rainfed` · ❓ **los otros tres códigos** |
| `constraints.excluded_crops` | Códigos elegidos en el resumen | ✅ |
| `notes` | Texto libre, sin interpretar | ✅ |

Pesos de partida de la UI (❓ supuestos, salvo los marcados): `Falta de agua` 0.55/0.25/0.20 (✅ el 0.55) ·
`Cuidar suelo` 0.25/0.55/0.20 · `Evitar pérdidas` 0.25/0.20/0.55 · `Quiero equilibrar` 0.40/0.35/0.25 (✅).
Están en `domain/FarmerModels.kt` (`Concern`, `Irrigation`).

### Response

Nivel superior (✅): `site`, `data_status`, `alerts`, `strategies`, `recommended_strategy_id`, `limitations`.

| Campo | Formas que acepta la app | Estado |
|---|---|---|
| `data_status` | Objeto libre. Se buscan `last_observation_date` (o `last_observation`, `last_valid_date`, `latest_observation_date`) y `downloaded_at` (o `last_download`, `last_successful_download`, `fetched_at`). El resto se lista tal cual en el módulo 04. | 🟡 |
| `alerts[]` | Texto, u objeto `{message\|text\|title, date\|period\|as_of, level\|severity, source, evidence_status}` | 🟡 **Toda alerta debería traer `date`** |
| `recommended_strategy_id` | `"E0"…"E3"` o `null`. Si es `null`, vacío o no coincide con ninguna estrategia → «Información insuficiente para ordenar opciones». | ✅ |
| `limitations` | Lista de textos | ✅ |
| `strategies[].id` / `name` | `E0`…`E3` + nombre visible | ✅ |
| `strategies[].sequence` | `["soy","sorghum"]` **o** `[{"season":"summer\|winter","crop":"soy"}]`. Solo con `season` la app rotula «Verano / Invierno». | 🟡 **preferido: objetos con `season`** |
| `strategies[].score` | Número 0–100 (o `{value\|median}`), `null` si no hay ranking | ✅ escala 0–100 |
| `strategies[].partial_score` | Booleano opcional. Si falta, la app marca «puntaje parcial» cuando hay `score` y algún criterio `null`. | ❓ |
| `strategies[].criteria` | `{"water":62.1,"soil":55,"risk":null}` **o** `{"water":{"score":62.1,"detail":"…","unit":"…","status":"…"}}`. Claves aceptadas: `water\|agua\|s_agua`, `soil\|suelo\|s_suelo`, `risk\|riesgo\|s_riesgo`. | 🟡 |
| `strategies[].uncertainty` | `{p10, p50\|median, p90, rank_stability\|ranking_stability, confidence_level\|confidence, confidence_reason}` | 🟡 nombres exactos |
| `rank_stability` | Fracción 0–1 (≤ 1 se muestra × 100) o porcentaje | ❓ confirmar unidad |
| `confidence_level` | `low\|medium\|high` (o `baja\|media\|alta`). **`high` se rotula «Media (limitada: datos regionales)»** por la política 3.7 del plan. | ✅ política |
| `reasons` | Lista de textos (o un texto) | ✅ |
| `evidence[]` | `{indicator, value, unit, source, period, scale, evidence_status}` | ✅ |
| `evidence_status` | `documented`, `derived`, `assumption`, `farmer_provided`, `unavailable` | ✅ |
| `tradeoffs` | `{advantage, disadvantage, main_risk}` (alias: `main_advantage`, `benefit`, `main_disadvantage`, `drawback`, `risk`) | 🟡 nombres exactos |
| `missing_information[]`, `agronomist_questions[]` | Listas de textos | ✅ |

**Módulo 04 «Qué viene de NASA»:** la app lista la evidencia con `evidence_status = documented` cuyo `source` contiene
«NASA». Para que esa sección no quede vacía, el `source` debe nombrar el producto (p. ej. `NASA POWER`).

**Menos de 3 estrategias:** la app muestra las que lleguen y el texto de `limitations`. No inventa E1–E3.

## 4. `GET /api/indicators`

🟡 El PDF solo dice «indicadores con unidades, procedencia y calidad; grilla mensual». Forma **propuesta** (la que usa el fixture demo):

```json
{
  "period": "2015-2026",
  "last_observation_date": "2026-11-10",
  "indicators": [
    { "id": "dry_spell_max", "name": "Racha seca más larga", "value": 24, "unit": "días",
      "period": "…", "source": "NASA POWER", "scale": "regional (~50 km)",
      "evidence_status": "derived", "level": "medium", "description": "…",
      "quality": { "valid_days": 4012, "missing_pct": 0.4 } }
  ],
  "monthly_grid": {
    "indicators": [
      { "id": "dry_spell_max", "name": "…", "unit": "días", "source": "…", "scale": "…",
        "cells": [ { "year": 2015, "month": 1, "value": 6, "quality": "ok" },
                   { "year": 2026, "month": 12, "value": null } ] }
    ]
  }
}
```

También se aceptan: grilla bajo `grid` / `heatmap`; lista plana de celdas `{year, month, <indicador>: valor}` o
`{year, month, indicator, value}`; y mapa por año `{"dry_spell": {"values": {"2015": [12 valores]}}}`.

- Sin grilla → la pantalla dice que no hay mapa de calor. **No se fabrica.**
- Celdas `null` → se dibujan como «sin dato».
- Valores con signos mezclados (p. ej. anomalía de lluvia) → escala divergente con gris al centro.
- «Baja cobertura» solo aparece como riesgo si llega un indicador con `ndvi` / `cover` en el `id` **y** valor no nulo.
- ❓ `level` (`low|medium|high`) es opcional; si no llega no se muestra semáforo.

## 5. `GET /api/map/layers`

🟡 Forma propuesta: `{"layers":[ … ]}` (o lista directa). La app siempre muestra **5 capas** y empareja por `id`:

| Capa | `id` aceptados |
|---|---|
| Lluvia | `rain`, `lluvia`, `precipitation` |
| Humedad del suelo | `soil_moisture`, `humedad_suelo`, `smap` |
| Calor | `heat`, `calor`, `temperature` |
| Vegetación | `vegetation`, `vegetacion`, `ndvi` |
| Agua | `water`, `agua`, `water_balance`, `balance_hidrico` |

Campos por capa: `id`, `status`, `value`, `unit`, `source`, `date`, `resolution`, `legend`, `interpretation`,
`explanation`, `evidence_status`, `tile_url`, `attribution`.

| `status` | Cómo se muestra |
|---|---|
| `unavailable` / `missing` / `none`, o capa ausente | «Datos no disponibles» |
| `regional` / `point`, o solo `value` sin teselas | «Información regional» (un valor, sin mapa de calor) |
| `available` **con** `tile_url` | Se dibuja sobre el mapa |
| `available` **sin** `tile_url` | Se trata como «Información regional» |

- `tile_url`: plantilla `https://…/{z}/{x}/{y}.png` o WMTS-REST con `{TileMatrix}/{TileRow}/{TileCol}`, en Web Mercator
  (EPSG:3857 / `GoogleMapsCompatible`). ❓ **No probado con una capa real.** La app nunca construye estas URL.
- `legend`: lista de `{label, color}` (color `#RRGGBB`) o de textos.

## 6. Nodo de campo

### Lectura (✅ contrato v1, PDF 9.2)

`schema_version`, `node_id`, `source`, `scenario`, `ts`, `internal{depth_cm, zones[{id, soil_moisture_vwc,
soil_moisture_raw, soil_temp_c, flags}]}`, `external{bme680{t_c, rh_pct, p_hpa, gas_kohm}, dht11{t_c, rh_pct},
ldr{raw, mv, light_pct, r_fixed_ohm}}`, `calibration{moisture_method, date, ds18b20_offsets_c}`, `quality_flags`.

| Regla | En la app |
|---|---|
| `soil_moisture_vwc` en **m³/m³** | Se muestra `× 100` como % y también el valor original. |
| `source` ∈ `simulated`, `wokwi`, `physical` | Insignia SIMULADO / WOKWI / FÍSICO siempre visible. Otro valor → «ORIGEN NO INFORMADO» (nunca «Físico»). |
| Varias fuentes mezcladas | Se muestran por separado; nunca en la misma curva. |
| `ts` ausente | Se usa `received_at` si viene. |
| `moisture_method = gravimetric_3pt` | «Humedad calibrada con 3 puntos». Otro valor se muestra literal; `null` → «calibración no informada». |
| Bandera `moisture_uncalibrated` | «Humedad SIN calibrar». |
| Datos faltantes | `null` → «sin dato»; las banderas se listan. |

### `GET /api/field-node/readings` (🟡 envoltorio)

Lista directa `[lectura, …]` u objeto `{"readings":[…]}` (también `items`, `data`, `results`). Una lectura con forma
inválida se descarta y se cuenta («N lecturas con formato inválido»).

### `GET /api/field-node/status` (🟡)

Campos buscados: `last_contact` (o `last_seen`, `last_received_at`), `node_id`, `source`, `calibration{moisture_method, date}`,
`quality_flags`.

❓ **Pedido al backend:** para la banda «Dato regional (~50 km): contexto, no tu campo» la app busca dentro del status un
objeto `power_context` (o `regional_context`): `{ "date": "2026-11-14", "t2m_c": 27.4, "rh2m_pct": 58.0, "source": "NASA POWER" }`.
Si no llega, la banda dice que no hay dato regional. El PDF pide la banda pero no dice de qué endpoint sale.

## 7. `GET /api/nasa/status`, `/api/health`, `/api/sources`, `/api/context/cuatro-canadas`

| Endpoint | Qué usa la app | Estado |
|---|---|---|
| `/api/health` | HTTP 200 = conectado. Si trae `{"status": …}` distinto de `ok/healthy/up` u `{"ok": false}` → «no está listo». | 🟡 |
| `/api/nasa/status` | `last_observation_date` y `last_download` (se buscan también dentro de sub-objetos por fuente) | 🟡 |
| `/api/sources` | Lista (o `{"sources":[…]}`) de `{title\|name, url\|source_url, description, reference_year, geographic_scope}` | 🟡 |
| `/api/context/cuatro-canadas` | Objeto libre: cada clave de primer nivel se muestra como una sección «etiqueta: valor», rotulada como contexto histórico | 🟡 |

## 8. Modo demo (fixtures)

- `FakeFarmealoTuRepository` devuelve datos **de ejemplo** con la forma de este contrato. Se activa en «Conexión» o con
  `farmealotu.demoMode=true` en `local.properties`.
- Toda pantalla en ese modo lleva la franja **«MODO DEMO · DATOS DE EJEMPLO»**; al cambiar de modo se borran los resultados.
- Para ensayar el cambio de prioridades, el falso backend combina subpuntajes fijos con los pesos. Eso imita al servidor;
  **no** es el motor real y no debe usarse como evidencia ni en capturas para el jurado.
- El demo no trae teselas, ni NDVI, ni SMAP: esas capas quedan «Datos no disponibles».

## 9. Pendientes para cerrar con Matías y Alvin

1. Publicar `openapi.json` y regenerar/ajustar `ApiDtos.kt`.
2. Confirmar: orden de `current_rotation`, códigos de `irrigation`, forma de `sequence`, `criteria`, `tradeoffs` y `uncertainty`.
3. Confirmar la forma de la grilla mensual de `/api/indicators` y el `id` de cada indicador.
4. Confirmar `/api/map/layers` (ids, `status`, `tile_url`) y probar una capa GIBS real en el visor.
5. Decidir de dónde sale `power_context` para «Datos de terreno».
6. Confirmar si `rank_stability` va en 0–1 o en %.
7. CORS no aplica a Android, pero el servidor debe escuchar en `0.0.0.0` para el celular físico.
