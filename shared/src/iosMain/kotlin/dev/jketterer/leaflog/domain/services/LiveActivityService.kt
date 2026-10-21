package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute

interface LiveActivityService {
    fun start(teaName: String, steepNumber: Int, totalSeconds: Double, remainingSeconds: Double, sessionId: String?)
    fun update(remainingSeconds: Double, isPaused: Boolean)
    fun end()
}

object LiveActivityServiceHolder {
    var instance: LiveActivityService? = null
}

/** The Live Activity's tap: it opens the Timer for its session, or the Quick Timer without one. */
fun openTimerFromLiveActivity(sessionId: String?) {
    DeepLinkHandler.setRoute(NavRoute.TimerRoute(sessionId))
}
