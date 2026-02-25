package dev.jketterer.leaflog.data.local.preferences

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import kotlinx.coroutines.flow.Flow

expect class PreferencesDataStore {
    fun getPreferencesFlow(): Flow<UserPreferences>
    suspend fun updateTemperatureUnit(unit: TemperatureUnit)
    suspend fun updateVolumeUnit(unit: VolumeUnit)
    suspend fun updateTeaSortOption(option: TeaSortOption)
    suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod)
}
