package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

enum class VolumeUnit {
    MILLILITERS,
    FLUID_OUNCES;


    fun toggle(): VolumeUnit = when (this) {
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

    val symbol: String
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
