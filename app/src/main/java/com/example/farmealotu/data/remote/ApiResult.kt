package com.example.farmealotu.data.remote

import com.example.farmealotu.data.model.asArrayOrNull
import com.example.farmealotu.data.model.asObjectOrNull
import com.example.farmealotu.data.model.asTextOrNull
import com.example.farmealotu.data.model.firstText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val failure: ApiFailure) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

data class FieldError(val field: String, val message: String)

/** Fallos que la interfaz sabe explicar al agricultor. Ninguno cierra la app. */
sealed interface ApiFailure {
    val title: String
    val message: String

    data object Timeout : ApiFailure {
        override val title = "El servidor tardó demasiado"
        override val message = "No llegó respuesta a tiempo. Revisá la señal y volvé a intentar."
    }

    data class Network(val detail: String?) : ApiFailure {
        override val title = "Sin conexión con el servidor"
        override val message = "No se pudo llegar al servidor de FarmealoTu. Revisá tu conexión o la dirección configurada."
    }

    data class Validation(val fields: List<FieldError>, val detail: String?) : ApiFailure {
        override val title = "Hay datos que el servidor no aceptó"
        override val message: String
            get() = when {
                fields.isNotEmpty() -> fields.joinToString("\n") { "• ${it.field}: ${it.message}" }
                detail != null -> detail
                else -> "Revisá tus respuestas y volvé a intentar."
            }
    }

    data class Server(val code: Int) : ApiFailure {
        override val title = "El servidor tuvo un problema"
        override val message = "Error interno del servidor (código $code). No es un problema de tu teléfono; probá de nuevo en un momento."
    }

    data class Http(val code: Int) : ApiFailure {
        override val title = "El servidor no pudo responder"
        override val message = "Respuesta inesperada (código $code). Puede que esa función todavía no esté disponible en el servidor."
    }

    data class Parse(val detail: String?) : ApiFailure {
        override val title = "No se pudo leer la respuesta"
        override val message = "El servidor respondió con un formato que esta versión de la app no entiende."
    }

    data class Unknown(val detail: String?) : ApiFailure {
        override val title = "Algo salió mal"
        override val message = "Ocurrió un error inesperado. Volvé a intentar."
    }
}

object ApiErrorMapper {

    fun fromThrowable(error: Throwable, json: Json): ApiFailure = when (error) {
        is SocketTimeoutException -> ApiFailure.Timeout
        is HttpException -> fromHttp(error.code(), runCatching { error.response()?.errorBody()?.string() }.getOrNull(), json)
        is IOException -> ApiFailure.Network(error.message)
        is SerializationException, is IllegalArgumentException -> ApiFailure.Parse(error.message)
        else -> ApiFailure.Unknown(error.message)
    }

    fun fromHttp(code: Int, body: String?, json: Json): ApiFailure = when {
        code == 422 || code == 400 -> parseValidation(body, json)
        code in 500..599 -> ApiFailure.Server(code)
        else -> ApiFailure.Http(code)
    }

    /** FastAPI: {"detail":[{"loc":["body","site","lat"],"msg":"...","type":"..."}]} o {"detail":"texto"}. */
    fun parseValidation(body: String?, json: Json): ApiFailure.Validation {
        val root: JsonElement? = body?.takeIf { it.isNotBlank() }?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
        val detail = root.asObjectOrNull()?.get("detail")
        val fields = (detail as? JsonArray)?.mapNotNull { item ->
            val obj = item.asObjectOrNull() ?: return@mapNotNull null
            val message = obj.firstText("msg", "message") ?: return@mapNotNull null
            val location = obj["loc"].asArrayOrNull().orEmpty()
                .mapNotNull { it.asTextOrNull() }
                .filterNot { it == "body" || it == "query" }
                .joinToString(" › ")
            FieldError(field = location.ifEmpty { "dato" }, message = message)
        }.orEmpty()
        return ApiFailure.Validation(fields = fields, detail = detail.asTextOrNull())
    }
}

/** Ejecuta una llamada y convierte cualquier excepción en un [ApiFailure]; respeta la cancelación. */
suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        ApiResult.Failure(ApiErrorMapper.fromThrowable(error, json))
    }
