package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateTeaUseCase(
    private val teaRepository: TeaRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        name: String,
        teaTypeId: String,
        origin: String? = null,
        producer: String? = null,
        purchaseDate: LocalDate? = null,
        defaultBrewingTime: Duration? = null,
        defaultTemperatureCelsius: Int? = null,
        defaultQuantity: Int? = null,
        description: String? = null,
        photos: List<String> = emptyList()
    ): Result<Tea> {
        // Validation
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea name cannot be empty"))
        }

        if (teaTypeId.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea type must be selected"))
        }

        if (defaultTemperatureCelsius != null && (defaultTemperatureCelsius !in 0..100)) {
            return Result.failure(IllegalArgumentException("Temperature must be between 0°C and 100°C"))
        }

        val now = Clock.System.now()
        val tea = Tea(
            id = Uuid.random().toString(),
            name = name.trim(),
            teaTypeId = teaTypeId,
            origin = origin?.takeIf { it.isNotBlank() }?.trim(),
            producer = producer?.takeIf { it.isNotBlank() }?.trim(),
            purchaseDate = purchaseDate,
            purchasePrice = null,
            stockAmount = null,
            defaultBrewingTime = defaultBrewingTime,
            defaultTemperatureCelsius = defaultTemperatureCelsius,
            defaultQuantity = defaultQuantity,
            description = description?.takeIf { it.isNotBlank() }?.trim(),
            photos = photos,
            isFavorite = false,
            totalSessions = 0,
            averageRating = null,
            lastBrewedAt = null,
            userId = null,
            syncStatus = SyncStatus.LOCAL_ONLY,
            createdAt = now,
            updatedAt = now
        )

        return try {
            teaRepository.upsert(tea)
            Result.success(tea)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}