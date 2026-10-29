package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.defaultBrew
import dev.jketterer.leaflog.domain.models.lastCompleted
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainSheetState
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import kotlinx.datetime.LocalDate

data class HomeState(
    val greeting: String = "Good day",
    // The day dailyStats counts, moved forward when Home resumes on a later one
    val today: LocalDate? = null,
    val dailyStats: DailyStats = DailyStats(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val brewAgainCards: List<BrewAgainCardData> = emptyList(),
    val inProgressSessionsCount: Int = 0,
    val mostRecentInProgress: InProgressSessionDetails? = null,
    // Timer status of mostRecentInProgress resolved against the clock, not as last persisted
    val inProgressTimerStatus: TimerStatus? = null,
    // Its progress resolved the same way, for when no live timer is tracking it
    val resolvedTimerProgress: Float? = null,
    val liveTimerState: TimerState? = null, // Live timer progress from TimerService
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = true,
    // FAB menu state
    val isFabExpanded: Boolean = false,
    val showDurationSheet: Boolean = false,
    val sessionPendingDelete: String? = null,
    // In-progress session conflict dialog
    val inProgressDialogState: InProgressDialogState? = null,
    val brewAgainSheet: BrewAgainSheetState? = null,
) {
    /**
     * Whether to show the empty state (no sessions at all).
     */
    val shouldShowEmptyState: Boolean
        get() = isEmpty && !isLoading

    /**
     * Whether to show the in-progress sessions banner.
     */
    val shouldShowInProgressBanner: Boolean
        get() = inProgressSessionsCount > 0

    /**
     * Live progress for the banner, but only when the running timer belongs to the session the
     * banner is showing. Otherwise one session's countdown renders on another session's card.
     * With no live timer, as after the process dies, the saved timer resolved at load.
     */
    val inProgressTimerProgress: Float?
        get() = liveTimerState
            ?.takeIf { it.sessionId == mostRecentInProgress?.session?.session?.id }
            ?.progress
            ?: resolvedTimerProgress
}

/** A pair, one tea in one vessel, on Home's Brew again row, showing the [brew] it replays. */
data class BrewAgainCardData(
    val teaId: String,
    val vesselId: String,
    val teaName: String,
    val teaTypeColorHex: String?,
    val vesselName: String,
    val brew: SessionWithSteeps,
)

private const val BREW_AGAIN_CARDS = 8

/**
 * One card per pair with a completed session, the pair brewed most recently first, at most 8.
 * Finished teas and archived vessels are left out.
 */
internal fun brewAgainCards(
    sessions: List<SessionWithSteeps>,
    teas: List<Tea>,
    teaTypes: List<TeaType>,
    vessels: List<BrewingVessel>,
): List<BrewAgainCardData> {
    val teaById = teas.associateBy { it.id }
    val typeById = teaTypes.associateBy { it.id }
    val vesselById = vessels.associateBy { it.id }
    return sessions.groupBy { it.session.teaId to it.session.vesselId }
        .mapNotNull { (pair, pairSessions) ->
            val tea = teaById[pair.first]?.takeIf { it.finishedAt == null } ?: return@mapNotNull null
            val vessel = vesselById[pair.second]?.takeIf { !it.isArchived } ?: return@mapNotNull null
            val last = pairSessions.lastCompleted() ?: return@mapNotNull null
            last.startedAt to BrewAgainCardData(
                teaId = tea.id,
                vesselId = vessel.id,
                teaName = tea.name,
                teaTypeColorHex = typeById[tea.teaTypeId]?.colorHex,
                vesselName = vessel.name,
                brew = pairSessions.defaultBrew() ?: last,
            )
        }
        .sortedByDescending { it.first }
        .take(BREW_AGAIN_CARDS)
        .map { it.second }
}
