package dev.jketterer.leaflog.presentation.ui.messages

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.resources.getString

/**
 * Shows [messages] in [hostState] in order, sending a tapped action's intent to [onAction]. A
 * message is marked shown only once its snackbar ends, so one interrupted by leaving the screen
 * comes back with it.
 */
@Composable
fun <I> UiMessageEffect(
    messages: UiMessages<I>,
    hostState: SnackbarHostState,
    onAction: (I) -> Unit = {},
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val pending by messages.pending.collectAsState()
    val current = pending.firstOrNull() ?: return
    LaunchedEffect(messages, hostState, current.id) {
        val action = current.message.action
        val result = hostState.showSnackbar(
            message = current.message.text.resolve(),
            actionLabel = action?.let { getString(it.label) },
            duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        messages.shown(current.id)
        if (result == SnackbarResult.ActionPerformed && action != null) {
            currentOnAction(action.intent)
        }
    }
}
