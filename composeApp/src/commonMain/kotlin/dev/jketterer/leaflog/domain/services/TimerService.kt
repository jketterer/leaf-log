package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
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
 */
class TimerService(
    private val coroutineScope: CoroutineScope,
    private val notificationService: TimerNotificationService,
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
        var lastNotificationSecond = -1L
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(100)  // Update every 100ms for smooth UI

                val current = _timerState.value
                if (current.status != TimerStatus.RUNNING || current.startedAt == null) {
                    break
                }

                // Calculate remaining time (simple coordination logic)
                val now = Clock.System.now()
                val elapsed = now - current.startedAt
                val remaining = (current.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

                _timerState.update { it.copy(remainingDuration = remaining) }

                // Trigger notification update every second
                val currentSecond = elapsed.inWholeSeconds
                if (currentSecond > lastNotificationSecond) {
                    notificationService.showTimerRunning(current)
                    lastNotificationSecond = currentSecond
                }

                // Check if timer completed
                if (remaining == Duration.ZERO) {
                    _timerState.update {
                        it.copy(status = TimerStatus.COMPLETE, remainingDuration = Duration.ZERO)
                    }
                    notificationService.showTimerComplete(current.teaName)
                    break
                }
            }
        }
    }

    fun cancelCountdown() {
        timerJob?.cancel()
        timerJob = null
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerState()
    }

    fun getCurrentState(): TimerState = _timerState.value
}

