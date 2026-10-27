package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetDailyStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    /** Stats for [today], which the caller passes so a new day can ask again. */
    operator fun invoke(today: LocalDate): Flow<DailyStats> =
        teaSessionRepository.getAllSessionsFlow().map { calculateDailyStats(it, today) }

    private fun calculateDailyStats(allSessions: List<SessionWithSteeps>, today: LocalDate): DailyStats {
        val todaySessions = allSessions.filter { session ->
            val sessionDate = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            sessionDate == today && session.session.status == SessionStatus.COMPLETED
        }

        val totalWaterMl = todaySessions.sumOf { session -> session.steeps.sumOf { it.waterQuantityMl } }

        val uniqueTeaIds = todaySessions.map { it.session.teaId }.distinct()

        return DailyStats(
            sessionCount = todaySessions.size,
            totalWaterMl = totalWaterMl,
            differentTeasCount = uniqueTeaIds.size,
        )
    }
}
