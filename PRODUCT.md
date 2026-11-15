# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Un solo usuario guía las decisiones de diseño: el **agricultor de Cuatro Cañadas (Santa Cruz, Bolivia)** que está
por decidir su próxima rotación de cultivos (soya, maíz, sorgo, girasol, trigo o barbecho).

- Usa un **celular Android de gama media**, al aire libre, **con sol fuerte**, a menudo **con una mano**.
- A veces tiene **datos móviles lentos** o se queda sin conexión.
- **Lee despacio o con dificultad** (baja alfabetización confirmada como necesidad).
- Puede tener **vista cansada** y subir el tamaño de letra del sistema.

Su trabajo en la app: responder 5 preguntas sobre su campo, ver qué opción de siembra conviene revisar, comparar
las opciones, entender sus riesgos y saber qué tan segura es la comparación, para conversarlo con su técnico.
El técnico o agrónomo aparece en la app como persona de referencia («Revisá con tu técnico»), no como usuario.

## Product Purpose

FarmealoTu acompaña al agricultor desde **cinco preguntas sencillas** hasta una **comparación de estrategias de
rotación (E0 Actual, E1 Cuidar suelo, E2 Cuidar agua, E3 Equilibrada)** calculada por el backend, con su evidencia,
ventajas y desventajas, riesgos por mes y año, e incertidumbre (P10–P90).

Éxito:
- El recorrido completo se entiende en **menos de 3 minutos**; la tarjeta de resultado, en **menos de 30 segundos**.
- El agricultor sale sabiendo **qué revisar y con quién**, no con una cifra que parezca una promesa.
- Un puntaje se lee como **relativo entre opciones**, nunca como probabilidad de éxito, ganancia o rendimiento.

## Positioning

**NASA aporta datos, FarmealoTu los interpreta** para una zona concreta (Cuatro Cañadas), y cada número dice de
dónde viene: documentado, calculado por FarmealoTu, supuesto, dato del agricultor o no disponible, con su escala
(«contexto regional ~50 km»). Los datos satelitales y los del nodo de campo de 3 zonas (ESP32) se muestran
separados y con su origen (SIMULADO / WOKWI / FÍSICO). La app no es un producto oficial de NASA ni afirma que NASA
recomiende rotaciones.

## Operating Context

- **Recorrido:** Inicio → 5 preguntas → Resultado → 01 Comparar formas de sembrar / 02 Ventajas y desventajas /
  03 Conocer los riesgos / 04 ¿Qué tan segura es esta comparación? Accesos secundarios: Explorar mi zona, Datos de
  terreno, Fuentes y límites, Conexión. Desde cualquier pantalla se vuelve al inicio y se puede «Editar mis datos».
- **Arquitectura:** la app Android solo habla con el backend FastAPI de FarmealoTu (y con el servidor de teselas del
  mapa base). El backend calcula agua/suelo/riesgo, MCDA, ranking y bootstrap; Android muestra, reenvía entradas y
  prioridades.
- **Modo demo:** sin backend, `FakeFarmealoTuRepository` entrega datos de ejemplo con la franja persistente
  «MODO DEMO · DATOS DE EJEMPLO». Se activa en «Conexión» o con `farmealotu.demoMode=true` en `local.properties`.
- **Contexto de evaluación:** NASA Space Apps Challenge 2026 «Field Shift: Adapting Farms with NASA Data»,
  hackathon de 24 h. La demo ante el jurado y el uso real en el campo pesan **por igual** al priorizar arreglos;
  las decisiones de diseño siguen al agricultor.

## Capabilities and Constraints

- **Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel + StateFlow, Retrofit/OkHttp,
  osmdroid para el mapa. `minSdk 28`. Agregar dependencias de Gradle o fuentes tipográficas requiere aprobación.
- **Datos honestos:** `null` nunca es 0 ni verde; lo que falta se muestra como faltante («sin dato», «no
  disponible», «dato insuficiente»). No se inventan cifras, fuentes, fechas ni URLs de teselas.
- **Sin motor en Android:** no se recalculan puntajes, ranking ni P10–P90.
- **Procedencia siempre visible y nunca solo con color:** `documented / derived / assumption / farmer_provided /
  unavailable`, la escala regional y las insignias del nodo SIMULADO / WOKWI / FÍSICO.
- **Confianza:** con datos regionales y parámetros genéricos nunca se rotula «alta»; `high` se muestra como
  «Media (limitada: datos regionales)».
- **Sin recomendación forzada:** si `recommended_strategy_id` no es válido se muestra «Información insuficiente para
  ordenar opciones» con sus razones.
- **Contrato fijo:** no se cambian `node_id`, campos JSON, rutas de la API ni `docs/UI_BACKEND_CONTRACT.md`.
- **Fuera de alcance:** login, pagos, chatbots, menús ajenos al problema, pronósticos de rendimiento.
- **Abierto:** la forma final de varias respuestas del backend está pendiente de confirmar (ver
  `docs/UI_BACKEND_CONTRACT.md` §9); la app acepta varias formas mientras tanto.

## Brand Commitments

- **Nombre:** FarmealoTu. Nunca «RotaSat» en pantalla (puede quedar en identificadores históricos de contrato).
- **Voz:** español sencillo con **voseo** («¿Qué sembrás?», «¿Tenés riego?», «Revisá con tu técnico»).
- **Aviso visible:** «Esta herramienta apoya tu decisión; no reemplaza tu experiencia ni a tu técnico.»
- **Identidad visual existente, a conservar** (trabajo de refinamiento): verdes de cultivo, tonos tierra, fondo
  papel, legibilidad al sol. El detalle vive en DESIGN.md.

## Evidence on Hand

- `FarmealoTu_Plan_de_Proyecto.pdf` (plan del proyecto, 44 páginas): fuente del contrato, UX y criterios.
- `FarmealoTu_Claude_Code_UI_Kotlin.md`: especificación funcional de cada pantalla.
- `docs/UI_BACKEND_CONTRACT.md`: contrato UI ↔ backend y lista de conciliación.
- **Ausencias que no se deben rellenar:** al 14-nov-2026 no había backend accesible; nada se probó contra la API
  real. Los fixtures demo no son evidencia NASA y sus capturas no se presentan como tal. El demo no trae teselas,
  NDVI ni SMAP.
- **Contexto histórico, no actual:** cifras municipales de 2023; la inundación de marzo 2025 y la sequía/lluvias
  de la campaña 2025–2026 no son alertas en tiempo real.

## Product Principles

1. **Qué hacer antes que datos.** Cada pantalla abre con la conclusión o la acción; el detalle técnico va después
   y plegado.
2. **Lo que falta se ve.** Un dato ausente, parcial o simulado se dice con palabras, nunca se disimula.
3. **Decide el agricultor con su técnico.** La app ordena y explica; no promete ni ordena.
4. **Legible al sol, con una mano, para quien lee despacio.** Frases cortas, una idea por bloque, controles grandes.
5. **Mostrar, no calcular.** La ciencia vive en el backend; la app es fiel a lo que recibe.

## Accessibility & Inclusion

- Texto principal **≥ 16 sp**, siempre en `sp`; la app debe seguir funcionando con **letra del sistema a 1.3×**
  sin cortes ni solapamientos.
- Objetivos táctiles **≥ 48 dp**, con separación; uso cómodo con una mano.
- **Contraste AA o mejor** pensando en sol directo.
- **Lectura lenta / baja alfabetización:** frases cortas, palabras de uso diario, íconos o símbolos siempre
  acompañados de texto, poco texto por bloque.
- Semáforos y niveles con **forma + texto**, nunca solo color.
- **TalkBack:** `contentDescription` en íconos y gráficos, y una alternativa en texto o tabla para cada gráfico.
