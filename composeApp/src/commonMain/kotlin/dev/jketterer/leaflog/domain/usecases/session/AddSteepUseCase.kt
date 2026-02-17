package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AddSteepUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        parentSession: TeaSession,
        brewingTime: Duration,
        temperatureCelsius: Int,
        waterQuantityMl: Int,
        notes: String? = null,
        photos: List<String> = emptyList()
    ): Result<TeaSession> {
        if (parentSession.parentSessionId != null) {
            return validationFailure("Cannot add steep to a child session")
        }

        if (temperatureCelsius !in 0..100) {
            return validationFailure("Temperature must be between 0°C and 100°C")
        }

        if (waterQuantityMl <= 0) {
            return validationFailure("Water quantity must be greater than 0")
        }

        if (brewingTime <= Duration.ZERO) {
            return validationFailure("Brewing time must be greater than 0")
        }

        // Get current steep number
        val existingSteeps = teaSessionRepository.getChildSteeps(parentSession.id)
        val nextSteepNumber = existingSteeps.maxOfOrNull { it.steepNumber }?.plus(1) ?: 2

        val now = Clock.System.now()
        val newSteep = TeaSession(
            id = Uuid.random().toString(),
            teaId = parentSession.teaId,
            parentSessionId = parentSession.id,
            steepNumber = nextSteepNumber,
            status = SessionStatus.IN_PROGRESS,
            teaQuantityGrams = parentSession.teaQuantityGrams,
            vesselId = parentSession.vesselId,
            waterType = parentSession.waterType,
            location = parentSession.location,
            timestamp = now,
            brewingTime = brewingTime,
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            notes = notes?.takeIf { it.isNotBlank() }?.trim(),
            photos = photos,
            userId = parentSession.userId,
            syncStatus = SyncStatus.LOCAL_ONLY,
            createdAt = now,
            updatedAt = now
        )

        return try {
            teaSessionRepository.upsert(newSteep)
            Result.success(newSteep)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validationFailure(message: String): Result<TeaSession> {
        return Result.failure(IllegalArgumentException(message))
    }
}