package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.vessel.DeleteBrewingVesselUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VesselDetailViewModel(
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val deleteBrewingVesselUseCase: DeleteBrewingVesselUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(VesselDetailState())
    val state: StateFlow<VesselDetailState> = _state.asStateFlow()

    private val _navigationEvent = MutableStateFlow<VesselDetailNavigationEvent?>(null)
    val navigationEvent: StateFlow<VesselDetailNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(volumeUnit = prefs.volumeUnit) },
        )
    }

    fun onIntent(intent: VesselDetailIntent) {
        when (intent) {
            is VesselDetailIntent.LoadVessel -> loadVessel(intent.vesselId)
            is VesselDetailIntent.EditVesselClicked -> navigateToEditVessel()
            is VesselDetailIntent.DeleteVesselClicked -> showDeleteConfirmation()
            is VesselDetailIntent.ConfirmDelete -> deleteVessel()
            is VesselDetailIntent.CancelDelete -> hideDeleteConfirmation()
            is VesselDetailIntent.BackClicked -> navigateBack()
        }
    }

    private fun loadVessel(vesselId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Load vessel
                val vessel = brewingVesselRepository.getById(vesselId)

                // Count total vessels
                val totalVesselCount = brewingVesselRepository.getAll().size

                // Count sessions using this vessel
                val sessionCount = teaSessionRepository.getByVesselId(vesselId).size

                _state.update {
                    it.copy(
                        vessel = vessel,
                        sessionCount = sessionCount,
                        totalVesselCount = totalVesselCount,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load vessel"
                    )
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun hideDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun deleteVessel() {
        val vesselId = _state.value.vessel?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmation = false, isLoading = true) }

            deleteBrewingVesselUseCase(vesselId)
                .onSuccess {
                    _navigationEvent.value = VesselDetailNavigationEvent.NavigateBackAfterDelete
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to delete vessel"
                        )
                    }
                }
        }
    }

    private fun navigateToEditVessel() {
        val vesselId = _state.value.vessel?.id ?: return
        _navigationEvent.value = VesselDetailNavigationEvent.NavigateToEditVessel(vesselId)
    }

    private fun navigateBack() {
        _navigationEvent.value = VesselDetailNavigationEvent.NavigateBack
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }
}

sealed interface VesselDetailNavigationEvent {
    data class NavigateToEditVessel(val vesselId: String) : VesselDetailNavigationEvent
    data object NavigateBack : VesselDetailNavigationEvent
    data object NavigateBackAfterDelete : VesselDetailNavigationEvent
}
