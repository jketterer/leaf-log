package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeaDetailViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteTeaUseCase: DeleteTeaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaDetailState())
    val state: StateFlow<TeaDetailState> = _state.asStateFlow()

    fun onIntent(intent: TeaDetailIntent) {
        when (intent) {
            is TeaDetailIntent.LoadTea -> loadTea(intent.teaId)
            is TeaDetailIntent.EditTeaClicked -> {
                // navigation handled by UI
            }

            is TeaDetailIntent.DeleteTeaClicked -> showDeleteConfirmation()
            is TeaDetailIntent.ConfirmDelete -> confirmDelete()
            is TeaDetailIntent.CancelDelete -> cancelDelete()
            is TeaDetailIntent.ToggleFavorite -> toggleFavorite()
            is TeaDetailIntent.SessionClicked -> {
                // navigation handled by UI
            }

            is TeaDetailIntent.BrewThisTeaClicked -> {
                // navigation handled by UI
            }

            is TeaDetailIntent.BackClicked -> {
                // navigation handled by UI
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
}