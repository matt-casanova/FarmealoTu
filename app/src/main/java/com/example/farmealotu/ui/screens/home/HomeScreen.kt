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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.domain.Alert
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.ui.components.FarmIcons
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.Level
import com.example.farmealotu.ui.components.LevelBadge
import com.example.farmealotu.ui.components.LoadingBlock
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.QuietAction
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatClock
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.UiState
import com.example.farmealotu.viewmodel.dataOrNull

/**
 * Inicio «Mi campo — Cuatro Cañadas».
 * Jerarquía limpia: fecha de actualización, tarjeta principal de recomendación,
 * advertencias, botón principal de siembra y acceso a los 4 módulos.
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
        val observation = result?.lastObservationDate ?: nasaStatus.dataOrNull?.lastObservationDate
        val download = result?.downloadedAt ?: nasaStatus.dataOrNull?.lastDownload

        SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
            Text(
                text = "Datos actualizados al: " + (formatApiDate(observation) ?: "fecha no disponible"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    observation == null && nasaStatus is UiState.Loading -> "Consultando el estado de los datos…"
                    observation == null -> "Sin fecha de datos todavía."
                    download != null -> "Descargados el ${formatApiDate(download)}."
                    else -> "Último dato disponible."
                } + if (connection.demoMode) " (Fechas de ejemplo.)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Tarjeta principal: qué revisar
        MainActionCard(compare, input.isComplete, onOpenDecision, onRetry = viewModel::submitCompare)

        // Advertencias
        if (result != null) AlertsCard(result)

        // Botón principal de siembra
        PrimaryButton("Comparar opciones de siembra", onCompareOptions)

        // Los 4 módulos principales
        SectionCard(title = "Los 4 pasos para decidir") {
            ModuleRow("01", "Comparar formas de sembrar", "Tu rotación actual frente a las alternativas.", onOpenCompare)
            ModuleRow("02", "Ventajas y desventajas", "Qué ganás y qué resignás con cada opción.", onOpenTradeoffs)
            ModuleRow("03", "Conocer los riesgos", "Rachas secas, calor y lluvias fuertes por mes y año.", onOpenRisks)
            ModuleRow("04", "¿Qué tan segura es esta comparación?", "Rangos P10-P90 y preguntas para tu técnico.", onOpenUncertainty)
        }

        // Accesos directos a campo y mapa
        SecondaryButton("Explorar mi zona", onOpenGeo)
        SecondaryButton("Datos de terreno", onOpenField)

        // Estado de conexión
        SectionCard(title = "Estado de conexión", containerColor = MaterialTheme.colorScheme.surfaceContainer, border = null) {
            Text(
                text = when {
                    connection.demoMode -> "Modo demo: datos de ejemplo."
                    health is UiState.Success && health.dataOrNull?.ok == true -> "Conectado."
                    health is UiState.Success -> "El servidor todavía no está listo."
                    health is UiState.Error -> "Sin conexión con el servidor. ${(health as UiState.Error).failure.message}"
                    else -> "Comprobando la conexión…"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            (health as? UiState.Success)?.let {
                Text("Comprobado el ${formatClock(it.fetchedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Acciones de mantenimiento: discretas, para no competir con la acción principal del Inicio.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QuietAction("Recomprobar", null, viewModel::refreshStatus, Modifier.weight(1f))
                QuietAction("Configurar conexión", null, onOpenConnection, Modifier.weight(1f))
            }
            QuietAction("Fuentes y límites", FarmIcons.ChevronRight, onOpenSources)
        }
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
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        when (compare) {
            UiState.Loading -> LoadingBlock("Comparando opciones…")
            is UiState.Error -> {
                Text(compare.failure.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(compare.failure.message, style = MaterialTheme.typography.bodyMedium)
                SecondaryButton("Reintentar", onRetry)
            }
            is UiState.Success -> {
                val recommended = compare.data.recommended
                if (recommended != null) {
                    Text(
                        text = "Conviene revisar la opción «${recommended.displayName}».",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(recommended.sequenceText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    recommended.reasons.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text("Revisá con tu técnico antes de decidir.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text("Faltan datos para ordenar las opciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ninguna opción sale mejor. Mirá el resultado.", style = MaterialTheme.typography.bodyMedium)
                }
                // Enlace discreto: la acción principal («Comparar opciones de siembra») lleva al mismo resultado.
                QuietAction("Ver resultado", FarmIcons.ChevronRight, onOpenDecision)
            }
            else -> {
                Text(
                    text = if (inputComplete) "Ya tenemos tus respuestas."
                    else "Respondé 5 preguntas y compará opciones de siembra.",
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
            Text("Sin advertencias.", style = MaterialTheme.typography.bodyMedium)
        } else {
            result.alerts.forEach { AlertItem(it) }
        }
        Text(
            "No son alertas en tiempo real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertItem(alert: Alert) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Level.from(alert.level)?.let { LevelBadge(it, prefix = "Atención") }
        Text(alert.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = "Fecha / período: ${formatApiDate(alert.date) ?: "no informado"}" + (alert.source?.let { " · Fuente: $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (alert.evidenceStatus != null) ProvenanceChip(alert.evidenceStatus)
    }
}

@Composable
fun ModuleRow(number: String, title: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(FarmIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true, name = "Pantalla de Inicio (HomeScreen)")
@Composable
fun HomeScreenPreview() {
    FarmealoTuTheme {
        HomeScreen(
            viewModel = sampleMainViewModel(),
            connection = ConnectionSettings(baseUrl = "http://localhost:8000/", demoMode = true),
            onCompareOptions = {},
            onOpenDecision = {},
            onOpenCompare = {},
            onOpenTradeoffs = {},
            onOpenRisks = {},
            onOpenUncertainty = {},
            onOpenGeo = {},
            onOpenField = {},
            onOpenConnection = {},
            onOpenSources = {},
        )
    }
}
