package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Edit
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.quicktimer.QuickTimerDetailsSheet
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerControlButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Quick Timer Screen - simple timer without a database session.
 */
@Composable
fun QuickTimerScreen(
    durationSeconds: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: QuickTimerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(durationSeconds) {
        viewModel.onIntent(QuickTimerIntent.Initialize(durationSeconds))
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is QuickTimerNavEvent.NavigateBack -> onNavigateBack()
                is QuickTimerNavEvent.NavigateToSession -> onNavigateToSession(event.sessionId)
                is QuickTimerNavEvent.NavigateToTimer -> onNavigateToTimer(event.sessionId)
            }
        }
    }

    QuickTimerContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickTimerContent(
    state: QuickTimerState,
    onIntent: (QuickTimerIntent) -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (state.isComplete && state.hasRequiredDetails) {
                QuickTimerCompletionBottomBar(state = state, onIntent = onIntent)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.status) {
                            TimerStatus.RUNNING -> "Quick Timer"
                            TimerStatus.PAUSED -> "Paused"
                            TimerStatus.COMPLETE -> "Complete"
                            else -> "Quick Timer"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(QuickTimerIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasRequiredDetails) {
                        IconButton(onClick = { onIntent(QuickTimerIntent.ShowDetailsSheet) }) {
                            Icon(FeatherIcons.Edit, contentDescription = "Edit brewing details")
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

            state.isComplete -> {
                QuickTimerCompleteContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            else -> {
                QuickTimerRunningContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }

    // Reset confirmation dialog
    if (state.showResetConfirmation) {
        TimerResetConfirmationDialog(
            durationText = "${state.totalDuration}",
            onConfirm = { onIntent(QuickTimerIntent.ConfirmReset) },
            onDismiss = { onIntent(QuickTimerIntent.CancelReset) },
        )
    }

    // Completion dialog (shown when timer completes without details)
    if (state.showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(QuickTimerIntent.DismissCompletionDialog) },
            title = { Text("Timer Complete!") },
            text = {
                Text(
                    if (state.hasRequiredDetails) {
                        "Your session is ready to be saved."
                    } else {
                        "Add tea and brewing details to save this session, or discard it."
                    }
                )
            },
            confirmButton = {
                if (state.hasRequiredDetails) {
                    Button(onClick = {
                        onIntent(QuickTimerIntent.DismissCompletionDialog)
                        onIntent(QuickTimerIntent.SaveSession)
                    }) {
                        Text("Save Session")
                    }
                } else {
                    Button(onClick = {
                        onIntent(QuickTimerIntent.DismissCompletionDialog)
                        onIntent(QuickTimerIntent.ShowDetailsSheet)
                    }) {
                        Text("Add Details")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(QuickTimerIntent.DiscardSession) }) {
                    Text("Discard")
                }
            },
        )
    }

    // Details sheet
    if (state.showDetailsSheet) {
        QuickTimerDetailsSheet(
            step = state.detailsSheetStep,
            selectedTea = state.selectedTea,
            selectedVessel = state.selectedVessel,
            teaQuantityGrams = state.teaQuantityGrams,
            temperatureDisplay = state.temperatureDisplay,
            waterQuantityDisplay = state.waterQuantityDisplay,
            waterType = state.waterType,
            prefillSource = state.prefillSource,
            availableTeas = state.filteredTeas,
            availableVessels = state.availableVessels,
            teaSearchQuery = state.teaSearchQuery,
            userPreferences = state.userPreferences,
            onTeaSearchQueryChanged = { onIntent(QuickTimerIntent.TeaSearchQueryChanged(it)) },
            onTeaSelected = { onIntent(QuickTimerIntent.TeaSelected(it)) },
            onVesselSelected = { onIntent(QuickTimerIntent.VesselSelected(it)) },
            onTeaQuantityChanged = { onIntent(QuickTimerIntent.TeaQuantityChanged(it)) },
            onTemperatureChanged = { onIntent(QuickTimerIntent.TemperatureChanged(it)) },
            onToggleTemperatureUnit = { onIntent(QuickTimerIntent.ToggleTemperatureUnit) },
            onWaterQuantityChanged = { onIntent(QuickTimerIntent.WaterQuantityChanged(it)) },
            onToggleVolumeUnit = { onIntent(QuickTimerIntent.ToggleVolumeUnit) },
            onWaterTypeSelected = { onIntent(QuickTimerIntent.WaterTypeSelected(it)) },
            onNextStep = { onIntent(QuickTimerIntent.NextDetailsStep) },
            onPreviousStep = { onIntent(QuickTimerIntent.PreviousDetailsStep) },
            onDismiss = { onIntent(QuickTimerIntent.HideDetailsSheet) },
        )
    }

    // Save configuration dialog
    if (state.showSaveConfigurationDialog && state.savedSession != null) {
        val session = state.savedSession
        val tea = state.selectedTea
        val vessel = state.selectedVessel
        if (tea != null && vessel != null) {
            SaveConfigurationDialog(
                teaName = tea.name,
                vesselName = vessel.name,
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                temperatureCelsius = session.temperatureCelsius,
                brewingTimeSeconds = session.brewingTime.inWholeSeconds.toInt(),
                rating = session.rating ?: 5f,
                suggestedLabel = "",
                onSave = { label ->
                    onIntent(QuickTimerIntent.SaveConfigurationClicked(label.ifBlank { null }))
                },
                onDismiss = { onIntent(QuickTimerIntent.SkipSaveConfiguration) },
            )
        }
    }
}

@Composable
private fun QuickTimerRunningContent(
    state: QuickTimerState,
    onIntent: (QuickTimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title - show tea name if selected, otherwise "Quick Timer"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.selectedTea?.name ?: "Quick Timer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (state.selectedVessel != null) {
                Text(
                    text = state.selectedVessel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Circular timer ring
        CircularTimerRing(
            progress = state.progress,
            timeText = state.formattedTime,
        )

        // Quick adjustment buttons
        if (state.isRunning || state.isPaused) {
            QuickAdjustButtons(
                onAdjust = { adjustment ->
                    onIntent(QuickTimerIntent.AdjustTime(adjustment))
                },
                enabled = state.controlsEnabled,
            )
        }

        // "Add details" nudge - show when timer is running/paused and no details yet
        if ((state.isRunning || state.isPaused) && !state.hasRequiredDetails) {
            OutlinedButton(
                onClick = { onIntent(QuickTimerIntent.ShowDetailsSheet) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Tea & Brewing Details")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons
        TimerControlButtons(
            isRunning = state.isRunning,
            isPaused = state.isPaused,
            onStartClick = { onIntent(QuickTimerIntent.StartTimer) },
            onPauseClick = { onIntent(QuickTimerIntent.PauseTimer) },
            onResumeClick = { onIntent(QuickTimerIntent.ResumeTimer) },
            onResetClick = { onIntent(QuickTimerIntent.ResetTimer) },
        )
    }
}

@Composable
private fun QuickTimerCompleteContent(
    state: QuickTimerState,
    onIntent: (QuickTimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Compact hero
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Timer Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (state.selectedTea != null) {
                    Text(
                        text = state.selectedTea.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Stat grid when details are filled
        if (state.hasRequiredDetails) {
            item {
                QuickTimerStatGrid(state = state)
            }
        }

        // Show details prompt when not filled
        if (!state.hasRequiredDetails) {
            item {
                Text(
                    text = "Add details to save this session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Rating section (only show when details are filled)
        if (state.hasRequiredDetails) {
            item {
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
                        rating = state.rating ?: 0f,
                        onRatingChange = { rating ->
                            onIntent(QuickTimerIntent.RatingChanged(rating.takeIf { it > 0f }))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if ((state.rating ?: 0f) == 0f) {
                        Text(
                            text = "Tap to rate (optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Notes field
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onIntent(QuickTimerIntent.NotesChanged(it)) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }

        // Error message
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Add details button when no details
        if (!state.hasRequiredDetails) {
            item {
                Button(
                    onClick = { onIntent(QuickTimerIntent.ShowDetailsSheet) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add Details to Save")
                }
            }
        }

        // Discard session (destructive, kept at bottom of scroll)
        item {
            TextButton(
                onClick = { onIntent(QuickTimerIntent.DiscardSession) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Discard & Exit")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickTimerStatGrid(
    state: QuickTimerState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.temperatureDisplay.toDoubleOrNull()?.let {
                StatItem(
                    value = "${state.temperatureDisplay}${state.userPreferences.temperatureUnit.symbol}",
                    label = "Temp",
                )
            }
            state.waterQuantityDisplay.toDoubleOrNull()?.let {
                StatItem(
                    value = "${state.waterQuantityDisplay} ${state.userPreferences.volumeUnit.symbol}",
                    label = "Water",
                )
            }
            StatItem(
                value = state.totalDuration.toString(),
                label = "Time",
            )
            state.teaQuantityGrams.toDoubleOrNull()?.takeIf { it > 0 }?.let { qty ->
                val teaQty = if (qty % 1.0 == 0.0) "${qty.toInt()}g" else "${qty}g"
                StatItem(value = teaQty, label = "Tea")
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickTimerCompletionBottomBar(
    state: QuickTimerState,
    onIntent: (QuickTimerIntent) -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onIntent(QuickTimerIntent.ContinueToNextSteep) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text("Continue to Steep 2")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onIntent(QuickTimerIntent.ShowDetailsSheet) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                ) {
                    Text("Edit Details")
                }
                Button(
                    onClick = { onIntent(QuickTimerIntent.SaveSession) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                ) {
                    Text("Save & Finish")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerScreenNotStartedPreview() {
    LeafLogTheme {
        QuickTimerContent(
            state = QuickTimerState(
                totalDuration = 2.minutes,
                remainingDuration = 2.minutes,
                status = TimerStatus.NOT_STARTED,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerScreenRunningPreview() {
    LeafLogTheme {
        QuickTimerContent(
            state = QuickTimerState(
                totalDuration = 2.minutes,
                remainingDuration = 1.minutes + 30.seconds,
                status = TimerStatus.RUNNING,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerScreenCompletePreview() {
    LeafLogTheme {
        QuickTimerContent(
            state = QuickTimerState(
                totalDuration = 2.minutes,
                remainingDuration = 0.seconds,
                status = TimerStatus.COMPLETE,
            ),
            onIntent = {},
        )
    }
}
