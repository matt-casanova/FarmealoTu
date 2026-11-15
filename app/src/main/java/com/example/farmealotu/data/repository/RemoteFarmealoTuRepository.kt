package com.example.farmealotu.data.repository

import com.example.farmealotu.data.model.DtoMappers
import com.example.farmealotu.data.remote.ApiResult
import com.example.farmealotu.data.remote.FarmealoTuApi
import com.example.farmealotu.data.remote.safeApiCall
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
import kotlinx.serialization.json.Json

/** Consume la API FastAPI real. No calcula nada: solo transporta y traduce a modelos de pantalla. */
class RemoteFarmealoTuRepository(
    private val api: FarmealoTuApi,
    private val json: Json,
) : FarmealoTuRepository {

    override val origin: DataOrigin = DataOrigin.REMOTE

    override suspend fun health(): ApiResult<HealthInfo> =
        safeApiCall(json) { DtoMappers.toHealth(api.health()) }

    override suspend fun nasaStatus(): ApiResult<NasaStatus> =
        safeApiCall(json) { DtoMappers.toNasaStatus(api.nasaStatus()) }

    override suspend fun compare(input: FarmerInput, priorities: Priorities): ApiResult<ComparisonResult> =
        safeApiCall(json) {
            DtoMappers.toComparison(api.compareRotations(DtoMappers.toCompareRequest(input, priorities)))
        }

    override suspend fun indicators(lat: Double, lon: Double, period: String?): ApiResult<IndicatorsResult> =
        safeApiCall(json) { DtoMappers.toIndicators(api.indicators(lat, lon, period)) }

    override suspend fun mapLayers(): ApiResult<List<MapLayer>> =
        safeApiCall(json) { DtoMappers.toMapLayers(api.mapLayers()) }

    override suspend fun fieldReadings(nodeId: String?, source: String?, since: String?, limit: Int?): ApiResult<FieldReadings> =
        safeApiCall(json) { DtoMappers.toFieldReadings(api.fieldNodeReadings(nodeId, source, since, limit), json) }

    override suspend fun fieldStatus(): ApiResult<NodeStatus> =
        safeApiCall(json) { DtoMappers.toNodeStatus(api.fieldNodeStatus()) }

    override suspend fun sources(): ApiResult<List<SourceRef>> =
        safeApiCall(json) { DtoMappers.toSources(api.sources()) }

    override suspend fun context(): ApiResult<List<InfoSection>> =
        safeApiCall(json) { DtoMappers.toContextSections(api.context()) }
}
