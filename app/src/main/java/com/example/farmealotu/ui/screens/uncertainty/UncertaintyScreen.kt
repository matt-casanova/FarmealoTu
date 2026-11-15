package com.example.farmealotu.ui.screens.uncertainty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.data.model.formatNumber
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.ConfidenceDisplay
import com.example.farmealotu.domain.Evidence
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FIXED_LIMITATIONS
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.LegendSwatch
import com.example.farmealotu.ui.components.RangeBar
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatCoordinate
import com.example.farmealotu.ui.components.formatPercentWeight
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.scoreAxisMax
import com.example.farmealotu.ui.components.scoreDecimals
import com.example.farmealotu.ui.screens.compare.shortLabel
import com.example.farmealotu.ui.screens.decision.ComparisonGate
import com.example.farmealotu.ui.screens.decision.OutdatedInputNotice
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel

private const val RANGE_MEANING =
    "Rangos según años pasados; no predicen la cosecha."

private fun Evidence.line(): String = buildString {
    append(indicator)
    append(": ")
    append(valueText?.let { it + (unit?.let { u -> " $u" } ?: "") } ?: "no disponible")
    val trace = listOfNotNull(source, formatApiDate(period), scale).joinToString(" · ")
    if (trace.isNotEmpty()) append(" ($trace)")
}

private fun evidenceWith(result: ComparisonResult, predicate: (Evidence) -> Boolean): List<String> =
    result.strategies.flatMap { it.evidence }.filter(predicate).map { it.line() }.distinct()

private fun Evidence.statusIs(vararg values: String): Boolean = status?.trim()?.lowercase() in values

/**
 * Módulo 04 — «¿Qué tan segura es esta comparación?».
 */
@Composable
fun UncertaintyScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()
    val appliedInput by viewModel.appliedInput.collectAsStateWithLifecycle()
    val appliedPriorities by viewModel.appliedPriorities.collectAsStateWithLifecycle()

    FarmScreen(title = "¿Qué tan segura es esta comparación?", subtitle = "Módulo 04", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        ComparisonGate(compare, onStartInput = onEditData, onRetry = viewModel::submitCompare) { state ->
            val result = state.data
            FreshnessNote(state, onRetry = viewModel::submitCompare)
            OutdatedInputNotice(viewModel)

            RangesCard(result)
            ConfidenceCard(result)
            ProvenanceSections(result, appliedInput, appliedPriorities)

            FooterActions(onEditData = onEditData, onHome = onHome)
        }
    }
}

@Composable
private fun RangesCard(result: ComparisonResult) {
    val withRange = result.strategies.filter { it.uncertainty?.hasRange == true }
    SectionCard(title = "Cuánto puede variar cada opción") {
        Text(RANGE_MEANING, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (withRange.isEmpty()) {
            Text(
                "Sin rangos P10–P90.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@SectionCard
        }
        val axisMax = scoreAxisMax(withRange.flatMap { listOf(it.uncertainty?.p10, it.uncertainty?.p50, it.uncertainty?.p90) })
        val decimals = scoreDecimals(axisMax)
        // La escala 0 · mitad · máximo se rotula una sola vez (en la primera barra): todas comparten el eje.
        val firstWithRange = result.strategies.indexOfFirst { it.uncertainty?.p10 != null && it.uncertainty?.p90 != null }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            result.strategies.forEachIndexed { index, strategy ->
                val uncertainty = strategy.uncertainty
                val color = ChartPalette.forStrategy(strategy.id, index)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LegendSwatch(color, strategy.shortLabel())
                    val p10 = uncertainty?.p10
                    val p90 = uncertainty?.p90
                    if (p10 != null && p90 != null) {
                        RangeBar(
                            p10 = p10, p50 = uncertainty?.p50, p90 = p90, axisMax = axisMax, color = color,
                            showScaleLabels = index == firstWithRange,
                        )
                        Text(
                            text = "La mayoría de las veces queda entre ${formatNumber(p10, decimals)} y ${formatNumber(p90, decimals)}" +
                                (uncertainty?.p50?.let { "; el valor del medio es ${formatNumber(it, decimals)}" } ?: "") + ".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text("Sin rango.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text(
            "Barra: P10 a P90. Punto: valor del medio. Si dos barras se pisan, la diferencia es poco firme.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExplainButton(Glossary.range)
    }
}

@Composable
private fun ConfidenceCard(result: ComparisonResult) {
    val informed = result.strategies.filter { it.uncertainty?.rankStability != null || it.uncertainty?.confidenceLevel != null }
    if (informed.isEmpty()) return
    SectionCard(title = "Qué tan firme es el orden") {
        informed.forEach { strategy -> ConfidenceItem(strategy) }
        Text(
            "Con datos de la zona, la confianza nunca es alta.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExplainButton(Glossary.rankStability)
        ExplainButton(Glossary.confidence)
    }
}

@Composable
private fun ConfidenceItem(strategy: Strategy) {
    val uncertainty = strategy.uncertainty ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(strategy.shortLabel(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        uncertainty.rankStability?.let { stability ->
            val percent = if (stability <= 1.0) stability * 100 else stability
            Text("Quedó primera en ${formatNumber(percent, 0)} % de las repeticiones.", style = MaterialTheme.typography.bodyMedium)
        }
        uncertainty.confidenceLevel?.let { level ->
            Text("Confianza: ${ConfidenceDisplay.from(level).label}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        uncertainty.confidenceReason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ProvenanceSections(result: ComparisonResult, appliedInput: FarmerInput?, appliedPriorities: Priorities?) {
    val fromNasa = evidenceWith(result) { it.statusIs("documented", "observed") && it.source?.contains("NASA", ignoreCase = true) == true }
    val documentedOther = evidenceWith(result) { it.statusIs("documented", "observed") && it.source?.contains("NASA", ignoreCase = true) != true }
    val derived = evidenceWith(result) { it.statusIs("derived") }
    val assumptions = evidenceWith(result) { it.statusIs("assumption") }
    val farmerEvidence = evidenceWith(result) { it.statusIs("farmer_provided") }
    val unavailable = evidenceWith(result) { it.statusIs("unavailable") }

    SectionCard(title = "Qué viene de NASA") {
        BulletList(fromNasa, emptyText = "Sin datos marcados de NASA.")
        ExplainButton(Glossary.regional)
    }

    if (documentedOther.isNotEmpty()) {
        SectionCard(title = "Otros datos documentados") { BulletList(documentedOther) }
    }

    SectionCard(title = "Qué calculó FarmealoTu") {
        BulletList(derived, emptyText = "Sin datos calculados.")
        Text(
            "FarmealoTu calcula el orden con reglas y tus prioridades.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard(title = "Qué indicaste vos") {
        val own = buildList {
            if (appliedInput != null) {
                add("Sembrás ahora: ${appliedInput.currentCrop?.label ?: "sin dato"}")
                add("Sembraste antes: ${appliedInput.previousCrop?.label ?: "sin dato"}")
                add("Riego: ${appliedInput.irrigation?.label ?: "sin dato"}")
                add("Punto de referencia: ${formatCoordinate(appliedInput.lat)}, ${formatCoordinate(appliedInput.lon)}")
                if (appliedInput.excludedCrops.isNotEmpty()) add("No querés sembrar: ${appliedInput.excludedCrops.joinToString { it.label }}")
                if (appliedInput.notes.isNotBlank()) add("Tu nota (no se interpreta): «${appliedInput.notes}»")
            }
            if (appliedPriorities != null) {
                add(
                    "Tus prioridades: agua ${formatPercentWeight(appliedPriorities.water)}, " +
                        "suelo ${formatPercentWeight(appliedPriorities.soil)}, riesgo ${formatPercentWeight(appliedPriorities.risk)}",
                )
            }
            addAll(farmerEvidence)
        }
        BulletList(own, emptyText = "Sin datos tuyos.")
    }

    SectionCard(title = "Supuestos iniciales") {
        BulletList(assumptions, emptyText = "Sin supuestos.")
        Text(
            "Pesos iniciales (agua 40 %, suelo 35 %, riesgo 25 %): una preferencia, no un valor científico.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard(title = "Información que falta", containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        val missing = (result.strategies.flatMap { it.missingInformation } + unavailable).distinct()
        BulletList(missing, emptyText = "El servidor no informó vacíos.")
    }

    SectionCard(title = "Preguntas para tu agrónomo", containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        BulletList(
            result.strategies.flatMap { it.agronomistQuestions }.distinct(),
            emptyText = "Revisala con tu técnico.",
        )
    }

    SectionCard(title = "Límites de esta herramienta") {
        BulletList((result.limitations + FIXED_LIMITATIONS).distinct())
    }

    if (result.dataStatus.isNotEmpty()) {
        SectionCard(title = "Estado de los datos (según el servidor)") {
            BulletList(result.dataStatus.map { "${it.label}: ${it.value}" })
        }
    }
}

@Preview(showBackground = true, name = "Módulo 04 - ¿Qué tan segura es esta comparación?")
@Composable
fun UncertaintyScreenPreview() {
    FarmealoTuTheme {
        UncertaintyScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
        )
    }
}
