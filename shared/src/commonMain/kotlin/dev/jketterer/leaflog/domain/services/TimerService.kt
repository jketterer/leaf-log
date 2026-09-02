package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.session.SessionReminderDelay
import dev.jketterer.leaflog.domain.usecases.timer.SaveTimerStateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Domain service that manages brewing timer countdown.
 *
 * @param coroutineScope Application-level scope (survives ViewModels)
 * @param notificationService Platform-specific notification service
 * @param saveTimerStateUseCase Persists timer state to database (for completion while backgrounded)
 * @param lifecycleHandler Platform bridge for foreground service / scheduled notifications
 */
class TimerService(
    private val coroutineScope: CoroutineScope,
    private val notificationService: TimerNotificationService,
    private val saveTimerStateUseCase: SaveTimerStateUseCase,
    private val lifecycleHandler: TimerLifecycleHandler,
    preferencesRepository: PreferencesRepository,
) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // Mirrored from preferences so the countdown loop can check them without suspending
    private val notificationPreferences = MutableStateFlow(UserPreferences())

    init {
        coroutineScope.launch {
            preferencesRepository.getPreferencesFlow().collect { prefs ->
                notificationPreferences.value = prefs
                applyNotificationPreferences(prefs)
            }
        }
    }

    /**
     * Withdraw or restore scheduled alerts so a toggle takes effect on the brew already running,
     * not just the next one.
     */
    private fun applyNotificationPreferences(prefs: UserPreferences) {
        val state = _timerState.value
        val isRunning = state.status == TimerStatus.RUNNING

        if (!prefs.timerCompletionNotificationsEnabled) {
            notificationService.cancelCompletionAlarm()
        } else if (isRunning) {
            notificationService.scheduleCompletionAlarm(
                teaName = state.teaName,
                remainingSeconds = state.remainingDuration.inWholeMilliseconds / 1000.0,
                sessionId = state.sessionId,
            )
        }

        if (!prefs.sessionReminderNotificationsEnabled) {
            notificationService.cancelSessionReminder()
        } else if (isRunning) {
            scheduleSessionReminder(state)
        }
    }

    /**
     * Update timer state (called by ViewModel after use case execution).
     */
    fun updateState(newState: TimerState) {
        _timerState.update { newState }
    }

    /**
     * Start the countdown loop with current state.
     * Call this after use case has prepared the state.
     */
    fun startCountdown() {
        val current = _timerState.value
        lifecycleHandler.onTimerStarted()
        if (notificationPreferences.value.timerCompletionNotificationsEnabled) {
            notificationService.scheduleCompletionAlarm(
                teaName = current.teaName,
                remainingSeconds = current.remainingDuration.inWholeMilliseconds / 1000.0,
                sessionId = current.sessionId,
            )
        }
        scheduleSessionReminder(current)

        timerJob?.cancel()
        var lastNotificationSecond = -1L
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(100.milliseconds)  // Update every 100ms for smooth UI

                val state = _timerState.value
                if (state.status != TimerStatus.RUNNING || state.startedAt == null) {
                    break
                }

                // Calculate remaining time (simple coordination logic)
                val now = Clock.System.now()
                val elapsed = now - state.startedAt
                val remaining = (state.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

                _timerState.update { it.copy(remainingDuration = remaining) }

                // Check if timer completed before sending notification update,
                // to avoid sending remainingSeconds=0 which races with the end call
                if (remaining > Duration.ZERO) {
                    val currentSecond = elapsed.inWholeSeconds
                    if (currentSecond > lastNotificationSecond) {
                        notificationService.showTimerRunning(state)
                        lastNotificationSecond = currentSecond
                    }
                }

                if (remaining == Duration.ZERO) {
                    val completedState = state.copy(
                        status = TimerStatus.COMPLETE,
                        remainingDuration = Duration.ZERO,
                    )
                    _timerState.update { completedState }
                    lifecycleHandler.onTimerStopped()
                    if (notificationPreferences.value.timerCompletionNotificationsEnabled) {
                        notificationService.showTimerComplete(state.teaName, state.sessionId)
                    }
                    notificationService.cancelCompletionAlarm()
                    // Persist completion so HomeScreen banner reflects correct state
                    // even if no ViewModel is active (e.g., app backgrounded)
                    saveTimerStateUseCase(completedState)
                    break
                }
            }
        }
    }

    /**
     * Stop the countdown but leave the timer on screen, holding at its current remaining time.
     */
    fun cancelCountdown() {
        timerJob?.cancel()
        timerJob = null
        notificationService.cancelCompletionAlarm()
        notificationService.cancelSessionReminder()
        notificationService.showTimerPaused(_timerState.value)
    }

    /**
     * Tear down the countdown and reset state. Deliberately leaves the session reminder alone:
     * this runs when a steep finishes, which is when the session *starts* waiting for review.
     * Use [onSessionResolved] for the case where review is no longer needed.
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        lifecycleHandler.onTimerStopped()
        notificationService.onTimerStopped()
        _timerState.value = TimerState()
    }

    /**
     * The session has been finished or discarded, so withdraw its finish-your-session reminder.
     * Skipped when a different session's timer is live, since that brew's reminder is still owed.
     */
    fun onSessionResolved(sessionId: String) {
        val activeSessionId = _timerState.value.sessionId
        if (activeSessionId == null || activeSessionId == sessionId) {
            notificationService.cancelSessionReminder()
        }
    }

    /**
     * Schedule the finish-your-session nudge for after the steep has run and the tea has cooled.
     * Deliberately not canceled when the timer completes: that is the point at which the session
     * starts waiting for review.
     */
    private fun scheduleSessionReminder(state: TimerState) {
        if (!notificationPreferences.value.sessionReminderNotificationsEnabled) return

        val sessionId = state.sessionId ?: return
        val coolDown = SessionReminderDelay.forBrewingTemperature(state.brewingTemperatureCelsius)
        val delay = state.remainingDuration + coolDown

        notificationService.scheduleSessionReminder(
            teaName = state.teaName,
            sessionId = sessionId,
            delaySeconds = delay.inWholeMilliseconds / 1000.0,
        )
    }

    /**
     * Called after time adjustments to reschedule the completion alarm.
     */
    fun onTimeAdjusted(state: TimerState) {
        if (state.status == TimerStatus.RUNNING) {
            notificationService.scheduleCompletionAlarm(
                teaName = state.teaName,
                remainingSeconds = state.remainingDuration.inWholeMilliseconds / 1000.0,
                sessionId = state.sessionId,
            )
            scheduleSessionReminder(state)
        }
    }

    fun getCurrentState(): TimerState = _timerState.value
}
