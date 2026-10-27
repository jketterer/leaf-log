package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class EditTeaUseCase(
    private val teaRepository: TeaRepository,
    private val clock: Clock,
) {
    /**
     * Replaces the editable fields of [existingTea] with the values given.
     */
    suspend operator fun invoke(
        existingTea: Tea,
        name: String,
        teaTypeId: String,
        origin: String?,
        producer: String?,
        purchaseDate: LocalDate?,
        defaultTemperatureCelsius: Double?,
        description: String?,
        photos: List<String>,
    ): Result<Tea> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea name cannot be empty"))
        }

        if (teaTypeId.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea type must be selected"))
        }

        if (defaultTemperatureCelsius != null && defaultTemperatureCelsius !in 0.0..100.0) {
            return Result.failure(
                IllegalArgumentException("Temperature must be between 0°C and 100°C"),
            )
        }

        val updatedTea = existingTea.copy(
            name = name.trim(),
            teaTypeId = teaTypeId,
            origin = origin,
            producer = producer,
            purchaseDate = purchaseDate,
            defaultTemperatureCelsius = defaultTemperatureCelsius,
            description = description,
            photos = photos,
            updatedAt = clock.now(),
        )

        return try {
            teaRepository.upsert(updatedTea)
            Result.success(updatedTea)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
