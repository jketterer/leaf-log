package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.DeleteBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateBack
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToEditTea
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToLogTea
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailNavigationEvent.NavigateToSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeaDetailViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteTeaUseCase: DeleteTeaUseCase,
    private val deleteBrewingConfigurationUseCase: DeleteBrewingConfigurationUseCase,
    private val updateBrewingConfigurationUseCase: UpdateBrewingConfigurationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaDetailState())
    val state: StateFlow<TeaDetailState> = _state.asStateFlow()

    private val _navEvents = Channel<TeaDetailNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferencesFlow()
                .catchError("Failed to load preferences")
                .collect { preferences ->
                    _state.update { it.copy(userPreferences = preferences) }
                }
        }
    }

    fun onIntent(intent: TeaDetailIntent) {
        when (intent) {
            is TeaDetailIntent.LoadTea -> loadTea(intent.teaId)
            is TeaDetailIntent.DeleteTeaClicked -> showDeleteConfirmation()
            is TeaDetailIntent.ConfirmDelete -> confirmDelete()
            is TeaDetailIntent.CancelDelete -> cancelDelete()
            is TeaDetailIntent.ToggleFavorite -> toggleFavorite()

            is TeaDetailIntent.EditTeaClicked -> _navEvents.trySend(NavigateToEditTea)
            is TeaDetailIntent.SessionClicked -> handleSessionClick(intent.sessionId)
            is TeaDetailIntent.EditSessionClicked -> _navEvents.trySend(NavigateToSession(intent.sessionId))
            is TeaDetailIntent.BrewThisTeaClicked -> _navEvents.trySend(NavigateToLogTea(intent.vesselId))
            is TeaDetailIntent.ViewAllSessionsClicked -> {
                val teaId = _state.value.tea?.id ?: return
                _navEvents.trySend(TeaDetailNavigationEvent.NavigateToHistory(teaId))
            }
            is TeaDetailIntent.BackClicked -> _navEvents.trySend(NavigateBack)

            is TeaDetailIntent.EditConfigurationClicked -> showEditConfigDialog(intent.configId)
            is TeaDetailIntent.DeleteConfigurationClicked -> deleteConfiguration(intent.configId)
            is TeaDetailIntent.SaveConfigurationChanges -> saveConfigurationChanges(intent)
            is TeaDetailIntent.DismissEditConfigDialog -> dismissEditConfigDialog()

            is TeaDetailIntent.AddConfigurationClicked -> {
                val teaId = _state.value.tea?.id ?: return
                _navEvents.trySend(TeaDetailNavigationEvent.NavigateToCreateConfig(teaId))
            }
        }
    }

    private fun loadTea(teaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                teaRepository.getByIdFlow(teaId)
                    .catchError("Failed to load tea")
                    .collect { tea ->
                        if (tea != null) {
                            _state.update { it.copy(tea = tea, isLoading = false) }
                            loadTeaType(tea.teaTypeId)
                            loadRecentSessions(teaId)
                            loadConfigurations(teaId)
                            loadVessels()
                        } else {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Tea not found",
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load tea: ${e.message}"
                    )
                }
            }
        }
    }

    private fun <T> Flow<T>.catchError(message: String): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false, error = "$message: ${e.message}") }
        }
    }

    private fun loadTeaType(teaTypeId: String) {
        viewModelScope.launch {
            teaTypeRepository.getByIdFlow(teaTypeId)
                .catchError("Failed to load tea type")
                .collect { teaType ->
                    _state.update { it.copy(teaType = teaType) }
                }
        }
    }

    private fun loadRecentSessions(teaId: String) {
        viewModelScope.launch {
            teaSessionRepository.getByTeaIdFlow(teaId)
                .catchError("Failed to load sessions")
                .map { it.take(5) }
                .collect { sessions ->
                    _state.update { it.copy(recentSessions = sessions) }
                }
        }
    }

    private fun loadConfigurations(teaId: String) {
        viewModelScope.launch {
            brewingConfigurationRepository.getByTeaIdFlow(teaId)
                .catchError("Failed to load configurations")
                .collect { configurations ->
                    _state.update { it.copy(configurations = configurations) }
                }
        }
    }

    private fun loadVessels() {
        viewModelScope.launch {
            brewingVesselRepository.getAllFlow()
                .catchError("Failed to load vessels")
                .collect { vessels ->
                    _state.update { it.copy(vessels = vessels) }
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

            deleteTeaUseCase(tea.id)
                .onSuccess { /* navigation handled by UI */ }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete tea: ${e.message}"
                        )
                    }
                }
        }
    }

    private fun toggleFavorite() {
        val tea = _state.value.tea ?: return

        viewModelScope.launch {
            toggleFavoriteUseCase(tea)
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to toggle favorite: ${e.message}") }
                }
        }
    }

    private fun showEditConfigDialog(configId: String) {
        viewModelScope.launch {
            val config = brewingConfigurationRepository.getById(configId)
            _state.update {
                it.copy(
                    editingConfig = config,
                    showEditConfigDialog = true
                )
            }
        }
    }

    private fun dismissEditConfigDialog() {
        _state.update {
            it.copy(
                showEditConfigDialog = false,
                editingConfig = null
            )
        }
    }

    private fun deleteConfiguration(configId: String) {
        viewModelScope.launch {
            deleteBrewingConfigurationUseCase(configId)
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to delete configuration: ${e.message}") }
                }
        }
    }

    private fun saveConfigurationChanges(intent: TeaDetailIntent.SaveConfigurationChanges) {
        val config = _state.value.editingConfig ?: return

        viewModelScope.launch {
            updateBrewingConfigurationUseCase(
                configurationId = config.id,
                label = intent.label,
                teaQuantityGrams = intent.teaQuantityGrams,
                waterQuantityMl = intent.waterQuantityMl,
                temperatureCelsius = intent.temperatureCelsius,
                brewingTime = intent.brewingTime,
                waterType = intent.waterType,
                isActive = intent.isActive
            )
                .onSuccess {
                    _state.update {
                        it.copy(
                            showEditConfigDialog = false,
                            editingConfig = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to update configuration: ${e.message}")
                    }
                }
        }
    }

    private fun handleSessionClick(sessionId: String) {
        viewModelScope.launch {
            val session = teaSessionRepository.getById(sessionId)
            if (session?.status == SessionStatus.IN_PROGRESS) {
                _navEvents.trySend(TeaDetailNavigationEvent.NavigateToTimer(sessionId))
            } else {
                _navEvents.trySend(NavigateToSession(sessionId))
            }
        }
    }
}

sealed interface TeaDetailNavigationEvent {
    data object NavigateBack : TeaDetailNavigationEvent
    data class NavigateToSession(val sessionId: String) : TeaDetailNavigationEvent
    data class NavigateToEditSession(val sessionId: String) : TeaDetailNavigationEvent
    data class NavigateToLogTea(val vesselId: String? = null) : TeaDetailNavigationEvent
    data class NavigateToTimer(val sessionId: String) : TeaDetailNavigationEvent
    data object NavigateToEditTea : TeaDetailNavigationEvent
    data class NavigateToHistory(val teaId: String) : TeaDetailNavigationEvent
    data class NavigateToCreateConfig(val teaId: String) : TeaDetailNavigationEvent
}