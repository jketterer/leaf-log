package dev.jketterer.leaflog.domain.models

/**
 * Formats temperature values according to user preferences.
 */
object TemperatureFormatter {
    /**
     * Formats a temperature value in Celsius to the user's preferred unit.
     *
     * @param celsius Temperature in Celsius (storage format)
     * @param unit User's preferred temperature unit
     * @return Formatted string with unit symbol (e.g., "80°C" or "176°F")
     */
    fun format(celsius: Int, unit: TemperatureUnit): String {
        val value = unit.fromCelsius(celsius)
        return "$value${unit.symbol}"
    }

    /**
     * Returns just the unit symbol for the user's preference.
     */
    fun getUnitSymbol(unit: TemperatureUnit): String = unit.symbol

    /**
     * Returns the label for input fields.
     */
    fun getInputLabel(unit: TemperatureUnit): String = "Temperature (${unit.symbol})"
}
