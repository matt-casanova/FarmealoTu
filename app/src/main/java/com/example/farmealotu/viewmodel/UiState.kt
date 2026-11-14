package com.example.farmealotu.viewmodel

import com.example.farmealotu.data.remote.ApiFailure
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.domain.DataOrigin

/**
 * Estado de cada bloque de datos en pantalla.
 * - [Success.origin] == DEMO => la pantalla muestra la franja «MODO DEMO · DATOS DE EJEMPLO».
 * - [Success.refreshError] != null => se conserva el dato anterior, marcado como desactualizado.
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>

    data object Loading : UiState<Nothing>

    data object Empty : UiState<Nothing>

    data class Success<T>(
        val data: T,
        val origin: DataOrigin,
        val fetchedAtMillis: Long,
        val refreshing: Boolean = false,
        val refreshError: ApiFailure? = null,
    ) : UiState<T> {
        val isDemo: Boolean get() = origin == DataOrigin.DEMO
    }

    data class Error(val failure: ApiFailure) : UiState<Nothing>
}

val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success<T>)?.data

/** Mientras se recarga: conserva lo que había (marcado como «actualizando») o muestra carga. */
fun <T> UiState<T>.toLoading(): UiState<T> = when (this) {
    is UiState.Success -> copy(refreshing = true)
    else -> UiState.Loading
}

/**
 * Combina el resultado nuevo con el estado previo: si la recarga falla y había datos del MISMO
 * origen, se conservan como desactualizados en vez de dejar la pantalla vacía.
 */
fun <T> UiState<T>.reduce(
    result: ApiResult<T>,
    origin: DataOrigin,
    nowMillis: Long,
    isEmpty: (T) -> Boolean = { false },
): UiState<T> = when (result) {
    is ApiResult.Success ->
        if (isEmpty(result.data)) UiState.Empty
        else UiState.Success(result.data, origin, nowMillis)
    is ApiResult.Failure -> {
        val previous = this as? UiState.Success<T>
        if (previous != null && previous.origin == origin) {
            previous.copy(refreshing = false, refreshError = result.failure)
        } else {
            UiState.Error(result.failure)
        }
    }
}
