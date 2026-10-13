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
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
        val rated = sessions.filter {
            it.session.status == SessionStatus.COMPLETED && it.effectiveRating != null
        }

        val teas = teaRepository.getAll().associateBy { it.id }

        return rated
            .groupBy { it.session.teaId }
            .mapNotNull { (teaId, sessions) ->
                val tea = teas[teaId] ?: return@mapNotNull null
                val avgRating = sessions.map { it.effectiveRating!! }.average().toFloat()
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
