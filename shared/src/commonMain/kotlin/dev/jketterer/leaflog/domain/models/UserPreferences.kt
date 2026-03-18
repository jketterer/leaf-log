package dev.jketterer.leaflog.domain.models

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val volumeUnit: VolumeUnit = VolumeUnit.FLUID_OUNCES,
    val teaSortOption: TeaSortOption = TeaSortOption.NAME_ASC,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_WEEK,
    val defaultWaterType: WaterType = WaterType.FILTERED,
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
