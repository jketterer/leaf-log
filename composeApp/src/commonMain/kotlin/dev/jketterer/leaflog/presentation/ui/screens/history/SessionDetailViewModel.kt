package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionDetailViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val brewAgainUseCase: BrewAgainUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailState())
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    fun onIntent(intent: SessionDetailIntent) {
        when (intent) {
            is SessionDetailIntent.LoadSession -> loadSession(intent.sessionId)
            is SessionDetailIntent.EditSessionClicked -> {
                // navigation handled by UI
            }

            is SessionDetailIntent.EditSteepClicked -> {
                // navigation handled by UI
            }

            is SessionDetailIntent.DeleteSessionClicked -> showDeleteConfirmation()
            is SessionDetailIntent.ConfirmDelete -> confirmDelete()
            is SessionDetailIntent.CancelDelete -> cancelDelete()
            is SessionDetailIntent.DeleteSteep -> deleteSteep(intent.steepId)
            is SessionDetailIntent.BrewAgainClicked -> brewAgain()
            is SessionDetailIntent.ViewTeaClicked -> {
                // navigation handled by UI
            }

            is SessionDetailIntent.BackClicked -> {
                // navigation handled by UI
            }
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
            .catch { e -> println("Failed to load tea: ${e.message}") }
            .firstOrNull()
            .let { tea ->
                _state.update { it.copy(tea = tea) }
                tea?.let { loadTeaType(it.teaTypeId) }
            }

        brewingVesselRepository.getByIdFlow(vesselId)
            .catch { e -> println("Failed to load vessel: ${e.message}") }
            .firstOrNull()
            .let { vessel ->
                _state.update { it.copy(vessel = vessel) }
            }
    }

    private suspend fun loadTeaType(teaTypeId: String) {
        teaTypeRepository.getByIdFlow(teaTypeId)
            .catch { e -> println("Failed to load tea type: ${e.message}") }
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

            deleteSessionUseCase(session.id)
                .onSuccess {
                    // navigation handled by UI - navigate back
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

    private fun deleteSteep(steepId: String) {
        viewModelScope.launch {
            deleteSessionUseCase(steepId)
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to delete steep: ${e.message}") }
                }
        }
    }

    private fun brewAgain() {
        val session = _state.value.parentSession ?: return

        viewModelScope.launch {
            brewAgainUseCase(session)
                .onSuccess {
                    // navigation handled by UI - navigate to Log Tea with new session
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to create new session: ${e.message}") }
                }
        }
    }
}