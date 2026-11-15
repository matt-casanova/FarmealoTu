package com.example.farmealotu.ui.screens.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.domain.CriterionKey
import com.example.farmealotu.domain.Evidence
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.EmptyBlock
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.GlossaryEntry
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.ScoreBar
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.traceDetails
import com.example.farmealotu.ui.screens.decision.ComparisonGate
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel

/**
 * «¿Por qué esta opción?»: los 5 bloques técnicos desglosados.
 */
@Composable
fun StrategyDetailScreen(
    viewModel: MainViewModel,
    strategyId: String,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()

    FarmScreen(title = "¿Por qué esta opción?", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        ComparisonGate(compare, onStartInput = onEditData, onRetry = viewModel::submitCompare) { state ->
            val result = state.data
            val strategy = result.strategies.firstOrNull { it.id == strategyId }
            if (strategy == null) {
                EmptyBlock(
                    title = "Esa opción ya no está en la comparación",
                    message = "Volvé a «Comparar formas de sembrar».",
                ) { SecondaryButton("Volver", onBack) }
                return@ComparisonGate
            }
            val axisMax = comparisonAxisMax(result)
            val color = ChartPalette.forStrategy(strategy.id, result.strategies.indexOf(strategy))

            SectionCard {
                StrategyHeader(strategy, position = null, isRecommended = result.recommended?.id == strategy.id)
                Text(strategy.sequenceText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            SectionCard(title = "Por qué se sugiere") {
                BulletList(strategy.reasons, emptyText = "Sin explicación.")
            }

            SectionCard(title = "Cómo le fue en cada criterio") {
                CriterionKey.entries.forEach { key ->
                    val criterion = key.of(strategy.criteria)
                    ScoreBar(key.label, criterion?.score, axisMax, color = color)
                    criterion?.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    ExplainButton(Glossary.forCriterion(key))
                }
                ScoreBar("Total", strategy.score, axisMax, color = color, emphasized = true)
                Text("Puntaje para comparar opciones, no probabilidad de éxito.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard(title = "Ventajas y riesgos") {
                TradeoffLine("Ventaja más importante", strategy.tradeoffs.advantage)
                TradeoffLine("Desventaja más importante", strategy.tradeoffs.disadvantage)
                TradeoffLine("Riesgo principal", strategy.tradeoffs.mainRisk)
                if (strategy.tradeoffs.extra.isNotEmpty()) BulletList(strategy.tradeoffs.extra)
            }

            SectionCard(title = "Evidencia") {
                if (strategy.evidence.isEmpty()) {
                    Text("Sin evidencia.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    strategy.evidence.forEachIndexed { index, evidence ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EvidenceItem(evidence)
                    }
                }
            }

            SectionCard(title = "Información que falta") {
                BulletList(strategy.missingInformation, emptyText = "Nada pendiente.")
            }

            SectionCard(title = "Preguntas para tu agrónomo", containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                BulletList(strategy.agronomistQuestions, emptyText = "Revisala con tu técnico.")
            }

            FooterActions(onEditData = onEditData, onHome = onHome)
        }
    }
}

/** Un dato de evidencia con su indicador, valor, unidad, fuente, período, escala y procedencia. */
@Composable
fun EvidenceItem(evidence: Evidence) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(evidence.indicator, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = evidence.valueText?.let { value -> value + (evidence.unit?.let { " $it" } ?: "") } ?: "No disponible",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        ProvenanceChip(evidence.status)
        Text(
            text = listOf(
                "Fuente: ${evidence.source ?: "no informada"}",
                "Período: ${formatApiDate(evidence.period) ?: "no informado"}",
                "Escala: ${evidence.scale ?: "no informada"}",
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExplainButton(
            entry = Glossary.forIndicatorId(evidence.indicator) ?: GlossaryEntry(
                title = evidence.indicator,
                body = "Dato usado para comparar.",
                limitation = if (evidence.scale?.contains("regional", ignoreCase = true) == true)
                    "Dato de la zona, no de tu campo." else null,
            ),
            details = traceDetails(evidence.source, evidence.period, evidence.unit, evidence.scale, evidence.status),
        )
    }
}

/** Texto de la secuencia con campañas */
fun Strategy.shortLabel(): String = if (name.isBlank()) id else "$id $name"

@Preview(showBackground = true, name = "¿Por qué esta opción? (StrategyDetail)")
@Composable
fun StrategyDetailScreenPreview() {
    FarmealoTuTheme {
        StrategyDetailScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            strategyId = "E1",
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
        )
    }
}
