package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

actual class TimerNotificationService : NSObject(),
    UNUserNotificationCenterDelegateProtocol {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        requestAuthorization()
        center.delegate = this
    }

    private fun requestAuthorization() {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, error ->
            if (!granted) {
                println("Notification permission denied: ${error?.localizedDescription}")
            }
        }
    }

    actual fun showTimerRunning(state: TimerState) {
        val minutes = state.remainingDuration.inWholeMinutes
        val seconds = state.remainingDuration.inWholeSeconds % 60
        val remainingTime = "${minutes}:${seconds.toString().padStart(2, '0')}"

        val content = UNMutableNotificationContent().apply {
            setTitle(state.teaName)
            setBody("$remainingTime remaining")
            setSound(null)  // No sound for ongoing
            setUserInfo(buildUserInfo(state.sessionId))
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NOTIFICATION_ID,
            content = content,
            trigger = null,  // Show immediately
        )

        center.addNotificationRequest(request) { error ->
            error?.let {
                println("Failed to show notification: ${it.localizedDescription}")
            }
        }
    }

    actual fun showTimerComplete(teaName: String, sessionId: String?) {
        val content = UNMutableNotificationContent().apply {
            setTitle("$teaName is ready!")
            setBody("Time to enjoy your tea")
            setSound(UNNotificationSound.defaultSound())
            setUserInfo(buildUserInfo(sessionId))
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = COMPLETION_NOTIFICATION_ID,
            content = content,
            trigger = null,
        )

        center.addNotificationRequest(request) { error ->
            error?.let {
                println("Failed to show completion notification: ${it.localizedDescription}")
            }
        }
    }

    /**
     * Schedule a local notification that iOS will fire even if the app is suspended.
     */
    actual fun scheduleCompletionAlarm(
        teaName: String,
        remainingSeconds: Double,
        sessionId: String?,
    ) {
        cancelCompletionAlarm()

        if (remainingSeconds <= 0) return

        val content = UNMutableNotificationContent().apply {
            setTitle("$teaName is ready!")
            setBody("Time to enjoy your tea")
            setSound(UNNotificationSound.defaultSound())
            setUserInfo(buildUserInfo(sessionId))
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = remainingSeconds,
            repeats = false,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = SCHEDULED_COMPLETION_ID,
            content = content,
            trigger = trigger,
        )

        center.addNotificationRequest(request) { error ->
            error?.let {
                println("Failed to schedule completion alarm: ${it.localizedDescription}")
            }
        }
    }

    actual fun cancelCompletionAlarm() {
        center.removePendingNotificationRequestsWithIdentifiers(
            listOf(SCHEDULED_COMPLETION_ID),
        )
    }

    // UNUserNotificationCenterDelegateProtocol — handle notification taps
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val sessionId = userInfo["sessionId"] as? String
        if (sessionId != null) {
            DeepLinkHandler.setRoute(NavRoute.TimerRoute(sessionId))
        }
        withCompletionHandler()
    }

    private fun buildUserInfo(sessionId: String?): Map<Any?, Any?> {
        return if (sessionId != null) {
            mapOf("sessionId" to sessionId)
        } else {
            emptyMap()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = "timer_running"
        private const val COMPLETION_NOTIFICATION_ID = "timer_complete"
        private const val SCHEDULED_COMPLETION_ID = "timer_scheduled_complete"
    }
}
