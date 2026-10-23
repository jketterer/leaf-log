package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val volumeUnit: VolumeUnit = VolumeUnit.FLUID_OUNCES,
    val weightUnit: WeightUnit = WeightUnit.GRAMS,
    val teaSortOption: TeaSortOption = TeaSortOption.NAME_ASC,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_WEEK,
    val defaultWaterType: WaterType = WaterType.FILTERED,
    val timerCompletionNotificationsEnabled: Boolean = true,
    val sessionReminderNotificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Null until chosen, when it follows the device's region through [Hemisphere.forRegion]. */
    val hemisphere: Hemisphere? = null,
    /** When an export was last delivered. This device's own fact, so it isn't backed up. */
    val lastBackupAt: Instant? = null,
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
