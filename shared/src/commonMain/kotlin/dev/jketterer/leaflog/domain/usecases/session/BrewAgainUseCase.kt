package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BrewAgainUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        sourceSession: TeaSession,
    ): Result<TeaSession> {
        if (teaSessionRepository.hasInProgressSession()) {
            return Result.failure(
                IllegalStateException("A session is already in progress. Please complete it before starting a new one.")
            )
        }

        val now = Clock.System.now()

        // Create new session with same parameters
        val newSession = TeaSession(
            id = Uuid.random().toString(),
            teaId = sourceSession.teaId,
            parentSessionId = null,
            steepNumber = 1,
            status = SessionStatus.IN_PROGRESS,
            teaQuantityGrams = sourceSession.teaQuantityGrams,
            vesselId = sourceSession.vesselId,
            waterType = sourceSession.waterType,
            location = sourceSession.location,
            rating = null,
            timestamp = now,
            brewingTime = sourceSession.brewingTime,
            temperatureCelsius = sourceSession.temperatureCelsius,
            waterQuantityMl = sourceSession.waterQuantityMl,
            notes = null,
            photos = emptyList(),
            userId = null,
            syncStatus = SyncStatus.LOCAL_ONLY,
            createdAt = now,
            updatedAt = now,
        )

        return try {
            teaSessionRepository.upsert(newSession)
            Result.success(newSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}