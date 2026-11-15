# SOWLT — Prompt para Claude Code: integración NASA ↔ Android (sin modificar UI)

> **Rol:** Senior Android Engineer / especialista en integración FastAPI, Kotlin, Jetpack Compose, Retrofit, NASA Earth Observation Data.
>
> **Objetivo exclusivo:** hacer que la aplicación Android terminada muestre **datos reales de NASA obtenidos a través del backend de Alvin**, respetando la UI actual sin alteraciones visuales ni cambios ajenos a la integración.

## 0. Repositorio, ramas y alcance

Repositorio: `https://github.com/matt-casanova/FarmealoTu.git`

- `ui_kendra_pos`: **UI Android terminada y aprobada**. Es la referencia visual inmutable.
- `alvin/datos-nasa`: **pipeline, API temporal y endpoints NASA** de Alvin. Es **solo lectura** para esta tarea.
- `integracion_nasa`: **rama de trabajo** creada desde `ui_kendra_pos`. Solo aquí debes realizar modificaciones.
- `master`: no tocar.
- `alvin/main-esqueleto`: rama de referencia opcional con más endpoints provisionales; **no asumir que constituye el motor final**.

La copia de lectura del backend debería estar en `../FarmealoTu-NASA` mediante `git worktree`. Si no existe, verifica primero las ramas e indica cómo crearla sin cambiar de rama dentro del proyecto Android. **No hagas merge de ramas enteras.**

Antes de modificar nada:

1. Ejecuta `git branch --show-current` y comprueba que es `integracion_nasa`. Si no lo es, detén la edición y explica el problema.
2. Ejecuta `git status --short` y comprueba si hay cambios preexistentes; **no los sobrescribas**.
3. Inspecciona el código de Android y la copia del backend. No improvises contratos ni dupliques clases que ya existen.

## 1. Regla crítica: la UI es intocable

**PROHIBIDO cambiar el aspecto, la navegación o la experiencia visual de la aplicación.** La UI de `ui_kendra_pos` es definitiva.

No cambiar ni crear elementos visuales, incluyendo:

- `app/src/main/java/com/example/farmealotu/ui/screens/`
- `app/src/main/java/com/example/farmealotu/ui/components/`
- `app/src/main/java/com/example/farmealotu/ui/navigation/`
- `app/src/main/java/com/example/farmealotu/ui/theme/`
- Íconos, drawables, colores, fuentes, tipografía, tamaños, layout, animaciones, rutas de navegación o archivos de diseño.

**Modificaciones permitidas solamente si son imprescindibles para conectar datos:**

- `app/src/main/java/com/example/farmealotu/data/remote/`
- `app/src/main/java/com/example/farmealotu/data/model/`
- `app/src/main/java/com/example/farmealotu/data/repository/`
- `app/src/main/java/com/example/farmealotu/viewmodel/`
- `AppContainer.kt` si es necesaria una corrección de configuración.
- Configuración de red/debug en Manifest, Gradle o `local.properties`, solo cuando sea estrictamente necesario.
- Pruebas que verifiquen compatibilidad de los contratos y estados existentes.

Si una incompatibilidad requiere modificar una pantalla, **NO la modifiques**. Primero busca resolverla mediante los DTOs, mappers, repositorios o ViewModels; documenta cualquier bloqueo residual.

**No cambies el nombre de paquetes ni refactorices código no relacionado. No hagas commits o push automáticamente.**

## 2. Arquitectura actual: reutilizarla

La UI ya implementa:

- `data/remote/FarmealoTuApi.kt` — endpoints Retrofit.
- `data/remote/ApiClient.kt` — OkHttp/Retrofit y serialización Kotlinx.
- `data/model/ApiDtos.kt`, `DtoMappers.kt`, `JsonExt.kt` — contratos y transformación JSON.
- `data/repository/RemoteFarmealoTuRepository.kt` — fuente remota.
- `AppContainer.kt` — selector de API real vs. datos demo, URL persistida.
- `viewmodel/MainViewModel.kt` y `SecondaryViewModels.kt` — estado observable.
- `ui/screens/settings/ConnectionScreen.kt` — pantalla existente para comprobar conexión y cambiar URL.

**No recrees** ninguno de estos componentes. Analiza qué está hecho y corrige solo discrepancias comprobadas.

## 3. Inspección y prueba del backend NASA real

Revisa:

- `../FarmealoTu-NASA/backend/app/routers/nasa.py`
- `../FarmealoTu-NASA/backend/app/dev_main.py`
- `../FarmealoTu-NASA/backend/analytics/indicators.py`
- `../FarmealoTu-NASA/backend/pipeline/`
- `../FarmealoTu-NASA/docs/contratos.md`
- `../FarmealoTu-NASA/docs/ejecucion_backend.md`
- `docs/UI_BACKEND_CONTRACT.md` en la rama Android.

**Endpoints conocidos en `alvin/datos-nasa` que debes verificar por código y OpenAPI efectivo:**

- `GET /api/health`
- `GET /api/nasa/status`
- `GET /api/indicators?lat=-17.2374&lon=-62.2427&period=recent`
- `GET /api/map/layers`

Para arrancar el servidor temporal de esa rama, desde `../FarmealoTu-NASA/backend`:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app.dev_main:app --host 0.0.0.0 --port 8000
```

Inspecciona `http://localhost:8000/openapi.json` y `http://localhost:8000/docs`. Comprueba respuestas reales, campos nulos, fechas, unidades, procedencia y errores. **Si el servidor no se puede ejecutar, informa la causa exacta. No afirmes que las pruebas pasaron.**

**Importante:** en `alvin/datos-nasa`, `dev_main.py` solo incluye endpoints NASA; no asumas que contiene `POST /api/rotations/compare`, datos de nodos ni el motor definitivo E0–E3. No fabriques recomendaciones, scores ni rankings. Si hay una rama más nueva con el motor real, puedes inspeccionarla para informar compatibilidad, pero **no hacer merge ni modificar backend sin autorización**.

## 4. Integrar datos NASA con el estado de Android

### A. Estado de NASA (`GET /api/nasa/status`)

Conecta la respuesta al flujo **existente** de `RemoteFarmealoTuRepository` → mapper → `MainViewModel` → pantallas ya implementadas. Verifica que se propaguen correctamente:

- Fecha de **última observación válida**, distinta de la fecha de descarga.
- Fecha de descarga exitosa y origen de caché.
- Disponibilidad y estado de las fuentes.
- Procedencia, limitaciones y escala espacial.

### B. Indicadores (`GET /api/indicators`)

Punto piloto inicial: `lat=-17.2374`, `lon=-62.2427`; parámetro inicial `period=recent` (o el valor por defecto confirmado).

Verifica los contratos reales y su mapeo a la pantalla de riesgos sin cambiar la pantalla:

- Lluvia acumulada y anomalía.
- Racha seca máxima.
- Días de calor y de lluvia intensa.
- Lluvia efectiva cuando existe.
- `monthly_grid` con series y celdas año × mes, `null`, calidad y unidades.
- Metadatos de evidencia (documentado, derivado, supuesto o no disponible).

No calcules indicadores científicos nuevos dentro de Android. **No sustituyas `null` por `0`.**

### C. Capas satelitales (`GET /api/map/layers`)

Conecta las cuatro capas NASA GIBS cuando el backend marque que están disponibles:

- Lluvia IMERG.
- Humedad del suelo SMAP L4.
- Temperatura de superficie VIIRS.
- Vegetación NDVI VIIRS.

Reutiliza el visor osmdroid existente sin tocar su apariencia. Valida cuidadosamente:

- `tile_url` y, si corresponde, `tile_url_zxy`.
- Orden `z/y/x` de GIBS (`TileMatrix/TileRow/TileCol`).
- `max_zoom`, fechas reales, estado y resolución de cada capa.
- Ausencia de teselas, `status=unavailable`, leyendas y atribución.

Si el visor actual no consume cierta metadata y su corrección exigiría modificar `ui/screens/` o `ui/components/`, documenta el problema en el informe final en lugar de violar la protección visual.

### D. Límite geográfico

El backend NASA de Alvin valida un alcance de aproximadamente **±0.25°** del punto piloto; la UI original puede permitir **±0.5°**. **No modifiques la UI para cambiar el selector.** Maneja el error de alcance desde la capa de datos/estado con mensajes ya soportados y reporta explícitamente la discrepancia. No sustituyas silenciosamente coordenadas del agricultor por el punto piloto ni etiquetes datos regionales como mediciones de la finca.

## 5. Datos más recientes disponibles, no falsamente «en vivo»

El backend lee una **caché NASA POWER** y las capas GIBS están fechadas. NASA no entrega necesariamente mediciones del minuto actual.

- Actualiza el estado del backend al entrar a las pantallas y permite reintento utilizando los mecanismos existentes.
- Si hace falta refresco periódico, impleméntalo **solo mientras la pantalla pertinente está visible**, mediante coroutines y lifecycle, sin bloquear el hilo principal. Evita tráfico excesivo y bucles.
- **No hagas consultas directas a NASA desde Android.**
- No redescargues NASA en cada petición del usuario. El proceso de refresco de caché pertenece al backend.
- Mostrar siempre la **fecha real de observación**, fuente y cobertura: datos regionales, no medición a nivel de campo.

## 6. URL y modo demo

El proyecto Android ya lee `local.properties`:

```properties
farmealotu.apiBaseUrl=http://10.0.2.2:8000/
farmealotu.demoMode=false
```

- `10.0.2.2` solo funciona para conectar el **emulador** con FastAPI en la **misma PC**.
- En un teléfono físico utilizar IP de la PC en la misma red o URL HTTPS del túnel/servidor.
- `AppSettings` guarda URL y modo demo en preferencias: verifica que no estén sobrescribiendo `local.properties`.
- Modo demo debe quedar **desactivado en pruebas con datos reales**.
- Nunca mezcles ni presentes fixtures como observaciones NASA.
- No expongas secretos ni URLs temporales en archivos versionados.

## 7. Pruebas de aceptación

Prueba **en este orden**:

1. `GET /api/health` desde PowerShell/navegador.
2. `GET /api/nasa/status` y validación de fechas.
3. `GET /api/indicators` con el punto piloto y `period=recent`.
4. `GET /api/map/layers` y comprobación de metadata de capas disponibles.
5. La aplicación Android en modo real muestra estado NASA sin fixtures.
6. Riesgos utiliza indicadores devueltos por FastAPI (incluida la grilla mensual).
7. El visor utiliza capas que el backend declara verificadas y disponibles.
8. Errores controlados para 404/422/5xx/timeout y campos `null`.
9. Compilación Android en Windows: `./gradlew.bat assembleDebug` (o `.\gradlew.bat assembleDebug` en PowerShell).
10. Ejecutar pruebas existentes y nuevas pruebas mínimas de contrato/mappers.

Antes de finalizar ejecuta:

```powershell
git diff --name-status ui_kendra_pos
git diff --check
```

**Condición obligatoria:** el diff no debe incluir `ui/screens/`, `ui/components/`, `ui/navigation/`, `ui/theme/` ni recursos gráficos. Si se modificó alguno, restaura solo **tus propios cambios** en esos archivos; conserva cualquier cambio previo del usuario.

## 8. Entrega final exigida

Resume con evidencia:

1. Endpoints que funcionan contra FastAPI real y resultados observados.
2. Qué información NASA aparece en qué pantallas **existentes**.
3. Archivos realmente modificados (rutas completas).
4. Comandos exactos de ejecución de backend y Android.
5. Pruebas ejecutadas, aprobadas, fallidas o no ejecutadas; sin inventar resultados.
6. Pendientes o bloqueos: especialmente motor de recomendaciones E0–E3, sensores y contratos no disponibles en esta rama.
7. Confirmación mediante `git diff` de que **la UI visual no cambió**.

**No ejecutes `git merge`, `git push` ni cambios sobre `ui_kendra_pos`, `alvin/datos-nasa` o `master`.**

---

### Criterio de éxito

**Sowlt conserva idéntica su UI terminada, pero consume en modo real los indicadores, fechas y capas NASA que FastAPI ya ofrece.** Toda parte que todavía dependa del motor de rotación o de servicios no implementados se declara como pendiente: nunca se simula silenciosamente.
