package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.isCompleteMethod
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class QuickTimerViewModel(
    private val route: NavRoute.QuickTimerRoute,
    private val teaRepository: TeaRepository,
    private val vesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: TimerNotificationService,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val saveTimerStateUseCase: SaveTimerStateUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickTimerState())
    val state: StateFlow<QuickTimerState> = _state.asStateFlow()

    private val _navigationEvents = Channel<QuickTimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var timerJob: Job? = null
    private var startedAt: Instant? = null

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs ->
                state.copy(
                    userPreferences = prefs,
                    waterType = if (state.selectedTea == null) prefs.defaultWaterType else state.waterType,
                )
            },
        )
        loadTeasAndVessels()
        initialize(route.durationSeconds)
    }

    private fun loadTeasAndVessels() {
        viewModelScope.launch {
            teaRepository.getAllFlow()
                .catch { e -> Logger.w(tag = "QuickTimer") { "Failed to load teas: ${e.message}" } }
                .collect { teas ->
                    _state.update { it.copy(availableTeas = teas) }
                }
        }

        viewModelScope.launch {
            vesselRepository.getActiveFlow()
                .catch { e -> Logger.w(tag = "QuickTimer") { "Failed to load vessels: ${e.message}" } }
                .collect { vessels ->
                    _state.update { it.copy(availableVessels = vessels) }
                }
        }
    }

    fun onIntent(intent: QuickTimerIntent) {
        when (intent) {
            is QuickTimerIntent.StartTimer -> startTimer()
            is QuickTimerIntent.PauseTimer -> pauseTimer()
            is QuickTimerIntent.ResumeTimer -> resumeTimer()
            is QuickTimerIntent.AdjustTime -> adjustTime(intent.adjustment)
            is QuickTimerIntent.ResetTimer -> showResetConfirmation()
            is QuickTimerIntent.ConfirmReset -> confirmReset()
            is QuickTimerIntent.CancelReset -> cancelReset()
            is QuickTimerIntent.StopTimer -> showStopConfirmation()
            is QuickTimerIntent.ConfirmStop -> confirmStop()
            is QuickTimerIntent.CancelStop -> cancelStop()
            is QuickTimerIntent.BackClicked -> handleBackClicked()
            is QuickTimerIntent.AddSessionDetails -> handleAddSessionDetails()
            is QuickTimerIntent.DiscardCompletedSession -> handleDiscardCompletedSession()
            is QuickTimerIntent.ConfirmDiscardComplete -> handleConfirmDiscardComplete()
            is QuickTimerIntent.CancelDiscardComplete -> handleCancelDiscardComplete()

            // Details sheet
            is QuickTimerIntent.ShowDetailsSheet -> showDetailsSheet()
            is QuickTimerIntent.HideDetailsSheet -> hideDetailsSheet()
            is QuickTimerIntent.NextDetailsStep -> nextDetailsStep()
            is QuickTimerIntent.PreviousDetailsStep -> previousDetailsStep()

            // Tea selection
            is QuickTimerIntent.TeaSearchQueryChanged -> updateTeaSearchQuery(intent.query)
            is QuickTimerIntent.TeaSelected -> selectTea(intent.teaId)

            // Vessel selection
            is QuickTimerIntent.VesselSelected -> selectVessel(intent.vesselId)

            // Brewing parameters
            is QuickTimerIntent.TeaQuantityChanged -> _state.update { it.copy(teaQuantityGrams = intent.grams) }
            is QuickTimerIntent.TeaBagModeChanged -> _state.update {
                it.copy(
                    isTeaBag = intent.isTeaBag,
                    teaQuantityGrams = if (intent.isTeaBag) null else it.teaQuantityGrams,
                )
            }
            is QuickTimerIntent.TemperatureChanged -> _state.update { it.copy(temperatureCelsius = intent.celsius) }
            is QuickTimerIntent.WaterQuantityChanged -> _state.update { it.copy(waterQuantityMl = intent.milliliters) }
            is QuickTimerIntent.WaterTypeSelected -> updateWaterType(intent.waterType)

            // Rating and notes
            is QuickTimerIntent.RatingChanged -> updateRating(intent.rating)
            is QuickTimerIntent.NotesChanged -> updateNotes(intent.notes)
        }
    }

    private fun initialize(durationSeconds: Int) {
        val duration = durationSeconds.seconds
        _state.update {
            it.copy(
                totalDuration = duration,
                remainingDuration = duration,
                status = TimerStatus.NOT_STARTED,
            )
        }
        // Auto-start timer immediately
        startTimer()
    }

    private fun startTimer() {
        startedAt = Clock.System.now()
        _state.update {
            it.copy(
                status = TimerStatus.RUNNING,
                originalDuration = it.originalDuration ?: it.totalDuration,
            )
        }
        val current = _state.value
        notificationService.scheduleCompletionAlarm(buildTimerState(current))
        startCountdown()
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.update {
            it.copy(status = TimerStatus.PAUSED)
        }
        notificationService.cancelCompletionAlarm()
        notificationService.showTimerPaused(buildTimerState(_state.value))
    }

    private fun resumeTimer() {
        // Recalculate startedAt based on remaining duration
        val remaining = _state.value.remainingDuration
        val total = _state.value.totalDuration
        val elapsed = total - remaining
        startedAt = Clock.System.now() - elapsed

        _state.update {
            it.copy(status = TimerStatus.RUNNING)
        }
        val current = _state.value
        notificationService.scheduleCompletionAlarm(buildTimerState(current))
        startCountdown()
    }

    private fun adjustTime(adjustment: Duration) {
        val current = _state.value
        val newTotal = (current.totalDuration + adjustment).coerceAtLeast(1.seconds)
        val newRemaining = (current.remainingDuration + adjustment).coerceAtLeast(Duration.ZERO)

        // Recalculate startedAt if timer is running
        if (current.status == TimerStatus.RUNNING && startedAt != null) {
            val elapsed = newTotal - newRemaining
            startedAt = Clock.System.now() - elapsed
        }

        _state.update {
            it.copy(
                totalDuration = newTotal,
                remainingDuration = newRemaining,
            )
        }

        // Reschedule completion alarm so notification fires at the correct (adjusted) time
        if (current.status == TimerStatus.RUNNING) {
            notificationService.scheduleCompletionAlarm(buildTimerState(_state.value))
        }
    }

    private fun showResetConfirmation() {
        _state.update { it.copy(showResetConfirmation = true) }
    }

    private fun cancelReset() {
        _state.update { it.copy(showResetConfirmation = false) }
    }

    private fun confirmReset() {
        val sessionToDelete = _state.value.inProgressSessionId
        timerJob?.cancel()
        timerJob = null
        startedAt = null
        notificationService.onTimerStopped()
        val resetTo = _state.value.originalDuration ?: _state.value.totalDuration
        _state.update {
            it.copy(
                totalDuration = resetTo,
                remainingDuration = resetTo,
                status = TimerStatus.NOT_STARTED,
                originalDuration = null,
                showResetConfirmation = false,
                inProgressSessionId = null,
            )
        }
        if (sessionToDelete != null) {
            viewModelScope.launch { deleteSessionUseCase(sessionToDelete) }
        }
    }

    private fun showStopConfirmation() {
        _state.update { it.copy(showStopConfirmation = true) }
    }

    private fun cancelStop() {
        _state.update { it.copy(showStopConfirmation = false) }
    }

    private fun confirmStop() {
        timerJob?.cancel()
        timerJob = null
        startedAt = null
        notificationService.onTimerStopped()
        _state.update { it.copy(showStopConfirmation = false) }
        _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
    }

    private fun handleBackClicked() {
        val current = _state.value
        when {
            current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED -> {
                showStopConfirmation()
            }
            current.status == TimerStatus.COMPLETE && current.inProgressSessionId != null -> {
                // Navigate to SteepComplete so the user can rate/finish
                _navigationEvents.trySend(
                    QuickTimerNavEvent.NavigateToSteepComplete(current.inProgressSessionId),
                )
            }
            current.status == TimerStatus.COMPLETE && current.inProgressSessionId == null -> {
                // Timer finished but session not saved — warn before discarding
                _state.update { it.copy(showDiscardCompleteConfirmation = true) }
            }
            else -> {
                notificationService.onTimerStopped()
                _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
            }
        }
    }

    private fun startCountdown() {
        var lastNotificationSecond = -1L
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100) // Update every 100ms for smooth UI

                val current = _state.value
                if (current.status != TimerStatus.RUNNING || startedAt == null) {
                    break
                }

                // Calculate remaining time
                val now = Clock.System.now()
                val elapsed = now - startedAt!!
                val remaining = (current.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

                _state.update { it.copy(remainingDuration = remaining) }

                // Update live activity once per second
                val currentSecond = elapsed.inWholeSeconds
                if (currentSecond > lastNotificationSecond) {
                    notificationService.showTimerRunning(buildTimerState(current.copy(remainingDuration = remaining)))
                    lastNotificationSecond = currentSecond
                }

                // Check if timer completed
                if (remaining == Duration.ZERO) {
                    _state.update {
                        it.copy(
                            status = TimerStatus.COMPLETE,
                            remainingDuration = Duration.ZERO,
                        )
                    }
                    notificationService.showTimerComplete(buildTimerState(_state.value))
                    // Auto-save as IN_PROGRESS if we have the required details
                    if (_state.value.hasRequiredDetails) {
                        autoSaveCompleted()
                    } else if (!_state.value.showDetailsSheet) {
                        _state.update { it.copy(showCompletionPrompt = true) }
                    }
                    break
                }
            }
        }
    }

    private fun buildTimerState(state: QuickTimerState): TimerState {
        return TimerState(
            sessionId = state.inProgressSessionId,
            teaName = effectiveTeaName(),
            steepNumber = 1,
            totalDuration = state.totalDuration,
            remainingDuration = state.remainingDuration,
            status = state.status,
            startedAt = startedAt,
        )
    }

    // Details sheet methods
    private fun showDetailsSheet() {
        _state.update {
            it.copy(
                showDetailsSheet = true,
                detailsSheetStep = 1,
            )
        }
    }

    private fun hideDetailsSheet() {
        _state.update {
            it.copy(
                showDetailsSheet = false,
                teaSearchQuery = "",
            )
        }
        // If the timer already completed, auto-save or re-prompt based on details
        val current = _state.value
        if (current.status == TimerStatus.COMPLETE && current.inProgressSessionId == null) {
            if (current.hasRequiredDetails) {
                autoSaveCompleted()
            } else {
                _state.update { it.copy(showCompletionPrompt = true) }
            }
        }
    }

    private fun nextDetailsStep() {
        val current = _state.value
        // Only advance if tea and vessel are selected
        if (current.selectedTea != null && current.selectedVessel != null) {
            _state.update { it.copy(detailsSheetStep = 2) }
        }
    }

    private fun previousDetailsStep() {
        _state.update { it.copy(detailsSheetStep = 1) }
    }

    private fun updateTeaSearchQuery(query: String) {
        _state.update { it.copy(teaSearchQuery = query) }
    }

    private fun selectTea(teaId: String) {
        val tea = _state.value.availableTeas.find { it.id == teaId } ?: return
        _state.update {
            it.copy(
                selectedTea = tea,
                teaSearchQuery = "",
            )
        }
        // Trigger prefill if vessel is also selected
        triggerPrefillIfReady()
    }

    private fun selectVessel(vesselId: String) {
        val vessel = _state.value.availableVessels.find { it.id == vesselId } ?: return
        _state.update { it.copy(selectedVessel = vessel) }
        // Trigger prefill if tea is also selected
        triggerPrefillIfReady()
    }

    private fun triggerPrefillIfReady() {
        val current = _state.value
        val tea = current.selectedTea ?: return
        val vessel = current.selectedVessel ?: return

        viewModelScope.launch {
            val prefill = getBrewingParametersPrefillUseCase(tea, vessel)

            _state.update {
                it.copy(
                    teaQuantityGrams = prefill.teaQuantityGrams?.toDouble(),
                    isTeaBag = prefill.source.isCompleteMethod && prefill.teaQuantityGrams == null,
                    temperatureCelsius = prefill.temperatureCelsius,
                    // Fall back to vessel capacity when no saved config provides water quantity
                    waterQuantityMl = prefill.waterQuantityMl ?: vessel.capacityMl?.toDouble(),
                    waterType = prefill.waterType ?: current.userPreferences.defaultWaterType,
                    prefillSource = prefill.source,
                )
            }
        }
    }

    private fun updateWaterType(waterType: WaterType) {
        _state.update { it.copy(waterType = waterType) }
    }

    private fun updateRating(rating: Float?) {
        _state.update { it.copy(rating = rating) }
    }

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    private fun effectiveTeaName(): String =
        _state.value.selectedTea?.name?.takeIf { it.isNotBlank() } ?: "Your tea"

    private fun handleAddSessionDetails() {
        _state.update { it.copy(showCompletionPrompt = false) }
        showDetailsSheet()
    }

    private fun handleDiscardCompletedSession() {
        _state.update { it.copy(showCompletionPrompt = false) }
        notificationService.onTimerStopped()
        _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
    }

    private fun handleConfirmDiscardComplete() {
        _state.update { it.copy(showDiscardCompleteConfirmation = false) }
        notificationService.onTimerStopped()
        _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
    }

    private fun handleCancelDiscardComplete() {
        _state.update { it.copy(showDiscardCompleteConfirmation = false) }
    }

    private fun autoSaveCompleted() {
        val current = _state.value
        if (current.inProgressSessionId != null) return
        if (!current.hasRequiredDetails) return

        val tea = current.selectedTea ?: return
        val vessel = current.selectedVessel ?: return
        val temperatureCelsius = current.temperatureCelsius ?: return
        val waterQuantityMl = current.waterQuantityMl ?: return

        viewModelScope.launch {
            try {
                val teaQuantityGrams = if (current.isTeaBag) null else current.teaQuantityGrams?.toFloat()

                val brewingTime = current.originalDuration ?: current.totalDuration
                startSessionUseCase(
                    teaId = tea.id,
                    teaQuantityGrams = teaQuantityGrams,
                    vesselId = vessel.id,
                    waterType = current.waterType,
                    brewingTime = brewingTime,
                    temperatureCelsius = temperatureCelsius,
                    waterQuantityMl = waterQuantityMl,
                ).onSuccess { started ->
                    val sessionId = started.session.id
                    // The steep has already been timed here, so the session waits for review
                    saveTimerStateUseCase(
                        TimerState(
                            sessionId = sessionId,
                            teaId = tea.id,
                            totalDuration = current.totalDuration,
                            status = TimerStatus.COMPLETE,
                        ),
                    )
                    _state.update { it.copy(inProgressSessionId = sessionId) }
                    _navigationEvents.trySend(QuickTimerNavEvent.NavigateToSteepComplete(sessionId))
                }
            } catch (_: Exception) {
                // Silent failure — user can still save manually from the complete screen
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        notificationService.onTimerStopped()
    }
}

sealed interface QuickTimerNavEvent {
    data object NavigateBack : QuickTimerNavEvent
    data class NavigateToSteepComplete(val sessionId: String) : QuickTimerNavEvent
    data class NavigateToTimer(val sessionId: String) : QuickTimerNavEvent
}
