package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Instant

class GetTopTeasUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): List<TopTea> {
        val sessions = teaSessionRepository.getByDateRange(start, end)
        val completedParents = sessions.filter {
            it.status == SessionStatus.COMPLETED && it.parentSessionId == null
        }

        val teas = teaRepository.getAll().associateBy { it.id }

        return completedParents
            .groupingBy { it.teaId }
            .eachCount()
            .mapNotNull { (teaId, count) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                TopTea(tea = tea, sessionCount = count)
            }
            .sortedByDescending { it.sessionCount }
            .take(5)
    }
}
