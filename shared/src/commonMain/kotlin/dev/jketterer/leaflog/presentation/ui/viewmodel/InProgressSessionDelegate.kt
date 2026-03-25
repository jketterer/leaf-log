package dev.jketterer.leaflog.presentation.ui.viewmodel

import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the in-progress session conflict resolution dialog.
 */
data class InProgressDialogState(
    val session: TeaSession,
    val teaName: String,
    val vesselName: String,
    val isProcessing: Boolean = false,
)

/**
 * Delegate that handles the in-progress session conflict resolution flow.
 *
 * When a user attempts an action that conflicts with an existing in-progress session
 * (e.g., starting a new session, brew again), this delegate:
 * 1. Checks if an in-progress session exists
 * 2. If not, executes the pending action directly
 * 3. If so, shows a dialog with three options: Resume, Complete & Continue, Discard & Continue
 * 4. On resolution, executes the original pending action
 *
 * Follows the same pattern as [ConfigurationSaveDelegate].
 *
 * @param T The type of the ViewModel state
 * @param PendingAction The ViewModel-specific pending action type
 */
class InProgressSessionDelegate<T, PendingAction>(
    private val viewModelScope: CoroutineScope,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val stateFlow: MutableStateFlow<T>,
    private val getDialogState: (T) -> InProgressDialogState?,
    private val setDialogState: (T, InProgressDialogState?) -> T,
    private val setError: (T, String) -> T,
    private val onResume: (TeaSession) -> Unit,
    private val executePendingAction: suspend (PendingAction) -> Unit,
) {
    private var pendingAction: PendingAction? = null

    /**
     * Checks for an in-progress session. If none exists, executes the action directly.
     * If one exists, stores the pending action and shows the conflict dialog.
     */
    fun checkAndProceed(action: PendingAction) {
        viewModelScope.launch {
            val inProgress = getInProgressSessionInfoUseCase()
            if (inProgress == null) {
                executePendingAction(action)
            } else {
                pendingAction = action
                stateFlow.update {
                    setDialogState(
                        it,
                        InProgressDialogState(
                            session = inProgress.session,
                            teaName = inProgress.teaName,
                            vesselName = inProgress.vesselName,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Navigates to the in-progress session (timer or steep-complete) and clears the dialog.
     */
    fun resume() {
        val session = getDialogState(stateFlow.value)?.session ?: return
        stateFlow.update { setDialogState(it, null) }
        pendingAction = null
        onResume(session)
    }

    /**
     * Completes the in-progress session and then executes the original pending action.
     */
    fun completeAndContinue() {
        val dialogState = getDialogState(stateFlow.value) ?: return
        val action = pendingAction ?: return

        viewModelScope.launch {
            stateFlow.update { setDialogState(it, dialogState.copy(isProcessing = true)) }

            val result = completeSessionUseCase(dialogState.session)
            if (result.isFailure) {
                stateFlow.update {
                    var s = setError(it, "Failed to complete session: ${result.exceptionOrNull()?.message}")
                    s = setDialogState(s, dialogState.copy(isProcessing = false))
                    s
                }
                return@launch
            }

            stateFlow.update { setDialogState(it, null) }
            pendingAction = null
            executePendingAction(action)
        }
    }

    /**
     * Discards the in-progress session and then executes the original pending action.
     * DeleteSessionUseCase handles stopping the timer.
     */
    fun discardAndContinue() {
        val dialogState = getDialogState(stateFlow.value) ?: return
        val action = pendingAction ?: return

        viewModelScope.launch {
            stateFlow.update { setDialogState(it, dialogState.copy(isProcessing = true)) }

            val result = deleteSessionUseCase(dialogState.session.id)
            if (result.isFailure) {
                stateFlow.update {
                    var s = setError(it, "Failed to discard session: ${result.exceptionOrNull()?.message}")
                    s = setDialogState(s, dialogState.copy(isProcessing = false))
                    s
                }
                return@launch
            }

            stateFlow.update { setDialogState(it, null) }
            pendingAction = null
            executePendingAction(action)
        }
    }

    /**
     * Dismisses the dialog without taking any action.
     */
    fun dismissDialog() {
        stateFlow.update { setDialogState(it, null) }
        pendingAction = null
    }
}
