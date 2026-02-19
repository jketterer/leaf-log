package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class CreateSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        teaId: String,
        teaQuantityGrams: Float? = null,
        vesselId: String,
        waterType: WaterType,
        location: String? = null,
        brewingTime: Duration,
        temperatureCelsius: Double,
        waterQuantityMl: Double,
        notes: String? = null,
        rating: Float? = null,
        photos: List<String> = emptyList(),
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        usedConfigurationId: String? = null,
    ): Result<TeaSession> {
        if (teaId.isBlank()) {
            return validationFailure("Tea must be selected")
        }

        if (vesselId.isBlank()) {
            return validationFailure("Brewing vessel must be selected")
        }

        if (temperatureCelsius !in 0.0..100.0) {
            return validationFailure("Temperature must be between 0°C and 100°C")
        }

        if (waterQuantityMl <= 0) {
            return validationFailure("Water quantity must be greater than 0")
        }

        if (teaQuantityGrams != null && teaQuantityGrams <= 0) {
            return validationFailure("Tea quantity must be greater than 0")
        }

        if (brewingTime <= Duration.ZERO) {
            return validationFailure("Brewing time must be greater than 0")
        }

        val now = Clock.System.now()
        val session = TeaSession(
            id = Uuid.random().toString(),
            teaId = teaId,
            steepNumber = 1,
            status = status,
            teaQuantityGrams = teaQuantityGrams,
            vesselId = vesselId,
            waterType = waterType,
            location = location?.takeIf { it.isNotBlank() }?.trim(),
            usedConfigurationId = usedConfigurationId,
            timestamp = now,
            brewingTime = brewingTime,
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            notes = notes?.takeIf { it.isNotBlank() }?.trim(),
            rating = rating,
            photos = photos,
            syncStatus = SyncStatus.LOCAL_ONLY,
            createdAt = now,
            updatedAt = now,
        )

        return try {
            teaSessionRepository.upsert(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validationFailure(message: String): Result<TeaSession> {
        return Result.failure(IllegalArgumentException(message))
    }
}