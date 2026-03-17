package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import kotlin.time.Clock

/**
 * Use case to update an existing brewing vessel.
 */
class UpdateBrewingVesselUseCase(
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    suspend operator fun invoke(
        existingVessel: BrewingVessel,
        name: String? = null,
        iconName: String? = null,
        imagePath: String? = null,
        clearImage: Boolean = false,
        capacityMl: Int? = null,
    ): Result<BrewingVessel> {
        val newName = name ?: existingVessel.name

        if (newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Vessel name cannot be empty"))
        }

        // Use explicit null check to distinguish between "not provided" and "set to null"
        val newCapacity = when {
            capacityMl != null && capacityMl == -1 -> null // -1 means "clear capacity"
            capacityMl != null && capacityMl <= 0 -> return Result.failure(
                IllegalArgumentException("Capacity must be greater than 0")
            )
            capacityMl != null -> capacityMl
            else -> existingVessel.capacityMl
        }

        val newImagePath = when {
            clearImage -> null
            imagePath != null -> imagePath
            else -> existingVessel.imagePath
        }

        val updatedVessel = existingVessel.copy(
            name = newName.trim(),
            iconName = iconName ?: existingVessel.iconName,
            imagePath = newImagePath,
            capacityMl = newCapacity,
            updatedAt = Clock.System.now(),
        )

        return try {
            brewingVesselRepository.upsert(updatedVessel)
            Result.success(updatedVessel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}