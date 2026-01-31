package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VesselListViewModel(
    private val brewingVesselRepository: BrewingVesselRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VesselListState())
    val state: StateFlow<VesselListState> = _state.asStateFlow()

    private val _navigationEvent = MutableStateFlow<VesselListNavigationEvent?>(null)
    val navigationEvent: StateFlow<VesselListNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadVessels()
    }

    fun onIntent(intent: VesselListIntent) {
        when (intent) {
            is VesselListIntent.LoadVessels -> loadVessels()
            is VesselListIntent.VesselClicked -> navigateToVesselDetail(intent.vesselId)
            is VesselListIntent.AddVesselClicked -> navigateToAddVessel()
            is VesselListIntent.ClearError -> clearError()
            is VesselListIntent.BackClicked -> navigateBack()
        }
    }

    private fun loadVessels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                brewingVesselRepository.getAllFlow().collect { vessels ->
                    _state.update {
                        it.copy(
                            vessels = vessels,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load vessels"
                    )
                }
            }
        }
    }

    private fun navigateToVesselDetail(vesselId: String) {
        _navigationEvent.value = VesselListNavigationEvent.NavigateToVesselDetail(vesselId)
    }

    private fun navigateToAddVessel() {
        _navigationEvent.value = VesselListNavigationEvent.NavigateToAddVessel
    }

    private fun navigateBack() {
        _navigationEvent.value = VesselListNavigationEvent.NavigateBack
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }
}

sealed interface VesselListNavigationEvent {
    data class NavigateToVesselDetail(val vesselId: String) : VesselListNavigationEvent
    data object NavigateToAddVessel : VesselListNavigationEvent
    data object NavigateBack : VesselListNavigationEvent
}
