package com.example.farmealotu.data.remote

import com.example.farmealotu.data.model.CompareRequestDto
import com.example.farmealotu.data.model.CompareResponseDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints del PDF (4.8). Android solo LEE el nodo: el POST /api/field-node/readings
 * lo usan el simulador, Wokwi y el ESP32, no la app.
 *
 * Las respuestas cuya forma interna aún no está congelada llegan como JsonElement
 * y se interpretan en DtoMappers.
 */
interface FarmealoTuApi {

    @GET("api/health")
    suspend fun health(): JsonElement

    @GET("api/context/cuatro-canadas")
    suspend fun context(): JsonElement

    @GET("api/nasa/status")
    suspend fun nasaStatus(): JsonElement

    @GET("api/indicators")
    suspend fun indicators(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("period") period: String? = null,
    ): JsonElement

    @POST("api/rotations/compare")
    suspend fun compareRotations(@Body request: CompareRequestDto): CompareResponseDto

    @GET("api/map/layers")
    suspend fun mapLayers(): JsonElement

    @GET("api/sources")
    suspend fun sources(): JsonElement

    @GET("api/field-node/readings")
    suspend fun fieldNodeReadings(
        @Query("node_id") nodeId: String? = null,
        @Query("source") source: String? = null,
        @Query("since") since: String? = null,
        @Query("limit") limit: Int? = null,
    ): JsonElement

    @GET("api/field-node/status")
    suspend fun fieldNodeStatus(): JsonElement
}
