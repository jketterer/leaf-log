package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.VesselDistribution
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Instant

class GetVesselDistributionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): List<VesselDistribution> {
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
        val completed = sessions.filter {
            it.session.status == SessionStatus.COMPLETED && it.session.vesselId.isNotBlank()
        }

        if (completed.isEmpty()) return emptyList()

        val vessels = brewingVesselRepository.getAll().associateBy { it.id }
        val countByVesselId = completed.groupingBy { it.session.vesselId }.eachCount()
        val total = completed.size.toFloat()

        return countByVesselId
            .mapNotNull { (vesselId, count) ->
                val vessel = vessels[vesselId] ?: return@mapNotNull null
                VesselDistribution(
                    vessel = vessel,
                    sessionCount = count,
                    percentage = count / total * 100f,
                )
            }
            .sortedByDescending { it.sessionCount }
    }
}
