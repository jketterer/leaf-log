package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetBrewingActivityUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    suspend operator fun invoke(
        start: Instant,
        end: Instant,
        period: AnalyticsPeriod,
    ): Result<List<ActivityCell>> = runCatching {
        if (period == AnalyticsPeriod.ALL_TIME) return@runCatching emptyList()

        val tz = TimeZone.currentSystemDefault()
        val sessions = teaSessionRepository.getByDateRange(start, end)
        val completedParents = sessions.filter {
            it.status == SessionStatus.COMPLETED && it.parentSessionId == null
        }

        val useWeekly = period == AnalyticsPeriod.LAST_90_DAYS || period == AnalyticsPeriod.THIS_YEAR

        if (completedParents.isEmpty()) {
            return@runCatching generateEmptyCells(start, end, useWeekly, tz)
        }

        val teas = teaRepository.getAll().associateBy { it.id }
        val teaTypes = teaTypeRepository.getAll().associateBy { it.id }

        // teaId -> colorHex (may be empty string if tea type has no color set)
        val teaColorMap: Map<String, String> = teas.values.associate { tea ->
            tea.id to (teaTypes[tea.teaTypeId]?.colorHex ?: "")
        }

        if (useWeekly) {
            buildWeeklyCells(completedParents, teaColorMap, start, end, tz)
        } else {
            buildDailyCells(completedParents, teaColorMap, start, end, tz)
        }
    }

    private fun buildDailyCells(
        sessions: List<dev.jketterer.leaflog.domain.models.TeaSession>,
        teaColorMap: Map<String, String>,
        start: Instant,
        end: Instant,
        tz: TimeZone,
    ): List<ActivityCell> {
        val startDate = start.toLocalDateTime(tz).date
        val endDate = end.toLocalDateTime(tz).date
        val sessionsByDay = sessions.groupBy { it.timestamp.toLocalDateTime(tz).date }

        val result = mutableListOf<ActivityCell>()
        var current = startDate
        while (current <= endDate) {
            val daySessions = sessionsByDay[current]
            val dominantColor = daySessions?.mapNotNull { teaColorMap[it.teaId] }
                ?.let { colors -> dominantNonEmpty(colors) }
            result.add(ActivityCell(date = current, dominantTeaTypeColorHex = dominantColor))
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    private fun buildWeeklyCells(
        sessions: List<dev.jketterer.leaflog.domain.models.TeaSession>,
        teaColorMap: Map<String, String>,
        start: Instant,
        end: Instant,
        tz: TimeZone,
    ): List<ActivityCell> {
        val startMonday = start.toLocalDateTime(tz).date.toWeekMonday()
        val endMonday = end.toLocalDateTime(tz).date.toWeekMonday()
        val sessionsByWeek = sessions.groupBy { it.timestamp.toLocalDateTime(tz).date.toWeekMonday() }

        val result = mutableListOf<ActivityCell>()
        var current = startMonday
        while (current <= endMonday) {
            val weekSessions = sessionsByWeek[current]
            val dominantColor = weekSessions?.mapNotNull { teaColorMap[it.teaId] }
                ?.let { colors -> dominantNonEmpty(colors) }
            result.add(ActivityCell(date = current, dominantTeaTypeColorHex = dominantColor))
            current = current.plus(7, DateTimeUnit.DAY)
        }
        return result
    }

    private fun generateEmptyCells(
        start: Instant,
        end: Instant,
        useWeekly: Boolean,
        tz: TimeZone,
    ): List<ActivityCell> {
        return if (useWeekly) {
            val startMonday = start.toLocalDateTime(tz).date.toWeekMonday()
            val endMonday = end.toLocalDateTime(tz).date.toWeekMonday()
            val result = mutableListOf<ActivityCell>()
            var current = startMonday
            while (current <= endMonday) {
                result.add(ActivityCell(date = current, dominantTeaTypeColorHex = null))
                current = current.plus(7, DateTimeUnit.DAY)
            }
            result
        } else {
            val startDate = start.toLocalDateTime(tz).date
            val endDate = end.toLocalDateTime(tz).date
            val result = mutableListOf<ActivityCell>()
            var current = startDate
            while (current <= endDate) {
                result.add(ActivityCell(date = current, dominantTeaTypeColorHex = null))
                current = current.plus(1, DateTimeUnit.DAY)
            }
            result
        }
    }

    // Returns the most frequent non-empty color, or null if none found
    private fun dominantNonEmpty(colors: List<String>): String? {
        val valid = colors.filter { it.isNotEmpty() }
        return valid.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    private fun LocalDate.toWeekMonday(): LocalDate {
        // dayOfWeek.ordinal: MONDAY=0, ..., SUNDAY=6
        return minus(dayOfWeek.ordinal, DateTimeUnit.DAY)
    }
}
