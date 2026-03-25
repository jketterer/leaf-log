package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.first

/**
 * Fetches the most recent in-progress parent session along with tea and vessel names.
 * Returns null if no session is in progress.
 */
class GetInProgressSessionInfoUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    suspend operator fun invoke(): InProgressSessionDetails? {
        val session = teaSessionRepository.getInProgressFlow().first().firstOrNull()
            ?: return null

        val tea = teaRepository.getById(session.teaId)
        val vessel = brewingVesselRepository.getById(session.vesselId)

        return InProgressSessionDetails(
            session = session,
            teaName = tea?.name ?: "Unknown Tea",
            vesselName = vessel?.name ?: "Unknown Vessel",
        )
    }
}
