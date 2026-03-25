package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun TimerStopConfirmationDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Stop Timer?",
    confirmText: String = "Stop",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
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
            text = "The session will be saved as in progress. You can complete it later from the History screen.",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun TimerStopConfirmationDialogDestructivePreview() {
    LeafLogTheme {
        TimerStopConfirmationDialog(
            title = "Discard Session?",
            text = "The timer is complete but your session hasn't been saved. Your brewing data will be lost.",
            confirmText = "Discard",
            dismissText = "Stay",
            isDestructive = true,
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
