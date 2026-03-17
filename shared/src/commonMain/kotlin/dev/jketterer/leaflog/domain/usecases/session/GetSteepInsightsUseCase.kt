package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SteepInsights
import dev.jketterer.leaflog.domain.models.TopReSteepedTea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetSteepInsightsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): SteepInsights {
        val periodSessions = teaSessionRepository.getByDateRange(start, end)
            .filter { it.status == SessionStatus.COMPLETED }

        val parentSessions = periodSessions.filter { it.parentSessionId == null }
        val childSessions = periodSessions.filter { it.parentSessionId != null }

        // Average steeps per session
        val averageSteepsPerSession = if (parentSessions.isEmpty()) {
            1.0f
        } else {
            val childCountByParent = childSessions.groupingBy { it.parentSessionId!! }.eachCount()
            val totalSteeps = parentSessions.sumOf { parent ->
                1 + (childCountByParent[parent.id] ?: 0)
            }
            totalSteeps.toFloat() / parentSessions.size
        }

        // Top re-steeped tea: group parent sessions by teaId, compute avg steeps per parent
        val teas = teaRepository.getAll().associateBy { it.id }
        val childCountByParent = childSessions.groupingBy { it.parentSessionId!! }.eachCount()
        val topReSteepedTea = parentSessions
            .groupBy { it.teaId }
            .filter { (_, parents) -> parents.size >= 2 }
            .mapNotNull { (teaId, parents) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                val avgSteeps = parents.map { parent ->
                    1 + (childCountByParent[parent.id] ?: 0)
                }.average().toFloat()
                if (avgSteeps < 2f) null else TopReSteepedTea(tea = tea, averageSteeps = avgSteeps)
            }
            .maxByOrNull { it.averageSteeps }

        // New tea discoveries: teas first brewed within [start, end)
        val allSessions = teaSessionRepository.getAll()
            .filter { it.status == SessionStatus.COMPLETED && it.parentSessionId == null }

        val earliestByTeaId = allSessions
            .groupBy { it.teaId }
            .mapValues { (_, sessions) -> sessions.minOf { it.timestamp } }

        val teaIdsInPeriod = parentSessions.map { it.teaId }.toSet()
        val newTeaDiscoveries = teaIdsInPeriod.count { teaId ->
            val earliest = earliestByTeaId[teaId] ?: return@count false
            earliest >= start && earliest < end
        }

        return SteepInsights(
            averageSteepsPerSession = averageSteepsPerSession,
            topReSteepedTea = topReSteepedTea,
            newTeaDiscoveries = newTeaDiscoveries,
        )
    }
}
