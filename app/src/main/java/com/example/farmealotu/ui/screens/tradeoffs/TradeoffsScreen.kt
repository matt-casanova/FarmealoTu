package com.example.farmealotu.ui.screens.tradeoffs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.data.model.formatNumber
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.CriterionKey
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.RadarChart
import com.example.farmealotu.ui.components.RadarSeries
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.SeriesLegend
import com.example.farmealotu.ui.components.scoreDecimals
import com.example.farmealotu.ui.screens.compare.StrategyHeader
import com.example.farmealotu.ui.screens.compare.TradeoffLine
import com.example.farmealotu.ui.screens.compare.comparisonAxisMax
import com.example.farmealotu.ui.screens.compare.shortLabel
import com.example.farmealotu.ui.screens.decision.ComparisonGate
import com.example.farmealotu.ui.screens.decision.OutdatedInputNotice
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.viewmodel.MainViewModel

/**
 * Frases de lectura directa de los criterios que mandó el servidor (quién tiene el valor más alto
 * en cada uno). No se calcula ni se pondera nada: es leer el máximo de una columna.
 */
fun criterionHighlights(result: ComparisonResult): List<String> = CriterionKey.entries.mapNotNull { key ->
    val scored = result.strategies.mapNotNull { strategy -> key.of(strategy.criteria)?.score?.let { strategy to it } }
    if (scored.size < 2) return@mapNotNull null
    val best = scored.maxOf { it.second }
    val leaders = scored.filter { it.second == best }
    val decimals = scoreDecimals(comparisonAxisMax(result))
    if (leaders.size == scored.size) {
        "En ${key.label.lowercase()}, todas las opciones quedan igual (${formatNumber(best, decimals)})."
    } else {
        "En ${key.label.lowercase()}, el puntaje más alto es de ${leaders.joinToString(" y ") { it.first.shortLabel() }} (${formatNumber(best, decimals)})."
    }
}

/**
 * Módulo 02 — «Ventajas y desventajas».
 * Primero frases simples (tradeoffs y criterios reales); el radar va después, plegado.
 */
@Composable
fun TradeoffsScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
    onOpenWhy: (String) -> Unit,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()

    FarmScreen(title = "Ventajas y desventajas", subtitle = "Módulo 02", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        ComparisonGate(compare, onStartInput = onEditData, onRetry = viewModel::submitCompare) { state ->
            val result = state.data
            FreshnessNote(state, onRetry = viewModel::submitCompare)
            OutdatedInputNotice(viewModel)

            // 1) Conclusiones comprensibles, siempre primero.
            val highlights = criterionHighlights(result)
            if (highlights.isNotEmpty()) {
                SectionCard(title = "En pocas palabras", containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    BulletList(highlights)
                    Text(
                        "Ninguna opción gana en todo: mejorar en un criterio suele costar en otro.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    )
                }
            }

            result.strategies.forEach { strategy ->
                SectionCard {
                    StrategyHeader(strategy, position = null, isRecommended = result.recommended?.id == strategy.id)
                    Text(strategy.sequenceText, style = MaterialTheme.typography.bodyLarge)
                    if (strategy.tradeoffs.isEmpty) {
                        Text("El servidor no envió ventajas ni desventajas para esta opción.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        TradeoffLine("Ventaja", strategy.tradeoffs.advantage)
                        TradeoffLine("Desventaja", strategy.tradeoffs.disadvantage)
                        TradeoffLine("Riesgo principal", strategy.tradeoffs.mainRisk)
                        if (strategy.tradeoffs.extra.isNotEmpty()) BulletList(strategy.tradeoffs.extra)
                    }
                    SecondaryButton("¿Por qué esta opción?", { onOpenWhy(strategy.id) })
                }
            }

            // 2) Comparación técnica: plegada por defecto, nunca delante de las frases.
            TechnicalComparison(result)

            FooterActions(onEditData = onEditData, onHome = onHome)
        }
    }
}

@Composable
private fun TechnicalComparison(result: ComparisonResult) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "Comparación técnica") {
        if (!expanded) {
            Text("Un gráfico con los tres criterios de cada opción, para quien quiera el detalle.", style = MaterialTheme.typography.bodyMedium)
            SecondaryButton("Ver comparación técnica", { expanded = true })
        } else {
            val axisMax = comparisonAxisMax(result)
            val series = result.strategies.mapIndexed { index, strategy ->
                RadarSeries(
                    name = strategy.shortLabel(),
                    color = ChartPalette.forStrategy(strategy.id, index),
                    values = CriterionKey.entries.map { it.of(strategy.criteria)?.score },
                )
            }
            Text(
                "Cada punta es un criterio: más lejos del centro, mejor puntaje relativo. No es probabilidad de éxito.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SeriesLegend(series.map { it.color to it.name })
            RadarChart(axes = CriterionKey.entries.map { it.label }, series = series, axisMax = axisMax)

            val incomplete = result.strategies.filter { it.criteria.hasMissing }
            if (incomplete.isNotEmpty()) {
                Text(
                    text = "Dato insuficiente: " + incomplete.joinToString("; ") { strategy ->
                        val missing = CriterionKey.entries.filter { it.of(strategy.criteria)?.score == null }.joinToString(", ") { it.label.lowercase() }
                        "${strategy.shortLabel()} no tiene valor de $missing"
                    } + ". En esos casos no se dibuja ese vértice.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            Text("Los mismos números, en tabla", style = MaterialTheme.typography.labelLarge)
            CriteriaTable(result.strategies, scoreDecimals(axisMax))
            ExplainButton(Glossary.relativeScore)
            SecondaryButton("Ocultar comparación técnica", { expanded = false })
        }
    }
}

@Composable
private fun CriteriaTable(strategies: List<Strategy>, decimals: Int) {
    Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text("Opción", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(150.dp))
            (CriterionKey.entries.map { it.label } + "Total").forEach { header ->
                Text(header, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
            }
        }
        strategies.forEach { strategy ->
            Row {
                Text(strategy.shortLabel(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(150.dp))
                (CriterionKey.entries.map { it.of(strategy.criteria)?.score } + strategy.score).forEach { value ->
                    Text(
                        text = value?.let { formatNumber(it, decimals) } ?: "s/d",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(64.dp),
                    )
                }
            }
        }
        Text("s/d = dato insuficiente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
