package com.example.farmealotu.data

import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.data.remote.ApiErrorMapper
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.remote.safeApiCall
import com.example.farmealotu.data.repository.FakeFarmealoTuRepository
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.DataOrigin
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.domain.LayerAvailability
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.Priorities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant

class ApiErrorAndFakeTest {

    private val json = ApiClient.json

    private fun httpError(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    // ---------- Estados de error ----------

    @Test
    fun `422 de FastAPI se traduce a campos legibles`() {
        val failure = ApiErrorMapper.fromThrowable(
            httpError(422, """{"detail":[{"loc":["body","site","lat"],"msg":"Input should be a valid number","type":"float_parsing"},
                                        {"loc":["body","current_rotation"],"msg":"Field required","type":"missing"}]}"""),
            json,
        )
        failure as ApiFailure.Validation
        assertEquals(listOf("site › lat", "current_rotation"), failure.fields.map { it.field })
        assertTrue(failure.message.contains("Field required"))
    }

    @Test
    fun `422 con detalle de texto o cuerpo ilegible no rompe`() {
        val text = ApiErrorMapper.fromThrowable(httpError(422, """{"detail":"Punto fuera del área piloto"}"""), json) as ApiFailure.Validation
        assertEquals("Punto fuera del área piloto", text.message)
        val garbage = ApiErrorMapper.fromThrowable(httpError(422, "<html>"), json) as ApiFailure.Validation
        assertTrue(garbage.fields.isEmpty())
        assertTrue(garbage.message.isNotBlank())
    }

    @Test
    fun `500 timeout y caida de red tienen su propio estado`() {
        assertEquals(ApiFailure.Server(500), ApiErrorMapper.fromThrowable(httpError(500, "Internal Server Error"), json))
        assertEquals(ApiFailure.Http(404), ApiErrorMapper.fromThrowable(httpError(404, "{}"), json))
        assertEquals(ApiFailure.Timeout, ApiErrorMapper.fromThrowable(SocketTimeoutException("timeout"), json))
        assertTrue(ApiErrorMapper.fromThrowable(ConnectException("refused"), json) is ApiFailure.Network)
        assertTrue(ApiErrorMapper.fromThrowable(UnknownHostException("host"), json) is ApiFailure.Network)
        assertTrue(ApiErrorMapper.fromThrowable(SerializationException("bad json"), json) is ApiFailure.Parse)
        assertTrue(ApiErrorMapper.fromThrowable(IllegalStateException("x"), json) is ApiFailure.Unknown)
    }

    @Test
    fun `safeApiCall nunca lanza salvo cancelacion`() = runTest {
        val failure = safeApiCall<String>(json) { throw SocketTimeoutException() }
        assertEquals(ApiResult.Failure(ApiFailure.Timeout), failure)
        assertEquals(ApiResult.Success("ok"), safeApiCall(json) { "ok" })
        try {
            safeApiCall<String>(json) { throw CancellationException("cancelado") }
            fail("la cancelación debe propagarse")
        } catch (expected: CancellationException) {
            // correcto
        }
    }

    @Test
    fun `la url base se valida y normaliza`() {
        assertEquals("http://10.0.2.2:8000/", ApiClient.normalizeBaseUrl("http://10.0.2.2:8000"))
        assertEquals("https://api.ejemplo.org/", ApiClient.normalizeBaseUrl(" https://api.ejemplo.org/ "))
        assertNull(ApiClient.normalizeBaseUrl("192.168.1.20:8000"))
        assertNull(ApiClient.normalizeBaseUrl(""))
        assertNull(ApiClient.normalizeBaseUrl("ftp://servidor"))
    }

    // ---------- Repositorio DEMO ----------

    private val fake = FakeFarmealoTuRepository(json, latencyMillis = 0, now = { Instant.parse("2026-11-14T15:00:00Z") })

    private val input = FarmerInput(
        currentCrop = Crop.SOY, previousCrop = Crop.SORGHUM, concern = Concern.WATER, irrigation = Irrigation.NO,
    )

    private suspend fun compare(priorities: Priorities, farmer: FarmerInput = input) =
        (fake.compare(farmer, priorities) as ApiResult.Success).data

    @Test
    fun `el fixture demo se identifica como DEMO y trae E0 a E3`() = runTest {
        assertEquals(DataOrigin.DEMO, fake.origin)
        val result = compare(Priorities.DEFAULT)
        assertEquals(listOf("E0", "E1", "E2", "E3"), result.strategies.map { it.id })
        assertEquals("Soya → Sorgo", result.current!!.sequence.joinToString(" → ") { it.cropLabel })
        assertNotNull(result.recommended)
        assertTrue(result.limitations.any { it.contains("DEMO") })
        assertTrue(result.strategies.all { it.uncertainty!!.hasRange })
        assertTrue(result.strategies.flatMap { it.evidence }.none { it.source?.contains("NASA") == true })
    }

    @Test
    fun `prueba C - cambiar pesos recalcula de forma reproducible y puede cambiar el orden`() = runTest {
        val water = compare(Priorities(0.8, 0.1, 0.1))
        val soil = compare(Priorities(0.1, 0.8, 0.1))
        assertEquals("E2", water.recommended!!.id)
        assertEquals("E1", soil.recommended!!.id)
        // Mismos pesos => mismo resultado.
        assertEquals(water.strategies.map { it.score }, compare(Priorities(0.8, 0.1, 0.1)).strategies.map { it.score })
        // Los subpuntajes no dependen de los pesos; solo cambia el total.
        assertEquals(water.strategies.map { it.criteria }, soil.strategies.map { it.criteria })
    }

    @Test
    fun `con cultivos excluidos quedan menos opciones y no se fuerza ganador`() = runTest {
        val result = compare(Priorities.DEFAULT, input.copy(excludedCrops = setOf(Crop.MAIZE, Crop.SUNFLOWER)))
        assertEquals(listOf("E0", "E3"), result.strategies.map { it.id })
        assertNull(result.recommended)
        assertTrue(result.limitations.any { it.contains("menos de tres") })
    }

    @Test
    fun `respuestas incompletas devuelven error de validacion`() = runTest {
        val result = fake.compare(FarmerInput(currentCrop = Crop.SOY), Priorities.DEFAULT)
        assertTrue((result as ApiResult.Failure).failure is ApiFailure.Validation)
    }

    @Test
    fun `los demas fixtures respetan las reglas de honestidad`() = runTest {
        val indicators = (fake.indicators(-17.2374, -62.2427) as ApiResult.Success).data
        val cover = indicators.summaries.first { it.id.contains("ndvi") }
        assertNull("sin NDVI no hay valor de cobertura", cover.valueText)
        val grid = indicators.grids.first()
        assertEquals((2015..2026).toList(), grid.years)
        assertNull("meses aún no observados quedan sin dato", grid.cell(2026, 12)!!.value)

        val layers = (fake.mapLayers() as ApiResult.Success).data
        assertEquals(5, layers.size)
        assertTrue("el demo no inventa teselas", layers.all { it.tileUrlTemplate == null })
        assertTrue(layers.none { it.availability == LayerAvailability.AVAILABLE })

        val readings = (fake.fieldReadings() as ApiResult.Success).data
        assertTrue(readings.readings.isNotEmpty())
        assertTrue("el demo del nodo nunca aparece como físico", readings.readings.all { it.source == NodeSource.SIMULATED })
        assertTrue(readings.readings.any { reading -> reading.zone("B")?.soilTempC == null })
        assertTrue((fake.fieldReadings(source = "physical") as ApiResult.Success).data.readings.isEmpty())
    }
}
