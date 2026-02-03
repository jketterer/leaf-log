package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.ChooseMethodDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
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
    onNavigateBack: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: LogTeaViewModel,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId, vesselId) {
        viewModel.onIntent(LogTeaIntent.TeaSelected(teaId))
        viewModel.onIntent(LogTeaIntent.VesselSelected(vesselId))
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTeaContent(
    state: LogTeaState,
    onIntent: (LogTeaIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Tea Session") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LogTeaIntent.BackClicked) }) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tea Selection
            item(key = "tea_selection") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Tea *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    if (state.selectedTea != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = state.selectedTea.name,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    // TODO: Show tea type from lookup
                                }
                                TextButton(onClick = { onIntent(LogTeaIntent.ShowTeaSearchDialog) }) {
                                    Text("Change")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { onIntent(LogTeaIntent.ShowTeaSearchDialog) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Select Tea")
                        }
                    }

                    if (state.teaError != null) {
                        Text(
                            text = state.teaError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // Vessel Selector
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
                )
            }

            // Brewing Parameters Section
            item(key = "parameters_header") {
                Text(
                    text = "BREWING PARAMETERS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Pre-fill Banner
            if (state.prefillSource != PrefillSource.None && state.selectedTea != null) {
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
            if (state.availableConfigurations.isNotEmpty() &&
                state.selectedTea != null &&
                state.selectedVessel != null &&
                state.prefillSource == PrefillSource.None
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

            // Tea Quantity
            item(key = "tea_quantity") {
                OutlinedTextField(
                    value = state.teaQuantityGrams,
                    onValueChange = { onIntent(LogTeaIntent.TeaQuantityChanged(it)) },
                    label = { Text("Tea Quantity (g)") },
                    supportingText = { Text("Optional - leave empty for tea bags") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }


            // Brewing Time
            item(key = "brewing_time") {
                DurationPicker(
                    duration = state.brewingTime,
                    onDurationChange = { duration ->
                        duration?.let { onIntent(LogTeaIntent.BrewingTimeChanged(it)) }
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
                    onValueChange = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
                    label = { Text("Temperature *") },
                    isError = state.temperatureError != null,
                    supportingText = state.temperatureError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(TemperatureFormatter.getUnitSymbol(state.userPreferences.temperatureUnit)) },
                )
            }

            // Water Quantity
            item(key = "water_quantity") {
                OutlinedTextField(
                    value = state.waterQuantityMl,
                    onValueChange = { onIntent(LogTeaIntent.WaterQuantityChanged(it)) },
                    label = { Text("Water Quantity *") },
                    isError = state.waterQuantityError != null,
                    supportingText = state.waterQuantityError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(state.userPreferences.volumeUnit.symbol) }
                )
            }

            // Water Type Selector
            item(key = "water_type") {
                WaterTypeSelector(
                    selectedWaterType = state.selectedWaterType,
                    onWaterTypeSelected = { waterType ->
                        onIntent(LogTeaIntent.WaterTypeSelected(waterType))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Location
            item(key = "location") {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { onIntent(LogTeaIntent.LocationChanged(it)) },
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Notes
            item(key = "notes") {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onIntent(LogTeaIntent.NotesChanged(it)) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }

            // Photos
            item(key = "photos") {
                OutlinedButton(
                    onClick = { onIntent(LogTeaIntent.AddPhotoClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📷 Add Photos (${state.photos.size})")
                }
            }

            // Action Buttons
            item(key = "actions") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            // TODO: Create session first, then navigate to timer with session ID
                            onIntent(LogTeaIntent.StartTimerClicked)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("⏱️ Start Timer & Brew")
                    }

                    OutlinedButton(
                        onClick = { onIntent(LogTeaIntent.SaveAsCompleted) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("Save Session")
                    }

                    OutlinedButton(
                        onClick = { onIntent(LogTeaIntent.SaveAsDraft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("Save as Draft")
                    }
                }
            }
        }
    }

    // Tea Search Dialog
    if (state.showTeaSearchDialog) {
        TeaSearchDialog(
            teas = state.availableTeas,
            searchQuery = state.teaSearchQuery,
            onSearchQueryChanged = { query ->
                onIntent(LogTeaIntent.TeaSearchQueryChanged(query))
            },
            onTeaSelected = { tea ->
                onIntent(LogTeaIntent.TeaSelected(tea.id))
            },
            onQuickAddClicked = { onIntent(LogTeaIntent.QuickAddTeaClicked) },
            onDismiss = { onIntent(LogTeaIntent.HideTeaSearchDialog) },
        )
    }

    // Quick Add Tea Dialog
    if (state.showQuickAddTeaDialog) {
        QuickAddTeaDialog(
            onTeaSaved = { tea ->
                onIntent(LogTeaIntent.QuickAddTeaSaved(tea))
            },
            onDismiss = {
                // TODO: Close dialog
            },
        )
    }

    // Choose Method Dialog
    if (state.showChooseMethodDialog && state.availableConfigurations.isNotEmpty()) {
        ChooseMethodDialog(
            configurations = state.availableConfigurations,
            selectedConfigurationId = state.usedConfigurationId,
            onSelect = { configId ->
                onIntent(LogTeaIntent.MethodSelected(configId))
            },
            onDismiss = {
                onIntent(LogTeaIntent.DismissChooseMethodDialog)
            }
        )
    }
}

@Composable
private fun TeaSearchDialog(
    teas: List<Tea>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onTeaSelected: (Tea) -> Unit,
    onQuickAddClicked: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tea") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = { Text("Search teas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (teas.isEmpty()) {
                    Text(
                        text = "No teas found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        teas.take(5).forEach { tea ->
                            TextButton(
                                onClick = { onTeaSelected(tea) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(tea.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onQuickAddClicked) {
                Text("+ Add New Tea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun QuickAddTeaDialog(
    onTeaSaved: (Tea) -> Unit,
    onDismiss: () -> Unit,
) {
    // TODO: Implement quick add tea dialog
    // For now, just a placeholder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Tea") },
        text = { Text("Quick add tea dialog - to be implemented") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
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
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
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

