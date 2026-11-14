package com.example.farmealotu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.domain.LabeledValue
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.DemoStripe
import com.example.farmealotu.ui.theme.OnDemoStripe
import com.example.farmealotu.viewmodel.UiState

// ---------------------------------------------------------------- andamiaje de pantalla

/**
 * Marco común: barra superior con «volver» e «Inicio», franja de modo demo persistente
 * y aviso fijo de que la herramienta apoya, no reemplaza, la decisión del agricultor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmScreen(
    title: String,
    demoMode: Boolean,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    subtitle: String? = null,
    scrollResetKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    // Cuando cambia el paso dentro de una misma pantalla, se vuelve arriba: lo nuevo no queda escondido.
    LaunchedEffect(scrollResetKey) { scrollState.scrollTo(0) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                            if (subtitle != null) {
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.semantics { contentDescription = "Volver" },
                            ) {
                                Text("←", fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics { })
                            }
                        }
                    },
                    actions = {
                        if (onHome != null) {
                            TextButton(
                                onClick = onHome,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                            ) { Text("Inicio") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                if (demoMode) DemoBanner()
            }
        },
        bottomBar = { DisclaimerBar() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Franja persistente: cualquier pantalla alimentada por fixtures la lleva (regla 9 del prompt). */
@Composable
fun DemoBanner() {
    Surface(color = DemoStripe, contentColor = OnDemoStripe, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MODO DEMO · DATOS DE EJEMPLO",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics { contentDescription = "Modo demo. Los datos de esta pantalla son de ejemplo, no son reales." },
        )
    }
}

@Composable
fun DisclaimerBar() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// ---------------------------------------------------------------- bloques básicos

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSurface),
        border = border,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Pie común de los módulos: desde cualquier pantalla se puede editar los datos o volver al inicio. */
@Composable
fun FooterActions(onEditData: () -> Unit, onHome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton("Editar mis datos", onEditData)
        SecondaryButton("Volver al inicio", onHome)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Etiqueta de procedencia en lenguaje sencillo (PDF sección 0: evidence_status). */
fun provenanceLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "documented", "observed" -> "Documentado / observado"
    "derived" -> "Calculado por FarmealoTu"
    "assumption" -> "Supuesto"
    "farmer_provided" -> "Dato tuyo"
    "unavailable" -> "No disponible"
    "simulated" -> "Simulado"
    "wokwi" -> "Wokwi (simulado)"
    "physical" -> "Medido (físico)"
    null, "" -> "Procedencia no informada"
    else -> status
}

@Composable
fun ProvenanceChip(status: String?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (status?.trim()?.lowercase()) {
        "documented", "observed", "physical" -> scheme.primaryContainer to scheme.onPrimaryContainer
        "derived" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        "assumption", "simulated", "wokwi" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        "unavailable" -> scheme.errorContainer to scheme.onErrorContainer
        else -> scheme.surfaceVariant to scheme.onSurface
    }
    Pill(provenanceLabel(status), modifier, container, content)
}

enum class Level(val text: String, val color: Color) {
    LOW("Bajo", ChartPalette.statusGood),
    MEDIUM("Medio", ChartPalette.statusWarning),
    HIGH("Alto", ChartPalette.statusCritical);

    companion object {
        fun from(apiLevel: String?): Level? = when (apiLevel?.trim()?.lowercase()) {
            "low", "bajo", "baja", "info", "ok", "good" -> LOW
            "medium", "medio", "media", "moderate", "warning" -> MEDIUM
            "high", "alto", "alta", "critical", "severe", "serious" -> HIGH
            else -> null
        }
    }
}

/** Semáforo de tres niveles: forma + texto, nunca solo color. */
@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier, prefix: String = "Nivel") {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$prefix: ${level.text}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(18.dp)) {
            when (level) {
                Level.LOW -> drawCircle(level.color)
                Level.MEDIUM -> drawPath(
                    Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    level.color,
                )
                Level.HIGH -> drawRect(level.color, Offset.Zero, Size(size.width, size.height))
            }
        }
        Text("$prefix: ${level.text}", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value ?: "no disponible",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
fun LabeledValueList(items: List<LabeledValue>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { InfoRow(it.label, it.value) }
    }
}

@Composable
fun BulletList(items: List<String>, emptyText: String? = null) {
    if (items.isEmpty()) {
        if (emptyText != null) Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clearAndSetSemantics { })
                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- «¿Qué significa?»

/** Botón que abre la explicación de un indicador: qué es, fuente, fecha, unidad, escala y límite. */
@Composable
fun ExplainButton(
    entry: GlossaryEntry,
    modifier: Modifier = Modifier,
    details: List<LabeledValue> = emptyList(),
    label: String = "¿Qué significa?",
) {
    var open by rememberSaveable(entry.title) { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = modifier.heightIn(min = 48.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Entendido") } },
            title = { Text(entry.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                    if (details.isNotEmpty()) LabeledValueList(details)
                    if (entry.limitation != null) {
                        Text(
                            text = "Límite: ${entry.limitation}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            },
        )
    }
}

/** Fuente, fecha, unidad y escala de un dato, listas para el diálogo «¿Qué significa?». */
fun traceDetails(source: String?, date: String?, unit: String?, scale: String?, status: String? = null): List<LabeledValue> =
    listOf(
        LabeledValue("Fuente", source ?: "no informada"),
        LabeledValue("Fecha / período", formatApiDate(date) ?: "no informada"),
        LabeledValue("Unidad", unit ?: "no informada"),
        LabeledValue("Escala", scale ?: "no informada"),
    ) + listOfNotNull(status?.let { LabeledValue("Procedencia", provenanceLabel(it)) })

// ---------------------------------------------------------------- estados

@Composable
fun LoadingBlock(text: String = "Cargando…") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorBlock(
    failure: ApiFailure,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    SectionCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Text(failure.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(failure.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        if (onRetry != null) PrimaryButton("Reintentar", onRetry)
        extra?.invoke(this)
    }
}

@Composable
fun EmptyBlock(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    SectionCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium)
        action?.invoke()
    }
}

/** Fecha en que la app consultó el dato + aviso si no se pudo actualizar (dato obsoleto). */
@Composable
fun <T> FreshnessNote(state: UiState.Success<T>, onRetry: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            text = "Consultado el ${formatClock(state.fetchedAtMillis)}" + if (state.isDemo) " · datos de ejemplo" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val error = state.refreshError
        if (error != null) {
            SectionCard(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
            ) {
                Text(
                    "No se pudo actualizar. Estás viendo lo último que se consultó, que puede estar desactualizado.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text("${error.title}. ${error.message}", style = MaterialTheme.typography.bodySmall)
                if (onRetry != null) SecondaryButton("Reintentar", onRetry)
            }
        }
    }
}

/** Dibuja carga / error / vacío y delega el contenido cuando hay datos. Nunca deja la pantalla en blanco. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    loadingText: String = "Cargando…",
    emptyTitle: String = "Todavía no hay datos",
    emptyMessage: String = "El servidor respondió, pero no envió información para mostrar.",
    errorExtra: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable (UiState.Success<T>) -> Unit,
) {
    when (state) {
        UiState.Idle, UiState.Loading -> LoadingBlock(loadingText)
        UiState.Empty -> EmptyBlock(emptyTitle, emptyMessage) { SecondaryButton("Volver a consultar", onRetry) }
        is UiState.Error -> ErrorBlock(state.failure, onRetry, extra = errorExtra)
        is UiState.Success -> content(state)
    }
}

@Composable
fun LegendSwatch(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun Gap(height: Int = 4) {
    Spacer(Modifier.heightIn(min = height.dp).width(1.dp))
}
