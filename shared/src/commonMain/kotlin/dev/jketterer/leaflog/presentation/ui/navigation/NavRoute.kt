package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.jketterer.leaflog.domain.models.CalendarYear
import dev.jketterer.leaflog.domain.models.RecapPeriod
import dev.jketterer.leaflog.domain.models.Season
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    /** [finishedSessionId]: a session just finished, offered for Undo once. */
    @Serializable
    data class HomeRoute(val finishedSessionId: String? = null) : NavRoute

    /** [sourceSessionId]: a past brew to replay, its tea and vessel filled from it. */
    @Serializable
    data class LogTeaRoute(
        val teaId: String? = null,
        val vesselId: String? = null,
        val sourceSessionId: String? = null,
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
    data class EditSessionRoute(val sessionId: String) : NavRoute

    /** No [sessionId]: the Quick Timer, timing a tea not yet named. */
    @Serializable
    data class TimerRoute(val sessionId: String? = null) : NavRoute

    @Serializable
    data object AnalyticsRoute : NavRoute

    /** [start]: the ISO date the recapped season, or the year when [year], starts on. */
    @Serializable
    data class RecapRoute(val start: String, val year: Boolean = false) : NavRoute {
        val period: RecapPeriod
            get() = LocalDate.parse(start).let { if (year) CalendarYear(it.year) else Season(it) }

        companion object {
            fun of(period: RecapPeriod) = RecapRoute(period.start.toString(), period is CalendarYear)
        }
    }

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
    data class FlavorDetailRoute(val tagId: String) : NavRoute

    @Serializable
    data class SteepCompleteRoute(val sessionId: String) : NavRoute

    /** [logged]: saved from Log Tea, so leaving returns to where Log Tea was opened, with no Undo. */
    @Serializable
    data class SessionReviewRoute(val sessionId: String, val logged: Boolean = false) : NavRoute
}