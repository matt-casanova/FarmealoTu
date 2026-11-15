# PROMPT — Claude Code · Pulido UI/UX de FarmealoTu Android con el skill Impeccable

> **Uso (Kendra):** abre una terminal en la raíz del repo, en la rama `ui_kendra_pos`, ejecuta `claude` y escribe:
> **«Lee `FarmealoTu_Impeccable_UI_prompt.md` y sigue sus instrucciones. Trabaja solo en `ui_kendra_pos`. Empieza por la Fase 0.»**

---

## 0. Misión

Eres un **diseñador de producto senior + ingeniero Android (Jetpack Compose, Material 3)**. Tu trabajo es **mejorar la calidad UI/UX** de la app FarmealoTu que ya existe en este repo, usando el skill **Impeccable** instalado en `.claude/skills/impeccable/` (versión 4.9.x).

- Es un trabajo de **refinamiento, no de rediseño**: conserva la identidad visual (verdes de cultivo, tonos tierra, fondo papel, legibilidad al sol), la navegación, las funciones y el texto factual. Impeccable lo llama *"Refinement preserves"*.
- El modo de superficie es **Operate**: el agricultor completa una tarea (responder 5 preguntas → comparar rotaciones → entender riesgos e incertidumbre). Prima la claridad, la consistencia y lo nativo de Android sobre la expresión.
- Usuario objetivo: agricultor de **Cuatro Cañadas, Santa Cruz, Bolivia**. Español sencillo, celular de gama media, uso al aire libre con sol fuerte, a veces con datos móviles lentos.
- **Edita los archivos, compila y verifica con capturas reales.** No entregues solo un informe.

---

## 1. Reglas obligatorias (no negociables)

### Git
1. Antes de tocar nada ejecuta:
   ```powershell
   git rev-parse --show-toplevel; git branch --show-current; git status --short
   ```
   La rama debe ser exactamente **`ui_kendra_pos`**. Si no lo es, **detente** y dime cómo cambiarla.
2. Trabaja solo en `ui_kendra_pos`. **No** hagas `switch`, `merge` ni `push` a `master`, `backend`, `ui_kendra` ni a las ramas `alvin/*`.
3. Commits pequeños y con mensaje claro (`style(ui): …`, `fix(a11y): …`). **No hagas push sin preguntarme.**
4. No borres trabajo existente ni archivos de otros integrantes.

### Producto y datos (vienen del plan del proyecto y de `FarmealoTu_Claude_Code_UI_Kotlin.md`)
5. **No inventes datos NASA, cifras, fuentes ni URLs de teselas.** El diseño no puede rellenar huecos con números "de ejemplo" que parezcan reales. Si un dato falta, se muestra como faltante.
6. **La app Android nunca llama a NASA directamente**; todo pasa por la API del backend. **No dupliques el motor científico** en Kotlin (agua/suelo/riesgo, MCDA, bootstrap los calcula el backend).
7. **Etiquetas de procedencia visibles y nunca solo con color**: `documented / derived / assumption / farmer_provided / unavailable`, la escala "contexto regional (~50 km)" y las insignias **SIMULADO / WOKWI / FÍSICO** del nodo de campo. Puedes mejorar su diseño, no quitarlas ni esconderlas.
8. La **franja "MODO DEMO"** (`DemoStripe`) debe seguir siendo inconfundible.
9. `ChartPalette` en `ui/theme/Color.kt` es una paleta **validada para daltonismo** (orden fijo de series, secuencial de un tono, divergente con gris al medio, estados reservados). No cambies sus valores sin volver a validarla; si la cambias, explica por qué y con qué comprobación.
10. Marca **FarmealoTu**. No cambies identificadores de contrato (`node_id`, campos JSON, rutas de la API, `docs/UI_BACKEND_CONTRACT.md`).
11. **Pregunta antes** de: reemplazar texto factual o agregar afirmaciones, cambiar la estructura de navegación, agregar dependencias nuevas a Gradle, o agregar fuentes tipográficas.

---

## 2. Cómo usar Impeccable en este proyecto (Windows + Android)

- El skill está en `.claude/skills/impeccable/`. Lee primero su `SKILL.md`.
- En Windows sin `sh`, el lanzador es:
  ```powershell
  .\.claude\skills\impeccable\scripts\impeccable.cmd context
  ```
  Ejecútalo **una vez por sesión**, desde la raíz del repo. Si falla, avísame con la frase que indica el skill ("Context loading did not run…") y sigue leyendo `PRODUCT.md` / `DESIGN.md` directamente.
- Plataforma nativa **Android**: aplica `reference/android.md` y las variantes `*.native.md` (`audit.native.md`, `adapt.native.md`). Material Design 3 manda en estructura, navegación e interacción; la marca se expresa por el tema (roles de color, escala tipográfica, formas, movimiento).
- Antes de **cualquier** edición de UI lee `reference/craft-floor.md`.
- Los detectores automáticos de Impeccable están pensados para web: en archivos `.kt` la evidencia principal son **las capturas del emulador**, no el detector.

### Capturas (evidencia obligatoria)
- Compila e instala: `.\gradlew.bat installDebug` (emulador abierto en Android Studio).
- Captura con adb (no desde un navegador):
  ```powershell
  adb exec-out screencap -p > docs\ui_review\antes\home_claro.png
  ```
  Guarda **antes** en `docs/ui_review/antes/` y **después** en `docs/ui_review/despues/`.
- Pasa siempre por: **tema claro**, **tema oscuro** (`adb shell cmd uimode night yes`) y **letra grande** (`adb shell settings put system font_scale 1.3`). Lee los valores actuales antes y **restáuralos al terminar** (`night no`, `font_scale 1.0`).
- Usa la app en **modo demo** (`farmealotu.demoMode` en `local.properties`) si el backend no está disponible, para que todas las pantallas tengan contenido.

---

## 3. Fases de trabajo

### Fase 0 — Inspección y contexto (≈15 min)
1. Verifica la rama (regla 1). Lee `FarmealoTu_Claude_Code_UI_Kotlin.md` (secciones de UX, pantallas y accesibilidad) y `docs/UI_BACKEND_CONTRACT.md`.
2. Recorre `app/src/main/java/com/example/farmealotu/ui/`: `theme/` (Color, Theme, Type), `components/` (Common, Charts, HeatScale, Glossary, Format) y las pantallas `home, input, decision, compare, tradeoffs, risks, uncertainty, geo, field, settings`.
3. Corre `impeccable.cmd context`.
4. Ejecuta **`/impeccable init`** para crear `PRODUCT.md` (plataforma `android`, modo Operate, usuarios y contexto de arriba). Muéstrame el borrador antes de seguir.
5. Ejecuta **`/impeccable document`** para generar `DESIGN.md` **a partir del código existente** (tokens de `Color.kt`, `Type.kt`, `Theme.kt`, componentes de `Common.kt`). Debe describir lo que hay, no un sistema nuevo.

### Fase 1 — Diagnóstico (≈30 min, sin editar UI)
1. Compila, instala y saca las capturas **antes** de cada pantalla (claro, oscuro, letra 1.3).
2. Ejecuta **`/impeccable critique`** sobre el recorrido principal: Home → Entrada (5 preguntas) → Decisión → Comparar → Detalle de estrategia → Ventajas/desventajas → Riesgos → Incertidumbre. Después Geo, Datos de campo, Fuentes y Conexión.
3. Ejecuta **`/impeccable audit`** en su variante nativa: contraste (sol directo → apunta a AA o mejor), objetivos táctiles **≥ 48 dp**, `sp` para texto, `contentDescription` en iconos y gráficos, orden de TalkBack, insets edge-to-edge (barras del sistema y teclado), Back del sistema / gesto predictivo, tema oscuro real.
4. Entrégame una **lista priorizada** (P0 bloquea la demo / P1 importante / P2 opcional) con pantalla, archivo, problema, evidencia (captura) y arreglo propuesto. **Espera mi OK** antes de la Fase 2.

### Fase 2 — Arreglos (por prioridad, commits pequeños)
Aplica en este orden, solo lo aprobado:
1. **Tema** (`ui/theme`): que todo use **roles de color de Material 3** (`primary`, `surface`, `surfaceVariant`, `outline`, `error`, contenedores…) en vez de hex sueltos en pantallas; un **esquema oscuro** diseñado de verdad; escala tipográfica M3 completa en `Type.kt` y pantallas que usen `MaterialTheme.typography` (nada de tamaños a mano).
2. **`/impeccable layout`**: espaciado y jerarquía consistentes (una escala de espaciado, p. ej. 4/8/12/16/24 dp), una acción principal clara por pantalla.
3. **`/impeccable clarify`**: textos en español sencillo para agricultores, etiquetas y mensajes de error que digan qué pasó y qué hacer. Sin cambiar cifras ni afirmaciones (regla 11).
4. **`/impeccable harden`**: estados de **carga, error, vacío, sin conexión y modo demo** en cada pantalla que consume la API; textos largos y números grandes que no rompan el diseño.
5. **`/impeccable adapt`** (nativo): teléfono pequeño (≈360 dp) y, si da el tiempo, tablet/horizontal.
6. **`/impeccable polish`** final sobre el recorrido principal.
- Gráficos (`Charts.kt`, `HeatScale.kt`): mejora legibilidad (ejes y rejilla discretos, leyendas, etiquetas directas selectivas, alternativa en texto/tabla para TalkBack) **sin cambiar los datos que muestran** y respetando la regla 9.
- **No** uses `/impeccable bolder`, `overdrive`, `live` ni `generate` (son para web o para rediseños) salvo que yo lo pida.

### Fase 3 — Verificación
1. `.\gradlew.bat assembleDebug` sin errores y `.\gradlew.bat testDebugUnitTest` en verde (si hay tests que fallaban antes, dímelo; no los borres).
2. Capturas **después** de las mismas pantallas y modos, y compáralas con las de **antes**.
3. Restaura `uimode` y `font_scale` del emulador.
4. Di qué evidencia salió del emulador y qué no se pudo comprobar (rendimiento, gestos reales y brillo al sol necesitan un teléfono físico).

---

## 4. Tiempo (hackathon de 24 h)

- **Checkpoint 2 — 21:00:** los 4 módulos (Comparar, Ventajas y desventajas, Riesgos, ¿Qué tan segura?) deben verse completos. Prioriza los P0 de esas pantallas.
- **Congelación de funciones — 01:00:** después de esa hora, solo arreglos visuales y de accesibilidad, nada de pantallas nuevas.
- Si algo va a tomar más de 30 min, avísame y propón una versión más chica.

---

## 5. Entregable final

Al terminar, dame:
1. `PRODUCT.md` y `DESIGN.md` en la raíz.
2. `docs/ui_review/` con capturas antes/después y un `docs/ui_review/README.md` corto: qué se cambió, por qué, en qué archivo, y qué quedó pendiente.
3. Lista de commits hechos en `ui_kendra_pos` y el estado de `git status`.
4. Lo que **no** cambiaste por respetar las reglas (procedencia, paleta de gráficos, contrato con el backend), para que el equipo lo sepa.
