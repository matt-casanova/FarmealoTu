# Prompt para Claude Code — Alvin (FarmealoTu / RotaSat)

> Copiá todo lo que está debajo de la línea y pegalo en Claude Code, abierto en la carpeta del repo `FarmealoTu`.

---

Hola, Claude. Soy **Alvin**, de Ingeniería de Sistemas. Estoy en la hackatón **NASA Space Apps Challenge 2026**, reto **«Field Shift: Adapting Farms with NASA Data»**. Nuestro equipo se llama **FarmealoTu** (6 personas). La app también se llama **FarmealoTu** (en el plan figura como «RotaSat»). Tenemos 24 horas: del **sábado 14-nov-2026 a las 09:00** al **domingo 15-nov a las 09:00**. Hay que enviar el domingo a las **08:00**.

Necesito que me guíes y programes conmigo **paso a paso** en todo lo que me toca a mí. Trabajá por pasos cortos. Al terminar cada paso, mostrame qué hiciste, cómo probarlo y cuál es el siguiente paso.

## 1. Contexto que tenés que leer primero

- El plan completo está en la raíz del repo: **`FarmealoTu_Plan_de_Proyecto.pdf`**. Es el documento central y manda sobre este prompt si se contradicen. Leelo (si no podés leer el PDF, extraé el texto con `pdftotext` o con Python). Las secciones más importantes para mí son:
  - **0**: etiquetas de procedencia (`evidence_status`).
  - **3.2 a 3.4**: arquitectura, datos NASA y fórmulas de los indicadores.
  - **4.8**: endpoints de la API.
  - **7, ficha «Alvin»**: mi misión y mi paso a paso con horarios.
  - **8 y 9**: cronograma y traspasos.
  - **10**: riesgos.
  - **11**: pruebas A y G, que son mías.
  - **12**: checklist del envío.
- Repo: `https://github.com/matt-casanova/FarmealoTu.git`. Ramas remotas: `master`, `backend` (backend de Matías y Alvin) y `ui_kendra` (interfaz de Kendra).
- **Cambio de stack respecto del plan:** la app ya **no** es React/Vite. Es una **app Android nativa en Kotlin con Jetpack Compose** (Android Studio). El proyecto ya está creado en la raíz del repo: módulo `app/`, paquete `com.example.farmealotu`, minSdk 28 y Compose con Material 3. El backend sigue siendo **Python + FastAPI**. La app Android consume la API, así que donde el plan dice «frontend React» hay que leer «app Android».

## 2. Mi rol (según el plan)

**Líder técnico, coordinación general y datos NASA.** Mi misión es que los datos NASA lleguen limpios y en caché hasta el motor, que las piezas se integren a tiempo y que el proyecto se envíe completo y verificado.

| Persona | Qué hace | Cómo me conecto con esa persona |
|---|---|---|
| **Matías** | Motor de recomendación + API FastAPI (`app/main.py`, `schemas.py`, `/rotations/compare`, endpoints del nodo) | Le entrego indicadores, ET0 y Pef. Mis endpoints viven dentro de su FastAPI |
| **Kendra** | Interfaz (ahora en Android/Compose, rama `ui_kendra`) | Le entrego `/api/indicators`, `/api/nasa/status`, `/api/map/layers` y la capa de datos Kotlin para consumirlos |
| **Christian** | Gemelo digital y `physics.py` (ET0 FAO-56) | Le entrego el CSV horario de POWER. Él me entrega `physics.py` |
| **Mateo** | Firmware ESP32 / Wokwi | Le paso la URL pública (túnel o despliegue) de la API |

**Archivos que son míos (estructura de la sección 4.9 del plan, dentro de `backend/`):**
- `backend/pipeline/power.py` (diario + horario, validación, caché, respaldo)
- `backend/analytics/indicators.py`
- `backend/pipeline/smap.py` y `backend/pipeline/imerg.py`: **solo opcionales (P2)**, únicamente si lo básico (P0) está sólido
- Endpoints `GET /api/indicators`, `GET /api/nasa/status` y `GET /api/map/layers`
- `backend/tests/test_power.py` (prueba A) y pruebas de indicadores
- `backend/data/cache/`: JSON de POWER, **sí se versiona**
- Documentación y coordinación: `README.md`, `docs/decisiones.md`, `docs/uso_ia.md`, catálogo de fuentes y borrador del formulario en inglés

## 3. Reglas que no se negocian

1. **Nunca inventes datos NASA.** Ni valores ni URLs de teselas. Si algo no está: `null` + `evidence_status: "unavailable"` + motivo.
2. **Todo número lleva fuente, fecha, unidad y escala.** Usá `evidence_status`: `documented | derived | assumption | farmer_provided | unavailable`. Los datos de POWER se rotulan «contexto regional (~50 km)», nunca «tu campo».
3. **−999 de POWER → `null`.** Nunca lo promedies.
4. **Nada de llamadas a NASA en vivo durante la demo.** Todo se precalcula y se guarda en caché. Si NASA no responde, se usa la última descarga válida y se avisa con su fecha.
5. **La app Android nunca llama a NASA directamente.** Siempre pasa por nuestra API.
6. **Nunca digas «NASA recomienda».** RotaSat interpreta datos NASA.
7. **Git:** trabajá en **mi rama** (ver Fase 0). Commits chicos y claros. **No hagas `push`, `merge` ni PR a `master`, `backend` ni `ui_kendra` sin preguntarme primero.** Tampoco sobrescribas trabajo de otros.
8. **Estoy en Windows con PowerShell.** Dame los comandos en PowerShell (por ejemplo `.\.venv\Scripts\Activate.ps1`).
9. **Algoritmos deterministas y explicables.** Nada de ML ni de LLM para calcular o recomendar.
10. Si una tarea tarda más de 1.5 veces lo estimado, **avisame y proponé un recorte**. Prioridad absoluta: «de punta a punta antes que perfecto».

## 4. Fase 0: reconocimiento (hacelo antes de programar)

1. Ejecutá `git fetch --all` y mostrame qué hay en cada rama (`master`, `origin/backend`, `origin/ui_kendra`). Sobre todo fijate si Matías ya creó `backend/` con `app/main.py` y `schemas.py`.
2. **Preguntame**:
   - qué hora es y qué cosas ya hice (el kickoff ya pasó, así que probablemente vamos atrasados);
   - si ya se congelaron el OpenAPI v0 y el `shared/sensor_reading.schema.json`;
   - si Christian ya recibió el CSV horario.
3. Creá mi rama desde la base que corresponda (la rama `backend` si ahí está el FastAPI de Matías; si no, `master`): `git switch -c alvin/datos-nasa`.
4. Si `backend/` no existe todavía, creá la estructura de la sección 4.9 **sin pisar nada de Matías**, solo mis carpetas: `backend/pipeline/`, `backend/analytics/`, `backend/data/cache/`, `backend/tests/`. Agregá también `backend/requirements.txt` si falta.
5. Prepará el entorno: `python -m venv .venv`, activalo e instalá `fastapi "uvicorn[standard]" pydantic requests numpy pandas pytest jsonschema`.
6. Con lo que viste, **reordená el plan de abajo según la hora real** y mostrámelo antes de seguir. Si el tiempo aprieta, aplicá el «mínimo funcional» de la sección 1 del plan.

## 5. Paso a paso (en orden de prioridad)

Los horarios son los del plan. Si vamos atrasados, respetá el orden y recortá lo opcional.

### Paso 1: `fetch_power_daily(lat, lon, start, end)` (P0, plan: 09:45–11:00)
- API: NASA POWER, endpoint diario por punto (`https://power.larc.nasa.gov/api/temporal/daily/point`), `community=AG`, `format=JSON`, sin clave. **Verificá los parámetros en la documentación oficial** antes de dar algo por hecho.
- Punto piloto: **lat −17.2374, lon −62.2427** (Cuatro Cañadas, Santa Cruz, Bolivia).
- Variables: `T2M, T2M_MAX, T2M_MIN, PRECTOTCORR, RH2M, WS2M, ALLSKY_SFC_SW_DWN`. **`GWETROOT` se prueba en una llamada aparte**: si no viene, se marca como no disponible y no rompe nada.
- `requests` con timeout de 60 s y 3 reintentos con backoff. Validá el código HTTP y que exista `properties.parameter`.
- Convertí las fechas `YYYYMMDD` a ISO y alineá las variables por fecha. −999 → `null`. Conservá las unidades (`parameters` del JSON) y la **elevación** de `geometry`, que Christian necesita para la ET0.
- Caché en `backend/data/cache/` con la fecha de descarga y la URL usada. Si falla la red, se lee la caché y se avisa.
- Descargas: **2015-01-01 → 2025-12-31** (histórico) y **2026-01-01 → 2026-11-14** (reciente). Detectá la **última fecha con observación válida**, porque POWER suele tener unos días de atraso.

### Paso 2: `fetch_power_hourly` → CSV para Christian (P0, **urgente**, plan: entrega a las 11:30)
- Endpoint horario de POWER para el punto piloto, en los períodos de los escenarios de la sección 5.4:
  - **S1**: el último mes completo disponible (14 días).
  - **S2**: la sequía de dic-2025 a ene-2026 (45 días).
  - **S3**: el exceso de lluvia de feb a mar-2026 (45 días).
- Salida: `hardware/simulation/forcing/power_hourly_S1.csv`, `power_hourly_S2.csv` y `power_hourly_S3.csv`.
- Cada CSV lleva unidades en los encabezados, **zona horaria declarada** (UTC o LST, la que devuelva POWER, dicha explícitamente) y fecha de descarga. Sin −999.
- Si falta alguna variable horaria, avisame. Christian la desagrega de la serie diaria y la rotula «desagregado».
- Al terminar, dame un mensaje corto para mandarle a Christian.

### Paso 3: prueba A (`backend/tests/test_power.py`) (plan: 11:30–12:00)
La prueba usa una llamada real o la caché y comprueba tres cosas:
- que el JSON se puede inspeccionar;
- que las unidades y la fuente están presentes;
- que no queda ningún −999.

Corre con `pytest -q`.

### Paso 4: `analytics/indicators.py` (plan: 13:15–14:30)
Fórmulas exactas en la sección 3.4. Cada valor va con `unit`, `source`, `period`, `scale` y `evidence_status: "derived"`.
- **Racha seca máxima (CDD):** días consecutivos con P < 1 mm/día (umbral configurable). Se calcula por campaña y por **año climático**, porque el bootstrap de Matías lo necesita.
- **Anomalía de lluvia %:** `100·(P_actual − P̄_hist)/P̄_hist`, con P̄ = media 2015–2025 del mismo período. **Si P̄ = 0, el resultado es «no definido»**: nunca dividas por cero.
- **Días de calor:** Tmáx > umbral del cultivo, si tiene fuente. Si no, Tmáx > P90 histórico del mes, rotulado como indicador meteorológico general.
- **Lluvia intensa:** días con P > P95 de los días húmedos (≥ 1 mm) del mismo mes. Es exposición, no predicción de inundación.
- **Lluvia efectiva mensual (Pef):** `0.8P − 25` si P > 75 mm; `0.6P − 10` si P ≤ 75 mm; mínimo 0. Se rotula `assumption` (método FAO empírico).
- Agregación mensual y por campaña (verano / invierno).
- **Grilla meses × años (2015–2026)** para el mapa de calor del módulo «Riesgos».
- Calidad de los datos: n.º de días válidos y % faltante.
- **Pruebas unitarias con series sintéticas de resultado conocido**, incluido el caso P̄ = 0.

### Paso 5: integrar `physics.py` de Christian (plan: 14:30–15:30)
- Esperamos de Christian la ET0 FAO-56 Penman-Monteith diaria (`backend/analytics/physics.py`). Ayudame a definir y acordar con él la firma de la función. Por ejemplo: entra un DataFrame diario con las variables de POWER más la elevación y sale ET0 en mm/día.
- Si llega tarde, **no la reimplementes por tu cuenta sin avisarme**: dejá ET0 = `unavailable` con su motivo, para que todo siga funcionando.
- Con la ET0, armá los insumos del balance hídrico (Pef y ET0 por mes). **Matías aplica el Kc** de cada cultivo (ETc = Kc·ET0).
- Entregable para Matías a las 15:30: funciones importables **más** el endpoint del Paso 6.

### Paso 6: endpoints `GET /api/indicators` y `GET /api/nasa/status` (plan: 15:30–17:00)
- Van **dentro del FastAPI de Matías**, por ejemplo con un `APIRouter` en `backend/app/routers/nasa.py` que él incluye en `main.py`. Coordiná conmigo antes de tocar `main.py` o `schemas.py`.
- `GET /api/indicators?lat&lon&period` devuelve los indicadores con unidad, procedencia y calidad, más la grilla mensual para el mapa de calor.
- `GET /api/nasa/status` devuelve, por fuente: disponibilidad, última descarga exitosa, última fecha con observaciones, versión del producto y si se usó caché.
- Las respuestas se leen **desde la caché**: nunca llaman a NASA en la petición.
- Antes de programar, proponeme el JSON de respuesta y escribilo en `docs/contratos.md`, para que Kendra pueda crear sus data classes de Kotlin sin dudas.

### Paso 7: túnel o despliegue y conexión con el teléfono (plan: entrega a las 16:00)
- Local: `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`. La API queda documentada en `/docs`.
- Túnel público: `cloudflared tunnel --url http://localhost:8000`, o ngrok. Esa URL HTTPS es para Mateo (Wokwi) y Christian, y también para el teléfono Android.
- **Android:** un teléfono físico no ve `localhost`. Desde el **emulador** la PC es `http://10.0.2.2:8000`. Con HTTP plano hay que habilitar cleartext con un `network_security_config` **solo en debug**. Lo ideal es usar la URL HTTPS del túnel. La `BASE_URL` va en `local.properties` → `BuildConfig`, no hardcodeada.
- Dame un mensaje corto con la URL para el canal del equipo.
- **Checkpoint 1 (17:00, lo dirijo yo):** tiene que funcionar el recorrido NASA → indicadores → motor → pantalla Android, con ≥ 3 estrategias reales. Preparame un checklist de 5 puntos para la demo interna y para decidir el go/no-go de los extras.

### Paso 8: `GET /api/map/layers` con GIBS (P1, plan: 17:30–19:00)
- Consultá el **GetCapabilities WMTS de NASA GIBS** y publicá **solo identificadores verificados** de vegetación (NDVI MODIS/VIIRS), precipitación y humedad.
- Para las 5 capas del visor (lluvia, humedad del suelo, calor, vegetación y agua) devolvé el estado (`available | regional | unavailable`), la fuente, la fecha, la escala y la plantilla de URL **solo si está verificada**.
- Plan B: todas las capas en «Información regional».

### Paso 9: capa de datos Android para mis endpoints (coordinado con Kendra)
- En el módulo `app/`, paquete `com.example.farmealotu.data`: cliente HTTP (Retrofit + kotlinx.serialization o Ktor, el que Kendra ya use; **revisá `ui_kendra` primero para no duplicar**), DTOs de `/api/indicators`, `/api/nasa/status` y `/api/map/layers`, y un `NasaRepository` con manejo de errores, carga y «sin conexión».
- Agregá el permiso `INTERNET` al `AndroidManifest.xml` si falta. Las dependencias van en `gradle/libs.versions.toml`.
- **Yo no hago las pantallas**: eso es de Kendra. Solo le dejo la capa de datos lista y un ejemplo mínimo de uso.
- Verificá que compile: `.\gradlew.bat assembleDebug`.

### Paso 10: despliegue estable y decisión sobre P2 (plan: 19:00–21:00)
- Backend estable (Render o Railway, **con la caché incluida**) o, como plan B, local + túnel + video de respaldo. Revisá CORS y las variables de entorno (`.env.example`, sin secretos en el repo).
- **A las 19:00 decidimos:** si lo básico está sólido, intentamos el adaptador SMAP L4 (necesita Earthdata Login y archivos ya descargados) o IMERG. Si no, se declara como limitación. Ayudame a decidir con criterios concretos.
- **Checkpoint 2 (21:00, lo dirijo yo):** los 4 módulos funcionando, con incertidumbre. Ahí se congela el alcance.

### Paso 11: coordinación y documentación (plan: 21:30–03:30)
- **README.md:** qué es, instalación (PowerShell, backend y Android), cómo reproducir, datos NASA usados, otras fuentes, limitaciones (copiá las de la sección 11.3 del plan) y uso de IA.
- **`docs/decisiones.md`:** registro de decisiones. Empezalo con el cambio de stack de React a Android/Kotlin.
- **`docs/uso_ia.md`:** qué herramienta de IA se usó y para qué. Anotá este uso de Claude Code.
- **Catálogo de fuentes** (`GET /api/sources` es de Matías; yo le paso la lista).
- **Borrador del formulario en inglés:** resumen, descripción, datos NASA usados y cómo se usó cada uno, otros recursos y declaración de IA. Seguí el checklist de la sección 12.1.
- Guion del pitch y contenido de las 7 diapositivas (sección 12.3), con los insumos de Vanesa, Christian y Kendra.
- **Desde la 01:00 no se programan funciones nuevas.** Solo se corrigen errores y se documenta.

### Paso 12: QA y envío (domingo 06:00–08:30)
- QA desde un teléfono real: la prueba F (llegar a la recomendación en menos de 3 min) y la prueba G (trazabilidad: todo resultado tiene fuente, fecha, unidad y explicación).
- Revisá los enlaces en una ventana privada.
- Envío a las **08:00**, no a las 08:59. Otra persona verifica el estado «enviado».

## 6. Cómo quiero que trabajes conmigo

- Al empezar cada paso: explicame en 2 o 3 líneas qué vamos a hacer y por qué, y cuánto tiempo estimás.
- Programá, ejecutá y **probá** (pytest, `curl` o `Invoke-RestMethod`, `gradlew`). No me des código sin haberlo probado.
- Al terminar cada paso: mostrame cómo verificarlo yo mismo, proponé el mensaje de commit y **preguntame antes de hacer push**.
- Cuando tenga que pasarle algo a alguien del equipo, redactame el mensaje corto listo para pegar en el chat del equipo.
- Si algo del plan choca con el cambio a Android/Kotlin, o si ves un riesgo (sección 10 del plan), avisame y proponé la solución más simple.
- Hablame en español.

**Empezá por la Fase 0.**
