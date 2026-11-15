package com.example.farmealotu.ui.screens.risks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.domain.HeatmapSeries
import com.example.farmealotu.domain.IndicatorSummary
import com.example.farmealotu.domain.IndicatorsResult
import com.example.farmealotu.domain.LabeledValue
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.GlossaryEntry
import com.example.farmealotu.ui.components.HeatLegend
import com.example.farmealotu.ui.components.HeatScale
import com.example.farmealotu.ui.components.HeatmapTable
import com.example.farmealotu.ui.components.InfoRow
import com.example.farmealotu.ui.components.Level
import com.example.farmealotu.ui.components.LevelBadge
import com.example.farmealotu.ui.components.MONTH_NAMES
import com.example.farmealotu.ui.components.MonthYearHeatmap
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.FoldRow
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.FoldCard
import com.example.farmealotu.ui.components.StateContent
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatOrMissing
import com.example.farmealotu.ui.components.provenanceLabel
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.traceDetails
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel

private fun IndicatorSummary.isCoverIndicator(): Boolean =
    listOf("ndvi", "cover", "cobertura").any { it in id.lowercase() }

/**
 * Módulo 03 — «Conocer los riesgos».
 */
@Composable
fun RisksScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
) {
    val indicators by viewModel.indicators.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIndicators() }

    FarmScreen(title = "Conocer los riesgos", subtitle = "Módulo 03", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
            Text(
                "Clima de la zona en años pasados. No es un pronóstico.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Lluvia intensa no es un pronóstico de inundación.",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            )
            ExplainButton(Glossary.regional, label = "¿De dónde salen estos datos?")
        }

        StateContent(
            state = indicators,
            onRetry = { viewModel.loadIndicators(force = true) },
            loadingText = "Consultando los indicadores de la zona…",
        ) { state ->
            val result = state.data
            FreshnessNote(state, onRetry = { viewModel.loadIndicators(force = true) })
            result.lastObservationDate?.let {
                Text("Última observación disponible: ${formatApiDate(it)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            SummaryCards(result)
            HeatmapSection(result)
        }

        FooterActions(onEditData = onEditData, onHome = onHome)
    }
}

@Composable
private fun SummaryCards(result: IndicatorsResult) {
    val (cover, others) = result.summaries.partition { it.isCoverIndicator() }
    if (others.isEmpty() && cover.isEmpty()) {
        SectionCard {
            Text("Sin resumen.", style = MaterialTheme.typography.bodyMedium)
        }
    }
    others.forEach { IndicatorCard(it) }
    val verifiedCover = cover.filter { it.valueText != null && it.status?.lowercase() != "unavailable" }
    verifiedCover.forEach { IndicatorCard(it) }
    if (verifiedCover.isEmpty()) {
        FoldCard(title = "Baja cobertura del suelo", containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            ProvenanceChip("unavailable")
            Text(
                "Sin datos de vegetación (NDVI) para este punto.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ExplainButton(Glossary.ndvi)
        }
    }
}

@Composable
private fun IndicatorCard(summary: IndicatorSummary) {
    SectionCard(title = summary.name) {
        Text(
            text = summary.valueText?.let { value -> value + (summary.unit?.let { " $it" } ?: "") } ?: "No disponible",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Level.from(summary.level)?.let { LevelBadge(it, prefix = "Exposición") }
        FoldRow("Ver detalles") {
        summary.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        ProvenanceChip(summary.status)
        InfoRow("Período", formatApiDate(summary.period) ?: "no informado")
        InfoRow("Fuente", summary.source ?: "no informada")
        InfoRow("Escala", summary.scale ?: "no informada")
        if (summary.validDays != null || summary.missingPct != null) {
            InfoRow(
                "Calidad",
                listOfNotNull(
                    summary.validDays?.let { "$it días válidos" },
                    summary.missingPct?.let { "${formatOrMissing(it, 1)} % faltante" },
                ).joinToString(" · "),
            )
        }
        ExplainButton(
            entry = Glossary.forIndicatorId(summary.id) ?: GlossaryEntry(summary.name, summary.description ?: "Calculado con datos de NASA POWER."),
            details = traceDetails(summary.source, summary.period, summary.unit, summary.scale, summary.status),
        )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeatmapSection(result: IndicatorsResult) {
    val grids = result.grids.filter { it.cells.isNotEmpty() }
    if (grids.isEmpty()) {
        SectionCard(title = "Riesgo por mes y año", containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            ProvenanceChip("unavailable")
            Text(
                "Sin mapa de calor para este dato.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    var selectedId by rememberSaveable { mutableStateOf(grids.first().id) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    val series = grids.firstOrNull { it.id == selectedId } ?: grids.first()
    val scale = remember(series) { HeatScale.from(series.cells.mapNotNull { it.value }) }

    SectionCard(title = "Riesgo por mes y año") {
        Text("Elegí qué indicador querés mirar:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            grids.forEach { grid ->
                FilterChip(
                    selected = grid.id == series.id,
                    onClick = {
                        selectedId = grid.id
                        selectedYear = null
                        selectedMonth = null
                    },
                    label = { Text(grid.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        Text(
            text = series.name + (series.unit?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        series.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(
            "Cada cuadro es un mes de un año. Tocalo para ver el valor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (scale == null) {
            ProvenanceChip("unavailable")
            Text("Sin valores.", style = MaterialTheme.typography.bodyMedium)
        } else {
            MonthYearHeatmap(
                series = series,
                scale = scale,
                selected = selectedYear?.let { year -> selectedMonth?.let { month -> year to month } },
                onSelect = { year, month ->
                    selectedYear = year
                    selectedMonth = month
                },
            )
            Text(
                text = if (scale.diverging) "Rangos (rojo: menos que lo habitual · azul: más que lo habitual)"
                else "Rangos (más oscuro = valor más alto)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            HeatLegend(scale, series.unit)
        }

        CellDetail(series, selectedYear, selectedMonth)

        SecondaryButton(if (showTable) "Ocultar tabla" else "Ver como tabla", { showTable = !showTable })
        if (showTable) HeatmapTable(series)

        ExplainButton(
            entry = Glossary.forIndicatorId(series.id) ?: GlossaryEntry(series.name, series.description ?: "Valor mensual."),
            details = traceDetails(series.source, "${series.years.firstOrNull() ?: "?"}–${series.years.lastOrNull() ?: "?"}", series.unit, series.scale, series.status),
        )
    }
}

@Composable
private fun CellDetail(series: HeatmapSeries, year: Int?, month: Int?) {
    SectionCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (year == null || month == null) {
            Text("Tocá un cuadro para ver el mes.", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        val cell = series.cell(year, month)
        Text("${MONTH_NAMES[month - 1].replaceFirstChar { it.uppercase() }} de $year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val details = listOf(
            LabeledValue("Valor", formatOrMissing(cell?.value, 1, series.unit, missing = "sin dato para ese mes")),
            LabeledValue("Indicador", series.name),
            LabeledValue("Fuente", series.source ?: "no informada"),
            LabeledValue(
                "Calidad",
                listOfNotNull(
                    cell?.quality,
                    cell?.validDays?.let { "$it días válidos" },
                    cell?.missingPct?.let { "${formatOrMissing(it, 1)} % faltante" },
                ).joinToString(" · ").ifEmpty { "no informada" },
            ),
            LabeledValue("Alcance espacial", series.scale ?: "no informado"),
            LabeledValue("Procedencia", provenanceLabel(series.status)),
        )
        details.forEach { InfoRow(it.label, it.value) }
    }
}

@Preview(showBackground = true, name = "Módulo 03 - Conocer los Riesgos")
@Composable
fun RisksScreenPreview() {
    FarmealoTuTheme {
        RisksScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
        )
    }
}
