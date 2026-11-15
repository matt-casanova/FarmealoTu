package com.example.farmealotu.ui.screens.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmealotu.domain.FieldReadings
import com.example.farmealotu.domain.MoistureKind
import com.example.farmealotu.domain.NodeReading
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.NodeStatus
import com.example.farmealotu.domain.ZoneReading
import com.example.farmealotu.domain.moistureKindFor
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.EmptyBlock
import com.example.farmealotu.ui.components.ErrorBlock
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.InfoRow
import com.example.farmealotu.ui.components.LinePoint
import com.example.farmealotu.ui.components.LineSeries
import com.example.farmealotu.ui.components.LoadingBlock
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.SeriesLegend
import com.example.farmealotu.ui.components.TimeSeriesChart
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatClock
import com.example.farmealotu.ui.components.formatOrMissing
import com.example.farmealotu.ui.components.formatSimClock
import com.example.farmealotu.ui.components.formatTimeOfDay
import com.example.farmealotu.ui.components.sampleFieldDataViewModel
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.AppViewModelFactory
import com.example.farmealotu.viewmodel.FieldDataViewModel
import com.example.farmealotu.viewmodel.UiState
import com.example.farmealotu.viewmodel.dataOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val REFRESH_MILLIS = 15_000L
private const val MAX_CHART_POINTS = 240

private data class ZoneInfo(val id: String, val title: String, val color: Color)

private val ZONES = listOf(
    ZoneInfo("A", "Zona A · seca", ChartPalette.series[0]),
    ZoneInfo("B", "Zona B · media", ChartPalette.series[1]),
    ZoneInfo("C", "Zona C · húmeda", ChartPalette.series[2]),
)

fun calibrationLabel(method: String?, flags: List<String>): String = when {
    flags.any { it.contains("uncalibrated", ignoreCase = true) } ->
        "Humedad SIN calibrar: el valor es relativo, no un % volumétrico real"
    method == null -> "Calibración no informada: tomá la humedad como valor orientativo"
    method.equals("gravimetric_3pt", ignoreCase = true) -> "Humedad calibrada con 3 puntos (método gravimétrico)"
    else -> "Calibración informada por el nodo: $method"
}

/** Texto principal de humedad de una zona, por prioridad: VWC → índice del sensor → lectura cruda → sin dato. */
internal fun moistureHeadline(zone: ZoneReading): String = when (zone.moistureKind) {
    // Espacio duro (U+00A0) entre número y «%»: el salto de línea nunca deja el «%» solo en otra línea.
    MoistureKind.VWC -> "Humedad del suelo: " + formatOrMissing(zone.moisturePct, 1, " %").replace("  ", " ")
    MoistureKind.INDEX -> "Índice de humedad del sensor: " + formatOrMissing(zone.moistureIndexPct, 0, " %").replace("  ", " ") + " (relativo, sin calibrar)"
    MoistureKind.RAW -> "Lectura cruda del sensor: " + formatOrMissing(zone.moistureRaw, 0) + " (sin calibrar)"
    null -> "Humedad del suelo: sin dato"
}

/** Valor de humedad de un tipo ya elegido (gráfico, lectura seleccionada, tabla). Null → [missing], nunca 0. */
internal fun formatMoisture(value: Double?, kind: MoistureKind, missing: String = "sin dato"): String = when (kind) {
    MoistureKind.VWC -> formatOrMissing(value, 1, "%", missing = missing)
    MoistureKind.INDEX -> formatOrMissing(value, 0, missing = missing)
    MoistureKind.RAW -> formatOrMissing(value, 0, missing = missing)
}

private data class MoistureChartLabels(val title: String, val unit: String, val column: String)

private fun MoistureKind.labels(): MoistureChartLabels = when (this) {
    MoistureKind.VWC -> MoistureChartLabels("Humedad del suelo por zona (%)", "% volumétrico", "hum. %")
    MoistureKind.INDEX -> MoistureChartLabels("Índice del sensor (0–100) por zona", "índice relativo del sensor (0–100), sin calibrar", "índice")
    MoistureKind.RAW -> MoistureChartLabels("Lectura cruda del sensor de humedad", "lectura cruda, sin calibrar", "crudo")
}

/**
 * «Datos de terreno»: lecturas del nodo ESP32 de 3 zonas.
 */
@Composable
fun FieldDataScreen(
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: FieldDataViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewModel.refresh()
                delay(REFRESH_MILLIS)
            }
        }
    }

    FarmScreen(title = "Datos de terreno", subtitle = "Nodo de campo de 3 zonas", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        SectionCard(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        ) {
            Text("Estos datos no cambian la recomendación en esta versión.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Muestran cómo varía un campo de un punto a otro: algo que el satélite, que mira zonas de kilómetros, promedia.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ExplainButton(Glossary.satelliteVsGround, label = "Satélite y sensor: ¿en qué se diferencian?")
        }

        when (val state = readings) {
            UiState.Idle, UiState.Loading -> LoadingBlock("Consultando el nodo de campo…")
            is UiState.Error -> {
                ErrorBlock(state.failure, onRetry = viewModel::refresh)
                StatusCard(status.dataOrNull, latest = null)
            }
            UiState.Empty -> {
                EmptyBlock(
                    title = "Todavía no hay lecturas del nodo",
                    message = "El servidor respondió, pero no tiene lecturas guardadas.",
                ) { SecondaryButton("Volver a consultar", viewModel::refresh) }
                StatusCard(status.dataOrNull, latest = null)
            }
            is UiState.Success -> {
                FreshnessNote(state, onRetry = viewModel::refresh)
                ReadingsContent(state.data, status.dataOrNull)
                SecondaryButton("Actualizar ahora", viewModel::refresh)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingsContent(data: FieldReadings, status: NodeStatus?) {
    val latestOverall = data.readings.last()
    val sources = data.readings.map { it.source }.distinct()
    var chosenCode by rememberSaveable { mutableStateOf<String?>(null) }
    val source = sources.firstOrNull { it.name == chosenCode } ?: latestOverall.source
    val sameSource = data.readings.filter { it.source == source }
    val latest = sameSource.last()

    SourceBadge(source, latest.scenario, latest.simEpochMillis)

    if (sources.size > 1) {
        SectionCard(title = "Hay lecturas de más de un origen") {
            Text("Se muestran por separado para no mezclar simulación con medición.", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sources.forEach { item ->
                    FilterChip(
                        selected = item == source,
                        onClick = { chosenCode = item.name },
                        label = { Text(item.badge, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }

    SectionCard(title = "Calibración") {
        Text(calibrationLabel(latest.calibrationMethod, latest.qualityFlags + latest.zones.flatMap { it.flags }), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        InfoRow("Fecha de calibración", formatApiDate(latest.calibrationDate) ?: "no informada")
        InfoRow("Profundidad de sensores", formatOrMissing(latest.depthCm, 0, "cm", missing = "no informada"))
        ExplainButton(Glossary.vwc)
    }

    ZONES.forEach { zone -> ZoneCard(zone, latest, source) }
    val extraZones = latest.zones.filter { reading -> ZONES.none { it.id.equals(reading.id, ignoreCase = true) } }
    if (extraZones.isNotEmpty()) {
        Text("El nodo envió además las zonas: ${extraZones.joinToString { it.id }}.", style = MaterialTheme.typography.bodySmall)
    }

    Charts(sameSource.takeLast(MAX_CHART_POINTS), source)
    AirCard(latest, source)
    RegionalBand(status)
    StatusCard(status, latest)

    val flags = sameSource.flatMap { reading -> reading.qualityFlags + reading.zones.flatMap { it.flags } }.distinct()
    if (flags.isNotEmpty() || data.skipped > 0) {
        SectionCard(title = "Avisos de calidad") {
            BulletList(flags.map { "Bandera del nodo: $it" })
            if (data.skipped > 0) {
                Text("${data.skipped} lectura(s) llegaron con formato inválido.", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Un dato faltante se muestra como «sin dato».", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SourceBadge(source: NodeSource, scenario: String?, simMillis: Long?) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (source) {
        NodeSource.PHYSICAL -> scheme.primary to scheme.onPrimary
        NodeSource.SIMULATED, NodeSource.WOKWI -> scheme.tertiary to scheme.onTertiary
        NodeSource.UNKNOWN -> scheme.error to scheme.onError
    }
    Surface(
        color = container, contentColor = content, shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Origen de los datos: ${source.badge}. ${source.description}" },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("ORIGEN: ${source.badge}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(source.description, style = MaterialTheme.typography.bodyMedium)
            if (scenario != null) Text("Escenario de simulación: $scenario", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (simMillis != null) Text("Hora simulada: ${formatSimClock(simMillis)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ZoneCard(zone: ZoneInfo, latest: NodeReading, source: NodeSource) {
    val reading = latest.zone(zone.id)
    SectionCard(title = zone.title) {
        Text(source.badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        if (reading == null) {
            Text("Sin dato de esta zona en la última lectura.", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        Text(
            text = moistureHeadline(reading),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        val secondary = MaterialTheme.colorScheme.onSurfaceVariant
        when (reading.moistureKind) {
            MoistureKind.VWC -> reading.moistureVwc?.let {
                Text("(${formatOrMissing(it, 3)} m³/m³, tal como lo envía el nodo)", style = MaterialTheme.typography.bodySmall, color = secondary)
            }
            MoistureKind.INDEX -> {
                reading.moistureRaw?.let {
                    Text("lectura cruda ${formatOrMissing(it, 0)}", style = MaterialTheme.typography.bodySmall, color = secondary)
                }
                Text(Glossary.moistureIndex.body, style = MaterialTheme.typography.bodySmall, color = secondary)
                ExplainButton(Glossary.moistureIndex)
            }
            MoistureKind.RAW, null -> Unit
        }
        Text(
            text = "Temperatura del suelo: " + formatOrMissing(reading.soilTempC, 1, "°C"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (latest.simEpochMillis != null) {
            InfoRow("Hora simulada", formatSimClock(latest.simEpochMillis))
            InfoRow("Recibida por el servidor", formatApiDate(latest.timestamp) ?: "no informada")
        } else {
            InfoRow("Hora de la lectura", formatApiDate(latest.timestamp) ?: "no informada")
        }
        if (reading.flags.isNotEmpty()) {
            Text("Avisos: ${reading.flags.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Charts(readings: List<NodeReading>, source: NodeSource) {
    // Si el origen trae hora simulada, el eje usa esa hora (y solo las lecturas que la tienen): el gemelo
    // manda muchas horas simuladas en pocos segundos reales y por la hora del servidor se amontonarían.
    val simulatedClock = readings.any { it.simEpochMillis != null }
    val timed = readings.filter { (if (simulatedClock) it.simEpochMillis else it.epochMillis) != null }
        .sortedBy { it.chartMillis }
    val clock: (Long) -> String = if (simulatedClock) ::formatSimClock else ::formatClock
    if (timed.size < 2) {
        SectionCard(title = "Evolución en el tiempo") {
            Text("Hacen falta al menos dos lecturas con hora para dibujar las curvas.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    var selectedTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    val selected = timed.firstOrNull { it.chartMillis == selectedTime }
    val moistureKind = moistureKindFor(timed)

    fun series(value: (NodeReading, String) -> Double?): List<LineSeries> = ZONES.map { zone ->
        LineSeries(zone.id, zone.color, timed.map { LinePoint(it.chartMillis!!, value(it, zone.id)) })
    }

    if (moistureKind == null) {
        SectionCard(title = "Humedad del suelo por zona") {
            Text(source.badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("Este origen no envió ningún dato de humedad del suelo.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        val labels = moistureKind.labels()
        SectionCard(title = labels.title) {
            Text(source.badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            SeriesLegend(ZONES.map { it.color to it.title })
            TimeSeriesChart(
                series = series { reading, id -> reading.zone(id)?.moistureValue(moistureKind) },
                unit = labels.unit,
                selectedTime = selectedTime,
                onSelect = { selectedTime = it },
                timeLabel = if (simulatedClock) ::formatSimClock else ::formatTimeOfDay,
            )
            if (moistureKind == MoistureKind.INDEX) {
                Text("Índice relativo del sensor: no es el % real de agua del suelo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SelectedReadout(selected, clock) { reading, id -> formatMoisture(reading.zone(id)?.moistureValue(moistureKind), moistureKind) }
        }
    }

    SectionCard(title = "Temperatura del suelo por zona (°C)") {
        Text(source.badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        SeriesLegend(ZONES.map { it.color to it.title })
        TimeSeriesChart(
            series = series { reading, id -> reading.zone(id)?.soilTempC },
            unit = "grados Celsius",
            selectedTime = selectedTime,
            onSelect = { selectedTime = it },
            timeLabel = if (simulatedClock) ::formatSimClock else ::formatTimeOfDay,
        )
        SelectedReadout(selected, clock) { reading, id -> formatOrMissing(reading.zone(id)?.soilTempC, 1, "°C") }
    }

    SecondaryButton(if (showTable) "Ocultar tabla de lecturas" else "Ver lecturas como tabla", { showTable = !showTable })
    if (showTable) ReadingsTable(timed.takeLast(20).reversed(), moistureKind, simulatedClock, clock)
}

@Composable
private fun SelectedReadout(selected: NodeReading?, clock: (Long) -> String, value: (NodeReading, String) -> String) {
    val time = selected?.chartMillis
    Text(
        text = if (selected == null || time == null) "Tocá el gráfico para ver los valores de un momento."
        else "${clock(time)} — " + ZONES.joinToString(" · ") { "${it.id}: ${value(selected, it.id)}" },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ReadingsTable(readings: List<NodeReading>, moistureKind: MoistureKind?, simulatedClock: Boolean, clock: (Long) -> String) {
    val moistureColumn = moistureKind?.labels()?.column ?: "hum."
    SectionCard(title = "Últimas ${readings.size} lecturas") {
        Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(if (simulatedClock) "Hora simulada" else "Hora", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                ZONES.forEach { zone ->
                    Text("${zone.id} $moistureColumn", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(84.dp))
                    Text("${zone.id} temp. °C", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(92.dp))
                }
            }
            readings.forEach { reading ->
                Row {
                    Text(reading.chartMillis?.let(clock) ?: "s/d", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(140.dp))
                    ZONES.forEach { zone ->
                        val item = reading.zone(zone.id)
                        val moisture = moistureKind?.let { formatMoisture(item?.moistureValue(it), it, missing = "s/d") } ?: "s/d"
                        Text(moisture, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(84.dp))
                        Text(formatOrMissing(item?.soilTempC, 1, missing = "s/d"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(92.dp))
                    }
                }
            }
            Text("s/d = sin dato", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AirCard(latest: NodeReading, source: NodeSource) {
    SectionCard(title = "Aire y luz (sensores externos)") {
        Text(source.badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text("BME680 (sensor principal)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        InfoRow("Temperatura", formatOrMissing(latest.bmeTempC, 1, "°C"))
        InfoRow("Humedad del aire", formatOrMissing(latest.bmeRhPct, 1, "%"))
        InfoRow("Presión", formatOrMissing(latest.bmePressureHpa, 1, "hPa"))
        InfoRow("Gas (resistencia)", formatOrMissing(latest.bmeGasKohm, 1, "kΩ"))

        Text("DHT11 (segundo sensor de control)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        InfoRow("Temperatura", formatOrMissing(latest.dhtTempC, 1, "°C"))
        InfoRow("Humedad del aire", formatOrMissing(latest.dhtRhPct, 1, "%"))

        Text("Luz (LDR)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        InfoRow("Luz (índice relativo)", formatOrMissing(latest.ldrLightPct, 0, "%", missing = "no disponible"))
        if (latest.ldrLightPct != null) {
            Text("No son lux ni radiación solar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExplainButton(Glossary.lightIndex)
        }
    }
}

@Composable
private fun RegionalBand(status: NodeStatus?) {
    val context = status?.regionalContext
    SectionCard(
        title = "Dato regional (~50 km): contexto, no tu campo",
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (context == null || (context.t2mC == null && context.rh2mPct == null)) {
            Text("El servidor no envió el dato regional de NASA POWER.", style = MaterialTheme.typography.bodyMedium)
        } else {
            InfoRow("Temperatura del aire (regional)", formatOrMissing(context.t2mC, 1, "°C"))
            InfoRow("Humedad del aire (regional)", formatOrMissing(context.rh2mPct, 1, "%"))
            InfoRow("Día", formatApiDate(context.date) ?: "no informado")
            InfoRow("Fuente", context.source ?: "no informada")
        }
        ExplainButton(Glossary.regional)
    }
}

@Composable
private fun StatusCard(status: NodeStatus?, latest: NodeReading?) {
    SectionCard(title = "Estado del nodo") {
        InfoRow("Último contacto", formatApiDate(status?.lastContact ?: latest?.timestamp) ?: "sin registro")
        InfoRow("Nodo", status?.nodeId ?: latest?.nodeId ?: "no informado")
        InfoRow("Origen", status?.source?.badge ?: "no informado")
    }
}

@Preview(showBackground = true, name = "Datos de Terreno (FieldDataScreen)")
@Composable
fun FieldDataScreenPreview() {
    FarmealoTuTheme {
        FieldDataScreen(
            demoMode = true,
            onBack = {},
            onHome = {},
            viewModel = sampleFieldDataViewModel(),
        )
    }
}
