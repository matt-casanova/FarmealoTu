package com.example.farmealotu.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.domain.Alert
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.Level
import com.example.farmealotu.ui.components.LevelBadge
import com.example.farmealotu.ui.components.LoadingBlock
import com.example.farmealotu.ui.components.NASA_ROLE
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatClock
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.UiState
import com.example.farmealotu.viewmodel.dataOrNull

/**
 * Inicio «Mi campo — Cuatro Cañadas», en el orden de la página 16 del plan:
 * encabezado, fecha de los datos, acción a revisar, advertencias fechadas, botón principal,
 * los 4 módulos, accesos secundarios y estado de conexión. Sin gráficos técnicos.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    connection: ConnectionSettings,
    onCompareOptions: () -> Unit,
    onOpenDecision: () -> Unit,
    onOpenCompare: () -> Unit,
    onOpenTradeoffs: () -> Unit,
    onOpenRisks: () -> Unit,
    onOpenUncertainty: () -> Unit,
    onOpenGeo: () -> Unit,
    onOpenField: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSources: () -> Unit,
) {
    val compare by viewModel.compare.collectAsStateWithLifecycle()
    val nasaStatus by viewModel.nasaStatus.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val result = compare.dataOrNull

    FarmScreen(title = "FarmealoTu", subtitle = "Mi campo — Cuatro Cañadas", demoMode = connection.demoMode) {
        // 2. Fecha REAL de la última observación (distinta de la fecha de descarga).
        val observation = result?.lastObservationDate ?: nasaStatus.dataOrNull?.lastObservationDate
        val download = result?.downloadedAt ?: nasaStatus.dataOrNull?.lastDownload
        SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
            Text(
                text = "Datos actualizados al: " + (formatApiDate(observation) ?: "fecha no disponible"),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = when {
                    observation == null && nasaStatus is UiState.Loading -> "Consultando el estado de los datos…"
                    observation == null -> "El servidor todavía no informó la fecha de la última observación."
                    download != null -> "Es la última observación disponible. Se descargaron el ${formatApiDate(download)}."
                    else -> "Es la última observación disponible; no es la fecha de hoy."
                } + if (connection.demoMode) " (Fechas de ejemplo.)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 3. Tarjeta principal: qué revisar.
        MainActionCard(compare, input.isComplete, onOpenDecision, onRetry = viewModel::submitCompare)

        // 4. Advertencias con fecha, solo las que calcula el servidor.
        if (result != null) AlertsCard(result)

        // 5. Botón principal.
        PrimaryButton("Comparar opciones de siembra", onCompareOptions)

        // 6. Los cuatro módulos.
        SectionCard(title = "Los 4 pasos para decidir") {
            ModuleRow("01", "Comparar formas de sembrar", "Tu rotación actual frente a las alternativas.", onOpenCompare)
            ModuleRow("02", "Ventajas y desventajas", "Qué ganás y qué resignás con cada opción.", onOpenTradeoffs)
            ModuleRow("03", "Conocer los riesgos", "Rachas secas, calor y lluvias fuertes por mes y año.", onOpenRisks)
            ModuleRow("04", "¿Qué tan segura es esta comparación?", "Rangos, vacíos de información y preguntas para tu técnico.", onOpenUncertainty)
        }

        // 7. Accesos secundarios.
        SecondaryButton("Explorar mi zona", onOpenGeo)
        SecondaryButton("Datos de terreno", onOpenField)

        // 8. Estado de conexión.
        SectionCard(title = "Conexión", containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
            Text(
                text = when {
                    connection.demoMode -> "Modo demo: se muestran datos de ejemplo, sin servidor."
                    health is UiState.Success && health.dataOrNull?.ok == true -> "Conectado al servidor de FarmealoTu."
                    health is UiState.Success -> "El servidor respondió, pero avisa que no está listo."
                    health is UiState.Error -> "Sin conexión con el servidor. ${(health as UiState.Error).failure.message}"
                    else -> "Comprobando la conexión…"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            (health as? UiState.Success)?.let {
                Text("Comprobado el ${formatClock(it.fetchedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SecondaryButton("Volver a comprobar", viewModel::refreshStatus)
            SecondaryButton("Configurar conexión", onOpenConnection)
            SecondaryButton("Fuentes y límites", onOpenSources)
        }

        Text(NASA_ROLE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MainActionCard(
    compare: UiState<ComparisonResult>,
    inputComplete: Boolean,
    onOpenDecision: () -> Unit,
    onRetry: () -> Unit,
) {
    SectionCard(
        title = "¿Qué conviene revisar antes de mi próxima siembra?",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        when (compare) {
            UiState.Loading -> LoadingBlock("Comparando opciones…")
            is UiState.Error -> {
                Text(compare.failure.title, style = MaterialTheme.typography.titleMedium)
                Text(compare.failure.message, style = MaterialTheme.typography.bodyMedium)
                SecondaryButton("Reintentar", onRetry)
            }
            is UiState.Success -> {
                val recommended = compare.data.recommended
                if (recommended != null) {
                    Text(
                        text = "Conviene revisar la opción «${recommended.displayName}».",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(recommended.sequenceText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    recommended.reasons.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text("Revisá con tu técnico antes de decidir.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                } else {
                    Text("Información insuficiente para ordenar opciones", style = MaterialTheme.typography.titleLarge)
                    Text("El servidor no eligió una opción mejor posicionada. Mirá las razones en el resultado.", style = MaterialTheme.typography.bodyMedium)
                }
                SecondaryButton("Ver resultado", onOpenDecision)
            }
            else -> {
                Text(
                    text = if (inputComplete) "Ya tenemos tus respuestas. Compará tus opciones de siembra."
                    else "Contanos de tu campo en 5 preguntas cortas y te mostramos opciones de siembra para comparar.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun AlertsCard(result: ComparisonResult) {
    SectionCard(title = "Advertencias") {
        if (result.alerts.isEmpty()) {
            Text("El servidor no calculó advertencias para esta consulta.", style = MaterialTheme.typography.bodyMedium)
        } else {
            result.alerts.forEach { AlertItem(it) }
        }
        Text(
            "Las advertencias se calculan con datos fechados. No son alertas en tiempo real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertItem(alert: Alert) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Level.from(alert.level)?.let { LevelBadge(it, prefix = "Atención") }
        Text(alert.message, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Fecha / período: ${formatApiDate(alert.date) ?: "no informado"}" + (alert.source?.let { " · Fuente: $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (alert.evidenceStatus != null) ProvenanceChip(alert.evidenceStatus)
    }
}

@Composable
private fun ModuleRow(number: String, title: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(number, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(40.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
