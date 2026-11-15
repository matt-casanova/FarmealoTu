# Pulido UI/UX de FarmealoTu Android (Impeccable) — 14-nov-2026

Rama `ui_kendra_pos`. Diagnóstico completo en [DIAGNOSTICO_FASE1.md](DIAGNOSTICO_FASE1.md).
Evidencia: Samsung Galaxy A73 (Android 16, ≈384 dp) en **modo demo**. Las capturas muestran la franja
«MODO DEMO · DATOS DE EJEMPLO» y **no son evidencia NASA**.

| Carpeta | Contenido |
|---|---|
| `antes/claro`, `antes/oscuro`, `antes/letra_1.3` | Estado al empezar (commit `f960ae8`) |
| `despues/claro`, `despues/oscuro`, `despues/letra_1.3` | Estado al terminar |

## Qué se cambió (P0 y P1 aprobados)

| # | Problema (antes) | Cambio | Archivo | Comparar |
|---|---|---|---|---|
| P0-1 | Mapa base: todas las teselas «403 Access blocked» | User-Agent propio `FarmealoTu-Android/<versión>` (OSM bloquea `com.example.*`) | `ui/screens/geo/OsmMap.kt` | `antes/claro/14_mi_zona_1` → `despues/claro/14_mi_zona_1` |
| P0-2 | Títulos de módulo truncados («Comparar formas de s…») | Barra superior propia: título hasta 3 líneas; Volver e Inicio como íconos de 48 dp | `ui/components/Common.kt` | `antes/claro/09_comparar_1`, `13_incertidumbre_1` → `despues/…` |
| P0-3 | Tabla del 02 sin la columna Riesgo (scroll lateral oculto) | Filas de dos líneas que entran en el ancho; descripción para TalkBack | `ui/screens/tradeoffs/TradeoffsScreen.kt` | `antes/claro/11_ventajas_radar_0` → `despues/claro/11_ventajas_tabla` |
| P1-4 | Franja vacía de ~45 dp sobre la barra inferior | `contentWindowInsets = WindowInsets(0)` en `FarmScreen` | `Common.kt` | borde inferior de cualquier captura |
| P1-5 | Sin tema oscuro | Esquema oscuro diseñado; gráficos sobre tarjeta clara (`chartSurface`), **ChartPalette sin cambios** | `ui/theme/Theme.kt`, `Charts.kt`, `MainActivity.kt`, `res/values-night` | `antes/oscuro/*` → `despues/oscuro/*` |
| P1-6 | Emoji como íconos (TalkBack los leía) | `FarmIcons`: trazados de Material Icons como `ImageVector`, sin dependencia nueva | `ui/components/FarmIcons.kt`, NavHost, pantallas | barra inferior |
| P1-7 | Jerarquía plana (Resultado: 1 + 8 botones iguales) | Módulos con `ModuleRow` también en Resultado; pie discreto (`QuietAction`); Inicio sin acciones duplicadas | `DecisionScreen.kt`, `HomeScreen.kt`, `Common.kt` | `08_resultado_2..4` |
| P1-8 | «❓ ¿Qué significa?» desalineado y rojo | Botón alineado al texto con ícono de ayuda verde | `Common.kt` | `10_detalle_1`, `12_riesgos_*` |
| P1-9 | Faltaba el aviso del plan §6 | `DecisionDisclaimer` al pie de Inicio y de los módulos (texto exacto del plan) | `Common.kt`, `HomeScreen.kt` | pie de cualquier módulo |
| P1-10 | Etiquetas de capas cortadas con letra 1.3 | Etiquetas como texto de Compose en franjas; la pila crece con el texto | `ui/screens/geo/GeoViewerScreen.kt` | `antes/letra_1.3/14_mi_zona_capas` → `despues/letra_1.3/14_mi_zona_capas` |
| P1-11 | Radar: «50» tapado, hueco debajo | Centrado por la forma real; anillos explicados en el texto | `Charts.kt`, `TradeoffsScreen.kt` | `11_ventajas_radar` |
| P1-12 | Tabla de riesgos con scroll lateral sin indicio | Dos bloques de 6 meses con la columna Año fija | `Charts.kt` | `antes/claro/12_riesgos_tabla*` → `despues/claro/12_riesgos_tabla` |
| P1-13 | 02 y 04 resaltaban «Más» | «Siembra» resaltada en 01, 02 y 04 (solo el resaltado; los destinos no cambian) | `navigation/FarmealoTuNavHost.kt` | barra inferior en `11_ventajas_1` |
| P1-14 | Etiquetas de la barra inferior apretadas con letra 1.3 | Label Small, una línea | `FarmealoTuNavHost.kt` | `letra_1.3/01_inicio_1` |

También se actualizó `DESIGN.md` y `.impeccable/design.json` para describir el sistema nuevo.

## Verificación

- `.\gradlew.bat assembleDebug` / `installDebug`: **OK**.
- `.\gradlew.bat testDebugUnitTest`: **63/63 en verde** (incluye `AppFlowTest`, el recorrido completo con Robolectric).
  Un cambio de texto («Ver resultado») rompió ese test durante el trabajo; se volvió al texto original.
- Revisado en el teléfono: tema claro, oscuro y letra 1.3. Al terminar se restauraron los valores del teléfono
  (modo oscuro activado, letra 1.0, No molestar apagado, «mantener encendida» = 7).

## Segundo pase: P2

| # | Problema (antes) | Cambio | Archivo | Comparar |
|---|---|---|---|---|
| P2-1 | Datos de terreno: etiquetas «A»/«B» superpuestas y eje 6 · 15.7 · 25.3 · 35 | Marcas redondas (`niceTicks`: pasos de 1, 2, 2,5 o 5 × 10ⁿ) y etiquetas finales que se separan si chocan | `ui/components/Charts.kt` | `antes/claro/15_datos_terreno_3..4` → `despues/claro/15_datos_terreno_humedad*` |
| P2-2 | Escala 0·50·100 repetida bajo cada opción del 04 | La escala se rotula solo en la primera barra; las demás conservan las marcas | `Charts.kt` (`RangeBar`), `UncertaintyScreen.kt` | `13_incertidumbre_1` |
| P2-3 | Leyenda «1 a 6 días / 6 a 11 días» (¿dónde va el 6?) | «1 a 6», «más de 6 a 11», igual que se asigna el color | `ui/components/HeatScale.kt` | `12_riesgos_mapa_calor` |
| P2-4 | Fondo crema y sombras de 1–2 dp | Papel con matiz verde (contrastes revalidados) y profundidad plana; `ChartPalette.surface` fijado para no cambiar la paleta | `ui/theme/Color.kt`, `Theme.kt`, `Common.kt` | cualquier captura |
| P2-5 | Preguntas 3 y 4: glifo «○ / ✓» y sin grupo para TalkBack | `RadioButton` de Material y `selectableGroup()` | `ui/screens/input/FarmerInputScreen.kt` | `despues/claro/04_entrada_p3_seleccion` |
| P2-6 | Fuentes: «318083» | «318 083» con espacio fino, solo en números de 5 o más cifras (los años no cambian) | `Format.kt` (`groupThousands`), `SourcesScreen.kt` | `16_fuentes_contexto` |
| P2-7 | `10.0.2.2` no anda en celular y nada lo decía | Aviso en Conexión cuando la dirección es 10.0.2.2 y la app corre en un celular | `ui/screens/settings/ConnectionScreen.kt` | `17_conexion_1` |

Tests: **63/63 en verde** después del segundo pase.

## Pendiente (no se hizo)

- **Sin probar:** tablet y horizontal, TalkBack de punta a punta (solo revisión de código), rendimiento medido,
  brillo bajo sol real, backend real (no disponible; todo en modo demo).
- `local.properties` quedó con `farmealotu.demoMode=true` (archivo local, fuera de git).

## Lo que no se cambió a propósito

- **Procedencia y escala:** etiquetas documented / derived / assumption / farmer_provided / unavailable, «regional
  (~50 km)» e insignias SIMULADO / WOKWI / FÍSICO: intactas (solo cambió su entorno).
- **Franja MODO DEMO:** mismo color, texto y posición.
- **`ChartPalette`:** ningún valor ni orden cambió; el tema oscuro pone los gráficos sobre tarjeta clara para no
  tener que revalidarla.
- **Contrato con el backend:** sin cambios en DTOs, rutas, `node_id` ni `docs/UI_BACKEND_CONTRACT.md`. Android no
  calcula nada nuevo.
- **Navegación:** mismos destinos y pestañas; solo cambió qué pestaña se resalta en 02 y 04 (aprobado).
- **Texto factual:** sin cambios, salvo el aviso del plan §6 (aprobado) y la explicación de los anillos del radar.
