package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Dialog for logging a session as already completed, capturing a rating and notes.
 */
@Composable
fun CompleteSessionDialog(
    notes: String,
    rating: Float,
    isSaving: Boolean,
    onNotesChanged: (String) -> Unit,
    onRatingChanged: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Complete Session") },
        text = {
            CompleteSessionDialogContent(
                notes = notes,
                rating = rating,
                onNotesChanged = onNotesChanged,
                onRatingChanged = onRatingChanged,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
            ) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CompleteSessionDialogContent(
    notes: String,
    rating: Float,
    onNotesChanged: (String) -> Unit,
    onRatingChanged: (Float) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "How was this brew?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Rating",
            style = MaterialTheme.typography.labelMedium,
        )
        RatingSelector(
            rating = rating,
            onRatingChange = onRatingChanged,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompleteSessionDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Complete Session",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}) { Text("Complete") }
            },
        ) {
            CompleteSessionDialogContent(
                notes = "Sweet, nutty finish",
                rating = 4f,
                onNotesChanged = {},
                onRatingChanged = {},
            )
        }
    }
}
