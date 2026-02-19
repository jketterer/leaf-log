package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Edit
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.RotateCw
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepParameterDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerControlButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerStopConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Timer Screen - brewing countdown timer.
 */
@Composable
fun TimerScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNextSteep: (String) -> Unit,
    onNavigateToComplete: (String) -> Unit,
    viewModel: TimerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(TimerIntent.Initialize(sessionId))
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is TimerNavEvent.NavigateToNextSteep -> {
                    onNavigateToNextSteep(event.sessionId)
                }

                is TimerNavEvent.NavigateToComplete -> {
                    onNavigateToComplete(event.sessionId)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.timerState.status) {
                            TimerStatus.RUNNING -> "Brewing"
                            TimerStatus.PAUSED -> "Paused"
                            TimerStatus.COMPLETE -> "Complete"
                            else -> "Timer"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TimerIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(TimerIntent.EditSession) }) {
                        Icon(FeatherIcons.Edit, contentDescription = "Edit parameters")
                    }
                    if (state.timerState.status != TimerStatus.COMPLETE) {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(FeatherIcons.MoreVertical, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
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
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.showCompletionScreen -> {
                CompletionContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            else -> {
                TimerRunningContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }

    // Stop confirmation dialog
    if (state.showStopConfirmation) {
        TimerStopConfirmationDialog(
            text = "The session will be saved as in progress. You can complete it later from the History screen.",
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

    // Next steep parameter dialog
    if (state.showNextSteepDialog && state.session != null) {
        val currentSession = state.session
        NextSteepParameterDialog(
            currentSession = currentSession,
            duration = state.nextSteepDuration,
            temperature = state.nextSteepTemperature ?: currentSession.temperatureCelsius,
            temperatureUnit = state.userPreferences.temperatureUnit,
            onDurationChange = { onIntent(TimerIntent.UpdateNextSteepDuration(it)) },
            onTemperatureChange = { onIntent(TimerIntent.UpdateNextSteepTemperature(it)) },
            onToggleUnit = {
                onIntent(TimerIntent.ToggleTemperatureUnit)
            },
            onConfirm = { onIntent(TimerIntent.ConfirmNextSteep(currentSession)) },
            onDismiss = { onIntent(TimerIntent.CancelNextSteepDialog) },
        )
    }

    // Discard session confirmation dialog
    if (state.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(TimerIntent.CancelDiscardSession) },
            title = { Text("Discard Session?") },
            text = { Text("This session will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(TimerIntent.ConfirmDiscardSession) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(TimerIntent.CancelDiscardSession) }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Save Configuration Dialog
    if (state.showSaveConfigurationDialog && state.savedSession != null) {
        val session = state.savedSession
        val vessel = state.vessel
        val tea = state.tea
        if (vessel != null && tea != null) {
            SaveConfigurationDialog(
                teaName = tea.name,
                vesselName = vessel.name,
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                temperatureCelsius = session.temperatureCelsius,
                brewingTimeSeconds = session.brewingTime.inWholeSeconds.toInt(),
                rating = session.rating ?: 5f,
                suggestedLabel = "", // Will be generated by use case
                onSave = { label ->
                    onIntent(TimerIntent.SaveConfigurationClicked(label.ifBlank { null }))
                },
                onDismiss = { onIntent(TimerIntent.SkipSaveConfiguration) }
            )
        }
    }

    // Edit session parameters sheet
    if (state.showEditSheet && state.editWaterType != null) {
        EditSessionParametersSheet(
            temperatureValue = state.editTemperatureCelsius,
            waterQuantityValue = state.editWaterQuantityMl,
            teaQuantityValue = state.editTeaQuantityGrams,
            waterType = state.editWaterType,
            temperatureUnit = state.userPreferences.temperatureUnit,
            volumeUnit = state.userPreferences.volumeUnit,
            onTemperatureChanged = { onIntent(TimerIntent.EditTemperatureChanged(it)) },
            onWaterQuantityChanged = { onIntent(TimerIntent.EditWaterQuantityChanged(it)) },
            onTeaQuantityChanged = { onIntent(TimerIntent.EditTeaQuantityChanged(it)) },
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
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Tea name and steep number
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.tea?.name ?: "Unknown Tea",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (state.session != null && state.session.steepNumber > 1) {
                Text(
                    text = "Steep ${state.session.steepNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Circular timer ring
        CircularTimerRing(
            progress = state.timerState.progress,
            timeText = state.formattedTime,
        )

        // Quick adjustment buttons
        QuickAdjustButtons(
            onAdjust = { adjustment ->
                onIntent(TimerIntent.AdjustTime(adjustment))
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        // Brewing parameters
        if (state.session != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${
                        TemperatureFormatter.format(
                            state.session.temperatureCelsius,
                            state.userPreferences.temperatureUnit
                        )
                    } • ${
                        VolumeFormatter.format(
                            state.session.waterQuantityMl,
                            state.userPreferences.volumeUnit
                        )
                    }",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun CompletionContent(
    state: TimerScreenState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Completion indicator
            Text(
                text = "✓",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Steep ${state.session?.steepNumber ?: 1} Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            Text(
                text = state.tea?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            // Brewing summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Session Summary",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    state.session?.let { session ->
                        Text(
                            text = "${
                                TemperatureFormatter.format(
                                    session.temperatureCelsius,
                                    state.userPreferences.temperatureUnit
                                )
                            } • ${
                                VolumeFormatter.format(
                                    session.waterQuantityMl,
                                    state.userPreferences.volumeUnit
                                )
                            } • ${session.brewingTime}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // Rating selector
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "How was it?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RatingSelector(
                    rating = state.rating,
                    onRatingChange = { onIntent(TimerIntent.RatingChanged(it)) },
                )
                if (state.rating == 0f) {
                    Text(
                        text = "Tap to rate (optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            // Notes input
            OutlinedTextField(
                value = state.notes ?: "",
                onValueChange = { onIntent(TimerIntent.NotesChanged(it)) },
                label = { Text("Notes for this steep (optional)") },
                placeholder = { Text("How did it taste?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
        }

        item {
            // Photos
            PhotoGrid(
                photos = state.photos,
                onAddPhoto = { onIntent(TimerIntent.PhotoSelected(it)) },
                onRemovePhoto = { onIntent(TimerIntent.PhotoRemoved(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Completion actions
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Continue to next steep (for multi-steep brewing)
                Button(
                    onClick = { onIntent(TimerIntent.ShowNextSteepDialog) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue to Steep ${(state.session?.steepNumber ?: 1) + 1}")
                }

                // Save and finish session
                Button(
                    onClick = {
                        state.session?.let { session ->
                            onIntent(TimerIntent.SaveAndFinish(session))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save & Finish Session")
                }

                // Restart timer (same parameters)
                OutlinedButton(
                    onClick = { onIntent(TimerIntent.RestartTimer) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = FeatherIcons.RotateCw,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Timer")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Discard session
                TextButton(
                    onClick = { onIntent(TimerIntent.DiscardSession) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Discard Session")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
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
                    temperatureCelsius = 80,
                    waterQuantityMl = 200,
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
                    temperatureCelsius = 80,
                    waterQuantityMl = 200,
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
private fun TimerScreenCompletePreview() {
    LeafLogTheme {
        CompletionContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    teaName = "Dragon Well",
                    steepNumber = 1,
                    totalDuration = 2.minutes,
                    remainingDuration = Duration.ZERO,
                    status = TimerStatus.COMPLETE,
                ),
                session = TeaSession(
                    id = "session-1",
                    teaId = "tea-1",
                    vesselId = "gaiwan",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 2.minutes,
                    temperatureCelsius = 80,
                    waterQuantityMl = 200,
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
