package com.example.farmealotu.data.remote

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * JSON tolerante: ignora campos nuevos del backend y acepta números donde se esperaba texto.
     * No se activa coerceInputValues: un null se queda null.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** Valida y normaliza una URL base (http/https, con barra final). Devuelve null si no sirve. */
    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        val parsed: HttpUrl = withSlash.toHttpUrlOrNull() ?: return null
        return parsed.toString()
    }

    fun create(baseUrl: String, debugLogging: Boolean): FarmealoTuApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            // El bootstrap del backend puede tardar unos segundos (meta del plan: < 3 s).
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (debugLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
            .create(FarmealoTuApi::class.java)
    }
}
