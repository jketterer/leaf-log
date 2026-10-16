package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.DomainFailure
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
            if (brewingVesselRepository.countActive() <= 1) {
                throw DomainFailure.LastRemaining("active vessel")
            }
            val sessions = teaSessionRepository.getSessionsByVessel(vesselId).size
            if (sessions > 0) throw DomainFailure.InUse(sessions, DomainFailure.Dependents.SESSIONS)
            brewingVesselRepository.delete(vesselId)
            brewingConfigurationRepository.deleteWithVessel(vesselId)
        }
    }
}
