package dev.jketterer.leaflog.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * One per process, on [scope]: the app's, so a test that ends its process releases the file and
 * the next one reads it fresh, as a real restart would.
 */
actual class PreferencesDataStore(context: Context, scope: CoroutineScope) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile("user_preferences") },
    )

    companion object {
        val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        val VOLUME_UNIT_KEY = stringPreferencesKey("volume_unit")
        val WEIGHT_UNIT_KEY = stringPreferencesKey("weight_unit")
        val TEA_SORT_OPTION_KEY = stringPreferencesKey("tea_sort_option")
        val ANALYTICS_PERIOD_KEY = stringPreferencesKey("analytics_period")
        val DEFAULT_WATER_TYPE_KEY = stringPreferencesKey("default_water_type")
        val TIMER_COMPLETION_NOTIFICATIONS_KEY =
            booleanPreferencesKey("timer_completion_notifications_enabled")
        val SESSION_REMINDER_NOTIFICATIONS_KEY =
            booleanPreferencesKey("session_reminder_notifications_enabled")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val HEMISPHERE_KEY = stringPreferencesKey("hemisphere")
        val LAST_BACKUP_AT_KEY = longPreferencesKey("last_backup_at")
        val QUICK_TIMER_KEY = stringPreferencesKey("quick_timer")
    }

    actual fun getPreferencesFlow(): Flow<UserPreferences> {
        return dataStore.data.map { prefs ->
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
                weightUnit = prefs[WEIGHT_UNIT_KEY]?.let {
                    try {
                        WeightUnit.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        WeightUnit.GRAMS
                    }
                } ?: WeightUnit.GRAMS,
                teaSortOption = prefs[TEA_SORT_OPTION_KEY]?.let {
                    try {
                        TeaSortOption.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        TeaSortOption.NAME_ASC
                    }
                } ?: TeaSortOption.NAME_ASC,
                analyticsPeriod = AnalyticsPeriod.named(prefs[ANALYTICS_PERIOD_KEY]),
                defaultWaterType = prefs[DEFAULT_WATER_TYPE_KEY]?.let {
                    try {
                        WaterType.valueOf(it)
                    } catch (_: IllegalArgumentException) {
                        WaterType.FILTERED
                    }
                } ?: WaterType.FILTERED,
                timerCompletionNotificationsEnabled =
                    prefs[TIMER_COMPLETION_NOTIFICATIONS_KEY] ?: true,
                sessionReminderNotificationsEnabled =
                    prefs[SESSION_REMINDER_NOTIFICATIONS_KEY] ?: true,
                themeMode = prefs[THEME_MODE_KEY]
                    ?.let { name -> ThemeMode.entries.find { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                hemisphere = prefs[HEMISPHERE_KEY]
                    ?.let { name -> Hemisphere.entries.find { it.name == name } },
                lastBackupAt = prefs[LAST_BACKUP_AT_KEY]?.let(Instant::fromEpochMilliseconds),
            )
        }
    }

    actual suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { prefs ->
            prefs[TEMPERATURE_UNIT_KEY] = unit.name
        }
    }

    actual suspend fun updateVolumeUnit(unit: VolumeUnit) {
        dataStore.edit { prefs ->
            prefs[VOLUME_UNIT_KEY] = unit.name
        }
    }

    actual suspend fun updateWeightUnit(unit: WeightUnit) {
        dataStore.edit { prefs ->
            prefs[WEIGHT_UNIT_KEY] = unit.name
        }
    }

    actual suspend fun updateTeaSortOption(option: TeaSortOption) {
        dataStore.edit { prefs ->
            prefs[TEA_SORT_OPTION_KEY] = option.name
        }
    }

    actual suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod) {
        dataStore.edit { prefs ->
            prefs[ANALYTICS_PERIOD_KEY] = period.name
        }
    }

    actual suspend fun updateDefaultWaterType(waterType: WaterType) {
        dataStore.edit { prefs ->
            prefs[DEFAULT_WATER_TYPE_KEY] = waterType.name
        }
    }

    actual suspend fun updateTimerCompletionNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TIMER_COMPLETION_NOTIFICATIONS_KEY] = enabled
        }
    }

    actual suspend fun updateSessionReminderNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SESSION_REMINDER_NOTIFICATIONS_KEY] = enabled
        }
    }

    actual suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    actual suspend fun updateHemisphere(hemisphere: Hemisphere?) {
        dataStore.edit { prefs ->
            if (hemisphere == null) {
                prefs.remove(HEMISPHERE_KEY)
            } else {
                prefs[HEMISPHERE_KEY] = hemisphere.name
            }
        }
    }

    actual suspend fun updateLastBackupAt(at: Instant) {
        dataStore.edit { prefs ->
            prefs[LAST_BACKUP_AT_KEY] = at.toEpochMilliseconds()
        }
    }

    actual suspend fun getQuickTimer(): String? = dataStore.data.first()[QUICK_TIMER_KEY]

    actual suspend fun updateQuickTimer(encoded: String?) {
        dataStore.edit { prefs ->
            if (encoded == null) prefs.remove(QUICK_TIMER_KEY) else prefs[QUICK_TIMER_KEY] = encoded
        }
    }
}
