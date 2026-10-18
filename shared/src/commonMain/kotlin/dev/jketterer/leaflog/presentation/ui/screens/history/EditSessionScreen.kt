package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.DiscardChangesGuardDialog
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_tea_quantity
import leaf_log.shared.generated.resources.field_temperature_required
import leaf_log.shared.generated.resources.field_water_quantity_required
import leaf_log.shared.generated.resources.header_notes
import leaf_log.shared.generated.resources.header_photos
import leaf_log.shared.generated.resources.header_rating
import leaf_log.shared.generated.resources.header_session_details
import leaf_log.shared.generated.resources.header_steep_parameters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes

/**
 * Edit Session Screen - edit both parent sessions and individual steeps.
 */
@Composable
fun EditSessionScreen(
    sessionId: String,
    editFullSession: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: EditSessionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(sessionId, editFullSession) {
        viewModel.onIntent(EditSessionIntent.LoadSession(sessionId, editFullSession))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditSessionNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    // Discarding also deletes any photos already written to disk, or they'd be orphaned.
    DiscardChangesGuardDialog(
        hasChanges = state.hasChanges,
        onDiscard = { viewModel.onIntent(EditSessionIntent.Discard) },
    ) { onBack ->
        EditSessionContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun EditSessionContent(
    state: EditSessionState,
    onIntent: (EditSessionIntent) -> Unit,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isParentSession) {
                            "Edit Session"
                        } else {
                            "Edit Steep ${state.steepNumber ?: ""}"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onIntent(EditSessionIntent.SaveClicked) },
                        enabled = state.hasChanges && state.isValid && !state.isSaving
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Check,
                            contentDescription = "Save",
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Steep Parameters Section (only when NOT editing full parent session)
                if (!state.isParentSession) {
                    item(key = "parameters_header") {
                        SectionHeader(stringResource(Res.string.header_steep_parameters))
                    }

                    // Brewing Time
                    item(key = "brewing_time") {
                        DurationPicker(
                            duration = state.brewingTime,
                            onDurationChange = { duration ->
                                duration?.let { onIntent(EditSessionIntent.BrewingTimeChanged(it)) }
                            },
                            label = "Brewing Time *",
                            isError = state.brewingTimeError != null,
                            errorMessage = state.brewingTimeError,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Temperature
                    item(key = "temperature") {
                        UnitInputField(
                            value = state.temperatureCelsius,
                            onValueChange = { onIntent(EditSessionIntent.TemperatureChanged(it)) },
                            unit = state.userPreferences.temperatureUnit,
                            label = stringResource(Res.string.field_temperature_required),
                            isError = state.temperatureError != null,
                            supportingText = state.temperatureError?.let { stringResource(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Session Details Section (parent only)
                if (state.isParentSession) {
                    item(key = "session_details_header") {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(stringResource(Res.string.header_session_details))
                        }
                    }

                    // Vessel Selector
                    item(key = "vessel") {
                        VesselSelector(
                            vessels = state.availableVessels,
                            selectedVessel = state.selectedVessel,
                            onVesselSelected = { vessel ->
                                onIntent(EditSessionIntent.VesselSelected(vessel))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Water Type Selector
                    item(key = "water_type") {
                        WaterTypeSelector(
                            selectedWaterType = state.selectedWaterType,
                            onWaterTypeSelected = { waterType ->
                                onIntent(EditSessionIntent.WaterTypeSelected(waterType))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Water Quantity
                    item(key = "water_quantity") {
                        UnitInputField(
                            value = state.waterQuantityMl,
                            onValueChange = { onIntent(EditSessionIntent.WaterQuantityChanged(it)) },
                            unit = state.userPreferences.volumeUnit,
                            label = stringResource(Res.string.field_water_quantity_required),
                            isError = state.waterQuantityError != null,
                            supportingText = state.waterQuantityError?.let { stringResource(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Tea Quantity
                    item(key = "tea_quantity") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    onClick = { onIntent(EditSessionIntent.TeaBagModeChanged(false)) },
                                    selected = !state.isTeaBag,
                                    label = { Text("Loose Leaf") },
                                )
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    onClick = { onIntent(EditSessionIntent.TeaBagModeChanged(true)) },
                                    selected = state.isTeaBag,
                                    label = { Text("Tea Bag") },
                                )
                            }
                            if (!state.isTeaBag) {
                                UnitInputField(
                                    value = state.teaQuantityGrams,
                                    onValueChange = { onIntent(EditSessionIntent.TeaQuantityChanged(it)) },
                                    unit = state.userPreferences.weightUnit,
                                    label = stringResource(Res.string.field_tea_quantity),
                                    isError = state.teaQuantityError != null,
                                    supportingText = state.teaQuantityError?.let { stringResource(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    // Location
                    item(key = "location") {
                        OutlinedTextField(
                            value = state.location,
                            onValueChange = { onIntent(EditSessionIntent.LocationChanged(it)) },
                            label = { Text("Location (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )
                    }
                }

                // Rating Section (only when NOT editing full parent session)
                if (!state.isParentSession) {
                    item(key = "rating_header") {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(stringResource(Res.string.header_rating))
                        }
                    }

                    item(key = "rating") {
                        RatingSelector(
                            rating = state.rating,
                            onRatingChange = { rating ->
                                onIntent(EditSessionIntent.RatingChanged(rating))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Notes Section (only when NOT editing full parent session)
                if (!state.isParentSession) {
                    item(key = "notes_header") {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(stringResource(Res.string.header_notes))
                        }
                    }

                    item(key = "notes") {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { onIntent(EditSessionIntent.NotesChanged(it)) },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )
                    }

                    // Photos Section (only when NOT editing full parent session)
                    item(key = "photos_header") {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(stringResource(Res.string.header_photos))
                        }
                    }

                    item(key = "photos") {
                        PhotoGrid(
                            photos = state.photos,
                            onAddPhoto = { onIntent(EditSessionIntent.PhotoSelected(it)) },
                            onRemovePhoto = { onIntent(EditSessionIntent.PhotoRemoved(it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    // Save Configuration Dialog
    if (state.showSaveConfigurationDialog && state.savedSession != null) {
        val session = state.savedSession
        val first = session.steeps.first()
        val vessel = state.selectedVessel
        if (vessel != null && state.teaName != null) {
            SaveConfigurationDialog(
                teaName = state.teaName,
                vesselName = vessel.name,
                teaQuantityGrams = session.session.teaQuantityGrams,
                waterQuantityMl = first.waterQuantityMl,
                temperatureCelsius = first.temperatureCelsius,
                brewingTimeSeconds = first.brewingTime.inWholeSeconds.toInt(),
                rating = session.effectiveRating,
                suggestedLabel = state.suggestedConfigurationLabel,
                onSave = { label ->
                    onIntent(EditSessionIntent.SaveConfigurationClicked(label.ifBlank { null }))
                },
                onDismiss = { onIntent(EditSessionIntent.SkipSaveConfiguration) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSessionScreenPreview() {
    LeafLogTheme {
        EditSessionContent(
            state = EditSessionState(
                isParentSession = true,
                brewingTime = 3.minutes,
                temperatureCelsius = 85.0,
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
                waterQuantityMl = 200.0,
                teaQuantityGrams = 5.0,
                isTeaBag = false,
                location = "Home",
                rating = 4f,
                notes = "Excellent brew",
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
private fun EditSteepScreenPreview() {
    LeafLogTheme {
        EditSessionContent(
            state = EditSessionState(
                isParentSession = false,
                steepNumber = 2,
                brewingTime = 1.minutes,
                temperatureCelsius = 90.0,
                notes = "Second steep, stronger flavor",
            ),
            onIntent = {},
        )
    }
}
