package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class HistoryState(
    val allSessions: List<SessionWithSteeps> = emptyList(),  // Unfiltered sessions from repository
    val groups: List<HistoryGroup> = emptyList(),  // Filtered & grouped
    val teas: Map<String, Tea> = emptyMap(),  // teaId -> Tea (for display)
    val brewedTeas: List<Tea> = emptyList(),  // Teas with a session, most recently brewed first
    val teaTypes: Map<String, TeaType> = emptyMap(),  // teaTypeId -> TeaType
    val vessels: Map<String, BrewingVessel> = emptyMap(),  // vesselId -> BrewingVessel
    val tagVocabulary: List<TastingTag> = emptyList(),

    val searchQuery: String = "",
    val filters: HistoryFilters = HistoryFilters(),

    // UI state
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val showFilterSheet: Boolean = false,
    val sessionPendingDelete: String? = null,
    val inProgressDialogState: InProgressDialogState? = null,
)

/** A session passes when it matches any of the chosen tea types and any of the chosen vessels. */
data class HistoryFilters(
    val teaTypeIds: Set<String> = emptySet(),
    val teaId: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    val minRating: Float? = null,
    val vesselIds: Set<String> = emptySet(),
) {
    /** Counts each kind of filter once: two tea types, or both ends of a date range, are one. */
    val activeCount: Int
        get() = listOf(
            teaTypeIds.isNotEmpty(),
            teaId != null,
            dateRangeStart != null || dateRangeEnd != null,
            minRating != null,
            vesselIds.isNotEmpty(),
        ).count { it }

    val isActive: Boolean get() = activeCount > 0
}

/** A run of History's sessions under one heading; a null [period] is the flat list of a search or filter. */
data class HistoryGroup(
    val period: HistoryPeriod?,
    val sessions: List<SessionWithSteeps>,
)

sealed interface HistoryPeriod {
    data class Day(val date: LocalDate) : HistoryPeriod

    /** [firstDay] is the 1st of the month. */
    data class Month(val firstDay: LocalDate) : HistoryPeriod
}

/** How many days, today included, History heads by day before it heads by month. */
internal const val DAYS_HEADED_BY_DAY = 30

/**
 * Groups newest-first sessions under the day each started on, for the last [DAYS_HEADED_BY_DAY]
 * days counting [today], and under its month before that.
 */
fun List<SessionWithSteeps>.groupByDay(today: LocalDate, tz: TimeZone): List<HistoryGroup> {
    val firstDayHeaded = today.minus(DAYS_HEADED_BY_DAY - 1, DateTimeUnit.DAY)
    return groupBy { session ->
        val date = session.startedAt.toLocalDateTime(tz).date
        if (date >= firstDayHeaded) {
            HistoryPeriod.Day(date)
        } else {
            HistoryPeriod.Month(LocalDate(date.year, date.month, 1))
        }
    }.map { (period, sessions) -> HistoryGroup(period, sessions) }
}
