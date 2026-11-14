package com.example.farmealotu.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.data.remote.ApiClient
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.repository.FakeFarmealoTuRepository
import com.example.farmealotu.data.repository.FarmealoTuRepository
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.DataOrigin
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.ui.components.HeatScale
import com.example.farmealotu.ui.screens.field.calibrationLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val demo = FakeFarmealoTuRepository(ApiClient.json, latencyMillis = 0)
    private val settings = MutableStateFlow(ConnectionSettings("http://10.0.2.2:8000/", demoMode = false))

    /** Backend de prueba: delega en el fixture, pero permite forzar fallos y registra lo que recibe. */
    private inner class ScriptedRepository(override val origin: DataOrigin = DataOrigin.REMOTE) :
        FarmealoTuRepository by demo {
        var compareFailure: ApiFailure? = null
        var statusFailure: ApiFailure? = null
        val receivedPriorities = mutableListOf<Priorities>()
        val receivedInputs = mutableListOf<FarmerInput>()

        override suspend fun compare(input: FarmerInput, priorities: Priorities): ApiResult<ComparisonResult> {
            receivedPriorities += priorities
            receivedInputs += input
            return compareFailure?.let { ApiResult.Failure(it) } ?: demo.compare(input, priorities)
        }

        override suspend fun health() = statusFailure?.let { ApiResult.Failure(it) } ?: demo.health()
        override suspend fun nasaStatus() = statusFailure?.let { ApiResult.Failure(it) } ?: demo.nasaStatus()
    }

    private val remote = ScriptedRepository()
    private var active: FarmealoTuRepository = remote

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        MainViewModel(handle, repositoryProvider = { active }, settings = settings, clock = { 1_000L })

    private fun MainViewModel.answerAll() {
        setCurrentCrop(Crop.SOY)
        setPreviousCrop(Crop.SORGHUM)
        setConcern(Concern.WATER)
        setIrrigation(Irrigation.NO)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `recorrido completo - responder y comparar muestra resultado real`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        assertEquals(Concern.WATER.startingPriorities, vm.priorities.value)

        vm.submitCompare()
        assertEquals(UiState.Loading, vm.compare.value)
        advanceUntilIdle()

        val state = vm.compare.value as UiState.Success
        assertEquals(DataOrigin.REMOTE, state.origin)
        assertFalse(state.isDemo)
        assertEquals(1_000L, state.fetchedAtMillis)
        assertEquals(4, state.data.strategies.size)
        assertEquals(vm.input.value, vm.appliedInput.value)
    }

    @Test
    fun `los pesos se envian normalizados - suman uno`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        vm.setPriorities(Priorities(1.0, 0.5, 0.5))
        vm.submitCompare()
        advanceUntilIdle()

        val sent = remote.receivedPriorities.single()
        assertEquals(1.0, sent.water + sent.soil + sent.risk, 1e-9)
        assertEquals(0.5, sent.water, 1e-9)
        assertEquals(sent, vm.appliedPriorities.value)
    }

    @Test
    fun `prueba C - cambiar pesos pide un nuevo calculo y actualiza el ranking`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        vm.setPriorities(Priorities(0.8, 0.1, 0.1))
        vm.submitCompare()
        advanceUntilIdle()
        assertEquals("E2", vm.compare.value.dataOrNull!!.recommended!!.id)

        vm.setPriorities(Priorities(0.1, 0.8, 0.1))
        // Mover los controles no cambia nada hasta pedir el recálculo al backend.
        assertEquals("E2", vm.compare.value.dataOrNull!!.recommended!!.id)
        vm.submitCompare()
        assertTrue((vm.compare.value as UiState.Success).refreshing)
        advanceUntilIdle()

        assertEquals("E1", vm.compare.value.dataOrNull!!.recommended!!.id)
        assertEquals(2, remote.receivedPriorities.size)
    }

    @Test
    fun `sin respuestas completas no se llama al servidor`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setCurrentCrop(Crop.SOY)
        vm.submitCompare()
        advanceUntilIdle()
        assertTrue((vm.compare.value as UiState.Error).failure is ApiFailure.Validation)
        assertTrue(remote.receivedInputs.isEmpty())
    }

    @Test
    fun `timeout 500 y 422 terminan en estado de error con reintento posible`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        listOf(ApiFailure.Timeout, ApiFailure.Server(500), ApiFailure.Validation(emptyList(), "dato inválido")).forEach { failure ->
            remote.compareFailure = failure
            vm.submitCompare()
            advanceUntilIdle()
            assertEquals(UiState.Error(failure), vm.compare.value)
        }
        // Reintentar con el servidor recuperado.
        remote.compareFailure = null
        vm.submitCompare()
        advanceUntilIdle()
        assertTrue(vm.compare.value is UiState.Success)
    }

    @Test
    fun `si la actualizacion falla se conserva el resultado anterior marcado como desactualizado`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        vm.submitCompare()
        advanceUntilIdle()
        val before = vm.compare.value.dataOrNull
        val appliedBefore = vm.appliedPriorities.value

        remote.compareFailure = ApiFailure.Network("sin red")
        vm.setPriorities(Priorities(0.1, 0.1, 0.8))
        vm.submitCompare()
        advanceUntilIdle()

        val state = vm.compare.value as UiState.Success
        assertEquals(before, state.data)
        assertEquals(ApiFailure.Network("sin red"), state.refreshError)
        assertFalse(state.refreshing)
        assertEquals("los pesos aplicados siguen siendo los del resultado en pantalla", appliedBefore, vm.appliedPriorities.value)
    }

    @Test
    fun `pesos en cero no se envian`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        vm.setPriorities(Priorities(0.0, 0.0, 0.0))
        vm.submitCompare()
        advanceUntilIdle()
        assertTrue(vm.compare.value is UiState.Error)
        assertTrue(remote.receivedPriorities.isEmpty())
    }

    @Test
    fun `rotacion de pantalla y cierre del proceso - las respuestas se restauran`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.answerAll()
        first.setNotes("Mi terreno suele secarse en invierno")
        first.toggleExcludedCrop(Crop.WHEAT)
        assertTrue(first.setLocation(-17.10, -62.30))
        first.setPriorities(Priorities(0.6, 0.2, 0.2))

        val restored = viewModel(handle)
        assertEquals(first.input.value, restored.input.value)
        assertEquals(Priorities(0.6, 0.2, 0.2), restored.priorities.value)
        assertTrue(restored.input.value.isComplete)
    }

    @Test
    fun `un punto fuera del area piloto no se guarda`() = runTest(dispatcher) {
        val vm = viewModel()
        assertFalse(vm.setLocation(-16.5, -68.15))
        assertEquals(FarmerInput().lat, vm.input.value.lat, 0.0)
    }

    @Test
    fun `al pasar a modo demo se descartan los resultados reales y lo nuevo queda rotulado DEMO`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.answerAll()
        vm.submitCompare()
        advanceUntilIdle()
        assertTrue(vm.compare.value is UiState.Success)

        active = ScriptedRepository(DataOrigin.DEMO)
        settings.value = settings.value.copy(demoMode = true)
        advanceUntilIdle()

        assertEquals("los resultados reales no quedan mezclados con el demo", UiState.Idle, vm.compare.value)
        assertNull(vm.appliedPriorities.value)
        assertTrue((vm.health.value as UiState.Success).isDemo)

        vm.submitCompare()
        advanceUntilIdle()
        assertTrue((vm.compare.value as UiState.Success).isDemo)
    }

    @Test
    fun `backend caido - inicio muestra error de conexion y no se cierra`() = runTest(dispatcher) {
        remote.statusFailure = ApiFailure.Network("sin red")
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(UiState.Error(ApiFailure.Network("sin red")), vm.health.value)
        assertEquals(UiState.Error(ApiFailure.Network("sin red")), vm.nasaStatus.value)
        assertEquals(UiState.Idle, vm.compare.value)
    }

    @Test
    fun `indicadores se cargan una vez y se pueden forzar`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.loadIndicators()
        advanceUntilIdle()
        val first = vm.indicators.value as UiState.Success
        assertNotNull(first.data.grids.firstOrNull())
        vm.loadIndicators()
        assertFalse((vm.indicators.value as UiState.Success).refreshing)
        vm.loadIndicators(force = true)
        assertTrue((vm.indicators.value as UiState.Success).refreshing)
        advanceUntilIdle()
        assertFalse((vm.indicators.value as UiState.Success).refreshing)
    }

    // ---------- Piezas pequeñas de presentación ----------

    @Test
    fun `lista vacia es estado Empty y no Success`() {
        val state: UiState<List<Int>> = UiState.Loading
        assertEquals(UiState.Empty, state.reduce(ApiResult.Success(emptyList()), DataOrigin.REMOTE, 0L) { it.isEmpty() })
        assertTrue(state.reduce(ApiResult.Success(listOf(1)), DataOrigin.REMOTE, 0L) { it.isEmpty() } is UiState.Success)
    }

    @Test
    fun `un dato demo previo no se conserva como respaldo de un fallo real`() {
        val demoState: UiState<Int> = UiState.Success(1, DataOrigin.DEMO, 0L)
        val reduced = demoState.reduce(ApiResult.Failure(ApiFailure.Timeout), DataOrigin.REMOTE, 1L)
        assertEquals(UiState.Error(ApiFailure.Timeout), reduced)
    }

    @Test
    fun `escala del mapa de calor - secuencial divergente y sin datos`() {
        assertNull(HeatScale.from(emptyList()))
        val sequential = HeatScale.from(listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0))!!
        assertFalse(sequential.diverging)
        assertEquals(5, sequential.binCount)
        assertEquals(0, sequential.binIndex(0.0))
        assertEquals(4, sequential.binIndex(50.0))
        assertEquals(2, sequential.binIndex(25.0))
        assertEquals(5, sequential.legend("días").size)

        val diverging = HeatScale.from(listOf(-60.0, -10.0, 5.0, 30.0))!!
        assertTrue(diverging.diverging)
        assertEquals("el cero cae en el rango neutro del medio", 2, diverging.binIndex(0.0))
        assertEquals(0, diverging.binIndex(-60.0))
        assertEquals(4, diverging.binIndex(60.0))

        assertEquals(1, HeatScale.from(listOf(7.0, 7.0))!!.binCount)
    }

    @Test
    fun `la insignia de calibracion dice lo que dice el registro`() {
        assertTrue(calibrationLabel("gravimetric_3pt", emptyList()).contains("3 puntos"))
        assertTrue(calibrationLabel("relative_2pt", emptyList()).contains("relative_2pt"))
        assertTrue(calibrationLabel(null, emptyList()).contains("no informada"))
        assertTrue(calibrationLabel("gravimetric_3pt", listOf("moisture_uncalibrated")).contains("SIN calibrar"))
    }
}
