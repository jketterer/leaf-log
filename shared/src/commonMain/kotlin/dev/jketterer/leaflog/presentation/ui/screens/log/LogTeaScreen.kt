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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
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
import dev.jketterer.leaflog.presentation.ui.components.collection.QuickAddTeaDialog
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.ChooseMethodDialog
import dev.jketterer.leaflog.presentation.ui.components.session.CompleteSessionDialog
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Log Tea Screen - create a new brewing session.
 */
@Composable
fun LogTeaScreen(
    teaId: String?,
    vesselId: String?,
    configurationId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: LogTeaViewModel,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId, vesselId, configurationId) {
        viewModel.onIntent(LogTeaIntent.TeaSelected(teaId))
        viewModel.onIntent(LogTeaIntent.VesselSelected(vesselId))
        if (configurationId != null) {
            viewModel.onIntent(LogTeaIntent.MethodSelected(configurationId))
        }
    }

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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTeaContent(
    state: LogTeaState,
    onIntent: (LogTeaIntent) -> Unit,
    imageStorage: ImageStorage? = null,
) {
    var showOptionalFields by remember { mutableStateOf(false) }

    val hasOptionalData = state.selectedWaterType != state.userPreferences.defaultWaterType ||
            state.location.isNotEmpty()

    LaunchedEffect(hasOptionalData) {
        if (hasOptionalData) {
            showOptionalFields = true
        }
    }

    val optionalSummaryParts = buildList {
        if (state.selectedWaterType != WaterType.FILTERED) add(state.selectedWaterType.displayName)
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
                        volumeUnit = state.userPreferences.volumeUnit,
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
                        val tempUnit = state.userPreferences.temperatureUnit
                        val displayValue = state.temperatureDisplay.ifEmpty {
                            state.temperatureCelsius.toDoubleOrNull()?.let { celsius ->
                                tempUnit.fromCelsius(celsius).toString()
                            } ?: state.temperatureCelsius
                        }

                        TemperatureSection(
                            displayValue = displayValue,
                            unit = tempUnit,
                            errorMessage = state.temperatureError,
                            onTemperatureChanged = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
                            onToggleUnit = { onIntent(LogTeaIntent.ToggleTemperatureUnit) },
                        )
                    }

                    item(key = "water_quantity") {
                        val volUnit = state.userPreferences.volumeUnit
                        val displayValue = state.waterQuantityDisplay.ifEmpty {
                            state.waterQuantityMl.toDoubleOrNull()?.let { ml ->
                                volUnit.fromMilliliters(ml).toString()
                            } ?: state.waterQuantityMl
                        }

                        WaterQuantitySection(
                            displayValue = displayValue,
                            unit = volUnit,
                            errorMessage = state.waterQuantityError,
                            onWaterQuantityChanged = {
                                onIntent(LogTeaIntent.WaterQuantityChanged(it))
                            },
                            onToggleUnit = { onIntent(LogTeaIntent.ToggleVolumeUnit) },
                        )
                    }

                    item(key = "optional_section") {
                        OptionalDetailsSection(
                            expanded = showOptionalFields,
                            onExpandedChange = { showOptionalFields = it },
                            summary = optionalSummary,
                            selectedWaterType = state.selectedWaterType,
                            location = state.location,
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

        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { onIntent(LogTeaIntent.ClearError) }) {
                        Text("Dismiss")
                    }
                },
            ) {
                Text(error)
            }
        }
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
            userPreferences = state.userPreferences,
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
            onAddTeaClicked = { onIntent(LogTeaIntent.QuickAddTeaClicked) },
            onDismiss = { onIntent(LogTeaIntent.HideTeaPicker) },
        )
    }

    if (state.showChooseMethodDialog && state.availableConfigurations.isNotEmpty()) {
        ChooseMethodDialog(
            configurations = state.availableConfigurations,
            selectedConfigurationId = state.usedConfigurationId,
            userPreferences = state.userPreferences,
            onSelect = { configId ->
                onIntent(LogTeaIntent.MethodSelected(configId))
            },
            onDismiss = {
                onIntent(LogTeaIntent.DismissChooseMethodDialog)
            },
        )
    }

    if (state.showQuickAddTeaDialog) {
        QuickAddTeaDialog(
            teaTypes = state.availableTeaTypes,
            initialName = state.teaPickerQuery.trim(),
            onSave = { name, teaTypeId ->
                onIntent(LogTeaIntent.QuickAddTeaSaved(name, teaTypeId))
            },
            onDismiss = { onIntent(LogTeaIntent.DismissQuickAddTeaDialog) },
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
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "green",
                    name = "Green",
                    colorHex = "#4CAF50",
                ),
                teaQuantityGrams = "5",
                waterQuantityMl = "200",
                temperatureCelsius = "80",
                brewingTime = 2.minutes + 30.seconds,
                selectedVessel = BrewingVessel(
                    id = "gaiwan",
                    name = "Gaiwan",
                    iconName = "gaiwan",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                selectedWaterType = WaterType.FILTERED,
                availableVessels = listOf(
                    BrewingVessel(
                        id = "gaiwan",
                        name = "Gaiwan",
                        iconName = "gaiwan",
                        isSystemDefault = true,
                        displayOrder = 0,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
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
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "black",
                    name = "Black",
                    colorHex = "#795548",
                ),
                waterQuantityMl = "250",
                temperatureCelsius = "100",
                brewingTime = 3.minutes,
                isTeaBag = true,
                selectedVessel = BrewingVessel(
                    id = "mug",
                    name = "Mug",
                    iconName = "mug",
                    isSystemDefault = true,
                    displayOrder = 1,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                availableVessels = listOf(
                    BrewingVessel(
                        id = "mug",
                        name = "Mug",
                        iconName = "mug",
                        isSystemDefault = true,
                        displayOrder = 1,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
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
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                selectedTeaType = TeaType(
                    id = "green",
                    name = "Green",
                    colorHex = "#4CAF50",
                ),
                waterQuantityMl = "200",
                temperatureCelsius = "80",
                brewingTime = 2.minutes + 30.seconds,
                teaQuantityGrams = "5",
                location = "Kitchen",
                selectedWaterType = WaterType.SPRING,
                selectedVessel = BrewingVessel(
                    id = "gaiwan",
                    name = "Gaiwan",
                    iconName = "gaiwan",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
            ),
            onIntent = {},
        )
    }
}
