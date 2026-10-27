package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import kotlin.time.Clock

/**
 * Use case to update an existing brewing vessel.
 */
class UpdateBrewingVesselUseCase(
    private val brewingVesselRepository: BrewingVesselRepository,
    private val clock: Clock,
) {
    /**
     * Replaces the editable fields of [existingVessel] with the values given.
     *
     * Every field is required, and null means the field is empty, not "leave it alone". Callers
     * pass the vessel's full intended state, so treating null as "unchanged" would make clearing
     * an optional field impossible: the old value would silently come back on save.
     */
    suspend operator fun invoke(
        existingVessel: BrewingVessel,
        name: String,
        iconName: String,
        imagePath: String?,
        capacityMl: Int?,
    ): Result<BrewingVessel> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Vessel name cannot be empty"))
        }

        if (capacityMl != null && capacityMl <= 0) {
            return Result.failure(
                IllegalArgumentException("Capacity must be greater than 0"),
            )
        }

        val updatedVessel = existingVessel.copy(
            name = name.trim(),
            iconName = iconName,
            imagePath = imagePath,
            capacityMl = capacityMl,
            updatedAt = clock.now(),
        )

        return try {
            brewingVesselRepository.upsert(updatedVessel)
            Result.success(updatedVessel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
