package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.best
import dev.jketterer.leaflog.domain.models.flavorProfile
import dev.jketterer.leaflog.domain.models.steepCurve
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateBack
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToEditTea
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToLogTea
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToSession
import dev.jketterer.leaflog.presentation.ui.viewmodel.createBrewAgainDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_session
import leaf_log.shared.generated.resources.couldnt_delete_tea
import leaf_log.shared.generated.resources.couldnt_load_tea
import leaf_log.shared.generated.resources.couldnt_start_session
import leaf_log.shared.generated.resources.couldnt_update_favorite
import leaf_log.shared.generated.resources.couldnt_update_tea
import leaf_log.shared.generated.resources.failure_not_found
import org.jetbrains.compose.resources.StringResource

class TeaDetailViewModel(
    private val route: NavRoute.TeaDetailsRoute,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteTeaUseCase: DeleteTeaUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val timerService: TimerService,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaDetailState())
    val state: StateFlow<TeaDetailState> = _state.asStateFlow()

    private val _navEvents = Channel<TeaDetailNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private var teaTypeJob: Job? = null

    private sealed interface PendingAction {
        data class BrewThisTea(val vesselId: String?) : PendingAction
        data class BrewAgain(val sessionId: String) : PendingAction
        data class AdjustBrew(val sessionId: String) : PendingAction
    }

    private val inProgressDelegate = createInProgressSessionDelegate<TeaDetailState, PendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        onFailure = messages::failure,
        onResume = { session ->
            _navEvents.trySend(TeaDetailNavigationEvent.NavigateToTimer(session.session.id))
        },
        executePendingAction = { action ->
            when (action) {
                is PendingAction.BrewThisTea ->
                    _navEvents.trySend(NavigateToLogTea(NavRoute.LogTeaRoute(route.teaId, action.vesselId)))

                is PendingAction.BrewAgain ->
                    brewAgain(action.sessionId)

                is PendingAction.AdjustBrew ->
                    _navEvents.trySend(NavigateToLogTea(NavRoute.LogTeaRoute(sourceSessionId = action.sessionId)))
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

    init {
        loadTea(route.teaId)
        loadRecentSessions(route.teaId)
    }

    fun onIntent(intent: TeaDetailIntent) {
        when (intent) {
            is TeaDetailIntent.DeleteTeaClicked -> showDeleteConfirmation()
            is TeaDetailIntent.ConfirmDelete -> confirmDelete()
            is TeaDetailIntent.CancelDelete -> cancelDelete()
            is TeaDetailIntent.ToggleFavorite -> toggleFavorite()
            is TeaDetailIntent.ToggleFinished -> toggleFinished()

            is TeaDetailIntent.EditTeaClicked -> _navEvents.trySend(NavigateToEditTea)
            is TeaDetailIntent.SessionClicked -> handleSessionClick(intent.sessionId)
            is TeaDetailIntent.BrewThisTeaClicked -> inProgressDelegate.checkAndProceed(
                PendingAction.BrewThisTea(intent.vesselId)
            )

            is TeaDetailIntent.BrewAgainClicked -> brewAgainDelegate.openSession(intent.sessionId)
            is TeaDetailIntent.BrewAgain -> brewAgainDelegate.onIntent(intent.intent)

            is TeaDetailIntent.ViewAllSessionsClicked -> {
                val teaId = _state.value.tea?.id ?: return
                _navEvents.trySend(TeaDetailNavigationEvent.NavigateToHistory(teaId))
            }

            is TeaDetailIntent.FlavorClicked -> _navEvents.trySend(TeaDetailNavigationEvent.NavigateToFlavor(intent.tagId))

            is TeaDetailIntent.BackClicked -> _navEvents.trySend(NavigateBack)

            is TeaDetailIntent.DeleteSessionClicked -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is TeaDetailIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is TeaDetailIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }

            // In-progress session conflict dialog
            is TeaDetailIntent.ResumeInProgress -> inProgressDelegate.resume()
            is TeaDetailIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is TeaDetailIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is TeaDetailIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()
        }
    }

    private fun loadTea(teaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                teaRepository.getByIdFlow(teaId)
                    .catchError(Res.string.couldnt_load_tea)
                    .collect { tea ->
                        if (tea != null) {
                            _state.update { it.copy(tea = tea, isLoading = false) }
                            loadTeaType(tea.teaTypeId)
                        } else if (_state.value.tea != null) {
                            // Deleted while open, from here or anywhere else
                            _navEvents.trySend(NavigateBack)
                        } else {
                            _state.update { it.copy(isLoading = false) }
                            messages.show(Res.string.failure_not_found)
                        }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_tea)
            }
        }
    }

    private fun <T> Flow<T>.catchError(couldnt: StringResource): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false) }
            messages.failure(e, couldnt)
        }
    }

    private fun loadTeaType(teaTypeId: String) {
        teaTypeJob?.cancel()
        teaTypeJob = viewModelScope.launch {
            teaTypeRepository.getByIdFlow(teaTypeId)
                .catchError(Res.string.couldnt_load_tea)
                .collect { teaType ->
                    _state.update { it.copy(teaType = teaType) }
                }
        }
    }

    private fun loadRecentSessions(teaId: String) {
        viewModelScope.launch {
            teaSessionRepository.getSessionsByTeaFlow(teaId)
                .combine(tastingTagRepository.getAllFlow(), ::Pair)
                .catchError(Res.string.couldnt_load_tea)
                .collect { (sessions, vocabulary) ->
                    _state.update {
                        it.copy(
                            recentSessions = sessions.take(5),
                            sessionsById = sessions.associateBy { session -> session.session.id },
                            bestBrew = sessions.best(),
                            flavorProfile = sessions.flavorProfile(vocabulary),
                            steepCurve = sessions.steepCurve(),
                            tagVocabulary = vocabulary,
                        )
                    }
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
        val tea = _state.value.tea ?: return

        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            // On success the tea flow emits null, which closes the page
            deleteTeaUseCase(tea.id)
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_delete_tea)
                }
        }
    }

    private fun toggleFavorite() {
        val tea = _state.value.tea ?: return

        viewModelScope.launch {
            toggleFavoriteUseCase(tea)
                .onFailure { messages.failure(it, Res.string.couldnt_update_favorite) }
        }
    }

    private fun toggleFinished() {
        val tea = _state.value.tea ?: return

        viewModelScope.launch {
            runCatching { teaRepository.setFinished(tea.id, finished = tea.finishedAt == null) }
                .onFailure { messages.failure(it, Res.string.couldnt_update_tea) }
        }
    }

    private fun confirmDeleteSession() {
        viewModelScope.launch {
            val sessionId = _state.value.sessionPendingDelete ?: return@launch
            _state.update { it.copy(sessionPendingDelete = null) }
            deleteSessionUseCase(sessionId)
                .onSuccess { timerService.onSessionResolved(sessionId) }
                .onFailure { messages.failure(it, Res.string.couldnt_delete_session) }
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
                .onSuccess { newSession ->
                    _navEvents.trySend(TeaDetailNavigationEvent.NavigateToTimer(newSession.session.id))
                }
                .onFailure { messages.failure(it, Res.string.couldnt_start_session) }
        }
    }

    private fun handleSessionClick(sessionId: String) {
        val session = _state.value.sessionsById[sessionId]
        if (session?.session?.status == SessionStatus.IN_PROGRESS) {
            _navEvents.trySend(TeaDetailNavigationEvent.NavigateToTimer(sessionId))
        } else {
            _navEvents.trySend(NavigateToSession(sessionId))
        }
    }
}

sealed interface TeaDetailNavigationEvent {
    data object NavigateBack : TeaDetailNavigationEvent
    data class NavigateToSession(val sessionId: String) : TeaDetailNavigationEvent
    data class NavigateToLogTea(val route: NavRoute.LogTeaRoute) : TeaDetailNavigationEvent
    data class NavigateToTimer(val sessionId: String) : TeaDetailNavigationEvent
    data object NavigateToEditTea : TeaDetailNavigationEvent
    data class NavigateToHistory(val teaId: String) : TeaDetailNavigationEvent
    data class NavigateToFlavor(val tagId: String) : TeaDetailNavigationEvent
}
