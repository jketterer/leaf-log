package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.screens.collection.validateQuantity
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
import leaf_log.shared.generated.resources.couldnt_save_session
import leaf_log.shared.generated.resources.error_tea_quantity_required
import leaf_log.shared.generated.resources.error_water_quantity_required
import leaf_log.shared.generated.resources.failure_not_found

class EditSessionViewModel(
    private val route: NavRoute.EditSessionRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val editSessionUseCase: EditSessionUseCase,
    private val editSteepUseCase: EditSteepUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditSessionState())
    val state: StateFlow<EditSessionState> = _state.asStateFlow()

    private val _navEvents = Channel<EditSessionNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        loadVessels()
        loadSession(route.sessionId)
    }

    fun onIntent(intent: EditSessionIntent) {
        when (intent) {
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
            is EditSessionIntent.NotesChanged -> updateNotes(intent.notes)
            is EditSessionIntent.SaveClicked -> save()
            is EditSessionIntent.Discard -> _navEvents.trySend(EditSessionNavEvent.NavigateBack)
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

    private fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val aggregate = teaSessionRepository.getSessionOfSteep(sessionId)

                if (aggregate != null) {
                    val session = aggregate.session
                    val steep = aggregate.steeps.first()

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
                            selectedVessel = vessel,
                            selectedWaterType = session.waterType,
                            rinsed = session.rinsed,
                            waterQuantityMl = steep.waterQuantityMl,
                            teaQuantityGrams = session.teaQuantityGrams?.toDouble(),
                            isTeaBag = session.teaQuantityGrams == null,
                            location = session.location ?: "",
                            notes = session.notes ?: "",
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

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
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
                        notes = currentState.notes,
                    )
                }.getOrThrow()
            }

            result
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _navEvents.send(EditSessionNavEvent.NavigateBack)
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_session)
                }
        }
    }
}

sealed interface EditSessionNavEvent {
    data object NavigateBack : EditSessionNavEvent
}
