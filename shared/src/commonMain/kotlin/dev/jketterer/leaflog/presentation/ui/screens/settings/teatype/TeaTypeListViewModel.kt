package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_tea_types

class TeaTypeListViewModel(
    private val teaTypeRepository: TeaTypeRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaTypeListState())
    val state: StateFlow<TeaTypeListState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    private val _navigationEvent = MutableStateFlow<TeaTypeListNavigationEvent?>(null)
    val navigationEvent: StateFlow<TeaTypeListNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadTeaTypes()
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
    }

    fun onIntent(intent: TeaTypeListIntent) {
        when (intent) {
            is TeaTypeListIntent.TeaTypeClicked -> navigateToEdit(intent.teaTypeId)
            is TeaTypeListIntent.AddClicked -> navigateToAdd()
            is TeaTypeListIntent.BackClicked -> navigateBack()
        }
    }

    private fun loadTeaTypes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                teaTypeRepository.getAllFlow().collect { teaTypes ->
                    _state.update {
                        it.copy(
                            teaTypes = teaTypes.sortedBy { type -> type.displayOrder },
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_tea_types)
            }
        }
    }

    private fun navigateToEdit(teaTypeId: String) {
        _navigationEvent.value = TeaTypeListNavigationEvent.NavigateToEdit(teaTypeId)
    }

    private fun navigateToAdd() {
        _navigationEvent.value = TeaTypeListNavigationEvent.NavigateToAdd
    }

    private fun navigateBack() {
        _navigationEvent.value = TeaTypeListNavigationEvent.NavigateBack
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }
}

sealed interface TeaTypeListNavigationEvent {
    data class NavigateToEdit(val teaTypeId: String) : TeaTypeListNavigationEvent
    data object NavigateToAdd : TeaTypeListNavigationEvent
    data object NavigateBack : TeaTypeListNavigationEvent
}
