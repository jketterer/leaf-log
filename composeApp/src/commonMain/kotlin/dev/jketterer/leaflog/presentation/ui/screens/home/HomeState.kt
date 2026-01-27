package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.TeaSession

data class HomeState(
    val greeting: String = "Good day",
    val dailyStats: DailyStats = DailyStats(),
    val recentSessions: List<TeaSession> = emptyList(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val draftSessionsCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = true,
) {
    /**
     * Whether to show the empty state (no sessions at all).
     */
    val shouldShowEmptyState: Boolean
        get() = isEmpty && !isLoading

    /**
     * Whether to show the draft sessions banner.
     */
    val shouldShowDraftBanner: Boolean
        get() = draftSessionsCount > 0
}
