package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.components.common.SteepParameterCard
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.quicktimer.QuickTimerDetailsSheet
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerControlButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerStopConfirmationDialog
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
    onNavigateToSteepComplete: (String) -> Unit,
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
                is QuickTimerNavEvent.NavigateToSteepComplete -> onNavigateToSteepComplete(event.sessionId)
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
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(state.selectedTea?.name ?: "Quick Timer")
                    state.selectedVessel?.name?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onIntent(QuickTimerIntent.BackClicked) }) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            },
            actions = {},
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
                QuickTimerRunningContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // Stop confirmation dialog
    if (state.showStopConfirmation) {
        TimerStopConfirmationDialog(
            text = "This session will be discarded. Are you sure?",
            onConfirm = { onIntent(QuickTimerIntent.ConfirmStop) },
            onDismiss = { onIntent(QuickTimerIntent.CancelStop) },
        )
    }

    // Reset confirmation dialog
    if (state.showResetConfirmation) {
        TimerResetConfirmationDialog(
            durationText = "${state.totalDuration}",
            onConfirm = { onIntent(QuickTimerIntent.ConfirmReset) },
            onDismiss = { onIntent(QuickTimerIntent.CancelReset) },
        )
    }

    // Completion prompt — timer done but session details not filled
    if (state.showCompletionPrompt) {
        TimerStopConfirmationDialog(
            title = "Timer Complete",
            text = "Your tea is done! Add session details to save this brew, or discard it.",
            confirmText = "Discard",
            dismissText = "Add Details",
            isDestructive = true,
            onConfirm = { onIntent(QuickTimerIntent.DiscardCompletedSession) },
            onDismiss = { onIntent(QuickTimerIntent.AddSessionDetails) },
        )
    }

    // Discard guard — user pressed back while timer complete but nothing saved
    if (state.showDiscardCompleteConfirmation) {
        TimerStopConfirmationDialog(
            title = "Discard Session?",
            text = "The timer is complete but your session hasn't been saved. Your brewing data will be lost.",
            confirmText = "Discard",
            dismissText = "Stay",
            isDestructive = true,
            onConfirm = { onIntent(QuickTimerIntent.ConfirmDiscardComplete) },
            onDismiss = { onIntent(QuickTimerIntent.CancelDiscardComplete) },
        )
    }

    // Details sheet
    if (state.showDetailsSheet) {
        QuickTimerDetailsSheet(
            step = state.detailsSheetStep,
            selectedTea = state.selectedTea,
            selectedVessel = state.selectedVessel,
            teaQuantityGrams = state.teaQuantityGrams,
            isTeaBag = state.isTeaBag,
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
            onTeaBagModeChanged = { onIntent(QuickTimerIntent.TeaBagModeChanged(it)) },
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
}

@Composable
private fun QuickTimerRunningContent(
    state: QuickTimerState,
    onIntent: (QuickTimerIntent) -> Unit,
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
            SteepParameterCard(
                teaQuantity = state.teaQuantityGrams.takeIf { it.isNotBlank() }
                    ?.let { "${it}g" },
                waterQuantity = state.waterQuantityDisplay.takeIf { it.isNotBlank() }
                    ?.let { "$it ${state.userPreferences.volumeUnit.symbol}" },
                temperature = state.temperatureDisplay.takeIf { it.isNotBlank() }
                    ?.let { "$it${state.userPreferences.temperatureUnit.symbol}" },
                brewingTime = formatBrewingTime(state.totalDuration.inWholeSeconds.toInt()),
                onEditClick = { onIntent(QuickTimerIntent.ShowDetailsSheet) },
            )

            // Circular timer ring + quick adjustment buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularTimerRing(
                    progress = state.progress,
                    timeText = state.formattedTime,
                    modifier = Modifier.size(ringSize),
                )

                if (state.isRunning || state.isPaused) {
                    QuickAdjustButtons(
                        onAdjust = { adjustment ->
                            onIntent(QuickTimerIntent.AdjustTime(adjustment))
                        },
                        enabled = state.controlsEnabled,
                    )
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
