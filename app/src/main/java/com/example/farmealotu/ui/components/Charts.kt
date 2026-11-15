package com.example.farmealotu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmealotu.data.model.formatNumber
import com.example.farmealotu.domain.HeatmapSeries
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.LocalFarmDarkTheme
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Gráficos en Compose Canvas. Reglas comunes:
 * - Solo se dibuja lo que mandó la API. Un null no es un 0: se muestra como «dato insuficiente».
 * - El color nunca es el único canal: siempre hay número, leyenda o tabla al lado.
 * - Marcas finas, rejilla tenue y un solo eje por gráfico.
 */

private val AxisTextStyle = TextStyle(fontSize = 12.sp, color = ChartPalette.muted)
private val LabelTextStyle = TextStyle(fontSize = 13.sp, color = ChartPalette.ink, fontWeight = FontWeight.Medium)

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    anchor: Offset,
    style: TextStyle = AxisTextStyle,
    alignX: Float = 0f,
    alignY: Float = 0f,
) {
    val layout = measurer.measure(text, style)
    val x = (anchor.x - layout.size.width * alignX).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
    val y = (anchor.y - layout.size.height * alignY).coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
    drawText(layout, topLeft = Offset(x, y))
}

// ---------------------------------------------------------------- barra de puntaje

/** Barra horizontal de un puntaje relativo. Si el valor es null se dice «dato insuficiente» y no se dibuja nada. */
@Composable
fun ScoreBar(
    label: String,
    value: Double?,
    axisMax: Double,
    modifier: Modifier = Modifier,
    color: Color = ChartPalette.series[0],
    emphasized: Boolean = false,
) {
    val decimals = scoreDecimals(axisMax)
    val valueText = value?.let { formatNumber(it, decimals) }
    val description = if (valueText != null) "$label: $valueText de ${formatNumber(axisMax, 0)}" else "$label: dato insuficiente"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(76.dp),
        )
        if (value == null) {
            Text(
                text = "Dato insuficiente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Canvas(
                Modifier
                    .weight(1f)
                    .height(if (emphasized) 18.dp else 14.dp)
            ) {
                val radius = CornerRadius(4.dp.toPx())
                drawRoundRect(ChartPalette.grid, size = size, cornerRadius = radius)
                val fraction = (value / axisMax).coerceIn(0.0, 1.0).toFloat()
                if (fraction > 0f) {
                    drawRoundRect(color, size = Size(size.width * fraction, size.height), cornerRadius = radius)
                    // Extremo de la base recto: la barra «nace» del eje.
                    drawRect(color, size = Size(minOf(radius.x, size.width * fraction), size.height))
                }
            }
            Text(
                text = valueText ?: "",
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.width(48.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- intervalo P10–P90

/** Intervalo horizontal P10–P90 con la marca de P50, sobre el mismo eje para todas las estrategias. */
@Composable
fun RangeBar(
    p10: Double,
    p50: Double?,
    p90: Double,
    axisMax: Double,
    color: Color,
    modifier: Modifier = Modifier,
    /** Números de la escala (0 · mitad · máximo). Con varias barras sobre el mismo eje, basta en una. */
    showScaleLabels: Boolean = true,
) {
    val measurer = rememberTextMeasurer()
    val decimals = scoreDecimals(axisMax)
    val description = "Rango de ${formatNumber(p10, decimals)} a ${formatNumber(p90, decimals)}" +
        (p50?.let { ", valor del medio ${formatNumber(it, decimals)}" } ?: "")
    Canvas(
        modifier
            .chartSurface()
            .fillMaxWidth()
            .height(if (showScaleLabels) 54.dp else 32.dp)
            .semantics { contentDescription = description }
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val trackY = 16.dp.toPx()
        fun x(value: Double): Float = left + (right - left) * (value / axisMax).coerceIn(0.0, 1.0).toFloat()

        // Eje y marcas (0, mitad, máximo).
        drawLine(ChartPalette.axis, Offset(left, trackY), Offset(right, trackY), 1.dp.toPx())
        listOf(0.0, axisMax / 2, axisMax).forEach { tick ->
            drawLine(ChartPalette.axis, Offset(x(tick), trackY - 4.dp.toPx()), Offset(x(tick), trackY + 4.dp.toPx()), 1.dp.toPx())
            if (showScaleLabels) {
                drawLabel(measurer, formatNumber(tick, decimals), Offset(x(tick), trackY + 22.dp.toPx()), alignX = 0.5f)
            }
        }
        // Intervalo.
        val barHeight = 10.dp.toPx()
        val start = x(minOf(p10, p90))
        val end = x(maxOf(p10, p90))
        drawRoundRect(
            color = color,
            topLeft = Offset(start, trackY - barHeight / 2),
            size = Size((end - start).coerceAtLeast(2.dp.toPx()), barHeight),
            cornerRadius = CornerRadius(barHeight / 2),
        )
        // Mediana: punto con anillo del color de fondo para que se lea sobre la barra.
        if (p50 != null) {
            drawCircle(ChartPalette.surface, 8.dp.toPx(), Offset(x(p50), trackY))
            drawCircle(ChartPalette.ink, 5.dp.toPx(), Offset(x(p50), trackY))
        }
    }
}

// ---------------------------------------------------------------- radar

data class RadarSeries(val name: String, val color: Color, val values: List<Double?>)

/**
 * Radar de 3 ejes. Un criterio null NO genera vértice: la figura queda abierta en ese eje
 * y la pantalla lo explica con texto («dato insuficiente»).
 */
@Composable
fun RadarChart(
    axes: List<String>,
    series: List<RadarSeries>,
    axisMax: Double,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val description = "Radar comparativo de ${axes.joinToString(", ")}. Los mismos números están en la tabla de abajo."
    Canvas(
        modifier
            .chartSurface()
            .fillMaxWidth()
            .height(240.dp)
            .semantics { contentDescription = description }
    ) {
        // Se centra la figura real (con 3 ejes es un triángulo que baja solo medio radio),
        // no un círculo imaginario: así no queda un hueco vacío debajo.
        val sines = axes.indices.map { sin(-PI / 2 + 2 * PI * it / axes.size).toFloat() }
        val top = sines.min()
        val bottom = sines.max()
        val labelRoom = 28.dp.toPx()
        val radius = minOf(size.width / 2f - 48.dp.toPx(), (size.height - 2 * labelRoom) / (bottom - top))
        val center = Offset(size.width / 2f, size.height / 2f - radius * (top + bottom) / 2f)
        fun point(axisIndex: Int, fraction: Float): Offset {
            val angle = -PI / 2 + 2 * PI * axisIndex / axes.size
            return Offset(center.x + radius * fraction * cos(angle).toFloat(), center.y + radius * fraction * sin(angle).toFloat())
        }

        // Anillos y ejes, tenues.
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
            val path = Path()
            axes.indices.forEach { index ->
                val p = point(index, ring)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, ChartPalette.grid, style = Stroke(1.dp.toPx()))
        }
        axes.indices.forEach { index ->
            drawLine(ChartPalette.grid, center, point(index, 1f), 1.dp.toPx())
            val labelAnchor = point(index, 1.16f)
            drawLabel(measurer, axes[index], labelAnchor, LabelTextStyle, alignX = 0.5f, alignY = 0.5f)
        }
        // Solo el máximo, junto a la punta superior: los valores intermedios (p. ej. «50») caían justo
        // donde pasan las series y quedaban tapados. Los anillos se explican en el texto del gráfico
        // y los números exactos están en la tabla.
        drawLabel(measurer, formatNumber(axisMax, scoreDecimals(axisMax)), point(0, 1f) + Offset(-10.dp.toPx(), 0f), alignX = 1f, alignY = 0.5f)

        series.forEach { item ->
            val points = axes.indices.map { index ->
                item.values.getOrNull(index)?.let { point(index, (it / axisMax).coerceIn(0.0, 1.0).toFloat()) }
            }
            if (points.all { it != null }) {
                val path = Path()
                points.filterNotNull().forEachIndexed { index, p -> if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                path.close()
                drawPath(path, item.color.copy(alpha = 0.10f))
                drawPath(path, item.color, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
            } else {
                // Solo se unen vértices vecinos que existen; no se inventa el que falta.
                points.indices.forEach { index ->
                    val a = points[index]
                    val b = points[(index + 1) % points.size]
                    if (a != null && b != null && points.size > 2) {
                        drawLine(item.color, a, b, 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            points.filterNotNull().forEach { p ->
                drawCircle(ChartPalette.surface, 6.dp.toPx(), p)
                drawCircle(item.color, 4.dp.toPx(), p)
            }
        }
    }
}

// ---------------------------------------------------------------- mapa de calor meses × años

/** Mapa de calor temporal: columnas = meses, filas = años. Cada celda es tocable. */
@Composable
fun MonthYearHeatmap(
    series: HeatmapSeries,
    scale: HeatScale?,
    selected: Pair<Int, Int>?,
    onSelect: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val years = series.years
    val cellsByKey = remember(series) { series.cells.associateBy { it.year to it.month } }
    val description = "Mapa de calor de ${series.name} por mes y año, de ${years.firstOrNull()} a ${years.lastOrNull()}. " +
        "Tocá una celda para ver el valor, o usá «Ver como tabla»."
    BoxWithConstraints(modifier.chartSurface().fillMaxWidth()) {
        val yearColumn = 44.dp
        val header = 22.dp
        val cellWidth = (maxWidth - yearColumn) / 12
        val cellHeight = 30.dp
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(header + cellHeight * years.size)
                .semantics { contentDescription = description }
                .pointerInput(series) {
                    detectTapGestures { tap ->
                        val column = floor((tap.x - yearColumn.toPx()) / cellWidth.toPx()).toInt()
                        val row = floor((tap.y - header.toPx()) / cellHeight.toPx()).toInt()
                        if (column in 0..11 && row in years.indices) onSelect(years[row], column + 1)
                    }
                }
        ) {
            val gap = 2.dp.toPx()
            val originX = yearColumn.toPx()
            val originY = header.toPx()
            val w = cellWidth.toPx()
            val h = cellHeight.toPx()
            MONTH_INITIALS.forEachIndexed { index, initial ->
                drawLabel(measurer, initial, Offset(originX + w * index + w / 2, originY / 2), alignX = 0.5f, alignY = 0.5f)
            }
            years.forEachIndexed { row, year ->
                drawLabel(measurer, year.toString(), Offset(0f, originY + h * row + h / 2), alignY = 0.5f)
                for (month in 1..12) {
                    val cell = cellsByKey[year to month]
                    val value = cell?.value
                    val topLeft = Offset(originX + w * (month - 1) + gap / 2, originY + h * row + gap / 2)
                    val cellSize = Size(w - gap, h - gap)
                    val corner = CornerRadius(3.dp.toPx())
                    if (value != null && scale != null) {
                        drawRoundRect(scale.colorFor(value), topLeft, cellSize, corner)
                    } else {
                        // Sin dato: celda neutra con una raya. No se pinta como si fuera un valor.
                        drawRoundRect(ChartPalette.noData, topLeft, cellSize, corner)
                        drawLine(
                            ChartPalette.axis,
                            Offset(topLeft.x + cellSize.width * 0.3f, topLeft.y + cellSize.height / 2),
                            Offset(topLeft.x + cellSize.width * 0.7f, topLeft.y + cellSize.height / 2),
                            1.5.dp.toPx(),
                        )
                    }
                    if (selected == (year to month)) {
                        drawRoundRect(ChartPalette.ink, topLeft, cellSize, corner, style = Stroke(2.5.dp.toPx()))
                    }
                }
            }
        }
    }
}

/**
 * Los mismos datos del mapa de calor como tabla (alternativa accesible, sin depender del color).
 * Dos bloques de 6 meses para que entre en el ancho de un teléfono sin desplazarse de costado
 * y sin perder la columna del año.
 */
@Composable
fun HeatmapTable(series: HeatmapSeries, modifier: Modifier = Modifier) {
    val cellsByKey = remember(series) { series.cells.associateBy { it.year to it.month } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(1..6, 7..12).forEach { months ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Año", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                    months.forEach { month ->
                        Text(
                            MONTH_NAMES[month - 1].take(3),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                series.years.forEach { year ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(year.toString(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.3f))
                        months.forEach { month ->
                            val value = cellsByKey[year to month]?.value
                            Text(
                                text = value?.let { formatNumber(it, 1) } ?: "s/d",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        Text("s/d = sin dato", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * En tema oscuro los gráficos se dibujan sobre una tarjeta clara con el fondo de [ChartPalette]:
 * así la paleta validada (tinta, rejilla, ejes) se usa tal cual y no hay que revalidarla.
 * En tema claro no cambia nada (el fondo del gráfico ya es el de la tarjeta).
 */
@Composable
fun Modifier.chartSurface(): Modifier =
    if (LocalFarmDarkTheme.current) {
        this
            .background(ChartPalette.surface, RoundedCornerShape(12.dp))
            .padding(8.dp)
    } else {
        this
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeatLegend(scale: HeatScale, unit: String?, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        scale.legend(unit).forEach { (color, label) -> LegendSwatch(color, label) }
        LegendSwatch(ChartPalette.noData, "Sin dato (–)")
    }
}

// ---------------------------------------------------------------- series de tiempo

data class LinePoint(val epochMillis: Long, val value: Double?)

data class LineSeries(val name: String, val color: Color, val points: List<LinePoint>)

/**
 * Líneas en el tiempo con UN solo eje vertical. Los null cortan la línea (no se interpolan).
 * Tocar el gráfico marca la lectura más cercana; [onSelect] la informa a la pantalla.
 */
@Composable
fun TimeSeriesChart(
    series: List<LineSeries>,
    unit: String,
    selectedTime: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 1,
    timeLabel: (Long) -> String = ::formatTimeOfDay,
) {
    val measurer = rememberTextMeasurer()
    val allPoints = series.flatMap { it.points }
    val values = allPoints.mapNotNull { it.value }
    val times = allPoints.map { it.epochMillis }.distinct().sorted()
    if (values.isEmpty() || times.isEmpty()) {
        Text("No hay valores válidos para graficar.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    // Eje con marcas redondas (p. ej. 10 · 20 · 30 · 40), no cortes arbitrarios como 15.7 o 25.3.
    val ticks = niceTicks(values.min(), values.max())
    val yMin = ticks.first()
    val yMax = ticks.last()
    val tickDecimals = if (ticks.size > 1 && (ticks[1] - ticks[0]) % 1.0 != 0.0) 1 else 0
    val tMin = times.first()
    val tMax = times.last()
    val description = "Gráfico de líneas en $unit: " + series.joinToString("; ") { item ->
        val last = item.points.lastOrNull { it.value != null }?.value
        "${item.name}, último valor ${last?.let { formatNumber(it, decimals) } ?: "sin dato"}"
    }

    Canvas(
        modifier
            .chartSurface()
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description }
            .pointerInput(times) {
                detectTapGestures { tap ->
                    val left = 44.dp.toPx()
                    val right = size.width - 30.dp.toPx()
                    val fraction = ((tap.x - left) / (right - left)).coerceIn(0f, 1f)
                    val target = tMin + ((tMax - tMin) * fraction).toLong()
                    times.minByOrNull { kotlin.math.abs(it - target) }?.let(onSelect)
                }
            }
    ) {
        val left = 44.dp.toPx()
        val right = size.width - 30.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        fun x(time: Long): Float = if (tMax == tMin) (left + right) / 2 else left + (right - left) * ((time - tMin).toFloat() / (tMax - tMin).toFloat())
        fun y(value: Double): Float = bottom - (bottom - top) * ((value - yMin) / (yMax - yMin)).toFloat()

        // Rejilla horizontal tenue + etiquetas del eje (un solo eje).
        ticks.forEach { value ->
            drawLine(ChartPalette.grid, Offset(left, y(value)), Offset(right, y(value)), 1.dp.toPx())
            drawLabel(measurer, formatNumber(value, tickDecimals), Offset(left - 6.dp.toPx(), y(value)), alignX = 1f, alignY = 0.5f)
        }

        // Etiquetas finales (A, B, C): si dos series terminan cerca, se separan verticalmente
        // en vez de dibujarse una encima de otra. El punto queda donde está el dato.
        val labelGap = 16.dp.toPx()
        val endLabelY = series.mapNotNull { item ->
            item.points.sortedBy { it.epochMillis }.lastOrNull { it.value != null }?.let { item.name to y(it.value!!) }
        }.sortedBy { it.second }.toMutableList()
        for (index in 1 until endLabelY.size) {
            val (name, wanted) = endLabelY[index]
            val minimum = endLabelY[index - 1].second + labelGap
            if (wanted < minimum) endLabelY[index] = name to minimum
        }
        val labelYByName = endLabelY.toMap()
        drawLine(ChartPalette.axis, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
        drawLabel(measurer, timeLabel(tMin), Offset(left, bottom + 4.dp.toPx()))
        if (tMax != tMin) drawLabel(measurer, timeLabel(tMax), Offset(right, bottom + 4.dp.toPx()), alignX = 1f)

        // Lectura seleccionada.
        if (selectedTime != null && selectedTime in tMin..tMax) {
            drawLine(ChartPalette.muted, Offset(x(selectedTime), top), Offset(x(selectedTime), bottom), 1.dp.toPx())
        }

        series.forEach { item ->
            val path = Path()
            var penDown = false
            item.points.sortedBy { it.epochMillis }.forEach { point ->
                val value = point.value
                if (value == null) {
                    penDown = false
                } else {
                    if (penDown) path.lineTo(x(point.epochMillis), y(value)) else path.moveTo(x(point.epochMillis), y(value))
                    penDown = true
                }
            }
            drawPath(path, item.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Punto final + etiqueta directa con el nombre de la serie.
            item.points.sortedBy { it.epochMillis }.lastOrNull { it.value != null }?.let { last ->
                val end = Offset(x(last.epochMillis), y(last.value!!))
                drawCircle(ChartPalette.surface, 6.dp.toPx(), end)
                drawCircle(item.color, 4.dp.toPx(), end)
                val labelY = labelYByName[item.name] ?: end.y
                drawLabel(measurer, item.name, Offset(end.x + 9.dp.toPx(), labelY), LabelTextStyle, alignY = 0.5f)
            }
            if (selectedTime != null) {
                item.points.firstOrNull { it.epochMillis == selectedTime }?.value?.let { value ->
                    val at = Offset(x(selectedTime), y(value))
                    drawCircle(ChartPalette.surface, 6.dp.toPx(), at)
                    drawCircle(item.color, 4.dp.toPx(), at)
                }
            }
        }
    }
}

/**
 * Marcas de eje «redondas» (paso de 1, 2, 2,5 o 5 × 10ⁿ) que cubren [min, max] con 3 a 5 divisiones.
 * Solo decide dónde van las líneas de referencia; los datos se dibujan con su valor exacto.
 */
internal fun niceTicks(min: Double, max: Double, target: Int = 4): List<Double> {
    val low = if (max > min) min else min - 1.0
    val high = if (max > min) max else max + 1.0
    val rough = (high - low) / target
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rough }
    val start = floor(low / step) * step
    val end = ceil(high / step) * step
    val count = ((end - start) / step).roundToInt()
    return (0..count).map { start + step * it }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeriesLegend(items: List<Pair<Color, String>>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (color, label) -> LegendSwatch(color, label, Modifier.padding(vertical = 2.dp)) }
    }
}
