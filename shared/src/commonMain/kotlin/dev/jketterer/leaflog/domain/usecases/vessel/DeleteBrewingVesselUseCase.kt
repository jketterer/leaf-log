package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

class DeleteBrewingVesselUseCase(
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(vesselId: String): Result<Unit> {
        if (vesselId.isBlank()) {
            return Result.failure(IllegalArgumentException("Vessel ID cannot be empty"))
        }

        // Check: at least one active (non-archived) vessel must remain
        val vesselCount = brewingVesselRepository.countActive()
        if (vesselCount <= 1) {
            return Result.failure(
                IllegalStateException("Cannot delete the last vessel. At least one vessel must remain.")
            )
        }

        // Check: no sessions reference this vessel
        val sessionsUsingVessel = teaSessionRepository.getByVesselId(vesselId)
        if (sessionsUsingVessel.isNotEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "Cannot delete vessel. ${sessionsUsingVessel.size} session(s) are using this vessel."
                )
            )
        }

        return try {
            brewingVesselRepository.delete(vesselId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}