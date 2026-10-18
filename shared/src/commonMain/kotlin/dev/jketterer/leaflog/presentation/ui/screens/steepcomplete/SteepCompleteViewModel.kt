package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_discard_steep
import leaf_log.shared.generated.resources.couldnt_finish_session
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_remove_photo
import leaf_log.shared.generated.resources.couldnt_save_changes
import leaf_log.shared.generated.resources.couldnt_save_photo
import leaf_log.shared.generated.resources.couldnt_start_steep
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SteepCompleteViewModel(
    private val route: NavRoute.SteepCompleteRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val timerService: TimerService,
    private val addSteepUseCase: AddSteepUseCase,
    private val editSteepUseCase: EditSteepUseCase,
    private val editSessionUseCase: EditSessionUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SteepCompleteState())
    val state: StateFlow<SteepCompleteState> = _state.asStateFlow()

    private val _navigationEvents = Channel<SteepCompleteNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        onFailure = messages::failure,
        createSuccessNavEvent = { SteepCompleteNavEvent.NavigateToHome(it.session.id) },
        sendNavEvent = { _navigationEvents.trySend(it) },
    )

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        initialize(route.sessionId)
    }

    fun onIntent(intent: SteepCompleteIntent) {
        when (intent) {
            is SteepCompleteIntent.RatingChanged -> updateRating(intent.rating)
            is SteepCompleteIntent.NotesChanged -> updateNotes(intent.notes)
            is SteepCompleteIntent.PhotoSelected -> addPhoto(intent.imageBytes)
            is SteepCompleteIntent.PhotoRemoved -> removePhoto(intent.path)
            is SteepCompleteIntent.ShowNextSteepDialog -> showNextSteepDialog()
            is SteepCompleteIntent.DismissNextSteepDialog -> dismissNextSteepDialog()
            is SteepCompleteIntent.NextSteepDurationChanged -> _state.update {
                it.copy(nextSteepDuration = intent.duration)
            }

            is SteepCompleteIntent.NextSteepTemperatureChanged -> _state.update {
                it.copy(nextSteepTemperature = intent.temperature)
            }

            is SteepCompleteIntent.NextSteepWaterQuantityChanged -> _state.update {
                it.copy(nextSteepWaterQuantity = intent.waterQuantityMl)
            }

            is SteepCompleteIntent.ConfirmNextSteep -> confirmNextSteep()
            is SteepCompleteIntent.FinishSession -> finishSession()
            is SteepCompleteIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)

            is SteepCompleteIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()

            is SteepCompleteIntent.ShowDiscardConfirmation -> _state.update {
                it.copy(showDiscardConfirmation = true)
            }

            is SteepCompleteIntent.DismissDiscardConfirmation -> _state.update {
                it.copy(showDiscardConfirmation = false)
            }

            is SteepCompleteIntent.ConfirmDiscard -> confirmDiscardSession()
            is SteepCompleteIntent.PreviousSteepRatingChanged ->
                updatePreviousSteepRating(intent.steepId, intent.rating)

            is SteepCompleteIntent.PreviousSteepNotesChanged ->
                updatePreviousSteepNotes(intent.steepId, intent.notes)

            is SteepCompleteIntent.BackClicked -> _navigationEvents.trySend(SteepCompleteNavEvent.NavigateBack)
            is SteepCompleteIntent.ShowEditParametersSheet -> openEditParametersSheet()
            is SteepCompleteIntent.DismissEditParametersSheet -> _state.update {
                it.copy(
                    showEditParametersSheet = false
                )
            }

            is SteepCompleteIntent.EditBrewingTimeChanged -> _state.update { it.copy(editBrewingTime = intent.duration) }
            is SteepCompleteIntent.EditTemperatureChanged -> _state.update {
                it.copy(
                    editTemperatureCelsius = intent.celsius
                )
            }

            is SteepCompleteIntent.EditWaterQuantityChanged -> _state.update {
                it.copy(
                    editWaterQuantityMl = intent.milliliters
                )
            }

            is SteepCompleteIntent.EditTeaQuantityChanged -> _state.update {
                it.copy(
                    editTeaQuantityGrams = intent.grams
                )
            }

            is SteepCompleteIntent.EditTeaBagModeChanged -> _state.update { it.copy(editIsTeaBag = intent.isTeaBag) }
            is SteepCompleteIntent.EditWaterTypeChanged -> _state.update { it.copy(editWaterType = intent.waterType) }
            is SteepCompleteIntent.ConfirmEditParameters -> confirmEditParameters()
            is SteepCompleteIntent.CancelEditParameters -> _state.update {
                it.copy(
                    showEditParametersSheet = false
                )
            }

        }
    }

    // Takes a steep's id too: steep 1's id is the session's, and older notifications carry a steep's
    private fun initialize(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val session = teaSessionRepository.getSessionOfSteep(id)
                if (session == null) {
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                    return@launch
                }

                val teaDeferred = async { teaRepository.getById(session.session.teaId) }
                val vesselDeferred = async { brewingVesselRepository.getById(session.session.vesselId) }
                val tea = teaDeferred.await()
                val vessel = vesselDeferred.await()
                val steep = session.currentSteep

                _state.update {
                    it.copy(
                        session = session,
                        tea = tea,
                        vessel = vessel,
                        rating = steep.rating ?: 0f,
                        notes = steep.notes ?: "",
                        photos = steep.photos,
                        previousSteeps = session.steeps.dropLast(1),
                        nextSteepDuration = steep.brewingTime,
                        nextSteepTemperature = steep.temperatureCelsius,
                        nextSteepWaterQuantity = steep.waterQuantityMl,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_session)
            }
        }
    }

    /**
     * Saves [edit] in the background. Edits read the screen's state when they run rather than
     * when they were queued, so the last one to land carries the latest text.
     */
    private fun editSteep(steepId: String, edit: (Steep) -> Steep) {
        viewModelScope.launch {
            editSteepUseCase(steepId, edit)
                .onSuccess { updated -> _state.update { it.copy(session = updated) } }
                .onFailure { messages.failure(it, Res.string.couldnt_save_changes) }
        }
    }

    private fun updateRating(rating: Float) {
        val steep = _state.value.session?.currentSteep ?: return
        _state.update { it.copy(rating = rating) }
        editSteep(steep.id) { it.copy(rating = _state.value.rating.takeIf { r -> r > 0f }) }
    }

    private fun updateNotes(notes: String) {
        val steep = _state.value.session?.currentSteep ?: return
        _state.update { it.copy(notes = notes) }
        editSteep(steep.id) { it.copy(notes = _state.value.notes) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun addPhoto(imageBytes: ByteArray) {
        val steep = _state.value.session?.currentSteep ?: return
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val persistedPath = imageStorage.saveImage(imageBytes, fileName, "session_images")
                _state.update { it.copy(photos = it.photos + persistedPath) }
                editSteep(steep.id) { it.copy(photos = _state.value.photos) }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_save_photo)
            }
        }
    }

    private fun removePhoto(path: String) {
        val steep = _state.value.session?.currentSteep ?: return
        _state.update { it.copy(photos = it.photos - path) }
        viewModelScope.launch {
            editSteepUseCase(steep.id) { it.copy(photos = _state.value.photos) }
                .onSuccess { updated ->
                    _state.update { it.copy(session = updated) }
                    try {
                        imageStorage.deleteImage(path)
                    } catch (_: Exception) {
                    }
                }
                .onFailure { messages.failure(it, Res.string.couldnt_remove_photo) }
        }
    }

    private fun showNextSteepDialog() {
        val steep = _state.value.session?.currentSteep ?: return
        _state.update {
            it.copy(
                showNextSteepDialog = true,
                nextSteepDuration = it.nextSteepDuration ?: steep.brewingTime,
                nextSteepTemperature = it.nextSteepTemperature ?: steep.temperatureCelsius,
                nextSteepWaterQuantity = it.nextSteepWaterQuantity ?: steep.waterQuantityMl,
            )
        }
    }

    private fun dismissNextSteepDialog() {
        _state.update { it.copy(showNextSteepDialog = false) }
    }

    private fun confirmNextSteep() {
        val session = _state.value.session ?: return
        val steep = session.currentSteep
        val duration = _state.value.nextSteepDuration ?: steep.brewingTime
        val temperature = _state.value.nextSteepTemperature ?: steep.temperatureCelsius
        val waterQuantity = _state.value.nextSteepWaterQuantity ?: steep.waterQuantityMl

        _state.update { it.copy(isLoading = true, showNextSteepDialog = false) }

        viewModelScope.launch {
            addSteepUseCase(
                sessionId = session.session.id,
                brewingTime = duration,
                temperatureCelsius = temperature,
                waterQuantityMl = waterQuantity,
            ).onSuccess { updated ->
                _state.update { it.copy(isLoading = false) }
                // Reset TimerService singleton state before the next Timer screen starts
                timerService.stop()
                _navigationEvents.send(SteepCompleteNavEvent.NavigateToTimer(updated.session.id))
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_start_steep)
            }
        }
    }

    private fun finishSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            finishSessionUseCase(session.session.id)
                .onSuccess { finished ->
                    timerService.onSessionResolved(finished.session.id)

                    val shouldPromptConfig = finished.steeps.size == 1 &&
                        (finished.effectiveRating ?: 0f) >= 3f &&
                        finished.session.usedConfigurationId == null

                    if (shouldPromptConfig) {
                        maybeShowSaveConfigDialog(finished)
                    } else {
                        _state.update { it.copy(isLoading = false) }
                        _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome(finished.session.id))
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_finish_session)
                }
        }
    }

    private suspend fun maybeShowSaveConfigDialog(session: SessionWithSteeps) {
        if (checkDuplicateConfigurationUseCase(session)) {
            _state.update { it.copy(isLoading = false) }
            _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome(session.session.id))
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
                isLoading = false,
                showSaveConfigurationDialog = true,
                suggestedConfigurationLabel = label,
                savedSession = session,
            )
        }
    }

    private fun confirmDiscardSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(showDiscardConfirmation = false, isLoading = true) }

        viewModelScope.launch {
            discardSteepUseCase(session.session.id)
                .onSuccess {
                    timerService.onSessionResolved(session.session.id)
                    _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome())
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_discard_steep)
                }
        }
    }

    private fun updatePreviousSteepRating(steepId: String, rating: Float) {
        _state.update {
            it.copy(
                previousSteeps = it.previousSteeps.map { s ->
                    if (s.id == steepId) s.copy(rating = rating.takeIf { r -> r > 0f }) else s
                },
            )
        }
        editSteep(steepId) { it.copy(rating = rating.takeIf { r -> r > 0f }) }
    }

    private fun updatePreviousSteepNotes(steepId: String, notes: String) {
        _state.update {
            it.copy(
                previousSteeps = it.previousSteeps.map { s ->
                    if (s.id == steepId) s.copy(notes = notes) else s
                },
            )
        }
        editSteep(steepId) { steep ->
            steep.copy(notes = _state.value.previousSteeps.find { it.id == steepId }?.notes)
        }
    }

    private fun openEditParametersSheet() {
        val session = _state.value.session ?: return
        val steep = session.currentSteep
        _state.update {
            it.copy(
                showEditParametersSheet = true,
                editBrewingTime = steep.brewingTime,
                editTemperatureCelsius = steep.temperatureCelsius,
                editWaterQuantityMl = steep.waterQuantityMl,
                editTeaQuantityGrams = session.session.teaQuantityGrams?.toDouble(),
                editIsTeaBag = session.session.teaQuantityGrams == null,
                editWaterType = session.session.waterType,
            )
        }
    }

    private fun confirmEditParameters() {
        val session = _state.value.session ?: return
        val steep = session.currentSteep
        val currentState = _state.value
        val temperatureCelsius = currentState.editTemperatureCelsius
        val waterQuantityMl = currentState.editWaterQuantityMl
        val brewingTime = currentState.editBrewingTime.takeIf { it > Duration.ZERO }
        val isTeaBag = currentState.editIsTeaBag
        val teaQuantityGrams = currentState.editTeaQuantityGrams?.toFloat()
        val waterType = currentState.editWaterType

        _state.update { it.copy(showEditParametersSheet = false) }
        viewModelScope.launch {
            editSteepUseCase(steep.id) {
                it.copy(
                    brewingTime = brewingTime ?: it.brewingTime,
                    temperatureCelsius = temperatureCelsius ?: it.temperatureCelsius,
                    waterQuantityMl = waterQuantityMl ?: it.waterQuantityMl,
                )
            }.onFailure { e ->
                messages.failure(e, Res.string.couldnt_save_changes)
                return@launch
            }
            editSessionUseCase(session.session.id) {
                it.copy(
                    teaQuantityGrams = if (isTeaBag) null else teaQuantityGrams ?: it.teaQuantityGrams,
                    waterType = waterType ?: it.waterType,
                )
            }
                .onSuccess { updated -> _state.update { it.copy(session = updated) } }
                .onFailure { messages.failure(it, Res.string.couldnt_save_changes) }
        }
    }
}

sealed interface SteepCompleteNavEvent {
    data class NavigateToHome(val finishedSessionId: String? = null) : SteepCompleteNavEvent
    data class NavigateToTimer(val sessionId: String) : SteepCompleteNavEvent
    data object NavigateBack : SteepCompleteNavEvent
}
