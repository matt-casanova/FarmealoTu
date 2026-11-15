package com.example.farmealotu.data

import com.example.farmealotu.data.model.DtoMappers
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.domain.FieldReadings
import com.example.farmealotu.domain.MoistureKind
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.moistureKindFor
import com.example.farmealotu.ui.components.formatSimClock
import com.example.farmealotu.ui.screens.field.formatMoisture
import com.example.farmealotu.ui.screens.field.moistureHeadline
import com.example.farmealotu.viewmodel.mergeFieldReadings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Datos de terreno» con el nodo físico (índice relativo del sensor) y el gemelo digital (VWC + sim_time).
 * Extractos REALES de la API (prompt de la app del nodo, 15-nov-2026).
 */
class FieldNodeMoistureTest {

    private val json = ApiClient.json

    private val physical = """{"node_id":"farmealotu-node-01","source":"physical","internal":{"depth_cm":7,"zones":[{"id":"A","flags":["moisture_uncalibrated"],"soil_moisture_raw":2600,"soil_moisture_vwc":null,"soil_moisture_index_pct":39.5,"soil_temp_c":23.7}]},"external":{"dht11":{"t_c":23.5,"rh_pct":48.6},"ldr":{"raw":2100,"mv":1700,"r_fixed_ohm":10000,"light_pct":51.3},"bme680":null}}"""
    private val simulated = """{"node_id":"farmealotu-gemelo-01","source":"simulated","scenario":"S1","sim_time":"2026-11-14T10:20:00Z","internal":{"depth_cm":7,"zones":[{"id":"A","soil_moisture_vwc":0.292,"soil_temp_c":24.7,"flags":[]}]},"external":{"dht11":{"t_c":23.0,"rh_pct":55.0},"ldr":{"light_pct":79.0},"bme680":null}}"""

    private fun read(item: String) = DtoMappers.toFieldReadings(json.parseToJsonElement("[$item]"), json).readings.single()

    @Test
    fun `physical node index is kept apart from VWC and is never converted`() {
        val reading = read(physical)
        val zone = reading.zone("A")!!
        assertEquals(NodeSource.PHYSICAL, reading.source)
        assertNull(zone.moistureVwc)                       // null sigue siendo null, no 0
        assertNull(zone.moisturePct)
        assertEquals(39.5, zone.moistureIndexPct!!, 1e-9)  // el índice tal cual, sin pasar a VWC
        assertEquals(2600.0, zone.moistureRaw!!, 1e-9)
        assertEquals(MoistureKind.INDEX, zone.moistureKind)
        assertEquals(51.3, reading.ldrLightPct!!, 1e-9)
        assertNull(reading.simTime)
        assertNull(reading.bmeTempC)
    }

    @Test
    fun `physical node headline says index, relative and uncalibrated, never soil moisture`() {
        val text = moistureHeadline(read(physical).zone("A")!!)
        assertTrue(text, text.startsWith("Índice de humedad del sensor: "))
        assertTrue(text, text.contains("relativo, sin calibrar"))
        assertTrue(text, text.contains("40 %"))                          // 39.5 → «40 %», sin cortar el «%»
        assertFalse(text, text.startsWith("Humedad del suelo"))
    }

    @Test
    fun `simulated reading uses VWC and reads sim_time`() {
        val reading = read(simulated)
        val zone = reading.zone("A")!!
        assertEquals(NodeSource.SIMULATED, reading.source)
        assertEquals("S1", reading.scenario)
        assertEquals(0.292, zone.moistureVwc!!, 1e-9)
        assertEquals(29.2, zone.moisturePct!!, 1e-9)
        assertNull(zone.moistureIndexPct)
        assertNull(zone.moistureRaw)
        assertEquals(MoistureKind.VWC, zone.moistureKind)
        assertEquals("Humedad del suelo: 29,2 %", moistureHeadline(zone).replace('.', ','))
        assertEquals("2026-11-14T10:20:00Z", reading.simTime)
        assertEquals("14-nov 10:20", formatSimClock(reading.simEpochMillis!!))   // hora tal como la escribe la simulación
        assertEquals(reading.simEpochMillis, reading.chartMillis)                // el gráfico usa la hora simulada
    }

    @Test
    fun `moisture priority falls back to raw and then to no data`() {
        val rawOnly = """{"node_id":"n","source":"wokwi","ts":"2026-11-15T09:00:00Z","internal":{"zones":[{"id":"A","soil_moisture_raw":2600}]}}"""
        val nothing = """{"node_id":"n","source":"wokwi","ts":"2026-11-15T09:00:00Z","internal":{"zones":[{"id":"A","soil_temp_c":20.0}]}}"""
        val raw = read(rawOnly).zone("A")!!
        assertEquals(MoistureKind.RAW, raw.moistureKind)
        assertEquals("Lectura cruda del sensor: 2600 (sin calibrar)", moistureHeadline(raw))
        val none = read(nothing).zone("A")!!
        assertNull(none.moistureKind)
        assertEquals("Humedad del suelo: sin dato", moistureHeadline(none))
        assertEquals("s/d", formatMoisture(null, MoistureKind.INDEX, missing = "s/d"))
    }

    @Test
    fun `chart never mixes units within one source`() {
        assertEquals(MoistureKind.INDEX, moistureKindFor(listOf(read(physical))))
        assertEquals(MoistureKind.VWC, moistureKindFor(listOf(read(simulated))))
        assertNull(moistureKindFor(emptyList()))
    }

    @Test
    fun `merge keeps the sources that answered and orders them`() {
        val phys = ApiResult.Success(FieldReadings(listOf(read(physical)), 0))
        val sim = ApiResult.Success(FieldReadings(listOf(read(simulated)), 1))
        val down = ApiResult.Failure(ApiFailure.Network("sin red"))
        val merged = mergeFieldReadings(listOf(sim, down, phys)) as ApiResult.Success
        assertEquals(setOf(NodeSource.PHYSICAL, NodeSource.SIMULATED), merged.data.readings.map { it.source }.toSet())
        assertEquals(1, merged.data.skipped)
        assertTrue(mergeFieldReadings(listOf(down, down)) is ApiResult.Failure)
    }
}
