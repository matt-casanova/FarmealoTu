package com.example.farmealotu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.farmealotu.ConnectionSettings
import com.example.farmealotu.FarmealoTuApp
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.repository.FarmealoTuRepository
import com.example.farmealotu.domain.FieldReadings
import com.example.farmealotu.domain.MapLayer
import com.example.farmealotu.domain.NodeReadingOrder
import com.example.farmealotu.domain.NodeSource
import com.example.farmealotu.domain.NodeStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** «Datos de terreno»: solo LEE el nodo (GET readings + GET status). */
class FieldDataViewModel(
    private val repositoryProvider: () -> FarmealoTuRepository,
    private val settings: StateFlow<ConnectionSettings>,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _readings = MutableStateFlow<UiState<FieldReadings>>(UiState.Idle)
    val readings: StateFlow<UiState<FieldReadings>> = _readings.asStateFlow()

    private val _status = MutableStateFlow<UiState<NodeStatus>>(UiState.Idle)
    val status: StateFlow<UiState<NodeStatus>> = _status.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            settings.drop(1).collect {
                job?.cancel()
                _readings.value = UiState.Idle
                _status.value = UiState.Idle
                refresh()
            }
        }
    }

    fun refresh() {
        if (job?.isActive == true) return
        val repository = repositoryProvider()
        // «Empty» también se recarga mostrando carga solo la primera vez.
        if (_readings.value !is UiState.Empty) _readings.value = _readings.value.toLoading()
        _status.value = _status.value.toLoading()
        job = viewModelScope.launch {
            // Una consulta por origen con límite: el nodo físico (1 lectura cada 10 s) no tapa a la simulación
            // y la pantalla no baja miles de lecturas en cada refresco.
            val perSource = FIELD_SOURCES.map { source ->
                async { repository.fieldReadings(source = source.apiCode, limit = READINGS_PER_SOURCE) }
            }.awaitAll()
            _readings.value = _readings.value.reduce(mergeFieldReadings(perSource), repository.origin, clock()) { it.readings.isEmpty() }
            _status.value = _status.value.reduce(repository.fieldStatus(), repository.origin, clock())
        }
    }

    companion object {
        const val READINGS_PER_SOURCE = 300
        val FIELD_SOURCES = NodeSource.entries.filter { it != NodeSource.UNKNOWN }
    }
}

/**
 * Junta las respuestas por origen. Si al menos una respondió, se muestran sus lecturas (un origen
 * caído no deja la pantalla vacía); si fallaron todas, se informa el primer error.
 */
internal fun mergeFieldReadings(results: List<ApiResult<FieldReadings>>): ApiResult<FieldReadings> {
    val ok = results.filterIsInstance<ApiResult.Success<FieldReadings>>().map { it.data }
    if (ok.isEmpty()) return results.firstOrNull { it is ApiResult.Failure } ?: ApiResult.Success(FieldReadings(emptyList(), 0))
    return ApiResult.Success(
        FieldReadings(ok.flatMap { it.readings }.distinct().sortedWith(NodeReadingOrder), ok.sumOf { it.skipped }),
    )
}

/** «Explorar mi zona»: metadatos de las 5 capas desde GET /api/map/layers. */
class GeoViewModel(
    private val repositoryProvider: () -> FarmealoTuRepository,
    private val settings: StateFlow<ConnectionSettings>,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _layers = MutableStateFlow<UiState<List<MapLayer>>>(UiState.Idle)
    val layers: StateFlow<UiState<List<MapLayer>>> = _layers.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            settings.drop(1).collect {
                job?.cancel()
                _layers.value = UiState.Idle
                load(force = true)
            }
        }
    }

    fun load(force: Boolean = false) {
        if (!force && (_layers.value is UiState.Success || _layers.value is UiState.Loading)) return
        val repository = repositoryProvider()
        job?.cancel()
        _layers.value = _layers.value.toLoading()
        job = viewModelScope.launch {
            _layers.value = _layers.value.reduce(repository.mapLayers(), repository.origin, clock())
        }
    }
}

object AppViewModelFactory {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = (this[APPLICATION_KEY] as FarmealoTuApp).container
            MainViewModel(createSavedStateHandle(), container::repository, container.settings.state)
        }
        initializer {
            val container = (this[APPLICATION_KEY] as FarmealoTuApp).container
            FieldDataViewModel(container::repository, container.settings.state)
        }
        initializer {
            val container = (this[APPLICATION_KEY] as FarmealoTuApp).container
            GeoViewModel(container::repository, container.settings.state)
        }
    }
}
