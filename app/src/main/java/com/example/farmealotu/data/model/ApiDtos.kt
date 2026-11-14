@file:OptIn(ExperimentalSerializationApi::class)

package com.example.farmealotu.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

/*
 * DTOs del contrato REST (PDF 4.8, 3.8 y 9.2).
 *
 * - Todos los campos de respuesta son opcionales/nullable: un dato ausente queda en null
 *   y la interfaz lo muestra como «no disponible»; nunca se rellena con 0.
 * - Donde el PDF no fija la forma interna (sequence, criteria, tradeoffs, alerts…) se recibe
 *   JsonElement y lo interpreta DtoMappers.kt de forma tolerante.
 * - Cuando exista el OpenAPI real hay que reconciliar este archivo: ver docs/UI_BACKEND_CONTRACT.md.
 */

// ---------- POST /api/rotations/compare ----------

@Serializable
data class SiteDto(
    val lat: Double? = null,
    val lon: Double? = null,
)

@Serializable
data class PrioritiesDto(
    val water: Double,
    val soil: Double,
    val risk: Double,
)

@Serializable
data class ConstraintsDto(
    val irrigation: String,
    @SerialName("excluded_crops") val excludedCrops: List<String> = emptyList(),
)

@Serializable
data class CompareRequestDto(
    val site: SiteDto,
    @SerialName("current_rotation") val currentRotation: List<String>,
    val priorities: PrioritiesDto,
    val constraints: ConstraintsDto,
    val notes: String = "",
)

@Serializable
data class CompareResponseDto(
    val site: SiteDto? = null,
    @SerialName("data_status") val dataStatus: JsonElement? = null,
    val alerts: JsonElement? = null,
    val strategies: List<StrategyDto>? = null,
    @SerialName("recommended_strategy_id") val recommendedStrategyId: String? = null,
    val limitations: JsonElement? = null,
)

@Serializable
data class StrategyDto(
    val id: String? = null,
    val name: String? = null,
    val sequence: JsonElement? = null,
    val score: JsonElement? = null,
    @JsonNames("partial", "is_partial") @SerialName("partial_score") val partialScore: Boolean? = null,
    val criteria: JsonElement? = null,
    val uncertainty: UncertaintyDto? = null,
    val reasons: JsonElement? = null,
    val evidence: List<EvidenceDto>? = null,
    val tradeoffs: JsonElement? = null,
    @SerialName("missing_information") val missingInformation: JsonElement? = null,
    @SerialName("agronomist_questions") val agronomistQuestions: JsonElement? = null,
)

@Serializable
data class UncertaintyDto(
    val p10: Double? = null,
    @JsonNames("median") val p50: Double? = null,
    val p90: Double? = null,
    @JsonNames("ranking_stability", "stability") @SerialName("rank_stability") val rankStability: Double? = null,
    @JsonNames("confidence", "confidence_label") @SerialName("confidence_level") val confidenceLevel: String? = null,
    @JsonNames("confidence_rationale", "reason") @SerialName("confidence_reason") val confidenceReason: String? = null,
)

@Serializable
data class EvidenceDto(
    @JsonNames("name", "label") val indicator: String? = null,
    val value: JsonElement? = null,
    val unit: String? = null,
    val source: String? = null,
    val period: String? = null,
    val scale: String? = null,
    @JsonNames("status") @SerialName("evidence_status") val evidenceStatus: String? = null,
)

// ---------- Nodo de campo: contrato JSON v1 (PDF 9.2) ----------

@Serializable
data class SensorReadingDto(
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("node_id") val nodeId: String? = null,
    val source: String? = null,
    val scenario: String? = null,
    val ts: String? = null,
    @SerialName("received_at") val receivedAt: String? = null,
    val internal: InternalDto? = null,
    val external: ExternalDto? = null,
    val calibration: CalibrationDto? = null,
    @SerialName("quality_flags") val qualityFlags: List<String>? = null,
)

@Serializable
data class InternalDto(
    @SerialName("depth_cm") val depthCm: Double? = null,
    val zones: List<ZoneDto>? = null,
)

@Serializable
data class ZoneDto(
    val id: String? = null,
    /** m³/m³ (0–1). NO es un porcentaje. */
    @SerialName("soil_moisture_vwc") val soilMoistureVwc: Double? = null,
    @SerialName("soil_moisture_raw") val soilMoistureRaw: Double? = null,
    @SerialName("soil_temp_c") val soilTempC: Double? = null,
    val flags: List<String>? = null,
)

@Serializable
data class ExternalDto(
    val bme680: Bme680Dto? = null,
    val dht11: Dht11Dto? = null,
    val ldr: LdrDto? = null,
)

@Serializable
data class Bme680Dto(
    @SerialName("t_c") val tC: Double? = null,
    @SerialName("rh_pct") val rhPct: Double? = null,
    @SerialName("p_hpa") val pHpa: Double? = null,
    @SerialName("gas_kohm") val gasKohm: Double? = null,
)

@Serializable
data class Dht11Dto(
    @SerialName("t_c") val tC: Double? = null,
    @SerialName("rh_pct") val rhPct: Double? = null,
)

@Serializable
data class LdrDto(
    val raw: Double? = null,
    val mv: Double? = null,
    @SerialName("light_pct") val lightPct: Double? = null,
    @SerialName("r_fixed_ohm") val rFixedOhm: Double? = null,
)

@Serializable
data class CalibrationDto(
    @SerialName("moisture_method") val moistureMethod: String? = null,
    val date: String? = null,
    @SerialName("ds18b20_offsets_c") val ds18b20OffsetsC: List<Double?>? = null,
)
