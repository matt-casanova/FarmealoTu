package com.example.farmealotu.ui.components

import com.example.farmealotu.data.model.formatNumber
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "BO"))
private val DAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "BO"))
private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "BO"))

val MONTH_NAMES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
val MONTH_INITIALS = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

/** Fecha de la API en formato local. Si no se reconoce, se muestra tal cual llegó. */
fun formatApiDate(text: String?): String? {
    val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return LocalDate.parse(value).format(DAY) }
    runCatching { return DAY_TIME.format(Instant.parse(value).atZone(ZoneId.systemDefault())) }
    runCatching { return DAY_TIME.format(OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())) }
    return value
}

fun formatClock(epochMillis: Long): String =
    DAY_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatTimeOfDay(epochMillis: Long): String =
    TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** Número o «sin dato». Nunca convierte un null en 0. */
fun formatOrMissing(value: Double?, decimals: Int = 1, unit: String? = null, missing: String = "sin dato"): String =
    if (value == null) missing else formatNumber(value, decimals) + (unit?.let { " $it" } ?: "")

fun formatPercentWeight(fraction: Double): String = "${Math.round(fraction * 100)} %"

fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.4f", value)

/** Eje de puntajes: 0–100 según el plan; si el backend usara 0–1 se adapta sin reescalar valores. */
fun scoreAxisMax(values: List<Double?>): Double {
    val max = values.filterNotNull().maxOrNull() ?: return 100.0
    return when {
        max <= 1.0 -> 1.0
        max <= 100.0 -> 100.0
        else -> max
    }
}

fun scoreDecimals(axisMax: Double): Int = if (axisMax <= 1.0) 2 else 0

private val LONG_NUMBER = Regex("""-?\d{5,}(\.\d+)?""")

/**
 * Agrupa los miles con un espacio fino («318083» → «318 083») solo si el texto es un número de 5 o más
 * cifras enteras: los años («2023») y los textos quedan igual. No cambia el valor, solo cómo se lee.
 * Se usa espacio (no punto) porque la app escribe los decimales con punto («612.4 mm»).
 */
fun groupThousands(text: String): String {
    val trimmed = text.trim()
    if (!LONG_NUMBER.matches(trimmed)) return text
    val negative = trimmed.startsWith("-")
    val digits = trimmed.removePrefix("-")
    val integer = digits.substringBefore('.')
    val decimals = digits.substringAfter('.', "")
    val grouped = integer.reversed().chunked(3).joinToString(" ").reversed()
    return (if (negative) "-" else "") + grouped + (if (decimals.isNotEmpty()) ".$decimals" else "")
}
