package com.example.farmealotu.ui.theme

import androidx.compose.ui.graphics.Color

// Tema agrícola: fondo claro, verdes de cultivo y tonos tierra. Prioridad: legibilidad al sol.
val FieldGreen = Color(0xFF2E6B34)
val FieldGreenDark = Color(0xFF1B4721)
val FieldGreenContainer = Color(0xFFD9EBD3)
val Earth = Color(0xFF7A4F2A)
val EarthContainer = Color(0xFFF1E2CF)
val Clay = Color(0xFF8A5A00)
val ClayContainer = Color(0xFFFFE9B8)
// Papel con un matiz verde leve (DESIGN.md, «The Green Paper Rule»); antes #F7F4EC.
val Paper = Color(0xFFF3F5EC)
val CardSurface = Color(0xFFFBFCF8)
val CardSurfaceVariant = Color(0xFFE6E9DB)
val Ink = Color(0xFF14180F)
val InkSecondary = Color(0xFF45493F)
val Outline = Color(0xFF7B7F72)
val OutlineSoft = Color(0xFFC6CBB9)
val AlertRed = Color(0xFFA62B22)
val AlertRedContainer = Color(0xFFFBE0DC)

/** Franja «MODO DEMO»: deliberadamente distinta de todo lo demás. */
val DemoStripe = Color(0xFF5B2A86)
val OnDemoStripe = Color(0xFFFFFFFF)

/**
 * Colores de gráficos (paleta validada para daltonismo; ver docs/ANDROID_RUN.md).
 * - series: identidad de E0..E3 o de las zonas A, B, C. Orden fijo, nunca se recicla.
 * - sequential: magnitud, un solo tono de claro a oscuro.
 * - diverging: valores bajo/sobre lo habitual, con gris neutro al medio.
 * - status: reservados para estados; siempre van con forma y texto.
 */
object ChartPalette {
    val series = listOf(Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100))
    val seriesOther = Color(0xFF898781)
    val sequential = listOf(Color(0xFFCDE2FB), Color(0xFF86B6EF), Color(0xFF3987E5), Color(0xFF1C5CAB), Color(0xFF0D366B))
    val diverging = listOf(Color(0xFFD03B3B), Color(0xFFEC9A93), Color(0xFFF0EFEC), Color(0xFF86B6EF), Color(0xFF1C5CAB))
    // Valor fijo de la paleta validada: no sigue a CardSurface si el tema cambia.
    val surface = Color(0xFFFCFCFB)
    val noData = Color(0xFFF0EFEC)
    val grid = Color(0xFFE1E0D9)
    val axis = Color(0xFFC3C2B7)
    val muted = Color(0xFF6F6D67)
    val ink = Color(0xFF0B0B0B)
    val inkSecondary = Color(0xFF52514E)
    val statusGood = Color(0xFF0CA30C)
    val statusWarning = Color(0xFFFAB219)
    val statusSerious = Color(0xFFEC835A)
    val statusCritical = Color(0xFFD03B3B)

    /** El color sigue a la estrategia (E0..E3), no a su posición en el ranking. */
    fun forStrategy(id: String, fallbackIndex: Int): Color {
        val index = id.trim().removePrefix("E").removePrefix("e").toIntOrNull() ?: fallbackIndex
        return series.getOrElse(index) { seriesOther }
    }
}
