package dev.jketterer.leaflog.presentation.ui.screens.home

sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data object Refresh : HomeIntent
    data object LogTeaClicked : HomeIntent
    data object QuickTimerClicked : HomeIntent
    data class StartQuickTimer(val durationSeconds: Int) : HomeIntent
    data object DismissDurationSheet : HomeIntent
    data class FabExpandedChanged(val expanded: Boolean) : HomeIntent
    data class BrewAgainClicked(val sessionId: String) : HomeIntent
    data class QuickBrewClicked(
        val configurationId: String,
        val teaId: String,
        val vesselId: String,
    ) : HomeIntent

    data class EditQuickBrewClicked(val teaId: String, val configurationId: String) : HomeIntent
    data object ManageQuickBrewClicked : HomeIntent
    data object DismissManageQuickBrewSheet : HomeIntent
    data class PinConfigurationToggled(val configurationId: String, val isPinned: Boolean) :
        HomeIntent

    data class PinnedConfigurationsReordered(val orderedIds: List<String>) : HomeIntent

    data class DeleteSessionClicked(val sessionId: String) : HomeIntent
    data object ConfirmDeleteSession : HomeIntent
    data object CancelDeleteSession : HomeIntent
    data class SessionClicked(val sessionId: String) : HomeIntent
    data object ViewAllSessionsClicked : HomeIntent
    data object ViewAllStatsClicked : HomeIntent
    data object DailyStatsTodaySessionsClicked : HomeIntent
    data object DailyStatsWaterCardClicked : HomeIntent
    data object DailyStatsTeasCardClicked : HomeIntent
    data object ResumeInProgressClicked : HomeIntent
    data object SettingsClicked : HomeIntent
    data object ClearError : HomeIntent

    // In-progress session conflict dialog
    data object ResumeInProgressFromDialog : HomeIntent
    data object DismissInProgressDialog : HomeIntent
    data object CompleteInProgressAndContinue : HomeIntent
    data object DiscardInProgressAndContinue : HomeIntent
}
