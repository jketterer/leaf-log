package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/** Deletes a vessel no session used, with its methods. The last active vessel is kept. */
class DeleteBrewingVesselUseCase(
    private val transactor: Transactor,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(vesselId: String): Result<Unit> = runCatching {
        require(vesselId.isNotBlank()) { "Vessel ID cannot be empty" }
        transactor {
            check(brewingVesselRepository.countActive() > 1) {
                "Cannot delete the last vessel. At least one vessel must remain."
            }
            val sessionsUsingVessel = teaSessionRepository.getSessionsByVessel(vesselId)
            check(sessionsUsingVessel.isEmpty()) {
                "Cannot delete vessel. ${sessionsUsingVessel.size} session(s) are using this vessel."
            }
            brewingVesselRepository.delete(vesselId)
            brewingConfigurationRepository.deleteWithVessel(vesselId)
        }
    }
}
