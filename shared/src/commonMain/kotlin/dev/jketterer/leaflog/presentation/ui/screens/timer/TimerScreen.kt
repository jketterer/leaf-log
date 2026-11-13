package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.BellOff
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerSheet
import dev.jketterer.leaflog.presentation.ui.components.common.BrewParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.timer.CircularTimerRing
import dev.jketterer.leaflog.presentation.ui.components.timer.NotificationsOffDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.TimerResetConfirmationDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_session_text
import leaf_log.shared.generated.resources.discard_session_title
import leaf_log.shared.generated.resources.discard_steep_text
import leaf_log.shared.generated.resources.discard_steep_title
import leaf_log.shared.generated.resources.finish_early
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.notifications_off
import leaf_log.shared.generated.resources.stop
import leaf_log.shared.generated.resources.stop_timer_text
import leaf_log.shared.generated.resources.stop_timer_title
import leaf_log.shared.generated.resources.tea_not_found
import leaf_log.shared.generated.resources.timer_done
import leaf_log.shared.generated.resources.timer_of_total
import leaf_log.shared.generated.resources.timer_paused
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Timer Screen - brewing countdown timer.
 */
@Composable
fun TimerScreen(
    route: NavRoute.TimerRoute,
    onNavigateBack: () -> Unit,
    onNavigateToSteepComplete: (String) -> Unit,
    onNavigateToSessionTimer: (String) -> Unit,
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

                is TimerNavEvent.NavigateToSessionTimer -> onNavigateToSessionTimer(event.sessionId)

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
    var showNotificationsOff by remember { mutableStateOf(false) }
    // Finish early is on the bar while running; paused, Reset has its place
    val canFinishEarly = state.timerState.canResume
    val isLoaded = !state.isLoading && (state.isQuickTimer || state.session != null)

    // System back asks before ending a Quick Timer, as the toolbar arrow does
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isQuickTimer,
        onBackCompleted = { onIntent(TimerIntent.BackClicked) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onIntent(TimerIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.navigate_up))
                    }
                },
                actions = {
                    if (state.notificationsOff) {
                        IconButton(onClick = { showNotificationsOff = true }) {
                            Icon(
                                FeatherIcons.BellOff,
                                contentDescription = stringResource(Res.string.notifications_off),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    // A Quick Timer has nothing to discard
                    if (!state.isQuickTimer || canFinishEarly) {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(FeatherIcons.MoreVertical, contentDescription = stringResource(Res.string.more_options))
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            if (canFinishEarly) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.finish_early)) },
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
                            }
                            if (!state.isQuickTimer) {
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
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = isLoaded, enter = fadeIn(), exit = fadeOut()) {
                TimerBottomBar(
                    status = state.timerState.status,
                    isQuickTimer = state.isQuickTimer,
                    onStart = { onIntent(TimerIntent.StartTimer) },
                    onPause = { onIntent(TimerIntent.PauseTimer) },
                    onResume = { onIntent(TimerIntent.ResumeTimer) },
                    onReset = { onIntent(TimerIntent.ResetTimer) },
                    onCompleteNow = { onIntent(TimerIntent.CompleteNow) },
                    onJustATimer = { onIntent(TimerIntent.JustATimer) },
                    onLogIt = { onIntent(TimerIntent.ShowTeaPicker) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // No spinner: the load is quick, so the page fades in whole once it's there. A session
        // that failed to load stays blank; the snackbar says why
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TimerBody(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showNotificationsOff) {
        NotificationsOffDialog(onDismiss = { showNotificationsOff = false })
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

    if (state.showEndTimerConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.stop_timer_title),
            text = stringResource(Res.string.stop_timer_text),
            confirmLabel = stringResource(Res.string.stop),
            onConfirm = { onIntent(TimerIntent.ConfirmEndTimer) },
            onDismiss = { onIntent(TimerIntent.CancelEndTimer) },
        )
    }

    // Reset confirmation dialog
    if (state.showResetConfirmation) {
        val resetTo = state.session?.currentSteep?.brewingTime ?: state.timerState.totalDuration
        TimerResetConfirmationDialog(
            durationText = formatBrewingTime(resetTo.inWholeSeconds.toInt()),
            onConfirm = { onIntent(TimerIntent.ConfirmReset) },
            onDismiss = { onIntent(TimerIntent.CancelReset) },
        )
    }

    if (state.showTeaPicker) {
        var filter by remember { mutableStateOf<TeaPickerFilter>(TeaPickerFilter.All) }
        TeaPickerSheet(
            teas = state.pickerTeas,
            recentTeas = state.recentTeas,
            teaTypes = state.teaTypes,
            filter = filter,
            selectedTeaId = null,
            onFilterChanged = { filter = it },
            onTeaSelected = { onIntent(TimerIntent.TeaPicked(it.id)) },
            onTeaCreated = { name, teaTypeId -> onIntent(TimerIntent.QuickAddTea(name, teaTypeId)) },
            onDismiss = { onIntent(TimerIntent.HideTeaPicker) },
        )
    }

    // Edit session parameters sheet
    if (state.showEditSheet && state.editWaterType != null) {
        BrewParametersSheet(
            temperatureCelsius = state.editTemperatureCelsius,
            waterQuantityMl = state.editWaterQuantityMl,
            hasChanges = state.hasEditChanges,
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
private fun TimerBody(
    state: TimerScreenState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer = state.timerState
    val session = state.session
    // A Quick Timer's plan is the length it started at; adjusting it keeps the same steps
    val quickTimerPlanned = remember(timer.steepNumber) { timer.totalDuration }
    val caption = when (timer.status) {
        TimerStatus.NOT_STARTED -> null
        TimerStatus.RUNNING -> stringResource(Res.string.timer_of_total, state.formattedTotal)
        TimerStatus.PAUSED -> stringResource(Res.string.timer_paused)
        TimerStatus.COMPLETE -> stringResource(Res.string.timer_done)
    }

    BoxWithConstraints(modifier = modifier) {
        TimerPageLayout(
            viewportHeight = maxHeight,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            header = {
                if (session != null) {
                    val steep = session.currentSteep
                    SteepTimerHeaderSection(
                        teaName = state.tea?.name ?: stringResource(Res.string.tea_not_found),
                        vesselName = state.vessel?.name,
                        teaColor = state.teaTypeColorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
                            ?: MaterialTheme.colorScheme.primary,
                        steepNumber = steep.number,
                        temperature = formatTemperature(steep.temperatureCelsius),
                        water = formatVolume(steep.waterQuantityMl),
                        leaf = session.session.teaQuantityGrams?.let { formatWeight(it) },
                        onEditParameters = { onIntent(TimerIntent.EditSession) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    QuickTimerHeaderSection(
                        onPickTea = { onIntent(TimerIntent.ShowTeaPicker) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            ring = {
                CircularTimerRing(
                    progress = timer.progress,
                    timeText = state.formattedTime,
                    caption = caption,
                    captionColor = if (timer.status == TimerStatus.PAUSED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            controls = {
                TimerControlsSection(
                    planned = session?.currentSteep?.brewingTime ?: quickTimerPlanned,
                    isComplete = timer.isComplete,
                    nextByFeel = timer.nextByFeel().totalDuration.takeIf { state.isQuickTimer && timer.isComplete },
                    onAdjust = { onIntent(TimerIntent.AdjustTime(it)) },
                    onNextByFeel = { onIntent(TimerIntent.NextByFeel) },
                )
            },
        )
    }
}

private val previewTea = Tea(
    id = "tea-1",
    name = "Dragon Well",
    teaTypeId = "green",
    createdAt = PreviewInstant,
    updatedAt = PreviewInstant,
    syncStatus = SyncStatus.LOCAL_ONLY,
)

private val previewVessel = BrewingVessel(
    id = "vessel-1",
    name = "Gaiwan",
    iconName = null,
    isSystemDefault = true,
    displayOrder = 0,
    createdAt = PreviewInstant,
    updatedAt = PreviewInstant,
)

@Preview(showBackground = true)
@Composable
private fun TimerScreenNotStartedPreview() {
    LeafLogTheme {
        TimerContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    steepNumber = 1,
                    totalDuration = 2.minutes,
                    remainingDuration = 2.minutes,
                ),
                session = previewSession(),
                tea = previewTea,
                teaTypeColorHex = "#4CAF50",
                vessel = previewVessel,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenRunningPreview() {
    LeafLogTheme {
        TimerContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    steepNumber = 1,
                    totalDuration = 2.minutes,
                    remainingDuration = 1.minutes + 30.seconds,
                    status = TimerStatus.RUNNING,
                    startedAt = PreviewInstant,
                ),
                session = previewSession(),
                tea = previewTea,
                teaTypeColorHex = "#4CAF50",
                vessel = previewVessel,
                notificationsOff = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPausedPreview() {
    LeafLogTheme {
        TimerContent(
            state = TimerScreenState(
                timerState = TimerState(
                    sessionId = "session-1",
                    teaId = "tea-1",
                    steepNumber = 3,
                    totalDuration = 25.seconds,
                    remainingDuration = 12.seconds,
                    status = TimerStatus.PAUSED,
                    startedAt = PreviewInstant,
                    pausedAt = PreviewInstant,
                ),
                session = previewSession(ratings = listOf(4f, 4.5f, null), brewingTime = 20.seconds),
                tea = previewTea,
                teaTypeColorHex = "#4CAF50",
                vessel = previewVessel,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenQuickTimerRunningPreview() {
    LeafLogTheme {
        TimerContent(
            state = TimerScreenState(
                isQuickTimer = true,
                timerState = TimerState(
                    totalDuration = 30.seconds,
                    remainingDuration = 12.seconds,
                    status = TimerStatus.RUNNING,
                    startedAt = PreviewInstant,
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenQuickTimerDonePreview() {
    LeafLogTheme {
        TimerContent(
            state = TimerScreenState(
                isQuickTimer = true,
                timerState = TimerState(
                    totalDuration = 2.minutes,
                    status = TimerStatus.COMPLETE,
                    startedAt = PreviewInstant,
                ),
            ),
            onIntent = {},
        )
    }
}
