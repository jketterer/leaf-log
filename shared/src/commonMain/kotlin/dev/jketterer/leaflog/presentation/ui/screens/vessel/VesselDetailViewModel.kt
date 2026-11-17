package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselProfile
import dev.jketterer.leaflog.domain.models.vesselProfile
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.DeleteBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.SetVesselArchivedUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessage
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.messages.UiText
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_archive_vessel
import leaf_log.shared.generated.resources.couldnt_delete_vessel
import leaf_log.shared.generated.resources.couldnt_load_vessel
import leaf_log.shared.generated.resources.couldnt_unarchive_vessel
import leaf_log.shared.generated.resources.failure_not_found
import leaf_log.shared.generated.resources.undo
import leaf_log.shared.generated.resources.vessel_archived

class VesselDetailViewModel(
    private val route: NavRoute.VesselDetailRoute,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val deleteBrewingVesselUseCase: DeleteBrewingVesselUseCase,
    private val setVesselArchivedUseCase: SetVesselArchivedUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val timerService: TimerService,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(VesselDetailState())
    val state: StateFlow<VesselDetailState> = _state.asStateFlow()

    private val _navEvents = Channel<VesselDetailNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<VesselDetailIntent>()

    private sealed interface PendingAction {
        data object BrewWithVessel : PendingAction
    }

    private val inProgressDelegate = createInProgressSessionDelegate<VesselDetailState, PendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        onFailure = messages::failure,
        onResume = { session ->
            _navEvents.trySend(VesselDetailNavigationEvent.NavigateToTimer(session.session.id))
        },
        executePendingAction = { action ->
            when (action) {
                is PendingAction.BrewWithVessel -> _navEvents.trySend(
                    VesselDetailNavigationEvent.NavigateToLogTea(NavRoute.LogTeaRoute(vesselId = route.vesselId)),
                )
            }
        },
    )

    init {
        loadVessel()
    }

    fun onIntent(intent: VesselDetailIntent) {
        when (intent) {
            is VesselDetailIntent.BackClicked -> _navEvents.trySend(VesselDetailNavigationEvent.NavigateBack)
            is VesselDetailIntent.EditVesselClicked ->
                _navEvents.trySend(VesselDetailNavigationEvent.NavigateToEditVessel(route.vesselId))
            is VesselDetailIntent.TeaClicked ->
                _navEvents.trySend(VesselDetailNavigationEvent.NavigateToTea(intent.teaId))
            is VesselDetailIntent.BrewWithVesselClicked -> inProgressDelegate.checkAndProceed(PendingAction.BrewWithVessel)

            is VesselDetailIntent.ArchiveVesselClicked -> setArchived(true)
            is VesselDetailIntent.UnarchiveVesselClicked -> setArchived(false)

            is VesselDetailIntent.DeleteVesselClicked -> _state.update { it.copy(showDeleteConfirmation = true) }
            is VesselDetailIntent.ConfirmDelete -> deleteVessel()
            is VesselDetailIntent.CancelDelete -> _state.update { it.copy(showDeleteConfirmation = false) }

            // In-progress session conflict dialog
            is VesselDetailIntent.ResumeInProgress -> inProgressDelegate.resume()
            is VesselDetailIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is VesselDetailIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is VesselDetailIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()
        }
    }

    private fun loadVessel() {
        viewModelScope.launch {
            combine(
                brewingVesselRepository.getByIdFlow(route.vesselId),
                teaSessionRepository.getSessionsByVesselFlow(route.vesselId),
                teaRepository.getAllFlow(),
                teaTypeRepository.getAllFlow(),
            ) { vessel, sessions, teas, teaTypes ->
                Loaded(
                    vessel = vessel,
                    profile = sessions.vesselProfile(teas.associateBy { it.id }, teaTypes.associateBy { it.id }),
                    hasSessions = sessions.isNotEmpty(),
                )
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_load_vessel)
                }
                .collect { loaded ->
                    when {
                        loaded.vessel != null -> _state.update {
                            it.copy(
                                vessel = loaded.vessel,
                                profile = loaded.profile,
                                hasSessions = loaded.hasSessions,
                                isLoading = false,
                            )
                        }
                        // Deleted while open, from here or anywhere else
                        _state.value.vessel != null -> _navEvents.trySend(VesselDetailNavigationEvent.NavigateBack)
                        else -> {
                            _state.update { it.copy(isLoading = false) }
                            messages.show(Res.string.failure_not_found)
                        }
                    }
                }
        }
    }

    private data class Loaded(val vessel: BrewingVessel?, val profile: VesselProfile?, val hasSessions: Boolean)

    private fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            setVesselArchivedUseCase(route.vesselId, archived)
                .onSuccess {
                    if (archived) {
                        messages.show(
                            UiMessage(
                                text = UiText.Plain(Res.string.vessel_archived),
                                action = UiMessage.Action(Res.string.undo, VesselDetailIntent.UnarchiveVesselClicked),
                            ),
                        )
                    }
                }
                .onFailure {
                    messages.failure(it, if (archived) Res.string.couldnt_archive_vessel else Res.string.couldnt_unarchive_vessel)
                }
        }
    }

    private fun deleteVessel() {
        _state.update { it.copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            // On success the vessel flow emits null, which closes the page
            deleteBrewingVesselUseCase(route.vesselId)
                .onFailure { messages.failure(it, Res.string.couldnt_delete_vessel) }
        }
    }
}

sealed interface VesselDetailNavigationEvent {
    data object NavigateBack : VesselDetailNavigationEvent
    data class NavigateToEditVessel(val vesselId: String) : VesselDetailNavigationEvent
    data class NavigateToTea(val teaId: String) : VesselDetailNavigationEvent
    data class NavigateToLogTea(val route: NavRoute.LogTeaRoute) : VesselDetailNavigationEvent
    data class NavigateToTimer(val sessionId: String) : VesselDetailNavigationEvent
}
