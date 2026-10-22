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
import kotlin.time.Clock

data class HistoryState(
    val allSessions: List<SessionWithSteeps> = emptyList(),  // Unfiltered sessions from repository
    val groupedSessions: Map<String, List<SessionWithSteeps>> = emptyMap(),  // Filtered & grouped
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

data class HistoryFilters(
    val teaTypeId: String? = null,
    val teaId: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    val minRating: Float? = null,
    val vesselId: String? = null,
) {
    /** A date range counts once, however many of its ends are set. */
    val activeCount: Int
        get() = listOf(
            teaTypeId != null,
            teaId != null,
            dateRangeStart != null || dateRangeEnd != null,
            minRating != null,
            vesselId != null,
        ).count { it }

    val isActive: Boolean get() = activeCount > 0
}

/**
 * Helper to group sessions by time period.
 */
fun List<SessionWithSteeps>.groupByTimePeriod(): Map<String, List<SessionWithSteeps>> {
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    return groupBy { session ->
        val sessionDate = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        when {
            sessionDate == today -> "Today"
            sessionDate == yesterday -> "Yesterday"
            sessionDate >= today.minus(7, DateTimeUnit.DAY) -> "This Week"
            sessionDate >= today.minus(30, DateTimeUnit.DAY) -> "This Month"
            else -> sessionDate.year.toString()
        }
    }
}
