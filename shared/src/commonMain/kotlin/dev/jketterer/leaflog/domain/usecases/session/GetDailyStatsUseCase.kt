package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
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
            teaSessionRepository.getAllSessionsFlow(),
            preferencesRepository.getPreferencesFlow(),
        ) { sessions, prefs -> calculateDailyStats(sessions, prefs) }
    }

    private fun calculateDailyStats(
        allSessions: List<SessionWithSteeps>,
        prefs: UserPreferences
    ): DailyStats {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        val todaySessions = allSessions.filter { session ->
            val sessionDate = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            sessionDate == today && session.session.status == SessionStatus.COMPLETED
        }

        val totalWaterMl = todaySessions.sumOf { session -> session.steeps.sumOf { it.waterQuantityMl } }
        val formattedWaterQuantity = VolumeFormatter.format(totalWaterMl, prefs.volumeUnit)

        val uniqueTeaIds = todaySessions.map { it.session.teaId }.distinct()

        return DailyStats(
            sessionCount = todaySessions.size,
            formattedWaterQuantity = formattedWaterQuantity,
            differentTeasCount = uniqueTeaIds.size,
        )
    }
}
