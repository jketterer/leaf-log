package dev.jketterer.leaflog.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

actual class PreferencesDataStore(private val context: Context) {

    companion object {
        val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        val VOLUME_UNIT_KEY = stringPreferencesKey("volume_unit")
        val TEA_SORT_OPTION_KEY = stringPreferencesKey("tea_sort_option")
        val ANALYTICS_PERIOD_KEY = stringPreferencesKey("analytics_period")
        val DEFAULT_WATER_TYPE_KEY = stringPreferencesKey("default_water_type")
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
                } ?: VolumeUnit.MILLILITERS,
                teaSortOption = prefs[TEA_SORT_OPTION_KEY]?.let {
                    try {
                        TeaSortOption.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        TeaSortOption.NAME_ASC
                    }
                } ?: TeaSortOption.NAME_ASC,
                analyticsPeriod = prefs[ANALYTICS_PERIOD_KEY]?.let {
                    try {
                        AnalyticsPeriod.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        AnalyticsPeriod.THIS_WEEK
                    }
                } ?: AnalyticsPeriod.THIS_WEEK,
                defaultWaterType = prefs[DEFAULT_WATER_TYPE_KEY]?.let {
                    try {
                        WaterType.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        WaterType.FILTERED
                    }
                } ?: WaterType.FILTERED,
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

    actual suspend fun updateTeaSortOption(option: TeaSortOption) {
        context.dataStore.edit { prefs ->
            prefs[TEA_SORT_OPTION_KEY] = option.name
        }
    }

    actual suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod) {
        context.dataStore.edit { prefs ->
            prefs[ANALYTICS_PERIOD_KEY] = period.name
        }
    }

    actual suspend fun updateDefaultWaterType(waterType: WaterType) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_WATER_TYPE_KEY] = waterType.name
        }
    }
}
