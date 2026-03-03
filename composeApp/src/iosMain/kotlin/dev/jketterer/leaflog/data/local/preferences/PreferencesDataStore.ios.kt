package dev.jketterer.leaflog.data.local.preferences

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

actual class PreferencesDataStore {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    companion object {
        private const val TEMPERATURE_UNIT_KEY = "temperature_unit"
        private const val VOLUME_UNIT_KEY = "volume_unit"
        private const val TEA_SORT_OPTION_KEY = "tea_sort_option"
        private const val ANALYTICS_PERIOD_KEY = "analytics_period"
        private const val DEFAULT_WATER_TYPE_KEY = "default_water_type"
    }

    private val _preferencesFlow = MutableStateFlow(loadPreferences())

    actual fun getPreferencesFlow(): Flow<UserPreferences> {
        return _preferencesFlow.asStateFlow()
    }

    actual suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        userDefaults.setObject(unit.name, TEMPERATURE_UNIT_KEY)
        userDefaults.synchronize()
        _preferencesFlow.update { it.copy(temperatureUnit = unit) }
    }

    actual suspend fun updateVolumeUnit(unit: VolumeUnit) {
        userDefaults.setObject(unit.name, VOLUME_UNIT_KEY)
        userDefaults.synchronize()
        _preferencesFlow.update { it.copy(volumeUnit = unit) }
    }

    actual suspend fun updateTeaSortOption(option: TeaSortOption) {
        userDefaults.setObject(option.name, TEA_SORT_OPTION_KEY)
        userDefaults.synchronize()
        _preferencesFlow.update { it.copy(teaSortOption = option) }
    }

    actual suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod) {
        userDefaults.setObject(period.name, ANALYTICS_PERIOD_KEY)
        userDefaults.synchronize()
        _preferencesFlow.update { it.copy(analyticsPeriod = period) }
    }

    actual suspend fun updateDefaultWaterType(waterType: WaterType) {
        userDefaults.setObject(waterType.name, DEFAULT_WATER_TYPE_KEY)
        userDefaults.synchronize()
        _preferencesFlow.update { it.copy(defaultWaterType = waterType) }
    }

    private fun loadPreferences(): UserPreferences {
        val temperatureUnitString = userDefaults.stringForKey(TEMPERATURE_UNIT_KEY)
        val volumeUnitString = userDefaults.stringForKey(VOLUME_UNIT_KEY)
        val teaSortOptionString = userDefaults.stringForKey(TEA_SORT_OPTION_KEY)
        val analyticsPeriodString = userDefaults.stringForKey(ANALYTICS_PERIOD_KEY)
        val defaultWaterTypeString = userDefaults.stringForKey(DEFAULT_WATER_TYPE_KEY)

        return UserPreferences(
            temperatureUnit = temperatureUnitString?.let {
                try {
                    TemperatureUnit.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    TemperatureUnit.CELSIUS
                }
            } ?: TemperatureUnit.CELSIUS,
            volumeUnit = volumeUnitString?.let {
                try {
                    VolumeUnit.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    VolumeUnit.MILLILITERS
                }
            } ?: VolumeUnit.MILLILITERS,
            teaSortOption = teaSortOptionString?.let {
                try {
                    TeaSortOption.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    TeaSortOption.NAME_ASC
                }
            } ?: TeaSortOption.NAME_ASC,
            analyticsPeriod = analyticsPeriodString?.let {
                try {
                    AnalyticsPeriod.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    AnalyticsPeriod.THIS_WEEK
                }
            } ?: AnalyticsPeriod.THIS_WEEK,
            defaultWaterType = defaultWaterTypeString?.let {
                try {
                    WaterType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    WaterType.FILTERED
                }
            } ?: WaterType.FILTERED,
        )
    }
}
