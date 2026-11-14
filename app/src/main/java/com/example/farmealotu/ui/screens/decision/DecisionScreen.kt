package com.example.farmealotu.ui.screens.decision

import androidx.compose.foundation.BorderStroke
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
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.ConfidenceDisplay
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.EmptyBlock
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FooterActions
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.Pill
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.ScoreBar
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.StateContent
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.components.scoreAxisMax
import com.example.farmealotu.ui.screens.home.AlertsCard
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.UiState

/** Estado común de los módulos que dependen de la comparación */
@Composable
fun ComparisonGate(
    state: UiState<ComparisonResult>,
    onStartInput: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable (UiState.Success<ComparisonResult>) -> Unit,
) {
    if (state is UiState.Idle) {
        EmptyBlock(
            title = "Primero contanos de tu campo",
            message = "Para comparar opciones necesitamos saber qué sembrás, qué sembraste antes, qué te preocupa y si tenés riego.",
        ) { PrimaryButton("Responder las 5 preguntas", onStartInput) }
        return
    }
    StateContent(
        state = state,
        onRetry = onRetry,
        loadingText = "Comparando opciones de siembra…",
        emptyTitle = "El servidor no devolvió opciones",
        errorExtra = { SecondaryButton("Editar mis datos", onStartInput) },
        content = content,
    )
}

/** Aviso cuando lo que está en pantalla se calculó con respuestas distintas */
@Composable
fun OutdatedInputNotice(viewModel: MainViewModel) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val applied by viewModel.appliedInput.collectAsStateWithLifecycle()
    if (applied != null && applied != input) {
        SectionCard(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        ) {
            Text(
                "Cambiaste tus datos después de esta comparación. Lo que ves corresponde a tus respuestas anteriores.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            SecondaryButton("Actualizar comparación", viewModel::submitCompare)
        }
    }
}

/**
 * Resultado principal: opción mejor posicionada, secuencia, razón, riesgo y confianza.
 */
@Composable
fun DecisionScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onEditData: () -> Unit,
    onOpenCompare: () -> Unit,
    onChangePriorities: () -> Unit,
    onOpenWhy: (String) -> Unit,
    onOpenTradeoffs: () -> Unit,
    onOpenRisks: () -> Unit,
    onOpenUncertainty: () -> Unit,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()

    FarmScreen(title = "Resultado", subtitle = "Mi campo — Cuatro Cañadas", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        ComparisonGate(compare, onStartInput = onEditData, onRetry = viewModel::submitCompare) { state ->
            val result = state.data
            FreshnessNote(state, onRetry = viewModel::submitCompare)
            OutdatedInputNotice(viewModel)

            val recommended = result.recommended
            if (recommended != null) {
                RecommendedCard(recommended, result)
            } else {
                InsufficientCard(result)
            }

            PrimaryButton("Comparar todas", onOpenCompare)
            SecondaryButton("Cambiar mis prioridades", onChangePriorities)
            if (recommended != null) SecondaryButton("❓ ¿Por qué esta opción?", { onOpenWhy(recommended.id) })

            SectionCard(title = "Seguir revisando") {
                SecondaryButton("02 · Ventajas y desventajas", onOpenTradeoffs)
                SecondaryButton("03 · Conocer los riesgos", onOpenRisks)
                SecondaryButton("04 · ¿Qué tan segura es esta comparación?", onOpenUncertainty)
            }

            AlertsCard(result)
            FooterActions(onEditData = onEditData, onHome = onHome)
        }
    }
}

@Composable
private fun RecommendedCard(strategy: Strategy, result: ComparisonResult) {
    val axisMax = scoreAxisMax(result.strategies.map { it.score })
    SectionCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text("Opción mejor posicionada", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(strategy.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (strategy.sequence.isEmpty()) {
                Text("Secuencia no informada", style = MaterialTheme.typography.bodyLarge)
            } else {
                strategy.sequence.forEachIndexed { index, step ->
                    Text(
                        text = "${step.seasonLabel ?: "Campaña ${index + 1}"}: ${step.cropLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        strategy.reasons.firstOrNull()?.let { reason ->
            Text("Por qué se sugiere", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(reason, style = MaterialTheme.typography.bodyLarge)
        }

        val caution = strategy.tradeoffs.mainRisk ?: strategy.tradeoffs.disadvantage
        if (caution != null) {
            Text("A tener en cuenta", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(caution, style = MaterialTheme.typography.bodyLarge)
        }

        if (strategy.score != null) {
            ScoreBar("Puntaje general", strategy.score, axisMax, color = ChartPalette.forStrategy(strategy.id, 0), emphasized = true)
            Text(
                "Puntaje relativo entre opciones, no probabilidad de éxito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (strategy.partialScore) Pill("Puntaje parcial: falta un criterio")

        val confidence = ConfidenceDisplay.from(strategy.uncertainty?.confidenceLevel)
        Text("Confianza de la comparación: ${confidence.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        strategy.uncertainty?.confidenceReason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        ExplainButton(Glossary.confidence)

        Text("Revisá con tu técnico antes de decidir.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InsufficientCard(result: ComparisonResult) {
    SectionCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
    ) {
        Text("Información insuficiente para ordenar opciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = when (result.strategies.size) {
                0 -> "El servidor no devolvió opciones para comparar."
                else -> "El servidor devolvió ${result.strategies.size} opción(es), pero no eligió una mejor posicionada. No se fuerza un ganador."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        val reasons = (result.limitations + result.strategies.flatMap { it.missingInformation }).distinct()
        Text("Razones informadas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        BulletList(reasons, emptyText = "El servidor no explicó el motivo.")
        Text("Revisá con tu técnico: igual podés comparar las opciones una por una.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, name = "Resultado Principal (DecisionScreen)")
@Composable
fun DecisionScreenPreview() {
    FarmealoTuTheme {
        DecisionScreen(
            viewModel = sampleMainViewModel(withComparison = true),
            demoMode = true,
            onBack = {},
            onHome = {},
            onEditData = {},
            onOpenCompare = {},
            onChangePriorities = {},
            onOpenWhy = {},
            onOpenTradeoffs = {},
            onOpenRisks = {},
            onOpenUncertainty = {},
        )
    }
}
