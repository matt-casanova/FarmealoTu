package com.example.farmealotu.ui.screens.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.CriterionKey
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.Pill
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.ScoreBar
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.FoldRow
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatPercentWeight
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.scoreAxisMax
import com.example.farmealotu.ui.screens.decision.ComparisonGate
import com.example.farmealotu.ui.screens.decision.OutdatedInputNotice
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel

/** Eje común de puntajes para todas las tarjetas */
fun comparisonAxisMax(result: ComparisonResult): Double = scoreAxisMax(
    result.strategies.flatMap { strategy ->
        listOf(strategy.score, strategy.criteria.water?.score, strategy.criteria.soil?.score, strategy.criteria.risk?.score)
    }
)

/** Orden de presentación */
fun rankedStrategies(result: ComparisonResult): List<Strategy> =
    result.strategies.sortedWith(compareByDescending<Strategy> { it.score != null }.thenByDescending { it.score ?: 0.0 })

/**
 * Módulo 01 — «Comparar formas de sembrar».
 */
@Composable
fun CompareRotationsScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
    onOpenWhy: (String) -> Unit,
    expandPriorities: Boolean = false,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()
    val priorities by viewModel.priorities.collectAsStateWithLifecycle()
    val applied by viewModel.appliedPriorities.collectAsStateWithLifecycle()

    FarmScreen(title = "Comparar formas de sembrar", subtitle = "Módulo 01", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        ComparisonGate(compare, onStartInput = onEditData, onRetry = viewModel::submitCompare) { state ->
            val result = state.data
            val axisMax = comparisonAxisMax(result)
            FreshnessNote(state, onRetry = viewModel::submitCompare)
            OutdatedInputNotice(viewModel)

            SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
                Text("Puntaje para comparar opciones, no probabilidad de éxito.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "No indica cosecha ni ganancia.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ExplainButton(Glossary.relativeScore)
            }

            PrioritiesEditor(
                priorities = priorities,
                applied = applied,
                refreshing = state.refreshing,
                expandedInitially = expandPriorities,
                onChange = viewModel::setPriorities,
                onApply = viewModel::submitCompare,
            )

            if (result.strategies.size < 3) {
                SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)) {
                    Text(
                        "Hay menos de tres opciones posibles.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    )
                    FoldRow("Ver motivos") { BulletList(result.limitations, emptyText = "El servidor no explicó el motivo.") }
                }
            }

            rankedStrategies(result).forEachIndexed { index, strategy ->
                StrategyCard(
                    strategy = strategy,
                    position = if (strategy.score != null && result.recommended != null) index + 1 else null,
                    isRecommended = result.recommended?.id == strategy.id,
                    axisMax = axisMax,
                    colorIndex = result.strategies.indexOf(strategy),
                    onOpenWhy = { onOpenWhy(strategy.id) },
                )
            }

            FooterActions(onEditData = onEditData, onHome = onHome)
        }
    }
}

@Composable
private fun StrategyCard(
    strategy: Strategy,
    position: Int?,
    isRecommended: Boolean,
    axisMax: Double,
    colorIndex: Int,
    onOpenWhy: () -> Unit,
) {
    val color = ChartPalette.forStrategy(strategy.id, colorIndex)
    SectionCard(
        border = if (isRecommended) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        StrategyHeader(strategy, position, isRecommended)

        Text("Qué se siembra", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(strategy.sequenceText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CriterionKey.entries.forEach { key ->
                ScoreBar(key.label, key.of(strategy.criteria)?.score, axisMax, color = color)
            }
            ScoreBar("Total", strategy.score, axisMax, color = color, emphasized = true)
        }
        if (strategy.partialScore) {
            Pill("Puntaje parcial", container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
                "Puntaje parcial: falta un criterio.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FoldRow("Ver ventajas y riesgos") {
            TradeoffLine("Ventaja más importante", strategy.tradeoffs.advantage)
            TradeoffLine("Desventaja más importante", strategy.tradeoffs.disadvantage)
            TradeoffLine("Riesgo principal", strategy.tradeoffs.mainRisk)
        }

        SecondaryButton("¿Por qué esta opción?", onOpenWhy)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrategyHeader(strategy: Strategy, position: Int?, isRecommended: Boolean) {
    Text(strategy.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (position != null) Pill("Puesto $position")
        if (isRecommended) {
            Pill("Mejor posicionada", container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
        }
        if (strategy.id.equals("E0", ignoreCase = true)) Pill("Lo que hacés hoy")
    }
}

@Composable
fun TradeoffLine(label: String, text: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            text = text ?: "El servidor no lo informó.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (text == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PrioritiesEditor(
    priorities: Priorities,
    applied: Priorities?,
    refreshing: Boolean,
    expandedInitially: Boolean,
    onChange: (Priorities) -> Unit,
    onApply: () -> Unit,
) {
    val normalized = priorities.normalized()
    val pending = normalized != null && (applied == null || !normalized.approxEquals(applied))
    var expanded by rememberSaveable { mutableStateOf(expandedInitially) }
    SectionCard(title = "Mis prioridades") {
        if (applied != null) {
            Text(
                text = "Agua ${formatPercentWeight(applied.water)} · Suelo ${formatPercentWeight(applied.soil)} · Riesgo ${formatPercentWeight(applied.risk)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (!expanded) {
            SecondaryButton("Cambiar mis prioridades", { expanded = true })
            return@SectionCard
        }
        Text("Mové las barras: siempre suman 100 %.", style = MaterialTheme.typography.bodyMedium)
        ProvenanceChip("farmer_provided")
        PrioritySlider("Agua", priorities.water, normalized?.water) { onChange(priorities.copy(water = it)) }
        PrioritySlider("Suelo", priorities.soil, normalized?.soil) { onChange(priorities.copy(soil = it)) }
        PrioritySlider("Riesgo", priorities.risk, normalized?.risk) { onChange(priorities.copy(risk = it)) }
        if (normalized == null) {
            Text("Dejá al menos una prioridad mayor que cero.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
        if (applied != null && pending) {
            Text(
                "Tocá «Actualizar comparación» para aplicarlas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(
            text = if (refreshing) "Actualizando…" else "Actualizar comparación",
            onClick = onApply,
            enabled = pending && !refreshing,
        )
        Text(
            "Se recalcula con tus prioridades.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton("Volver a los valores iniciales (40 / 35 / 25)", { onChange(Priorities.DEFAULT) })
    }
}

@Composable
private fun PrioritySlider(label: String, raw: Double, share: Double?, onChange: (Double) -> Unit) {
    val shareText = share?.let { formatPercentWeight(it) } ?: "—"
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(shareText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
        }
        Slider(
            value = raw.toFloat().coerceIn(0f, 1f),
            onValueChange = { onChange((Math.round(it * 20f) / 20.0)) },
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Prioridad de $label: $shareText" },
        )
    }
}

@Preview(showBackground = true, name = "Módulo 01 - Comparar Formas de Sembrar")
@Composable
fun CompareRotationsScreenPreview() {
    FarmealoTuTheme {
        CompareRotationsScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
            onOpenWhy = {},
            expandPriorities = false,
        )
    }
}
