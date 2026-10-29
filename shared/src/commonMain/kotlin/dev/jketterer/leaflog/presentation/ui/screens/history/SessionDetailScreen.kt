package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Plus
import compose.icons.feathericons.MoreVertical
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.timeline
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.BrewAgainSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.components.session.SteepEditSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SteepTimelineSection
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepParameterDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_steep
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import leaf_log.shared.generated.resources.delete_steep_text
import leaf_log.shared.generated.resources.delete_steep_title
import leaf_log.shared.generated.resources.edit_session
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.next_session
import leaf_log.shared.generated.resources.previous_session
import leaf_log.shared.generated.resources.session_not_found
import leaf_log.shared.generated.resources.session_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds

/** A session read back as it happened: its setup and note, then each steep on a timeline. */
@Composable
fun SessionDetailScreen(
    route: NavRoute.SessionDetailsRoute,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToTea: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToLogTea: (sourceSessionId: String) -> Unit,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is SessionDetailNavEvent.NavigateToEditSession -> onNavigateToEdit(event.sessionId)
                is SessionDetailNavEvent.NavigateToTeaDetails -> onNavigateToTea(event.teaId)
                is SessionDetailNavEvent.NavigateToTimer -> onNavigateToTimer(event.sessionId)
                is SessionDetailNavEvent.NavigateToLogTea -> onNavigateToLogTea(event.sourceSessionId)
                is SessionDetailNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    SessionDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SessionDetailContent(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.session_title)) },
            navigationIcon = {
                IconButton(onClick = { onIntent(SessionDetailIntent.BackClicked) }) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.navigate_up))
                }
            },
            actions = {
                // Disabled rather than hidden at either end, so the other icons stay put
                IconButton(
                    onClick = { state.nextSessionId?.let { onIntent(SessionDetailIntent.StepToSession(it)) } },
                    enabled = state.nextSessionId != null,
                ) {
                    Icon(FeatherIcons.ChevronUp, contentDescription = stringResource(Res.string.next_session))
                }
                IconButton(
                    onClick = { state.previousSessionId?.let { onIntent(SessionDetailIntent.StepToSession(it)) } },
                    enabled = state.previousSessionId != null,
                ) {
                    Icon(FeatherIcons.ChevronDown, contentDescription = stringResource(Res.string.previous_session))
                }
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(FeatherIcons.MoreVertical, contentDescription = stringResource(Res.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit_session)) },
                            onClick = {
                                showMenu = false
                                onIntent(SessionDetailIntent.EditSessionClicked)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete_session)) },
                            onClick = {
                                showMenu = false
                                onIntent(SessionDetailIntent.DeleteSessionClicked)
                            },
                        )
                    }
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            // Only the content moves when stepping; the bars stay put
            AnimatedContent(
                targetState = state,
                contentKey = { it.session?.session?.id },
                transitionSpec = { stepTransition(initialState.session, targetState.session) },
                label = "session",
            ) { shown -> SessionDetailBody(shown, onIntent) }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Surface(shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onIntent(SessionDetailIntent.BrewAgainClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(FeatherIcons.Coffee, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.brew_again))
                }
            }
        }
    }

    SessionDetailDialogs(state, onIntent)
}

@Composable
private fun SessionDetailBody(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
) {
    val session = state.session
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        session == null -> EmptyState(
            message = stringResource(Res.string.session_not_found),
            modifier = Modifier.fillMaxSize(),
        )

        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val tea = state.tea
            SessionHeaderSection(
                teaName = tea?.name,
                onTeaClick = tea?.let { { onIntent(SessionDetailIntent.ViewTeaClicked(it.id)) } },
                startedAt = session.startedAt,
                rating = session.effectiveRating,
                session = session.session,
                vesselName = state.vessel?.name,
                photos = session.steeps.flatMap { it.photos },
            )
            SteepTimelineSection(
                timeline = session.timeline(),
                totalBrewTime = session.totalBrewTime,
                vocabulary = state.vocabulary,
                onSteepClick = { onIntent(SessionDetailIntent.SteepClicked(it)) },
            )
            // Recording a forgotten steep; an in-progress session adds steeps as it brews
            if (session.session.status == SessionStatus.COMPLETED) {
                OutlinedButton(
                    onClick = { onIntent(SessionDetailIntent.AddSteepClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(FeatherIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.add_steep))
                }
            }
        }
    }
}

/**
 * A newer session comes down from above, as in History's newest-first list, and an older one up from
 * below. The first load, or a session deleted from under the screen, just appears.
 */
private fun stepTransition(from: SessionWithSteeps?, to: SessionWithSteeps?): ContentTransform {
    if (from == null || to == null) return EnterTransition.None togetherWith ExitTransition.None
    val direction = if (to.startedAt > from.startedAt) -1 else 1
    return slideInVertically { it * direction } togetherWith slideOutVertically { -it * direction }
}

@Composable
private fun SessionDetailDialogs(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
) {
    val editingSteep = state.session?.steeps?.firstOrNull { it.id == state.editingSteepId }
    if (editingSteep != null) {
        SteepEditSheet(
            steep = editingSteep,
            rating = state.editRating,
            notes = state.editNotes,
            vocabulary = state.vocabulary,
            onRatingChange = { onIntent(SessionDetailIntent.SteepRatingChanged(it)) },
            onNotesChange = { onIntent(SessionDetailIntent.SteepNotesChanged(it)) },
            onTagToggle = { onIntent(SessionDetailIntent.SteepTagToggled(it)) },
            onTagCreate = { onIntent(SessionDetailIntent.SteepTagCreated(it)) },
            onParametersChange = { time, temperature, water ->
                onIntent(SessionDetailIntent.SteepParametersChanged(time, temperature, water))
            },
            onDismiss = { onIntent(SessionDetailIntent.DismissSteepEdit) },
            // Steep 1 is the session's own row
            onDelete = if (editingSteep.number > 1) {
                { onIntent(SessionDetailIntent.DeleteSteepClicked(editingSteep.id)) }
            } else null,
        )
    }

    state.brewAgainSheet?.let { sheet ->
        val onBrewAgain = { intent: BrewAgainIntent -> onIntent(SessionDetailIntent.BrewAgain(intent)) }
        BrewAgainSheet(
            teaName = sheet.teaName,
            teaTypeColorHex = sheet.teaTypeColorHex,
            vesselName = sheet.vesselName,
            brew = sheet.brew,
            tagVocabulary = sheet.tagVocabulary,
            onStart = { onBrewAgain(BrewAgainIntent.Start) },
            onAdjust = { onBrewAgain(BrewAgainIntent.Adjust) },
            onDismiss = { onBrewAgain(BrewAgainIntent.Dismiss) },
        )
    }

    state.inProgressDialogState?.let { dialogState ->
        SessionInProgressDialog(
            teaName = dialogState.teaName,
            vesselName = dialogState.vesselName,
            session = dialogState.session,
            isProcessing = dialogState.isProcessing,
            onResume = { onIntent(SessionDetailIntent.ResumeInProgress) },
            onCompleteAndContinue = { onIntent(SessionDetailIntent.CompleteInProgressAndContinue) },
            onDiscardAndContinue = { onIntent(SessionDetailIntent.DiscardInProgressAndContinue) },
            onDismiss = { onIntent(SessionDetailIntent.DismissInProgressDialog) },
        )
    }

    if (state.showDeleteConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_session_title),
            text = stringResource(Res.string.delete_session_text),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(SessionDetailIntent.ConfirmDelete) },
            onDismiss = { onIntent(SessionDetailIntent.CancelDelete) },
        )
    }

    val lastSteep = state.session?.currentSteep
    if (state.showAddSteepDialog && lastSteep != null) {
        NextSteepParameterDialog(
            nextSteepNumber = lastSteep.number + 1,
            duration = state.nextSteepDuration,
            temperatureCelsius = state.nextSteepTemperature,
            temperatureUnit = LocalUnits.current.temperature,
            onDurationChange = { onIntent(SessionDetailIntent.UpdateNextSteepDuration(it)) },
            onTemperatureChange = { onIntent(SessionDetailIntent.UpdateNextSteepTemperature(it)) },
            onConfirm = { onIntent(SessionDetailIntent.ConfirmAddSteep) },
            onDismiss = { onIntent(SessionDetailIntent.CancelAddSteep) },
        )
    }

    if (state.showDeleteSteepConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_steep_title),
            text = stringResource(Res.string.delete_steep_text),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(SessionDetailIntent.ConfirmDeleteSteep) },
            onDismiss = { onIntent(SessionDetailIntent.CancelDeleteSteep) },
        )
    }
}

@Preview
@Composable
private fun SessionDetailScreenPreview() {
    LeafLogTheme {
        SessionDetailContent(
            state = SessionDetailState(
                session = previewSession(
                    ratings = listOf(5f, 4f, null),
                    status = SessionStatus.COMPLETED,
                    brewingTime = 25.seconds,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 120.0,
                    notes = "Sweet and long, faded by the third",
                ),
                vessel = BrewingVessel(
                    id = "vessel-1",
                    name = "Gaiwan",
                    isSystemDefault = false,
                    displayOrder = 1,
                    updatedAt = PreviewInstant,
                    createdAt = PreviewInstant,
                    iconName = "gaiwan",
                ),
                tea = Tea(
                    id = "tea-1",
                    name = "Bai Hao Oolong",
                    teaTypeId = "oolong",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                previousSessionId = "session-0",
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun SessionDetailScreenNotFoundPreview() {
    LeafLogTheme {
        SessionDetailContent(state = SessionDetailState(), onIntent = {})
    }
}
