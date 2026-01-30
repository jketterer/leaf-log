package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object HomeRoute : NavRoute

    @Serializable
    data class LogTeaRoute(val teaId: String? = null) : NavRoute

    @Serializable
    data object CollectionRoute : NavRoute

    @Serializable
    data class TeaDetailsRoute(val teaId: String) : NavRoute

    @Serializable
    data class EditTeaRoute(val teaId: String? = null) : NavRoute

    @Serializable
    data class HistoryRoute(val showDraftsOnly: Boolean = false) : NavRoute

    @Serializable
    data object MoreRoute : NavRoute

    @Serializable
    data class SessionDetailsRoute(val sessionId: String) : NavRoute

    @Serializable
    data class EditSessionRoute(
        val sessionId: String,
        val editFullSession: Boolean = false
    ) : NavRoute

    @Serializable
    data class TimerRoute(val sessionId: String) : NavRoute

    @Serializable
    data object AnalyticsRoute : NavRoute

    @Serializable
    data object SettingsRoute : NavRoute

    @Serializable
    data object VesselListRoute : NavRoute

    @Serializable
    data class VesselDetailRoute(val vesselId: String) : NavRoute

    @Serializable
    data class EditVesselRoute(val vesselId: String? = null) : NavRoute
}