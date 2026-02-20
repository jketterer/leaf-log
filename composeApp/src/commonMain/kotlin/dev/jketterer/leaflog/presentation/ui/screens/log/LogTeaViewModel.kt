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
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val brewingVesselRepository: BrewingVesselRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val searchTeasUseCase: SearchTeasUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LogTeaState())
    val state: StateFlow<LogTeaState> = _state.asStateFlow()

    private val _navEvents = Channel<LogTeaNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

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
            is LogTeaIntent.TeaQuantityChanged -> updateTeaQuantity(intent.quantity)
            is LogTeaIntent.WaterQuantityChanged -> updateWaterQuantity(intent.quantity)
            is LogTeaIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is LogTeaIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is LogTeaIntent.VesselSelected -> selectVessel(intent.vesselId)
            is LogTeaIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is LogTeaIntent.LocationChanged -> updateLocation(intent.location)
            is LogTeaIntent.NotesChanged -> updateNotes(intent.notes)
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

            is LogTeaIntent.StartTimerClicked -> saveSession(
                status = SessionStatus.IN_PROGRESS,
                startTimer = true
            )

            is LogTeaIntent.BackClicked -> _navEvents.trySend(LogTeaNavigationEvent.NavigateBack)

            is LogTeaIntent.ChooseDifferentMethodClicked -> showChooseMethodDialog()
            is LogTeaIntent.MethodSelected -> selectMethod(intent.configurationId)
            is LogTeaIntent.DismissChooseMethodDialog -> dismissChooseMethodDialog()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            combine(
                preferencesRepository.getPreferencesFlow(),
                teaRepository.getAllFlow(),
                brewingVesselRepository.getAllFlow(),
            ) { prefs, teas, vessels -> Triple(prefs, teas, vessels) }
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load data: ${e.message}",
                        )
                    }
                }
                .collect { (prefs, teas, vessels) ->
                    _state.update {
                        it.copy(
                            userPreferences = prefs,
                            availableTeas = teas,
                            availableVessels = vessels,
                            isLoading = false,
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
                println("Search failed: ${e.message}")
            }
        }
    }

    private fun selectTea(teaId: String?) = viewModelScope.launch {
        if (teaId == null) {
            _state.update {
                it.copy(
                    selectedTea = null,
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
        _state.update {
            it.copy(
                selectedTea = tea,
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
            // Always load available configurations for this tea + vessel
            val configurations = brewingConfigurationRepository.getByTeaAndVessel(tea.id, vessel.id)

            // Only prefill if user hasn't manually edited brewing parameters yet
            if (!_state.value.hasEditedBrewingParameters) {
                // Use the smart pre-fill use case
                val prefill = getBrewingParametersPrefillUseCase(tea, vessel)

                // Prefill values are already in storage units (Celsius/mL), store them directly
                _state.update { state ->
                    state.copy(
                        teaQuantityGrams = prefill.teaQuantityGrams?.toString() ?: "",
                        waterQuantityMl = prefill.waterQuantityMl?.toString() ?: "",
                        waterQuantityDisplay = "", // Clear so screen derives from storage via conversion
                        temperatureCelsius = prefill.temperatureCelsius?.toString() ?: "",
                        temperatureDisplay = "", // Clear so screen derives from storage via conversion
                        brewingTime = prefill.brewingTime,
                        selectedWaterType = prefill.waterType ?: state.selectedWaterType,
                        prefillSource = prefill.source,
                        availableConfigurations = configurations,
                        usedConfigurationId = configurations.firstOrNull()?.id,
                        hasEditedBrewingParameters = false, // Reset flag when prefilling
                    )
                }
            } else {
                // User has edited parameters, just update configurations without prefilling
                _state.update { currentState ->
                    currentState.copy(
                        availableConfigurations = configurations,
                        // Clear prefill source since we're not using it anymore
                        prefillSource = PrefillSource.None,
                        usedConfigurationId = null,
                    )
                }
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
                    teaQuantityGrams = "",
                    waterQuantityMl = "",
                    waterQuantityDisplay = "",
                    temperatureCelsius = "",
                    temperatureDisplay = "",
                    brewingTime = null,
                    usedConfigurationId = null,
                    prefillSource = PrefillSource.None,
                    hasEditedBrewingParameters = true, // Mark as edited since user is manually entering
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
                            teaQuantityGrams = config.teaQuantityGrams?.toString() ?: "",
                            waterQuantityMl = config.waterQuantityMl.toString(),
                            waterQuantityDisplay = "", // Clear so screen derives from storage via conversion
                            temperatureCelsius = config.temperatureCelsius.toString(),
                            temperatureDisplay = "", // Clear so screen derives from storage via conversion
                            brewingTime = config.brewingTime,
                            selectedWaterType = config.waterType,
                            usedConfigurationId = configurationId,
                            hasEditedBrewingParameters = false, // Reset flag - user chose a config
                            // Restore prefill source so banner shows which config is being used
                            prefillSource = PrefillSource.DirectSession(
                                sessionId = config.sourceSessionId,
                                rating = config.rating,
                                timestamp = config.lastUsedAt ?: config.createdAt
                            )
                        )
                    }

                    // Increment usage counter
                    brewingConfigurationRepository.incrementTimesUsed(
                        configurationId,
                        Clock.System.now()
                    )
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

    private fun updateTeaQuantity(quantity: String) {
        _state.update {
            it.copy(
                teaQuantityGrams = quantity,
                hasUnsavedChanges = true,
                hasEditedBrewingParameters = true,
                // Clear prefill source since user is manually editing
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
                hasEditedBrewingParameters = true,
                // Clear prefill source since user is manually editing
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
                hasEditedBrewingParameters = true,
                // Clear prefill source since user is manually editing
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
                hasEditedBrewingParameters = true,
                // Clear prefill source since user is manually editing
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun selectVessel(vesselId: String?) = viewModelScope.launch {
        if (vesselId == null) return@launch

        state.first { !it.isLoading }

        val vessel = _state.value.availableVessels.firstOrNull { it.id == vesselId }
        println("selected vessel: ${vessel?.name}")
        _state.update { currentState ->
            currentState.copy(
                selectedVessel = vessel,
                vesselError = null, // Clear error when vessel is selected
                hasUnsavedChanges = true
            )
        }

        if (vessel == null) return@launch

        // Load configurations and optionally pre-fill if tea is also selected
        val tea = _state.value.selectedTea
        if (tea != null) {
            loadConfigurationsAndPrefill(tea, vessel)
        } else {
            // If no tea selected, just auto-populate water quantity from vessel capacity
            autoPopulateWaterQuantity(vessel)
        }
    }

    private fun autoPopulateWaterQuantity(vessel: BrewingVessel) {
        _state.update { currentState ->
            val shouldAutoPopulate =
                vessel.capacityMl != null && currentState.waterQuantityMl.isBlank()
            val newWaterQuantity = if (shouldAutoPopulate) {
                vessel.capacityMl.toString()
            } else {
                currentState.waterQuantityMl
            }

            currentState.copy(
                waterQuantityMl = newWaterQuantity,
                waterQuantityDisplay = "", // Clear so screen derives from storage via conversion
                waterQuantityError = if (shouldAutoPopulate) null else currentState.waterQuantityError,
            )
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update {
            it.copy(
                selectedWaterType = waterType,
                hasUnsavedChanges = true,
                hasEditedBrewingParameters = true,
                // Clear prefill source since user is manually editing
                prefillSource = PrefillSource.None,
                usedConfigurationId = null
            )
        }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location, hasUnsavedChanges = true) }
    }

    private fun updateNotes(notes: String) {
        _state.update {
            it.copy(
                notes = notes,
                hasUnsavedChanges = true
            )
        }
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
                completionDialogNotes = it.notes,
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
                teaQuantityGrams = currentState.teaQuantityGrams.toFloatOrNull(),
                vesselId = currentState.selectedVessel!!.id,
                waterType = currentState.selectedWaterType,
                location = currentState.location.takeIf { it.isNotBlank() },
                brewingTime = currentState.brewingTime!!,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                notes = (overrideNotes ?: currentState.notes).takeIf { it.isNotBlank() },
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
        println("clearing LogTeaViewModel")
        super.onCleared()
    }
}

sealed interface LogTeaNavigationEvent {
    data object NavigateBack : LogTeaNavigationEvent
    data class NavigateToTimer(val sessionId: String) : LogTeaNavigationEvent
}