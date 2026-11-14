---
name: FarmealoTu
description: Herramienta Android que ayuda al agricultor de Cuatro Cañadas a comparar rotaciones con datos NASA interpretados y procedencia visible.
colors:
  field-green: "#2E6B34"
  field-green-deep: "#1B4721"
  field-green-container: "#D9EBD3"
  earth: "#7A4F2A"
  earth-container: "#F1E2CF"
  on-earth-container: "#3B2410"
  clay: "#8A5A00"
  clay-container: "#FFE9B8"
  on-clay-container: "#3D2800"
  paper: "#F7F4EC"
  card-surface: "#FCFCFB"
  surface-container-lowest: "#FFFFFF"
  surface-container: "#F3F0E6"
  card-surface-variant: "#EDE9DD"
  surface-container-highest: "#E6E2D4"
  ink: "#14180F"
  ink-secondary: "#45493F"
  outline: "#7B7F72"
  outline-soft: "#CFCBBD"
  alert-red: "#A62B22"
  alert-red-container: "#FBE0DC"
  on-alert-red-container: "#5C120D"
  on-color: "#FFFFFF"
  demo-stripe: "#5B2A86"
  on-demo-stripe: "#FFFFFF"
  chart-series-e0: "#2A78D6"
  chart-series-e1: "#EB6834"
  chart-series-e2: "#1BAF7A"
  chart-series-e3: "#EDA100"
  chart-series-other: "#898781"
  chart-seq-1: "#CDE2FB"
  chart-seq-2: "#86B6EF"
  chart-seq-3: "#3987E5"
  chart-seq-4: "#1C5CAB"
  chart-seq-5: "#0D366B"
  chart-div-low: "#D03B3B"
  chart-div-low-soft: "#EC9A93"
  chart-div-mid: "#F0EFEC"
  chart-div-high-soft: "#86B6EF"
  chart-div-high: "#1C5CAB"
  chart-no-data: "#F0EFEC"
  chart-grid: "#E1E0D9"
  chart-axis: "#C3C2B7"
  chart-muted: "#6F6D67"
  chart-ink: "#0B0B0B"
  chart-ink-secondary: "#52514E"
  status-good: "#0CA30C"
  status-warning: "#FAB219"
  status-serious: "#EC835A"
  status-critical: "#D03B3B"
typography:
  headline-medium:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "26sp"
    fontWeight: 700
    lineHeight: "32sp"
  headline-small:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "23sp"
    fontWeight: 700
    lineHeight: "29sp"
  title-large:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "21sp"
    fontWeight: 600
    lineHeight: "27sp"
  title-medium:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "18sp"
    fontWeight: 600
    lineHeight: "24sp"
  title-small:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "22sp"
  body-large:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "18sp"
    fontWeight: 400
    lineHeight: "26sp"
  body-medium:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "23sp"
  body-small:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  label-large:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "22sp"
  label-medium:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "18sp"
  label-small:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "13sp"
    fontWeight: 500
    lineHeight: "17sp"
  chart-axis:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "12sp"
    fontWeight: 400
  chart-label:
    fontFamily: "Roboto (FontFamily.Default)"
    fontSize: "13sp"
    fontWeight: 500
rounded:
  swatch: "3dp"
  bar: "4dp"
  sm: "12dp"
  md: "14dp"
  lg: "16dp"
  full: "50%"
spacing:
  xxs: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
components:
  top-app-bar:
    backgroundColor: "{colors.field-green}"
    textColor: "{colors.on-color}"
    typography: "{typography.title-large}"
  demo-stripe:
    backgroundColor: "{colors.demo-stripe}"
    textColor: "{colors.on-demo-stripe}"
    typography: "{typography.label-large}"
    padding: "6dp 12dp"
  button-primary:
    backgroundColor: "{colors.field-green}"
    textColor: "{colors.on-color}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.md}"
    padding: "12dp 16dp"
    height: "54dp"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.field-green}"
    typography: "{typography.label-large}"
    rounded: "{rounded.md}"
    padding: "10dp 16dp"
    height: "50dp"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.field-green}"
    typography: "{typography.label-large}"
    height: "48dp"
  card:
    backgroundColor: "{colors.card-surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  card-highlight:
    backgroundColor: "{colors.field-green-container}"
    textColor: "{colors.field-green-deep}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  card-quiet:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  card-caution:
    backgroundColor: "{colors.clay-container}"
    textColor: "{colors.on-clay-container}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  card-error:
    backgroundColor: "{colors.alert-red-container}"
    textColor: "{colors.on-alert-red-container}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  module-row:
    backgroundColor: "{colors.card-surface-variant}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "12dp 14dp"
    height: "64dp"
  pill:
    backgroundColor: "{colors.card-surface-variant}"
    textColor: "{colors.ink}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.full}"
    padding: "5dp 12dp"
  provenance-documented:
    backgroundColor: "{colors.field-green-container}"
    textColor: "{colors.field-green-deep}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.full}"
  provenance-derived:
    backgroundColor: "{colors.earth-container}"
    textColor: "{colors.on-earth-container}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.full}"
  provenance-assumption:
    backgroundColor: "{colors.clay-container}"
    textColor: "{colors.on-clay-container}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.full}"
  provenance-unavailable:
    backgroundColor: "{colors.alert-red-container}"
    textColor: "{colors.on-alert-red-container}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.full}"
  filter-chip:
    typography: "{typography.label-large}"
    height: "48dp"
  navigation-bar:
    backgroundColor: "{colors.card-surface}"
    textColor: "{colors.ink-secondary}"
    typography: "{typography.label-medium}"
  navigation-bar-selected:
    backgroundColor: "{colors.field-green-container}"
    textColor: "{colors.field-green}"
    typography: "{typography.label-medium}"
  score-bar:
    backgroundColor: "{colors.chart-grid}"
    rounded: "{rounded.bar}"
    height: "14dp"
  score-bar-emphasized:
    backgroundColor: "{colors.chart-grid}"
    rounded: "{rounded.bar}"
    height: "18dp"
---

# Design System: FarmealoTu

> Este documento describe el sistema **tal como está implementado** en `app/src/main/java/com/example/farmealotu/ui/`
> (`theme/Color.kt`, `theme/Type.kt`, `theme/Theme.kt`, `components/Common.kt`, `components/Charts.kt`,
> `components/HeatScale.kt`). Las decisiones aprobadas que todavía no están en el código se marcan
> **«aprobada, pendiente de aplicar»**.

## Overview

**Creative North Star: "El Campo Ordenado"**

Una herramienta de trabajo sobria y honesta: tonos de campo suaves de fondo, una estructura clara y ordenada, poco
texto por bloque y nada que abrume. La sencillez está pensada para el agricultor que lee despacio y usa el celular
con una mano y a pleno sol. El mismo orden tiene que convencer a un jurado de que hay buen diseño detrás. La marca no
se luce con adornos: está en el verde de cultivo, en la tierra y el ocre que distinguen procedencias, y en lo
preciso de cada etiqueta.

La densidad es baja y vertical. Cada pantalla es una sola columna de tarjetas grandes con 16 dp de aire, que se lee
de arriba abajo: primero la conclusión o la acción, después el detalle y, plegado al final, lo técnico. Los colores
de marca sirven para orientar (acción principal, tarjeta destacada, procedencia); los gráficos usan una paleta
aparte, validada para daltonismo, que nunca se mezcla con la de la interfaz.

La app sigue el modo del teléfono, sin color dinámico (`Theme.kt`). El esquema claro, de papel, es el de pleno sol.
El esquema oscuro está diseñado aparte: carbón cálido con un matiz verde, con los mismos tonos de cultivo, tierra y
ocre en versión nocturna. En oscuro, los gráficos se dibujan sobre una tarjeta clara (`chartSurface()`) para usar
`ChartPalette` sin cambios. Los valores del esquema oscuro están en `Theme.kt` (`FarmDarkScheme`); los tokens de
arriba describen el claro.

**Key Characteristics:**
- Una columna, tarjetas grandes y un ritmo vertical constante de 16 dp.
- Verde de cultivo para la acción y lo destacado, tierra y ocre para matizar, papel claro de base.
- Texto principal de 16 sp o más; botones de 50–54 dp de alto.
- Procedencia, niveles y estados siempre con texto y forma, nunca solo con color.
- La franja morada de MODO DEMO no se parece a nada más en la app.
- Gráficos en Canvas con paleta propia y alternativa en texto o tabla.

## Colors

Una paleta de campo: un verde de cultivo profundo acompañado de tierra y ocre, sobre papel cálido y tinta casi negra
con matiz verde.

### Primary
- **Verde Cultivo** (`field-green`): barra superior, botón principal, borde de la tarjeta destacada, texto de botones
  secundarios y de texto, números de módulo y el indicador de navegación seleccionado. Es el color de la acción.
- **Verde Cultivo Hondo** (`field-green-deep`): texto sobre el contenedor verde (tarjeta destacada, procedencia
  «documentado»).
- **Brote** (`field-green-container`): fondo de la tarjeta «¿Qué conviene revisar?», del resultado recomendado, de la
  procedencia «documentado / medido» y del indicador de la barra de navegación.

### Secondary
- **Tierra Cañadera** (`earth`) y **Tierra Clara** (`earth-container` con texto `on-earth-container`): se usan sobre
  todo en la procedencia «Calculado por FarmealoTu» (`derived`).

### Tertiary
- **Ocre Rastrojo** (`clay`) y **Rastrojo Claro** (`clay-container` con texto `on-clay-container`): avisos de
  precaución que no son errores, como «Cambiaste tus datos», «No se pudo actualizar» o «Información insuficiente», y
  la procedencia supuesta o simulada (`assumption`, `simulated`, `wokwi`).

### Neutral
- **Papel de Libreta** (`paper`): fondo de todas las pantallas.
- **Hoja** (`card-surface`): superficie de tarjetas y de la barra de navegación.
- **Papel Tostado** (`surface-container`): tarjetas tranquilas (fecha de datos, estado de conexión, vacíos).
- **Cartulina** (`card-surface-variant`): filas de módulo y píldoras neutras.
- **Tinta de Campo** (`ink`) y **Tinta Gris** (`ink-secondary`): texto principal y texto auxiliar.
- **Alambrado** (`outline`) y **Alambrado Suave** (`outline-soft`): contornos y bordes finos de las tarjetas (el suave
  al 70 %).
- **Rojo Alerta** (`alert-red`, con `alert-red-container` / `on-alert-red-container`): errores y la procedencia
  «No disponible».

### Señal de demo
- **Morado Demo** (`demo-stripe` con `on-demo-stripe`): solo para la franja «MODO DEMO · DATOS DE EJEMPLO».

### Paleta de gráficos (`ChartPalette`)
Validada para daltonismo. Estos son sus valores normativos:
- **Series** (`chart-series-e0` … `e3`, `chart-series-other`): la identidad de E0–E3 o de las zonas A, B y C. El
  color sigue a la estrategia, no a su puesto en el ranking (`ChartPalette.forStrategy`).
- **Secuencial** (`chart-seq-1` … `5`): magnitud, un solo tono de claro a oscuro (mapa de calor).
- **Divergente** (`chart-div-*`): valores por debajo o por encima de lo habitual, con gris neutro al medio.
- **Estados** (`status-*`): reservados para semáforos; siempre van con forma y texto.
- **Estructura** (`chart-grid`, `chart-axis`, `chart-muted`, `chart-no-data`, `chart-ink*`): rejilla, ejes y
  celdas sin dato.

### Named Rules
**The Two Palettes Rule.** La interfaz usa los roles de `MaterialTheme.colorScheme`; los gráficos usan
`ChartPalette`. Ningún color de la interfaz representa una serie y ningún color de serie pinta la interfaz.

**The Fixed Order Rule.** El orden y los valores de `ChartPalette` están validados para daltonismo. No se reciclan,
no se reordenan y no se cambian sin volver a validarlos.

**The Purple Means Demo Rule.** El morado aparece solo en la franja de MODO DEMO. Si lo ves en otro lugar, es un error.

**The Green Paper Rule (aprobada, pendiente de aplicar).** Llevar `paper` y las superficies neutras a un papel con un
matiz verde leve, sin perder la calidez ni el contraste de la tinta. Es un cambio pequeño dentro de la misma
identidad; se aplica en la Fase 2 (tema) y se revalidan los contrastes.

## Typography

**Display Font:** Roboto del sistema (`FontFamily.Default`)
**Body Font:** Roboto del sistema (`FontFamily.Default`)

**Character:** una sola familia del sistema, con la jerarquía hecha de tamaño y peso (Bold 700 en titulares,
SemiBold 600 en títulos y etiquetas). No hay fuentes de marca. La escala es más grande que el Material por defecto,
para leer al sol.

### Hierarchy
- **Headline Medium** (700, 26/32 sp): definido en el tema, casi sin uso en pantalla.
- **Headline Small** (700, 23/29 sp): el nombre de la opción recomendada y «Información insuficiente para ordenar
  opciones».
- **Title Large** (600, 21/27 sp; en la barra superior, Bold): títulos de pantalla y la conclusión de la tarjeta
  principal.
- **Title Medium** (600, 18/24 sp): títulos de tarjeta (`SectionCard`), el texto del botón principal y el valor de
  las barras destacadas.
- **Title Small** (600, 16/22 sp): títulos de fila de módulo, «Datos actualizados al» y etiquetas de slider.
- **Body Large** (400, 18/26 sp): mensajes principales dentro de las tarjetas, como la razón o «a tener en cuenta».
- **Body Medium** (400, 16/23 sp): el texto corriente. Es el piso del texto principal.
- **Body Small** (400, 14/20 sp): solo notas auxiliares (fechas de consulta, aclaraciones del puntaje, descripciones
  de módulo).
- **Label Large / Medium / Small** (600 / 500 / 500, 16 / 14 / 13 sp): botones, chips, píldoras de procedencia y
  etiquetas de la barra de navegación.
- **Texto de gráfico** (12 sp para ejes, 13 sp Medium para etiquetas): se dibuja en Canvas fuera de la escala M3, con
  `ChartPalette.muted` y `ChartPalette.ink`.

### Named Rules
**The 16 sp Floor Rule.** El texto que hay que leer va a 16 sp o más. 14 sp queda para lo auxiliar, y 12–13 sp solo
para ejes y etiquetas de gráficos.

**The Roles Not Sizes Rule.** Las pantallas usan `MaterialTheme.typography.*`, también las etiquetas de la pila de
capas del geovisor. Solo el texto de ejes de los gráficos (12–13 sp) queda fuera de la escala.

## Layout

- **Modelo:** una columna con scroll vertical dentro de `FarmScreen`: barra superior, la franja de demo opcional y el
  contenido con 16 dp de margen lateral y superior, separado con `spacedBy(16.dp)` entre bloques.
- **Navegación persistente:** barra inferior de 5 destinos (Inicio, Siembra, Riesgos, Mi Zona, Más) que envuelve a
  todas las pantallas.
- **Dentro de las tarjetas:** 16 dp de relleno y 12 dp entre elementos. Las listas internas usan 4–8 dp.
- **Ritmo:** la escala que domina es 4 / 8 / 12 / 16 / 24 dp. Todavía aparecen valores sueltos de 2, 6, 10 y 14 dp
  (6 dp es el más repetido, sobre todo en listas y leyendas).
- **Pares de acciones discretas:** dos `QuietAction` con `weight(1f)` y 8 dp de separación (pie de los módulos y
  «Estado de conexión»).
- **Tablas:** entran en el ancho del teléfono, sin scroll lateral. Si no caben, se parten en filas de dos líneas o
  en bloques (6 meses + 6 meses).
- **Responsivo:** no hay disposición específica para tablet ni horizontal. La columna se estira al ancho disponible.
- **Insets:** edge-to-edge (`enableEdgeToEdge`) con `imePadding()` en el andamiaje de pantalla. La barra superior
  consume la barra de estado; el `Scaffold` de `FarmScreen` no aplica insets propios (`WindowInsets(0)`) porque la
  barra inferior ya consume la de navegación.

### Named Rules
**The One Column Rule.** Una pantalla es una sola columna que se lee de arriba abajo: conclusión, acción y luego el
detalle. Lo técnico va al final y plegado.

## Elevation & Depth

Hoy el sistema es mixto. Las tarjetas combinan una sombra de 1 dp con un borde fino de `outline-soft` al 70 %. El
botón principal tiene 2 dp de sombra, y la barra de navegación usa elevación tonal de 6 dp. Las tarjetas tranquilas
(`surface-container`, sin borde) ya se apoyan solo en el tono.

### Shadow Vocabulary
- **Tarjeta** (elevación Material 1 dp + borde de 1 dp en `outline-soft` al 70 %): todas las `SectionCard`.
- **Botón principal** (elevación Material 2 dp): `PrimaryButton`.
- **Barra de navegación** (elevación tonal de 6 dp): `NavigationBar`.
- **Sombra del geovisor** (negro al 20 %, `0x33000000`): sombra de las capas de la pila isométrica en Canvas.

### Named Rules
**The Flat Tonal Rule (aprobada, pendiente de aplicar).** La profundidad se expresa con los niveles tonales de
superficie de Material 3 y bordes finos, con una sombra casi nula. Así se lee más limpio y ordenado, y al sol no se
ensucia. La tarjeta destacada se distingue por su contenedor y su borde de 2 dp, no por la sombra.

## Shapes

Las esquinas son redondeadas medias, amables sin ser blandas. Las tarjetas usan 16 dp. Los botones y las filas de
módulo usan 14 dp, y hay un caso de 12 dp. Las píldoras de procedencia y de estado son totalmente redondeadas. Las
barras de puntaje usan 4 dp, con el extremo de la base recto para que «nazcan» del eje. Las muestras de leyenda usan
3 dp. Los semáforos (`LevelBadge`) suman forma al color: círculo para Bajo, triángulo para Medio y cuadrado para
Alto. Los bordes son finos (1 dp), salvo en la tarjeta destacada y la de «Información insuficiente» (2 dp) y en el
botón secundario (1,5 dp).

## Components

**Personalidad de componentes:** grandes, firmes y tranquilos. Objetivos amplios, redondeo medio, poco adorno y una
acción principal clara por pantalla.

### Buttons
- **Shape:** esquinas redondeadas medias (14 dp).
- **Primary (`PrimaryButton`):** relleno Verde Cultivo con texto blanco en Title Medium Bold; ancho completo, alto
  mínimo de 54 dp, relleno de 12/16 dp y 2 dp de sombra. Ejemplos: «Comparar opciones de siembra», «Comparar todas»,
  «Reintentar».
- **Secondary (`SecondaryButton`):** contorno de 1,5 dp en Verde Cultivo y texto verde en Label Large SemiBold;
  ancho completo, alto mínimo de 50 dp. Se usa para navegar y para acciones alternativas.
- **Íconos en botones:** `PrimaryButton` y `SecondaryButton` aceptan un ícono decorativo de 20 dp antes del texto.
- **Discreta (`QuietAction`):** botón de texto verde con ícono opcional, alto mínimo de 48 dp. Para acciones de
  salida o mantenimiento («Editar mis datos», «Volver al inicio», «Recomprobar»), que no deben competir con la
  acción principal.
- **Text (`ExplainButton`):** botón de texto verde Bold con ícono de ayuda, alto mínimo de 48 dp y alineado al borde
  del texto, para «¿Qué significa?». Abre un `AlertDialog` con explicación, detalles y «Límite».
- **Estados:** los que trae Material 3 por defecto (ripple, deshabilitado). No hay tratamientos propios de foco ni de
  pulsación.

### Chips
- **Filter chips:** `FilterChip` de Material 3 sin personalizar, con etiqueta Label Large SemiBold y alto mínimo de
  48 dp. Sirven para elegir cultivo, excluir cultivos y filtrar capas, zonas e indicadores.
- **Píldoras (`Pill`):** fondo Cartulina, totalmente redondeadas, Label Medium SemiBold, relleno de 5/12 dp. Muestran
  estados como «Puntaje parcial: falta un criterio».
- **Procedencia (`ProvenanceChip`):** una píldora con texto en lenguaje sencillo, con el color según el estado:
  documentado o medido en Brote, calculado en Tierra Clara, supuesto o simulado en Rastrojo Claro, no disponible en
  rojo claro y el resto neutro. El texto siempre está; el color solo refuerza.

### Cards / Containers
- **Corner Style:** 16 dp.
- **Background:** Hoja (`SectionCard` normal), Brote con borde verde de 2 dp (destacada), Papel Tostado sin borde
  (tranquila), Rastrojo Claro con borde ocre (precaución) y rojo claro con borde rojo (error).
- **Shadow Strategy:** ver Elevation & Depth.
- **Border:** 1 dp en Alambrado Suave al 70 %.
- **Internal Padding:** 16 dp, con 12 dp entre hijos. Título opcional en Title Medium Bold, marcado como `heading()`.

### Inputs / Fields
- **Style:** `OutlinedTextField` de Material 3 sin personalizar (contorno, etiqueta flotante y `isError` para
  validar). Notas de 4 líneas como mínimo; latitud y longitud en una sola línea.
- **Sliders de prioridad:** `Slider` de Material 3 de 0 a 1 en pasos de 0,05, con la etiqueta (Title Small Bold) y el
  porcentaje normalizado (Title Medium Bold) en una fila encima.
- **Switch:** el `Switch` de Material 3 en «Conexión» (modo demo).

### Navigation
- **Íconos (`FarmIcons`):** trazados de Material Icons (Apache 2.0) escritos como `ImageVector`, sin biblioteca de
  íconos: Volver, Inicio, Siguiente, Editar, Ayuda, Hoja (Siembra), Nube (Riesgos), Mapa, Más. Nunca emoji.
- **Barra superior (`FarmScreen`):** barra propia con fondo Verde Cultivo (Verde Hondo en tema oscuro). El título va
  en Title Large Bold y ocupa hasta 3 líneas, para que el título obligatorio de cada módulo se lea completo; el
  subtítulo opcional va en Body Small (por ejemplo «Módulo 01»). Volver e Inicio son `IconButton` de 48 dp con
  descripción («Volver», «Ir al inicio»).
- **Barra inferior:** `NavigationBar` sobre Hoja, con indicador Brote, ícono y texto seleccionados en Verde Cultivo y
  los no seleccionados en Tinta Gris. Las etiquetas van en Label Small, en una línea (Bold si está seleccionada).
  «Siembra» abarca las preguntas, el resultado y los módulos 01, 02 y 04; el 03 tiene su pestaña «Riesgos».
- **Filas de módulo (`ModuleRow`):** superficies tocables de 64 dp de alto mínimo, en Cartulina con borde suave.
  Llevan el número de módulo en verde, título, descripción e ícono de avance. Son el único modo de abrir un módulo,
  en Inicio y en Resultado.

### Aviso de decisión (`DecisionDisclaimer`)
Texto exacto del plan (§6), en Body Small: «Esta herramienta apoya tu decisión; no reemplaza tu experiencia ni a tu
técnico.» Va al pie de Inicio y de cada módulo (dentro de `FooterActions`).

### Franja MODO DEMO (`DemoBanner`)
Firma del sistema. Es una franja a todo el ancho en Morado Demo, justo debajo de la barra superior, en todas las
pantallas: «MODO DEMO · DATOS DE EJEMPLO» en Label Large Bold centrado, con descripción para TalkBack. Tiene que
seguir siendo inconfundible.

### Semáforo (`LevelBadge`)
Combina una forma (círculo, triángulo o cuadrado) con el color de estado de `ChartPalette` y el texto «Prefijo:
Nivel». Para TalkBack se fusiona en una sola descripción.

### Gráficos (`Charts.kt`, `HeatScale.kt`)
Todos se dibujan en Compose Canvas:
- `ScoreBar`: barra de puntaje horizontal de 14 dp (18 dp si está destacada) sobre la rejilla, con la etiqueta a la
  izquierda (76 dp) y el valor a la derecha (48 dp). Si el valor es `null` muestra «Dato insuficiente».
- `RangeBar`: intervalo P10–P90 con marca P50, sobre un eje común.
- `RadarChart`: tres ejes, sin vértices falsos cuando un valor es `null`.
- `MonthYearHeatmap`: mapa de calor de meses por años, con celdas tocables, y `HeatmapTable` como alternativa
  accesible.
- `TimeSeriesChart`: series de tiempo, con `SeriesLegend` y `HeatLegend`.
Cada gráfico tiene `contentDescription` o un equivalente en texto.

## Do's and Don'ts

### Do:
- **Do** usar los roles de `MaterialTheme.colorScheme` y `MaterialTheme.typography` en las pantallas; los valores
  hex viven solo en `ui/theme/`.
- **Do** abrir cada pantalla con la conclusión o la acción en una tarjeta destacada (Brote con borde verde de 2 dp) y
  dejar una sola acción principal con `PrimaryButton`.
- **Do** mantener el texto principal en 16 sp o más y los objetivos táctiles en 48 dp o más (botones de 50–54 dp).
- **Do** acompañar cada color de estado o de procedencia con texto y, en los semáforos, con forma.
- **Do** mostrar «sin dato», «no disponible» o «dato insuficiente» en el lugar del valor, con el mismo peso visual
  que un dato.
- **Do** usar el color de serie de `ChartPalette.forStrategy(id)` para identificar E0–E3 en todos los gráficos.

### Don't:
- **Don't** usar emoji ni glifos de texto (←, ›, ❓) como íconos: van los de `FarmIcons`, y TalkBack lee el texto del
  botón, no el ícono.
- **Don't** usar scroll lateral en tablas ni recortar texto de procedencia o títulos de módulo.
- **Don't** cambiar valores ni orden de `ChartPalette` sin volver a validarla para daltonismo.
- **Don't** usar el morado de la franja demo en otro lugar, ni esconder, achicar o volver ambigua la franja «MODO
  DEMO · DATOS DE EJEMPLO».
- **Don't** quitar ni esconder las etiquetas de procedencia, la escala «contexto regional (~50 km)» o las insignias
  SIMULADO / WOKWI / FÍSICO.
- **Don't** pintar un dato `null` o `unavailable` como 0 o en verde.
- **Don't** poner gráficos técnicos en el Inicio ni delante de la conclusión en lenguaje sencillo.
- **Don't** mostrar «RotaSat» en pantalla.
