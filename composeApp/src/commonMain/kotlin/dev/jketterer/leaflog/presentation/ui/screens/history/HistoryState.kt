package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class HistoryState(
    val sessions: List<TeaSession> = emptyList(),
    val groupedSessions: Map<String, List<TeaSession>> = emptyMap(),  // "Today", "Yesterday", etc.
    val teas: Map<String, Tea> = emptyMap(),  // teaId -> Tea (for display)
    val teaTypes: Map<String, TeaType> = emptyMap(),  // teaTypeId -> TeaType
    val vessels: Map<String, BrewingVessel> = emptyMap(),  // vesselId -> BrewingVessel

    // Filters
    val searchQuery: String = "",
    val selectedTeaTypeId: String? = null,
    val selectedTeaId: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    val minRating: Float? = null,
    val showDraftsOnly: Boolean = false,

    // Draft completion
    val draftToComplete: TeaSession? = null,
    val showCompleteDraftDialog: Boolean = false,

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val showFilterSheet: Boolean = false,
    val userPreferences: UserPreferences = UserPreferences(),
)

/**
 * Helper to group sessions by time period.
 */
fun List<TeaSession>.groupByTimePeriod(): Map<String, List<TeaSession>> {
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    return groupBy { session ->
        val sessionDate = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
        when {
            sessionDate == today -> "Today"
            sessionDate == yesterday -> "Yesterday"
            sessionDate >= today.minus(7, DateTimeUnit.DAY) -> "This Week"
            sessionDate >= today.minus(30, DateTimeUnit.DAY) -> "This Month"
            else -> sessionDate.year.toString()
        }
    }
}