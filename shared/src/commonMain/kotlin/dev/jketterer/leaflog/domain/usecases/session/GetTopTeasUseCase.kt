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
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
        val completed = sessions.filter { it.session.status == SessionStatus.COMPLETED }

        val teas = teaRepository.getAll().associateBy { it.id }

        return completed
            .groupingBy { it.session.teaId }
            .eachCount()
            .mapNotNull { (teaId, count) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                TopTea(tea = tea, sessionCount = count)
            }
            .sortedByDescending { it.sessionCount }
            .take(5)
    }
}
