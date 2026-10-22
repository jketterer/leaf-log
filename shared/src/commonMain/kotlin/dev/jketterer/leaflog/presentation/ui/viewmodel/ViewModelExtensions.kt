package dev.jketterer.leaflog.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * Loads user preferences and updates the ViewModel state automatically.
 *
 * This extension function handles the common pattern of collecting preferences from a repository
 * and updating the ViewModel state. It launches a coroutine in the viewModelScope, collects
 * preferences, and updates the state using the provided lambda.
 *
 * @param T The type of the state
 * @param preferencesRepository Repository providing user preferences
 * @param stateFlow The state flow to update
 * @param updateState Lambda that takes current state and preferences, returns updated state
 *
 * Example usage:
 * ```kotlin
 * class MyViewModel(
 *     private val preferencesRepository: PreferencesRepository,
 * ) : ViewModel() {
 *     private val _state = MutableStateFlow(MyState())
 *
 *     init {
 *         loadPreferences(
 *             preferencesRepository = preferencesRepository,
 *             stateFlow = _state,
 *             updateState = { state, prefs -> state.copy(userPreferences = prefs) }
 *         )
 *     }
 * }
 * ```
 */
fun <T> ViewModel.loadPreferences(
    preferencesRepository: PreferencesRepository,
    stateFlow: MutableStateFlow<T>,
    updateState: (T, UserPreferences) -> T,
) {
    viewModelScope.launch {
        preferencesRepository.getPreferencesFlow()
            .catch { e -> Logger.w(tag = "Preferences") { "Failed to load preferences: ${e.message}" } }
            .collect { preferences ->
                stateFlow.update { updateState(it, preferences) }
            }
    }
}

/**
 * Creates an InProgressSessionDelegate for handling in-progress session conflict resolution.
 *
 * This factory extension function creates a delegate that handles the common pattern of
 * checking for in-progress sessions before starting a new one. When a conflict is found,
 * it shows a dialog with options to resume, complete, or discard the existing session.
 *
 * @param T The type of the state
 * @param PendingAction The ViewModel-specific pending action type
 * @param getInProgressSessionInfoUseCase Use case to fetch in-progress session info
 * @param finishSessionUseCase Use case to finish the in-progress session
 * @param discardSteepUseCase Use case to discard the steep being brewed
 * @param timerService Stops the session's timer and withdraws its reminder once it's resolved
 * @param stateFlow The state flow to update
 * @param getDialogState Lambda to extract dialog state from ViewModel state
 * @param setDialogState Lambda to set dialog state in ViewModel state
 * @param onFailure Reports a failure, as [UiMessages.failure] does
 * @param onResume Lambda called when user chooses to resume the in-progress session
 * @param executePendingAction Lambda to execute the deferred action after conflict resolution
 */
fun <T, PendingAction> ViewModel.createInProgressSessionDelegate(
    getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    finishSessionUseCase: FinishSessionUseCase,
    discardSteepUseCase: DiscardSteepUseCase,
    timerService: TimerService,
    stateFlow: MutableStateFlow<T>,
    getDialogState: (T) -> InProgressDialogState?,
    setDialogState: (T, InProgressDialogState?) -> T,
    onFailure: (error: Throwable, couldnt: StringResource) -> Unit,
    onResume: (SessionWithSteeps) -> Unit,
    executePendingAction: suspend (PendingAction) -> Unit,
): InProgressSessionDelegate<T, PendingAction> {
    return InProgressSessionDelegate(
        viewModelScope = viewModelScope,
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = stateFlow,
        getDialogState = getDialogState,
        setDialogState = setDialogState,
        onFailure = onFailure,
        onResume = onResume,
        executePendingAction = executePendingAction,
    )
}
