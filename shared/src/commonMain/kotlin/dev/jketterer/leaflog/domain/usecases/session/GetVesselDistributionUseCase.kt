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
        val sessions = teaSessionRepository.getByDateRange(start, end)
        val completedParents = sessions.filter { session ->
            session.status == SessionStatus.COMPLETED &&
                session.parentSessionId == null &&
                session.vesselId.isNotBlank()
        }

        if (completedParents.isEmpty()) return emptyList()

        val vessels = brewingVesselRepository.getAll().associateBy { it.id }
        val countByVesselId = completedParents.groupingBy { it.vesselId }.eachCount()
        val total = completedParents.size.toFloat()

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
