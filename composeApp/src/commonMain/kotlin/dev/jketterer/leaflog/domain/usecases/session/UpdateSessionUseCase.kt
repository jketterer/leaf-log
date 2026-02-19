package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.time.Duration

class UpdateSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(
        existingSession: TeaSession,
        teaQuantityGrams: Float? = null,
        vesselId: String? = null,
        waterType: WaterType? = null,
        location: String? = null,
        rating: Float? = null,
        brewingTime: Duration? = null,
        temperatureCelsius: Double? = null,
        waterQuantityMl: Double? = null,
        notes: String? = null,
        photos: List<String>? = null,
    ): Result<TeaSession> {
        val newTemp = temperatureCelsius ?: existingSession.temperatureCelsius
        if (newTemp !in 0.0..100.0) {
            return validationFailure("Temperature must be between 0°C and 100°C")
        }

        val newWaterQty = waterQuantityMl ?: existingSession.waterQuantityMl
        if (newWaterQty <= 0) {
            return validationFailure("Water quantity must be greater than 0")
        }

        val newTeaQty = teaQuantityGrams ?: existingSession.teaQuantityGrams
        if (newTeaQty != null && newTeaQty <= 0) {
            return validationFailure("Tea quantity must be greater than 0")
        }

        val newBrewingTime = brewingTime ?: existingSession.brewingTime
        if (newBrewingTime <= Duration.ZERO) {
            return validationFailure("Brewing time must be greater than 0")
        }

        val newVesselId = vesselId ?: existingSession.vesselId
        if (newVesselId.isBlank()) {
            return validationFailure("Brewing vessel must be selected")
        }

        val newRating = rating ?: existingSession.rating
        if (newRating != null && (newRating !in 0f..5f)) {
            return validationFailure("Rating must be between 0 and 5")
        }

        val updatedSession = existingSession.copy(
            teaQuantityGrams = newTeaQty,
            vesselId = newVesselId,
            waterType = waterType ?: existingSession.waterType,
            location = location ?: existingSession.location,
            rating = newRating,
            brewingTime = newBrewingTime,
            temperatureCelsius = newTemp,
            waterQuantityMl = newWaterQty,
            notes = notes ?: existingSession.notes,
            photos = photos ?: existingSession.photos,
            updatedAt = Clock.System.now()
        )

        return try {
            teaSessionRepository.upsert(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validationFailure(message: String): Result<TeaSession> {
        return Result.failure(IllegalArgumentException(message))
    }
}
