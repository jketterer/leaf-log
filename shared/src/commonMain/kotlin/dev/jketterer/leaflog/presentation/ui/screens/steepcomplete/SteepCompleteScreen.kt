package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.MoreVertical
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SteepParameterCard
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepAdjustSheet
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_session_action
import leaf_log.shared.generated.resources.discard_session_text
import leaf_log.shared.generated.resources.discard_session_title
import leaf_log.shared.generated.resources.discard_steep_text
import leaf_log.shared.generated.resources.discard_steep_title
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.review_add_note
import leaf_log.shared.generated.resources.review_adjust
import leaf_log.shared.generated.resources.review_another_in
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_next_in
import leaf_log.shared.generated.resources.review_note_label
import leaf_log.shared.generated.resources.review_rate_steep
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds

@Composable
fun SteepCompleteScreen(
    route: NavRoute.SteepCompleteRoute,
    onNavigateToHome: (finishedSessionId: String?) -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SteepCompleteViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SteepCompleteNavEvent.NavigateToHome -> onNavigateToHome(event.finishedSessionId)
                is SteepCompleteNavEvent.NavigateToReview -> onNavigateToReview(event.sessionId)
                is SteepCompleteNavEvent.NavigateToTimer -> onNavigateToTimer(event.sessionId)
                is SteepCompleteNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    SteepCompleteContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SteepCompleteContent(
    state: SteepCompleteState,
    onIntent: (SteepCompleteIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
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
                IconButton(onClick = { onIntent(SteepCompleteIntent.BackClicked) }) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            },
            actions = {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(FeatherIcons.MoreVertical, contentDescription = stringResource(Res.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.discard_session_action),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onIntent(SteepCompleteIntent.ShowDiscardConfirmation)
                        },
                    )
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Failed to load; the snackbar says why
                state.session == null -> {}

                else -> {
                    SteepCompleteBody(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (!state.isLoading && state.session != null) {
            SteepCompleteBottomBar(
                finishLeads = state.finishLeads,
                nextTime = state.nextSteepDuration?.inWholeSeconds?.toInt()?.let(::formatBrewingTime),
                onNext = { onIntent(SteepCompleteIntent.StartNextSteep) },
                onAdjust = { onIntent(SteepCompleteIntent.ShowAdjustSheet) },
                onFinishSession = { onIntent(SteepCompleteIntent.FinishSession) },
            )
        }
    }

    if (state.showAdjustSheet && state.session != null) {
        NextSteepAdjustSheet(
            nextSteepNumber = state.session.currentSteep.number + 1,
            duration = state.nextSteepDuration,
            temperatureCelsius = state.nextSteepTemperature,
            temperatureUnit = state.userPreferences.temperatureUnit,
            onDurationChange = { onIntent(SteepCompleteIntent.NextSteepDurationChanged(it)) },
            onTemperatureChange = { onIntent(SteepCompleteIntent.NextSteepTemperatureChanged(it)) },
            onStart = { onIntent(SteepCompleteIntent.StartNextSteep) },
            onDismiss = { onIntent(SteepCompleteIntent.DismissAdjustSheet) },
        )
    }

    // Discard confirmation dialog
    if (state.showDiscardConfirmation) {
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
            onConfirm = { onIntent(SteepCompleteIntent.ConfirmDiscard) },
            onDismiss = { onIntent(SteepCompleteIntent.DismissDiscardConfirmation) },
        )
    }

    // Edit parameters sheet
    if (state.showEditParametersSheet && state.editWaterType != null) {
        EditSessionParametersSheet(
            brewingTime = state.editBrewingTime,
            onBrewingTimeChanged = { onIntent(SteepCompleteIntent.EditBrewingTimeChanged(it)) },
            temperatureCelsius = state.editTemperatureCelsius,
            waterQuantityMl = state.editWaterQuantityMl,
            teaQuantityGrams = state.editTeaQuantityGrams,
            isTeaBag = state.editIsTeaBag,
            waterType = state.editWaterType,
            temperatureUnit = state.userPreferences.temperatureUnit,
            volumeUnit = state.userPreferences.volumeUnit,
            weightUnit = state.userPreferences.weightUnit,
            onTemperatureChanged = { onIntent(SteepCompleteIntent.EditTemperatureChanged(it)) },
            onWaterQuantityChanged = { onIntent(SteepCompleteIntent.EditWaterQuantityChanged(it)) },
            onTeaQuantityChanged = { onIntent(SteepCompleteIntent.EditTeaQuantityChanged(it)) },
            onTeaBagModeChanged = { onIntent(SteepCompleteIntent.EditTeaBagModeChanged(it)) },
            onWaterTypeSelected = { onIntent(SteepCompleteIntent.EditWaterTypeChanged(it)) },
            onSave = { onIntent(SteepCompleteIntent.ConfirmEditParameters) },
            onDismiss = { onIntent(SteepCompleteIntent.CancelEditParameters) },
            vessels = state.vessels,
            selectedVessel = state.editVessel,
            onVesselSelected = { onIntent(SteepCompleteIntent.EditVesselChanged(it)) },
        )
    }

}

@Composable
private fun SteepCompleteBody(
    state: SteepCompleteState,
    onIntent: (SteepCompleteIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        state.session?.let { session ->
            SteepParameterCard(
                session = session,
                onEditClick = { onIntent(SteepCompleteIntent.ShowEditParametersSheet) },
            )
        }

        HorizontalDivider()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.review_rate_steep),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            RatingSelector(
                rating = state.rating,
                onRatingChange = { onIntent(SteepCompleteIntent.RatingChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()

        // Collapsed until asked for, open when the steep already has a note
        var noteOpen by remember(state.session?.currentSteep?.id) { mutableStateOf(state.notes.isNotEmpty()) }
        if (noteOpen) {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onIntent(SteepCompleteIntent.NotesChanged(it)) },
                label = { Text(stringResource(Res.string.review_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        } else {
            TextButton(onClick = { noteOpen = true }) {
                Text(stringResource(Res.string.review_add_note))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Next leads a gongfu plan; Finish leads a western one, with "Another" in Next's place. */
@Composable
private fun SteepCompleteBottomBar(
    finishLeads: Boolean,
    nextTime: String?,
    onNext: () -> Unit,
    onAdjust: () -> Unit,
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextLabel = nextTime?.let {
        stringResource(if (finishLeads) Res.string.review_another_in else Res.string.review_next_in, it)
    } ?: stringResource(Res.string.review_adjust)
    val finish = stringResource(Res.string.review_finish_session)

    Surface(modifier = modifier, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (finishLeads) {
                    Button(onClick = onFinishSession, modifier = Modifier.weight(1f)) { Text(finish) }
                    OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) { Text(nextLabel) }
                } else {
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(nextLabel) }
                    OutlinedButton(onClick = onFinishSession, modifier = Modifier.weight(1f)) { Text(finish) }
                }
            }
            TextButton(onClick = onAdjust, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(Res.string.review_adjust))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepCompleteScreenWesternPreview() {
    LeafLogTheme {
        SteepCompleteContent(
            state = SteepCompleteState(
                vessel = BrewingVessel(
                    id = "test",
                    name = "Teapot",
                    iconName = "icon",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    isSystemDefault = false,
                    displayOrder = 0,
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
                nextSteepDuration = 180.seconds,
                finishLeads = true,
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepCompleteScreenGongfuPreview() {
    LeafLogTheme {
        SteepCompleteContent(
            state = SteepCompleteState(
                vessel = BrewingVessel(
                    id = "test",
                    name = "Teapot",
                    iconName = "icon",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    isSystemDefault = false,
                    displayOrder = 0,
                ),
                session = previewSession(
                    ratings = listOf(4f, 5f, null),
                    brewingTime = 45.seconds,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 100.0,
                ),
                tea = Tea(
                    id = "tea-1",
                    name = "Tie Guan Yin",
                    teaTypeId = "oolong",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                nextSteepDuration = 55.seconds,
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}
