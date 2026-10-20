package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.ChooseMethodDialog
import dev.jketterer.leaflog.presentation.ui.components.session.CompleteSessionDialog
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_temperature_required
import leaf_log.shared.generated.resources.field_water_quantity_required
import leaf_log.shared.generated.resources.rinsed
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Log Tea Screen - create a new brewing session.
 */
@Composable
fun LogTeaScreen(
    route: NavRoute.LogTeaRoute,
    onNavigateBack: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: LogTeaViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is LogTeaNavigationEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is LogTeaNavigationEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    LogTeaContent(
        state = state,
        onIntent = viewModel::onIntent,
        imageStorage = koinInject(),
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun LogTeaContent(
    state: LogTeaState,
    onIntent: (LogTeaIntent) -> Unit,
    imageStorage: ImageStorage? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showOptionalFields by remember { mutableStateOf(false) }

    val hasOptionalData = state.selectedWaterType != state.userPreferences.defaultWaterType ||
            state.rinsed ||
            state.location.isNotEmpty()

    LaunchedEffect(hasOptionalData) {
        if (hasOptionalData) {
            showOptionalFields = true
        }
    }

    val optionalSummaryParts = buildList {
        if (state.selectedWaterType != WaterType.FILTERED) add(state.selectedWaterType.displayName)
        if (state.rinsed) add(stringResource(Res.string.rinsed))
        if (state.location.isNotEmpty()) add(state.location)
    }
    val optionalSummary = optionalSummaryParts.joinToString(" · ")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Log Tea Session") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LogTeaIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "tea_selection") {
                    TeaSelectionSection(
                        selectedTea = state.selectedTea,
                        teaTypeName = state.selectedTeaType?.name ?: "",
                        teaTypeColorHex = state.selectedTeaType?.colorHex,
                        errorMessage = state.teaError,
                        onSelectTeaClicked = { onIntent(LogTeaIntent.ShowTeaPicker) },
                        imageStorage = imageStorage,
                    )
                }

                item(key = "vessel") {
                    VesselSelector(
                        vessels = state.availableVessels,
                        selectedVessel = state.selectedVessel,
                        onVesselSelected = { vessel ->
                            onIntent(LogTeaIntent.VesselSelected(vessel.id))
                        },
                        label = "Brewing Vessel *",
                        isError = state.vesselError != null,
                        errorMessage = state.vesselError,
                        modifier = Modifier.fillMaxWidth(),
                        imageStorage = imageStorage,
                    )
                }

                // Prompt when tea or vessel not yet selected
                if (state.selectedTea == null || state.selectedVessel == null) {
                    item(key = "parameters_hint") {
                        SelectTeaAndVesselPrompt()
                    }
                }

                // Brewing Parameters Section (gated behind tea + vessel selection)
                if (state.selectedTea != null && state.selectedVessel != null) {
                    item(key = "parameters_header") {
                        BrewingParametersHeader()
                    }

                    if (state.prefillSource != PrefillSource.None) {
                        item(key = "prefill_banner") {
                            PrefillBanner(
                                source = state.prefillSource,
                                teaName = state.selectedTea.name,
                                modifier = Modifier.fillMaxWidth(),
                                hasMultipleMethods = state.availableConfigurations.isNotEmpty(),
                                onChooseDifferentMethod = {
                                    onIntent(LogTeaIntent.ChooseDifferentMethodClicked)
                                }
                            )
                        }
                    }

                    // Brewing Method Selector (when configurations exist but no banner shown)
                    if (state.availableConfigurations.isNotEmpty() && state.prefillSource == PrefillSource.None
                    ) {
                        item(key = "method_selector") {
                            OutlinedButton(
                                onClick = { onIntent(LogTeaIntent.ChooseDifferentMethodClicked) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choose Saved Brewing Method")
                            }
                        }
                    }

                    item(key = "tea_quantity") {
                        TeaQuantitySection(
                            isTeaBag = state.isTeaBag,
                            teaQuantityGrams = state.teaQuantityGrams,
                            weightUnit = state.userPreferences.weightUnit,
                            errorMessage = state.teaQuantityError,
                            onTeaBagModeChanged = { onIntent(LogTeaIntent.TeaBagModeChanged(it)) },
                            onTeaQuantityChanged = { onIntent(LogTeaIntent.TeaQuantityChanged(it)) },
                        )
                    }

                    item(key = "brewing_time") {
                        BrewingTimeSection(
                            brewingTime = state.brewingTime,
                            errorMessage = state.brewingTimeError,
                            onBrewingTimeChanged = { onIntent(LogTeaIntent.BrewingTimeChanged(it)) },
                        )
                    }

                    item(key = "temperature") {
                        UnitInputField(
                            value = state.temperatureCelsius,
                            onValueChange = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
                            unit = state.userPreferences.temperatureUnit,
                            label = stringResource(Res.string.field_temperature_required),
                            isError = state.temperatureError != null,
                            supportingText = state.temperatureError?.let { stringResource(it) },
                            presets = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item(key = "water_quantity") {
                        UnitInputField(
                            value = state.waterQuantityMl,
                            onValueChange = { onIntent(LogTeaIntent.WaterQuantityChanged(it)) },
                            unit = state.userPreferences.volumeUnit,
                            label = stringResource(Res.string.field_water_quantity_required),
                            isError = state.waterQuantityError != null,
                            supportingText = state.waterQuantityError?.let { stringResource(it) },
                            presets = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item(key = "optional_section") {
                        OptionalDetailsSection(
                            expanded = showOptionalFields,
                            onExpandedChange = { showOptionalFields = it },
                            summary = optionalSummary,
                            selectedWaterType = state.selectedWaterType,
                            rinsed = state.rinsed,
                            location = state.location,
                            onRinsedChange = { onIntent(LogTeaIntent.RinsedChanged(it)) },
                            onWaterTypeSelected = { waterType ->
                                onIntent(LogTeaIntent.WaterTypeSelected(waterType))
                            },
                            onLocationChanged = { onIntent(LogTeaIntent.LocationChanged(it)) },
                        )
                    }

                } // end parameters gate
            }
        }

        AnimatedVisibility(
            visible = state.canSave,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                LogTeaActionButtons(
                    onLogSessionClicked = { onIntent(LogTeaIntent.SaveAsCompleted) },
                    onBeginSessionClicked = { onIntent(LogTeaIntent.StartTimerClicked) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    } // end Box

    LogTeaDialogs(
        state = state,
        onIntent = onIntent,
    )
}

/**
 * Dialogs layered over the log screen, each shown when its state flag is set.
 */
@Composable
private fun LogTeaDialogs(
    state: LogTeaState,
    onIntent: (LogTeaIntent) -> Unit,
) {
    state.inProgressDialogState?.let { dialogState ->
        SessionInProgressDialog(
            teaName = dialogState.teaName,
            vesselName = dialogState.vesselName,
            session = dialogState.session,
            isProcessing = dialogState.isProcessing,
            onResume = { onIntent(LogTeaIntent.ResumeInProgress) },
            onCompleteAndContinue = { onIntent(LogTeaIntent.CompleteInProgressAndContinue) },
            onDiscardAndContinue = { onIntent(LogTeaIntent.DiscardInProgressAndContinue) },
            onDismiss = { onIntent(LogTeaIntent.DismissInProgressDialog) },
        )
    }

    if (state.showTeaPicker) {
        TeaPickerSheet(
            teas = state.availableTeas,
            recentTeas = state.suggestedTeas,
            teaTypes = state.availableTeaTypes,
            query = state.teaPickerQuery,
            filter = state.teaPickerFilter,
            selectedTeaId = state.selectedTea?.id,
            onQueryChanged = { query ->
                onIntent(LogTeaIntent.TeaPickerQueryChanged(query))
            },
            onFilterChanged = { filter ->
                onIntent(LogTeaIntent.TeaPickerFilterChanged(filter))
            },
            onTeaSelected = { tea ->
                onIntent(LogTeaIntent.TeaSelected(tea.id))
            },
            onTeaCreated = { name, teaTypeId ->
                onIntent(LogTeaIntent.QuickAddTeaSaved(name, teaTypeId))
            },
            onDismiss = { onIntent(LogTeaIntent.HideTeaPicker) },
        )
    }

    if (state.showChooseMethodDialog && state.availableConfigurations.isNotEmpty()) {
        ChooseMethodDialog(
            configurations = state.availableConfigurations,
            selectedConfigurationId = state.usedConfigurationId,
            onSelect = { configId ->
                onIntent(LogTeaIntent.MethodSelected(configId))
            },
            onDismiss = {
                onIntent(LogTeaIntent.DismissChooseMethodDialog)
            },
        )
    }

    if (state.showCompleteSessionDialog) {
        CompleteSessionDialog(
            notes = state.completionDialogNotes,
            rating = state.completionRating,
            isSaving = state.isSaving,
            onNotesChanged = { onIntent(LogTeaIntent.CompletionDialogNotesChanged(it)) },
            onRatingChanged = { onIntent(LogTeaIntent.CompletionRatingChanged(it)) },
            onConfirm = { onIntent(LogTeaIntent.ConfirmCompleteSession) },
            onDismiss = { onIntent(LogTeaIntent.DismissCompleteSessionDialog) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaScreenPreview() {
    LeafLogTheme {
        LogTeaContent(
            state = LogTeaState(
                selectedTea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    totalSessions = 12,
                    averageRating = 4.2f,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "green",
                    name = "Green",
                    colorHex = "#4CAF50",
                ),
                teaQuantityGrams = 5.0,
                waterQuantityMl = 200.0,
                temperatureCelsius = 80.0,
                brewingTime = 2.minutes + 30.seconds,
                selectedVessel = BrewingVessel(
                    id = "gaiwan",
                    name = "Gaiwan",
                    iconName = "gaiwan",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                selectedWaterType = WaterType.FILTERED,
                availableVessels = listOf(
                    BrewingVessel(
                        id = "gaiwan",
                        name = "Gaiwan",
                        iconName = "gaiwan",
                        isSystemDefault = true,
                        displayOrder = 0,
                        createdAt = PreviewInstant,
                        updatedAt = PreviewInstant,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaScreenEmptyPreview() {
    LeafLogTheme {
        LogTeaContent(
            state = LogTeaState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaScreenTeaBagPreview() {
    LeafLogTheme {
        LogTeaContent(
            state = LogTeaState(
                selectedTea = Tea(
                    id = "tea-1",
                    name = "English Breakfast",
                    teaTypeId = "black",
                    totalSessions = 3,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "black",
                    name = "Black",
                    colorHex = "#795548",
                ),
                waterQuantityMl = 250.0,
                temperatureCelsius = 100.0,
                brewingTime = 3.minutes,
                isTeaBag = true,
                selectedVessel = BrewingVessel(
                    id = "mug",
                    name = "Mug",
                    iconName = "mug",
                    isSystemDefault = true,
                    displayOrder = 1,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                availableVessels = listOf(
                    BrewingVessel(
                        id = "mug",
                        name = "Mug",
                        iconName = "mug",
                        isSystemDefault = true,
                        displayOrder = 1,
                        createdAt = PreviewInstant,
                        updatedAt = PreviewInstant,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaScreenOptionalFilledPreview() {
    LeafLogTheme {
        LogTeaContent(
            state = LogTeaState(
                selectedTea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    totalSessions = 5,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "green",
                    name = "Green",
                    colorHex = "#4CAF50",
                ),
                waterQuantityMl = 200.0,
                temperatureCelsius = 80.0,
                brewingTime = 2.minutes + 30.seconds,
                teaQuantityGrams = 5.0,
                location = "Kitchen",
                selectedWaterType = WaterType.SPRING,
                selectedVessel = BrewingVessel(
                    id = "gaiwan",
                    name = "Gaiwan",
                    iconName = "gaiwan",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
            ),
            onIntent = {},
        )
    }
}
