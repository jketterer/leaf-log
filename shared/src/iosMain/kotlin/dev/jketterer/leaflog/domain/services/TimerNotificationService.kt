package dev.jketterer.leaflog.domain.services

import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
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
import kotlin.coroutines.resume

class TimerNotificationServiceImpl : NSObject(),
    UNUserNotificationCenterDelegateProtocol {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    private var lastState: TimerState? = null
    private var isActivityActive = false

    init {
        requestAuthorization()
        center.delegate = this
    }

    private fun requestAuthorization() {
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, error ->
            if (!granted) {
                Logger.w("Notification") { "Notification permission denied: ${error?.localizedDescription}" }
            }
        }
    }

    fun showTimerRunning(state: TimerState) {
        val service = LiveActivityServiceHolder.instance
        val prev = lastState
        lastState = state

        if (service != null) {
            if (!isActivityActive || prev?.sessionId != state.sessionId) {
                service.start(
                    teaName = state.teaName,
                    steepNumber = state.steepNumber,
                    totalSeconds = state.totalDuration.inWholeSeconds.toDouble(),
                    remainingSeconds = state.remainingDuration.inWholeSeconds.toDouble(),
                    sessionId = state.sessionId,
                )
                isActivityActive = true
            } else {
                service.update(
                    remainingSeconds = state.remainingDuration.inWholeSeconds.toDouble(),
                    isPaused = false,
                )
            }
        }
    }

    fun showTimerComplete(teaName: String, sessionId: String?) {
        // The UNTimeIntervalNotificationTrigger scheduled in scheduleCompletionAlarm()
        // fires independently and is shown via willPresentNotification (even in foreground).
        endLiveActivity()
    }

    fun onTimerStopped() {
        cancelCompletionAlarm()
        endLiveActivity()
    }

    private fun endLiveActivity() {
        if (isActivityActive) {
            LiveActivityServiceHolder.instance?.end()
            isActivityActive = false
            lastState = null
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
                Logger.w("Notification") { "Failed to schedule completion alarm: ${it.localizedDescription}" }
            }
        }
    }

    fun cancelCompletionAlarm() {
        center.removePendingNotificationRequestsWithIdentifiers(
            listOf(SCHEDULED_COMPLETION_ID),
        )
        if (isActivityActive) {
            val remaining = lastState?.remainingDuration?.inWholeSeconds?.toDouble() ?: 0.0
            LiveActivityServiceHolder.instance?.update(
                remainingSeconds = remaining,
                isPaused = true
            )
        }
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
        endLiveActivity()
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val sessionId = userInfo["sessionId"] as? String
        if (sessionId != null) {
            DeepLinkHandler.setRoute(NavRoute.TimerRoute(sessionId))
        }
        withCompletionHandler()
    }

    suspend fun hasNotificationPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            continuation.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized)
        }
    }

    private fun buildUserInfo(sessionId: String?): Map<Any?, Any?> {
        return if (sessionId != null) {
            mapOf("sessionId" to sessionId)
        } else {
            emptyMap()
        }
    }

}

private const val SCHEDULED_COMPLETION_ID = "timer_scheduled_complete"
