package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_changes_text
import leaf_log.shared.generated.resources.discard_changes_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before a form with unsaved changes is left, whether by the toolbar arrow or by system back.
 *
 * [content] receives the toolbar arrow's callback. [onDiscard] leaves the form, and runs straight
 * away when there is nothing to lose.
 *
 * Call it from the screen, not from the previewable content: the back handler needs the nav host's
 * dispatcher, which a @Preview doesn't have.
 */
@Composable
fun DiscardChangesGuardDialog(
    hasChanges: Boolean,
    onDiscard: () -> Unit,
    content: @Composable (onBack: () -> Unit) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val discard = {
        showDialog = false
        focusManager.clearFocus()
        onDiscard()
    }

    // Enabled only when there is something to lose, so predictive back keeps its peek animation
    // on a pristine form.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = hasChanges,
        onBackCompleted = { showDialog = true },
    )

    content { if (hasChanges) showDialog = true else discard() }

    if (showDialog) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.discard_changes_title),
            text = stringResource(Res.string.discard_changes_text),
            confirmLabel = stringResource(Res.string.discard),
            onConfirm = discard,
            onDismiss = { showDialog = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscardChangesDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = stringResource(Res.string.discard_changes_title),
            buttons = {
                TextButton(onClick = {}) { Text(stringResource(Res.string.cancel)) }
                TextButton(onClick = {}) {
                    Text(stringResource(Res.string.discard), color = MaterialTheme.colorScheme.error)
                }
            },
        ) {
            Text(stringResource(Res.string.discard_changes_text))
        }
    }
}
