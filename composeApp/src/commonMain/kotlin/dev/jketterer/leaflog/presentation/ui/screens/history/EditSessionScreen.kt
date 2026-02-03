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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
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

    EditSessionContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionContent(
    state: EditSessionState,
    onIntent: (EditSessionIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                IconButton(onClick = { onIntent(EditSessionIntent.BackClicked) }) {
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Steep Parameters Section (only when NOT editing full parent session)
            if (!state.isParentSession) {
                item(key = "parameters_header") {
                    Text(
                        text = "STEEP PARAMETERS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
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
                    OutlinedTextField(
                        value = state.temperatureCelsius,
                        onValueChange = { onIntent(EditSessionIntent.TemperatureChanged(it)) },
                        label = { Text("${TemperatureFormatter.getInputLabel(state.userPreferences.temperatureUnit)} *") },
                        isError = state.temperatureError != null,
                        supportingText = state.temperatureError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text(TemperatureFormatter.getUnitSymbol(state.userPreferences.temperatureUnit)) },
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
                        Text(
                            text = "SESSION DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
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
                        volumeUnit = state.userPreferences.volumeUnit,
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
                    OutlinedTextField(
                        value = state.waterQuantityMl,
                        onValueChange = { onIntent(EditSessionIntent.WaterQuantityChanged(it)) },
                        label = { Text("${VolumeFormatter.getInputLabel(state.userPreferences.volumeUnit)} *") },
                        isError = state.waterQuantityError != null,
                        supportingText = state.waterQuantityError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Tea Quantity
                item(key = "tea_quantity") {
                    OutlinedTextField(
                        value = state.teaQuantityGrams,
                        onValueChange = { onIntent(EditSessionIntent.TeaQuantityChanged(it)) },
                        label = { Text("Tea Quantity (g)") },
                        supportingText = { Text("Optional - leave empty for tea bags") },
                        isError = state.teaQuantityError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Location
                item(key = "location") {
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = { onIntent(EditSessionIntent.LocationChanged(it)) },
                        label = { Text("Location (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                        Text(
                            text = "RATING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
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
                        Text(
                            text = "NOTES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
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
                    )
                }

                // Photos Section (only when NOT editing full parent session)
                item(key = "photos_header") {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PHOTOS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                item(key = "photos") {
                    OutlinedButton(
                        onClick = { onIntent(EditSessionIntent.AddPhotoClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("📷 Add Photos (${state.photos.size})")
                    }
                }
            }
        }
    }

    // Discard Dialog
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(EditSessionIntent.CancelDiscard) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(EditSessionIntent.ConfirmDiscard) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(EditSessionIntent.CancelDiscard) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Configuration Dialog
    if (state.showSaveConfigurationDialog && state.savedSession != null) {
        val session = state.savedSession
        val vessel = state.selectedVessel
        if (vessel != null && state.teaName != null) {
            SaveConfigurationDialog(
                teaName = state.teaName,
                vesselName = vessel.name,
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                temperatureCelsius = session.temperatureCelsius,
                brewingTimeSeconds = session.brewingTime.inWholeSeconds.toInt(),
                rating = session.rating ?: 5f,
                suggestedLabel = "", // Will be generated by use case
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
                temperatureCelsius = "85",
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
                waterQuantityMl = "200",
                teaQuantityGrams = "5",
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
private fun EditSteepScreenPreview() {
    LeafLogTheme {
        EditSessionContent(
            state = EditSessionState(
                isParentSession = false,
                steepNumber = 2,
                brewingTime = 1.minutes,
                temperatureCelsius = "90",
                notes = "Second steep, stronger flavor",
            ),
            onIntent = {},
        )
    }
}
