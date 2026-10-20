package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SteepParameterCard
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerControlButtons
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerStopConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_session_text
import leaf_log.shared.generated.resources.discard_session_title
import leaf_log.shared.generated.resources.discard_steep_text
import leaf_log.shared.generated.resources.discard_steep_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.core.parameter.parametersOf

/**
 * Timer Screen - brewing countdown timer.
 */
@Composable
fun TimerScreen(
    route: NavRoute.TimerRoute,
    onNavigateBack: () -> Unit,
    onNavigateToSteepComplete: (String) -> Unit,
    viewModel: TimerViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

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
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun TimerContent(
    state: TimerScreenState,
    onIntent: (TimerIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
                                    stringResource(Res.string.discard),
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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
        val steepNumber = state.session?.currentSteep?.number ?: 1
        ConfirmDestructiveDialog(
            title = if (steepNumber == 1) {
                stringResource(Res.string.discard_session_title)
            } else {
                stringResource(Res.string.discard_steep_title, steepNumber)
            },
            text = if (steepNumber == 1) {
                stringResource(Res.string.discard_session_text)
            } else {
                stringResource(Res.string.discard_steep_text, steepNumber)
            },
            confirmLabel = stringResource(Res.string.discard),
            onConfirm = { onIntent(TimerIntent.ConfirmStop) },
            onDismiss = { onIntent(TimerIntent.CancelStop) },
        )
    }

    // Reset confirmation dialog
    if (state.showResetConfirmation) {
        TimerResetConfirmationDialog(
            durationText = formatBrewingTime(
                state.session?.currentSteep?.brewingTime?.inWholeSeconds?.toInt() ?: 0,
            ),
            onConfirm = { onIntent(TimerIntent.ConfirmReset) },
            onDismiss = { onIntent(TimerIntent.CancelReset) },
        )
    }

    // Edit session parameters sheet
    if (state.showEditSheet && state.editWaterType != null) {
        EditSessionParametersSheet(
            temperatureCelsius = state.editTemperatureCelsius,
            waterQuantityMl = state.editWaterQuantityMl,
            teaQuantityGrams = state.editTeaQuantityGrams,
            isTeaBag = state.editIsTeaBag,
            waterType = state.editWaterType,
            temperatureUnit = state.userPreferences.temperatureUnit,
            volumeUnit = state.userPreferences.volumeUnit,
            weightUnit = state.userPreferences.weightUnit,
            onTemperatureChanged = { onIntent(TimerIntent.EditTemperatureChanged(it)) },
            onWaterQuantityChanged = { onIntent(TimerIntent.EditWaterQuantityChanged(it)) },
            onTeaQuantityChanged = { onIntent(TimerIntent.EditTeaQuantityChanged(it)) },
            onTeaBagModeChanged = { onIntent(TimerIntent.EditTeaBagModeChanged(it)) },
            onWaterTypeSelected = { onIntent(TimerIntent.EditWaterTypeChanged(it)) },
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
    BoxWithConstraints(modifier = modifier) {
        // At least screen height, so the weights below share what the other content leaves;
        // it scrolls only if that content alone doesn't fit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Notification permission warning
            if (state.showNotificationWarning) {
                NotificationWarningBanner(
                    onDismiss = { onIntent(TimerIntent.DismissNotificationWarning) },
                )
            }

            // Brewing parameters card
            if (state.session != null) {
                SteepParameterCard(
                    session = state.session,
                    onEditClick = { onIntent(TimerIntent.EditSession) },
                )
            }

            // Circular timer ring + quick adjustment buttons. The ring grows into the space left
            // over, up to 240dp; what it doesn't use falls below the buttons
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularTimerRing(
                    progress = state.timerState.progress,
                    timeText = state.formattedTime,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                        .aspectRatio(1f),
                )
                QuickAdjustButtons(
                    onAdjust = { adjustment ->
                        onIntent(TimerIntent.AdjustTime(adjustment))
                    },
                )
            }

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

@Composable
private fun NotificationWarningBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FeatherIcons.AlertTriangle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Notifications are disabled, so you won't be alerted when your tea is ready in the background",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = FeatherIcons.X,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationWarningBannerPreview() {
    LeafLogTheme {
        NotificationWarningBanner(onDismiss = {})
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
                    startedAt = PreviewInstant,
                ),
                session = previewSession(),
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
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
                    startedAt = PreviewInstant,
                    pausedAt = PreviewInstant,
                ),
                session = previewSession(),
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
            ),
            onIntent = {},
        )
    }
}

