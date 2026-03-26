package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlin.time.Clock

class EditTeaUseCase(
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(
        existingTea: Tea,
        name: String? = null,
        teaTypeId: String? = null,
        origin: String? = null,
        producer: String? = null,
        purchaseDate: kotlinx.datetime.LocalDate? = null,
        defaultTemperatureCelsius: Double? = null,
        description: String? = null,
        photos: List<String>? = null,
        isFavorite: Boolean? = null
    ): Result<Tea> {
        // Validation
        val newName = name ?: existingTea.name
        if (newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea name cannot be empty"))
        }

        val newTeaTypeId = teaTypeId ?: existingTea.teaTypeId
        if (newTeaTypeId.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea type must be selected"))
        }

        val newTemp = defaultTemperatureCelsius ?: existingTea.defaultTemperatureCelsius
        if (newTemp != null && (newTemp !in 0.0..100.0)) {
            return Result.failure(IllegalArgumentException("Temperature must be between 0°C and 100°C"))
        }

        val updatedTea = existingTea.copy(
            name = newName.trim(),
            teaTypeId = newTeaTypeId,
            origin = origin ?: existingTea.origin,
            producer = producer ?: existingTea.producer,
            purchaseDate = purchaseDate ?: existingTea.purchaseDate,
            defaultTemperatureCelsius = newTemp,
            description = description ?: existingTea.description,
            photos = photos ?: existingTea.photos,
            isFavorite = isFavorite ?: existingTea.isFavorite,
            updatedAt = Clock.System.now()
        )

        return try {
            teaRepository.upsert(updatedTea)
            Result.success(updatedTea)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}