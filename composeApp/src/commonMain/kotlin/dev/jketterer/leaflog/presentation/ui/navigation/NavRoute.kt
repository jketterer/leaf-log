package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object HomeRoute : NavRoute

    @Serializable
    data object LogTeaRoute : NavRoute

    @Serializable
    data object CollectionRoute : NavRoute

    @Serializable
    data class TeaDetailsRoute(val teaId: String) : NavRoute

    @Serializable
    data class EditTeaRoute(val teaId: String? = null) : NavRoute

    @Serializable
    data object HistoryRoute : NavRoute

    @Serializable
    data object MoreRoute : NavRoute

    @Serializable
    data class SessionDetailsRoute(val sessionId: String) : NavRoute

    @Serializable
    data object TimerRoute : NavRoute

    @Serializable
    data object AnalyticsRoute : NavRoute

    @Serializable
    data object SettingsRoute : NavRoute
}