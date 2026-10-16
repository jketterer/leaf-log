package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimerStopConfirmationDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    confirmText: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
fun TimerResetConfirmationDialog(
    durationText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Timer?") },
        text = { Text("This will reset the timer to $durationText.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview
@Composable
private fun TimerStopConfirmationDialogPreview() {
    LeafLogTheme {
        TimerStopConfirmationDialog(
            title = "Complete Now?",
            text = "This will skip the remaining brew time and mark the steep as complete.",
            confirmText = "Complete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun TimerResetConfirmationDialogPreview() {
    LeafLogTheme {
        TimerResetConfirmationDialog(
            durationText = "2:00",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
