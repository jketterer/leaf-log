package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Instant

class GetTopRatedTeasUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): List<TopRatedTea> {
        val sessions = teaSessionRepository.getByDateRange(start, end)
        val ratedParents = sessions.filter { session ->
            session.status == SessionStatus.COMPLETED &&
                session.parentSessionId == null &&
                (session.averageRating != null || session.rating != null)
        }

        val teas = teaRepository.getAll().associateBy { it.id }

        return ratedParents
            .groupBy { it.teaId }
            .filter { (_, sessions) -> sessions.size >= 2 }
            .mapNotNull { (teaId, sessions) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                val avgRating = sessions.map { session ->
                    session.averageRating ?: session.rating!!
                }.average().toFloat()
                TopRatedTea(
                    tea = tea,
                    averageRating = avgRating,
                    ratedSessionCount = sessions.size,
                )
            }
            .sortedByDescending { it.averageRating }
            .take(5)
    }
}
