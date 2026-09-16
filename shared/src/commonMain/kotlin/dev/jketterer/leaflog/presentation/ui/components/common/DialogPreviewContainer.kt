package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Lays dialog content out the way an AlertDialog would, for use in @Preview functions.
 *
 * Previews host a real dialog in a separate window that the renderer does not capture, so a preview
 * of an AlertDialog comes out blank. Previewing the dialog's body inside this container renders it.
 */
@Composable
fun DialogPreviewContainer(
    title: String,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AlertDialogDefaults.titleContentColor,
            )
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                content = buttons,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogPreviewContainerPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Dialog Title",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                TextButton(onClick = {}) { Text("Confirm") }
            },
        ) {
            Text("Dialog body content")
        }
    }
}
