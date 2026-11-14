package com.example.farmealotu.ui.screens.input

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.CropMatch
import com.example.farmealotu.domain.CropNormalizer
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.domain.PilotArea
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.InfoRow
import com.example.farmealotu.ui.components.PrimaryButton
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.SecondaryButton
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatCoordinate
import com.example.farmealotu.ui.components.formatPercentWeight
import com.example.farmealotu.viewmodel.MainViewModel

private const val SUMMARY_STEP = 5
private val CROP_OPTIONS = listOf(Crop.SOY, Crop.MAIZE, Crop.SORGHUM, Crop.SUNFLOWER, Crop.WHEAT)

/**
 * Las 5 preguntas, una por pantalla, y un resumen editable antes de consultar al servidor.
 * Nada de lo que escribe el agricultor se interpreta con IA: los cultivos se reconocen con un
 * diccionario fijo y, si no se reconocen, se pide elegir de la lista.
 */
@Composable
fun FarmerInputScreen(
    viewModel: MainViewModel,
    demoMode: Boolean,
    startAtSummary: Boolean,
    onExit: () -> Unit,
    onHome: () -> Unit,
    onCompare: () -> Unit,
) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val priorities by viewModel.priorities.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(if (startAtSummary && input.isComplete) SUMMARY_STEP else 0) }
    var returnToSummary by rememberSaveable { mutableStateOf(false) }
    var currentText by rememberSaveable { mutableStateOf("") }
    var previousText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun goTo(target: Int) {
        error = null
        step = target
    }

    fun advance() {
        if (returnToSummary) {
            returnToSummary = false
            goTo(SUMMARY_STEP)
        } else {
            goTo(step + 1)
        }
    }

    fun back() {
        when {
            returnToSummary -> {
                returnToSummary = false
                goTo(SUMMARY_STEP)
            }
            step == 0 -> onExit()
            else -> goTo(step - 1)
        }
    }

    BackHandler(onBack = ::back)

    FarmScreen(
        title = if (step == SUMMARY_STEP) "Revisá tus datos" else "Contanos de tu campo",
        demoMode = demoMode,
        onBack = ::back,
        onHome = onHome,
        scrollResetKey = step,
    ) {
        if (step < SUMMARY_STEP) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pregunta ${step + 1} de 5", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
            }
        }

        when (step) {
            0 -> CropStep(
                question = "¿Qué sembrás actualmente?",
                text = currentText,
                selected = input.currentCrop,
                options = CROP_OPTIONS,
                allowFallow = false,
                error = error,
                onTextChange = { text ->
                    currentText = text
                    error = null
                    viewModel.setCurrentCrop((CropNormalizer.match(text, allowFallow = false) as? CropMatch.Found)?.crop)
                },
                onPick = { crop ->
                    currentText = ""
                    error = null
                    viewModel.setCurrentCrop(crop)
                },
                onNext = {
                    error = cropError(input.currentCrop, currentText, allowFallow = false)
                    if (error == null) advance()
                },
            )
            1 -> CropStep(
                question = "¿Qué sembraste antes?",
                text = previousText,
                selected = input.previousCrop,
                options = CROP_OPTIONS + Crop.FALLOW,
                allowFallow = true,
                error = error,
                onTextChange = { text ->
                    previousText = text
                    error = null
                    viewModel.setPreviousCrop((CropNormalizer.match(text) as? CropMatch.Found)?.crop)
                },
                onPick = { crop ->
                    previousText = ""
                    error = null
                    viewModel.setPreviousCrop(crop)
                },
                onNext = {
                    error = cropError(input.previousCrop, previousText, allowFallow = true)
                    if (error == null) advance()
                },
            )
            2 -> ChoiceStep(
                question = "¿Qué te preocupa más?",
                hint = "Con esto elegimos tus prioridades de partida. Después las podés cambiar.",
                options = Concern.entries.map { it.label },
                selectedIndex = input.concern?.ordinal,
                error = error,
                onSelect = { viewModel.setConcern(Concern.entries[it]); error = null },
                onNext = { if (input.concern == null) error = "Elegí una opción para seguir." else advance() },
            )
            3 -> ChoiceStep(
                question = "¿Tenés riego?",
                hint = null,
                options = Irrigation.entries.map { it.label },
                selectedIndex = input.irrigation?.ordinal,
                error = error,
                onSelect = { viewModel.setIrrigation(Irrigation.entries[it]); error = null },
                onNext = { if (input.irrigation == null) error = "Elegí una opción para seguir." else advance() },
            )
            4 -> NotesStep(notes = input.notes, onNotesChange = viewModel::setNotes, onNext = ::advance)
            else -> SummaryStep(
                input = input,
                priorities = priorities,
                onEditStep = { target ->
                    returnToSummary = true
                    goTo(target)
                },
                onToggleExcluded = viewModel::toggleExcludedCrop,
                onSetLocation = viewModel::setLocation,
                onResetLocation = viewModel::resetLocation,
                onCompare = {
                    viewModel.submitCompare()
                    onCompare()
                },
            )
        }
    }
}

/** Mensaje de error del paso de cultivo, o null si se puede avanzar. Nunca adivina el cultivo. */
private fun cropError(selected: Crop?, text: String, allowFallow: Boolean): String? {
    if (selected != null) return null
    return when (val match = CropNormalizer.match(text, allowFallow)) {
        is CropMatch.Found -> null
        CropMatch.Empty -> "Elegí un cultivo de la lista o escribilo."
        is CropMatch.Unknown -> "No reconocí «${match.text}». Elegí un cultivo de la lista."
        is CropMatch.Ambiguous -> "Nombraste más de un cultivo (${match.crops.joinToString { it.label }}). Elegí uno de la lista."
    }
}

@Composable
private fun QuestionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
}

@Composable
private fun StepError(error: String?) {
    if (error != null) {
        Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CropStep(
    question: String,
    text: String,
    selected: Crop?,
    options: List<Crop>,
    allowFallow: Boolean,
    error: String?,
    onTextChange: (String) -> Unit,
    onPick: (Crop) -> Unit,
    onNext: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    QuestionTitle(question)
    Text("Tocá una opción o escribilo con tus palabras.", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { crop ->
            FilterChip(
                selected = selected == crop && text.isBlank(),
                onClick = { onPick(crop) },
                label = { Text(crop.label, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        FilterChip(
            selected = text.isNotBlank(),
            onClick = { focusRequester.requestFocus() },
            label = { Text("Otro", style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text("O escribilo acá") },
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        supportingText = {
            val match = CropNormalizer.match(text, allowFallow)
            when {
                text.isBlank() -> Text("Ejemplos: soya, maíz, sorgo, girasol, trigo" + if (allowFallow) ", barbecho" else "")
                match is CropMatch.Found -> Text("Reconocido: ${match.crop.label}")
                else -> Text("Todavía no lo reconozco. Podés elegir de la lista.")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
    StepError(error)
    PrimaryButton("Siguiente", onNext)
}

@Composable
private fun ChoiceStep(
    question: String,
    hint: String?,
    options: List<String>,
    selectedIndex: Int?,
    error: String?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    QuestionTitle(question)
    if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, label ->
            ChoiceButton(label, selected = selectedIndex == index, onClick = { onSelect(index) })
        }
    }
    StepError(error)
    PrimaryButton("Siguiente", onNext)
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (selected) "✓" else "○", style = MaterialTheme.typography.titleLarge, color = scheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NotesStep(notes: String, onNotesChange: (String) -> Unit, onNext: () -> Unit) {
    QuestionTitle("¿Querés contar algo de tu terreno?")
    Text("Es opcional. Lo guardamos tal cual lo escribís; la app no lo interpreta.", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Por ejemplo: «Mi terreno suele secarse en invierno»") },
        minLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(if (notes.isBlank()) "Seguir sin contar nada" else "Siguiente", onNext)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStep(
    input: FarmerInput,
    priorities: Priorities,
    onEditStep: (Int) -> Unit,
    onToggleExcluded: (Crop) -> Unit,
    onSetLocation: (Double, Double) -> Boolean,
    onResetLocation: () -> Unit,
    onCompare: () -> Unit,
) {
    SectionCard(title = "Tus respuestas") {
        ProvenanceChip("farmer_provided")
        SummaryRow("Sembrás ahora", input.currentCrop?.label) { onEditStep(0) }
        SummaryRow("Sembraste antes", input.previousCrop?.label) { onEditStep(1) }
        SummaryRow("Te preocupa más", input.concern?.label) { onEditStep(2) }
        SummaryRow("Riego", input.irrigation?.label) { onEditStep(3) }
        SummaryRow("Sobre tu terreno", input.notes.ifBlank { "(nada)" }) { onEditStep(4) }
    }

    SectionCard(title = "Tus prioridades de partida") {
        val normalized = priorities.normalized() ?: Priorities.DEFAULT
        InfoRow("Agua", formatPercentWeight(normalized.water))
        InfoRow("Suelo", formatPercentWeight(normalized.soil))
        InfoRow("Riesgo", formatPercentWeight(normalized.risk))
        ProvenanceChip("assumption")
        Text(
            "Salen de lo que más te preocupa. Son un punto de partida, no un valor científico: las podés cambiar después de comparar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard(title = "Cultivos que no querés sembrar (opcional)") {
        Text("Las opciones que los usen no se van a proponer.", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CROP_OPTIONS.filter { it != input.currentCrop }.forEach { crop ->
                FilterChip(
                    selected = crop in input.excludedCrops,
                    onClick = { onToggleExcluded(crop) },
                    label = { Text(crop.label, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }

    LocationCard(input, onSetLocation, onResetLocation)

    if (!input.isComplete) {
        Text("Todavía faltan respuestas. Tocá «Cambiar» en la que falte.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    PrimaryButton("Comparar opciones de siembra", onCompare, enabled = input.isComplete)
}

@Composable
private fun SummaryRow(label: String, value: String?, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value ?: "Falta responder",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (value == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cambiar") }
    }
}

@Composable
private fun LocationCard(input: FarmerInput, onSetLocation: (Double, Double) -> Boolean, onResetLocation: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var latText by rememberSaveable(input.lat) { mutableStateOf(formatCoordinate(input.lat)) }
    var lonText by rememberSaveable(input.lon) { mutableStateOf(formatCoordinate(input.lon)) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    SectionCard(title = "Ubicación de referencia") {
        Text(
            text = "Lat ${formatCoordinate(input.lat)}, Lon ${formatCoordinate(input.lon)}" +
                if (PilotArea.isPilotPoint(input.lat, input.lon)) " (punto piloto de ${PilotArea.NAME})" else " (punto que indicaste)",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Es un punto de referencia de la zona, no necesariamente tu finca. Los datos son regionales (~50 km): no tienen precisión de parcela.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!editing) {
            SecondaryButton("Cambiar ubicación", { editing = true })
        } else {
            OutlinedTextField(
                value = latText, onValueChange = { latText = it; error = null },
                label = { Text("Latitud (ej. -17.2374)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lonText, onValueChange = { lonText = it; error = null },
                label = { Text("Longitud (ej. -62.2427)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) Text(error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            SecondaryButton("Guardar ubicación", {
                val lat = latText.trim().replace(',', '.').toDoubleOrNull()
                val lon = lonText.trim().replace(',', '.').toDoubleOrNull()
                when {
                    lat == null || lon == null -> error = "Escribí la latitud y la longitud con números, por ejemplo -17.2374."
                    !onSetLocation(lat, lon) -> error = "Ese punto queda fuera del área piloto de ${PilotArea.NAME} (hasta medio grado del punto de referencia)."
                    else -> editing = false
                }
            })
            SecondaryButton("Usar el punto piloto", {
                onResetLocation()
                error = null
                editing = false
            })
        }
    }
}
