package dev.jketterer.leaflog.presentation.ui.screens.history

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.timeline
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.components.session.SteepEditSheet
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepParameterDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
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
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.review_save_method_header
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
                IconButton(onClick = { onIntent(SessionDetailIntent.EditSessionClicked) }) {
                    Icon(FeatherIcons.Edit2, contentDescription = stringResource(Res.string.edit_session))
                }
                IconButton(onClick = { onIntent(SessionDetailIntent.DeleteSessionClicked) }) {
                    Icon(FeatherIcons.Trash2, contentDescription = stringResource(Res.string.delete_session))
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
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
                        methodLabel = state.usedConfigurationLabel,
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
                if (state.session != null && state.usedConfigurationLabel == null) {
                    OutlinedButton(
                        onClick = { onIntent(SessionDetailIntent.SaveAsConfigurationClicked) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.review_save_method_header))
                    }
                }
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

    if (state.showSaveConfigurationDialog && state.session != null) {
        val first = state.session.steeps.first()
        SaveConfigurationDialog(
            teaName = state.tea?.name ?: "",
            vesselName = state.vessel?.name ?: "",
            teaQuantityGrams = state.session.session.teaQuantityGrams,
            waterQuantityMl = first.waterQuantityMl,
            temperatureCelsius = first.temperatureCelsius,
            brewingTimeSeconds = first.brewingTime.inWholeSeconds.toInt(),
            rating = state.session.effectiveRating,
            suggestedLabel = state.suggestedConfigurationLabel,
            onSave = { label -> onIntent(SessionDetailIntent.ConfirmSaveConfiguration(label)) },
            onDismiss = { onIntent(SessionDetailIntent.DismissSaveConfiguration) },
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
