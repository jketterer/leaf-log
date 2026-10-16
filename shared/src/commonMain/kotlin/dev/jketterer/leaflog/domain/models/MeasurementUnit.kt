package dev.jketterer.leaflog.domain.models

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
