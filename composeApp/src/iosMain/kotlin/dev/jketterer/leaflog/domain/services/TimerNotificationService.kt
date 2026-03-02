package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

class TimerNotificationServiceImpl : NSObject(),
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

    fun showTimerRunning(state: TimerState) {
        // No-op on iOS: the UI shows the countdown while the app is in the foreground,
        // and the app is suspended in the background so we cannot update every second.
        // Background completion is handled by scheduleCompletionAlarm().
    }

    fun showTimerComplete(teaName: String, sessionId: String?) {
        // Cancel the scheduled alarm first so it doesn't double-fire alongside this one.
        cancelCompletionAlarm()

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
    fun scheduleCompletionAlarm(
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

    fun cancelCompletionAlarm() {
        center.removePendingNotificationRequestsWithIdentifiers(
            listOf(SCHEDULED_COMPLETION_ID),
        )
    }

    // Required to display notifications while the app is in the foreground.
    // Without this, iOS silently drops all notifications when the app is active.
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (ULong) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
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

}

private const val COMPLETION_NOTIFICATION_ID = "timer_complete"
private const val SCHEDULED_COMPLETION_ID = "timer_scheduled_complete"
