package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository

class DeleteBrewingVesselUseCase(
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    suspend operator fun invoke(vesselId: String): Result<Unit> {
        if (vesselId.isBlank()) {
            return Result.failure(IllegalArgumentException("Vessel ID cannot be empty"))
        }

        return try {
            brewingVesselRepository.delete(vesselId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}