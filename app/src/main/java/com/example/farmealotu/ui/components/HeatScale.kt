package com.example.farmealotu.ui.components

import androidx.compose.ui.graphics.Color
import com.example.farmealotu.data.model.formatNumber
import com.example.farmealotu.ui.theme.ChartPalette
import kotlin.math.abs
import kotlin.math.max

/**
 * Escala de color del mapa de calor, construida SOLO con los valores recibidos.
 * - Magnitudes (todo >= 0 o todo <= 0): un tono, de claro (poco) a oscuro (mucho).
 * - Valores a ambos lados de cero (p. ej. anomalía de lluvia): dos tonos con gris neutro al medio.
 * Los null no entran en la escala: se dibujan como «sin dato».
 */
class HeatScale private constructor(
    val diverging: Boolean,
    private val edges: List<Double>,
    val colors: List<Color>,
) {
    val binCount: Int get() = colors.size

    /** Índice del rango al que pertenece el valor (0 = el más bajo). */
    fun binIndex(value: Double): Int {
        for (index in 0 until binCount) {
            if (value <= edges[index + 1]) return index
        }
        return binCount - 1
    }

    fun colorFor(value: Double): Color = colors[binIndex(value)]

    /** Texto del color que va sobre la celda: blanco en los tonos oscuros. */
    fun inkFor(value: Double): Color {
        val index = binIndex(value)
        val dark = if (diverging) index == 0 || index == binCount - 1 else index >= binCount - 2
        return if (dark) Color.White else ChartPalette.ink
    }

    /**
     * Rangos sin bordes compartidos, igual que [binIndex]: el primero incluye ambos extremos y los
     * siguientes empiezan «más de» el límite anterior («1 a 6 días», «más de 6 a 11 días»…).
     */
    fun legend(unit: String?): List<Pair<Color, String>> = colors.mapIndexed { index, color ->
        val suffix = unit?.let { " $it" } ?: ""
        val low = formatNumber(edges[index], 1)
        val high = formatNumber(edges.getOrElse(index + 1) { edges[index] }, 1)
        val label = when {
            low == high -> "$low$suffix"
            index == 0 -> "$low a $high$suffix"
            else -> "más de $low a $high$suffix"
        }
        color to label
    }

    companion object {
        fun from(values: List<Double>): HeatScale? {
            if (values.isEmpty()) return null
            val min = values.min()
            val maxValue = values.max()
            if (min < 0 && maxValue > 0) {
                val limit = max(abs(min), abs(maxValue))
                val edges = listOf(-limit, -0.6 * limit, -0.2 * limit, 0.2 * limit, 0.6 * limit, limit)
                return HeatScale(true, edges, ChartPalette.diverging)
            }
            if (min == maxValue) {
                return HeatScale(false, listOf(min, maxValue), listOf(ChartPalette.sequential[2]))
            }
            val step = (maxValue - min) / ChartPalette.sequential.size
            val edges = List(ChartPalette.sequential.size + 1) { index ->
                if (index == ChartPalette.sequential.size) maxValue else min + step * index
            }
            return HeatScale(false, edges, ChartPalette.sequential)
        }
    }
}
