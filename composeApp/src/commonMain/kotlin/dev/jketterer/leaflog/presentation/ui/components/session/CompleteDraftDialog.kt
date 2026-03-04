package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Star
import compose.icons.fontawesomeicons.solid.Star
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Dialog for completing an in-progress session.
 * Allows user to add rating and notes to finish the session.
 */
@Composable
fun CompleteInProgressDialog(
    session: TeaSession,
    teaName: String,
    onComplete: (rating: Float?, notes: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableFloatStateOf(session.rating ?: 0f) }
    var notes by remember { mutableStateOf(session.notes ?: "") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Complete Session") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Session summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = teaName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Steep ${session.steepNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${session.temperatureCelsius}°C • ${session.waterQuantityMl}ml • ${session.brewingTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Rating selector
                Column {
                    Text(
                        text = "How was it?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingSelector(
                        rating = rating,
                        onRatingChange = { rating = it },
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("How did it taste?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onComplete(
                        rating.takeIf { it > 0f },
                        notes.takeIf { it.isNotBlank() },
                    )
                },
            ) {
                Text("Complete Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Save for Later")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CompleteInProgressDialogPreview() {
    LeafLogTheme {
        CompleteInProgressDialog(
            session = TeaSession(
                id = "session-1",
                teaId = "tea-1",
                vesselId = "gaiwan",
                waterType = WaterType.FILTERED,
                timestamp = Clock.System.now(),
                brewingTime = 2.minutes + 30.seconds,
                temperatureCelsius = 80.0,
                waterQuantityMl = 200.0,
                notes = "Initial notes from brewing",
                status = SessionStatus.IN_PROGRESS,
                syncStatus = SyncStatus.LOCAL_ONLY,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
            teaName = "Dragon Well",
            onComplete = { _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompleteInProgressDialogWithRatingPreview() {
    LeafLogTheme {
        CompleteInProgressDialog(
            session = TeaSession(
                id = "session-1",
                teaId = "tea-1",
                vesselId = "gaiwan",
                waterType = WaterType.FILTERED,
                timestamp = Clock.System.now(),
                brewingTime = 2.minutes,
                temperatureCelsius = 75.0,
                waterQuantityMl = 150.0,
                status = SessionStatus.IN_PROGRESS,
                syncStatus = SyncStatus.LOCAL_ONLY,
                rating = 4f,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
            teaName = "Sencha",
            onComplete = {_, _ -> },
            onDismiss = {},
        )
    }
}
