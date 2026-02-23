package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
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
) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

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
        notificationService.scheduleCompletionAlarm(
            teaName = current.teaName,
            remainingSeconds = current.remainingDuration.inWholeMilliseconds / 1000.0,
            sessionId = current.sessionId,
        )

        timerJob?.cancel()
        var lastNotificationSecond = -1L
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(100)  // Update every 100ms for smooth UI

                val state = _timerState.value
                if (state.status != TimerStatus.RUNNING || state.startedAt == null) {
                    break
                }

                // Calculate remaining time (simple coordination logic)
                val now = Clock.System.now()
                val elapsed = now - state.startedAt
                val remaining = (state.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

                _timerState.update { it.copy(remainingDuration = remaining) }

                // Trigger notification update every second
                val currentSecond = elapsed.inWholeSeconds
                if (currentSecond > lastNotificationSecond) {
                    notificationService.showTimerRunning(state)
                    lastNotificationSecond = currentSecond
                }

                // Check if timer completed
                if (remaining == Duration.ZERO) {
                    val completedState = state.copy(
                        status = TimerStatus.COMPLETE,
                        remainingDuration = Duration.ZERO,
                    )
                    _timerState.update { completedState }
                    lifecycleHandler.onTimerStopped()
                    notificationService.showTimerComplete(state.teaName, state.sessionId)
                    // Persist completion so HomeScreen banner reflects correct state
                    // even if no ViewModel is active (e.g., app backgrounded)
                    saveTimerStateUseCase(completedState)
                    break
                }
            }
        }
    }

    fun cancelCountdown() {
        timerJob?.cancel()
        timerJob = null
        notificationService.cancelCompletionAlarm()
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        lifecycleHandler.onTimerStopped()
        notificationService.cancelCompletionAlarm()
        _timerState.value = TimerState()
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
        }
    }

    fun getCurrentState(): TimerState = _timerState.value
}
