package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

enum class VolumeUnit : MeasurementUnit {
    MILLILITERS,
    FLUID_OUNCES;

    override val decimals: Int
        get() = when (this) {
            MILLILITERS -> 0
            FLUID_OUNCES -> 1
        }

    override fun toCanonical(value: Double): Double = when (this) {
        MILLILITERS -> value
        FLUID_OUNCES -> value * 29.5735
    }

    override fun fromCanonical(value: Double): Double = when (this) {
        MILLILITERS -> value
        FLUID_OUNCES -> value / 29.5735
    }

    override fun toggle(): VolumeUnit = when (this) {
        MILLILITERS -> FLUID_OUNCES
        FLUID_OUNCES -> MILLILITERS
    }

    /** Converts a display-unit value to milliliters without rounding (preserves precision). */
    fun toMilliliters(value: Int): Double = when (this) {
        MILLILITERS -> value.toDouble()
        FLUID_OUNCES -> value * 29.5735
    }

    /** Converts a milliliter value to the display unit, rounded to Int for display. */
    fun fromMilliliters(ml: Double): Int = when (this) {
        MILLILITERS -> ml.roundToInt()
        FLUID_OUNCES -> (ml / 29.5735).roundToInt()
    }

    override val symbol: String
        get() = when (this) {
            MILLILITERS -> "mL"
            FLUID_OUNCES -> "fl oz"
        }

    val largeSymbol: String
        get() = when (this) {
            MILLILITERS -> "L"
            FLUID_OUNCES -> "gal"
        }
}
