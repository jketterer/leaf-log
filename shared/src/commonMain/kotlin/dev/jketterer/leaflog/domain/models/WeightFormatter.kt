package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

/**
 * Formats dry leaf weights according to user preferences.
 */
object WeightFormatter {
    private const val KILOGRAMS_THRESHOLD_G = 1000.0
    private const val POUNDS_THRESHOLD_G = WeightUnit.GRAMS_PER_POUND

    /**
     * Formats a weight in grams to the user's preferred unit, upgrading to kg or lb once the
     * total gets large enough that the small unit stops reading cleanly.
     *
     * Single brews are a few grams, so the small unit keeps one decimal place; totals that reach
     * the large unit are rounded the same way.
     */
    fun format(grams: Double, unit: WeightUnit): String {
        return when (unit) {
            WeightUnit.GRAMS ->
                if (grams >= KILOGRAMS_THRESHOLD_G) {
                    "${oneDecimal(grams / KILOGRAMS_THRESHOLD_G)} ${unit.largeSymbol}"
                } else {
                    "${oneDecimal(grams)} ${unit.symbol}"
                }

            WeightUnit.OUNCES ->
                if (grams >= POUNDS_THRESHOLD_G) {
                    "${oneDecimal(grams / WeightUnit.GRAMS_PER_POUND)} ${unit.largeSymbol}"
                } else {
                    "${oneDecimal(unit.fromGrams(grams))} ${unit.symbol}"
                }
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}
