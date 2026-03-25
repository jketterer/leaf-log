package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration

class LogTeaViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val searchTeasUseCase: SearchTeasUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LogTeaState())
    val state: StateFlow<LogTeaState> = _state.asStateFlow()

    private val _navEvents = Channel<LogTeaNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private sealed interface PendingAction {
        data object StartTimer : PendingAction
        data class SaveAsCompleted(val notes: String?, val rating: Float?) : PendingAction
    }

    private val inProgressDelegate = createInProgressSessionDelegate<LogTeaState, PendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        completeSessionUseCase = completeSessionUseCase,
        deleteSessionUseCase = deleteSessionUseCase,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        setError = { state, error -> state.copy(error = error) },
        onResume = { session ->
            _navEvents.trySend(LogTeaNavigationEvent.NavigateToTimer(session.id))
        },
        executePendingAction = { action ->
            when (action) {
                is PendingAction.StartTimer -> saveSession(
                    status = SessionStatus.IN_PROGRESS,
                    startTimer = true,
                )
                is PendingAction.SaveAsCompleted -> saveSession(
                    status = SessionStatus.COMPLETED,
                    startTimer = false,
                    overrideNotes = action.notes,
                    rating = action.rating,
                )
            }
        },
    )

    init {
        onIntent(LogTeaIntent.LoadData)
    }

    fun onIntent(intent: LogTeaIntent) {
        when (intent) {
            is LogTeaIntent.LoadData -> loadData()
            is LogTeaIntent.ShowTeaSearchDialog -> showTeaSearchDialog()
            is LogTeaIntent.HideTeaSearchDialog -> hideTeaSearchDialog()
            is LogTeaIntent.TeaSearchQueryChanged -> updateTeaSearchQuery(intent.query)
            is LogTeaIntent.TeaSelected -> selectTea(intent.teaId)
            is LogTeaIntent.QuickAddTeaClicked -> showQuickAddTeaDialog()
            is LogTeaIntent.QuickAddTeaSaved -> handleQuickAddTeaSaved(intent.tea)
            is LogTeaIntent.TeaBagModeChanged -> updateTeaBagMode(intent.isTeaBag)
            is LogTeaIntent.TeaQuantityChanged -> updateTeaQuantity(intent.quantity)
            is LogTeaIntent.WaterQuantityChanged -> updateWaterQuantity(intent.quantity)
            is LogTeaIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is LogTeaIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is LogTeaIntent.VesselSelected -> selectVessel(intent.vesselId)
            is LogTeaIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is LogTeaIntent.LocationChanged -> updateLocation(intent.location)
            is LogTeaIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is LogTeaIntent.ToggleVolumeUnit -> toggleVolumeUnit()
            is LogTeaIntent.AddPhotoClicked -> {
                // Photo picker handled by UI
            }

            is LogTeaIntent.PhotoSelected -> addPhoto(intent.photoUri)
            is LogTeaIntent.PhotoRemoved -> removePhoto(intent.photoUri)
            is LogTeaIntent.SaveAsCompleted -> showCompleteSessionDialog()
            is LogTeaIntent.CompletionDialogNotesChanged -> _state.update {
                it.copy(completionDialogNotes = intent.notes)
            }

            is LogTeaIntent.CompletionRatingChanged -> _state.update {
                it.copy(completionRating = intent.rating)
            }

            is LogTeaIntent.ConfirmCompleteSession -> {
                val current = _state.value
                saveSession(
                    status = SessionStatus.COMPLETED,
                    startTimer = false,
                    overrideNotes = current.completionDialogNotes.takeIf { it.isNotBlank() },
                    rating = current.completionRating.takeIf { it > 0f },
                )
            }

            is LogTeaIntent.DismissCompleteSessionDialog -> _state.update {
                it.copy(showCompleteSessionDialog = false)
            }

            is LogTeaIntent.StartTimerClicked -> inProgressDelegate.checkAndProceed(PendingAction.StartTimer)

            is LogTeaIntent.BackClicked -> _navEvents.trySend(LogTeaNavigationEvent.NavigateBack)
            is LogTeaIntent.ClearError -> _state.update { it.copy(error = null) }

            is LogTeaIntent.ChooseDifferentMethodClicked -> showChooseMethodDialog()
            is LogTeaIntent.MethodSelected -> selectMethod(intent.configurationId)
            is LogTeaIntent.DismissChooseMethodDialog -> dismissChooseMethodDialog()

            // In-progress session conflict dialog
            is LogTeaIntent.ResumeInProgress -> inProgressDelegate.resume()
            is LogTeaIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is LogTeaIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is LogTeaIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            combine(
                preferencesRepository.getPreferencesFlow(),
                teaRepository.getAllFlow(),
                brewingVesselRepository.getActiveFlow(),
                teaRepository.getRecentlyBrewedFlow(5),
            ) { prefs, teas, vessels, recentTeas ->
                object {
                    val prefs = prefs
                    val teas = teas
                    val vessels = vessels
                    val recentTeas = recentTeas
                }
            }
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load data: ${e.message}",
                        )
                    }
                }
                .collect { data ->
                    _state.update {
                        it.copy(
                            userPreferences = data.prefs,
                            availableTeas = data.teas,
                            suggestedTeas = data.recentTeas,
                            availableVessels = data.vessels,
                            isLoading = false,
                            selectedWaterType = data.prefs.defaultWaterType,
                        )
                    }
                }
        }
    }

    private fun showTeaSearchDialog() {
        _state.update { it.copy(showTeaSearchDialog = true) }
    }

    private fun hideTeaSearchDialog() {
        _state.update {
            it.copy(
                showTeaSearchDialog = false,
                teaSearchQuery = ""
            )
        }
    }

    private fun updateTeaSearchQuery(query: String) {
        _state.update { it.copy(teaSearchQuery = query) }

        if (query.isBlank()) {
            loadData()
            return
        }

        viewModelScope.launch {
            try {
                // Use the use case - it has business logic (trim, validation)
                val results = searchTeasUseCase(query)
                _state.update { it.copy(availableTeas = results) }
            } catch (e: Exception) {
                Logger.w("LogTea") { "Search failed: ${e.message}" }
            }
        }
    }

    private fun selectTea(teaId: String?) = viewModelScope.launch {
        if (teaId == null) {
            _state.update {
                it.copy(
                    selectedTea = null,
                    selectedTeaType = null,
                    teaError = null,
                    showTeaSearchDialog = false,
                    teaSearchQuery = "",
                    hasUnsavedChanges = true,
                )
            }
            return@launch
        }

        // Wait for initial data load to complete before attempting to find tea
        state.first { !it.isLoading }

        val tea = _state.value.availableTeas.firstOrNull { it.id == teaId }
        val teaType = tea?.let { teaTypeRepository.getById(it.teaTypeId) }
        _state.update {
            it.copy(
                selectedTea = tea,
                selectedTeaType = teaType,
                teaError = null,
                showTeaSearchDialog = false,
                teaSearchQuery = "",
                hasUnsavedChanges = true,
            )
        }

        // Load configurations and optionally pre-fill if vessel is also selected
        if (tea != null) {
            val vessel = _state.value.selectedVessel
            if (vessel != null) {
                loadConfigurationsAndPrefill(tea, vessel)
            }
        }
    }

    private fun loadConfigurationsAndPrefill(tea: Tea, vessel: BrewingVessel) {
        viewModelScope.launch {
            val configurations = brewingConfigurationRepository.getByTeaAndVessel(tea.id, vessel.id)
            val prefill = getBrewingParametersPrefillUseCase(tea, vessel)
            _state.update { state ->
                state.copy(
                    isTeaBag = prefill.source != PrefillSource.None && prefill.teaQuantityGrams == null,
                    teaQuantityGrams = prefill.teaQuantityGrams?.toString() ?: "",
                    waterQuantityMl = prefill.waterQuantityMl?.toString()
                        ?: vessel.capacityMl?.toString() ?: "",
                    waterQuantityDisplay = "",
                    temperatureCelsius = prefill.temperatureCelsius?.toString() ?: "",
                    temperatureDisplay = "",
                    brewingTime = prefill.brewingTime,
                    selectedWaterType = prefill.waterType ?: state.selectedWaterType,
                    prefillSource = prefill.source,
                    availableConfigurations = configurations,
                    usedConfigurationId = configurations.firstOrNull()?.id,
                )
            }
        }
    }

    private fun showChooseMethodDialog() {
        _state.update { it.copy(showChooseMethodDialog = true) }
    }

    private fun dismissChooseMethodDialog() {
        _state.update { it.copy(showChooseMethodDialog = false) }
    }

    private fun selectMethod(configurationId: String?) {
        _state.update { it.copy(showChooseMethodDialog = false) }

        if (configurationId == null) {
            // User chose "Custom" - clear pre-filled parameters
            _state.update {
                it.copy(
                    isTeaBag = false,
                    teaQuantityGrams = "",
                    waterQuantityMl = "",
                    waterQuantityDisplay = "",
                    temperatureCelsius = "",
                    temperatureDisplay = "",
                    brewingTime = null,
                    usedConfigurationId = null,
                    prefillSource = PrefillSource.None,
                )
            }
        } else {
            // Load and apply the selected configuration
            // This is an explicit user action to use a configuration, so reset the edit flag
            viewModelScope.launch {
                val config = brewingConfigurationRepository.getById(configurationId)
                if (config != null) {
                    _state.update { currentState ->
                        // Configuration values are already in storage units (Celsius/mL), store directly
                        currentState.copy(
                            isTeaBag = config.teaQuantityGrams == null,
                            teaQuantityGrams = config.teaQuantityGrams?.toString() ?: "",
                            waterQuantityMl = config.waterQuantityMl.toString(),
                            waterQuantityDisplay = "",
                            temperatureCelsius = config.temperatureCelsius.toString(),
                            temperatureDisplay = "",
                            brewingTime = config.brewingTime,
                            selectedWaterType = config.waterType,
                            usedConfigurationId = configurationId,
                            prefillSource = PrefillSource.SavedConfig,
                        )
                    }

                }
            }
        }
    }

    private fun showQuickAddTeaDialog() {
        _state.update { it.copy(showQuickAddTeaDialog = true) }
    }

    private fun handleQuickAddTeaSaved(tea: Tea) {
        _state.update { it.copy(showQuickAddTeaDialog = false) }
        selectTea(tea.id)
    }

    private fun updateTeaBagMode(isTeaBag: Boolean) {
        _state.update {
            it.copy(
                isTeaBag = isTeaBag,
                teaQuantityGrams = if (isTeaBag) "" else it.teaQuantityGrams,
                teaQuantityError = null,
                hasUnsavedChanges = true,
                prefillSource = if (isTeaBag) PrefillSource.None else it.prefillSource,
                usedConfigurationId = if (isTeaBag) null else it.usedConfigurationId,
            )
        }
    }

    private fun updateTeaQuantity(quantity: String) {
        val error = when {
            quantity.isBlank() -> "Tea quantity is required"
            quantity.toFloatOrNull() == null -> "Invalid quantity"
            quantity.toFloat() <= 0 -> "Quantity must be greater than 0"
            else -> null
        }
        _state.update {
            it.copy(
                teaQuantityGrams = quantity,
                teaQuantityError = error,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun updateWaterQuantity(quantity: String) {
        val currentState = _state.value
        val volumeUnit = currentState.userPreferences.volumeUnit

        // Convert from display unit to storage unit (mL)
        val storageValue = quantity.toIntOrNull()?.let { displayValue ->
            volumeUnit.toMilliliters(displayValue).toString()
        } ?: quantity

        val error = when {
            quantity.isBlank() -> "Water quantity is required"
            quantity.toIntOrNull() == null -> "Invalid quantity"
            storageValue.toIntOrNull()?.let { it <= 0 } == true -> "Quantity must be greater than 0"
            else -> null
        }

        _state.update {
            it.copy(
                waterQuantityMl = storageValue,
                waterQuantityDisplay = quantity,
                waterQuantityError = error,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun updateTemperature(temperature: String) {
        val currentState = _state.value
        val tempUnit = currentState.userPreferences.temperatureUnit

        // Convert from display unit to storage unit (Celsius)
        val storageValue = temperature.toIntOrNull()?.let { displayValue ->
            tempUnit.toCelsius(displayValue).toString()
        } ?: temperature

        // Validate in display unit for user-friendly error messages
        val error = when {
            temperature.isBlank() -> "Temperature is required"
            temperature.toIntOrNull() == null -> "Invalid temperature"
            else -> {
                val value = temperature.toInt()
                when (tempUnit) {
                    TemperatureUnit.CELSIUS -> {
                        if (value !in 0..100) "Temperature must be 0-100°C" else null
                    }

                    TemperatureUnit.FAHRENHEIT -> {
                        if (value !in 32..212) "Temperature must be 32-212°F" else null
                    }
                }
            }
        }

        _state.update {
            it.copy(
                temperatureCelsius = storageValue,
                temperatureDisplay = temperature,
                temperatureError = error,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun updateBrewingTime(duration: Duration) {
        val error = if (duration <= Duration.ZERO) {
            "Brewing time must be greater than 0"
        } else null

        _state.update {
            it.copy(
                brewingTime = duration,
                brewingTimeError = error,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun selectVessel(vesselId: String?) = viewModelScope.launch {
        if (vesselId == null) return@launch

        state.first { !it.isLoading }

        val vessel = _state.value.availableVessels.firstOrNull { it.id == vesselId }
        _state.update { currentState ->
            currentState.copy(
                selectedVessel = vessel,
                vesselError = null, // Clear error when vessel is selected
                hasUnsavedChanges = true
            )
        }

        if (vessel == null) return@launch

        val tea = _state.value.selectedTea
        if (tea != null) {
            loadConfigurationsAndPrefill(tea, vessel)
        } else if (vessel.capacityMl != null) {
            _state.update {
                it.copy(
                    waterQuantityMl = vessel.capacityMl.toString(),
                    waterQuantityDisplay = "",
                )
            }
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update {
            it.copy(
                selectedWaterType = waterType,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location, hasUnsavedChanges = true) }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(_state.value.userPreferences.temperatureUnit.toggle())
            _state.update { it.copy(temperatureDisplay = "") } // Clear so screen reconverts from storage
        }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(_state.value.userPreferences.volumeUnit.toggle())
            _state.update { it.copy(waterQuantityDisplay = "") } // Clear so screen reconverts from storage
        }
    }

    private fun addPhoto(photoUri: String) {
        _state.update {
            it.copy(
                photos = it.photos + photoUri,
                hasUnsavedChanges = true
            )
        }
    }

    private fun removePhoto(photoUri: String) {
        _state.update {
            it.copy(
                photos = it.photos - photoUri,
                hasUnsavedChanges = true
            )
        }
    }

    private fun showCompleteSessionDialog() {
        _state.update {
            it.copy(
                showCompleteSessionDialog = true,
                completionDialogNotes = "",
                completionRating = 0f,
            )
        }
    }

    private fun saveSession(
        status: SessionStatus,
        startTimer: Boolean,
        overrideNotes: String? = null,
        rating: Float? = null,
    ) {
        val currentState = _state.value

        if (!currentState.isValid) {
            _state.update {
                it.copy(
                    teaError = if (it.selectedTea == null) "Tea is required" else null,
                    vesselError = if (it.selectedVessel == null) "Brewing vessel is required" else null,
                    teaQuantityError = if (!it.isTeaBag && it.teaQuantityGrams.isBlank()) "Tea quantity is required" else it.teaQuantityError,
                    waterQuantityError = if (it.waterQuantityMl.toDoubleOrNull() == null) "Water quantity is required" else it.waterQuantityError,
                    temperatureError = if (it.temperatureCelsius.toDoubleOrNull() == null) "Temperature is required" else it.temperatureError,
                    brewingTimeError = if (it.brewingTime == null) "Brewing time is required" else it.brewingTimeError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            // Values are already in storage units (Celsius/mL), use directly
            val temperatureCelsius = currentState.temperatureCelsius.toDouble()
            val waterQuantityMl = currentState.waterQuantityMl.toDouble()

            // Use the use case - it has business logic (validation, ID generation, timestamps)
            createSessionUseCase(
                teaId = currentState.selectedTea!!.id,
                teaQuantityGrams = if (currentState.isTeaBag) null else currentState.teaQuantityGrams.toFloatOrNull(),
                vesselId = currentState.selectedVessel!!.id,
                waterType = currentState.selectedWaterType,
                location = currentState.location.takeIf { it.isNotBlank() },
                brewingTime = currentState.brewingTime!!,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                notes = overrideNotes?.takeIf { it.isNotBlank() },
                rating = rating,
                photos = currentState.photos,
                status = status,
                usedConfigurationId = currentState.usedConfigurationId,
            )
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            showCompleteSessionDialog = false,
                        )
                    }

                    if (startTimer) {
                        _navEvents.send(LogTeaNavigationEvent.NavigateToTimer(it.id))
                    } else {
                        _navEvents.send(LogTeaNavigationEvent.NavigateBack)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to save session: ${e.message}",
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

sealed interface LogTeaNavigationEvent {
    data object NavigateBack : LogTeaNavigationEvent
    data class NavigateToTimer(val sessionId: String) : LogTeaNavigationEvent
}