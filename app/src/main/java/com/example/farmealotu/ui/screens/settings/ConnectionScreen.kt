package com.example.farmealotu.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.AppSettings
import com.example.farmealotu.BuildConfig
import com.example.farmealotu.ui.components.BulletList
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatClock
import com.example.farmealotu.ui.components.sampleAppSettings
import com.example.farmealotu.ui.components.sampleMainViewModel
import com.example.farmealotu.ui.theme.FarmealoTuTheme
import com.example.farmealotu.viewmodel.MainViewModel
import com.example.farmealotu.viewmodel.UiState

/**
 * Configuración de conexión: URL del backend FastAPI y modo demo.
 */
@Composable
fun ConnectionScreen(
    settings: AppSettings,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val connection by settings.state.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    var urlText by rememberSaveable(connection.baseUrl) { mutableStateOf(connection.baseUrl) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

    FarmScreen(title = "Conexión", subtitle = "Servidor de FarmealoTu", demoMode = connection.demoMode, onBack = onBack, onHome = onHome) {
        SectionCard(title = "Estado actual") {
            Text(
                text = when (val state = health) {
                    is UiState.Success -> if (connection.demoMode) "Modo demo activo: no se consulta ningún servidor."
                    else if (state.data.ok) "Conectado. El servidor respondió bien." else "El servidor respondió, pero avisa que no está listo."
                    is UiState.Error -> "❌ ${state.failure.title}. ${state.failure.message}"
                    else -> "⏳ Comprobando estado…"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            (health as? UiState.Success)?.let { state ->
                state.data.detail?.let { Text("Respuesta: $it", style = MaterialTheme.typography.bodySmall) }
                Text("Comprobado el ${formatClock(state.fetchedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SecondaryButton("Probar conexión", viewModel::refreshStatus)
        }

        SectionCard(title = "Dirección del servidor") {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it; message = null },
                label = { Text("URL base de la API") },
                singleLine = true,
                isError = isError && message != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            // 10.0.2.2 es «la PC» solo dentro del emulador; en un celular no llega a ningún lado.
            if ("10.0.2.2" in urlText && !isProbablyEmulator()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "La dirección 10.0.2.2 solo funciona en el emulador. En este celular, escribí la IP de la " +
                            "computadora que corre el servidor (en la misma red Wi-Fi), por ejemplo http://192.168.1.10:8000/.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            PrimaryButton("Guardar dirección", {
                val trimmed = urlText.trim()
                when {
                    !BuildConfig.DEBUG && trimmed.startsWith("http://") -> {
                        isError = true
                        message = "Esta versión solo acepta direcciones https://."
                    }
                    settings.setBaseUrl(trimmed) -> {
                        isError = false
                        message = "Dirección guardada correctamente."
                    }
                    else -> {
                        isError = true
                        message = "Esa dirección no es válida. Tiene que empezar con http:// o https://."
                    }
                }
            })
            SecondaryButton("Usar dirección de fábrica (${settings.defaultBaseUrl})", {
                settings.setBaseUrl(settings.defaultBaseUrl)
                isError = false
                message = "Se restableció la dirección de fábrica."
            })
            Text("Guía rápida", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            BulletList(
                listOf(
                    "Emulador de Android: http://10.0.2.2:8000/",
                    "Celular físico: http://IP-DEL-PC:8000/ (en la misma red Wi-Fi).",
                    "Demo pública: la dirección https:// del servidor desplegado.",
                )
            )
        }

        SectionCard(title = "Modo demo") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Usar datos de ejemplo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Sirve para recorrer la app sin servidor. Las pantallas llevarán la franja MODO DEMO.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = connection.demoMode,
                    onCheckedChange = settings::setDemoMode,
                    modifier = Modifier.semantics { contentDescription = "Usar datos de ejemplo, modo demo" },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Configuración de Conexión (ConnectionScreen)")
@Composable
fun ConnectionScreenPreview() {
    val context = LocalContext.current
    FarmealoTuTheme {
        ConnectionScreen(
            settings = sampleAppSettings(context),
            viewModel = sampleMainViewModel(withComparison = false),
            onBack = {},
            onHome = {},
        )
    }
}

/** Heurística conocida para el emulador de Android Studio (no se usa para nada más que este aviso). */
private fun isProbablyEmulator(): Boolean =
    android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.FINGERPRINT.contains("emulator") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.PRODUCT.contains("sdk")
