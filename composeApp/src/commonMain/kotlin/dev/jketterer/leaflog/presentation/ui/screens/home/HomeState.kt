package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.UserPreferences

/**
 * Information about an in-progress session for display on the home banner.
 */
data class InProgressSessionInfo(
    val session: TeaSession,
    val teaName: String,
    val vesselName: String,
)

data class HomeState(
    val greeting: String = "Good day",
    val dailyStats: DailyStats = DailyStats(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val inProgressSessionsCount: Int = 0,
    val mostRecentInProgress: InProgressSessionInfo? = null,
    val liveTimerState: TimerState? = null, // Live timer progress from TimerService
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = true,
    val userPreferences: UserPreferences = UserPreferences(),
    // FAB menu state
    val isFabExpanded: Boolean = false,
    val showDurationSheet: Boolean = false,
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
}
