package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class PreferencesRepositoryImpl(
    private val preferencesDataStore: PreferencesDataStore
) : PreferencesRepository {

    override fun getPreferencesFlow(): Flow<UserPreferences> {
        return preferencesDataStore.getPreferencesFlow()
    }

    override suspend fun updateTemperatureUnit(unit: TemperatureUnit): Result<Unit> {
        return try {
            preferencesDataStore.updateTemperatureUnit(unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVolumeUnit(unit: VolumeUnit): Result<Unit> {
        return try {
            preferencesDataStore.updateVolumeUnit(unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
