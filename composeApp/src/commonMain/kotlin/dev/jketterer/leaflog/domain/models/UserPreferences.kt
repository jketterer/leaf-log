package dev.jketterer.leaflog.domain.models

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITERS,
    val teaSortOption: TeaSortOption = TeaSortOption.NAME_ASC,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_WEEK,
) {
    companion object {
        val IMPERIAL = UserPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            volumeUnit = VolumeUnit.FLUID_OUNCES,
        )

        val METRIC = UserPreferences(
            temperatureUnit = TemperatureUnit.CELSIUS,
            volumeUnit = VolumeUnit.MILLILITERS,
        )
    }
}
