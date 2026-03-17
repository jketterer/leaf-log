package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.SteepParameterCard
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerControlButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerStopConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Timer Screen - brewing countdown timer.
 */
@Composable
fun TimerScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSteepComplete: (String) -> Unit,
    viewModel: TimerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(TimerIntent.Initialize(sessionId))
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is TimerNavEvent.NavigateToSteepComplete -> {
                    onNavigateToSteepComplete(event.sessionId)
                }

                is TimerNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    TimerContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerContent(
    state: TimerScreenState,
    onIntent: (TimerIntent) -> Unit,
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(state.tea?.name ?: "")
                    state.vessel?.name?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onIntent(TimerIntent.BackClicked) }) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(FeatherIcons.MoreVertical, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Complete Now") },
                        onClick = {
                            showOverflowMenu = false
                            onIntent(TimerIntent.CompleteNow)
                        },
                        leadingIcon = {
                            Icon(
                                FeatherIcons.CheckCircle,
                                contentDescription = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Cancel Session",
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onIntent(TimerIntent.DiscardSession)
                        },
                        leadingIcon = {
                            Icon(
                                FeatherIcons.Trash2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            },
        )
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                TimerRunningContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // Complete now confirmation dialog
    if (state.showCompleteNowConfirmation) {
        TimerStopConfirmationDialog(
            title = "Complete Now?",
            text = "This will skip the remaining brew time and mark the steep as complete.",
            confirmText = "Complete",
            onConfirm = { onIntent(TimerIntent.ConfirmCompleteNow) },
            onDismiss = { onIntent(TimerIntent.CancelCompleteNow) },
        )
    }

    // Discard session confirmation dialog
    if (state.showDiscardSessionConfirmation) {
        TimerStopConfirmationDialog(
            title = "Cancel Session?",
            text = "This will permanently delete the session. This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = { onIntent(TimerIntent.ConfirmStop) },
            onDismiss = { onIntent(TimerIntent.CancelStop) },
        )
    }

    // Reset confirmation dialog
    if (state.showResetConfirmation) {
        TimerResetConfirmationDialog(
            durationText = "${state.session?.brewingTime}",
            onConfirm = { onIntent(TimerIntent.ConfirmReset) },
            onDismiss = { onIntent(TimerIntent.CancelReset) },
        )
    }

    // Edit session parameters sheet
    if (state.showEditSheet && state.editWaterType != null) {
        EditSessionParametersSheet(
            temperatureValue = state.editTemperatureCelsius,
            waterQuantityValue = state.editWaterQuantityMl,
            teaQuantityValue = state.editTeaQuantityGrams,
            isTeaBag = state.editIsTeaBag,
            waterType = state.editWaterType,
            temperatureUnit = state.userPreferences.temperatureUnit,
            volumeUnit = state.userPreferences.volumeUnit,
            onTemperatureChanged = { onIntent(TimerIntent.EditTemperatureChanged(it)) },
            onWaterQuantityChanged = { onIntent(TimerIntent.EditWaterQuantityChanged(it)) },
            onTeaQuantityChanged = { onIntent(TimerIntent.EditTeaQuantityChanged(it)) },
            onTeaBagModeChanged = { onIntent(TimerIntent.EditTeaBagModeChanged(it)) },
            onWaterTypeSelected = { onIntent(TimerIntent.EditWaterTypeChanged(it)) },
            onToggleTemperatureUnit = { onIntent(TimerIntent.ToggleTemperatureUnit) },
            onToggleVolumeUnit = { onIntent(TimerIntent.ToggleVolumeUnit) },
            onSave = { onIntent(TimerIntent.ConfirmEditSession) },
            onDismiss = { onIntent(TimerIntent.CancelEditSession) },
        )
    }
}

@Composable
private fun TimerRunningContent(
    state: TimerScreenState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.padding(24.dp)) {
        // Shrink the ring on smaller screens (e.g. iPhone SE) so control buttons stay visible.
        // 420.dp accounts for the approximate height of all other fixed content + gaps.
        val ringSize = (maxHeight - 420.dp).coerceIn(100.dp, 240.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Brewing parameters card
            if (state.session != null) {
                SteepParameterCard(
                    session = state.session,
                    userPreferences = state.userPreferences,
                    onEditClick = { onIntent(TimerIntent.EditSession) },
                )
            }

            // Circular timer ring + quick adjustment buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularTimerRing(
                    progress = state.timerState.progress,
                    timeText = state.formattedTime,
                    modifier = Modifier.size(ringSize),
                )
                QuickAdjustButtons(
                    onAdjust = { adjustment ->
                        onIntent(TimerIntent.AdjustTime(adjustment))
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Control buttons
            TimerControlButtons(
                isRunning = state.timerState.isRunning,
                isPaused = state.timerState.status == TimerStatus.PAUSED,
                onStartClick = { onIntent(TimerIntent.StartTimer) },
                onPauseClick = { onIntent(TimerIntent.PauseTimer) },
                onResumeClick = { onIntent(TimerIntent.ResumeTimer) },
                onResetClick = { onIntent(TimerIntent.ResetTimer) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenRunningPreview() {
    LeafLogTheme {
        TimerRunningContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    teaName = "Dragon Well",
                    steepNumber = 1,
                    totalDuration = 2.minutes,
                    remainingDuration = 1.minutes + 30.seconds,
                    status = TimerStatus.RUNNING,
                    startedAt = Clock.System.now(),
                ),
                session = TeaSession(
                    id = "session-1",
                    teaId = "tea-1",
                    vesselId = "gaiwan",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 2.minutes,
                    temperatureCelsius = 80.0,
                    waterQuantityMl = 200.0,
                    status = SessionStatus.IN_PROGRESS,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPausedPreview() {
    LeafLogTheme {
        TimerRunningContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    teaName = "Dragon Well",
                    steepNumber = 1,
                    totalDuration = 2.minutes,
                    remainingDuration = 1.minutes + 23.seconds,
                    status = TimerStatus.PAUSED,
                    startedAt = Clock.System.now(),
                    pausedAt = Clock.System.now(),
                ),
                session = TeaSession(
                    id = "session-1",
                    teaId = "tea-1",
                    vesselId = "gaiwan",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 2.minutes,
                    temperatureCelsius = 80.0,
                    waterQuantityMl = 200.0,
                    status = SessionStatus.IN_PROGRESS,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
            ),
            onIntent = {},
        )
    }
}

