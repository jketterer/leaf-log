package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Dialog shown when the user tries to start a new session while one is already in progress.
 * Offers three resolution options: Resume, Complete & Start New, or Discard & Start New.
 */
@Composable
fun SessionInProgressDialog(
    teaName: String,
    vesselName: String,
    session: SessionWithSteeps,
    isProcessing: Boolean,
    onResume: () -> Unit,
    onCompleteAndContinue: () -> Unit,
    onDiscardAndContinue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steep = session.currentSteep
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = modifier,
        title = { Text("Session In Progress") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "You have an active session. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Session summary card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = teaName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Steep ${steep.number} · $vesselName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BrewingParamChip(
                                FeatherIcons.Thermometer,
                                formatTemperature(steep.temperatureCelsius),
                            )
                            BrewingParamChip(
                                FeatherIcons.Clock,
                                formatBrewingTime(steep.brewingTime.inWholeSeconds.toInt()),
                            )
                            BrewingParamChip(
                                FeatherIcons.Droplet,
                                formatVolume(steep.waterQuantityMl),
                            )
                        }
                    }
                }

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp,
                    )
                } else {
                    // Action buttons stacked vertically
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onResume,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Resume Session")
                        }

                        Button(
                            onClick = onCompleteAndContinue,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Complete & Start New")
                        }

                        TextButton(
                            onClick = onDiscardAndContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Discard & Start New")
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SessionInProgressDialogPreview() {
    LeafLogTheme {
        SessionInProgressDialog(
            teaName = "Dragon Well",
            vesselName = "Gaiwan",
            session = previewSession(brewingTime = 2.minutes + 30.seconds),
            isProcessing = false,
            onResume = {},
            onCompleteAndContinue = {},
            onDiscardAndContinue = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionInProgressDialogProcessingPreview() {
    LeafLogTheme {
        SessionInProgressDialog(
            teaName = "Sencha",
            vesselName = "Kyusu",
            session = previewSession(
                id = "session-2",
                brewingTime = 1.minutes,
                temperatureCelsius = 75.0,
                waterQuantityMl = 150.0,
            ),
            isProcessing = true,
            onResume = {},
            onCompleteAndContinue = {},
            onDiscardAndContinue = {},
            onDismiss = {},
        )
    }
}
