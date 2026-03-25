package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState

data class HomeState(
    val greeting: String = "Good day",
    val dailyStats: DailyStats = DailyStats(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val quickBrewConfigurations: List<QuickBrewCardData> = emptyList(),
    val inProgressSessionsCount: Int = 0,
    val mostRecentInProgress: InProgressSessionDetails? = null,
    val liveTimerState: TimerState? = null, // Live timer progress from TimerService
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = true,
    val userPreferences: UserPreferences = UserPreferences(),
    // FAB menu state
    val isFabExpanded: Boolean = false,
    val showDurationSheet: Boolean = false,
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
}
