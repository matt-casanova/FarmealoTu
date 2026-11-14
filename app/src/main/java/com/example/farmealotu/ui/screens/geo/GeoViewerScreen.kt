package com.example.farmealotu.ui.screens.geo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.LayerAvailability
import com.example.farmealotu.domain.LayerKind
import com.example.farmealotu.domain.MapLayer
import com.example.farmealotu.domain.PilotArea
import com.example.farmealotu.ui.components.ErrorBlock
import com.example.farmealotu.ui.components.ExplainButton
import com.example.farmealotu.ui.components.FarmScreen
import com.example.farmealotu.ui.components.FreshnessNote
import com.example.farmealotu.ui.components.Glossary
import com.example.farmealotu.ui.components.GlossaryEntry
import com.example.farmealotu.ui.components.InfoRow
import com.example.farmealotu.ui.components.LegendSwatch
import com.example.farmealotu.ui.components.LoadingBlock
import com.example.farmealotu.ui.components.Pill
import com.example.farmealotu.ui.components.ProvenanceChip
import com.example.farmealotu.ui.components.SectionCard
import com.example.farmealotu.ui.components.formatApiDate
import com.example.farmealotu.ui.components.formatCoordinate
import com.example.farmealotu.ui.components.traceDetails
import com.example.farmealotu.ui.theme.ChartPalette
import com.example.farmealotu.ui.theme.EarthContainer
import com.example.farmealotu.ui.theme.FieldGreen
import com.example.farmealotu.viewmodel.AppViewModelFactory
import com.example.farmealotu.viewmodel.GeoViewModel
import com.example.farmealotu.viewmodel.UiState
import kotlin.math.abs

private fun LayerKind.glossary(): GlossaryEntry = when (this) {
    LayerKind.RAIN -> GlossaryEntry(
        "Lluvia", "Cuánta agua cayó en la zona en el período indicado.",
        "Con NASA POWER es un solo valor para un área de unos 50 km; no es la lluvia de tu campo.",
    )
    LayerKind.SOIL_MOISTURE -> Glossary.soilMoistureSmap
    LayerKind.HEAT -> Glossary.heat
    LayerKind.VEGETATION -> Glossary.ndvi
    LayerKind.WATER -> Glossary.waterBalance
}

private fun availabilityColor(availability: LayerAvailability): Color = when (availability) {
    LayerAvailability.AVAILABLE -> FieldGreen
    LayerAvailability.REGIONAL -> EarthContainer
    LayerAvailability.UNAVAILABLE -> ChartPalette.noData
}

/**
 * «Explorar mi zona»: mapa 2D con el punto piloto (y el del agricultor si lo indicó) y una pila
 * isométrica conceptual de 5 capas. Los metadatos salen de GET /api/map/layers; la app no inventa
 * URL de teselas ni rásters. Un valor de punto se rotula «Información regional».
 */
@Composable
fun GeoViewerScreen(
    input: FarmerInput,
    demoMode: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: GeoViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val layersState by viewModel.layers.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    var selectedName by rememberSaveable { mutableStateOf(LayerKind.RAIN.name) }
    val selectedKind = LayerKind.entries.firstOrNull { it.name == selectedName } ?: LayerKind.RAIN

    // Si la API falla, igual se muestran las 5 capas, todas como «Datos no disponibles».
    val layers: List<MapLayer> = (layersState as? UiState.Success)?.data ?: LayerKind.entries.map { MapLayer.unavailable(it) }
    val selected = layers.firstOrNull { it.kind == selectedKind } ?: MapLayer.unavailable(selectedKind)

    val farmerMoved = !PilotArea.isPilotPoint(input.lat, input.lon)
    val points = buildList {
        add(MapPoint(PilotArea.LAT, PilotArea.LON, "Punto piloto", "Referencia de ${PilotArea.NAME}"))
        if (farmerMoved) add(MapPoint(input.lat, input.lon, "Tu punto", "Ubicación que indicaste"))
    }

    FarmScreen(title = "Explorar mi zona", subtitle = PilotArea.NAME, demoMode = demoMode, onBack = onBack, onHome = onHome) {
        SectionCard(title = "Mapa de la zona") {
            OsmMap(
                points = points,
                tileLayer = selected.tileUrlTemplate?.let { TileLayerSpec(selected.kind.name, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .semantics {
                        contentDescription = "Mapa de la zona de ${PilotArea.NAME} con el punto piloto" +
                            if (farmerMoved) " y el punto que indicaste" else ""
                    },
            )
            Text("Mapa base © colaboradores de OpenStreetMap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            InfoRow("Punto piloto", "${formatCoordinate(PilotArea.LAT)}, ${formatCoordinate(PilotArea.LON)}")
            if (farmerMoved) InfoRow("Tu punto", "${formatCoordinate(input.lat)}, ${formatCoordinate(input.lon)}")
            Text(
                "Los datos son de la zona (decenas de kilómetros). El punto es una referencia, no los límites de tu campo. " +
                    "Si no hay internet el mapa base no se ve; las coordenadas siguen valiendo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (val state = layersState) {
            UiState.Idle, UiState.Loading -> LoadingBlock("Consultando las capas disponibles…")
            is UiState.Error -> ErrorBlock(state.failure, onRetry = { viewModel.load(force = true) })
            is UiState.Success -> FreshnessNote(state, onRetry = { viewModel.load(force = true) })
            UiState.Empty -> Unit
        }

        LayerStackCard(layers, selectedKind) { selectedName = it.name }
        LayerPanel(selected)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerStackCard(layers: List<MapLayer>, selected: LayerKind, onSelect: (LayerKind) -> Unit) {
    SectionCard(title = "Las 5 capas") {
        Text("Tocá una capa para ver de dónde sale y qué significa.", style = MaterialTheme.typography.bodyMedium)
        IsometricLayerStack(layers, selected, onSelect)
        // Mismo selector como botones (accesible con lector de pantalla).
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            layers.forEach { layer ->
                FilterChip(
                    selected = layer.kind == selected,
                    onClick = { onSelect(layer.kind) },
                    label = { Text(layer.kind.title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LayerAvailability.entries.forEach { LegendSwatch(availabilityColor(it), it.label) }
        }
    }
}

/**
 * Pila isométrica conceptual: 5 rombos escalonados dibujados en Canvas (sin motor 3D).
 * Sirve para navegar entre capas; no representa datos espaciales ni texturas de medición.
 */
@Composable
private fun IsometricLayerStack(layers: List<MapLayer>, selected: LayerKind, onSelect: (LayerKind) -> Unit) {
    val measurer = rememberTextMeasurer()
    val titleStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ChartPalette.ink)
    val statusStyle = TextStyle(fontSize = 13.sp, color = ChartPalette.inkSecondary)
    val description = "Pila de 5 capas: " + layers.joinToString(", ") { "${it.kind.title} (${it.availability.label})" } +
        ". Seleccionada: ${selected.title}."

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .semantics { contentDescription = description }
            .pointerInput(layers) {
                detectTapGestures { tap ->
                    val geometry = StackGeometry(size.width.toFloat(), size.height.toFloat(), layers.size)
                    // De arriba hacia abajo: la capa superior tapa a las de abajo.
                    val hit = layers.indices.firstOrNull { geometry.contains(it, tap) }
                        ?: layers.indices.minByOrNull { abs(geometry.centerY(it) - tap.y) }
                    hit?.let { onSelect(layers[it].kind) }
                }
            }
    ) {
        val geometry = StackGeometry(size.width, size.height, layers.size)
        // Se dibuja de abajo hacia arriba para que cada capa se apoye sobre la anterior.
        for (index in layers.indices.reversed()) {
            val layer = layers[index]
            val isSelected = layer.kind == selected
            val lift = if (isSelected) -6.dp.toPx() else 0f
            val center = Offset(geometry.centerX, geometry.centerY(index) + lift)
            val shadow = geometry.rhombus(center + Offset(0f, 7.dp.toPx()))
            val shape = geometry.rhombus(center)
            drawPath(shadow, Color(0x33000000))
            drawPath(shape, availabilityColor(layer.availability))
            drawPath(
                shape,
                color = if (isSelected) ChartPalette.ink else ChartPalette.axis,
                style = Stroke(if (isSelected) 3.dp.toPx() else 1.dp.toPx()),
            )
            // Etiqueta a la derecha: nombre + estado en texto (no depende del color).
            val labelX = geometry.centerX + geometry.halfWidth + 10.dp.toPx()
            val title = measurer.measure("${index + 1} · ${layer.kind.title}", titleStyle)
            val status = measurer.measure(layer.availability.label, statusStyle)
            val top = center.y - (title.size.height + status.size.height) / 2f
            drawText(title, topLeft = Offset(labelX, top))
            drawText(status, topLeft = Offset(labelX, top + title.size.height))
            if (isSelected) {
                drawLine(ChartPalette.ink, Offset(geometry.centerX + geometry.halfWidth, center.y), Offset(labelX - 3.dp.toPx(), center.y), 2.dp.toPx())
            }
        }
    }
}

/** Geometría de la pila: rombos centrados a la izquierda, etiquetas a la derecha. */
private class StackGeometry(width: Float, height: Float, private val count: Int) {
    val halfWidth = width * 0.26f
    val halfHeight = halfWidth * 0.36f
    val centerX = halfWidth + width * 0.03f
    private val top = halfHeight + height * 0.04f
    private val spacing = if (count > 1) (height - 2 * top - height * 0.04f) / (count - 1) else 0f

    fun centerY(index: Int): Float = top + spacing * index

    fun rhombus(center: Offset): Path = Path().apply {
        moveTo(center.x, center.y - halfHeight)
        lineTo(center.x + halfWidth, center.y)
        lineTo(center.x, center.y + halfHeight)
        lineTo(center.x - halfWidth, center.y)
        close()
    }

    fun contains(index: Int, point: Offset): Boolean =
        abs(point.x - centerX) / halfWidth + abs(point.y - centerY(index)) / halfHeight <= 1f
}

@Composable
private fun LayerPanel(layer: MapLayer) {
    SectionCard(
        title = "Capa: ${layer.kind.title}",
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Pill(
            text = layer.availability.label,
            container = when (layer.availability) {
                LayerAvailability.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
                LayerAvailability.REGIONAL -> MaterialTheme.colorScheme.secondaryContainer
                LayerAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer
            },
        )
        when (layer.availability) {
            LayerAvailability.UNAVAILABLE -> Text(
                "No hay datos válidos de esta capa. No se dibuja nada en su lugar.",
                style = MaterialTheme.typography.bodyLarge,
            )
            LayerAvailability.REGIONAL -> {
                Text(
                    text = layer.valueText?.let { "Valor de la zona: $it" + (layer.unit?.let { u -> " $u" } ?: "") } ?: "Valor de la zona: no informado",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Es un solo valor para toda la zona. No se convierte en un mapa de colores por parcela.",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                )
            }
            LayerAvailability.AVAILABLE -> {
                val drawable = layer.tileUrlTemplate?.let { isDrawableTileTemplate(it) } == true
                Text(
                    text = if (drawable) "La capa se dibuja sobre el mapa de arriba."
                    else "El servidor publicó esta capa, pero en un formato que esta versión todavía no dibuja sobre el mapa.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (layer.kind == LayerKind.WATER) {
            Text(
                "«Agua» es un balance calculado (lluvia efectiva menos lo que pide el cultivo). No es una inundación observada.",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            )
        }
        layer.interpretation?.let {
            Text("Qué te dice", style = MaterialTheme.typography.labelLarge)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        ProvenanceChip(layer.evidenceStatus ?: if (layer.availability == LayerAvailability.UNAVAILABLE) "unavailable" else null)
        InfoRow("Fuente", layer.source ?: "no informada")
        InfoRow("Fecha", formatApiDate(layer.date) ?: "no informada")
        InfoRow("Resolución", layer.resolution ?: "no informada")
        InfoRow("Unidad", layer.unit ?: "no informada")
        if (layer.legend.isNotEmpty()) {
            Text("Leyenda", style = MaterialTheme.typography.labelLarge)
            layer.legend.forEach { entry ->
                val color = entry.colorHex?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() }
                if (color != null) LegendSwatch(color, entry.label) else Text("• ${entry.label}", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            InfoRow("Leyenda", "no informada por el servidor")
        }
        layer.attribution?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        val entry = layer.kind.glossary()
        ExplainButton(
            entry = if (layer.explanation != null) entry.copy(body = layer.explanation) else entry,
            details = traceDetails(layer.source, layer.date, layer.unit, layer.resolution, layer.evidenceStatus),
        )
    }
}
