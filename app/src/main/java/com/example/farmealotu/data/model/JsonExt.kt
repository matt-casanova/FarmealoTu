package com.example.farmealotu.data.model

import com.example.farmealotu.domain.LabeledValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale

/* Lectura tolerante de JSON. Regla fija: lo que falta o es null se queda en null. */

fun JsonElement?.orNullIfJsonNull(): JsonElement? = this?.takeIf { it !is JsonNull }

fun JsonElement?.asDoubleOrNull(): Double? {
    val primitive = this.orNullIfJsonNull() as? JsonPrimitive ?: return null
    val number = if (primitive.isString) primitive.content.trim().toDoubleOrNull() else primitive.doubleOrNull
    return number?.takeIf { it.isFinite() }
}

fun JsonElement?.asIntOrNull(): Int? = asDoubleOrNull()?.takeIf { it == Math.floor(it) }?.toInt()

fun JsonElement?.asBooleanOrNull(): Boolean? = (this.orNullIfJsonNull() as? JsonPrimitive)?.booleanOrNull

/** Texto de un primitivo (los números se devuelven tal como vienen escritos). */
fun JsonElement?.asTextOrNull(): String? =
    (this.orNullIfJsonNull() as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() }

fun JsonElement?.asObjectOrNull(): JsonObject? = this.orNullIfJsonNull() as? JsonObject

fun JsonElement?.asArrayOrNull(): JsonArray? = this.orNullIfJsonNull() as? JsonArray

fun JsonObject.first(vararg keys: String): JsonElement? =
    keys.firstNotNullOfOrNull { key -> this[key].orNullIfJsonNull() }

fun JsonObject.firstText(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> this[key].asTextOrNull() }

fun JsonObject.firstDouble(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { key -> this[key].asDoubleOrNull() }

private val TEXT_KEYS = arrayOf("text", "message", "question", "description", "reason", "title", "label", "name")

/** Acepta "texto", ["a","b"] o [{"text":"a"}]. */
fun JsonElement?.asStringList(): List<String> = when (val element = this.orNullIfJsonNull()) {
    is JsonArray -> element.mapNotNull { item -> item.asTextOrNull() ?: item.asObjectOrNull()?.firstText(*TEXT_KEYS) }
    is JsonPrimitive -> listOfNotNull(element.asTextOrNull())
    is JsonObject -> listOfNotNull(element.firstText(*TEXT_KEYS))
    else -> emptyList()
}

/** Número para mostrar: sin ceros de relleno, máximo [decimals] decimales. */
fun formatNumber(value: Double, decimals: Int = 1): String {
    val text = String.format(Locale.US, "%.${decimals}f", value)
    return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
}

/** Valor de evidencia: número o texto, tal como lo manda la API. */
fun JsonElement?.asDisplayValue(): String? {
    val primitive = this.orNullIfJsonNull() as? JsonPrimitive ?: return null
    if (primitive.isString) return primitive.content.trim().takeIf { it.isNotEmpty() }
    primitive.booleanOrNull?.let { return if (it) "sí" else "no" }
    return primitive.doubleOrNull?.takeIf { it.isFinite() }?.let { formatNumber(it, 2) }
}

fun prettyKey(key: String): String =
    key.replace('_', ' ').replace('-', ' ').trim().replaceFirstChar { it.titlecase(Locale.ROOT) }

/**
 * Aplana un JSON cualquiera en pares «etiqueta: valor» para poder mostrar lo que diga la API
 * sin inventar su forma. Los null se muestran como «no disponible».
 */
fun JsonElement?.flattenToLabeledValues(prefix: String = "", maxDepth: Int = 3): List<LabeledValue> {
    val element = this ?: return emptyList()
    return when (element) {
        is JsonNull -> if (prefix.isEmpty()) emptyList() else listOf(LabeledValue(prefix, "no disponible"))
        is JsonPrimitive -> {
            val text = element.asDisplayValue() ?: "no disponible"
            listOf(LabeledValue(prefix.ifEmpty { "Valor" }, text))
        }
        is JsonArray -> {
            if (element.all { it is JsonPrimitive }) {
                val joined = element.mapNotNull { it.asDisplayValue() }.joinToString(", ")
                listOf(LabeledValue(prefix.ifEmpty { "Valores" }, joined.ifEmpty { "ninguno" }))
            } else if (maxDepth <= 0) {
                emptyList()
            } else {
                element.flatMapIndexed { index, item ->
                    val label = item.asObjectOrNull()?.firstText("name", "id", "source", "title") ?: "${index + 1}"
                    val itemPrefix = if (prefix.isEmpty()) label else "$prefix · $label"
                    item.flattenToLabeledValues(itemPrefix, maxDepth - 1)
                }
            }
        }
        is JsonObject -> {
            if (maxDepth <= 0) emptyList()
            else element.entries.flatMap { (key, value) ->
                val label = if (prefix.isEmpty()) prettyKey(key) else "$prefix · ${prettyKey(key)}"
                value.flattenToLabeledValues(label, maxDepth - 1)
            }
        }
    }
}
