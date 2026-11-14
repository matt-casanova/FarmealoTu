package com.example.farmealotu.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.repository.FarmealoTuRepository
import com.example.farmealotu.domain.ComparisonResult
import com.example.farmealotu.domain.Concern
import com.example.farmealotu.domain.Crop
import com.example.farmealotu.domain.FarmerInput
import com.example.farmealotu.domain.HealthInfo
import com.example.farmealotu.domain.IndicatorsResult
import com.example.farmealotu.domain.InfoSection
import com.example.farmealotu.domain.Irrigation
import com.example.farmealotu.domain.NasaStatus
import com.example.farmealotu.domain.PilotArea
import com.example.farmealotu.domain.Priorities
import com.example.farmealotu.domain.SourceRef
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Estado compartido del recorrido Inicio -> 5 preguntas -> Resultado -> 4 módulos.
 *
 * Las respuestas del agricultor se guardan en [SavedStateHandle] (sobreviven a la rotación y al
 * cierre del proceso); los resultados viven en el ViewModel (sobreviven a la rotación).
 * Este ViewModel NO calcula puntajes: solo normaliza los pesos y los reenvía al backend.
 */
class MainViewModel(
    private val savedState: SavedStateHandle,
    private val repositoryProvider: () -> FarmealoTuRepository,
    private val settings: StateFlow<ConnectionSettings>,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _input = MutableStateFlow(restoreInput())
    val input: StateFlow<FarmerInput> = _input.asStateFlow()

    /** Pesos que el agricultor está eligiendo (pueden no sumar 1; se normalizan al enviar). */
    private val _priorities = MutableStateFlow(restorePriorities())
    val priorities: StateFlow<Priorities> = _priorities.asStateFlow()

    /** Pesos y respuestas con los que se obtuvo la comparación que está en pantalla. */
    private val _appliedPriorities = MutableStateFlow<Priorities?>(null)
    val appliedPriorities: StateFlow<Priorities?> = _appliedPriorities.asStateFlow()

    private val _appliedInput = MutableStateFlow<FarmerInput?>(null)
    val appliedInput: StateFlow<FarmerInput?> = _appliedInput.asStateFlow()

    private val _compare = MutableStateFlow<UiState<ComparisonResult>>(UiState.Idle)
    val compare: StateFlow<UiState<ComparisonResult>> = _compare.asStateFlow()

    private val _health = MutableStateFlow<UiState<HealthInfo>>(UiState.Idle)
    val health: StateFlow<UiState<HealthInfo>> = _health.asStateFlow()

    private val _nasaStatus = MutableStateFlow<UiState<NasaStatus>>(UiState.Idle)
    val nasaStatus: StateFlow<UiState<NasaStatus>> = _nasaStatus.asStateFlow()

    private val _indicators = MutableStateFlow<UiState<IndicatorsResult>>(UiState.Idle)
    val indicators: StateFlow<UiState<IndicatorsResult>> = _indicators.asStateFlow()

    private val _sources = MutableStateFlow<UiState<List<SourceRef>>>(UiState.Idle)
    val sources: StateFlow<UiState<List<SourceRef>>> = _sources.asStateFlow()

    private val _context = MutableStateFlow<UiState<List<InfoSection>>>(UiState.Idle)
    val context: StateFlow<UiState<List<InfoSection>>> = _context.asStateFlow()

    private var compareJob: Job? = null
    private var statusJob: Job? = null
    private var indicatorsJob: Job? = null
    private var sourcesJob: Job? = null

    init {
        viewModelScope.launch {
            // Al cambiar de servidor o de modo (real <-> demo) se descarta todo lo mostrado:
            // los resultados de demo nunca quedan mezclados con los reales.
            settings.drop(1).collect { onConnectionChanged() }
        }
        refreshStatus()
    }

    // ---------------------------------------------------------------- respuestas

    fun setCurrentCrop(crop: Crop?) = updateInput { it.copy(currentCrop = crop?.takeIf { c -> c != Crop.FALLOW }) }

    fun setPreviousCrop(crop: Crop?) = updateInput { it.copy(previousCrop = crop) }

    /** Elegir la preocupación fija los pesos de partida (editables después). */
    fun setConcern(concern: Concern) {
        updateInput { it.copy(concern = concern) }
        setPriorities(concern.startingPriorities)
    }

    fun setIrrigation(irrigation: Irrigation) = updateInput { it.copy(irrigation = irrigation) }

    fun setNotes(notes: String) = updateInput { it.copy(notes = notes.take(MAX_NOTES)) }

    fun toggleExcludedCrop(crop: Crop) = updateInput {
        val excluded = if (crop in it.excludedCrops) it.excludedCrops - crop else it.excludedCrops + crop
        it.copy(excludedCrops = excluded)
    }

    /** @return false si el punto queda fuera del área piloto (no se guarda). */
    fun setLocation(lat: Double, lon: Double): Boolean {
        if (!PilotArea.contains(lat, lon)) return false
        updateInput { it.copy(lat = lat, lon = lon) }
        return true
    }

    fun resetLocation() = updateInput { it.copy(lat = PilotArea.LAT, lon = PilotArea.LON) }

    fun setPriorities(priorities: Priorities) {
        _priorities.value = priorities
        savedState[KEY_PW] = priorities.water
        savedState[KEY_PS] = priorities.soil
        savedState[KEY_PR] = priorities.risk
    }

    private fun updateInput(transform: (FarmerInput) -> FarmerInput) {
        val updated = transform(_input.value)
        _input.value = updated
        savedState[KEY_CURRENT] = updated.currentCrop?.name
        savedState[KEY_PREVIOUS] = updated.previousCrop?.name
        savedState[KEY_CONCERN] = updated.concern?.name
        savedState[KEY_IRRIGATION] = updated.irrigation?.name
        savedState[KEY_NOTES] = updated.notes
        savedState[KEY_LAT] = updated.lat
        savedState[KEY_LON] = updated.lon
        savedState[KEY_EXCLUDED] = updated.excludedCrops.joinToString(",") { it.name }
    }

    // ---------------------------------------------------------------- llamadas

    /** POST /api/rotations/compare con las respuestas y los pesos normalizados (suman 1). */
    fun submitCompare() {
        val currentInput = _input.value
        val normalized = _priorities.value.normalized()
        if (!currentInput.isComplete) {
            _compare.value = UiState.Error(ApiFailure.Validation(emptyList(), "Faltan respuestas: completá las 5 preguntas."))
            return
        }
        if (normalized == null) {
            _compare.value = _compare.value.reduce(
                ApiResult.Failure(ApiFailure.Validation(emptyList(), "Elegí al menos una prioridad mayor que cero.")),
                repositoryProvider().origin, clock(),
            )
            return
        }
        val repository = repositoryProvider()
        compareJob?.cancel()
        _compare.value = _compare.value.toLoading()
        compareJob = viewModelScope.launch {
            val result = repository.compare(currentInput, normalized)
            _compare.value = _compare.value.reduce(result, repository.origin, clock())
            if (result is ApiResult.Success) {
                _appliedPriorities.value = normalized
                _appliedInput.value = currentInput
                // Los indicadores dependen del punto: se recargan si cambió la ubicación.
                val loaded = _indicators.value
                if (loaded is UiState.Success && (loadedLat != currentInput.lat || loadedLon != currentInput.lon)) {
                    loadIndicators(force = true)
                }
            }
        }
    }

    private var loadedLat: Double? = null
    private var loadedLon: Double? = null

    fun refreshStatus() {
        val repository = repositoryProvider()
        statusJob?.cancel()
        _health.value = _health.value.toLoading()
        _nasaStatus.value = _nasaStatus.value.toLoading()
        statusJob = viewModelScope.launch {
            _health.value = _health.value.reduce(repository.health(), repository.origin, clock())
            _nasaStatus.value = _nasaStatus.value.reduce(repository.nasaStatus(), repository.origin, clock())
        }
    }

    fun loadIndicators(force: Boolean = false) {
        if (!force && (_indicators.value is UiState.Success || _indicators.value is UiState.Loading)) return
        val repository = repositoryProvider()
        val point = _input.value
        indicatorsJob?.cancel()
        _indicators.value = _indicators.value.toLoading()
        indicatorsJob = viewModelScope.launch {
            val result = repository.indicators(point.lat, point.lon, null)
            _indicators.value = _indicators.value.reduce(result, repository.origin, clock())
            if (result is ApiResult.Success) {
                loadedLat = point.lat
                loadedLon = point.lon
            }
        }
    }

    fun loadSources(force: Boolean = false) {
        if (!force && (_sources.value is UiState.Success || _sources.value is UiState.Loading)) return
        val repository = repositoryProvider()
        sourcesJob?.cancel()
        _sources.value = _sources.value.toLoading()
        _context.value = _context.value.toLoading()
        sourcesJob = viewModelScope.launch {
            _sources.value = _sources.value.reduce(repository.sources(), repository.origin, clock()) { it.isEmpty() }
            _context.value = _context.value.reduce(repository.context(), repository.origin, clock()) { it.isEmpty() }
        }
    }

    private fun onConnectionChanged() {
        listOf(compareJob, statusJob, indicatorsJob, sourcesJob).forEach { it?.cancel() }
        _compare.value = UiState.Idle
        _appliedPriorities.value = null
        _appliedInput.value = null
        _health.value = UiState.Idle
        _nasaStatus.value = UiState.Idle
        _indicators.value = UiState.Idle
        _sources.value = UiState.Idle
        _context.value = UiState.Idle
        loadedLat = null
        loadedLon = null
        refreshStatus()
    }

    // ---------------------------------------------------------------- restauración

    private fun restoreInput(): FarmerInput = FarmerInput(
        currentCrop = enumOrNull<Crop>(savedState[KEY_CURRENT]),
        previousCrop = enumOrNull<Crop>(savedState[KEY_PREVIOUS]),
        concern = enumOrNull<Concern>(savedState[KEY_CONCERN]),
        irrigation = enumOrNull<Irrigation>(savedState[KEY_IRRIGATION]),
        notes = savedState[KEY_NOTES] ?: "",
        lat = savedState[KEY_LAT] ?: PilotArea.LAT,
        lon = savedState[KEY_LON] ?: PilotArea.LON,
        excludedCrops = (savedState.get<String>(KEY_EXCLUDED) ?: "")
            .split(',').mapNotNull { enumOrNull<Crop>(it) }.toSet(),
    )

    private fun restorePriorities(): Priorities {
        val water: Double? = savedState[KEY_PW]
        val soil: Double? = savedState[KEY_PS]
        val risk: Double? = savedState[KEY_PR]
        return if (water != null && soil != null && risk != null) Priorities(water, soil, risk) else Priorities.DEFAULT
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        name?.takeIf { it.isNotBlank() }?.let { value -> enumValues<E>().firstOrNull { it.name == value } }

    private companion object {
        const val MAX_NOTES = 500
        const val KEY_CURRENT = "input_current"
        const val KEY_PREVIOUS = "input_previous"
        const val KEY_CONCERN = "input_concern"
        const val KEY_IRRIGATION = "input_irrigation"
        const val KEY_NOTES = "input_notes"
        const val KEY_LAT = "input_lat"
        const val KEY_LON = "input_lon"
        const val KEY_EXCLUDED = "input_excluded"
        const val KEY_PW = "priority_water"
        const val KEY_PS = "priority_soil"
        const val KEY_PR = "priority_risk"
    }
}
