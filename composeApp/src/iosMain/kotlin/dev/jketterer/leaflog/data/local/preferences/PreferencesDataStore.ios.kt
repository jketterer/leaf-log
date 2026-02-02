package dev.jketterer.leaflog.data.local.preferences

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
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

    private fun loadPreferences(): UserPreferences {
        val temperatureUnitString = userDefaults.stringForKey(TEMPERATURE_UNIT_KEY)
        val volumeUnitString = userDefaults.stringForKey(VOLUME_UNIT_KEY)

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
            } ?: VolumeUnit.MILLILITERS
        )
    }
}
