package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.screens.collection.validateQuantity
import dev.jketterer.leaflog.presentation.ui.screens.collection.validateTemperature
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_save_photo
import leaf_log.shared.generated.resources.couldnt_save_session
import leaf_log.shared.generated.resources.error_tea_quantity_required
import leaf_log.shared.generated.resources.error_temperature_required
import leaf_log.shared.generated.resources.error_water_quantity_required
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Duration
import kotlin.uuid.Uuid

class EditSessionViewModel(
    private val route: NavRoute.EditSessionRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val editSessionUseCase: EditSessionUseCase,
    private val editSteepUseCase: EditSteepUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) : ViewModel() {

    /** Tracks photos added during this edit session for cleanup on discard. */
    private val newlyAddedPhotos = mutableListOf<String>()

    private val _state = MutableStateFlow(EditSessionState())
    val state: StateFlow<EditSessionState> = _state.asStateFlow()

    private val _navEvents = Channel<EditSessionNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        onFailure = messages::failure,
        createSuccessNavEvent = { EditSessionNavEvent.NavigateBack },
        sendNavEvent = { _navEvents.trySend(it) },
    )

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        loadVessels()
        loadSession(route.sessionId, route.editFullSession)
    }

    fun onIntent(intent: EditSessionIntent) {
        when (intent) {
            is EditSessionIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is EditSessionIntent.TemperatureChanged -> updateTemperature(intent.celsius)
            is EditSessionIntent.VesselSelected -> selectVessel(intent.vessel)
            is EditSessionIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is EditSessionIntent.RinsedChanged -> _state.update { it.copy(rinsed = intent.rinsed) }
            is EditSessionIntent.WaterQuantityChanged -> updateWaterQuantity(intent.milliliters)
            is EditSessionIntent.TeaQuantityChanged -> updateTeaQuantity(intent.grams)
            is EditSessionIntent.TeaBagModeChanged -> _state.update {
                it.copy(
                    isTeaBag = intent.isTeaBag,
                    teaQuantityGrams = if (intent.isTeaBag) null else it.teaQuantityGrams,
                    teaQuantityError = null,
                )
            }

            is EditSessionIntent.LocationChanged -> updateLocation(intent.location)
            is EditSessionIntent.RatingChanged -> updateRating(intent.rating)
            is EditSessionIntent.NotesChanged -> updateNotes(intent.notes)
            is EditSessionIntent.PhotoSelected -> addPhoto(intent.imageBytes)
            is EditSessionIntent.PhotoRemoved -> removePhoto(intent.path)
            is EditSessionIntent.SaveClicked -> save()
            is EditSessionIntent.Discard -> discard()
            is EditSessionIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)

            is EditSessionIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()
        }
    }

    private fun loadVessels() {
        viewModelScope.launch {
            brewingVesselRepository.getActiveFlow()
                .catch { e ->
                    Logger.w(tag = "EditSession") { "Failed to load vessels: ${e.message}" }
                }
                .collect { vessels ->
                    // Include the session's current vessel even if archived
                    val sessionVesselId = _state.value.existingSession?.vesselId
                    val vesselList = if (sessionVesselId != null &&
                        vessels.none { it.id == sessionVesselId }
                    ) {
                        val currentVessel = brewingVesselRepository.getById(sessionVesselId)
                        if (currentVessel != null) listOf(currentVessel) + vessels else vessels
                    } else {
                        vessels
                    }
                    _state.update { it.copy(availableVessels = vesselList) }
                }
        }
    }

    private fun loadSession(sessionId: String, editFullSession: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val aggregate = teaSessionRepository.getSessionOfSteep(sessionId)

                if (aggregate != null) {
                    val session = aggregate.session
                    val steep = aggregate.steeps.first { it.id == sessionId }
                    // Load tea name for SaveConfigurationDialog
                    val tea = teaRepository.getById(session.teaId)
                    // Use editFullSession flag to determine whether to show parent fields
                    // When true: edit full session (from top app bar)
                    // When false: edit only steep parameters (from steep card)
                    val isParent = editFullSession

                    if (_state.value.availableVessels.isEmpty()) {
                        val vessels = brewingVesselRepository.getActive()
                        // Include the session's current vessel even if archived
                        val currentVessel = if (vessels.none { it.id == session.vesselId }) {
                            brewingVesselRepository.getById(session.vesselId)
                        } else {
                            null
                        }
                        val vesselList = if (currentVessel != null) {
                            listOf(currentVessel) + vessels
                        } else {
                            vessels
                        }
                        _state.update { it.copy(availableVessels = vesselList) }
                    }
                    // Find vessel
                    val vessel =
                        _state.value.availableVessels.firstOrNull { it.id == session.vesselId }

                    _state.update {
                        it.copy(
                            existingSession = session,
                            existingSteep = steep,
                            isParentSession = isParent,
                            steepNumber = steep.number,
                            teaName = tea?.name,
                            brewingTime = steep.brewingTime,
                            temperatureCelsius = steep.temperatureCelsius,
                            selectedVessel = vessel,
                            selectedWaterType = session.waterType,
                            rinsed = session.rinsed,
                            waterQuantityMl = steep.waterQuantityMl,
                            teaQuantityGrams = session.teaQuantityGrams?.toDouble(),
                            isTeaBag = session.teaQuantityGrams == null,
                            location = session.location ?: "",
                            rating = steep.rating ?: 0f,
                            notes = steep.notes ?: "",
                            photos = steep.photos,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_session)
            }
        }
    }

    private fun updateBrewingTime(duration: Duration) {
        val error = if (duration <= Duration.ZERO) {
            "Brewing time must be greater than 0"
        } else null

        _state.update {
            it.copy(
                brewingTime = duration,
                brewingTimeError = error
            )
        }
    }

    private fun updateTemperature(celsius: Double?) {
        _state.update {
            it.copy(
                temperatureCelsius = celsius,
                temperatureError = if (celsius == null) {
                    Res.string.error_temperature_required
                } else {
                    validateTemperature(celsius)
                },
            )
        }
    }

    private fun selectVessel(vessel: BrewingVessel) {
        _state.update {
            it.copy(
                selectedVessel = vessel,
                vesselError = null
            )
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update { it.copy(selectedWaterType = waterType) }
    }

    private fun updateWaterQuantity(milliliters: Double?) {
        _state.update {
            it.copy(
                waterQuantityMl = milliliters,
                waterQuantityError = validateQuantity(
                    milliliters,
                    Res.string.error_water_quantity_required
                ),
            )
        }
    }

    private fun updateTeaQuantity(grams: Double?) {
        _state.update {
            it.copy(
                teaQuantityGrams = grams,
                teaQuantityError = validateQuantity(grams, Res.string.error_tea_quantity_required),
            )
        }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location) }
    }

    private fun updateRating(rating: Float) {
        val error = if (rating !in 0f..5f) {
            "Rating must be between 0 and 5"
        } else null

        _state.update {
            it.copy(
                rating = rating,
                ratingError = error
            )
        }
    }

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    private fun addPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val persistedPath = imageStorage.saveImage(imageBytes, fileName, "session_images")
                newlyAddedPhotos.add(persistedPath)
                _state.update {
                    it.copy(photos = it.photos + persistedPath)
                }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_save_photo)
            }
        }
    }

    private fun removePhoto(path: String) {
        viewModelScope.launch {
            // Only delete the file if it was newly added (not an existing photo)
            if (path in newlyAddedPhotos) {
                try {
                    imageStorage.deleteImage(path)
                } catch (_: Exception) {
                }
                newlyAddedPhotos.remove(path)
            }
            _state.update {
                it.copy(photos = it.photos - path)
            }
        }
    }

    private fun save() {
        val currentState = _state.value

        if (!currentState.isValid) {
            return
        }

        val session = currentState.existingSession ?: return
        val steep = currentState.existingSteep ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = runCatching {
                if (currentState.isParentSession) {
                    val waterQuantityMl = currentState.waterQuantityMl
                    if (waterQuantityMl != null && waterQuantityMl != steep.waterQuantityMl) {
                        editSteepUseCase(steep.id) { it.copy(waterQuantityMl = waterQuantityMl) }
                            .getOrThrow()
                    }
                    editSessionUseCase(session.id) {
                        it.copy(
                            vesselId = currentState.selectedVessel?.id ?: it.vesselId,
                            waterType = currentState.selectedWaterType,
                            rinsed = currentState.rinsed,
                            teaQuantityGrams = if (currentState.isTeaBag) null
                            else currentState.teaQuantityGrams?.toFloat(),
                            location = currentState.location,
                        )
                    }.getOrThrow()
                } else {
                    editSteepUseCase(steep.id) {
                        it.copy(
                            brewingTime = currentState.brewingTime ?: it.brewingTime,
                            temperatureCelsius = currentState.temperatureCelsius
                                ?: it.temperatureCelsius,
                            rating = currentState.rating.takeIf { r -> r > 0f },
                            notes = currentState.notes,
                            photos = currentState.photos,
                        )
                    }.getOrThrow()
                }
            }

            result
                .onSuccess { savedSession ->
                    // Clean up photo files that were removed from the original steep
                    val removedPhotos = steep.photos - currentState.photos.toSet()
                    for (photo in removedPhotos) {
                        try {
                            imageStorage.deleteImage(photo)
                        } catch (_: Exception) {
                        }
                    }
                    newlyAddedPhotos.clear()

                    _state.update { it.copy(isSaving = false, savedSession = savedSession) }

                    // Offer to save a method after a steep is rated 5
                    if (!currentState.isParentSession && currentState.rating >= 5f) {
                        maybeShowSaveConfigDialog(savedSession)
                    } else {
                        _navEvents.send(EditSessionNavEvent.NavigateBack)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_session)
                }
        }
    }

    private suspend fun maybeShowSaveConfigDialog(
        session: SessionWithSteeps,
    ) {
        if (checkDuplicateConfigurationUseCase(session)) {
            _navEvents.send(EditSessionNavEvent.NavigateBack)
            return
        }
        val first = session.steeps.first()
        val label = generateConfigurationLabelUseCase(
            teaQuantityGrams = session.session.teaQuantityGrams,
            waterQuantityMl = first.waterQuantityMl,
            brewingTime = first.brewingTime,
        )
        _state.update {
            it.copy(
                showSaveConfigurationDialog = true,
                suggestedConfigurationLabel = label,
            )
        }
    }

    private fun discard() {
        // Clean up any newly added photos that won't be saved
        viewModelScope.launch {
            for (photo in newlyAddedPhotos) {
                try {
                    imageStorage.deleteImage(photo)
                } catch (_: Exception) {
                }
            }
            newlyAddedPhotos.clear()
        }
        _navEvents.trySend(EditSessionNavEvent.NavigateBack)
    }
}

sealed interface EditSessionNavEvent {
    data object NavigateBack : EditSessionNavEvent
}
