package com.example.farmealotu.data.repository

import com.example.farmealotu.data.model.CompareResponseDto
import com.example.farmealotu.data.model.DtoMappers
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.DataOrigin
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.FieldReadings
import com.example.farmealotu.domain.HealthInfo
import com.example.farmealotu.domain.IndicatorsResult
import com.example.farmealotu.domain.InfoSection
import com.example.farmealotu.domain.MapLayer
import com.example.farmealotu.domain.NasaStatus
import com.example.farmealotu.domain.NodeStatus
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.domain.SourceRef
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.round
import kotlin.math.sin

/**
 * MODO DEMO · DATOS DE EJEMPLO.
 *
 * Fixtures con la FORMA del contrato del PDF para poder recorrer la app sin backend.
 * Todos los números de este archivo son ILUSTRATIVOS: no vienen de NASA, no son resultado
 * del motor de recomendación y no deben usarse como evidencia ni en capturas «reales».
 *
 * Para poder ensayar la prueba C (cambiar prioridades -> recalcular), este falso backend
 * combina subpuntajes fijos de ejemplo con los pesos recibidos. Eso imita al servidor;
 * la interfaz en sí nunca calcula puntajes.
 *
 * Cada respuesta se arma como JSON y pasa por los mismos DTO y mapeadores que la API real.
 */
class FakeFarmealoTuRepository(
    private val json: Json,
    private val latencyMillis: Long = 500,
    private val now: () -> Instant = { Instant.now() },
) : FarmealoTuRepository {

    override val origin: DataOrigin = DataOrigin.DEMO

    override suspend fun health(): ApiResult<HealthInfo> {
        delay(latencyMillis / 3)
        return ApiResult.Success(HealthInfo(ok = true, detail = "modo demo (sin servidor)"))
    }

    override suspend fun nasaStatus(): ApiResult<NasaStatus> {
        delay(latencyMillis / 3)
        return ApiResult.Success(DtoMappers.toNasaStatus(nasaStatusJson()))
    }

    override suspend fun compare(input: FarmerInput, priorities: Priorities): ApiResult<ComparisonResult> {
        delay(latencyMillis)
        val normalized = priorities.normalized()
        if (!input.isComplete || normalized == null) {
            return ApiResult.Failure(ApiFailure.Validation(emptyList(), "Faltan respuestas o los pesos no son válidos."))
        }
        val dto = json.decodeFromJsonElement<CompareResponseDto>(compareJson(input, normalized))
        return ApiResult.Success(DtoMappers.toComparison(dto))
    }

    override suspend fun indicators(lat: Double, lon: Double, period: String?): ApiResult<IndicatorsResult> {
        delay(latencyMillis)
        return ApiResult.Success(DtoMappers.toIndicators(indicatorsJson()))
    }

    override suspend fun mapLayers(): ApiResult<List<MapLayer>> {
        delay(latencyMillis)
        return ApiResult.Success(DtoMappers.toMapLayers(mapLayersJson()))
    }

    override suspend fun fieldReadings(nodeId: String?, source: String?, since: String?, limit: Int?): ApiResult<FieldReadings> {
        delay(latencyMillis / 2)
        val all = DtoMappers.toFieldReadings(fieldReadingsJson(), json)
        val filtered = if (source.isNullOrBlank()) all else all.copy(readings = all.readings.filter { it.source.apiCode == source })
        return ApiResult.Success(if (limit == null) filtered else filtered.copy(readings = filtered.readings.takeLast(limit)))
    }

    override suspend fun fieldStatus(): ApiResult<NodeStatus> {
        delay(latencyMillis / 3)
        return ApiResult.Success(DtoMappers.toNodeStatus(fieldStatusJson()))
    }

    override suspend fun sources(): ApiResult<List<SourceRef>> {
        delay(latencyMillis / 3)
        return ApiResult.Success(DtoMappers.toSources(sourcesJson()))
    }

    override suspend fun context(): ApiResult<List<InfoSection>> {
        delay(latencyMillis / 3)
        return ApiResult.Success(DtoMappers.toContextSections(contextJson()))
    }

    // ------------------------------------------------------------------ fixtures

    private class DemoStrategy(
        val id: String,
        val name: String,
        val sequence: List<String>,
        val water: Double?,
        val soil: Double?,
        val risk: Double?,
        val advantage: String,
        val disadvantage: String,
        val mainRisk: String,
        val spread: Double,
        val stability: Double,
    )

    private fun compareJson(input: FarmerInput, weights: Priorities): JsonObject {
        val current = input.currentCrop ?: Crop.SOY
        val candidates = listOf(
            DemoStrategy(
                "E0", "Actual", input.currentRotationCodes, 58.0, 52.0, 55.0,
                "Es lo que ya conocés y sabés manejar.",
                "No destaca en agua ni en suelo frente a las otras opciones.",
                "Queda más expuesta a rachas secas en invierno.", 9.0, 0.14,
            ),
            DemoStrategy(
                "E1", "Cuidar suelo", listOf(current.apiCode, Crop.MAIZE.apiCode), 49.0, 71.0, 50.0,
                "Deja más rastrojo y mantiene el suelo cubierto más meses.",
                "Pide más agua en la época seca.",
                "Con poca lluvia en invierno puede no completar su ciclo.", 12.0, 0.22,
            ),
            DemoStrategy(
                "E2", "Cuidar agua", listOf(current.apiCode, Crop.SUNFLOWER.apiCode), 72.0, 47.0, 61.0,
                "Pide menos agua en la época seca.",
                "Deja menos rastrojo sobre el suelo.",
                "El suelo queda más descubierto después de la cosecha.", 8.0, 0.38,
            ),
            DemoStrategy(
                "E3", "Equilibrada", listOf(current.apiCode, Crop.WHEAT.apiCode), 61.0, 60.0, 58.0,
                "No es la mejor en nada, pero tampoco es débil en ningún criterio.",
                "No aprovecha al máximo ni el agua ni el suelo.",
                "Depende de que haya humedad al momento de la siembra de invierno.", 10.0, 0.26,
            ),
        )
        val excluded = input.excludedCrops.map { it.apiCode }.toSet()
        val removed = candidates.filter { it.id != "E0" && it.sequence.any { crop -> crop in excluded } }
        val kept = candidates - removed.toSet()
        val alternatives = kept.count { it.id != "E0" }
        val scores = kept.associate { it.id to demoScore(it, weights) }
        val recommended = if (alternatives >= 2) scores.maxByOrNull { it.value }?.key else null

        return buildJsonObject {
            putJsonObject("site") {
                put("lat", input.lat)
                put("lon", input.lon)
            }
            putJsonObject("data_status") {
                put("mode", "DEMO: datos de ejemplo, no son de NASA")
                put("last_observation_date", DEMO_OBSERVATION_DATE)
                put("downloaded_at", DEMO_DOWNLOAD_DATE)
                put("uses_cache", true)
            }
            putJsonArray("alerts") {
                add(buildJsonObject {
                    put("message", "Ejemplo de advertencia: lluvias por debajo de lo habitual en el período.")
                    put("date", "período de ejemplo (DEMO)")
                    put("level", "medium")
                    put("source", DEMO_SOURCE)
                    put("evidence_status", "derived")
                })
            }
            putJsonArray("strategies") {
                kept.forEach { strategy -> add(strategyJson(strategy, scores.getValue(strategy.id), input)) }
            }
            if (recommended != null) put("recommended_strategy_id", recommended) else put("recommended_strategy_id", JsonNull)
            putJsonArray("limitations") {
                add("MODO DEMO: estos resultados son de ejemplo y no salen del motor real.")
                add("Los puntajes son relativos entre opciones; no son probabilidad de éxito ni rendimiento.")
                add("Cambiar la rotación no elimina el riesgo de inundación.")
                removed.forEach {
                    add("${it.id} ${it.name} no se muestra porque usa un cultivo que excluiste.")
                }
                if (recommended == null) {
                    add("Quedan menos de tres opciones defendibles: no se ordena un ganador.")
                }
            }
        }
    }

    private fun demoScore(strategy: DemoStrategy, weights: Priorities): Double {
        val parts = listOf(strategy.water to weights.water, strategy.soil to weights.soil, strategy.risk to weights.risk)
            .filter { it.first != null }
        val weightSum = parts.sumOf { it.second }
        if (parts.isEmpty() || weightSum <= 0.0) return 0.0
        return round1(parts.sumOf { it.first!! * it.second } / weightSum)
    }

    private fun strategyJson(strategy: DemoStrategy, score: Double, input: FarmerInput): JsonObject = buildJsonObject {
        put("id", strategy.id)
        put("name", strategy.name)
        putJsonArray("sequence") {
            strategy.sequence.forEachIndexed { index, crop ->
                add(buildJsonObject {
                    put("season", if (index == 0) "summer" else "winter")
                    put("crop", crop)
                })
            }
        }
        put("score", score)
        putJsonObject("criteria") {
            put("water", strategy.water)
            put("soil", strategy.soil)
            put("risk", strategy.risk)
        }
        putJsonObject("uncertainty") {
            put("p10", round1((score - strategy.spread).coerceAtLeast(0.0)))
            put("p50", score)
            put("p90", round1((score + strategy.spread * 0.8).coerceAtMost(100.0)))
            put("rank_stability", strategy.stability)
            put("confidence_level", if (strategy.stability < 0.2) "low" else "medium")
            put("confidence_reason", "Ejemplo: solo datos regionales y parámetros genéricos, sin análisis de suelo.")
        }
        putJsonArray("reasons") {
            add(strategy.advantage)
            add("Ejemplo de explicación generada por el servidor a partir de los indicadores.")
        }
        putJsonArray("evidence") {
            add(evidenceJson("Racha seca máxima en invierno", JsonPrimitive(21), "días", DEMO_SOURCE, "ejemplo", "regional (~50 km)", "derived"))
            add(evidenceJson("Lluvia del período", JsonPrimitive(612.4), "mm", DEMO_SOURCE, "ejemplo", "regional (~50 km)", "documented"))
            add(evidenceJson("Pesos de prioridad", JsonPrimitive("los que elegiste"), null, "Vos", null, null, "farmer_provided"))
            add(evidenceJson("Riego", JsonPrimitive(input.irrigation?.label ?: "sin dato"), null, "Vos", null, null, "farmer_provided"))
            add(evidenceJson("Umbral de racha seca", JsonPrimitive(15), "días", "Supuesto configurable del modelo", null, null, "assumption"))
            add(evidenceJson("Análisis de suelo", JsonNull, null, null, null, "tu campo", "unavailable"))
        }
        putJsonObject("tradeoffs") {
            put("advantage", strategy.advantage)
            put("disadvantage", strategy.disadvantage)
            put("main_risk", strategy.mainRisk)
        }
        putJsonArray("missing_information") {
            add("Sin análisis de suelo de tu campo")
            add("Sin NDVI ni SMAP para este período")
            add("Umbral de calor genérico")
        }
        putJsonArray("agronomist_questions") {
            add("¿Mi suelo retiene suficiente humedad para un cultivo de invierno?")
            add("¿Qué fecha de siembra conviene en mi zona para esta secuencia?")
        }
    }

    private fun evidenceJson(
        indicator: String, value: JsonElement, unit: String?, source: String?, period: String?, scale: String?, status: String,
    ): JsonObject = buildJsonObject {
        put("indicator", indicator)
        put("value", value)
        put("unit", unit)
        put("source", source)
        put("period", period)
        put("scale", scale)
        put("evidence_status", status)
    }

    private fun nasaStatusJson(): JsonObject = buildJsonObject {
        put("mode", "DEMO: estado de ejemplo, no consultado a NASA")
        put("last_observation_date", DEMO_OBSERVATION_DATE)
        put("last_download", DEMO_DOWNLOAD_DATE)
        put("uses_cache", true)
        putJsonObject("sources") {
            putJsonObject("power") {
                put("available", true)
                put("scale", "regional (~50 km)")
            }
            putJsonObject("smap") { put("available", false) }
            putJsonObject("ndvi") { put("available", false) }
        }
    }

    private fun indicatorsJson(): JsonObject = buildJsonObject {
        put("period", "2015-2026 (ejemplo)")
        put("last_observation_date", DEMO_OBSERVATION_DATE)
        putJsonArray("indicators") {
            add(summaryJson("dry_spell_max", "Racha seca más larga", JsonPrimitive(24), "días", "medium",
                "Días seguidos casi sin lluvia (menos de 1 mm)."))
            add(summaryJson("hot_days", "Días de calor", JsonPrimitive(38), "días", "medium",
                "Días con temperatura máxima sobre el umbral general del mes; no afirma daño al cultivo."))
            add(summaryJson("heavy_rain_days", "Días de lluvia intensa", JsonPrimitive(7), "días", "low",
                "Días con lluvia mayor al percentil 95 histórico. Es exposición a lluvia fuerte, no predicción de desborde."))
            add(summaryJson("ndvi_low_cover", "Baja cobertura (NDVI)", JsonNull, null, null,
                "Sin serie NDVI descargada para el punto piloto.", status = "unavailable"))
        }
        putJsonObject("monthly_grid") {
            putJsonArray("indicators") {
                add(gridJson("dry_spell_max", "Racha seca más larga del mes", "días",
                    "Días seguidos casi sin lluvia (menos de 1 mm) dentro de cada mes.") { year, month ->
                    val base = intArrayOf(6, 5, 6, 9, 13, 18, 22, 21, 15, 10, 7, 6)[month - 1]
                    (base + (year * 7 + month * 13) % 9 - 4).coerceIn(0, 31).toDouble()
                })
                add(gridJson("hot_days", "Días de calor", "días",
                    "Días del mes con temperatura máxima sobre el umbral general.") { year, month ->
                    val base = intArrayOf(9, 7, 6, 3, 1, 0, 0, 2, 6, 10, 11, 10)[month - 1]
                    (base + (year * 5 + month * 3) % 5 - 2).coerceAtLeast(0).toDouble()
                })
                add(gridJson("rain_anomaly_pct", "Anomalía de lluvia", "%",
                    "Cuánto llovió más o menos que lo habitual en el mismo mes.") { year, month ->
                    (((year * 31 + month * 17) % 121) - 60).toDouble()
                })
            }
        }
    }

    private fun summaryJson(
        id: String, name: String, value: JsonElement, unit: String?, level: String?, description: String, status: String = "derived",
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("value", value)
        put("unit", unit)
        put("period", "período de ejemplo")
        put("source", DEMO_SOURCE)
        put("scale", "regional (~50 km)")
        put("evidence_status", status)
        put("level", level)
        put("description", description)
        if (status != "unavailable") {
            putJsonObject("quality") {
                put("valid_days", 4012)
                put("missing_pct", 0.4)
            }
        }
    }

    private fun gridJson(
        id: String, name: String, unit: String, description: String, value: (year: Int, month: Int) -> Double,
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("unit", unit)
        put("source", DEMO_SOURCE)
        put("scale", "regional (~50 km)")
        put("evidence_status", "derived")
        put("description", description)
        putJsonArray("cells") {
            for (year in 2015..2026) {
                for (month in 1..12) {
                    // Sin dato: meses aún no observados y un hueco de ejemplo.
                    val missing = (year == 2026 && month >= 11) || (year == 2019 && month == 6)
                    add(buildJsonObject {
                        put("year", year)
                        put("month", month)
                        if (missing) {
                            put("value", JsonNull)
                            put("quality", "sin dato")
                        } else {
                            put("value", value(year, month))
                            put("quality", "ejemplo")
                        }
                    })
                }
            }
        }
    }

    private fun mapLayersJson(): JsonObject = buildJsonObject {
        putJsonArray("layers") {
            add(layerJson("rain", "regional", JsonPrimitive(612.4), "mm (acumulado del período de ejemplo)",
                "Lluvia de la zona. Sirve de contexto: no es la lluvia medida en tu campo.", "documented"))
            add(layerJson("soil_moisture", "unavailable", JsonNull, null,
                "No hay datos de humedad del suelo (SMAP) descargados.", "unavailable"))
            add(layerJson("heat", "regional", JsonPrimitive(38), "días de calor (período de ejemplo)",
                "Calor de la zona. Un valor alto pide revisar las fechas de siembra con tu técnico.", "derived"))
            add(layerJson("vegetation", "unavailable", JsonNull, null,
                "No hay serie de vegetación (NDVI) descargada.", "unavailable"))
            add(layerJson("water", "regional", JsonPrimitive(-84.0), "mm (lluvia efectiva menos demanda del cultivo)",
                "Balance de agua calculado. Negativo: faltó agua. No es un mapa de inundación observada.", "derived"))
        }
    }

    private fun layerJson(
        id: String, status: String, value: JsonElement, unit: String?, interpretation: String, evidenceStatus: String,
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("status", status)
        put("value", value)
        put("unit", unit)
        put("interpretation", interpretation)
        put("evidence_status", evidenceStatus)
        if (status != "unavailable") {
            put("source", DEMO_SOURCE)
            put("date", DEMO_OBSERVATION_DATE)
            put("resolution", "regional (~50 km)")
        }
    }

    /** 30 lecturas simuladas cada 10 min: riego de ejemplo solo en la zona A (escenario S4 del plan). */
    private fun fieldReadingsJson(): JsonArray {
        val end = now().truncatedTo(ChronoUnit.MINUTES)
        val count = 30
        val irrigationIndex = 18
        return buildJsonArray {
            for (index in 0 until count) {
                val ts = end.minus(((count - 1 - index) * 10L), ChronoUnit.MINUTES)
                val hours = index / 6.0
                val afterIrrigation = (index - irrigationIndex).coerceAtLeast(0) / 6.0
                val moistureA = 0.11 - 0.004 * hours + if (index >= irrigationIndex) 0.10 * exp(-0.15 * afterIrrigation) else 0.0
                val moistureB = 0.19 - 0.002 * hours
                val moistureC = 0.31 - 0.003 * hours
                val wave = sin(2 * PI * index / 36.0)
                add(buildJsonObject {
                    put("schema_version", "1.0")
                    put("node_id", "rotasat-node-01")
                    put("source", "simulated")
                    put("scenario", "S4")
                    put("ts", ts.toString())
                    putJsonObject("internal") {
                        put("depth_cm", 7)
                        putJsonArray("zones") {
                            add(zoneJson("A", moistureA, 24.6 + 1.4 * wave, emptyList()))
                            // Ejemplo de dato faltante: la sonda de B se «desconecta» en una lectura.
                            if (index == 9) add(zoneJson("B", moistureB, null, listOf("ds18b20_B_disconnected")))
                            else add(zoneJson("B", moistureB, 23.9 + 1.1 * wave, emptyList()))
                            add(zoneJson("C", moistureC, 23.1 + 0.8 * wave, emptyList()))
                        }
                    }
                    putJsonObject("external") {
                        putJsonObject("bme680") {
                            put("t_c", round1(26.1 + 2.0 * wave))
                            put("rh_pct", round1(48.2 - 5.0 * wave))
                            put("p_hpa", 965.0)
                            put("gas_kohm", 85.4)
                        }
                        putJsonObject("dht11") {
                            put("t_c", round(26.0 + 2.0 * wave))
                            put("rh_pct", round(50.0 - 5.0 * wave))
                        }
                        putJsonObject("ldr") {
                            put("raw", 1830)
                            put("mv", 1480)
                            put("light_pct", round1(45.0 + 30.0 * wave).coerceIn(0.0, 100.0))
                            put("r_fixed_ohm", 2200)
                        }
                    }
                    putJsonObject("calibration") {
                        put("moisture_method", "gravimetric_3pt")
                        put("date", "ejemplo (DEMO)")
                    }
                    putJsonArray("quality_flags") {
                        if (index == 9) add("ds18b20_B_disconnected")
                    }
                })
            }
        }
    }

    private fun zoneJson(id: String, vwc: Double, tempC: Double?, flags: List<String>): JsonObject = buildJsonObject {
        put("id", id)
        put("soil_moisture_vwc", round(vwc * 1000.0) / 1000.0)
        put("soil_moisture_raw", round(2900 - vwc * 3400))
        put("soil_temp_c", tempC?.let { round1(it) })
        putJsonArray("flags") { flags.forEach { add(it) } }
    }

    private fun fieldStatusJson(): JsonObject = buildJsonObject {
        put("node_id", "rotasat-node-01")
        put("last_contact", now().truncatedTo(ChronoUnit.MINUTES).toString())
        put("source", "simulated")
        putJsonObject("calibration") {
            put("moisture_method", "gravimetric_3pt")
            put("date", "ejemplo (DEMO)")
        }
        putJsonArray("quality_flags") { add("ds18b20_B_disconnected") }
        putJsonObject("power_context") {
            put("date", DEMO_OBSERVATION_DATE)
            put("t2m_c", 27.4)
            put("rh2m_pct", 58.0)
            put("source", DEMO_SOURCE)
        }
    }

    private fun sourcesJson(): JsonArray = buildJsonArray {
        add(sourceJson("NASA POWER — API diaria", "https://power.larc.nasa.gov/docs/services/api/temporal/daily/",
            "Clima regional (~50 km): lluvia, temperatura, humedad, viento y radiación."))
        add(sourceJson("FAO-56 — Crop evapotranspiration (Allen et al., 1998)", "https://www.fao.org/4/X0490E",
            "Método de evapotranspiración de referencia y coeficientes de cultivo."))
        add(sourceJson("MDPyEP — Santa Cruz Industrial y Productiva 2024", null,
            "Línea base municipal 2023. Contexto histórico, no datos actuales de cosecha."))
        add(sourceJson("ANAPO — siembra directa y rotación (jul-2024)", null,
            "Prácticas regionales documentadas."))
    }

    private fun sourceJson(title: String, url: String?, description: String): JsonObject = buildJsonObject {
        put("title", title)
        put("url", url)
        put("description", description)
    }

    private fun contextJson(): JsonObject = buildJsonObject {
        putJsonObject("contexto_historico_2023") {
            put("nota", "Cifras municipales de 2023 tomadas del plan del proyecto; pendientes de verificar contra la fuente. No son valores actuales.")
            put("soya_superficie_ha", 318083)
            put("sorgo_superficie_ha", 93955)
        }
    }

    private fun round1(value: Double): Double = round(value * 10.0) / 10.0

    private companion object {
        const val DEMO_SOURCE = "DEMO · dato de ejemplo"
        const val DEMO_OBSERVATION_DATE = "2026-11-10"
        const val DEMO_DOWNLOAD_DATE = "2026-11-14"
    }
}
