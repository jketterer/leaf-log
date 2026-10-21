package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface PreferencesRepository {
    fun getPreferencesFlow(): Flow<UserPreferences>
    suspend fun getPreferences(): UserPreferences
    suspend fun updateTemperatureUnit(unit: TemperatureUnit): Result<Unit>
    suspend fun updateVolumeUnit(unit: VolumeUnit): Result<Unit>
    suspend fun updateWeightUnit(unit: WeightUnit): Result<Unit>
    suspend fun updateTeaSortOption(option: TeaSortOption): Result<Unit>
    suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod): Result<Unit>
    suspend fun updateDefaultWaterType(waterType: WaterType): Result<Unit>
    suspend fun updateTimerCompletionNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun updateSessionReminderNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun updateThemeMode(mode: ThemeMode): Result<Unit>
    suspend fun updateLastBackupAt(at: Instant): Result<Unit>

    /** The timer with no session, kept so it survives process death. Null when there is none. */
    suspend fun getQuickTimer(): TimerState?
    suspend fun updateQuickTimer(timer: TimerState?): Result<Unit>
}
