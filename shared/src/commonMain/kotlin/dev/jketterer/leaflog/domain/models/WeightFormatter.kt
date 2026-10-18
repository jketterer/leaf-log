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
     * The small unit shows as many decimals as it's typed in ("5.5 g", "0.18 oz"); totals that
     * reach the large unit keep one.
     */
    fun format(grams: Double, unit: WeightUnit): String {
        return when (unit) {
            WeightUnit.GRAMS ->
                if (grams >= KILOGRAMS_THRESHOLD_G) {
                    "${oneDecimal(grams / KILOGRAMS_THRESHOLD_G)} ${unit.largeSymbol}"
                } else {
                    "${formatDecimals(grams, unit.decimals)} ${unit.symbol}"
                }

            WeightUnit.OUNCES ->
                if (grams >= POUNDS_THRESHOLD_G) {
                    "${oneDecimal(grams / WeightUnit.GRAMS_PER_POUND)} ${unit.largeSymbol}"
                } else {
                    "${formatDecimals(unit.fromGrams(grams), unit.decimals)} ${unit.symbol}"
                }
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}
