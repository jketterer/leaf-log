package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SteepInsights
import dev.jketterer.leaflog.domain.models.TopReSteepedTea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Instant

class GetSteepInsightsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): SteepInsights {
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
            .filter { it.session.status == SessionStatus.COMPLETED }

        val averageSteepsPerSession = if (sessions.isEmpty()) {
            1.0f
        } else {
            sessions.map { it.steeps.size }.average().toFloat()
        }

        // Top re-steeped tea: the tea with the most steeps per session, over 2+ sessions
        val teas = teaRepository.getAll().associateBy { it.id }
        val topReSteepedTea = sessions
            .groupBy { it.session.teaId }
            .filter { (_, teaSessions) -> teaSessions.size >= 2 }
            .mapNotNull { (teaId, teaSessions) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                val avgSteeps = teaSessions.map { it.steeps.size }.average().toFloat()
                if (avgSteeps < 2f) null else TopReSteepedTea(tea = tea, averageSteeps = avgSteeps)
            }
            .maxByOrNull { it.averageSteeps }

        // New tea discoveries: teas first brewed within [start, end)
        val earliestByTeaId = teaSessionRepository.getAllSessions()
            .filter { it.session.status == SessionStatus.COMPLETED }
            .groupBy { it.session.teaId }
            .mapValues { (_, teaSessions) -> teaSessions.minOf { it.startedAt } }

        val teaIdsInPeriod = sessions.map { it.session.teaId }.toSet()
        val newTeaDiscoveries = teaIdsInPeriod.count { teaId ->
            val earliest = earliestByTeaId[teaId] ?: return@count false
            earliest in start..<end
        }

        return SteepInsights(
            averageSteepsPerSession = averageSteepsPerSession,
            topReSteepedTea = topReSteepedTea,
            newTeaDiscoveries = newTeaDiscoveries,
        )
    }
}
