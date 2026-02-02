package dev.jketterer.leaflog.domain.models

/**
 * Formats volume values according to user preferences.
 */
object VolumeFormatter {
    /**
     * Formats a volume value in milliliters to the user's preferred unit.
     *
     * @param milliliters Volume in milliliters (storage format)
     * @param unit User's preferred volume unit
     * @return Formatted string with unit symbol (e.g., "200mL" or "6fl oz")
     */
    fun format(milliliters: Int, unit: VolumeUnit): String {
        val value = unit.fromMilliliters(milliliters)
        return "$value ${unit.symbol}"
    }

    /**
     * Returns the label for input fields.
     */
    fun getInputLabel(unit: VolumeUnit): String = "Water Quantity (${unit.symbol})"
}
