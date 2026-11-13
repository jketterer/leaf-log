package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.notifications_off_text
import leaf_log.shared.generated.resources.notifications_off_title
import leaf_log.shared.generated.resources.ok
import org.jetbrains.compose.resources.stringResource

/** Why a timer may finish without a sound: notifications are off for the app. */
@Composable
fun NotificationsOffDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(Res.string.notifications_off_title)) },
        text = { NotificationsOffDialogContent() },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
    )
}

@Composable
private fun NotificationsOffDialogContent() {
    Text(stringResource(Res.string.notifications_off_text))
}

@Preview
@Composable
private fun NotificationsOffDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = stringResource(Res.string.notifications_off_title),
            buttons = {
                TextButton(onClick = {}) { Text(stringResource(Res.string.ok)) }
            },
        ) {
            NotificationsOffDialogContent()
        }
    }
}
