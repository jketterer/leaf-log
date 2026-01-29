package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual class TimerNotificationService {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        requestAuthorization()
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
            setTitle("🍵 ${state.teaName}")
            setBody("$remainingTime remaining")
            setSound(null)  // No sound for ongoing
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

    /**
     * Show timer completion notification with sound.
     */
    actual fun showTimerComplete(teaName: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle("✅ $teaName is ready!")
            setBody("Time to enjoy your tea ☕")
            setSound(UNNotificationSound.defaultSound())
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
     * Cancel all timer notifications.
     */
    actual fun cancelNotifications() {
        center.removeDeliveredNotificationsWithIdentifiers(
            listOf(NOTIFICATION_ID, COMPLETION_NOTIFICATION_ID),
        )
        center.removePendingNotificationRequestsWithIdentifiers(
            listOf(NOTIFICATION_ID, COMPLETION_NOTIFICATION_ID),
        )
    }

    companion object {
        private const val NOTIFICATION_ID = "timer_running"
        private const val COMPLETION_NOTIFICATION_ID = "timer_complete"
    }
}