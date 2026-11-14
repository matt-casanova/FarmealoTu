package com.example.farmealotu.ui.screens.tradeoffs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.example.farmealotu.ui.components.FarmIcons
import com.example.farmealotu.ui.components.LegendSwatch
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.scoreDecimals
import com.example.farmealotu.ui.screens.compare.StrategyHeader
import com.example.farmealotu.ui.screens.compare.TradeoffLine
import com.example.farmealotu.ui.screens.compare.comparisonAxisMax
import com.example.farmealotu.ui.screens.compare.shortLabel
import com.example.farmealotu.ui.screens.decision.ComparisonGate
import com.example.farmealotu.ui.screens.decision.OutdatedInputNotice
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel

/**
 * Frases de lectura directa de los criterios que mandó el servidor
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

            val highlights = criterionHighlights(result)
            if (highlights.isNotEmpty()) {
                SectionCard(title = "En pocas palabras", containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    BulletList(highlights)
                    Text(
                        "Ninguna opción gana en todo: mejorar en un criterio suele costar en otro.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    )
                }
            }

            result.strategies.forEach { strategy ->
                SectionCard {
                    StrategyHeader(strategy, position = null, isRecommended = result.recommended?.id == strategy.id)
                    Text(strategy.sequenceText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (strategy.tradeoffs.isEmpty) {
                        Text("El servidor no envió ventajas ni desventajas para esta opción.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        TradeoffLine("Ventaja", strategy.tradeoffs.advantage)
                        TradeoffLine("Desventaja", strategy.tradeoffs.disadvantage)
                        TradeoffLine("Riesgo principal", strategy.tradeoffs.mainRisk)
                        if (strategy.tradeoffs.extra.isNotEmpty()) BulletList(strategy.tradeoffs.extra)
                    }
                    SecondaryButton("¿Por qué esta opción?", { onOpenWhy(strategy.id) }, icon = FarmIcons.Help)
                }
            }

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
                "Cada punta es un criterio: más lejos del centro, mejor puntaje relativo. " +
                    "Cada anillo suma un cuarto de la escala (0 en el centro, ${formatNumber(axisMax, scoreDecimals(axisMax))} en el borde). " +
                    "No es probabilidad de éxito.",
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
                    fontWeight = FontWeight.Bold,
                )
            }

            Text("Los mismos números, en tabla", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            CriteriaTable(result.strategies, scoreDecimals(axisMax))
            ExplainButton(Glossary.relativeScore)
            SecondaryButton("Ocultar comparación técnica", { expanded = false })
        }
    }
}

/**
 * Tabla de criterios que entra en el ancho del teléfono (también con letra grande): el nombre de la
 * opción va en su propia línea y debajo sus cuatro valores, alineados con el encabezado.
 * Antes eran 150 dp + 4 × 64 dp con scroll lateral y la columna «Riesgo» quedaba fuera de pantalla.
 */
@Composable
private fun CriteriaTable(strategies: List<Strategy>, decimals: Int) {
    val headers = CriterionKey.entries.map { it.label } + "Total"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            headers.forEach { header ->
                Text(
                    header,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        strategies.forEachIndexed { index, strategy ->
            val values = CriterionKey.entries.map { it.of(strategy.criteria)?.score } + strategy.score
            val texts = values.map { value -> value?.let { formatNumber(it, decimals) } ?: "s/d" }
            val spoken = strategy.shortLabel() + ": " + headers.zip(texts).joinToString(", ") { (header, text) ->
                "$header ${if (text == "s/d") "dato insuficiente" else text}"
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = spoken },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LegendSwatch(ChartPalette.forStrategy(strategy.id, index), strategy.shortLabel())
                Row(Modifier.fillMaxWidth()) {
                    texts.forEachIndexed { column, text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (column == texts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Text("s/d = dato insuficiente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, name = "Módulo 02 - Ventajas y Desventajas")
@Composable
fun TradeoffsScreenPreview() {
    FarmealoTuTheme {
        TradeoffsScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
            onOpenWhy = {},
        )
    }
}
