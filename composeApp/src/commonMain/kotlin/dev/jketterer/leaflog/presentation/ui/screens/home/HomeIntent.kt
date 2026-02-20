package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.TeaSession

sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data object Refresh : HomeIntent
    data object LogTeaClicked : HomeIntent
    data object QuickTimerClicked : HomeIntent
    data class StartQuickTimer(val durationSeconds: Int) : HomeIntent
    data object DismissDurationSheet : HomeIntent
    data class FabExpandedChanged(val expanded: Boolean) : HomeIntent
    data class BrewAgainClicked(val session: TeaSession) : HomeIntent

    data class DeleteSessionClicked(val sessionId: String) : HomeIntent
    data class EditSessionClicked(val sessionId: String) : HomeIntent
    data class SessionClicked(val sessionId: String) : HomeIntent
    data object ViewAllSessionsClicked : HomeIntent
    data object InProgressBannerClicked : HomeIntent
    data object DailyStatsTodaySessionsClicked : HomeIntent
    data object DailyStatsWaterCardClicked : HomeIntent
    data object DailyStatsTeasCardClicked : HomeIntent
    data object ResumeInProgressClicked : HomeIntent
    data object SettingsClicked : HomeIntent
    data object ClearError : HomeIntent
}
