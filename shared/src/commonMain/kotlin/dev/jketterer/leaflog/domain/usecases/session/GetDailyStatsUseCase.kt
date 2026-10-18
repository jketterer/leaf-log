package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GetDailyStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    operator fun invoke(): Flow<DailyStats> =
        teaSessionRepository.getAllSessionsFlow().map(::calculateDailyStats)

    private fun calculateDailyStats(allSessions: List<SessionWithSteeps>): DailyStats {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

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
