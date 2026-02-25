package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getPreferencesFlow(): Flow<UserPreferences>
    suspend fun getPreferences(): UserPreferences
    suspend fun updateTemperatureUnit(unit: TemperatureUnit): Result<Unit>
    suspend fun updateVolumeUnit(unit: VolumeUnit): Result<Unit>
    suspend fun updateTeaSortOption(option: TeaSortOption): Result<Unit>
    suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod): Result<Unit>
}
