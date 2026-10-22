package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
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
        load()
    }

    fun onIntent(intent: EditSessionIntent) {
        when (intent) {
            is EditSessionIntent.TeaSelected -> _state.update { it.copy(tea = intent.tea) }
            is EditSessionIntent.DateChanged -> moveStart { LocalDateTime(intent.date, it.time) }
            is EditSessionIntent.TimeChanged -> moveStart {
                LocalDateTime(it.date, LocalTime(intent.hour, intent.minute, it.second, it.nanosecond))
            }

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

    /**
     * The session first, then the lists: a session brewed in an archived vessel or of a deleted tea
     * still shows it, because neither comes from the active lists.
     */
    private fun load() = viewModelScope.launch {
        val sessionVessel = try {
            val aggregate = teaSessionRepository.getSessionOfSteep(route.sessionId) ?: run {
                messages.show(Res.string.failure_not_found)
                return@launch
            }
            val session = aggregate.session
            val steep = aggregate.steeps.first()
            val vessel = brewingVesselRepository.getById(session.vesselId)
            val tea = teaRepository.getById(session.teaId)
            _state.update {
                it.copy(
                    existingSession = session,
                    existingSteep = steep,
                    tea = tea,
                    startedAt = steep.timestamp,
                    selectedVessel = vessel,
                    selectedWaterType = session.waterType,
                    rinsed = session.rinsed,
                    waterQuantityMl = steep.waterQuantityMl,
                    teaQuantityGrams = session.teaQuantityGrams?.toDouble(),
                    isTeaBag = session.teaQuantityGrams == null,
                    location = session.location ?: "",
                    notes = session.notes ?: "",
                )
            }
            vessel
        } catch (e: Exception) {
            messages.failure(e, Res.string.couldnt_load_session)
            return@launch
        }

        combine(
            brewingVesselRepository.getActiveFlow(),
            teaRepository.getAllFlow(),
            teaRepository.getRecentlyBrewedFlow(RECENT_TEAS),
            teaTypeRepository.getAllFlow(),
        ) { vessels, teas, recentTeas, teaTypes ->
            val withSessionVessel = if (sessionVessel == null || vessels.any { it.id == sessionVessel.id }) {
                vessels
            } else {
                listOf(sessionVessel) + vessels
            }
            _state.update {
                it.copy(
                    availableVessels = withSessionVessel,
                    availableTeas = teas,
                    recentTeas = recentTeas,
                    teaTypes = teaTypes,
                )
            }
        }
            .catch { messages.failure(it, Res.string.couldnt_load_session) }
            .collect()
    }

    /** Moves the start within the device's time zone; [save] shifts every steep with it. */
    private fun moveStart(transform: (LocalDateTime) -> LocalDateTime) = _state.update {
        val startedAt = it.startedAt ?: return@update it
        val zone = TimeZone.currentSystemDefault()
        it.copy(startedAt = transform(startedAt.toLocalDateTime(zone)).toInstant(zone))
    }

    private fun selectVessel(vessel: BrewingVessel) {
        _state.update { it.copy(selectedVessel = vessel) }
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
                editSessionUseCase(session.id, startedAt = currentState.startedAt) {
                    it.copy(
                        teaId = currentState.tea?.id ?: it.teaId,
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

private const val RECENT_TEAS = 5

sealed interface EditSessionNavEvent {
    data object NavigateBack : EditSessionNavEvent
}
