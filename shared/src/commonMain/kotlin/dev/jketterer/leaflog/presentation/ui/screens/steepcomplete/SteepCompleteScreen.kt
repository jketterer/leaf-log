package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Edit
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.SteepEditBody
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
import leaf_log.shared.generated.resources.review_another_in
import leaf_log.shared.generated.resources.review_change_steep_n
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_next_in
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.start_steep_n
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
                nextSteepNumber = state.session.currentSteep.number + 1,
                nextTime = state.nextSteepDuration?.inWholeSeconds?.toInt()?.let(::formatBrewingTime),
                nextTemperature = state.nextSteepTemperature?.let { formatTemperature(it) },
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
    val session = state.session ?: return
    val steep = session.currentSteep
    // Sits on the buttons, where a thumb reaches the stars; the room left over goes under the title
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            Text(
                text = stringResource(Res.string.review_steep_n, steep.number),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            SteepEditBody(
                steep = steep,
                rating = state.rating,
                notes = state.notes,
                vocabulary = state.vocabulary,
                onRatingChange = { onIntent(SteepCompleteIntent.RatingChanged(it)) },
                onNotesChange = { onIntent(SteepCompleteIntent.NotesChanged(it)) },
                onTagToggle = { onIntent(SteepCompleteIntent.TagToggled(it)) },
                onTagCreate = { onIntent(SteepCompleteIntent.TagCreated(it)) },
                onEditParameters = { onIntent(SteepCompleteIntent.ShowEditParametersSheet) },
                teaQuantityGrams = session.session.teaQuantityGrams,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Next leads a gongfu plan; Finish leads a western one, with "Another" in Next's place. */
@Composable
private fun SteepCompleteBottomBar(
    finishLeads: Boolean,
    nextSteepNumber: Int,
    nextTime: String?,
    nextTemperature: String?,
    onNext: () -> Unit,
    onAdjust: () -> Unit,
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextLabel = stringResource(Res.string.start_steep_n, nextSteepNumber)
    val finish = stringResource(Res.string.review_finish_session)

    Surface(modifier = modifier, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NextSteepRow(
                steepNumber = nextSteepNumber,
                time = nextTime,
                temperature = nextTemperature,
                onClick = onAdjust,
            )
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
        }
    }
}

/** The steep the buttons start, tapped to change its time and temperature first. */
@Composable
private fun NextSteepRow(
    steepNumber: Int,
    time: String?,
    temperature: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.review_steep_n, steepNumber),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        time?.let { NextSteepValue(FeatherIcons.Clock, it) }
        temperature?.let { NextSteepValue(FeatherIcons.Thermometer, it) }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                FeatherIcons.Edit,
                contentDescription = stringResource(Res.string.review_change_steep_n, steepNumber),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextSteepValue(icon: ImageVector, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                nextSteepTemperature = 95.0,
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
                nextSteepTemperature = 76.4,
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}
