# PROMPT DEFINITIVO — Claude Code · FarmealoTu Android (Kotlin + Jetpack Compose)

> **Uso:** guarda este archivo en la **raíz del repositorio Git abierto en Android Studio**, junto al PDF `FarmealoTu_Plan_de_Proyecto.pdf`. Desde una terminal abierta en esa raíz, asegúrate de estar en `ui_kendra`, ejecuta `claude` y escribe: **«Lee `FarmealoTu_Claude_Code_UI_Kotlin.md` y `FarmealoTu_Plan_de_Proyecto.pdf`. Ejecuta las instrucciones del prompt, trabajando solamente en la rama `ui_kendra`. Empieza por la inspección del repositorio y luego implementa la aplicación.»**

---

## 0. TU MISIÓN: IMPLEMENTAR, NO SOLO PLANIFICAR

Eres un **ingeniero Android senior + arquitecto móvil + diseñador UX para agricultores + integrador REST**. Construye una aplicación Android **nativa, compilable, utilizable y conectable a la API real**, denominada **FarmealoTu**, para el desafío NASA Space Apps Challenge 2026 **«Field Shift: Adapting Farms with NASA Data»**.

**Entregable principal:** el frontend Android de FarmealoTu, con Kotlin + Jetpack Compose, dentro del proyecto Android Studio y la rama **`ui_kendra`**. Debe guiar a un agricultor de **Cuatro Cañadas, Santa Cruz, Bolivia**, desde cinco preguntas sencillas hasta una recomendación de rotación **calculada por el backend**, con evidencia, comparación, riesgos e incertidumbre.

**No entregues únicamente diagramas, mockups, descripciones, TODOs ni archivos sueltos para que el usuario copie. Edita los archivos del repositorio, compila, comprueba y corrige.** Usa el tiempo de hackathon de forma pragmática: primero recorrido funcional extremo a extremo, luego calidad visual y extras.

---

## 1. FUENTES, PRECEDENCIA Y CAMBIOS AUTORIZADOS

1. **Lee íntegramente las 44 páginas** de `FarmealoTu_Plan_de_Proyecto.pdf` en la raíz del repositorio. Si tu herramienta no extrae el contenido directamente, usa `pdftotext`, PyMuPDF u otra utilidad local disponible; comprueba especialmente tablas y diagramas. Si falta el PDF, informa qué archivo falta y continúa únicamente con el contexto explícito de este prompt, sin fingir haberlo leído.
2. Las prioridades del PDF siguen vigentes, **excepto dos decisiones expresas del equipo**:
   - **Nombre final: `FarmealoTu`**, nunca `RotaSat` como marca mostrada al usuario. Respeta identificadores históricos de integración cuando cambiarlos rompa un contrato existente, como el `node_id` del ESP32.
   - **Frontend: Android Studio + Kotlin + Jetpack Compose**, NO React/Vite/Tailwind/TypeScript ni WebView para implementar las pantallas.
3. Conserva el resto del PDF: alcance hiperlocal, fórmulas y motor en Python, funciones de cada pantalla, criterios de aceptación, separación satélite/sensor, esquema del nodo y etiquetas de procedencia.
4. Secciones esenciales: **0** (decisiones), **2** (problema), **3.2–3.8** (arquitectura, datos, motor y JSON de rotaciones), **4.1–4.8** (UX, pantallas y API), **7** (responsabilidades), **9.1–9.2** (contratos), **10** (contingencias), **11** (pruebas A–N), **13** (backlog).
5. **No reinterpretar números científicos**: el backend calcula agua/suelo/riesgo, MCDA con pesos iniciales `0.40/0.35/0.25`, estrategias E0–E3 y bootstrap 200 veces con semilla 42. Android **muestra** resultados, reenvía entradas y prioridades, pero **no duplica ni reemplaza el motor científico**.

---

## 2. GIT: RESTRICCIÓN OBLIGATORIA DE RAMAS

Este repositorio contiene tres ramas de equipo:

| Rama | Propósito | Política en esta sesión |
|---|---|---|
| `master` | Integración final estable | **No editar, no fusionar, no publicar** |
| `ui_kendra` | UI Android de FarmealoTu | **Única rama sobre la que debes trabajar** |
| `backend` | Python, NASA, FastAPI, motor de recomendación y nodo | **Trabajo de otros integrantes: no editar ni fusionar** |

**Antes de cualquier cambio:**

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short
git branch -a
```

- Comprueba que la rama actual es exactamente `ui_kendra`. **Si no lo es, detente y explica el comando para cambiar de rama; no modifiques archivos antes de corregirlo.**
- No ejecutes `git switch backend`, `git switch master` ni fusiones en el directorio activo mientras desarrollas la UI.
- **No borres** ni reinicialices `.git`, el proyecto Android existente, archivos de colaboradores ni datos locales.
- Prohibido `git reset --hard`, `git clean -fd`, `push --force`, rebase de ramas compartidas o commits/push/PR sin autorización del usuario.
- Si detectas cambios sin commit, presérvalos; inspecciónalos antes de editar el mismo archivo.
- **No intentes implementar el backend desde esta rama.** Si hay código backend dentro de la carpeta visible, trátalo como solo lectura salvo una corrección expresamente solicitada por el equipo.
- Para revisar contratos de otra rama sin cambiar de rama activa, usa documentación, `/docs`, una URL real de la API, o lectura no destructiva como `git show origin/backend:<ruta>` **si la rama remota está disponible**. No supongas que el backend ya está en GitHub o desplegado. Si se necesita un trabajo paralelo local, sugiere `git worktree` en otra carpeta, pero no lo crees sin necesidad.
- Cuando el backend esté listo, su integración final se hará mediante PR/revisión por el equipo hacia `master`; **no automatices ese merge**.

**Definición de éxito Git:** al terminar, `git branch --show-current` devuelve `ui_kendra` y `git diff` contiene únicamente los cambios deliberados de UI, configuración Android y documentación propia de la integración.

---

## 3. LIMITACIÓN DE ALCANCE Y RESPONSABILIDADES

En el PDF, Kendra es responsable de la interfaz y recibe de Matías (API/motor) y Alvin (NASA/indicadores) los contratos y datos. Adapta ese trabajo al stack Android.

**Tú implementas en `ui_kendra`:**

- Navegación Kotlin/Compose, temas, accesibilidad, pantallas, componentes y estados.
- DTOs de request/response; Retrofit/OkHttp; `Repository`, `ViewModel`, `StateFlow`, coroutines.
- Flujo de captura de información agrícola, edición, prioridades y consulta a la API.
- Presentación de recomendaciones, barras de comparación, radar, heatmap temporal e intervalos P10–P90.
- Visor geoespacial Android con capas reales o estados de indisponibilidad.
- Pantalla «Datos de terreno» consumiendo API de lecturas ESP32.
- Configuración de URLs debug/emulador/celular, pruebas móviles, documentación de conexión y build APK.

**No implementas en esta rama:** API FastAPI, ingestión NASA, cálculos ET0, generación agronómica de estrategias, bootstrap, firmware del ESP32, gemelo digital ni datos de producción inventados. Estos pertenecen a `backend` o `hardware`, gestionados por otros integrantes.

**Diseño para bloqueo mínimo:** si API no está lista, crea un `FakeFarmealoTuRepository` **intercambiable** y **etiquetado DEMO**. No dejes los resultados de demo mezclados con resultados reales ni uses mocks como evidencia NASA. Debe existir un `RemoteFarmealoTuRepository` consumiendo la API y un modo de selección controlado por configuración debug. No se envían datos simulados al endpoint real sin intención explícita.

---

## 4. STACK ANDROID Y CRITERIOS DE INGENIERÍA

Aprovecha el Empty Project ya generado; **no lo sustituyas por otro proyecto** y preserva `namespace` y `applicationId` existentes.

- **Kotlin, Jetpack Compose, Material 3**.
- **Navigation Compose** para navegación.
- **ViewModel + StateFlow + coroutines** para estado y concurrencia.
- **Retrofit + OkHttp + Kotlinx Serialization** (o un conversor coherente ya presente en el proyecto) para REST.
- Arquitectura ligera **MVVM + Repository**, sin sobreingeniería ni frameworks DI obligatorios; inyección manual simple si es más rápida.
- **Compose Canvas** para barras especiales, radar, cuadrícula y rangos de incertidumbre; gráficos sobrios y legibles. No importes bibliotecas de gráficos pesadas solo por estética.
- Para el mapa usa una biblioteca Android compatible con el Gradle/SDK existente, preferiblemente una opción OpenStreetMap sin API key comercial, integrada con `AndroidView` si hace falta. Verifica disponibilidad y atribución de teselas. En caso de bloqueo técnico, entrega primero una selección geográfica con coordenadas y lista de cinco capas, sin fingir geodatos.
- `DataStore` es opcional para conservar las respuestas del agricultor y la configuración; conserva el estado con `SavedStateHandle`/ViewModel si es suficiente. No introduzcas Room por defecto.
- Sigue el catálogo de versiones Gradle existente, detecta versiones compatibles y evita upgrades masivos innecesarios.
- Crea código compile-ready: imports completos, manejo de nulos, estados de carga/error, sin placeholders `TODO()` en P0.
- `INTERNET` en `AndroidManifest.xml`. Permisos y políticas de HTTP solo para desarrollo; HTTPS en demo pública.

### Estructura sugerida dentro del paquete actual

```text
app/src/main/java/<paquete_existente>/
  MainActivity.kt
  data/
    remote/FarmealoTuApi.kt
    remote/ApiClient.kt
    model/ApiDtos.kt
    repository/FarmealoTuRepository.kt
    repository/RemoteFarmealoTuRepository.kt
    repository/FakeFarmealoTuRepository.kt
  ui/
    navigation/FarmealoTuNavHost.kt
    screens/home/HomeScreen.kt
    screens/input/FarmerInputScreen.kt
    screens/decision/DecisionScreen.kt
    screens/compare/CompareRotationsScreen.kt
    screens/tradeoffs/TradeoffsScreen.kt
    screens/risks/RisksScreen.kt
    screens/uncertainty/UncertaintyScreen.kt
    screens/geo/GeoViewerScreen.kt
    screens/field/FieldDataScreen.kt
    components/ (tarjetas, chips, semáforos, barras, rango, leyendas)
    theme/ (colores, tipografía, componentes Material 3)
  viewmodel/ (o por pantalla; evita duplicación)
  domain/ (solo si simplifica modelos y mapeos)
```

La estructura es sugerida, no un requisito de añadir carpetas vacías. Mantén un diseño consistente con el proyecto real.

---

## 5. CONTRATO REST: SOURCE OF TRUTH = API REAL + PDF

**No inventes un contrato incompatible con el backend del compañero.** El PDF contiene el contrato inicial; cuando `/docs` o `openapi.json` reales estén disponibles, reconcilia DTOs contra la versión real y registra cualquier discrepancia. No modifiques el servidor desde esta rama.

### Endpoints del PDF (página 18)

| Método | Ruta | Uso Android |
|---|---|---|
| GET | `/api/health` | Estado del backend |
| GET | `/api/context/cuatro-canadas` | Cultivos, prácticas y contexto histórico |
| GET | `/api/nasa/status` | Fuente NASA, última observación, caché |
| GET | `/api/indicators?lat&lon&period` | Riesgos, métricas y heatmap mensual |
| POST | `/api/rotations/compare` | Comparar E0–E3, ranking y explicación |
| GET | `/api/map/layers` | Cinco capas y metadatos |
| GET | `/api/sources` | Bibliografía y procedencia |
| POST | `/api/field-node/readings` | Lo usa el nodo/simulador; Android normalmente solo lee |
| GET | `/api/field-node/readings?node_id&source&since` | Gráficas y lecturas del nodo |
| GET | `/api/field-node/status` | Última conexión, calibración y calidad |

Request de comparación (modelo inicial del PDF):

```json
{
  "site": { "lat": -17.2374, "lon": -62.2427 },
  "current_rotation": ["soy", "sorghum"],
  "priorities": { "water": 0.40, "soil": 0.35, "risk": 0.25 },
  "constraints": { "irrigation": "rainfed", "excluded_crops": [] },
  "notes": "Mi terreno suele secarse en invierno"
}
```

Respuesta mínima de comparación definida en el documento:

- `site`, `data_status`, `alerts`, `strategies`, `recommended_strategy_id`, `limitations`.
- Por estrategia: `id`, `name`, `sequence`, `score`, `criteria`, `uncertainty`, `reasons`, `evidence[]`, `tradeoffs`, `missing_information[]`, `agronomist_questions[]`.
- Los elementos `evidence` deben llevar indicador, valor, unidad, fuente, período, escala y `evidence_status` cuando lo proporcione la API.
- En la deserialización, trata correctamente los valores faltantes y los campos opcionales. **No conviertas `null` en 0** ni inventes porcentajes.
- En los cambios de prioridades, normaliza pesos para que sumen 1 y envíalos al **mismo endpoint**, sin hacer una llamada nueva a NASA desde Android.
- Cuando la API devuelva menos de tres alternativas defendibles, muestra las razones sin inventar estrategias E1–E3.

**Procedencia obligatoria:** `documented` (documentado/observado), `derived` (derivado), `assumption` (supuesto), `farmer_provided` (del agricultor), `unavailable` (no disponible); y para nodo `simulated`, `wokwi`, `physical`. Muéstralas con etiquetas comprensibles.

### URL base Android

- **Emulador Android estándar**: `http://10.0.2.2:8000/` si FastAPI corre en el equipo anfitrión.
- **Celular físico**: `http://<IP_LAN_DEL_PC>:8000/` (el PC y el celular en la misma red; FastAPI escuchando en `0.0.0.0`, firewall accesible).
- **Demo**: URL HTTPS real del backend desplegado o túnel, configurable sin editar cada pantalla.
- `localhost` **dentro de Android** no apunta al PC.
- Configura `BuildConfig`/`local.properties` no versionado o una interfaz de configuración debug, sin publicar secretos. Cleartext solo para builds debug/hosts de desarrollo si la plataforma lo requiere.

### Resiliencia

Implementa tipos de estado `Loading`, `Success`, `Empty`, `Error`, `Demo` o equivalentes. Maneja 422 (campos), 500, timeout, caída de red y datos obsoletos. Nunca cierres la app ni muestres pantallas vacías. Muestra botón «Reintentar» y **fecha de actualización**. Los fixtures de demo deben tener marca visible en toda pantalla que los utilice.

---

## 6. EXPERIENCIA GENERAL: AGRICULTORES PRIMERO

El PDF manda que la experiencia se entienda **en menos de 3 minutos**. En Android esto significa:

- **Español sencillo y voseo**: «¿Qué sembrás?», «¿Tenés riego?», «Revisá con tu técnico».
- Texto principal **≥ 16 sp**, botones cómodos (área táctil preferiblemente ≥ 48 dp), contraste alto, fácil de usar con una mano.
- Uso de **Material 3** y tema agrícola coherente; fondo claro, verdes y tonos tierra, alertas claramente diferenciadas. Prioriza legibilidad frente a una estética de ciencia ficción.
- Inicio centrado en **qué hacer / qué revisar**, no un mosaico de datos científicos.
- No mostrar un puntaje como probabilidad de éxito, porcentaje de ganancia o rendimiento previsto.
- Cada indicador importante debe abrir «¿Qué significa?» con fuente, fecha, unidad, escala y limitación.
- Semáforos **con texto** (no solo colores); lectores de pantalla y descripciones accesibles.
- Navegación clara, desde cualquier pantalla regreso a inicio y opción «Editar mis datos».
- Sin login, pagos, chatbots ni menús ajenos al problema.
- Advertencia visible: **«Esta herramienta apoya tu decisión; no reemplaza tu experiencia ni a tu técnico.»**
- No presentarse como producto oficial de NASA ni afirmar que NASA recomienda rotaciones; **NASA aporta datos, FarmealoTu los interpreta**.

### Flujo funcional

`Inicio → 5 preguntas → Resultado → 01 Comparar / 02 Ventajas y desventajas / 03 Riesgos / 04 Incertidumbre`, con accesos secundarios a `Explorar mi zona` y `Datos de terreno`. Resultados y datos ingresados se mantienen al navegar o rotar la pantalla.

---

## 7. PANTALLAS A IMPLEMENTAR (ESPECIFICACIÓN FUNCIONAL COMPLETA)

### 7.1 HomeScreen — «Mi campo — Cuatro Cañadas»

Orden de componentes **exacto** según página 16:

1. Encabezado **FarmealoTu** / «Mi campo — Cuatro Cañadas».
2. «Datos actualizados al: [fecha REAL de observación disponible]»; distingue fecha de descarga.
3. Tarjeta principal con **acción a revisar** derivada de la respuesta real o, si no hay consulta todavía, invitación a registrar el campo.
4. Advertencias pertinentes fechadas; **no reutilizar como alertas actuales** las noticias de inundación de 2025 y sequía de 2025–2026.
5. CTA muy visible «Comparar opciones de siembra».
6. Accesos a los **cuatro módulos** de decisión.
7. Accesos secundarios «Explorar mi zona» y «Datos de terreno».
8. Estado de conexión con el backend cuando aporte valor.

No muestres charts técnicos en el inicio.

### 7.2 FarmerInputScreen — 5 preguntas editables

Hazlo en pasos cortos o tarjetas claras, nunca un formulario interminable:

1. **«¿Qué sembrás actualmente?»**: texto libre + chips `Soya`, `Maíz`, `Sorgo`, `Girasol`, `Trigo`, `Otro`.
2. **«¿Qué sembraste antes?»**: texto libre + chips + `Barbecho/descanso`.
3. **«¿Qué te preocupa más?»**: `Falta de agua`, `Cuidar suelo`, `Evitar pérdidas`, `Quiero equilibrar`.
4. **«¿Tenés riego?»**: `Sí`, `No`, `A veces`, `No sé`.
5. **«¿Querés contar algo de tu terreno?»**: texto libre opcional, no interpretarlo con IA.

Normaliza tildes y sinónimos con un diccionario **verificable** (`soya/soja/soy → soy`, `maíz/maiz → maize`, `sorgo → sorghum`, `girasol → sunflower`, `trigo → wheat`, `barbecho/descanso → fallow`). Si no reconoces el cultivo, pide elegir de la lista; **no adivines**. Traduce el riego a los códigos reales aceptados por la API, validándolos con OpenAPI.

Al finalizar, muestra resumen editable, permite restricciones si API las ofrece y envía `POST /api/rotations/compare`. Valor inicial de ubicación de referencia: **lat -17.2374, lon -62.2427** (punto piloto, no necesariamente la finca real). Permite cambiarlo dentro del alcance Cuatro Cañadas sin prometer precisión a escala de parcela.

### 7.3 DecisionScreen — Resultado principal

Tarjeta entendible en menos de 30 segundos:

- Recomendación mejor posicionada **solo cuando `recommended_strategy_id` sea válido**.
- Secuencia verano/invierno, razón principal, advertencia/riesgo principal.
- Calidad/confianza y «Revisá con tu técnico».
- Botones «Comparar todas», «Cambiar mis prioridades», «¿Por qué?».
- Si el backend no puede generar ranking, mostrar **«Información insuficiente para ordenar opciones»** y las razones, sin forzar ganador.

### 7.4 Module 01 — CompareRotationsScreen

Título visible: **«Comparar formas de sembrar»**.

Presentar E0 Actual, E1 Cuidar suelo, E2 Cuidar agua, E3 Equilibrada **cuando existan**. Cada tarjeta incluye:

- Secuencia de cultivos por campaña.
- Barras horizontales para `S_agua`, `S_suelo`, `S_riesgo` y puntaje total, **solo cuando son válidos**.
- Leyenda visible: «Puntaje relativo entre opciones, no probabilidad de éxito».
- Ventaja más importante, desventaja más importante, riesgo principal.
- «¿Por qué esta opción?» abre explicaciones, evidencia, vacíos y preguntas para el agrónomo.
- Comparación legible sin exigir interpretar gráficos.

Incorpora controles de prioridades. Un ajuste dispara nueva comparación al backend (usa un botón «Actualizar comparación» o debounce para no enviar peticiones excesivas). La clasificación puede cambiar **solo si los cálculos del backend cambian**.

### 7.5 Module 02 — TradeoffsScreen

Título: **«Ventajas y desventajas»**.

- Primero frases simples **derivadas de `tradeoffs` y criterios reales**.
- Luego una sección **plegable**, cerrada por defecto: «Ver comparación técnica».
- Radar con 3 ejes: Agua, Suelo y Riesgo, dibujado en **Compose Canvas** con etiquetas, leyenda y números reales.
- No dibujes vértices falsos cuando un criterio sea `null`; explica «dato insuficiente».
- Nunca muestres el radar por delante de las conclusiones comprensibles.

### 7.6 Module 03 — RisksScreen

Título: **«Conocer los riesgos»**.

- Tarjetas sobre rachas secas, calor, lluvias intensas y baja cobertura **solo si hay NDVI verificable**.
- **Heatmap temporal interactivo** con eje horizontal de meses y vertical de años (2015–2026, solo períodos realmente disponibles). Celdas coloreadas según un indicador de `/api/indicators`, no celdas simuladas como observaciones.
- Leyenda de rangos y descripción del indicador.
- Al tocar la celda: mes, año, valor, unidad, fuente, calidad, alcance espacial.
- Si la API no entrega grilla, **mostrar ausencia de datos** o un resumen de indicadores; no fabricar la grilla.
- Diferencia explícita: exposición a lluvia extrema **no equivale** a predicción de desborde del río.

### 7.7 Module 04 — UncertaintyScreen

Título: **«¿Qué tan segura es esta comparación?»**.

- Dibujar intervalo horizontal P10–P90 con marca P50 por estrategia (Compose Canvas o Layout Compose).
- Mostrar estabilidad de ranking y nivel de confianza **solo si están en la API**.
- Explicar: qué viene de NASA, qué calculó FarmealoTu, qué indicó el agricultor, supuestos, información que falta y preguntas para su agrónomo.
- Texto preciso: «Estos rangos muestran cómo varía la evaluación al reutilizar distintos años del historial; no predicen el rendimiento».
- Con datos regionales y parámetros genéricos, **nunca etiquetar confianza como alta**.

### 7.8 GeoViewerScreen — «Explorar mi zona»

Sustituye la implementación React/Leaflet del PDF por Android nativo:

- Mapa 2D Android con punto piloto y, si existe, ubicación seleccionada por el agricultor.
- Pila **isométrica conceptual de cinco capas**, representada con Canvas/Compose. Puede usar polígonos escalonados, sombras moderadas y selección táctil, sin un motor 3D.
- Capas exactas: **Lluvia, Humedad del suelo, Calor, Vegetación y Agua**.
- Panel para la capa seleccionada: fuente, fecha, resolución, unidad, leyenda, interpretación y «¿Qué significa?».
- Los metadatos y URLs de capas reales provienen de `/api/map/layers`; **no inventes URL WMTS**, imágenes NASA ni rasters.
- NASA POWER (contexto regional) **no** se convierte en mapa de calor por parcela. Si solo existe valor de punto, muestra «Información regional». SMAP/NDVI/GIBS solo cuando realmente están disponibles.
- La capa «Agua» representa balance hídrico **derivado** cuando existe, **no una inundación observada**.
- Cuando la librería de mapa no pueda integrarse sin arriesgar el build, conserva un visor geoespacial simple con mapa o ubicación básica, capas seleccionables y metadatos; deja la pila isométrica como mejora posterior. El núcleo del MVP tiene precedencia.

### 7.9 FieldDataScreen — «Datos de terreno»

Recibe GET `/api/field-node/readings` y `/api/field-node/status`.

- Tarjetas **Zona A seca, B media, C húmeda**: humedad, temperatura, hora y calibración.
- **Humedad del nodo viene en `soil_moisture_vwc` (m³/m³)**: cuando se muestre como %, multiplicar por **100**, conservando precisión y procedencia. No confundir con un valor ya expresado en `%`.
- Una gráfica para humedad de 3 zonas y **otra separada** para temperatura de suelo (no doble eje).
- Aire `bme680` (T, RH, presión, gas), `dht11` (T, RH), diferencia entre sensores y `ldr` (luz relativa, si está disponible).
- Insignia de fuente **SIEMPRE visible**: `SIMULADO`, `WOKWI` o `FÍSICO`, derivada de `source`.
- Calidad/calibración: `gravimetric_3pt` si existe; de lo contrario describir lo que realmente dice el registro.
- Banda de comparación cualitativa con POWER: **«Dato regional (~50 km): contexto, no tu campo»**.
- Aviso permanente: **«Estos datos no cambian la recomendación en esta versión»**.
- Si no hay lecturas, mostrar una pantalla sin datos y estado del último contacto, no valores inventados.

#### Contrato JSON de nodo (página 38 del PDF)

El nodo envía `schema_version`, `node_id`, `source`, `scenario`, `ts`, `internal`, `external`, `calibration`, `quality_flags`.

- `internal.depth_cm`.
- `internal.zones[]`: `id`, `soil_moisture_vwc`, `soil_moisture_raw`, `soil_temp_c`, `flags`.
- `external.bme680`: `t_c`, `rh_pct`, `p_hpa`, `gas_kohm`.
- `external.dht11`: `t_c`, `rh_pct`.
- `external.ldr`: `raw`, `mv`, `light_pct`, `r_fixed_ohm`.
- `calibration.moisture_method`, `date`, `ds18b20_offsets_c`.
- `source` solo `simulated`, `wokwi`, `physical`.
- Datos faltantes: `null` + indicadores de calidad, **nunca ceros inventados**.

El ejemplo de la página 38 es ilustrativo. Usa su forma para definir DTOs o fixture claramente marcado como DEMO, nunca como medición real. Si el contrato implementado difiere del ejemplo, concilia con `/docs` y mantén compatibilidad sin modificar la API ajena.

---

## 8. INTEGRACIÓN, PRUEBAS Y CIENCIA HONESTA

**Reglas no negociables:**

1. La app Android **no calcula de nuevo ni altera** los puntajes E0–E3; muestra los que devuelve FastAPI.
2. No afirmes «NASA recomienda este cultivo». Fuente NASA y decisión de FarmealoTu deben distinguirse.
3. Las cifras de superficie/producción municipal de **2023** son contexto histórico, nunca valores actuales de cosecha.
4. La noticia de inundación de **marzo 2025** y la sequía/lluvias de la campaña **2025–2026** no son alertas en tiempo real.
5. NASA POWER es clima regional (~50–60 km), SMAP es estimación regional (~9 km); no atribuyas esas observaciones a una parcela exacta.
6. Puntajes relativos **no** son probabilidades de cosecha exitosa, predicciones de ganancias ni valores de rendimiento.
7. Bootstrap P10–P90 es **variabilidad del modelo bajo remuestreo histórico**, no pronóstico futuro.
8. No cambiar la condición de `unavailable` por cero o verde.
9. Si la API está caída y activas fixtures de desarrollo, mostrar una franja **«MODO DEMO · DATOS DE EJEMPLO»** persistente y no incluir esos pantallazos como evidencia real de NASA.
10. Mantener como mensaje visible que el agricultor y su técnico deciden.

### Pruebas que sí te competen

Del PDF, la UI debe permitir comprobar:

- **B:** mostrar rotación actual + alternativas reales cuando la API las ofrezca.
- **C:** cambiar pesos → solicitar recálculo → actualizar ranking y comparativas.
- **D:** suelo/NDVI/SMAP faltantes → UI funcional y vacíos explícitos.
- **E:** intervalos P10/P90 reales y comparación comprensible.
- **F:** recorrido en celular en menos de 3 minutos con un usuario no técnico.
- **G:** trazabilidad en cada resultado importante.
- **J:** nodo etiquetado según fuente, hora y calibración.

Las pruebas NASA A y del hardware H–N requieren trabajo de otros integrantes. No asegures que pasaron si no se ejecutaron.

Añade pruebas unitarias locales para normalización de cultivo, pesos, estados de error y mappers de DTO; UI tests de navegación si el entorno permite correrlos. Prueba escenarios: API disponible, timeout, 422, vacíos, `null`, 0 lecturas, `simulated`/`physical`, y rotación de pantalla.

---

## 9. PLAN DE EJECUCIÓN Y CHECKPOINTS (NO CAMBIAR EL ORDEN)

Trabaja en fases cortas y deja la app compilando tras cada hito.

### FASE 1 — Inspección + compilación de base (P0)

- Verifica `ui_kendra`, estado Git, SDK, Gradle y PDF.
- Lee las secciones críticas del PDF.
- Construye tema, `MainActivity`, `NavHost` y rutas de todas las pantallas.
- Añade Retrofit, DTOs y Repository intercambiable con fixtures explícitos.
- Implementa `HomeScreen` básica y captura de errores.
- **Ejecuta `gradlew.bat :app:assembleDebug`** (o variante real del proyecto) y corrige errores.

### FASE 2 — Usuario → API → Resultado (P0)

- Implementa cinco preguntas, sinónimos, resumen editable, valores iniciales.
- Conecta `POST /api/rotations/compare` real si el servidor está accesible.
- Implementa `DecisionScreen` y `CompareRotationsScreen` con barras verdaderas.
- Prueba entrada → consulta → renderizado; nunca bloquear por API ajena si no está disponible: usa fixtures explícitos y deja preparada la conexión remota.
- Compila.

### FASE 3 — Los cuatro módulos (P0)

- `TradeoffsScreen`: frases y radar plegado.
- `RisksScreen`: heatmap temporal + leyenda.
- `UncertaintyScreen`: P10–P90.
- Recalcular pesos con API; estados faltantes y explicaciones.
- Compila y corrige.

### FASE 4 — Nodo y geovisor (P1)

- `FieldDataScreen` conectada a endpoints del nodo; insignias de fuente y calibración.
- `GeoViewerScreen` con 5 capas seleccionables, leyendas y datos reales/regionales/indisponibles.
- Si el mapa isométrico retrasa demasiado, aplicar el plan B del PDF, documentado. No sacrifiques P0 por gráficos.
- Compila.

### FASE 5 — Validación final y APK

- Ejecuta build debug, pruebas unitarias disponibles, revisión de navegación y errores.
- Genera APK debug cuando el entorno lo permita; reporta la ruta real.
- Deja instrucciones para emulador y dispositivo físico.
- Muestra `git diff --stat`, `git status`, estado actual de la rama.
- Resume de forma verificable: **funciona / probado / depende del backend / no disponible**.
- No hagas commit ni push sin autorización.

**Checkpoint irrenunciable:** una persona puede abrir Android → ingresar cultivos/agua/prioridades → ver comparaciones derivadas de `/api/rotations/compare` o, durante el desarrollo solamente, de un fixture claramente rotulado → conocer riesgos e incertidumbre. El siguiente checkpoint es conectarlo a la API real y retirar el modo demo de la demostración final.

---

## 10. MODO DE COMUNICACIÓN EN ESTA SESIÓN

- **Empieza ahora**: inspección y ejecución. No te quedes esperando una confirmación por cada archivo.
- Si necesitas una decisión técnica de menor importancia, usa la alternativa más simple compatible y documenta por qué.
- Si no puedes leer el PDF, no tienes Gradle/SDK o el backend no está disponible, declara la limitación y realiza todo lo demás que sea posible; no informes como verificado lo que no ejecutaste.
- Si detectas riesgo de borrar código o modificar `master`/`backend`, **detente antes de la operación**.
- Tras cada fase, da un resumen corto de archivos, funcionalidad visible, comandos efectivamente ejecutados y bloqueos. Luego continúa con la siguiente fase.
- No modifiques el backend de los compañeros, aunque falten endpoints; registra en `docs/UI_BACKEND_CONTRACT.md` qué espera Android, qué está confirmado y qué está pendiente de la API.
- Crea `docs/ANDROID_RUN.md` con comandos de compilación, arranque del backend existente, conexión al emulador/celular y posibles errores.
- **No produzcas un diseño genérico**: todas las pantallas y textos deben responder al agricultor de Cuatro Cañadas y a los cuatro módulos exactos del PDF.

### TU PRIMERA ACCIÓN

Ejecuta los comandos de revisión Git, comprueba que estás en `ui_kendra`, lee el PDF, inspecciona Gradle/AndroidManifest/package name, crea la navegación y cliente de API, y **realiza la primera compilación antes de continuar con las demás pantallas**. Luego implementa el recorrido completo, respetando todas las restricciones anteriores.

**CRITERIO FINAL: FarmealoTu Android nativo, instalable, entendible para agricultores y preparado para un backend FastAPI real; rama `ui_kendra` intacta como contexto de trabajo, sin pisar `backend` ni `master`.**
