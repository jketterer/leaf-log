package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.viewmodel.ConfigurationSaveDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
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
    private val teaRepository: TeaRepository,
    private val vesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickTimerState())
    val state: StateFlow<QuickTimerState> = _state.asStateFlow()

    private val _navigationEvents = Channel<QuickTimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var timerJob: Job? = null
    private var startedAt: Instant? = null

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        setError = { state, error -> state.copy(error = error) },
        createSuccessNavEvent = { session -> QuickTimerNavEvent.NavigateToSession(session.id) },
        sendNavEvent = { _navigationEvents.trySend(it) },
    )

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        loadTeasAndVessels()
    }

    private fun loadTeasAndVessels() {
        viewModelScope.launch {
            teaRepository.getAllFlow()
                .catch { e -> println("Failed to load teas: ${e.message}") }
                .collect { teas ->
                    _state.update { it.copy(availableTeas = teas) }
                }
        }

        viewModelScope.launch {
            vesselRepository.getAllFlow()
                .catch { e -> println("Failed to load vessels: ${e.message}") }
                .collect { vessels ->
                    _state.update { it.copy(availableVessels = vessels) }
                }
        }
    }

    fun onIntent(intent: QuickTimerIntent) {
        when (intent) {
            is QuickTimerIntent.Initialize -> initialize(intent.durationSeconds)
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
            is QuickTimerIntent.TeaQuantityChanged -> updateTeaQuantity(intent.quantity)
            is QuickTimerIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is QuickTimerIntent.WaterQuantityChanged -> updateWaterQuantity(intent.quantity)
            is QuickTimerIntent.WaterTypeSelected -> updateWaterType(intent.waterType)

            // Rating and notes
            is QuickTimerIntent.RatingChanged -> updateRating(intent.rating)
            is QuickTimerIntent.NotesChanged -> updateNotes(intent.notes)

            // Completion
            is QuickTimerIntent.ShowCompletionDialog -> showCompletionDialog()
            is QuickTimerIntent.DismissCompletionDialog -> dismissCompletionDialog()
            is QuickTimerIntent.SaveSession -> saveSession()
            is QuickTimerIntent.DiscardSession -> discardSession()

            // Configuration saving
            is QuickTimerIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)
            is QuickTimerIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()
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
    }

    private fun startTimer() {
        startedAt = Clock.System.now()
        _state.update {
            it.copy(status = TimerStatus.RUNNING)
        }
        startCountdown()
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.update {
            it.copy(status = TimerStatus.PAUSED)
        }
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
    }

    private fun showResetConfirmation() {
        _state.update { it.copy(showResetConfirmation = true) }
    }

    private fun cancelReset() {
        _state.update { it.copy(showResetConfirmation = false) }
    }

    private fun confirmReset() {
        timerJob?.cancel()
        timerJob = null
        startedAt = null
        val total = _state.value.totalDuration
        _state.update {
            it.copy(
                remainingDuration = total,
                status = TimerStatus.NOT_STARTED,
                showResetConfirmation = false,
            )
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
        _state.update { it.copy(showStopConfirmation = false) }
        _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
    }

    private fun handleBackClicked() {
        val current = _state.value
        // If timer is running or paused, show stop confirmation
        if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED) {
            showStopConfirmation()
        } else {
            _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
        }
    }

    private fun startCountdown() {
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

                // Check if timer completed
                if (remaining == Duration.ZERO) {
                    _state.update {
                        it.copy(
                            status = TimerStatus.COMPLETE,
                            remainingDuration = Duration.ZERO,
                        )
                    }
                    // If no details, show completion dialog
                    if (!_state.value.hasRequiredDetails) {
                        _state.update { it.copy(showCompletionDialog = true) }
                    }
                    break
                }
            }
        }
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

            // Convert temperature from Celsius to user's preferred unit
            val temperatureUnit = current.userPreferences.temperatureUnit
            val temperatureInPreferredUnit = prefill.temperatureCelsius?.let {
                temperatureUnit.fromCelsius(it).toString()
            } ?: ""

            // Convert water quantity from ml to user's preferred unit
            val volumeUnit = current.userPreferences.volumeUnit
            val waterQuantityInPreferredUnit = prefill.waterQuantityMl?.let {
                volumeUnit.fromMilliliters(it).toString()
            } ?: ""

            // Tea quantity is stored in grams (no conversion needed)
            val teaQuantityGrams = prefill.teaQuantityGrams?.toString() ?: ""

            _state.update {
                it.copy(
                    teaQuantityGrams = teaQuantityGrams,
                    temperatureCelsius = temperatureInPreferredUnit,
                    waterQuantityMl = waterQuantityInPreferredUnit,
                    waterType = prefill.waterType ?: WaterType.FILTERED,
                    prefillSource = prefill.source,
                )
            }
        }
    }

    private fun updateTeaQuantity(quantity: String) {
        _state.update { it.copy(teaQuantityGrams = quantity) }
    }

    private fun updateTemperature(temperature: String) {
        _state.update { it.copy(temperatureCelsius = temperature) }
    }

    private fun updateWaterQuantity(quantity: String) {
        _state.update { it.copy(waterQuantityMl = quantity) }
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

    // Completion methods
    private fun showCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = true) }
    }

    private fun dismissCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = false) }
    }

    private fun discardSession() {
        timerJob?.cancel()
        timerJob = null
        startedAt = null
        _state.update { it.copy(showCompletionDialog = false) }
        _navigationEvents.trySend(QuickTimerNavEvent.NavigateBack)
    }

    private fun saveSession() {
        val current = _state.value

        // Validate required fields
        val tea = current.selectedTea ?: return
        val vessel = current.selectedVessel ?: return
        val temperatureStr = current.temperatureCelsius.takeIf { it.isNotBlank() } ?: return
        val waterQuantityStr = current.waterQuantityMl.takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Convert temperature from user's preferred unit back to Celsius
                val temperatureInUserUnit = temperatureStr.toIntOrNull() ?: return@launch
                val temperatureCelsius = current.userPreferences.temperatureUnit
                    .toCelsius(temperatureInUserUnit)

                // Convert water quantity from user's preferred unit back to ml
                val waterQuantityInUserUnit = waterQuantityStr.toIntOrNull() ?: return@launch
                val waterQuantityMl = current.userPreferences.volumeUnit
                    .toMilliliters(waterQuantityInUserUnit)

                // Parse tea quantity (already in grams)
                val teaQuantityGrams = current.teaQuantityGrams.toFloatOrNull()

                val result = createSessionUseCase(
                    teaId = tea.id,
                    teaQuantityGrams = teaQuantityGrams,
                    vesselId = vessel.id,
                    waterType = current.waterType,
                    brewingTime = current.totalDuration,
                    temperatureCelsius = temperatureCelsius,
                    waterQuantityMl = waterQuantityMl,
                    notes = current.notes.takeIf { it.isNotBlank() },
                    rating = current.rating,
                    status = SessionStatus.COMPLETED,
                )

                result.fold(
                    onSuccess = { session ->
                        _state.update { it.copy(isLoading = false, savedSession = session) }

                        // Check if we should show the save configuration dialog
                        // Only for sessions with rating >= 5
                        if (session.rating != null && session.rating >= 5f) {
                            _state.update { it.copy(showSaveConfigurationDialog = true) }
                        } else {
                            _navigationEvents.trySend(QuickTimerNavEvent.NavigateToSession(session.id))
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to save session",
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save session",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

sealed interface QuickTimerNavEvent {
    data object NavigateBack : QuickTimerNavEvent
    data class NavigateToSession(val sessionId: String) : QuickTimerNavEvent
}
