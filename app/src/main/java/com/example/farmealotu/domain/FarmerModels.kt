package com.example.farmealotu.domain

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Cultivos que la app sabe nombrar. Los códigos son los del contrato del PDF (sección 4.4).
 * La lista definitiva la confirma el backend (crops.json / OpenAPI).
 */
enum class Crop(val apiCode: String, val label: String) {
    SOY("soy", "Soya"),
    MAIZE("maize", "Maíz"),
    SORGHUM("sorghum", "Sorgo"),
    SUNFLOWER("sunflower", "Girasol"),
    WHEAT("wheat", "Trigo"),
    FALLOW("fallow", "Barbecho / descanso");

    companion object {
        fun fromApiCode(code: String?): Crop? {
            val clean = code?.trim()?.lowercase(Locale.ROOT) ?: return null
            return entries.firstOrNull { it.apiCode == clean }
        }

        /** Nombre legible de un código de la API; si no se conoce, se muestra el código tal cual. */
        fun labelFor(code: String): String = fromApiCode(code)?.label ?: code
    }
}

sealed interface CropMatch {
    data class Found(val crop: Crop) : CropMatch
    data object Empty : CropMatch
    data class Unknown(val text: String) : CropMatch
    data class Ambiguous(val crops: List<Crop>) : CropMatch
}

/**
 * Diccionario verificable de sinónimos (PDF 4.4). No adivina: si no hay exactamente
 * un cultivo reconocido en el texto, la interfaz pide elegir de la lista.
 */
object CropNormalizer {
    val synonyms: Map<String, Crop> = mapOf(
        "soya" to Crop.SOY,
        "soja" to Crop.SOY,
        "soy" to Crop.SOY,
        "maiz" to Crop.MAIZE,
        "sorgo" to Crop.SORGHUM,
        "girasol" to Crop.SUNFLOWER,
        "trigo" to Crop.WHEAT,
        "barbecho" to Crop.FALLOW,
        "descanso" to Crop.FALLOW,
    )

    /** Minúsculas y sin tildes: «Maíz» -> «maiz». */
    fun normalizeText(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()

    fun match(text: String, allowFallow: Boolean = true): CropMatch {
        val normalized = normalizeText(text)
        if (normalized.isEmpty()) return CropMatch.Empty
        val found = normalized.split(Regex("[^a-z]+"))
            .mapNotNull { synonyms[it] }
            .filter { allowFallow || it != Crop.FALLOW }
            .distinct()
        return when (found.size) {
            0 -> CropMatch.Unknown(text.trim())
            1 -> CropMatch.Found(found.first())
            else -> CropMatch.Ambiguous(found)
        }
    }
}

/**
 * Riego declarado por el agricultor. `rainfed` sale del ejemplo del PDF; los demás códigos
 * están PENDIENTES de validar contra el OpenAPI real (ver docs/UI_BACKEND_CONTRACT.md).
 */
enum class Irrigation(val apiCode: String, val label: String) {
    YES("irrigated", "Sí"),
    NO("rainfed", "No"),
    SOMETIMES("partial", "A veces"),
    UNKNOWN("unknown", "No sé"),
}

/** Pesos de prioridad (agua, suelo, riesgo). Son preferencias del agricultor, no ciencia. */
data class Priorities(val water: Double, val soil: Double, val risk: Double) {

    val isValid: Boolean
        get() = listOf(water, soil, risk).all { it.isFinite() && it >= 0.0 } && (water + soil + risk) > 0.0

    /** Devuelve los pesos escalados para que sumen 1, o null si no se pueden normalizar. */
    fun normalized(): Priorities? {
        if (!isValid) return null
        val sum = water + soil + risk
        return Priorities(water / sum, soil / sum, risk / sum)
    }

    fun approxEquals(other: Priorities, tolerance: Double = 0.005): Boolean =
        abs(water - other.water) < tolerance && abs(soil - other.soil) < tolerance && abs(risk - other.risk) < tolerance

    companion object {
        /** Pesos iniciales del plan (SUPUESTO de producto, sección 3.6). */
        val DEFAULT = Priorities(0.40, 0.35, 0.25)
    }
}

/**
 * «¿Qué te preocupa más?» -> pesos de partida. Solo «Falta de agua -> agua 0.55» figura en el
 * PDF; el resto son SUPUESTOS de la interfaz, visibles y ajustables en «Cambiar mis prioridades».
 */
enum class Concern(val label: String, val startingPriorities: Priorities) {
    WATER("Falta de agua", Priorities(0.55, 0.25, 0.20)),
    SOIL("Cuidar suelo", Priorities(0.25, 0.55, 0.20)),
    LOSSES("Evitar pérdidas", Priorities(0.25, 0.20, 0.55)),
    BALANCE("Quiero equilibrar", Priorities.DEFAULT),
}

/** Punto piloto y caja de alcance. La caja es un SUPUESTO de la UI; el backend valida el alcance real. */
object PilotArea {
    const val NAME = "Cuatro Cañadas"
    const val LAT = -17.2374
    const val LON = -62.2427

    /** ±0.5° ≈ una celda de NASA POWER alrededor del punto piloto. */
    const val HALF_SPAN_DEG = 0.5

    fun contains(lat: Double, lon: Double): Boolean =
        lat.isFinite() && lon.isFinite() &&
            abs(lat - LAT) <= HALF_SPAN_DEG && abs(lon - LON) <= HALF_SPAN_DEG

    fun isPilotPoint(lat: Double, lon: Double): Boolean =
        abs(lat - LAT) < 1e-6 && abs(lon - LON) < 1e-6
}

/** Respuestas a las 5 preguntas. Todo esto es «dato del agricultor» (farmer_provided). */
data class FarmerInput(
    val currentCrop: Crop? = null,
    val previousCrop: Crop? = null,
    val concern: Concern? = null,
    val irrigation: Irrigation? = null,
    val notes: String = "",
    val lat: Double = PilotArea.LAT,
    val lon: Double = PilotArea.LON,
    val excludedCrops: Set<Crop> = emptySet(),
) {
    val isComplete: Boolean
        get() = currentCrop != null && previousCrop != null && concern != null && irrigation != null

    /** Rotación actual en el orden del ejemplo del PDF: [actual, anterior]. */
    val currentRotationCodes: List<String>
        get() = listOfNotNull(currentCrop?.apiCode, previousCrop?.apiCode)
}
