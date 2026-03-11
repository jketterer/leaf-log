package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.EditSessionParametersSheet
import dev.jketterer.leaflog.presentation.ui.components.common.SteepParameterCard
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepParameterDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaflog.composeapp.generated.resources.Res
import leaflog.composeapp.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun SteepCompleteScreen(
    sessionId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SteepCompleteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(SteepCompleteIntent.Initialize(sessionId))
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SteepCompleteNavEvent.NavigateToHome -> onNavigateToHome()
                is SteepCompleteNavEvent.NavigateToTimer -> onNavigateToTimer(event.sessionId)
                is SteepCompleteNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    SteepCompleteContent(
        state = state,
        onIntent = viewModel::onIntent,
        imageStorage = koinInject(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SteepCompleteContent(
    state: SteepCompleteState,
    onIntent: (SteepCompleteIntent) -> Unit,
    imageStorage: ImageStorage?,
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
                IconButton(onClick = { onIntent(SteepCompleteIntent.ShowDiscardConfirmation) }) {
                    Icon(
                        FeatherIcons.Trash2,
                        contentDescription = "Discard session",
                        tint = MaterialTheme.colorScheme.error,
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

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                else -> {
                    SteepCompleteBody(
                        state = state,
                        onIntent = onIntent,
                        imageStorage = imageStorage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (!state.isLoading && state.session != null) {
            SteepCompleteBottomBar(
                isLoading = state.isLoading,
                onShowNextSteep = { onIntent(SteepCompleteIntent.ShowNextSteepDialog) },
                onFinishSession = { onIntent(SteepCompleteIntent.FinishSession) },
            )
        }
    }

    // Next steep parameter dialog
    if (state.showNextSteepDialog && state.session != null) {
        val currentSession = state.session
        NextSteepParameterDialog(
            currentSession = currentSession,
            duration = state.nextSteepDuration,
            temperature = state.nextSteepTemperature ?: currentSession.temperatureCelsius,
            temperatureUnit = state.userPreferences.temperatureUnit,
            onDurationChange = { onIntent(SteepCompleteIntent.NextSteepDurationChanged(it)) },
            onTemperatureChange = { onIntent(SteepCompleteIntent.NextSteepTemperatureChanged(it)) },
            onToggleUnit = { onIntent(SteepCompleteIntent.ToggleTemperatureUnit) },
            onConfirm = { onIntent(SteepCompleteIntent.ConfirmNextSteep) },
            onDismiss = { onIntent(SteepCompleteIntent.DismissNextSteepDialog) },
        )
    }

    // Discard confirmation dialog
    if (state.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(SteepCompleteIntent.DismissDiscardConfirmation) },
            title = { Text("Discard Session?") },
            text = { Text("This session will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(SteepCompleteIntent.ConfirmDiscard) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SteepCompleteIntent.DismissDiscardConfirmation) }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Edit parameters sheet
    if (state.showEditParametersSheet && state.editWaterType != null) {
        EditSessionParametersSheet(
            brewingTime = state.editBrewingTime,
            onBrewingTimeChanged = { onIntent(SteepCompleteIntent.EditBrewingTimeChanged(it)) },
            temperatureValue = state.editTemperatureCelsius,
            waterQuantityValue = state.editWaterQuantityMl,
            teaQuantityValue = state.editTeaQuantityGrams,
            isTeaBag = state.editIsTeaBag,
            waterType = state.editWaterType,
            temperatureUnit = state.userPreferences.temperatureUnit,
            volumeUnit = state.userPreferences.volumeUnit,
            onTemperatureChanged = { onIntent(SteepCompleteIntent.EditTemperatureChanged(it)) },
            onWaterQuantityChanged = { onIntent(SteepCompleteIntent.EditWaterQuantityChanged(it)) },
            onTeaQuantityChanged = { onIntent(SteepCompleteIntent.EditTeaQuantityChanged(it)) },
            onTeaBagModeChanged = { onIntent(SteepCompleteIntent.EditTeaBagModeChanged(it)) },
            onWaterTypeSelected = { onIntent(SteepCompleteIntent.EditWaterTypeChanged(it)) },
            onToggleTemperatureUnit = { onIntent(SteepCompleteIntent.ToggleTemperatureUnit) },
            onToggleVolumeUnit = { onIntent(SteepCompleteIntent.ToggleVolumeUnit) },
            onSave = { onIntent(SteepCompleteIntent.ConfirmEditParameters) },
            onDismiss = { onIntent(SteepCompleteIntent.CancelEditParameters) },
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
                suggestedLabel = state.suggestedConfigurationLabel,
                userPreferences = state.userPreferences,
                onSave = { label ->
                    onIntent(SteepCompleteIntent.SaveConfigurationClicked(label.ifBlank { null }))
                },
                onDismiss = { onIntent(SteepCompleteIntent.SkipSaveConfiguration) },
            )
        }
    }
}

@Composable
private fun SteepCompleteBody(
    state: SteepCompleteState,
    onIntent: (SteepCompleteIntent) -> Unit,
    imageStorage: ImageStorage?,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val hasPreviousSteeps = state.previousSteeps.isNotEmpty()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Brewing parameters
        state.session?.let { session ->
            SteepParameterCard(
                session = session,
                userPreferences = state.userPreferences,
                onEditClick = { onIntent(SteepCompleteIntent.ShowEditParametersSheet) },
            )
        }

        // Photos — capture the moment while sipping
        PhotoGrid(
            photos = state.photos,
            onAddPhoto = { onIntent(SteepCompleteIntent.PhotoSelected(it)) },
            onRemovePhoto = { onIntent(SteepCompleteIntent.PhotoRemoved(it)) },
            imageStorage = imageStorage,
            modifier = Modifier.fillMaxWidth(),
        )

        // Rating — comes last, after tasting
        HorizontalDivider()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Rate this steep",
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

        // Notes — jot down flavor observations
        OutlinedTextField(
            value = state.notes,
            onValueChange = { onIntent(SteepCompleteIntent.NotesChanged(it)) },
            label = { Text("Tasting notes (optional)") },
            placeholder = { Text("Flavor, aroma, mouthfeel...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (hasPreviousSteeps) 3 else 5,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )


        // Previous steeps section
        if (hasPreviousSteeps) {
            HorizontalDivider()

            Text(
                text = "Previous Steeps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            state.previousSteeps.forEach { steep ->
                PreviousSteepItem(
                    steep = steep,
                    userPreferences = state.userPreferences,
                    onRatingChanged = { rating ->
                        onIntent(SteepCompleteIntent.PreviousSteepRatingChanged(steep.id, rating))
                    },
                    onNotesChanged = { notes ->
                        onIntent(SteepCompleteIntent.PreviousSteepNotesChanged(steep.id, notes))
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PreviousSteepItem(
    steep: TeaSession,
    userPreferences: UserPreferences,
    onRatingChanged: (Float) -> Unit,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Steep ${steep.steepNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    steep.teaQuantityGrams?.let {
                        BrewingParamChip(
                            icon = vectorResource(Res.drawable.ic_tea_leaf),
                            text = "${it}g"
                        )
                    }
                    BrewingParamChip(
                        icon = FeatherIcons.Droplet,
                        text = VolumeFormatter.format(
                            steep.waterQuantityMl,
                            userPreferences.volumeUnit
                        ),
                    )
                    BrewingParamChip(
                        icon = FeatherIcons.Thermometer,
                        text = TemperatureFormatter.format(
                            steep.temperatureCelsius,
                            userPreferences.temperatureUnit
                        ),
                    )
                    BrewingParamChip(
                        icon = FeatherIcons.Clock,
                        text = formatBrewingTime(steep.brewingTime.inWholeSeconds.toInt()),
                    )
                }

            }
            // Rating
            RatingSelector(
                rating = steep.rating ?: 0f,
                onRatingChange = onRatingChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            // Notes field
            OutlinedTextField(
                value = steep.notes ?: "",
                onValueChange = onNotesChanged,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
    }
}

@Composable
private fun SteepCompleteBottomBar(
    isLoading: Boolean,
    onShowNextSteep: () -> Unit,
    onFinishSession: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onShowNextSteep,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
            ) {
                Text("Next Steep")
            }
            Button(
                onClick = onFinishSession,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
            ) {
                Text("Finish Session")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepCompleteScreenSingleSteepPreview() {
    LeafLogTheme {
        SteepCompleteContent(
            state = SteepCompleteState(
                vessel = BrewingVessel(
                    id = "test",
                    name = "Teapot",
                    iconName = "icon",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    isSystemDefault = false,
                    displayOrder = 0,
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
                isLoading = false,
            ),
            onIntent = {},
            imageStorage = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepCompleteScreenMultiSteepPreview() {
    LeafLogTheme {
        SteepCompleteContent(
            state = SteepCompleteState(
                vessel = BrewingVessel(
                    id = "test",
                    name = "Teapot",
                    iconName = "icon",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    isSystemDefault = false,
                    displayOrder = 0,
                ),
                session = TeaSession(
                    id = "session-3",
                    teaId = "tea-1",
                    parentSessionId = "session-1",
                    steepNumber = 3,
                    vesselId = "gaiwan",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 45.minutes,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 100.0,
                    status = SessionStatus.IN_PROGRESS,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                tea = Tea(
                    id = "tea-1",
                    name = "Tie Guan Yin",
                    teaTypeId = "oolong",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                previousSteeps = listOf(
                    TeaSession(
                        id = "session-1",
                        teaId = "tea-1",
                        steepNumber = 1,
                        vesselId = "gaiwan",
                        waterType = WaterType.FILTERED,
                        timestamp = Clock.System.now(),
                        brewingTime = 30.minutes,
                        temperatureCelsius = 95.0,
                        waterQuantityMl = 100.0,
                        rating = 4f,
                        status = SessionStatus.COMPLETED,
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    TeaSession(
                        id = "session-2",
                        teaId = "tea-1",
                        parentSessionId = "session-1",
                        steepNumber = 2,
                        vesselId = "gaiwan",
                        waterType = WaterType.FILTERED,
                        timestamp = Clock.System.now(),
                        brewingTime = 40.minutes,
                        temperatureCelsius = 95.0,
                        waterQuantityMl = 100.0,
                        rating = 5f,
                        status = SessionStatus.COMPLETED,
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                ),
                isLoading = false,
            ),
            onIntent = {},
            imageStorage = null,
        )
    }
}
