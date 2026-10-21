package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.TimedSteep
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class PreferencesRepositoryImpl(
    private val preferencesDataStore: PreferencesDataStore,
) : PreferencesRepository {

    override fun getPreferencesFlow(): Flow<UserPreferences> {
        return preferencesDataStore.getPreferencesFlow()
    }

    override suspend fun getPreferences(): UserPreferences {
        return preferencesDataStore.getPreferencesFlow().first()
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

    override suspend fun updateTeaSortOption(option: TeaSortOption): Result<Unit> {
        return try {
            preferencesDataStore.updateTeaSortOption(option)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod): Result<Unit> {
        return try {
            preferencesDataStore.updateAnalyticsPeriod(period)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDefaultWaterType(waterType: WaterType): Result<Unit> {
        return try {
            preferencesDataStore.updateDefaultWaterType(waterType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWeightUnit(unit: WeightUnit): Result<Unit> {
        return try {
            preferencesDataStore.updateWeightUnit(unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTimerCompletionNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            preferencesDataStore.updateTimerCompletionNotificationsEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSessionReminderNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            preferencesDataStore.updateSessionReminderNotificationsEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode): Result<Unit> {
        return try {
            preferencesDataStore.updateThemeMode(mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastBackupAt(at: Instant): Result<Unit> {
        return try {
            preferencesDataStore.updateLastBackupAt(at)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuickTimer(): TimerState? {
        val encoded = preferencesDataStore.getQuickTimer() ?: return null
        // Unreadable means stored by a build that wrote it differently: there's no timer to restore
        return runCatching { Json.decodeFromString<StoredQuickTimer>(encoded).toTimerState() }.getOrNull()
    }

    override suspend fun updateQuickTimer(timer: TimerState?): Result<Unit> = runCatching {
        preferencesDataStore.updateQuickTimer(timer?.let { Json.encodeToString(StoredQuickTimer.of(it)) })
    }
}

@Serializable
private data class StoredQuickTimer(
    val steepNumber: Int,
    val totalMs: Long,
    val remainingMs: Long,
    val status: TimerStatus,
    val startedAtMs: Long?,
    val pausedAtMs: Long?,
    val finished: List<StoredSteep> = emptyList(),
) {
    fun toTimerState() = TimerState(
        steepNumber = steepNumber,
        totalDuration = totalMs.milliseconds,
        remainingDuration = remainingMs.milliseconds,
        status = status,
        startedAt = startedAtMs?.let(Instant::fromEpochMilliseconds),
        pausedAt = pausedAtMs?.let(Instant::fromEpochMilliseconds),
        finishedSteeps = finished.map {
            TimedSteep(Instant.fromEpochMilliseconds(it.startedAtMs), it.lengthMs.milliseconds)
        },
    )

    companion object {
        fun of(timer: TimerState) = StoredQuickTimer(
            steepNumber = timer.steepNumber,
            totalMs = timer.totalDuration.inWholeMilliseconds,
            remainingMs = timer.remainingDuration.inWholeMilliseconds,
            status = timer.status,
            startedAtMs = timer.startedAt?.toEpochMilliseconds(),
            pausedAtMs = timer.pausedAt?.toEpochMilliseconds(),
            finished = timer.finishedSteeps.map {
                StoredSteep(it.startedAt.toEpochMilliseconds(), it.length.inWholeMilliseconds)
            },
        )
    }
}

@Serializable
private data class StoredSteep(val startedAtMs: Long, val lengthMs: Long)
