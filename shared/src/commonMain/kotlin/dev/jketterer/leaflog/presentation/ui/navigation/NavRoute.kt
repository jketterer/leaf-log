package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    /** [finishedSessionId]: a session just finished, offered for Undo once. */
    @Serializable
    data class HomeRoute(val finishedSessionId: String? = null) : NavRoute

    @Serializable
    data class LogTeaRoute(
        val teaId: String? = null,
        val vesselId: String? = null,
        val configurationId: String? = null,
    ) : NavRoute

    @Serializable
    data object CollectionRoute : NavRoute

    @Serializable
    data class TeaDetailsRoute(val teaId: String) : NavRoute

    @Serializable
    data class EditTeaRoute(val teaId: String? = null) : NavRoute

    @Serializable
    data class HistoryRoute(
        val filterTeaTypeId: String? = null,
        val filterTeaId: String? = null,
        val filterDateStart: String? = null,
        val filterDateEnd: String? = null,
    ) : NavRoute

    @Serializable
    data class SessionDetailsRoute(val sessionId: String) : NavRoute

    @Serializable
    data class EditSessionRoute(
        val sessionId: String,
        val editFullSession: Boolean = false
    ) : NavRoute

    /** No [sessionId]: the Quick Timer, timing a tea not yet named. */
    @Serializable
    data class TimerRoute(val sessionId: String? = null) : NavRoute

    @Serializable
    data object AnalyticsRoute : NavRoute

    @Serializable
    data object SettingsRoute : NavRoute

    @Serializable
    data class VesselDetailRoute(val vesselId: String) : NavRoute

    @Serializable
    data class EditVesselRoute(val vesselId: String? = null) : NavRoute

    @Serializable
    data object TeaTypeListRoute : NavRoute

    @Serializable
    data class EditTeaTypeRoute(val teaTypeId: String? = null) : NavRoute

    @Serializable
    data class CreateBrewingConfigurationRoute(
        val teaId: String,
        val configurationId: String? = null,
    ) : NavRoute

    @Serializable
    data class SteepCompleteRoute(val sessionId: String) : NavRoute

    /** [logged]: saved from Log Tea, so leaving returns to where Log Tea was opened, with no Undo. */
    @Serializable
    data class SessionReviewRoute(val sessionId: String, val logged: Boolean = false) : NavRoute
}