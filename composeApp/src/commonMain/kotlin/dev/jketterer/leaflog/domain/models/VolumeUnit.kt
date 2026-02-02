package dev.jketterer.leaflog.domain.models

enum class VolumeUnit {
    MILLILITERS,
    FLUID_OUNCES;

    fun toMilliliters(value: Int): Int = when (this) {
        MILLILITERS -> value
        FLUID_OUNCES -> (value * 29.5735).toInt()
    }

    fun fromMilliliters(ml: Int): Int = when (this) {
        MILLILITERS -> ml
        FLUID_OUNCES -> (ml / 29.5735).toInt()
    }

    val symbol: String
        get() = when (this) {
            MILLILITERS -> "mL"
            FLUID_OUNCES -> "fl oz"
        }
}
