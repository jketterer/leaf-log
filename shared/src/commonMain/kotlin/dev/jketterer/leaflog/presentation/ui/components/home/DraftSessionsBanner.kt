package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlin.time.Duration.Companion.minutes

/**
 * Enhanced banner for in-progress sessions showing the most recent one with details.
 */
@Composable
fun InProgressSessionsBanner(
    inProgressInfo: InProgressSessionDetails?,
    totalInProgressCount: Int,
    resolvedTimerStatus: TimerStatus? = null,
    timerProgress: Float? = null,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (inProgressInfo == null && totalInProgressCount == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            if (inProgressInfo != null) {
                // Caller resolves the status against the clock so this label and the screen the
                // button opens cannot disagree. Falling back to the stored status keeps previews
                // and any caller that has not resolved yet rendering something sensible.
                val steep = inProgressInfo.session.currentSteep
                val timerStatus = resolvedTimerStatus ?: steep.timerStatus
                val timerRemainingMs = steep.timerRemainingMs
                val totalDurationMs = steep.timerTotalMs ?: steep.brewingTime.inWholeMilliseconds

                val isTimerComplete = timerStatus == TimerStatus.COMPLETE

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = FeatherIcons.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = inProgressInfo.teaName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "Steep ${steep.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                    Button(onClick = onResumeClick) {
                        Text(if (isTimerComplete) "Finish" else "Resume")
                    }
                }

                // Show progress bar if timer has state
                if (timerStatus != null && timerRemainingMs != null && totalDurationMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // A brew that ran out while the app was closed still has a stale remaining
                    // time stored, so fill the bar from the resolved status rather than from it.
                    val targetProgress = when {
                        isTimerComplete -> 1f
                        else -> timerProgress ?: (1f - (timerRemainingMs.toFloat() / totalDurationMs))
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 300),
                        label = "progress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            isTimerComplete -> MaterialTheme.colorScheme.primary
                            timerStatus == TimerStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isTimerComplete -> "Ready to finish"
                            timerStatus == TimerStatus.PAUSED -> "Paused"
                            timerStatus == TimerStatus.RUNNING -> "Brewing..."
                            else -> "Ready to brew"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            } else {
                // Fallback when we have count but no details
                Text(
                    text = "You have $totalInProgressCount incomplete ${if (totalInProgressCount == 1) "session" else "sessions"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private fun previewInProgress(
    steeps: Int,
    timerStatus: TimerStatus?,
    timerRemainingMs: Long?,
): SessionWithSteeps {
    val session = previewSession(ratings = List(steeps) { null }, brewingTime = 3.minutes)
    val current = session.currentSteep.copy(
        timerStatus = timerStatus,
        timerRemainingMs = timerRemainingMs,
    )
    return session.copy(steeps = session.steeps.dropLast(1) + current)
}

@Preview(showBackground = true)
@Composable
private fun InProgressSessionsBannerPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // With info - timer paused
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionDetails(
                    session = previewInProgress(
                        steeps = 2,
                        timerStatus = TimerStatus.PAUSED,
                        timerRemainingMs = 90_000L,
                    ),
                    teaName = "Dragon Well Green Tea",
                    vesselName = "Gaiwan",
                ),
                totalInProgressCount = 1,
                onResumeClick = {},
            )

            // With info - timer complete
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionDetails(
                    session = previewInProgress(
                        steeps = 1,
                        timerStatus = TimerStatus.COMPLETE,
                        timerRemainingMs = 0L,
                    ),
                    teaName = "High Mountain Oolong",
                    vesselName = "Yixing Teapot",
                ),
                totalInProgressCount = 1,
                onResumeClick = {},
            )

            // Without timer state
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionDetails(
                    session = previewInProgress(steeps = 1, timerStatus = null, timerRemainingMs = null),
                    teaName = "Aged Pu-erh",
                    vesselName = "Gaiwan",
                ),
                totalInProgressCount = 1,
                onResumeClick = {},
            )
        }
    }
}
