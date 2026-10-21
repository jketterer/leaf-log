package dev.jketterer.leaflog.data.local.preferences

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

expect class PreferencesDataStore {
    fun getPreferencesFlow(): Flow<UserPreferences>
    suspend fun updateTemperatureUnit(unit: TemperatureUnit)
    suspend fun updateVolumeUnit(unit: VolumeUnit)
    suspend fun updateWeightUnit(unit: WeightUnit)
    suspend fun updateTeaSortOption(option: TeaSortOption)
    suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod)
    suspend fun updateDefaultWaterType(waterType: WaterType)
    suspend fun updateTimerCompletionNotificationsEnabled(enabled: Boolean)
    suspend fun updateSessionReminderNotificationsEnabled(enabled: Boolean)
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateLastBackupAt(at: Instant)

    // The Quick Timer, encoded by the repository. Not a preference, so not in [UserPreferences]
    suspend fun getQuickTimer(): String?
    suspend fun updateQuickTimer(encoded: String?)
}
