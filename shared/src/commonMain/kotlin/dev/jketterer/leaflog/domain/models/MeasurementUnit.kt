package dev.jketterer.leaflog.domain.models

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A unit a measured value is shown and typed in. Values are stored in canonical units, °C, mL and
 * g, and converted only for display and input.
 */
sealed interface MeasurementUnit {
    val symbol: String

    /** Decimal places the unit is shown and typed to. */
    val decimals: Int

    fun toCanonical(value: Double): Double

    fun fromCanonical(value: Double): Double

    fun toggle(): MeasurementUnit
}

/** Rounded to [decimals] places, trailing zeros dropped: "6.8", "7", "0.05". */
fun formatDecimals(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (value * factor).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val fraction = (abs(scaled) % factor).toString().padStart(decimals, '0').trimEnd('0')
    val whole = "$sign${abs(scaled) / factor}"
    return if (fraction.isEmpty()) whole else "$whole.$fraction"
}
