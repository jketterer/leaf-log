package dev.jketterer.leaflog.presentation.ui.viewmodel

import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegate class that handles saving brewing configurations and navigation.
 *
 * This delegate encapsulates the common pattern of:
 * 1. Saving a brewing configuration (with optional custom label)
 * 2. Dismissing the save dialog
 * 3. Navigating to the appropriate screen
 * 4. Handling errors gracefully
 *
 * It's designed to be generic and work with any state type and navigation event type.
 * Create instances using the [createConfigurationSaveDelegate] extension function.
 *
 * @param T The type of the state
 * @param NavEvent The type of navigation events
 * @param viewModelScope The coroutine scope for launching async operations
 * @param saveBrewingConfigurationUseCase Use case for saving configurations
 * @param stateFlow The state flow to update
 * @param getSavedSession Lambda to extract the saved session from state
 * @param dismissDialog Lambda to dismiss the save configuration dialog
 * @param setError Lambda to set an error message in state
 * @param createSuccessNavEvent Lambda to create a navigation event on success
 * @param sendNavEvent Lambda to send navigation events
 */
class ConfigurationSaveDelegate<T, NavEvent>(
    private val viewModelScope: CoroutineScope,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val stateFlow: MutableStateFlow<T>,
    private val getSavedSession: (T) -> TeaSession?,
    private val dismissDialog: (T) -> T,
    private val setError: (T, String) -> T,
    private val createSuccessNavEvent: (TeaSession) -> NavEvent,
    private val sendNavEvent: (NavEvent) -> Unit,
) {
    /**
     * Saves the brewing configuration with an optional custom label.
     *
     * This method:
     * 1. Extracts the saved session from current state
     * 2. Calls the save use case with the session and custom label
     * 3. Dismisses the save dialog
     * 4. Navigates to the success screen
     * 5. Handles errors by setting error state but still navigating
     *
     * @param customLabel Optional custom label for the configuration
     */
    fun saveConfiguration(customLabel: String?) {
        viewModelScope.launch {
            val session = getSavedSession(stateFlow.value)
            if (session != null) {
                saveBrewingConfigurationUseCase(
                    session = session,
                    customLabel = customLabel,
                ).onFailure { error ->
                    stateFlow.update { setError(it, error.message ?: "Failed to save configuration") }
                }
            }

            // Dismiss dialog and navigate regardless of save result
            stateFlow.update { dismissDialog(it) }
            session?.let { sendNavEvent(createSuccessNavEvent(it)) }
        }
    }

    /**
     * Skips saving the configuration and navigates directly.
     *
     * This method:
     * 1. Dismisses the save dialog
     * 2. Navigates to the success screen without saving
     */
    fun skipSaveConfiguration() {
        viewModelScope.launch {
            stateFlow.update { dismissDialog(it) }
            val session = getSavedSession(stateFlow.value)
            session?.let { sendNavEvent(createSuccessNavEvent(it)) }
        }
    }
}
