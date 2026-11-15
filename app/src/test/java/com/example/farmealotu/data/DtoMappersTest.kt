package com.example.farmealotu.data

import com.example.farmealotu.data.model.CompareResponseDto
import com.example.farmealotu.data.model.DtoMappers
import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.domain.LayerAvailability
import com.example.farmealotu.domain.LayerKind
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.Priorities
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoMappersTest {

    private val json = ApiClient.json

    private fun compare(text: String) = DtoMappers.toComparison(json.decodeFromString<CompareResponseDto>(text))

    private val completeInput = FarmerInput(
        currentCrop = Crop.SOY, previousCrop = Crop.SORGHUM, concern = Concern.WATER, irrigation = Irrigation.NO,
        notes = "  Mi terreno suele secarse en invierno ", excludedCrops = setOf(Crop.WHEAT),
    )

    // ---------- Request ----------

    @Test
    fun `el request tiene la forma del PDF y pesos que suman uno`() {
        val request = DtoMappers.toCompareRequest(completeInput, Priorities(0.55, 0.25, 0.20))
        val body = json.encodeToJsonElement(com.example.farmealotu.data.model.CompareRequestDto.serializer(), request).jsonObject
        assertEquals(setOf("site", "current_rotation", "priorities", "constraints", "notes"), body.keys)
        assertEquals("""{"lat":-17.2374,"lon":-62.2427}""", body["site"].toString())
        assertEquals("""["soy","sorghum"]""", body["current_rotation"].toString())
        assertEquals("""{"irrigation":"rainfed","excluded_crops":["wheat"]}""", body["constraints"].toString())
        assertEquals("\"Mi terreno suele secarse en invierno\"", body["notes"].toString())
        val p = request.priorities
        assertEquals(1.0, p.water + p.soil + p.risk, 1e-9)
    }

    @Test
    fun `pesos sin normalizar se normalizan al enviar`() {
        val p = DtoMappers.toCompareRequest(completeInput, Priorities(1.0, 1.0, 1.0)).priorities
        assertEquals(0.3333, p.water, 1e-9)
        assertEquals(0.3333, p.soil, 1e-9)
        assertEquals(1.0, p.water + p.soil + p.risk, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se arma un request con respuestas incompletas`() {
        DtoMappers.toCompareRequest(FarmerInput(currentCrop = Crop.SOY), Priorities.DEFAULT)
    }

    // ---------- Comparación ----------

    @Test
    fun `respuesta completa se mapea sin alterar los numeros`() {
        val result = compare(
            """
            {"site":{"lat":-17.2374,"lon":-62.2427},
             "data_status":{"last_observation_date":"2026-11-10","downloaded_at":"2026-11-14","power":{"available":true}},
             "alerts":[{"message":"Lluvias bajo lo habitual","date":"ene-oct 2026","level":"medium","source":"NASA POWER"}],
             "strategies":[
               {"id":"E0","name":"Actual","sequence":["soy","sorghum"],"score":55.4,
                "criteria":{"water":58.0,"soil":52.0,"risk":55.0},
                "uncertainty":{"p10":48.1,"p50":55.4,"p90":61.0,"rank_stability":0.14,"confidence_level":"low","confidence_reason":"sin suelo"},
                "reasons":["Es tu rotación actual"],
                "evidence":[{"indicator":"Racha seca","value":21,"unit":"días","source":"NASA POWER","period":"2015-2025","scale":"~50 km","evidence_status":"derived"}],
                "tradeoffs":{"advantage":"La conocés","disadvantage":"No destaca","main_risk":"Rachas secas"},
                "missing_information":["Sin análisis de suelo"],
                "agronomist_questions":["¿Cuándo sembrar?"]},
               {"id":"E2","name":"Cuidar agua","sequence":[{"season":"summer","crop":"soy"},{"season":"winter","crop":"sunflower"}],"score":61.2,
                "criteria":{"water":{"score":72.0,"detail":"cubre 72 % de la demanda"},"soil":47.0,"risk":61.0}}
             ],
             "recommended_strategy_id":"E2",
             "limitations":["Puntajes relativos"]}
            """
        )
        assertEquals("2026-11-10", result.lastObservationDate)
        assertEquals("2026-11-14", result.downloadedAt)
        assertEquals(2, result.strategies.size)
        assertEquals("E2", result.recommended?.id)
        assertEquals("ene-oct 2026", result.alerts.single().date)

        val e0 = result.strategies[0]
        assertEquals(55.4, e0.score!!, 0.0)
        assertEquals(58.0, e0.criteria.water!!.score!!, 0.0)
        assertEquals("Soya → Sorgo", e0.sequenceText)
        assertEquals(48.1, e0.uncertainty!!.p10!!, 0.0)
        assertEquals("21", e0.evidence.single().valueText)
        assertEquals("derived", e0.evidence.single().status)
        assertEquals("Rachas secas", e0.tradeoffs.mainRisk)
        assertFalse(e0.partialScore)

        val e2 = result.strategies[1]
        assertEquals("Verano: Soya → Invierno: Girasol", e2.sequenceText)
        assertEquals(72.0, e2.criteria.water!!.score!!, 0.0)
        assertEquals("cubre 72 % de la demanda", e2.criteria.water!!.detail)
        assertNull(e2.uncertainty)
        assertTrue(e2.tradeoffs.isEmpty)
    }

    @Test
    fun `null nunca se convierte en cero`() {
        val result = compare(
            """
            {"strategies":[{"id":"E1","name":"Cuidar suelo","score":null,
               "criteria":{"water":null,"soil":71.0},
               "uncertainty":{"p10":null,"p50":null,"p90":null,"rank_stability":null},
               "evidence":[{"indicator":"SMAP","value":null,"evidence_status":"unavailable"}]}],
             "recommended_strategy_id":null}
            """
        )
        val strategy = result.strategies.single()
        assertNull(strategy.score)
        assertNotNull(strategy.criteria.water)
        assertNull(strategy.criteria.water!!.score)
        assertEquals(71.0, strategy.criteria.soil!!.score!!, 0.0)
        assertNull("criterio ausente", strategy.criteria.risk)
        assertTrue(strategy.criteria.hasMissing)
        assertFalse(strategy.uncertainty!!.hasRange)
        assertNull(strategy.uncertainty!!.rankStability)
        assertNull(strategy.evidence.single().valueText)
        assertNull(result.recommended)
        assertNull(result.lastObservationDate)
        assertTrue(result.alerts.isEmpty())
    }

    @Test
    fun `puntaje con criterio faltante se marca como parcial`() {
        val result = compare("""{"strategies":[{"id":"E0","score":50.0,"criteria":{"water":null,"soil":60.0,"risk":40.0}}]}""")
        assertTrue(result.strategies.single().partialScore)
    }

    @Test
    fun `un id recomendado que no existe no fuerza ganador`() {
        val result = compare("""{"strategies":[{"id":"E0","score":50.0}],"recommended_strategy_id":"E9"}""")
        assertEquals("E9", result.recommendedId)
        assertNull(result.recommended)
    }

    @Test
    fun `respuesta vacia y campos desconocidos no rompen`() {
        val result = compare("""{"nuevo_campo":{"x":1}}""")
        assertTrue(result.strategies.isEmpty())
        assertNull(result.recommended)
        assertTrue(result.limitations.isEmpty())
    }

    @Test
    fun `menos de tres estrategias se muestran tal cual sin inventar`() {
        val result = compare(
            """{"strategies":[{"id":"E0","name":"Actual"},{"id":"E1","name":"Cuidar suelo"}],
                "limitations":["E2 excluida: demanda hídrica alta en secano"]}"""
        )
        assertEquals(listOf("E0", "E1"), result.strategies.map { it.id })
        assertEquals("E2 excluida: demanda hídrica alta en secano", result.limitations.single())
    }

    // ---------- Indicadores ----------

    @Test
    fun `grilla mensual con series y celdas null`() {
        val result = DtoMappers.toIndicators(
            json.parseToJsonElement(
                """
                {"last_observation_date":"2026-11-10",
                 "indicators":[{"id":"dry_spell_max","name":"Racha seca","value":24,"unit":"días","source":"NASA POWER",
                                "quality":{"valid_days":4012,"missing_pct":0.4}}],
                 "monthly_grid":{"indicators":[{"id":"dry_spell_max","cells":[
                    {"year":2015,"month":1,"value":6},{"year":2015,"month":2,"value":null},{"year":2015,"month":13,"value":9}]}]}}
                """
            )
        )
        val summary = result.summaries.single()
        assertEquals("24", summary.valueText)
        assertEquals(4012, summary.validDays)
        val grid = result.grids.single()
        assertEquals("Racha seca", grid.name)
        assertEquals("días", grid.unit)
        assertEquals(2, grid.cells.size)
        assertEquals(6.0, grid.cell(2015, 1)!!.value!!, 0.0)
        assertNull(grid.cell(2015, 2)!!.value)
        assertNull("mes inválido descartado", grid.cell(2015, 13))
    }

    @Test
    fun `grilla como lista plana y como mapa por año`() {
        val flat = DtoMappers.toIndicators(
            json.parseToJsonElement("""{"grid":[{"year":2020,"month":3,"hot_days":4,"dry_spell":11},{"period":"2020-04","hot_days":null,"dry_spell":15}]}""")
        )
        assertEquals(setOf("hot_days", "dry_spell"), flat.grids.map { it.id }.toSet())
        val hot = flat.grids.first { it.id == "hot_days" }
        assertEquals(4.0, hot.cell(2020, 3)!!.value!!, 0.0)
        assertNull(hot.cell(2020, 4)!!.value)

        val byYear = DtoMappers.toIndicators(
            json.parseToJsonElement("""{"heatmap":{"dry_spell":{"unit":"días","values":{"2015":[1,2,3,4,5,6,7,8,9,10,11,null]}}}}""")
        )
        val series = byYear.grids.single()
        assertEquals(12, series.cells.size)
        assertEquals(11.0, series.cell(2015, 11)!!.value!!, 0.0)
        assertNull(series.cell(2015, 12)!!.value)
    }

    @Test
    fun `sin grilla no se fabrica ninguna`() {
        val result = DtoMappers.toIndicators(json.parseToJsonElement("""{"indicators":[{"id":"x","value":1}]}"""))
        assertTrue(result.grids.isEmpty())
    }

    // ---------- Capas ----------

    @Test
    fun `siempre hay cinco capas y las ausentes quedan no disponibles`() {
        val layers = DtoMappers.toMapLayers(
            json.parseToJsonElement(
                """{"layers":[
                    {"id":"rain","status":"regional","value":612.4,"unit":"mm","source":"NASA POWER"},
                    {"id":"vegetation","tile_url":"https://example.org/{z}/{y}/{x}.png","source":"GIBS"},
                    {"id":"heat","status":"available","value":38}
                ]}"""
            )
        )
        assertEquals(LayerKind.entries.toList(), layers.map { it.kind })
        assertEquals(LayerAvailability.REGIONAL, layers.first { it.kind == LayerKind.RAIN }.availability)
        assertEquals(LayerAvailability.AVAILABLE, layers.first { it.kind == LayerKind.VEGETATION }.availability)
        assertEquals("https://example.org/{z}/{y}/{x}.png", layers.first { it.kind == LayerKind.VEGETATION }.tileUrlTemplate)
        // «available» sin ráster: no se inventa una capa dibujable, queda como información regional.
        assertEquals(LayerAvailability.REGIONAL, layers.first { it.kind == LayerKind.HEAT }.availability)
        assertNull(layers.first { it.kind == LayerKind.HEAT }.tileUrlTemplate)
        assertEquals(LayerAvailability.UNAVAILABLE, layers.first { it.kind == LayerKind.SOIL_MOISTURE }.availability)
        assertEquals(LayerAvailability.UNAVAILABLE, layers.first { it.kind == LayerKind.WATER }.availability)
    }

    // ---------- Nodo de campo ----------

    private val pdfReading = """
        {"schema_version":"1.0","node_id":"rotasat-node-01","source":"physical","scenario":null,"ts":"2026-11-14T20:15:00Z",
         "internal":{"depth_cm":7,"zones":[
           {"id":"A","soil_moisture_vwc":0.11,"soil_moisture_raw":2710,"soil_temp_c":24.6,"flags":[]},
           {"id":"B","soil_moisture_vwc":0.19,"soil_moisture_raw":2290,"soil_temp_c":23.9,"flags":[]},
           {"id":"C","soil_moisture_vwc":0.31,"soil_moisture_raw":1840,"soil_temp_c":23.1,"flags":[]}]},
         "external":{"bme680":{"t_c":26.1,"rh_pct":48.2,"p_hpa":965.0,"gas_kohm":85.4},"dht11":{"t_c":26.0,"rh_pct":50.0},
                     "ldr":{"raw":1830,"mv":1480,"light_pct":45.0,"r_fixed_ohm":2200}},
         "calibration":{"moisture_method":"gravimetric_3pt","date":"2026-11-14","ds18b20_offsets_c":[0.1,-0.2,0.0]},
         "quality_flags":[]}
    """

    @Test
    fun `lectura del contrato v1 y humedad vwc a porcentaje por cien`() {
        val readings = DtoMappers.toFieldReadings(json.parseToJsonElement("[$pdfReading]"), json)
        val reading = readings.readings.single()
        assertEquals(NodeSource.PHYSICAL, reading.source)
        assertEquals(0.11, reading.zone("A")!!.moistureVwc!!, 1e-9)
        assertEquals(11.0, reading.zone("A")!!.moisturePct!!, 1e-9)
        assertEquals(31.0, reading.zone("C")!!.moisturePct!!, 1e-9)
        assertEquals(23.9, reading.zone("B")!!.soilTempC!!, 1e-9)
        assertEquals(965.0, reading.bmePressureHpa!!, 1e-9)
        assertEquals("gravimetric_3pt", reading.calibrationMethod)
        assertEquals(1794687300000L, reading.epochMillis)
        assertEquals(0, readings.skipped)
    }

    @Test
    fun `datos faltantes del nodo quedan en null y las lecturas invalidas se cuentan`() {
        val root = json.parseToJsonElement(
            """{"readings":[
                 {"source":"simulated","scenario":"S5","ts":"2026-11-14T20:25:00Z",
                  "internal":{"zones":[{"id":"A","soil_moisture_vwc":null,"soil_temp_c":null,"flags":["ds18b20_A_disconnected"]}]},
                  "external":{"dht11":{"t_c":null,"rh_pct":null}},"quality_flags":["dht11_read_fail"]},
                 {"source":"simulated","ts":"2026-11-14T20:15:00Z","internal":{"zones":"esto no es una lista"}},
                 {"source":"wokwi","ts":"2026-11-14T20:05:00Z"}
               ]}"""
        )
        val result = DtoMappers.toFieldReadings(root, json)
        assertEquals(1, result.skipped)
        assertEquals(2, result.readings.size)
        // Orden cronológico: la más reciente al final.
        assertEquals(NodeSource.WOKWI, result.readings.first().source)
        val last = result.readings.last()
        assertEquals(NodeSource.SIMULATED, last.source)
        assertNull(last.zone("A")!!.moistureVwc)
        assertNull(last.zone("A")!!.moisturePct)
        assertNull(last.zone("A")!!.soilTempC)
        assertNull(last.dhtTempC)
        assertNull(last.bmeTempC)
        assertNull(last.calibrationMethod)
        assertEquals(listOf("dht11_read_fail"), last.qualityFlags)
    }

    @Test
    fun `origen desconocido nunca se trata como fisico`() {
        assertEquals(NodeSource.UNKNOWN, NodeSource.from("sensor"))
        assertEquals(NodeSource.UNKNOWN, NodeSource.from(null))
        assertEquals(NodeSource.SIMULATED, NodeSource.from("simulated"))
        assertEquals("FÍSICO", NodeSource.from("physical").badge)
    }

    @Test
    fun `cero lecturas y estado del nodo`() {
        assertTrue(DtoMappers.toFieldReadings(json.parseToJsonElement("[]"), json).readings.isEmpty())
        assertTrue(DtoMappers.toFieldReadings(json.parseToJsonElement("""{"readings":[]}"""), json).readings.isEmpty())
        val status = DtoMappers.toNodeStatus(
            json.parseToJsonElement(
                """{"last_contact":"2026-11-14T20:15:02Z","source":"simulated","calibration":{"moisture_method":"relative_2pt"},
                    "quality_flags":["moisture_uncalibrated"],"power_context":{"date":"2026-11-14","t2m_c":27.4,"rh2m_pct":58}}"""
            )
        )
        assertEquals("2026-11-14T20:15:02Z", status.lastContact)
        assertEquals(NodeSource.SIMULATED, status.source)
        assertEquals("relative_2pt", status.calibrationMethod)
        assertEquals(27.4, status.regionalContext!!.t2mC!!, 1e-9)
        val empty = DtoMappers.toNodeStatus(JsonObject(emptyMap()))
        assertNull(empty.lastContact)
        assertNull(empty.regionalContext)
    }
}
