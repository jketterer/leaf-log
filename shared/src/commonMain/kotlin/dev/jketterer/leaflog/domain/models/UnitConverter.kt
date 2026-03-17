package dev.jketterer.leaflog.domain.models

/**
 * Helper object for converting between user input units and storage units.
 * Storage is always in Celsius and Milliliters.
 */
object UnitConverter {
    /**
     * Converts user input temperature to Celsius for storage.
     *
     * @param inputValue Temperature value as entered by user
     * @param inputUnit Unit the user is using
     * @return Temperature in Celsius for storage
     */
    fun inputTemperatureToCelsius(inputValue: Int, inputUnit: TemperatureUnit): Double {
        return inputUnit.toCelsius(inputValue)
    }

    /**
     * Converts stored Celsius temperature to user's preferred unit for display.
     *
     * @param celsius Temperature in Celsius (from storage)
     * @param displayUnit Unit to display to user
     * @return Temperature in user's preferred unit
     */
    fun celsiusToDisplayTemperature(celsius: Double, displayUnit: TemperatureUnit): Int {
        return displayUnit.fromCelsius(celsius)
    }

    /**
     * Converts user input volume to milliliters for storage.
     *
     * @param inputValue Volume value as entered by user
     * @param inputUnit Unit the user is using
     * @return Volume in milliliters for storage
     */
    fun inputVolumeToMilliliters(inputValue: Int, inputUnit: VolumeUnit): Double {
        return inputUnit.toMilliliters(inputValue)
    }

    /**
     * Converts stored milliliters to user's preferred unit for display.
     *
     * @param milliliters Volume in milliliters (from storage)
     * @param displayUnit Unit to display to user
     * @return Volume in user's preferred unit
     */
    fun millilitersToDisplayVolume(milliliters: Double, displayUnit: VolumeUnit): Int {
        return displayUnit.fromMilliliters(milliliters)
    }
}
