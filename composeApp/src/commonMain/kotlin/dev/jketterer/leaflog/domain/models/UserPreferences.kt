package dev.jketterer.leaflog.domain.models

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITERS,
)
