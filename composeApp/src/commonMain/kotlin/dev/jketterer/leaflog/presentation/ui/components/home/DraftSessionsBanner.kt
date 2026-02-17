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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.screens.home.InProgressSessionInfo
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Enhanced banner for in-progress sessions showing the most recent one with details.
 */
@Composable
fun InProgressSessionsBanner(
    inProgressInfo: InProgressSessionInfo?,
    totalInProgressCount: Int,
    timerProgress: Float? = null,
    onResumeClick: () -> Unit,
    onViewAllClick: () -> Unit,
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
                val timerStatus = inProgressInfo.session.timerStatus
                val timerRemainingMs = inProgressInfo.session.timerRemainingMs
                val totalDurationMs = inProgressInfo.session.brewingTime.inWholeMilliseconds

                // Determine if timer is complete (either from status or from progress)
                val isTimerComplete = timerStatus == TimerStatus.COMPLETE ||
                    (timerProgress != null && timerProgress >= 1f) ||
                    (timerRemainingMs != null && timerRemainingMs <= 0)

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
                            text = "Steep ${inProgressInfo.session.steepNumber}",
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
                    val targetProgress =
                        timerProgress ?: (1f - (timerRemainingMs.toFloat() / totalDurationMs))
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                        if (totalInProgressCount > 1) {
                            TextButton(onClick = onViewAllClick) {
                                Text("View all ($totalInProgressCount)")
                            }
                        }
                    }
                } else if (totalInProgressCount > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onViewAllClick) {
                            Text("View all ($totalInProgressCount)")
                        }
                    }
                }
            } else {
                // Fallback when we have count but no details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "You have $totalInProgressCount incomplete ${if (totalInProgressCount == 1) "session" else "sessions"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    TextButton(onClick = onViewAllClick) {
                        Text("View")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InProgressSessionsBannerPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // With info - timer paused
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionInfo(
                    session = TeaSession(
                        id = "1",
                        teaId = "tea1",
                        vesselId = "vessel1",
                        waterType = WaterType.FILTERED,
                        timestamp = now,
                        brewingTime = 3.minutes,
                        temperatureCelsius = 85,
                        waterQuantityMl = 150,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        steepNumber = 2,
                        status = SessionStatus.IN_PROGRESS,
                        timerStatus = TimerStatus.PAUSED,
                        timerRemainingMs = 90_000L,
                    ),
                    teaName = "Dragon Well Green Tea",
                    vesselName = "Gaiwan",
                ),
                totalInProgressCount = 3,
                onResumeClick = {},
                onViewAllClick = {},
            )

            // With info - timer complete
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionInfo(
                    session = TeaSession(
                        id = "2",
                        teaId = "tea2",
                        vesselId = "vessel1",
                        waterType = WaterType.FILTERED,
                        timestamp = now,
                        brewingTime = 2.minutes,
                        temperatureCelsius = 90,
                        waterQuantityMl = 100,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        steepNumber = 1,
                        status = SessionStatus.IN_PROGRESS,
                        timerStatus = TimerStatus.COMPLETE,
                        timerRemainingMs = 0L,
                    ),
                    teaName = "High Mountain Oolong",
                    vesselName = "Yixing Teapot",
                ),
                totalInProgressCount = 1,
                onResumeClick = {},
                onViewAllClick = {},
            )

            // Without timer state
            InProgressSessionsBanner(
                inProgressInfo = InProgressSessionInfo(
                    session = TeaSession(
                        id = "3",
                        teaId = "tea3",
                        vesselId = "vessel1",
                        waterType = WaterType.SPRING,
                        timestamp = now,
                        brewingTime = 5.minutes,
                        temperatureCelsius = 100,
                        waterQuantityMl = 200,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        steepNumber = 1,
                        status = SessionStatus.IN_PROGRESS,
                    ),
                    teaName = "Aged Pu-erh",
                    vesselName = "Gaiwan",
                ),
                totalInProgressCount = 2,
                onResumeClick = {},
                onViewAllClick = {},
            )
        }
    }
}
