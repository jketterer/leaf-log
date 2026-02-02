package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GetDailyStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    operator fun invoke(): Flow<DailyStats> {
        return combine(
            teaSessionRepository.getAllFlow(),
            preferencesRepository.getPreferencesFlow(),
        ) { sessions, prefs -> calculateDailyStats(sessions, prefs) }
    }

    private fun calculateDailyStats(
        allSessions: List<TeaSession>,
        prefs: UserPreferences
    ): DailyStats {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Filter to today's completed sessions only
        val todaySessions = allSessions.filter { session ->
            val sessionDate =
                session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
            sessionDate == today && session.status == SessionStatus.COMPLETED
        }

        // Count only parent sessions (not child steeps)
        val parentSessions = todaySessions.filter { it.parentSessionId == null }

        // Calculate total water consumed (sum across all steeps, including children)
        val totalWaterMl = todaySessions.sumOf { it.waterQuantityMl }
        val formattedWaterQuantity = VolumeFormatter.format(totalWaterMl, prefs.volumeUnit)

        // Count unique teas brewed today (only parent sessions)
        val uniqueTeaIds = parentSessions.map { it.teaId }.distinct()

        return DailyStats(
            sessionCount = parentSessions.size,
            formattedWaterQuantity = formattedWaterQuantity,
            differentTeasCount = uniqueTeaIds.size,
        )
    }
}
