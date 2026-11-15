package com.example.farmealotu.domain

/**
 * Modelos que consume la interfaz. Se construyen SOLO a partir de lo que devuelve la API
 * (o el fixture DEMO). Un valor ausente es `null`; nunca se convierte en 0.
 */

enum class DataOrigin { REMOTE, DEMO }

data class LabeledValue(val label: String, val value: String)

data class InfoSection(val title: String, val lines: List<LabeledValue>)

data class HealthInfo(val ok: Boolean, val detail: String?)

// ---------- Comparación de rotaciones ----------

data class ComparisonResult(
    val siteLat: Double?,
    val siteLon: Double?,
    val lastObservationDate: String?,
    val downloadedAt: String?,
    val dataStatus: List<LabeledValue>,
    val alerts: List<Alert>,
    val strategies: List<Strategy>,
    val recommendedId: String?,
    val limitations: List<String>,
) {
    /** Solo hay recomendación cuando el id devuelto existe entre las estrategias. */
    val recommended: Strategy?
        get() = recommendedId?.takeIf { it.isNotBlank() }?.let { id -> strategies.firstOrNull { it.id == id } }

    val current: Strategy?
        get() = strategies.firstOrNull { it.id.equals("E0", ignoreCase = true) }
}

data class Alert(
    val message: String,
    val date: String?,
    val level: String?,
    val source: String?,
    val evidenceStatus: String?,
)

data class Strategy(
    val id: String,
    val name: String,
    val sequence: List<SequenceStep>,
    val score: Double?,
    val partialScore: Boolean,
    val criteria: Criteria,
    val uncertainty: Uncertainty?,
    val reasons: List<String>,
    val evidence: List<Evidence>,
    val tradeoffs: Tradeoffs,
    val missingInformation: List<String>,
    val agronomistQuestions: List<String>,
) {
    val displayName: String
        get() = if (name.isBlank() || name.startsWith(id, ignoreCase = true)) name.ifBlank { id } else "$id · $name"

    val sequenceText: String
        get() = if (sequence.isEmpty()) "Secuencia no informada" else sequence.joinToString(" → ") { it.display }
}

data class SequenceStep(val season: String?, val cropCode: String) {
    val cropLabel: String get() = Crop.labelFor(cropCode)

    val seasonLabel: String?
        get() = when (season?.trim()?.lowercase()) {
            null, "" -> null
            "summer", "verano" -> "Verano"
            "winter", "invierno" -> "Invierno"
            else -> season
        }

    val display: String get() = seasonLabel?.let { "$it: $cropLabel" } ?: cropLabel
}

data class CriterionValue(
    val score: Double?,
    val detail: String? = null,
    val unit: String? = null,
    val status: String? = null,
)

data class Criteria(
    val water: CriterionValue?,
    val soil: CriterionValue?,
    val risk: CriterionValue?,
) {
    val hasMissing: Boolean
        get() = water?.score == null || soil?.score == null || risk?.score == null
}

enum class CriterionKey(val label: String, val technical: String) {
    WATER("Agua", "S_agua"),
    SOIL("Suelo", "S_suelo"),
    RISK("Riesgo", "S_riesgo");

    fun of(criteria: Criteria): CriterionValue? = when (this) {
        WATER -> criteria.water
        SOIL -> criteria.soil
        RISK -> criteria.risk
    }
}

data class Uncertainty(
    val p10: Double?,
    val p50: Double?,
    val p90: Double?,
    val rankStability: Double?,
    val confidenceLevel: String?,
    val confidenceReason: String?,
) {
    val hasRange: Boolean get() = p10 != null && p90 != null
}

/**
 * Nivel de confianza para mostrar. Política del plan (3.7): con datos solo regionales y parámetros
 * genéricos (toda esta versión), el máximo que se rotula es «media», aunque la API diga otra cosa.
 */
enum class ConfidenceDisplay(val label: String) {
    LOW("Baja"),
    MEDIUM("Media"),
    MEDIUM_CAPPED("Media (limitada: datos regionales)"),
    UNKNOWN("No informada");

    companion object {
        fun from(apiLevel: String?): ConfidenceDisplay = when (apiLevel?.trim()?.lowercase()) {
            "low", "baja", "bajo" -> LOW
            "medium", "media", "medio", "moderate" -> MEDIUM
            "high", "alta", "alto" -> MEDIUM_CAPPED
            else -> UNKNOWN
        }
    }
}

data class Evidence(
    val indicator: String,
    val valueText: String?,
    val unit: String?,
    val source: String?,
    val period: String?,
    val scale: String?,
    val status: String?,
)

data class Tradeoffs(
    val advantage: String?,
    val disadvantage: String?,
    val mainRisk: String?,
    val extra: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = advantage == null && disadvantage == null && mainRisk == null && extra.isEmpty()
}

// ---------- Indicadores y riesgos ----------

data class IndicatorsResult(
    val lastObservationDate: String?,
    val period: String?,
    val summaries: List<IndicatorSummary>,
    val grids: List<HeatmapSeries>,
)

data class IndicatorSummary(
    val id: String,
    val name: String,
    val valueText: String?,
    val unit: String?,
    val period: String?,
    val source: String?,
    val scale: String?,
    val status: String?,
    val validDays: Int?,
    val missingPct: Double?,
    val description: String?,
    val level: String?,
)

data class HeatmapSeries(
    val id: String,
    val name: String,
    val unit: String?,
    val source: String?,
    val scale: String?,
    val status: String?,
    val description: String?,
    val cells: List<HeatCell>,
) {
    val years: List<Int> get() = cells.map { it.year }.distinct().sorted()
    val hasValues: Boolean get() = cells.any { it.value != null }
    fun cell(year: Int, month: Int): HeatCell? = cells.firstOrNull { it.year == year && it.month == month }
}

data class HeatCell(
    val year: Int,
    val month: Int,
    val value: Double?,
    val quality: String? = null,
    val validDays: Int? = null,
    val missingPct: Double? = null,
)

// ---------- NASA / capas / fuentes ----------

data class NasaStatus(
    val lastObservationDate: String?,
    val lastDownload: String?,
    val usesCache: Boolean?,
    val details: List<LabeledValue>,
)

enum class LayerKind(val title: String, val apiIds: List<String>) {
    RAIN("Lluvia", listOf("rain", "lluvia", "precipitation", "precip")),
    SOIL_MOISTURE("Humedad del suelo", listOf("soil_moisture", "humedad", "humedad_suelo", "smap", "moisture")),
    HEAT("Calor", listOf("heat", "calor", "temperature", "temp")),
    VEGETATION("Vegetación", listOf("vegetation", "vegetacion", "ndvi")),
    WATER("Agua", listOf("water", "agua", "water_balance", "balance_hidrico", "balance")),
}

enum class LayerAvailability(val label: String) {
    AVAILABLE("Capa disponible"),
    REGIONAL("Información regional"),
    UNAVAILABLE("Datos no disponibles"),
}

data class LegendEntry(val label: String, val colorHex: String?)

data class MapLayer(
    val kind: LayerKind,
    val availability: LayerAvailability,
    val name: String?,
    val source: String?,
    val date: String?,
    val resolution: String?,
    val unit: String?,
    val valueText: String?,
    val legend: List<LegendEntry>,
    val interpretation: String?,
    val explanation: String?,
    val evidenceStatus: String?,
    val tileUrlTemplate: String?,
    val attribution: String?,
    /** Nivel de zoom más alto con teselas publicadas (GIBS: 6 a 8 según la capa). null = no informado. */
    val maxZoom: Int? = null,
) {
    companion object {
        fun unavailable(kind: LayerKind) = MapLayer(
            kind = kind,
            availability = LayerAvailability.UNAVAILABLE,
            name = null, source = null, date = null, resolution = null, unit = null, valueText = null,
            legend = emptyList(), interpretation = null, explanation = null, evidenceStatus = "unavailable",
            tileUrlTemplate = null, attribution = null,
        )
    }
}

data class SourceRef(
    val title: String,
    val url: String?,
    val detail: String?,
)

// ---------- Nodo de campo ----------

enum class NodeSource(val apiCode: String, val badge: String, val description: String) {
    SIMULATED("simulated", "SIMULADO", "Producido por el gemelo digital. No es una medición."),
    WOKWI("wokwi", "WOKWI", "Producido por el circuito simulado en Wokwi. No es una medición."),
    PHYSICAL("physical", "FÍSICO", "Medido por el prototipo físico de 3 macetas."),
    UNKNOWN("", "ORIGEN NO INFORMADO", "La lectura no dice de dónde viene; no la trates como medición.");

    companion object {
        fun from(code: String?): NodeSource =
            entries.firstOrNull { it != UNKNOWN && it.apiCode == code?.trim()?.lowercase() } ?: UNKNOWN
    }
}

/**
 * Cómo llega la humedad del suelo de una zona, en orden de prioridad para mostrarla.
 * Nunca se convierte una en otra: cada una se muestra con su propio nombre y unidad.
 */
enum class MoistureKind {
    /** Humedad volumétrica calibrada (`soil_moisture_vwc`, m³/m³), se muestra en %. */
    VWC,
    /** Índice RELATIVO del sensor (`soil_moisture_index_pct`, 0–100: seco al aire = 0, en agua = 100). No es VWC. */
    INDEX,
    /** Lectura cruda del sensor (`soil_moisture_raw`, cuentas del ADC), sin calibrar. */
    RAW,
}

data class ZoneReading(
    val id: String,
    /** Humedad volumétrica en m³/m³ tal como llega (`soil_moisture_vwc`). */
    val moistureVwc: Double?,
    val moistureRaw: Double?,
    val soilTempC: Double?,
    val flags: List<String>,
    /** Índice relativo del sensor, 0–100 (`soil_moisture_index_pct`). NO es humedad volumétrica. */
    val moistureIndexPct: Double? = null,
) {
    /** m³/m³ -> % volumétrico. Solo para mostrar. */
    val moisturePct: Double? get() = moistureVwc?.let { it * 100.0 }

    /** El mejor dato de humedad que trae la zona (VWC → índice → crudo); null = sin dato. */
    val moistureKind: MoistureKind? get() = MoistureKind.entries.firstOrNull { moistureValue(it) != null }

    /** Valor de la zona para ese tipo, sin conversiones (VWC en %). */
    fun moistureValue(kind: MoistureKind): Double? = when (kind) {
        MoistureKind.VWC -> moisturePct
        MoistureKind.INDEX -> moistureIndexPct
        MoistureKind.RAW -> moistureRaw
    }
}

/**
 * Tipo de humedad para graficar un conjunto de lecturas de UN origen sin mezclar unidades:
 * VWC si alguna lectura la trae; si no, el índice; si no, la lectura cruda; null si no hay nada.
 */
fun moistureKindFor(readings: List<NodeReading>): MoistureKind? = MoistureKind.entries.firstOrNull { kind ->
    readings.any { reading -> reading.zones.any { it.moistureValue(kind) != null } }
}

data class NodeReading(
    val nodeId: String?,
    val source: NodeSource,
    val scenario: String?,
    val timestamp: String?,
    val epochMillis: Long?,
    val depthCm: Double?,
    val zones: List<ZoneReading>,
    val bmeTempC: Double?,
    val bmeRhPct: Double?,
    val bmePressureHpa: Double?,
    val bmeGasKohm: Double?,
    val dhtTempC: Double?,
    val dhtRhPct: Double?,
    val ldrLightPct: Double?,
    val calibrationMethod: String?,
    val calibrationDate: String?,
    val qualityFlags: List<String>,
    /** Hora DENTRO de la simulación (`sim_time`, solo el gemelo digital). `timestamp` es la hora del servidor. */
    val simTime: String? = null,
    val simEpochMillis: Long? = null,
) {
    fun zone(id: String): ZoneReading? = zones.firstOrNull { it.id.equals(id, ignoreCase = true) }

    /**
     * Hora para ubicar la lectura en el gráfico: la simulada si existe (el gemelo manda muchas horas
     * simuladas en pocos segundos reales, todas con casi el mismo `ts`), si no la del nodo.
     */
    val chartMillis: Long? get() = simEpochMillis ?: epochMillis
}

data class NodeStatus(
    val lastContact: String?,
    val nodeId: String?,
    val source: NodeSource?,
    val calibrationMethod: String?,
    val calibrationDate: String?,
    val qualityFlags: List<String>,
    val regionalContext: RegionalContext?,
    val details: List<LabeledValue>,
)

/** Contexto NASA POWER del mismo día, si el backend lo incluye. Regional (~50 km). */
data class RegionalContext(
    val date: String?,
    val t2mC: Double?,
    val rh2mPct: Double?,
    val source: String?,
)

data class FieldReadings(
    val readings: List<NodeReading>,
    val skipped: Int,
)

/** Orden de las lecturas: hora del nodo/servidor y, a igual hora, la hora simulada. */
val NodeReadingOrder: Comparator<NodeReading> =
    compareBy({ it.epochMillis ?: Long.MIN_VALUE }, { it.simEpochMillis ?: Long.MIN_VALUE })
