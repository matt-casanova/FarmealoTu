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

## Pendiente (no se hizo)

- **P2 (no aprobados en este pase):** marcas del eje y etiquetas superpuestas en «Datos de terreno»; eje
  0·50·100 repetido en el módulo 04; leyenda de rangos con bordes compartidos («1 a 6 / 6 a 11»); papel con matiz
  verde y elevación plana tonal (aprobados en `DESIGN.md`, sin aplicar); `selectableGroup()` en las preguntas 3 y 4;
  separador de miles en Fuentes; nota de que `10.0.2.2` solo sirve en emulador.
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
