package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import kotlinx.datetime.LocalDate

data class HomeState(
    val greeting: String = "Good day",
    // The day dailyStats counts, moved forward when Home resumes on a later one
    val today: LocalDate? = null,
    val dailyStats: DailyStats = DailyStats(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val quickBrewConfigurations: List<QuickBrewCardData> = emptyList(),
    val allBrewingConfigurations: List<QuickBrewCardData> = emptyList(),
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
    val showManageQuickBrewSheet: Boolean = false,
    val sessionPendingDelete: String? = null,
    // In-progress session conflict dialog
    val inProgressDialogState: InProgressDialogState? = null,
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
