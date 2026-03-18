package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateAverageRatingUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionDetailViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val updateAverageRatingUseCase: UpdateAverageRatingUseCase,
    private val brewAgainUseCase: BrewAgainUseCase,
    private val addSteepUseCase: AddSteepUseCase,
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailState())
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    private val _navEvents = Channel<SessionDetailNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferencesFlow()
                .catch { e -> Logger.w("SessionDetail") { "Failed to load preferences: ${e.message}" } }
                .collect { preferences ->
                    _state.update { it.copy(userPreferences = preferences) }
                }
        }
    }

    fun onIntent(intent: SessionDetailIntent) {
        when (intent) {
            is SessionDetailIntent.LoadSession -> loadSession(intent.sessionId)
            is SessionDetailIntent.EditSteepClicked -> {
                // Edit only steep parameters (editFullSession = false)
                _navEvents.trySend(
                    SessionDetailNavEvent.NavigateToEditSession(
                        sessionId = intent.steepId,
                        editFullSession = false
                    )
                )
            }

            is SessionDetailIntent.DeleteSessionClicked -> showDeleteConfirmation()
            is SessionDetailIntent.ConfirmDelete -> confirmDelete()
            is SessionDetailIntent.CancelDelete -> cancelDelete()
            is SessionDetailIntent.DeleteSteepClicked -> showDeleteSteepConfirmation(intent.steepId)
            is SessionDetailIntent.ConfirmDeleteSteep -> confirmDeleteSteep()
            is SessionDetailIntent.CancelDeleteSteep -> cancelDeleteSteep()
            is SessionDetailIntent.BrewAgainClicked -> brewAgain()
            is SessionDetailIntent.AddSteepClicked -> showAddSteepDialog()
            is SessionDetailIntent.CancelAddSteep -> cancelAddSteep()
            is SessionDetailIntent.UpdateNextSteepDuration -> _state.update { it.copy(nextSteepDuration = intent.duration) }
            is SessionDetailIntent.UpdateNextSteepTemperature -> _state.update { it.copy(nextSteepTemperature = intent.temperature) }
            is SessionDetailIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is SessionDetailIntent.ConfirmAddSteep -> confirmAddSteep()

            // navigation intents
            is SessionDetailIntent.EditSessionClicked -> {
                val sessionId = _state.value.parentSession?.id ?: return
                // Edit full session details (editFullSession = true)
                _navEvents.trySend(
                    SessionDetailNavEvent.NavigateToEditSession(
                        sessionId = sessionId,
                        editFullSession = true
                    )
                )
            }

            is SessionDetailIntent.ViewTeaClicked -> _navEvents.trySend(
                SessionDetailNavEvent.NavigateToTeaDetails(
                    intent.teaId
                )
            )

            is SessionDetailIntent.BackClicked -> {
                _navEvents.trySend(SessionDetailNavEvent.NavigateBack)
            }

            is SessionDetailIntent.ClearError -> _state.update { it.copy(error = null) }
            is SessionDetailIntent.SaveAsConfigurationClicked -> saveAsConfiguration()
            is SessionDetailIntent.ConfirmSaveConfiguration -> confirmSaveConfiguration(intent.label)
            is SessionDetailIntent.DismissSaveConfiguration -> _state.update { it.copy(showSaveConfigurationDialog = false) }
        }
    }

    private fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                teaSessionRepository.getByIdFlow(sessionId)
                    .catch { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load session: ${e.message}",
                            )
                        }
                    }
                    .collect { session ->
                        if (session != null) {
                            val parentSession = if (session.parentSessionId == null) {
                                session
                            } else {
                                teaSessionRepository.getById(session.parentSessionId)
                            }

                            if (parentSession != null) {
                                val childSteeps =
                                    teaSessionRepository.getChildSteeps(parentSession.id)

                                _state.update {
                                    it.copy(
                                        parentSession = parentSession,
                                        childSteeps = childSteeps,
                                        isLoading = false
                                    )
                                }

                                // Load related data
                                loadRelatedData(parentSession.teaId, parentSession.vesselId)
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Session not found"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load session: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun loadRelatedData(teaId: String, vesselId: String) {
        teaRepository.getByIdFlow(teaId)
            .catch { e -> Logger.w("SessionDetail") { "Failed to load tea: ${e.message}" } }
            .firstOrNull()
            .let { tea ->
                _state.update { it.copy(tea = tea) }
                tea?.let { loadTeaType(it.teaTypeId) }
            }

        brewingVesselRepository.getByIdFlow(vesselId)
            .catch { e -> Logger.w("SessionDetail") { "Failed to load vessel: ${e.message}" } }
            .firstOrNull()
            .let { vessel ->
                _state.update { it.copy(vessel = vessel) }
            }
    }

    private suspend fun loadTeaType(teaTypeId: String) {
        teaTypeRepository.getByIdFlow(teaTypeId)
            .catch { e -> Logger.w("SessionDetail") { "Failed to load tea type: ${e.message}" } }
            .firstOrNull()
            .let { teaType ->
                _state.update { it.copy(teaType = teaType) }
            }
    }

    private fun showDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun cancelDelete() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun confirmDelete() {
        val session = _state.value.parentSession ?: return

        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            // Clean up photo files for all steeps
            val allSteeps = _state.value.allSteeps
            for (steep in allSteeps) {
                for (photo in steep.photos) {
                    try {
                        imageStorage.deleteImage(photo)
                    } catch (_: Exception) {
                    }
                }
            }

            deleteSessionUseCase(session.id)
                .onSuccess {
                    _navEvents.send(SessionDetailNavEvent.NavigateBack)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete session: ${e.message}",
                        )
                    }
                }
        }
    }

    private fun showDeleteSteepConfirmation(steepId: String) {
        _state.update {
            it.copy(
                showDeleteSteepConfirmation = true,
                steepToDelete = steepId
            )
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
        val parentSession = _state.value.parentSession ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    showDeleteSteepConfirmation = false,
                    isLoading = true
                )
            }

            // Clean up photos for the steep being deleted
            val steepToDelete = _state.value.allSteeps.find { it.id == steepId }
            steepToDelete?.photos?.forEach { photo ->
                try {
                    imageStorage.deleteImage(photo)
                } catch (_: Exception) {
                }
            }

            // Delete the steep
            deleteSessionUseCase(steepId)
                .onSuccess {
                    // Get all remaining child steeps
                    val remainingSteeps = teaSessionRepository.getChildSteeps(parentSession.id)
                        .sortedBy { it.steepNumber }

                    // Renumber the remaining steeps sequentially
                    remainingSteeps.forEachIndexed { index, steep ->
                        val newSteepNumber = index + 2 // +2 because parent is steep 1
                        if (steep.steepNumber != newSteepNumber) {
                            val updatedSteep = steep.copy(
                                steepNumber = newSteepNumber,
                                updatedAt = kotlin.time.Clock.System.now()
                            )
                            teaSessionRepository.upsert(updatedSteep)
                        }
                    }

                    // Update parent session's average rating
                    updateAverageRatingUseCase(parentSession.id)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            steepToDelete = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            steepToDelete = null,
                            error = "Failed to delete steep: ${e.message}"
                        )
                    }
                }
        }
    }

    private fun showAddSteepDialog() {
        val lastSteep = _state.value.allSteeps.lastOrNull() ?: return
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
        val parentSession = _state.value.parentSession ?: return
        val duration = _state.value.nextSteepDuration ?: return
        val temperature = _state.value.nextSteepTemperature ?: return

        viewModelScope.launch {
            _state.update { it.copy(showAddSteepDialog = false, isLoading = true) }

            addSteepUseCase(
                parentSession = parentSession,
                brewingTime = duration,
                temperatureCelsius = temperature,
                waterQuantityMl = parentSession.waterQuantityMl,
            )
                .onSuccess { newSteep ->
                    _navEvents.send(SessionDetailNavEvent.NavigateToTimer(newSteep.id))
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to add steep: ${e.message}",
                        )
                    }
                }
        }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val newUnit = _state.value.userPreferences.temperatureUnit.toggle()
            preferencesRepository.updateTemperatureUnit(newUnit)
        }
    }

    private fun saveAsConfiguration() {
        val session = _state.value.parentSession ?: return

        viewModelScope.launch {
            val isDuplicate = checkDuplicateConfigurationUseCase(session)
            if (isDuplicate) {
                _state.update { it.copy(error = "A matching configuration already exists") }
                return@launch
            }

            val label = generateConfigurationLabelUseCase(
                vesselName = _state.value.vessel?.name ?: "",
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                brewingTime = session.brewingTime,
            )
            _state.update {
                it.copy(
                    showSaveConfigurationDialog = true,
                    suggestedConfigurationLabel = label,
                )
            }
        }
    }

    private fun confirmSaveConfiguration(label: String) {
        val session = _state.value.parentSession ?: return

        viewModelScope.launch {
            _state.update { it.copy(showSaveConfigurationDialog = false) }
            saveBrewingConfigurationUseCase(
                session = session,
                customLabel = label,
                skipRatingCheck = true,
            ).onFailure { e ->
                _state.update { it.copy(error = "Failed to save configuration: ${e.message}") }
            }
        }
    }

    private fun brewAgain() {
        val session = _state.value.parentSession ?: return

        viewModelScope.launch {
            brewAgainUseCase(session)
                .onSuccess {
                    _navEvents.send(SessionDetailNavEvent.NavigateToTimer(it.id))
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to create new session: ${e.message}") }
                }
        }
    }
}

sealed interface SessionDetailNavEvent {
    data object NavigateBack : SessionDetailNavEvent
    data class NavigateToEditSession(
        val sessionId: String,
        val editFullSession: Boolean = false
    ) : SessionDetailNavEvent

    data class NavigateToTeaDetails(val teaId: String) : SessionDetailNavEvent
    data class NavigateToTimer(val sessionId: String) : SessionDetailNavEvent
}