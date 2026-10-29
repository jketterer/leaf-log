package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.best
import dev.jketterer.leaflog.domain.models.lastCompleted
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewingParametersPrefill
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.screens.collection.validateQuantity
import dev.jketterer.leaflog.presentation.ui.screens.collection.validateTemperature
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load
import leaf_log.shared.generated.resources.couldnt_save_session
import leaf_log.shared.generated.resources.couldnt_save_tea
import leaf_log.shared.generated.resources.error_tea_quantity_required
import leaf_log.shared.generated.resources.error_temperature_required
import leaf_log.shared.generated.resources.error_water_quantity_required
import kotlin.time.Duration

class LogTeaViewModel(
    private val route: NavRoute.LogTeaRoute,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val createTeaUseCase: CreateTeaUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val timerService: TimerService,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LogTeaState())
    val state: StateFlow<LogTeaState> = _state.asStateFlow()

    private val _navEvents = Channel<LogTeaNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private sealed interface PendingAction {
        data object StartTimer : PendingAction
    }

    private val inProgressDelegate = createInProgressSessionDelegate<LogTeaState, PendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        onFailure = messages::failure,
        onResume = { session ->
            _navEvents.trySend(LogTeaNavigationEvent.NavigateToTimer(session.session.id))
        },
        executePendingAction = { action ->
            when (action) {
                is PendingAction.StartTimer -> saveSession(SessionStatus.IN_PROGRESS)
            }
        },
    )

    init {
        onIntent(LogTeaIntent.LoadData)
        collectPairBrews()
        loadTagVocabulary()
        route.sourceSessionId?.let(::selectSourceSession)
        route.teaId?.let(::selectTea)
        route.vesselId?.let(::selectVessel)
    }

    fun onIntent(intent: LogTeaIntent) {
        when (intent) {
            is LogTeaIntent.LoadData -> loadData()
            is LogTeaIntent.ShowTeaPicker -> showTeaPicker()
            is LogTeaIntent.HideTeaPicker -> hideTeaPicker()
            is LogTeaIntent.TeaPickerQueryChanged -> updateTeaPickerQuery(intent.query)
            is LogTeaIntent.TeaPickerFilterChanged -> updateTeaPickerFilter(intent.filter)
            is LogTeaIntent.TeaSelected -> selectTea(intent.teaId)
            is LogTeaIntent.QuickAddTeaSaved -> handleQuickAddTeaSaved(intent.name, intent.teaTypeId)
            is LogTeaIntent.TeaBagModeChanged -> updateTeaBagMode(intent.isTeaBag)
            is LogTeaIntent.TeaQuantityChanged -> updateTeaQuantity(intent.grams)
            is LogTeaIntent.WaterQuantityChanged -> updateWaterQuantity(intent.milliliters)
            is LogTeaIntent.TemperatureChanged -> updateTemperature(intent.celsius)
            is LogTeaIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is LogTeaIntent.VesselSelected -> selectVessel(intent.vesselId)
            is LogTeaIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is LogTeaIntent.RinsedChanged -> selectRinsed(intent.rinsed)
            is LogTeaIntent.LocationChanged -> updateLocation(intent.location)
            is LogTeaIntent.AddPhotoClicked -> {
                // Photo picker handled by UI
            }

            is LogTeaIntent.PhotoSelected -> addPhoto(intent.photoUri)
            is LogTeaIntent.PhotoRemoved -> removePhoto(intent.photoUri)
            is LogTeaIntent.SaveAsCompleted -> saveSession(SessionStatus.COMPLETED)
            is LogTeaIntent.StartTimerClicked -> inProgressDelegate.checkAndProceed(PendingAction.StartTimer)

            is LogTeaIntent.BackClicked -> _navEvents.trySend(LogTeaNavigationEvent.NavigateBack)
            is LogTeaIntent.SourceBrewClicked ->
                _navEvents.trySend(LogTeaNavigationEvent.NavigateToSession(intent.sessionId))

            is LogTeaIntent.SourceBrewSelected -> selectSourceBrew(intent.sessionId)

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
                teaTypeRepository.getAllFlow(),
            ) { prefs, teas, vessels, recentTeas, teaTypes ->
                object {
                    val prefs = prefs
                    val teas = teas
                    val vessels = vessels
                    val recentTeas = recentTeas
                    val teaTypes = teaTypes
                }
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_load)
                }
                .collect { data ->
                    _state.update {
                        it.copy(
                            userPreferences = data.prefs,
                            availableTeas = data.teas,
                            suggestedTeas = data.recentTeas,
                            availableVessels = data.vessels,
                            availableTeaTypes = data.teaTypes,
                            isLoading = false,
                            selectedWaterType = data.prefs.defaultWaterType,
                        )
                    }
                }
        }
    }

    /**
     * Follows the selected pair's sessions, so the switch is never stale and the source card
     * never a deleted session. A list for a pair no longer selected is dropped.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectPairBrews() = viewModelScope.launch {
        _state.map { it.pair }
            .distinctUntilChanged()
            .flatMapLatest { pair ->
                val (teaId, vesselId) = pair ?: return@flatMapLatest flowOf(null to emptyList())
                teaSessionRepository.getSessionsByTeaFlow(teaId)
                    .map { sessions -> pair to sessions.filter { it.session.vesselId == vesselId } }
            }
            .catch { messages.failure(it, Res.string.couldnt_load) }
            .collect { (pair, sessions) ->
                _state.update { state ->
                    if (state.pair != pair) return@update state
                    state.copy(
                        lastBrew = sessions.lastCompleted(),
                        bestBrew = sessions.best(),
                        sourceBrew = state.sourceBrew?.let { source ->
                            sessions.firstOrNull { it.session.id == source.session.id }
                        },
                    )
                }
            }
    }

    private val LogTeaState.pair: Pair<String, String>?
        get() {
            val tea = selectedTea ?: return null
            val vessel = selectedVessel ?: return null
            return tea.id to vessel.id
        }

    private fun loadTagVocabulary() = viewModelScope.launch {
        runCatching { tastingTagRepository.getAll() }
            .onSuccess { tags -> _state.update { it.copy(tagVocabulary = tags) } }
            .onFailure { messages.failure(it, Res.string.couldnt_load) }
    }

    private fun showTeaPicker() {
        _state.update { it.copy(showTeaPicker = true) }
    }

    private fun hideTeaPicker() {
        _state.update { it.copy(showTeaPicker = false).withPickerReset() }
    }

    private fun updateTeaPickerQuery(query: String) {
        _state.update { it.copy(teaPickerQuery = query) }
    }

    private fun updateTeaPickerFilter(filter: TeaPickerFilter) {
        _state.update { it.copy(teaPickerFilter = filter) }
    }

    private fun LogTeaState.withPickerReset() = copy(
        teaPickerQuery = "",
        teaPickerFilter = TeaPickerFilter.All,
    )

    private fun selectTea(teaId: String?) = viewModelScope.launch {
        if (teaId == null) {
            _state.update {
                it.copy(
                    selectedTea = null,
                    selectedTeaType = null,
                    sourceBrew = null,
                    showTeaPicker = false,
                    hasUnsavedChanges = true,
                ).withPickerReset()
            }
            return@launch
        }

        // Wait for initial data load to complete before attempting to find tea
        state.first { !it.isLoading }

        val tea = _state.value.availableTeas.firstOrNull { it.id == teaId }
        val teaType = tea?.let { teaTypeRepository.getById(it.teaTypeId) }
        val prefill = prefillFor(tea, _state.value.selectedVessel)
        _state.update {
            prefill(
                it.copy(
                    selectedTea = tea,
                    selectedTeaType = teaType,
                    showTeaPicker = false,
                    hasUnsavedChanges = true,
                ).withPickerReset(),
            )
        }
    }

    /**
     * Reads the parameters a tea and vessel prefill, for the update that selects them to apply.
     * The fields appear with that selection, so a value typed into them is never overwritten.
     */
    private suspend fun prefillFor(tea: Tea?, vessel: BrewingVessel?): (LogTeaState) -> LogTeaState {
        if (tea == null || vessel == null) return { it }
        val prefill = getBrewingParametersPrefillUseCase(tea, vessel)
        return { it.filledFrom(prefill) }
    }

    /** The form as [prefill] fills it; a past brew becomes the source the session replays. */
    private fun LogTeaState.filledFrom(prefill: BrewingParametersPrefill): LogTeaState {
        val source = (prefill.source as? PrefillSource.PastBrew)?.session
        return copy(
            // A past brew that named no tea quantity was a bag; a default temperature names nothing
            isTeaBag = source != null && prefill.teaQuantityGrams == null,
            teaQuantityGrams = prefill.teaQuantityGrams?.toDouble(),
            waterQuantityMl = prefill.waterQuantityMl ?: selectedVessel?.capacityMl?.toDouble(),
            temperatureCelsius = prefill.temperatureCelsius,
            brewingTime = prefill.brewingTime,
            selectedWaterType = prefill.waterType ?: selectedWaterType,
            rinsed = prefill.rinsed,
            prefillSource = if (source != null) PrefillSource.None else prefill.source,
            sourceBrew = source,
        )
    }

    /** Adjust from a Brew again sheet: [sessionId]'s tea and vessel, filled from it. */
    private fun selectSourceSession(sessionId: String) = viewModelScope.launch {
        state.first { !it.isLoading }

        val brew = teaSessionRepository.getSession(sessionId) ?: return@launch
        val tea = _state.value.availableTeas.firstOrNull { it.id == brew.session.teaId }
        val teaType = tea?.let { teaTypeRepository.getById(it.teaTypeId) }
        val vessel = _state.value.availableVessels.firstOrNull { it.id == brew.session.vesselId }
        _state.update {
            it.copy(selectedTea = tea, selectedTeaType = teaType, selectedVessel = vessel)
                .filledFrom(BrewingParametersPrefill.of(brew))
        }
    }

    /** Start from: refills the form from the pair's last or best brew, or clears it for scratch. */
    private fun selectSourceBrew(sessionId: String?) {
        if (sessionId == null) return startFromScratch()
        _state.update { state ->
            val brew = listOfNotNull(state.lastBrew, state.bestBrew)
                .firstOrNull { it.session.id == sessionId } ?: return@update state
            state.filledFrom(BrewingParametersPrefill.of(brew)).copy(hasUnsavedChanges = true)
        }
    }

    /** Clears what a source filled, as typing every field fresh would start. */
    private fun startFromScratch() {
        _state.update {
            it.copy(
                isTeaBag = false,
                teaQuantityGrams = null,
                waterQuantityMl = null,
                temperatureCelsius = null,
                brewingTime = null,
                prefillSource = PrefillSource.None,
                sourceBrew = null,
                hasUnsavedChanges = true,
            )
        }
    }

    private fun handleQuickAddTeaSaved(name: String, teaTypeId: String) {
        viewModelScope.launch {
            createTeaUseCase(name = name, teaTypeId = teaTypeId)
                .onSuccess { tea ->
                    val teaType = teaTypeRepository.getById(tea.teaTypeId)
                    val prefill = prefillFor(tea, _state.value.selectedVessel)
                    _state.update {
                        prefill(
                            it.copy(
                                selectedTea = tea,
                                selectedTeaType = teaType,
                                showTeaPicker = false,
                                hasUnsavedChanges = true,
                            ).withPickerReset(),
                        )
                    }
                }
                .onFailure { messages.failure(it, Res.string.couldnt_save_tea) }
        }
    }

    private fun updateTeaBagMode(isTeaBag: Boolean) {
        _state.update {
            it.copy(
                isTeaBag = isTeaBag,
                teaQuantityGrams = if (isTeaBag) null else it.teaQuantityGrams,
                teaQuantityError = null,
                hasUnsavedChanges = true,
            )
        }
    }

    private fun updateTeaQuantity(grams: Double?) {
        _state.update {
            it.copy(
                teaQuantityGrams = grams,
                teaQuantityError = validateQuantity(grams, Res.string.error_tea_quantity_required),
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
            )
        }
    }

    private fun updateWaterQuantity(milliliters: Double?) {
        _state.update {
            it.copy(
                waterQuantityMl = milliliters,
                waterQuantityError = validateQuantity(milliliters, Res.string.error_water_quantity_required),
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
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
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
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
            )
        }
    }

    private fun selectVessel(vesselId: String?) = viewModelScope.launch {
        if (vesselId == null) return@launch

        state.first { !it.isLoading }

        val vessel = _state.value.availableVessels.firstOrNull { it.id == vesselId }
        val tea = _state.value.selectedTea
        val prefill = prefillFor(tea, vessel)
        _state.update { currentState ->
            val selected = prefill(
                currentState.copy(
                    selectedVessel = vessel,
                    hasUnsavedChanges = true
                ),
            )
            if (tea == null && vessel?.capacityMl != null) {
                selected.copy(waterQuantityMl = vessel.capacityMl.toDouble())
            } else {
                selected
            }
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update {
            it.copy(
                selectedWaterType = waterType,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
            )
        }
    }

    private fun selectRinsed(rinsed: Boolean) {
        _state.update {
            it.copy(
                rinsed = rinsed,
                hasUnsavedChanges = true,
                prefillSource = PrefillSource.None,
            )
        }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location, hasUnsavedChanges = true) }
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

    /** In progress goes to the timer; completed goes to the review, for its ratings and tags. */
    private fun saveSession(status: SessionStatus) {
        val currentState = _state.value

        if (!currentState.isValid) {
            _state.update {
                it.copy(
                    teaQuantityError = if (!it.isTeaBag && it.teaQuantityGrams == null) {
                        Res.string.error_tea_quantity_required
                    } else {
                        it.teaQuantityError
                    },
                    waterQuantityError = if (it.waterQuantityMl == null) {
                        Res.string.error_water_quantity_required
                    } else {
                        it.waterQuantityError
                    },
                    temperatureError = if (it.temperatureCelsius == null) {
                        Res.string.error_temperature_required
                    } else {
                        it.temperatureError
                    },
                    brewingTimeError = if (it.brewingTime == null) "Brewing time is required" else it.brewingTimeError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val temperatureCelsius = currentState.temperatureCelsius!!
            val waterQuantityMl = currentState.waterQuantityMl!!

            // Use the use case - it has business logic (validation, ID generation, timestamps)
            startSessionUseCase(
                teaId = currentState.selectedTea!!.id,
                teaQuantityGrams = if (currentState.isTeaBag) null else currentState.teaQuantityGrams?.toFloat(),
                vesselId = currentState.selectedVessel!!.id,
                waterType = currentState.selectedWaterType,
                rinsed = currentState.rinsed,
                location = currentState.location.takeIf { it.isNotBlank() },
                brewingTime = currentState.brewingTime!!,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                photos = currentState.photos,
                status = status,
                planSessionId = currentState.sourceBrew?.session?.id,
            )
                .onSuccess {
                    _state.update { state -> state.copy(isSaving = false, hasUnsavedChanges = false) }
                    _navEvents.send(
                        if (status == SessionStatus.IN_PROGRESS) {
                            LogTeaNavigationEvent.NavigateToTimer(it.session.id)
                        } else {
                            LogTeaNavigationEvent.NavigateToReview(it.session.id)
                        }
                    )
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_session)
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
    data class NavigateToReview(val sessionId: String) : LogTeaNavigationEvent
    data class NavigateToSession(val sessionId: String) : LogTeaNavigationEvent
}