package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case to create a new brewing vessel.
 * Contains validation and ID generation logic.
 */
class CreateBrewingVesselUseCase(
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        name: String,
        iconName: String? = null,
    ): Result<BrewingVessel> {
        // Validation
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Vessel name cannot be empty"))
        }

        val now = Clock.System.now()

        // Get max display order
        val existingVessels = brewingVesselRepository.getAll()
        val maxOrder = existingVessels.maxOfOrNull { it.displayOrder } ?: -1

        val vessel = BrewingVessel(
            id = Uuid.random().toString(),
            name = name.trim(),
            iconName = iconName,
            isSystemDefault = false,
            displayOrder = maxOrder + 1,
            userId = null,
            createdAt = now,
            updatedAt = now,
        )

        return try {
            brewingVesselRepository.upsert(vessel)
            Result.success(vessel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}