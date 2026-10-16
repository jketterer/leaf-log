package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import org.jetbrains.compose.resources.stringResource

/** Asks before an action that loses data. The confirm button is colored as an error. */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(Res.string.cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { DestructiveButton(confirmLabel, onConfirm) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}

@Composable
private fun DestructiveButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDestructiveDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = stringResource(Res.string.delete_session_title),
            buttons = {
                TextButton(onClick = {}) { Text(stringResource(Res.string.cancel)) }
                DestructiveButton(stringResource(Res.string.delete), onClick = {})
            },
        ) {
            Text(stringResource(Res.string.delete_session_text))
        }
    }
}
