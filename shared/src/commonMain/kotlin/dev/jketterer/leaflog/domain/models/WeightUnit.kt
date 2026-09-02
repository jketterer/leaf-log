package dev.jketterer.leaflog.domain.models

enum class WeightUnit {
    GRAMS,
    OUNCES;

    fun toggle(): WeightUnit = when (this) {
        GRAMS -> OUNCES
        OUNCES -> GRAMS
    }

    /** Converts a gram value to the display unit, unrounded so callers control precision. */
    fun fromGrams(grams: Double): Double = when (this) {
        GRAMS -> grams
        OUNCES -> grams / GRAMS_PER_OUNCE
    }

    val symbol: String
        get() = when (this) {
            GRAMS -> "g"
            OUNCES -> "oz"
        }

    val largeSymbol: String
        get() = when (this) {
            GRAMS -> "kg"
            OUNCES -> "lb"
        }

    companion object {
        const val GRAMS_PER_OUNCE = 28.3495
        const val GRAMS_PER_POUND = 453.592
    }
}
