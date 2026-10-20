package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

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
private fun TimerResetConfirmationDialogPreview() {
    LeafLogTheme {
        TimerResetConfirmationDialog(
            durationText = "2:00",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
