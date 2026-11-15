package com.example.farmealotu.data.repository

import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.domain.ComparisonResult
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

/**
 * Única puerta de la interfaz hacia los datos. Dos implementaciones intercambiables:
 * [RemoteFarmealoTuRepository] (API FastAPI real) y [FakeFarmealoTuRepository] (fixtures DEMO).
 * La interfaz nunca mezcla ambas: al cambiar de modo se descartan los resultados en pantalla.
 */
interface FarmealoTuRepository {
    val origin: DataOrigin

    suspend fun health(): ApiResult<HealthInfo>
    suspend fun nasaStatus(): ApiResult<NasaStatus>
    suspend fun compare(input: FarmerInput, priorities: Priorities): ApiResult<ComparisonResult>
    suspend fun indicators(lat: Double, lon: Double, period: String? = null): ApiResult<IndicatorsResult>
    suspend fun mapLayers(): ApiResult<List<MapLayer>>
    suspend fun fieldReadings(nodeId: String? = null, source: String? = null, since: String? = null): ApiResult<FieldReadings>
    suspend fun fieldStatus(): ApiResult<NodeStatus>
    suspend fun sources(): ApiResult<List<SourceRef>>
    suspend fun context(): ApiResult<List<InfoSection>>
}
