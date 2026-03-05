package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

/**
 * Formats volume values according to user preferences.
 */
object VolumeFormatter {
    private const val LITERS_THRESHOLD_ML = 1000.0
    private const val GALLONS_THRESHOLD_ML = 128 * 29.5735  // ~3785.408 mL

    /**
     * Formats a volume value in milliliters to the user's preferred unit.
     * Auto-upgrades to larger units for large volumes (≥1000 mL → L, ≥128 fl oz → gal).
     *
     * @param milliliters Volume in milliliters (storage format)
     * @param unit User's preferred volume unit
     * @return Formatted string with unit symbol (e.g., "200 mL" or "1.5 L")
     */
    fun format(milliliters: Double, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.MILLILITERS -> {
                if (milliliters >= LITERS_THRESHOLD_ML) {
                    "${oneDecimal(milliliters / 1000.0)} ${unit.largeSymbol}"
                } else {
                    "${unit.fromMilliliters(milliliters)} ${unit.symbol}"
                }
            }
            VolumeUnit.FLUID_OUNCES -> {
                if (milliliters >= GALLONS_THRESHOLD_ML) {
                    "${oneDecimal(milliliters / 3785.41)} ${unit.largeSymbol}"
                } else {
                    "${unit.fromMilliliters(milliliters)} ${unit.symbol}"
                }
            }
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }

    /**
     * Returns the label for input fields.
     */
    fun getInputLabel(unit: VolumeUnit): String = "Water Quantity (${unit.symbol})"
}
