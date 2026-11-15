# Diagnóstico UI/UX — Fase 1 (antes de arreglar)

- **Fecha:** 14-nov-2026 · **Rama:** `ui_kendra_pos` · **Commit evaluado:** `f960ae8`
- **Dispositivo:** Samsung Galaxy A73 5G (SM-A736B), Android 16, 1080×2400 a 450 dpi (≈384 dp de ancho). **Celular físico, no emulador.**
- **Datos:** modo demo (`farmealotu.demoMode=true`), franja «MODO DEMO · DATOS DE EJEMPLO» visible en todas las capturas. Ninguna captura es evidencia NASA.
- **Modos capturados:** `antes/claro/` (recorrido completo), `antes/oscuro/` (muestra), `antes/letra_1.3/` (pantallas clave).
- **Método:** Impeccable `critique` (evaluación de diseño; el detector automático no aplica en plataforma nativa) + `audit` nativo (revisión de código + capturas).

## Puntajes

### Critique — heurísticas de Nielsen (26/40 · Aceptable)

| # | Heurística | Nota | Hallazgo clave |
|---|---|---|---|
| 1 | Visibilidad del estado | 3 | «Consultado el…», progreso 1 de 5 y aviso de datos desactualizados funcionan; el mapa muestra un error 403 en inglés como si fuera el mapa |
| 2 | Lenguaje del usuario | 3 | Voseo claro; quedan «P10-P90», «percentil 95», «s/d» sin explicar en línea |
| 3 | Control y libertad | 3 | Volver, Inicio y «Editar mis datos» en todas partes (hasta redundante) |
| 4 | Consistencia | 2 | Módulos se abren con filas en Inicio y con botones en Resultado; 02 y 04 marcan «Más» en la barra; emoji como íconos |
| 5 | Prevención de errores | 3 | Cultivo no reconocido pide elegir; no se fuerza ganador |
| 6 | Reconocer en vez de recordar | 3 | Resumen editable, secuencia repetida en cada tarjeta |
| 7 | Eficiencia | 2 | Pantallas de 4–7 alturas de scroll; el mapa de calor de Riesgos queda al fondo |
| 8 | Estética y minimalismo | 2 | Todo con el mismo peso: 6–9 botones contorneados iguales por pantalla; «❓ ¿Qué significa?» repetido hasta 7 veces |
| 9 | Recuperación de errores | 2 | Bloques de error con «Reintentar» bien; el 403 del mapa muestra texto técnico ajeno |
| 10 | Ayuda | 3 | «¿Qué significa?» y «Fuentes y límites» completos; tanta ayuda compite con el contenido |

**Carga cognitiva:** 3 de 8 fallas (moderada) — foco único (Inicio), jerarquía visual (Inicio, Resultado), opciones mínimas (Resultado: 1 principal + 8 secundarias).

### Audit nativo (11/20 · Aceptable)

| # | Dimensión | Nota | Hallazgo clave |
|---|---|---|---|
| 1 | Accesibilidad | 2 | Emoji leídos por TalkBack en 20 textos; textos de Canvas y tablas se cortan con letra 1.3 |
| 2 | Rendimiento | 3 | Sin problemas visibles en el A73; no se perfiló (sin medición) |
| 3 | Apariencia y tema | 2 | Roles M3 bien usados (1 hex fuera de `theme/`), pero **no existe esquema oscuro** |
| 4 | Conformidad Android | 2 | Emoji en vez de íconos Material; inset inferior duplicado; títulos truncados por el botón «🏠 Inicio» |
| 5 | Adaptabilidad | 2 | Solo teléfono vertical; tablas desbordan a 384 dp; horizontal y tablet sin probar |

## Lista priorizada

P0 = bloquea o arruina la demo · P1 = importante · P2 = opcional. Evidencia en `docs/ui_review/antes/`.

### P0

| # | Pantalla | Archivo | Problema | Evidencia | Arreglo propuesto |
|---|---|---|---|---|---|
| 1 | Explorar mi zona | `ui/screens/geo/OsmMap.kt:45` | El mapa base sale como mosaico «403 Access blocked… tile usage policy». La app se identifica ante OSM como `com.example.farmealotu`, y OSM bloquea los identificadores genéricos. | `claro/14_mi_zona_1.png` | User-Agent propio de la app (p. ej. `FarmealoTu-Android/1.0`), sin datos personales; limpiar la caché de teselas de osmdroid. No toca datos ni capas NASA. |
| 2 | Módulos 01, 04 (y 02 con letra 1.3) | `ui/components/Common.kt:96-134` | Títulos obligatorios truncados: «Comparar formas de s…», «¿Qué tan segura es est…», «Ventajas y desven…». El botón «🏠 Inicio» ocupa ~90 dp de la barra. | `claro/09_comparar_1.png`, `claro/13_incertidumbre_1.png`, `letra_1.3/11_ventajas_1.png` | «Inicio» como botón de ícono de 48 dp con descripción; título hasta 2 líneas. |
| 3 | 02 Ventajas y desventajas | `ui/screens/tradeoffs/TradeoffsScreen.kt:163-164` | En «Los mismos números, en tabla» la columna **Riesgo** queda fuera de pantalla («Rie»); solo se ve deslizando de costado, sin indicio. | `claro/11_ventajas_radar_0.png`, `letra_1.3/11_ventajas_tabla.png` | Tabla que entra en el ancho: columna de opción flexible y tres columnas numéricas fijas; sin scroll horizontal. |

### P1

| # | Pantalla | Archivo | Problema | Evidencia | Arreglo propuesto |
|---|---|---|---|---|---|
| 4 | Todas | `ui/components/Common.kt:87` | Franja vacía de ~45 dp sobre la barra inferior: el `Scaffold` interno vuelve a aplicar el inset de la barra del sistema que ya consumió el externo. El contenido se corta antes. | cualquier captura, borde inferior | `contentWindowInsets = WindowInsets(0)` en `FarmScreen`. |
| 5 | Todas | `ui/theme/Theme.kt`, `MainActivity.kt` | **No hay tema oscuro**: con el sistema en oscuro la app se ve idéntica (decisión previa «legibilidad al sol»). El prompt de pulido pide uno diseñado. | `oscuro/*.png` | `darkColorScheme` propio (verdes y tierras en tono nocturno, papel → carbón cálido). Gráficos: ver pregunta abierta (regla 9). |
| 6 | Todas | `FarmealoTuNavHost.kt:153`, 20 textos con emoji | Íconos hechos con emoji (🏠 🌾 🌧️ 🗺️ ⚙️ ✏️ ❓ 📋 💾 🔄 ⚡ 📅): estilo ajeno a Material, el ❓ rojo compite con el verde, y TalkBack lee «casa con jardín, Inicio». No hay biblioteca de íconos en el proyecto. | todas | Íconos vectoriales Material (ver pregunta abierta: dependencia nueva o vectores propios en código). |
| 7 | Inicio, Resultado | `HomeScreen.kt`, `DecisionScreen.kt:119-126` | Jerarquía plana: Resultado tiene 1 botón principal + 8 contorneados iguales; «Seguir revisando» usa botones mientras Inicio usa filas de módulo; Inicio repite «Ver resultado» y «Comparar opciones de siembra» (mismo destino). | `claro/08_resultado_2.png`, `claro/01b_inicio_con_resultado_1-2.png` | Reutilizar `ModuleRow` para 02–04 en Resultado; pie como botones de texto; un solo CTA en Inicio. |
| 8 | Detalle, Riesgos, 04 | `ui/components/Common.kt:385-395` | «❓ ¿Qué significa?» aparece hasta 7 veces por pantalla, con sangría de ~16 dp que lo desalinea del texto. | `claro/10_detalle_1.png`, `claro/12_riesgos_3.png` | Botón de texto alineado al borde, ícono de ayuda en verde; mismo destino. |
| 9 | Resultado, Inicio | — | El aviso obligatorio **«Esta herramienta apoya tu decisión; no reemplaza tu experiencia ni a tu técnico.»** (plan §6) no aparece en ninguna pantalla (búsqueda en código: 0 resultados). | grep | Mostrarlo al pie de Resultado e Inicio. *Agrega texto del plan: necesita tu OK (regla 11).* |
| 10 | Explorar mi zona | `ui/screens/geo/GeoViewerScreen.kt:179-218` | Las etiquetas de la pila de capas se dibujan en Canvas con ancho fijo: con letra 1.3 se cortan («Información regiona», «Humedad del s», «Datos no disponible»), y es texto de procedencia. | `letra_1.3/14_mi_zona_capas.png` | Etiquetas como `Text` de Compose al lado del dibujo (o medir y partir líneas). |
| 11 | 02 (radar) | `ui/components/Charts.kt:196-233` | La etiqueta «50» queda tapada por los puntos; hueco de ~70 dp bajo el radar; 4 polígonos casi superpuestos. | `claro/11_ventajas_radar_0.png` | Etiquetas de anillo fuera de los ejes, alto ajustado. Mismos datos y colores. |
| 12 | 03 Riesgos (tabla) | `ui/components/Charts.kt:336-338` | La tabla de 12 meses se desplaza de costado sin indicio y la columna «Año» se va con el scroll. | `claro/12_riesgos_tabla.png`, `claro/12_riesgos_tabla_hscroll.png` | Columna de año fija + indicio de desplazamiento, o dos bloques (ene–jun / jul–dic). |
| 13 | Barra inferior | `FarmealoTuNavHost.kt:130-136` | 01 resalta «Siembra», pero 02 y 04 resaltan «Más»; «Más» abre directo «Fuentes y límites». | `claro/11_ventajas_1.png`, `claro/13_incertidumbre_1.png` | Resaltar «Siembra» en 01–04. *Toca la navegación: necesita tu OK (regla 11).* |
| 14 | Todas (letra 1.3) | `FarmealoTuNavHost.kt:145-170` | Etiquetas de la barra inferior casi se tocan («Siembra Riesgos Mi Zona»); en 360 dp se van a cortar. | `letra_1.3/01_inicio_1.png` | Ajustar estilo/ancho de etiquetas en `adapt`. |

### P2

| # | Pantalla | Archivo | Problema | Arreglo propuesto |
|---|---|---|---|---|
| 15 | Datos de terreno | `ui/components/Charts.kt:387+` | Etiquetas finales «A» y «B» superpuestas; marcas del eje no redondas (6 · 15.7 · 25.3 · 35). | Separar etiquetas que chocan; marcas redondas. |
| 16 | 04 | `ui/components/Charts.kt:140-167` | El eje 0 · 50 · 100 se repite bajo cada una de las 4 estrategias. | Un eje compartido al pie. |
| 17 | 03 (leyenda) | `ui/components/HeatScale.kt:39` | Rangos que comparten borde («1 a 6 días», «6 a 11 días»): ¿6 va en cuál? | Formato «1–6», «más de 6 a 11». |
| 18 | Tema | `Theme.kt`, `Common.kt` | Decisiones aprobadas en DESIGN.md: papel con matiz verde y elevación plana tonal (hoy sombras de 1–2 dp). | Aplicar en la Fase 2 (tema). |
| 19 | Pregunta 3 y 4 | `FarmerInputScreen.kt:317` | Opciones con `Role.RadioButton` pero sin `selectableGroup()`: TalkBack no anuncia «1 de 4». | Agregar `selectableGroup()` al contenedor. |
| 20 | Fuentes y límites | `SourcesScreen.kt` | Cifras sin separador de miles (318083). | Formato `318 083`; sin cambiar el valor. |
| 21 | Conexión | `ConnectionScreen.kt` | La dirección de fábrica `10.0.2.2` solo sirve en emulador; en el celular hay que poner la IP de la PC. | Nota en la guía rápida (no es UI). |

## Lo que está bien (conservar)

- La franja **MODO DEMO** es inconfundible en todas las pantallas.
- Procedencia con **texto** en cada dato (Documentado / Calculado / Supuesto / Dato tuyo / No disponible) y escala «regional (~50 km)».
- Semáforos con **forma + texto** (círculo / triángulo / cuadrado).
- Los datos que faltan se ven: celdas «—» en el mapa de calor, «s/d» en tablas, capas «Datos no disponibles», «Baja cobertura» como «No disponible».
- El cuestionario va de a **una pregunta por pantalla**, con progreso y resumen editable.
- Texto principal grande (≥ 16 sp) y objetivos táctiles ≥ 48 dp en todo lo revisado.
- Cada gráfico tiene su alternativa en tabla o texto.

## Lo que no se pudo comprobar

- Rendimiento medido (no se perfiló), brillo bajo sol real, horizontal y tablet, TalkBack de punta a punta (solo revisión de código).
- Comportamiento con el backend real (no disponible): solo modo demo.
