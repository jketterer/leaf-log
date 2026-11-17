package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/** Archives a vessel or brings it back. The last active vessel is kept. */
class SetVesselArchivedUseCase(
    private val transactor: Transactor,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(vesselId: String, archived: Boolean): Result<Unit> = runCatching {
        transactor {
            val vessel = brewingVesselRepository.getById(vesselId)
                ?: throw DomainFailure.NotFound("vessel $vesselId")
            if (vessel.isArchived != archived) {
                if (archived && brewingVesselRepository.countActive() <= 1) {
                    throw DomainFailure.LastRemaining("active vessel")
                }
                brewingVesselRepository.upsert(vessel.copy(isArchived = archived, updatedAt = clock.now()))
            }
        }
    }
}
