package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.toggled
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.DebouncedSave
import dev.jketterer.leaflog.presentation.ui.viewmodel.createBrewAgainDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_add_steep
import leaf_log.shared.generated.resources.couldnt_create_tag
import leaf_log.shared.generated.resources.couldnt_delete_session
import leaf_log.shared.generated.resources.couldnt_delete_steep
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_save_changes
import leaf_log.shared.generated.resources.couldnt_start_session
import leaf_log.shared.generated.resources.failure_not_found

class SessionDetailViewModel(
    private val route: NavRoute.SessionDetailsRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val deleteSteepUseCase: DeleteSteepUseCase,
    private val editSteepUseCase: EditSteepUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val addSteepUseCase: AddSteepUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val timerService: TimerService,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailState())
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    private val _navEvents = Channel<SessionDetailNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    // A save rewrites the session, rebuilds the tea's stats and reloads this screen, too much to
    // do per keystroke
    private val steepNotesSave = DebouncedSave(appScope)

    private sealed interface PendingAction {
        data class BrewAgain(val sessionId: String) : PendingAction
        data class AdjustBrew(val sessionId: String) : PendingAction
    }

    private val inProgressDelegate =
        createInProgressSessionDelegate<SessionDetailState, PendingAction>(
            getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
            finishSessionUseCase = finishSessionUseCase,
            discardSteepUseCase = discardSteepUseCase,
            timerService = timerService,
            stateFlow = _state,
            getDialogState = { it.inProgressDialogState },
            setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
            onFailure = messages::failure,
            onResume = { session ->
                _navEvents.trySend(SessionDetailNavEvent.NavigateToTimer(session.session.id))
            },
            executePendingAction = { action ->
                when (action) {
                    is PendingAction.BrewAgain -> brewAgain(action.sessionId)
                    is PendingAction.AdjustBrew ->
                        _navEvents.trySend(SessionDetailNavEvent.NavigateToLogTea(action.sessionId))
                }
            },
        )

    private val brewAgainDelegate = createBrewAgainDelegate(
        teaSessionRepository = teaSessionRepository,
        teaRepository = teaRepository,
        teaTypeRepository = teaTypeRepository,
        brewingVesselRepository = brewingVesselRepository,
        tastingTagRepository = tastingTagRepository,
        stateFlow = _state,
        getSheetState = { it.brewAgainSheet },
        setSheetState = { state, sheet -> state.copy(brewAgainSheet = sheet) },
        onFailure = messages::failure,
        onStart = { inProgressDelegate.checkAndProceed(PendingAction.BrewAgain(it)) },
        onAdjust = { inProgressDelegate.checkAndProceed(PendingAction.AdjustBrew(it)) },
    )

    /** Starts at the route's session; stepping moves it without leaving the screen. */
    private val shownSessionId = MutableStateFlow(route.sessionId)

    init {
        loadSession()
        loadNeighbours()
        loadVocabulary()
    }

    fun onIntent(intent: SessionDetailIntent) {
        when (intent) {
            is SessionDetailIntent.DeleteSessionClicked -> showDeleteConfirmation()
            is SessionDetailIntent.ConfirmDelete -> confirmDelete()
            is SessionDetailIntent.CancelDelete -> cancelDelete()
            // Notes typed just before land first, in case the delete is canceled
            is SessionDetailIntent.DeleteSteepClicked -> viewModelScope.launch {
                steepNotesSave.flush()
                showDeleteSteepConfirmation(intent.steepId)
            }

            is SessionDetailIntent.ConfirmDeleteSteep -> confirmDeleteSteep()
            is SessionDetailIntent.CancelDeleteSteep -> cancelDeleteSteep()
            is SessionDetailIntent.BrewAgainClicked -> {
                val sessionId = _state.value.session?.session?.id ?: return
                brewAgainDelegate.openSession(sessionId)
            }

            is SessionDetailIntent.BrewAgain -> brewAgainDelegate.onIntent(intent.intent)

            is SessionDetailIntent.AddSteepClicked -> showAddSteepDialog()
            is SessionDetailIntent.CancelAddSteep -> cancelAddSteep()
            is SessionDetailIntent.UpdateNextSteepDuration -> _state.update {
                it.copy(
                    nextSteepDuration = intent.duration
                )
            }

            is SessionDetailIntent.UpdateNextSteepTemperature -> _state.update {
                it.copy(
                    nextSteepTemperature = intent.temperature
                )
            }

            is SessionDetailIntent.ConfirmAddSteep -> confirmAddSteep()

            // navigation intents
            // Edit Session reads the session as stored and saves it whole, notes included
            is SessionDetailIntent.EditSessionClicked -> {
                val sessionId = _state.value.session?.session?.id ?: return
                viewModelScope.launch {
                    steepNotesSave.flush()
                    _navEvents.send(SessionDetailNavEvent.NavigateToEditSession(sessionId))
                }
            }

            is SessionDetailIntent.ViewTeaClicked -> _navEvents.trySend(
                SessionDetailNavEvent.NavigateToTeaDetails(
                    intent.teaId
                )
            )

            is SessionDetailIntent.BackClicked -> viewModelScope.launch {
                steepNotesSave.flush()
                _navEvents.send(SessionDetailNavEvent.NavigateBack)
            }

            is SessionDetailIntent.StepToSession -> shownSessionId.value = intent.sessionId

            is SessionDetailIntent.SteepClicked -> openSteep(intent.steepId)
            is SessionDetailIntent.SteepRatingChanged -> {
                val id = _state.value.editingSteepId ?: return
                _state.update { it.copy(editRating = intent.rating) }
                editSteep(id) { it.copy(rating = _state.value.editRating.takeIf { r -> r > 0f }) }
            }

            is SessionDetailIntent.SteepNotesChanged -> {
                val id = _state.value.editingSteepId ?: return
                val notes = intent.notes
                _state.update { it.copy(editNotes = notes) }
                steepNotesSave.schedule { saveSteep(id) { it.copy(notes = notes) } }
            }

            is SessionDetailIntent.SteepTagToggled -> toggleTag(intent.tagId)
            is SessionDetailIntent.SteepTagCreated -> createTag(intent.name)
            is SessionDetailIntent.SteepParametersChanged -> {
                val id = _state.value.editingSteepId ?: return
                editSteep(id) {
                    it.withBrewedTime(intent.brewedTime)
                        .copy(temperatureCelsius = intent.temperatureCelsius, waterQuantityMl = intent.waterQuantityMl)
                }
            }

            is SessionDetailIntent.DismissSteepEdit -> _state.update { it.copy(editingSteepId = null) }

            // In-progress session conflict dialog
            is SessionDetailIntent.ResumeInProgress -> inProgressDelegate.resume()
            is SessionDetailIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is SessionDetailIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is SessionDetailIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadSession() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                shownSessionId
                    .flatMapLatest { teaSessionRepository.getSessionFlow(it) }
                    .catch { e ->
                        _state.update { it.copy(isLoading = false) }
                        messages.failure(e, Res.string.couldnt_load_session)
                    }
                    .collectLatest { session ->
                        if (session != null) {
                            // Everything shown arrives in one update, so a stepped-to session never
                            // appears beside the last one's tea
                            val tea = loadTea(session.session.teaId)
                            val vessel = loadVessel(session.session.vesselId)
                            _state.update {
                                it.copy(
                                    session = session,
                                    tea = tea,
                                    vessel = vessel,
                                    isLoading = false,
                                )
                            }
                        } else {
                            // Not found shows in place of the session; one deleted from this screen
                            // is already being navigated away from
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_session)
            }
        }
    }

    private suspend fun loadTea(teaId: String) = teaRepository.getByIdFlow(teaId)
        .catch { e -> Logger.w(tag = "SessionDetail") { "Failed to load tea: ${e.message}" } }
        .firstOrNull()

    private suspend fun loadVessel(vesselId: String) = brewingVesselRepository.getByIdFlow(vesselId)
        .catch { e -> Logger.w(tag = "SessionDetail") { "Failed to load vessel: ${e.message}" } }
        .firstOrNull()

    private fun loadNeighbours() {
        viewModelScope.launch {
            combine(shownSessionId, teaSessionRepository.getSessionIdsFlow()) { id, ids -> id to ids }
                .catch { e -> Logger.w(tag = "SessionDetail") { "Failed to load neighbours: ${e.message}" } }
                .collect { (id, ids) ->
                    val index = ids.indexOf(id).takeIf { it >= 0 }
                    _state.update {
                        it.copy(
                            previousSessionId = index?.let { i -> ids.getOrNull(i - 1) },
                            nextSessionId = index?.let { i -> ids.getOrNull(i + 1) },
                        )
                    }
                }
        }
    }

    private fun loadVocabulary() {
        viewModelScope.launch {
            try {
                val vocabulary = tastingTagRepository.getAll()
                _state.update { it.copy(vocabulary = vocabulary) }
            } catch (e: Exception) {
                Logger.w(tag = "SessionDetail") { "Failed to load tags: ${e.message}" }
            }
        }
    }

    private fun showDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun cancelDelete() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun confirmDelete() {
        val sessionId = _state.value.session?.session?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            deleteSessionUseCase(sessionId)
                .onSuccess {
                    timerService.onSessionResolved(sessionId)
                    _navEvents.send(SessionDetailNavEvent.NavigateBack)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_delete_session)
                }
        }
    }

    private fun showDeleteSteepConfirmation(steepId: String) {
        _state.update {
            it.copy(
                editingSteepId = null,
                showDeleteSteepConfirmation = true,
                steepToDelete = steepId
            )
        }
    }

    private fun openSteep(steepId: String) {
        val steep = _state.value.session?.steeps?.firstOrNull { it.id == steepId } ?: return
        _state.update {
            it.copy(
                editingSteepId = steepId,
                editRating = steep.rating ?: 0f,
                editNotes = steep.notes ?: "",
            )
        }
    }

    /** The session flow brings the saved steep back. */
    private fun editSteep(steepId: String, edit: (Steep) -> Steep) {
        viewModelScope.launch { saveSteep(steepId, edit) }
    }

    private suspend fun saveSteep(steepId: String, edit: (Steep) -> Steep) {
        editSteepUseCase(steepId, edit)
            .onFailure { messages.failure(it, Res.string.couldnt_save_changes) }
    }

    private fun toggleTag(tagId: String) {
        val id = _state.value.editingSteepId ?: return
        editSteep(id) { it.copy(tags = it.tags.toggled(tagId)) }
    }

    /** Makes the tag and puts it on the steep being edited. */
    private fun createTag(name: String) {
        viewModelScope.launch {
            try {
                val tag = tastingTagRepository.createCustom(name)
                _state.update { it.copy(vocabulary = it.vocabulary + tag) }
                toggleTag(tag.id)
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_create_tag)
            }
        }
    }

    private fun cancelDeleteSteep() {
        _state.update {
            it.copy(
                showDeleteSteepConfirmation = false,
                steepToDelete = null
            )
        }
    }

    private fun confirmDeleteSteep() {
        val steepId = _state.value.steepToDelete ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    showDeleteSteepConfirmation = false,
                    steepToDelete = null
                )
            }
            deleteSteepUseCase(steepId)
                .onFailure { messages.failure(it, Res.string.couldnt_delete_steep) }
        }
    }

    private fun showAddSteepDialog() {
        val lastSteep = _state.value.session?.currentSteep ?: return
        _state.update {
            it.copy(
                showAddSteepDialog = true,
                nextSteepDuration = lastSteep.brewingTime,
                nextSteepTemperature = lastSteep.temperatureCelsius,
            )
        }
    }

    private fun cancelAddSteep() {
        _state.update {
            it.copy(
                showAddSteepDialog = false,
                nextSteepDuration = null,
                nextSteepTemperature = null,
            )
        }
    }

    private fun confirmAddSteep() {
        val session = _state.value.session ?: return
        val duration = _state.value.nextSteepDuration ?: return
        val temperature = _state.value.nextSteepTemperature ?: return

        viewModelScope.launch {
            _state.update { it.copy(showAddSteepDialog = false) }

            addSteepUseCase(
                sessionId = session.session.id,
                brewingTime = duration,
                temperatureCelsius = temperature,
                waterQuantityMl = session.currentSteep.waterQuantityMl,
            ).onFailure { messages.failure(it, Res.string.couldnt_add_steep) }
        }
    }

    private fun brewAgain(sessionId: String) {
        viewModelScope.launch {
            val session = teaSessionRepository.getSession(sessionId)
            if (session == null) {
                messages.show(Res.string.failure_not_found)
                return@launch
            }
            startSessionUseCase(session)
                .onSuccess {
                    _navEvents.send(SessionDetailNavEvent.NavigateToTimer(it.session.id))
                }
                .onFailure { messages.failure(it, Res.string.couldnt_start_session) }
        }
    }
}

sealed interface SessionDetailNavEvent {
    data object NavigateBack : SessionDetailNavEvent
    data class NavigateToEditSession(val sessionId: String) : SessionDetailNavEvent

    data class NavigateToTeaDetails(val teaId: String) : SessionDetailNavEvent
    data class NavigateToTimer(val sessionId: String) : SessionDetailNavEvent
    data class NavigateToLogTea(val sourceSessionId: String) : SessionDetailNavEvent
}