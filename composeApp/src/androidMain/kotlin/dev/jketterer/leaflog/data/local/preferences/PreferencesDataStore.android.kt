package dev.jketterer.leaflog.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

actual class PreferencesDataStore(private val context: Context) {

    companion object {
        val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        val VOLUME_UNIT_KEY = stringPreferencesKey("volume_unit")
    }

    actual fun getPreferencesFlow(): Flow<UserPreferences> {
        return context.dataStore.data.map { prefs ->
            UserPreferences(
                temperatureUnit = prefs[TEMPERATURE_UNIT_KEY]?.let {
                    try {
                        TemperatureUnit.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        TemperatureUnit.CELSIUS
                    }
                } ?: TemperatureUnit.CELSIUS,
                volumeUnit = prefs[VOLUME_UNIT_KEY]?.let {
                    try {
                        VolumeUnit.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        VolumeUnit.MILLILITERS
                    }
                } ?: VolumeUnit.MILLILITERS
            )
        }
    }

    actual suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { prefs ->
            prefs[TEMPERATURE_UNIT_KEY] = unit.name
        }
    }

    actual suspend fun updateVolumeUnit(unit: VolumeUnit) {
        context.dataStore.edit { prefs ->
            prefs[VOLUME_UNIT_KEY] = unit.name
        }
    }
}
