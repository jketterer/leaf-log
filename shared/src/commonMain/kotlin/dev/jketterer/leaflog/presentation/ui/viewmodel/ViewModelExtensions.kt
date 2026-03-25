package dev.jketterer.leaflog.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            .catch { e -> Logger.w("Preferences") { "Failed to load preferences: ${e.message}" } }
            .collect { preferences ->
                stateFlow.update { updateState(it, preferences) }
            }
    }
}

/**
 * Creates a ConfigurationSaveDelegate for handling brewing configuration save logic.
 *
 * This factory extension function creates a delegate that handles the common pattern of
 * saving brewing configurations and navigating after completion. It encapsulates the
 * multi-step flow of saving configurations and handling success/failure cases.
 *
 * @param T The type of the state
 * @param NavEvent The type of navigation events
 * @param saveBrewingConfigurationUseCase Use case for saving configurations
 * @param stateFlow The state flow to update
 * @param getSavedSession Lambda to extract the saved session from state
 * @param dismissDialog Lambda to dismiss the save configuration dialog
 * @param setError Lambda to set an error message in state
 * @param createSuccessNavEvent Lambda to create a navigation event on success
 * @param sendNavEvent Lambda to send navigation events
 *
 * Example usage:
 * ```kotlin
 * class MyViewModel(
 *     private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
 * ) : ViewModel() {
 *     private val _state = MutableStateFlow(MyState())
 *     private val _navigationEvents = Channel<MyNavEvent>()
 *
 *     private val configSaveDelegate = createConfigurationSaveDelegate(
 *         saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
 *         stateFlow = _state,
 *         getSavedSession = { it.savedSession },
 *         dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
 *         setError = { state, error -> state.copy(error = error) },
 *         createSuccessNavEvent = { session -> MyNavEvent.NavigateToSession(session.id) },
 *         sendNavEvent = { _navigationEvents.trySend(it) }
 *     )
 *
 *     fun onIntent(intent: MyIntent) {
 *         when (intent) {
 *             is MyIntent.SaveConfiguration ->
 *                 configSaveDelegate.saveConfiguration(intent.customLabel)
 *             is MyIntent.SkipSaveConfiguration ->
 *                 configSaveDelegate.skipSaveConfiguration()
 *         }
 *     }
 * }
 * ```
 */
fun <T, NavEvent> ViewModel.createConfigurationSaveDelegate(
    saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    stateFlow: MutableStateFlow<T>,
    getSavedSession: (T) -> TeaSession?,
    dismissDialog: (T) -> T,
    setError: (T, String) -> T,
    createSuccessNavEvent: (TeaSession) -> NavEvent,
    sendNavEvent: (NavEvent) -> Unit,
): ConfigurationSaveDelegate<T, NavEvent> {
    return ConfigurationSaveDelegate(
        viewModelScope = viewModelScope,
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = stateFlow,
        getSavedSession = getSavedSession,
        dismissDialog = dismissDialog,
        setError = setError,
        createSuccessNavEvent = createSuccessNavEvent,
        sendNavEvent = sendNavEvent,
    )
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
 * @param completeSessionUseCase Use case to complete a session
 * @param deleteSessionUseCase Use case to delete/discard a session
 * @param stateFlow The state flow to update
 * @param getDialogState Lambda to extract dialog state from ViewModel state
 * @param setDialogState Lambda to set dialog state in ViewModel state
 * @param setError Lambda to set an error message in state
 * @param onResume Lambda called when user chooses to resume the in-progress session
 * @param executePendingAction Lambda to execute the deferred action after conflict resolution
 */
fun <T, PendingAction> ViewModel.createInProgressSessionDelegate(
    getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    completeSessionUseCase: CompleteSessionUseCase,
    deleteSessionUseCase: DeleteSessionUseCase,
    stateFlow: MutableStateFlow<T>,
    getDialogState: (T) -> InProgressDialogState?,
    setDialogState: (T, InProgressDialogState?) -> T,
    setError: (T, String) -> T,
    onResume: (TeaSession) -> Unit,
    executePendingAction: suspend (PendingAction) -> Unit,
): InProgressSessionDelegate<T, PendingAction> {
    return InProgressSessionDelegate(
        viewModelScope = viewModelScope,
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        completeSessionUseCase = completeSessionUseCase,
        deleteSessionUseCase = deleteSessionUseCase,
        stateFlow = stateFlow,
        getDialogState = getDialogState,
        setDialogState = setDialogState,
        setError = setError,
        onResume = onResume,
        executePendingAction = executePendingAction,
    )
}
