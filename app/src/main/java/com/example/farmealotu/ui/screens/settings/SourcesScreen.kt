package com.example.farmealotu.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.domain.SourceRef
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.FIXED_LIMITATIONS
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.LabeledValueList
import com.example.farmealotu.ui.components.NASA_ROLE
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.StateContent
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.UiState

/** «Fuentes y límites»: bibliografía (GET /api/sources), contexto histórico (GET /api/context) y límites declarados. */
@Composable
fun SourcesScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadSources() }

    FarmScreen(title = "Fuentes y límites", demoMode = demoMode, onBack = onBack, onHome = onHome) {
        SectionCard(title = "Quién hace qué", containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(NASA_ROLE, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("NASA no recomienda cultivos ni rotaciones. La comparación la arma FarmealoTu; la decisión es tuya y de tu técnico.", style = MaterialTheme.typography.bodyMedium)
        }

        SectionCard(title = "Límites de esta herramienta") { BulletList(FIXED_LIMITATIONS) }

        Text("Fuentes de los datos", style = MaterialTheme.typography.titleMedium)
        StateContent(
            state = sources,
            onRetry = { viewModel.loadSources(force = true) },
            loadingText = "Consultando las fuentes…",
            emptyTitle = "El servidor no listó fuentes",
            emptyMessage = "Cuando el servidor publique su catálogo de fuentes va a aparecer acá.",
        ) { state ->
            FreshnessNote(state, onRetry = { viewModel.loadSources(force = true) })
            SectionCard {
                state.data.forEachIndexed { index, source ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SourceItem(source)
                }
            }
        }

        // Contexto histórico: solo si el servidor lo envía. Nunca se presenta como dato actual.
        (context as? UiState.Success)?.let { state ->
            Text("Contexto histórico de la zona", style = MaterialTheme.typography.titleMedium)
            Text(
                "Son cifras de años pasados (por ejemplo, superficie y producción municipal de 2023). No son valores actuales de cosecha ni alertas.",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            )
            state.data.forEach { section ->
                SectionCard(title = section.title) { LabeledValueList(section.lines) }
            }
        }
    }
}

@Composable
private fun SourceItem(source: SourceRef) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(source.title, style = MaterialTheme.typography.titleSmall)
        source.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        val url = source.url
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
            TextButton(onClick = { runCatching { uriHandler.openUri(url) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Abrir la fuente")
            }
            Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
