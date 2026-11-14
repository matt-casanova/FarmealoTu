package com.example.farmealotu.data.model

import com.example.farmealotu.domain.Alert
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.Criteria
import com.example.farmealotu.domain.CriterionValue
import com.example.farmealotu.domain.CropNormalizer
import com.example.farmealotu.domain.Evidence
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.FieldReadings
import com.example.farmealotu.domain.HealthInfo
import com.example.farmealotu.domain.HeatCell
import com.example.farmealotu.domain.HeatmapSeries
import com.example.farmealotu.domain.IndicatorSummary
import com.example.farmealotu.domain.IndicatorsResult
import com.example.farmealotu.domain.InfoSection
import com.example.farmealotu.domain.LayerAvailability
import com.example.farmealotu.domain.LayerKind
import com.example.farmealotu.domain.LegendEntry
import com.example.farmealotu.domain.MapLayer
import com.example.farmealotu.domain.NasaStatus
import com.example.farmealotu.domain.NodeReading
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.NodeStatus
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.domain.RegionalContext
import com.example.farmealotu.domain.SequenceStep
import com.example.farmealotu.domain.SourceRef
import com.example.farmealotu.domain.Strategy
import com.example.farmealotu.domain.Tradeoffs
import com.example.farmealotu.domain.Uncertainty
import com.example.farmealotu.domain.ZoneReading
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.round

/**
 * DTO/JSON -> modelos de dominio. Aquí NO se calcula ningún puntaje: solo se leen los valores
 * que manda el backend. Los alias aceptados están listados en docs/UI_BACKEND_CONTRACT.md.
 */
object DtoMappers {

    // ---------- Request ----------

    /** @throws IllegalArgumentException si faltan respuestas o los pesos no se pueden normalizar. */
    fun toCompareRequest(input: FarmerInput, priorities: Priorities): CompareRequestDto {
        require(input.isComplete) { "Faltan respuestas del agricultor" }
        val normalized = requireNotNull(priorities.normalized()) { "Los pesos no se pueden normalizar" }
        val water = round4(normalized.water)
        val soil = round4(normalized.soil)
        val risk = round4(1.0 - water - soil).coerceAtLeast(0.0)
        return CompareRequestDto(
            site = SiteDto(lat = input.lat, lon = input.lon),
            currentRotation = input.currentRotationCodes,
            priorities = PrioritiesDto(water = water, soil = soil, risk = risk),
            constraints = ConstraintsDto(
                irrigation = input.irrigation!!.apiCode,
                excludedCrops = input.excludedCrops.map { it.apiCode }.sorted(),
            ),
            notes = input.notes.trim(),
        )
    }

    private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0

    // ---------- Comparación ----------

    fun toComparison(dto: CompareResponseDto): ComparisonResult {
        val statusObject = dto.dataStatus.asObjectOrNull()
        return ComparisonResult(
            siteLat = dto.site?.lat,
            siteLon = dto.site?.lon,
            lastObservationDate = dto.dataStatus.findFirstText(LAST_OBSERVATION_KEYS),
            downloadedAt = dto.dataStatus.findFirstText(DOWNLOAD_KEYS),
            dataStatus = (statusObject ?: dto.dataStatus).flattenToLabeledValues(),
            alerts = toAlerts(dto.alerts),
            strategies = dto.strategies.orEmpty().mapIndexedNotNull { index, strategy -> toStrategy(strategy, index) },
            recommendedId = dto.recommendedStrategyId?.trim()?.takeIf { it.isNotEmpty() },
            limitations = dto.limitations.asStringList(),
        )
    }

    private fun toStrategy(dto: StrategyDto, index: Int): Strategy? {
        val id = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val criteria = toCriteria(dto.criteria)
        val score = dto.score.asDoubleOrNull()
            ?: dto.score.asObjectOrNull()?.firstDouble("value", "median", "score", "p50")
        return Strategy(
            id = id,
            name = dto.name?.trim().orEmpty().ifEmpty { "Opción ${index + 1}" },
            sequence = toSequence(dto.sequence),
            score = score,
            partialScore = dto.partialScore ?: (score != null && criteria.hasMissing),
            criteria = criteria,
            uncertainty = dto.uncertainty?.let {
                Uncertainty(
                    p10 = it.p10,
                    p50 = it.p50,
                    p90 = it.p90,
                    rankStability = it.rankStability,
                    confidenceLevel = it.confidenceLevel?.trim()?.takeIf { text -> text.isNotEmpty() },
                    confidenceReason = it.confidenceReason?.trim()?.takeIf { text -> text.isNotEmpty() },
                )
            },
            reasons = dto.reasons.asStringList(),
            evidence = dto.evidence.orEmpty().mapNotNull { toEvidence(it) },
            tradeoffs = toTradeoffs(dto.tradeoffs),
            missingInformation = dto.missingInformation.asStringList(),
            agronomistQuestions = dto.agronomistQuestions.asStringList(),
        )
    }

    private fun toEvidence(dto: EvidenceDto): Evidence? {
        val indicator = dto.indicator?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return Evidence(
            indicator = indicator,
            valueText = dto.value.asDisplayValue(),
            unit = dto.unit?.takeIf { it.isNotBlank() },
            source = dto.source?.takeIf { it.isNotBlank() },
            period = dto.period?.takeIf { it.isNotBlank() },
            scale = dto.scale?.takeIf { it.isNotBlank() },
            status = dto.evidenceStatus?.takeIf { it.isNotBlank() },
        )
    }

    /** Acepta ["soy","sorghum"] o [{"season":"summer","crop":"soy"}]. */
    fun toSequence(element: JsonElement?): List<SequenceStep> =
        element.asArrayOrNull().orEmpty().mapNotNull { item ->
            val asText = item.asTextOrNull()
            if (asText != null) {
                SequenceStep(season = null, cropCode = asText)
            } else {
                val obj = item.asObjectOrNull() ?: return@mapNotNull null
                val crop = obj.firstText("crop", "crop_id", "id", "name") ?: return@mapNotNull null
                SequenceStep(season = obj.firstText("season", "campaign", "campana"), cropCode = crop)
            }
        }

    /** Acepta {"water": 62.1} o {"water": {"score": 62.1, "detail": "..."}}; null se conserva. */
    fun toCriteria(element: JsonElement?): Criteria {
        val obj = element.asObjectOrNull() ?: return Criteria(null, null, null)
        fun criterion(vararg keys: String): CriterionValue? {
            val key = keys.firstOrNull { it in obj } ?: return null
            val value = obj[key]
            val nested = value.asObjectOrNull()
            return if (nested != null) {
                CriterionValue(
                    score = nested.firstDouble("score", "value", "s"),
                    detail = nested.firstText("detail", "explanation", "label", "description"),
                    unit = nested.firstText("unit"),
                    status = nested.firstText("status", "evidence_status"),
                )
            } else {
                CriterionValue(score = value.asDoubleOrNull())
            }
        }
        return Criteria(
            water = criterion("water", "agua", "s_agua", "S_agua", "s_water"),
            soil = criterion("soil", "suelo", "s_suelo", "S_suelo", "s_soil"),
            risk = criterion("risk", "riesgo", "s_riesgo", "S_riesgo", "s_risk"),
        )
    }

    fun toTradeoffs(element: JsonElement?): Tradeoffs {
        val obj = element.asObjectOrNull()
            ?: return Tradeoffs(null, null, null, extra = element.asStringList())
        return Tradeoffs(
            advantage = obj.firstText("advantage", "main_advantage", "benefit", "main_benefit", "ventaja", "pro"),
            disadvantage = obj.firstText("disadvantage", "main_disadvantage", "drawback", "desventaja", "con"),
            mainRisk = obj.firstText("main_risk", "risk", "riesgo", "riesgo_principal"),
        )
    }

    fun toAlerts(element: JsonElement?): List<Alert> =
        element.asArrayOrNull().orEmpty().mapNotNull { item ->
            val asText = item.asTextOrNull()
            if (asText != null) {
                Alert(message = asText, date = null, level = null, source = null, evidenceStatus = null)
            } else {
                val obj = item.asObjectOrNull() ?: return@mapNotNull null
                val message = obj.firstText("message", "text", "title", "description") ?: return@mapNotNull null
                Alert(
                    message = message,
                    date = obj.firstText("date", "period", "as_of", "computed_at"),
                    level = obj.firstText("level", "severity"),
                    source = obj.firstText("source"),
                    evidenceStatus = obj.firstText("evidence_status", "status"),
                )
            }
        }

    // ---------- Salud y estado NASA ----------

    fun toHealth(element: JsonElement?): HealthInfo {
        val obj = element.asObjectOrNull()
        val statusText = obj?.firstText("status", "state") ?: element.asTextOrNull()
        val okFlag = obj?.get("ok").asBooleanOrNull()
        val ok = okFlag ?: (statusText == null || statusText.lowercase() in setOf("ok", "healthy", "up", "alive", "running"))
        return HealthInfo(ok = ok, detail = listOfNotNull(statusText, obj?.firstText("version")).joinToString(" · ").ifEmpty { null })
    }

    private val LAST_OBSERVATION_KEYS = listOf(
        "last_observation_date", "last_observation", "last_valid_date", "latest_observation_date", "latest_observation",
    )
    private val DOWNLOAD_KEYS = listOf(
        "last_download", "last_successful_download", "downloaded_at", "download_date", "fetched_at",
    )
    private val CACHE_KEYS = listOf("uses_cache", "cache_used", "from_cache", "cached")

    fun toNasaStatus(element: JsonElement?): NasaStatus = NasaStatus(
        lastObservationDate = element.findFirstText(LAST_OBSERVATION_KEYS),
        lastDownload = element.findFirstText(DOWNLOAD_KEYS),
        usesCache = element.findFirst(CACHE_KEYS).asBooleanOrNull(),
        details = element.flattenToLabeledValues(),
    )

    /** Busca la primera clave conocida, primero en el nivel actual y luego hacia adentro. */
    private fun JsonElement?.findFirst(keys: List<String>, depth: Int = 3): JsonElement? {
        when (val element = this.orNullIfJsonNull()) {
            is JsonObject -> {
                keys.firstNotNullOfOrNull { element[it].orNullIfJsonNull() }?.let { return it }
                if (depth > 0) element.values.forEach { child -> child.findFirst(keys, depth - 1)?.let { return it } }
            }
            is JsonArray -> if (depth > 0) element.forEach { child -> child.findFirst(keys, depth - 1)?.let { return it } }
            else -> Unit
        }
        return null
    }

    private fun JsonElement?.findFirstText(keys: List<String>): String? = findFirst(keys).asTextOrNull()

    // ---------- Indicadores y grilla mensual ----------

    fun toIndicators(root: JsonElement?): IndicatorsResult {
        val obj = root.asObjectOrNull()
        val summaries = toSummaries(obj?.first("indicators", "summary", "metrics") ?: root.asArrayOrNull())
        val gridElement = obj?.first("monthly_grid", "grid", "heatmap", "heatmap_grid", "monthly")
        return IndicatorsResult(
            lastObservationDate = root.findFirstText(LAST_OBSERVATION_KEYS),
            period = obj?.firstText("period"),
            summaries = summaries,
            grids = toGrids(gridElement, summaries),
        )
    }

    private fun toSummaries(element: JsonElement?): List<IndicatorSummary> = when (val value = element.orNullIfJsonNull()) {
        is JsonArray -> value.mapNotNull { item -> item.asObjectOrNull()?.let { toSummary(it, fallbackId = null) } }
        is JsonObject -> value.entries.mapNotNull { (key, item) ->
            val nested = item.asObjectOrNull()
            when {
                nested != null -> toSummary(nested, fallbackId = key)
                item is JsonPrimitive -> IndicatorSummary(
                    id = key, name = prettyKey(key), valueText = item.asDisplayValue(), unit = null, period = null,
                    source = null, scale = null, status = if (item.asDisplayValue() == null) "unavailable" else null,
                    validDays = null, missingPct = null, description = null, level = null,
                )
                else -> null
            }
        }
        else -> emptyList()
    }

    private fun toSummary(obj: JsonObject, fallbackId: String?): IndicatorSummary? {
        val id = obj.firstText("id", "key", "indicator") ?: fallbackId ?: return null
        val quality = obj["quality"].asObjectOrNull()
        return IndicatorSummary(
            id = id,
            name = obj.firstText("name", "label", "title") ?: prettyKey(id),
            valueText = obj["value"].asDisplayValue(),
            unit = obj.firstText("unit"),
            period = obj.firstText("period"),
            source = obj.firstText("source"),
            scale = obj.firstText("scale", "spatial_scale"),
            status = obj.firstText("evidence_status", "status"),
            validDays = (quality?.first("valid_days", "n_valid_days") ?: obj["valid_days"]).asIntOrNull(),
            missingPct = quality?.firstDouble("missing_pct", "pct_missing") ?: obj.firstDouble("missing_pct"),
            description = obj.firstText("description", "explanation", "meaning", "limitation"),
            level = obj.firstText("level", "risk_level", "severity"),
        )
    }

    private fun toGrids(element: JsonElement?, summaries: List<IndicatorSummary>): List<HeatmapSeries> {
        fun series(id: String, meta: JsonObject?, cells: List<HeatCell>): HeatmapSeries? {
            if (cells.isEmpty()) return null
            val summary = summaries.firstOrNull { it.id == id }
            return HeatmapSeries(
                id = id,
                name = meta?.firstText("name", "label", "title") ?: summary?.name ?: prettyKey(id),
                unit = meta?.firstText("unit") ?: summary?.unit,
                source = meta?.firstText("source") ?: summary?.source,
                scale = meta?.firstText("scale", "spatial_scale") ?: summary?.scale,
                status = meta?.firstText("evidence_status", "status") ?: summary?.status,
                description = meta?.firstText("description", "explanation", "meaning") ?: summary?.description,
                cells = cells.sortedWith(compareBy({ it.year }, { it.month })),
            )
        }

        fun seriesFromObject(obj: JsonObject, fallbackId: String?): HeatmapSeries? {
            val id = obj.firstText("id", "key", "indicator") ?: fallbackId ?: return null
            val cellsElement = obj.first("cells", "values", "data", "grid")
            val cells = cellsElement.asArrayOrNull()?.mapNotNull { toCell(it.asObjectOrNull(), "value") }
                ?: yearMapCells(cellsElement.asObjectOrNull() ?: obj)
            return series(id, obj, cells)
        }

        fun fromFlatCells(array: JsonArray): List<HeatmapSeries> {
            val objects = array.mapNotNull { it.asObjectOrNull() }
            if (objects.any { it.firstText("indicator", "indicator_id") != null }) {
                return objects.groupBy { it.firstText("indicator", "indicator_id") }
                    .mapNotNull { (id, items) -> id?.let { series(it, null, items.mapNotNull { cell -> toCell(cell, "value") }) } }
            }
            val reserved = setOf(
                "year", "month", "period", "date", "quality", "valid_days", "missing_pct", "source", "unit", "evidence_status",
            )
            val columns = objects.flatMap { it.keys }.distinct().filter { it !in reserved }
            return columns.mapNotNull { column -> series(column, null, objects.mapNotNull { toCell(it, column) }) }
        }

        return when (val value = element.orNullIfJsonNull()) {
            is JsonArray -> {
                val objects = value.mapNotNull { it.asObjectOrNull() }
                if (objects.any { it.first("cells", "values") != null }) objects.mapNotNull { seriesFromObject(it, null) }
                else fromFlatCells(value)
            }
            is JsonObject -> {
                val listed = value.first("indicators", "series").asArrayOrNull()
                val flat = value["cells"].asArrayOrNull()
                when {
                    listed != null -> listed.mapNotNull { item -> item.asObjectOrNull()?.let { seriesFromObject(it, null) } }
                    flat != null && value.firstText("id", "indicator") == null -> fromFlatCells(flat)
                    flat != null -> listOfNotNull(seriesFromObject(value, null))
                    else -> value.entries.mapNotNull { (key, item) -> item.asObjectOrNull()?.let { seriesFromObject(it, key) } }
                }
            }
            else -> emptyList()
        }
    }

    private val YEAR_MONTH = Regex("^(\\d{4})-(\\d{1,2})")

    private fun toCell(obj: JsonObject?, valueKey: String): HeatCell? {
        obj ?: return null
        var year = obj["year"].asIntOrNull()
        var month = obj["month"].asIntOrNull()
        if (year == null || month == null) {
            val match = obj.firstText("period", "date", "month")?.let { YEAR_MONTH.find(it) }
            if (match != null) {
                year = match.groupValues[1].toIntOrNull()
                month = match.groupValues[2].toIntOrNull()
            }
        }
        if (year == null || month == null || month !in 1..12) return null
        if (valueKey !in obj && valueKey != "value") return null
        return HeatCell(
            year = year,
            month = month,
            value = obj[valueKey].asDoubleOrNull(),
            quality = obj.firstText("quality", "quality_flag"),
            validDays = obj["valid_days"].asIntOrNull(),
            missingPct = obj.firstDouble("missing_pct"),
        )
    }

    /** {"2015": [12 valores]} o {"2015": {"1": v, "2": v}}. */
    private fun yearMapCells(obj: JsonObject): List<HeatCell> = obj.entries.flatMap { (key, value) ->
        val year = key.toIntOrNull()?.takeIf { it in 1900..2100 } ?: return@flatMap emptyList()
        when (val months = value.orNullIfJsonNull()) {
            is JsonArray -> months.take(12).mapIndexed { index, item -> HeatCell(year, index + 1, item.asDoubleOrNull()) }
            is JsonObject -> months.entries.mapNotNull { (monthKey, item) ->
                monthKey.toIntOrNull()?.takeIf { it in 1..12 }?.let { HeatCell(year, it, item.asDoubleOrNull()) }
            }
            else -> emptyList()
        }
    }

    // ---------- Capas del visor ----------

    /** Siempre devuelve las 5 capas del plan; la que la API no trae queda «Datos no disponibles». */
    fun toMapLayers(root: JsonElement?): List<MapLayer> {
        val items = (root.asArrayOrNull() ?: root.asObjectOrNull()?.first("layers", "items", "data").asArrayOrNull())
            .orEmpty().mapNotNull { it.asObjectOrNull() }
        val used = mutableSetOf<JsonObject>()
        return LayerKind.entries.map { kind ->
            val match = items.firstOrNull { it !in used && layerMatches(it, kind, exact = true) }
                ?: items.firstOrNull { it !in used && layerMatches(it, kind, exact = false) }
            if (match == null) MapLayer.unavailable(kind) else {
                used += match
                toLayer(match, kind)
            }
        }
    }

    private fun layerMatches(obj: JsonObject, kind: LayerKind, exact: Boolean): Boolean {
        val id = CropNormalizer.normalizeText(obj.firstText("id", "key", "layer", "kind") ?: obj.firstText("name") ?: return false)
            .replace(' ', '_')
        return if (exact) id in kind.apiIds else kind.apiIds.any { id.contains(it) }
    }

    private fun toLayer(obj: JsonObject, kind: LayerKind): MapLayer {
        val tileUrl = obj.firstText("tile_url", "tile_url_template", "url_template", "tiles", "url")
        val valueText = obj["value"].asDisplayValue()
        val statusText = obj.firstText("status", "state", "availability")?.lowercase()
        val availableFlag = obj["available"].asBooleanOrNull()
        val availability = when {
            availableFlag == false -> LayerAvailability.UNAVAILABLE
            statusText in setOf("unavailable", "missing", "none", "error", "no_data", "not_available") -> LayerAvailability.UNAVAILABLE
            statusText in setOf("regional", "point", "regional_info", "regional_context") -> LayerAvailability.REGIONAL
            statusText in setOf("available", "ok", "ready", "raster", "tiles") ->
                if (tileUrl != null) LayerAvailability.AVAILABLE else LayerAvailability.REGIONAL
            tileUrl != null -> LayerAvailability.AVAILABLE
            valueText != null -> LayerAvailability.REGIONAL
            else -> LayerAvailability.UNAVAILABLE
        }
        return MapLayer(
            kind = kind,
            availability = availability,
            name = obj.firstText("name", "title", "label"),
            source = obj.firstText("source"),
            date = obj.firstText("date", "period", "last_observation_date", "updated_at"),
            resolution = obj.firstText("resolution", "scale", "spatial_scale", "spatial_resolution"),
            unit = obj.firstText("unit"),
            valueText = valueText,
            legend = toLegend(obj["legend"]),
            interpretation = obj.firstText("interpretation", "agronomic_interpretation"),
            explanation = obj.firstText("explanation", "what_it_means", "description", "meaning"),
            evidenceStatus = obj.firstText("evidence_status"),
            tileUrlTemplate = tileUrl.takeIf { availability == LayerAvailability.AVAILABLE },
            attribution = obj.firstText("attribution", "credit"),
        )
    }

    private fun toLegend(element: JsonElement?): List<LegendEntry> = when (val value = element.orNullIfJsonNull()) {
        is JsonArray -> value.mapNotNull { item ->
            val text = item.asTextOrNull()
            if (text != null) LegendEntry(text, null) else item.asObjectOrNull()?.let { obj ->
                val label = obj.firstText("label", "text", "name", "range")
                    ?: listOfNotNull(obj["min"].asDisplayValue(), obj["max"].asDisplayValue())
                        .joinToString(" – ").takeIf { it.isNotEmpty() }
                label?.let { LegendEntry(it, obj.firstText("color", "colour", "hex")) }
            }
        }
        is JsonObject -> value.flattenToLabeledValues().map { LegendEntry("${it.label}: ${it.value}", null) }
        is JsonPrimitive -> listOfNotNull(value.asTextOrNull()?.let { LegendEntry(it, null) })
        else -> emptyList()
    }

    // ---------- Fuentes y contexto ----------

    fun toSources(root: JsonElement?): List<SourceRef> {
        val items = root.asArrayOrNull()
            ?: root.asObjectOrNull()?.first("sources", "references", "bibliography", "items").asArrayOrNull()
        return items.orEmpty().mapNotNull { item ->
            val text = item.asTextOrNull()
            if (text != null) SourceRef(text, null, null) else item.asObjectOrNull()?.let { obj ->
                val title = obj.firstText("title", "name", "citation", "id") ?: return@let null
                val detail = listOfNotNull(
                    obj.firstText("description", "use", "usage", "notes"),
                    obj.firstText("reference_year", "year")?.let { "Año de referencia: $it" },
                    obj.firstText("geographic_scope", "scale")?.let { "Alcance: $it" },
                ).joinToString(" · ").ifEmpty { null }
                SourceRef(title, obj.firstText("url", "source_url", "link"), detail)
            }
        }
    }

    fun toContextSections(root: JsonElement?): List<InfoSection> {
        val obj = root.asObjectOrNull() ?: return emptyList()
        return obj.entries.mapNotNull { (key, value) ->
            val lines = value.flattenToLabeledValues(maxDepth = 3)
            if (lines.isEmpty()) null else InfoSection(prettyKey(key), lines)
        }
    }

    // ---------- Nodo de campo ----------

    fun toFieldReadings(root: JsonElement?, json: Json): FieldReadings {
        val items = root.asArrayOrNull()
            ?: root.asObjectOrNull()?.first("readings", "items", "data", "results").asArrayOrNull()
            ?: return FieldReadings(emptyList(), 0)
        var skipped = 0
        val readings = items.mapNotNull { item ->
            runCatching { toReading(json.decodeFromJsonElement<SensorReadingDto>(item)) }
                .getOrElse { skipped++; null }
        }
        return FieldReadings(readings.sortedBy { it.epochMillis ?: Long.MIN_VALUE }, skipped)
    }

    fun toReading(dto: SensorReadingDto): NodeReading {
        val timestamp = dto.ts?.takeIf { it.isNotBlank() } ?: dto.receivedAt?.takeIf { it.isNotBlank() }
        return NodeReading(
            nodeId = dto.nodeId,
            source = NodeSource.from(dto.source),
            scenario = dto.scenario,
            timestamp = timestamp,
            epochMillis = parseEpochMillis(timestamp),
            depthCm = dto.internal?.depthCm,
            zones = dto.internal?.zones.orEmpty().mapNotNull { zone ->
                val id = zone.id?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                ZoneReading(
                    id = id,
                    moistureVwc = zone.soilMoistureVwc?.takeIf { it.isFinite() },
                    moistureRaw = zone.soilMoistureRaw,
                    soilTempC = zone.soilTempC?.takeIf { it.isFinite() },
                    flags = zone.flags.orEmpty(),
                )
            },
            bmeTempC = dto.external?.bme680?.tC,
            bmeRhPct = dto.external?.bme680?.rhPct,
            bmePressureHpa = dto.external?.bme680?.pHpa,
            bmeGasKohm = dto.external?.bme680?.gasKohm,
            dhtTempC = dto.external?.dht11?.tC,
            dhtRhPct = dto.external?.dht11?.rhPct,
            ldrLightPct = dto.external?.ldr?.lightPct,
            calibrationMethod = dto.calibration?.moistureMethod?.takeIf { it.isNotBlank() },
            calibrationDate = dto.calibration?.date?.takeIf { it.isNotBlank() },
            qualityFlags = dto.qualityFlags.orEmpty(),
        )
    }

    fun parseEpochMillis(timestamp: String?): Long? {
        val text = timestamp?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { Instant.parse(text).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(text + "Z").toEpochMilli() }
            .getOrNull()
    }

    fun toNodeStatus(root: JsonElement?): NodeStatus {
        val obj = root.asObjectOrNull()
        val calibration = obj?.get("calibration")
        val calibrationObject = calibration.asObjectOrNull()
        val context = obj?.first("power_context", "regional_context", "nasa_context").asObjectOrNull()
        return NodeStatus(
            lastContact = obj?.firstText("last_contact", "last_seen", "last_received_at", "last_reading_at", "received_at", "ts"),
            nodeId = obj?.firstText("node_id"),
            source = obj?.firstText("source", "last_source")?.let { NodeSource.from(it) },
            calibrationMethod = calibrationObject?.firstText("moisture_method", "method")
                ?: calibration.asTextOrNull() ?: obj?.firstText("moisture_method"),
            calibrationDate = calibrationObject?.firstText("date") ?: obj?.firstText("calibration_date"),
            qualityFlags = obj?.first("quality_flags", "recent_quality_flags", "flags").asStringList(),
            regionalContext = context?.let {
                RegionalContext(
                    date = it.firstText("date", "day"),
                    t2mC = it.firstDouble("t2m_c", "t2m", "T2M"),
                    rh2mPct = it.firstDouble("rh2m_pct", "rh2m", "RH2M"),
                    source = it.firstText("source"),
                )
            },
            details = root.flattenToLabeledValues(),
        )
    }
}
