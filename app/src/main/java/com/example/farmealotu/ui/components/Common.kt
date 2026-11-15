package com.example.farmealotu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.domain.LabeledValue
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.DemoStripe
import com.example.farmealotu.ui.theme.LocalFarmDarkTheme
import com.example.farmealotu.ui.theme.OnDemoStripe
import com.example.farmealotu.viewmodel.UiState

// ---------------------------------------------------------------- andamiaje de pantalla

/**
 * Marco común: barra superior con «volver» e «Inicio», franja de modo demo persistente
 * y aviso de apoyo a la decisión del agricultor.
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
    LaunchedEffect(scrollResetKey) { scrollState.scrollTo(0) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        // La barra de navegación inferior (Scaffold externo) ya consume el inset del sistema;
        // si este Scaffold lo vuelve a aplicar queda una franja vacía sobre la barra.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                FarmTopBar(title = title, subtitle = subtitle, onBack = onBack, onHome = onHome)
                if (demoMode) DemoBanner()
            }
        },
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

/**
 * Barra superior: el título obligatorio de cada módulo se lee completo (hasta 3 líneas, también con
 * letra grande). «Volver» e «Inicio» son íconos de 48 dp para no quitarle ancho al título.
 */
@Composable
private fun FarmTopBar(title: String, subtitle: String?, onBack: (() -> Unit)?, onHome: (() -> Unit)?) {
    val colors = farmTopBarColors()
    Surface(color = colors.first, contentColor = colors.second, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 64.dp)
                .padding(start = if (onBack == null) 16.dp else 4.dp, end = if (onHome == null) 16.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(FarmIcons.ArrowBack, contentDescription = "Volver") }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onBack != null) 4.dp else 0.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.second.copy(alpha = 0.88f),
                    )
                }
            }
            if (onHome != null) {
                IconButton(onClick = onHome) { Icon(FarmIcons.Home, contentDescription = "Ir al inicio") }
            }
        }
    }
}

/** Verde de cultivo en tema claro; verde hondo en tema oscuro (un verde claro de fondo encandilaría). */
@Composable
private fun farmTopBarColors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return if (LocalFarmDarkTheme.current) scheme.primaryContainer to scheme.onPrimaryContainer
    else scheme.primary to scheme.onPrimary
}

/** Franja persistente de MODO DEMO */
@Composable
fun DemoBanner() {
    Surface(color = DemoStripe, contentColor = OnDemoStripe, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MODO DEMO · DATOS DE EJEMPLO",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics { contentDescription = "Modo demo activo con datos de ejemplo" },
        )
    }
}

// ---------------------------------------------------------------- bloques básicos

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSurface),
        border = border,
        // Profundidad plana (DESIGN.md, «The Flat Tonal Rule»): borde fino y tono, sin sombra.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}

/**
 * Tarjeta plegable: el título es un botón grande que abre o cierra el detalle. Sirve para que el
 * agricultor vea primero lo esencial y abra solo lo que quiera. Cerrada por defecto; recuerda su estado
 * al rotar la pantalla. TalkBack anuncia «abierto» / «cerrado».
 */
@Composable
fun FoldCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val chevron by animateFloatAsState(if (expanded) 90f else 0f, FarmMotion.enter(FarmMotion.MEDIUM), label = "flecha")
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSurface),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Cerrar" else "Abrir") { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "abierto" else "cerrado" }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Icon(
                FarmIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp).rotate(chevron),
            )
        }
        AnimatedVisibility(visible = expanded, enter = FarmMotion.revealEnter, exit = FarmMotion.revealExit) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/**
 * Botón desplegable liviano para usar DENTRO de una tarjeta: una fila «Ver …» con flecha que abre el
 * detalle en el mismo lugar (sin otra tarjeta encima). Cerrado por defecto; ≥ 48 dp; TalkBack anuncia el estado.
 */
@Composable
fun FoldRow(
    label: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(initiallyExpanded) }
    val chevron by animateFloatAsState(if (expanded) 90f else 0f, FarmMotion.enter(FarmMotion.MEDIUM), label = "flecha")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Cerrar" else "Abrir") { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "abierto" else "cerrado" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                FarmIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).rotate(chevron),
            )
        }
        AnimatedVisibility(visible = expanded, enter = FarmMotion.revealEnter, exit = FarmMotion.revealExit) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ButtonIcon(icon)
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        ButtonIcon(icon)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Ícono decorativo antes del texto de un botón: el texto ya nombra la acción para TalkBack. */
@Composable
private fun ButtonIcon(icon: ImageVector?) {
    if (icon == null) return
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
}

/**
 * Pie común de los módulos: acciones de salida, más discretas que la acción principal de la pantalla.
 * (El aviso de que la decisión es del agricultor vive una sola vez en «Fuentes y límites».)
 */
@Composable
fun FooterActions(onEditData: () -> Unit, onHome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuietAction("Editar mis datos", FarmIcons.Edit, onEditData, Modifier.weight(1f))
            QuietAction("Volver al inicio", FarmIcons.Home, onHome, Modifier.weight(1f))
        }
    }
}

/** Acción secundaria de bajo peso visual (botón de texto con ícono, ≥ 48 dp). */
@Composable
fun QuietAction(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        ButtonIcon(icon)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Aviso obligatorio del plan (§6), con el texto exacto. */
@Composable
fun DecisionDisclaimer(modifier: Modifier = Modifier) {
    Text(
        text = "Esta herramienta apoya tu decisión; no reemplaza tu experiencia ni a tu técnico.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp),
    )
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

/** Etiqueta de procedencia en lenguaje sencillo */
fun provenanceLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "documented", "observed" -> "Documentado / observado"
    "derived" -> "Calculado por FarmealoTu"
    "assumption" -> "Supuesto inicial"
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

/** Semáforo de tres niveles: forma + texto */
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
        Text("$prefix: ${level.text}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            fontWeight = FontWeight.SemiBold,
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
                Text("•", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics { })
                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- «¿Qué significa?»

/** Botón que abre la explicación de un indicador */
@Composable
fun ExplainButton(
    entry: GlossaryEntry,
    modifier: Modifier = Modifier,
    details: List<LabeledValue> = emptyList(),
    label: String = "¿Qué significa?",
) {
    var open by rememberSaveable(entry.title) { mutableStateOf(false) }
    // Alineado al borde del texto de la tarjeta (sin la sangría de 12 dp del TextButton).
    TextButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        ButtonIcon(FarmIcons.Help)
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Entendido", fontWeight = FontWeight.Bold) } },
            title = { Text(entry.title, fontWeight = FontWeight.Bold) },
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
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
        )
    }
}

/** Fuente, fecha, unidad y escala de un dato */
fun traceDetails(source: String?, date: String?, unit: String?, scale: String?, status: String? = null): List<LabeledValue> =
    listOf(
        LabeledValue("Fuente", source ?: "no informada"),
        LabeledValue("Fecha / período", formatApiDate(date) ?: "no informada"),
        LabeledValue("Unidad", unit ?: "no informada"),
        LabeledValue("Escala", scale ?: "no informada"),
    ) + listOfNotNull(status?.let { LabeledValue("Procedencia", provenanceLabel(it)) })

// ---------------------------------------------------------------- estados

@Composable
fun LoadingBlock(text: String = "Cargando datos…") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
        Text(failure.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(failure.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        if (onRetry != null) PrimaryButton("Reintentar", onRetry)
        extra?.invoke(this)
    }
}

@Composable
fun EmptyBlock(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    SectionCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium)
        action?.invoke()
    }
}

/** Fecha en que la app consultó el dato */
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
                    "Sin actualizar: ves lo último guardado.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("${error.title}. ${error.message}", style = MaterialTheme.typography.bodySmall)
                if (onRetry != null) SecondaryButton("Reintentar", onRetry)
            }
        }
    }
}

/** Dibuja carga / error / vacío y delega el contenido */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    loadingText: String = "Cargando…",
    emptyTitle: String = "Todavía no hay datos",
    emptyMessage: String = "Sin datos para mostrar.",
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
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Gap(height: Int = 4) {
    Spacer(Modifier.heightIn(min = height.dp).width(1.dp))
}
