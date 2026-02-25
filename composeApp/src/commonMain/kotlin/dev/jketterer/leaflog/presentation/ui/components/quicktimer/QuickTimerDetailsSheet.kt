package dev.jketterer.leaflog.presentation.ui.components.quicktimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock

/**
 * Two-step bottom sheet for adding details to a quick timer.
 * Step 1: Select tea and vessel
 * Step 2: Review/edit brewing parameters (pre-filled based on selection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTimerDetailsSheet(
    step: Int,
    selectedTea: Tea?,
    selectedVessel: BrewingVessel?,
    teaQuantityGrams: String,
    temperatureCelsius: String,
    waterQuantityMl: String,
    waterType: WaterType,
    prefillSource: PrefillSource,
    availableTeas: List<Tea>,
    availableVessels: List<BrewingVessel>,
    teaSearchQuery: String,
    userPreferences: UserPreferences,
    onTeaSearchQueryChanged: (String) -> Unit,
    onTeaSelected: (String) -> Unit,
    onVesselSelected: (String) -> Unit,
    onTeaQuantityChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        when (step) {
            1 -> Step1Content(
                selectedTea = selectedTea,
                selectedVessel = selectedVessel,
                availableTeas = availableTeas,
                availableVessels = availableVessels,
                teaSearchQuery = teaSearchQuery,
                userPreferences = userPreferences,
                onTeaSearchQueryChanged = onTeaSearchQueryChanged,
                onTeaSelected = onTeaSelected,
                onVesselSelected = onVesselSelected,
                onNext = onNextStep,
                onCancel = onDismiss,
            )

            2 -> Step2Content(
                selectedTea = selectedTea,
                teaQuantityGrams = teaQuantityGrams,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                waterType = waterType,
                prefillSource = prefillSource,
                userPreferences = userPreferences,
                onTeaQuantityChanged = onTeaQuantityChanged,
                onTemperatureChanged = onTemperatureChanged,
                onWaterQuantityChanged = onWaterQuantityChanged,
                onWaterTypeSelected = onWaterTypeSelected,
                onBack = onPreviousStep,
                onDone = onDismiss,
            )
        }
    }
}

@Composable
private fun Step1Content(
    selectedTea: Tea?,
    selectedVessel: BrewingVessel?,
    availableTeas: List<Tea>,
    availableVessels: List<BrewingVessel>,
    teaSearchQuery: String,
    userPreferences: UserPreferences,
    onTeaSearchQueryChanged: (String) -> Unit,
    onTeaSelected: (String) -> Unit,
    onVesselSelected: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val canProceed = selectedTea != null && selectedVessel != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Add Session Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Step 1 of 2: Select tea and vessel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tea Selection
        Text(
            text = "Tea",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTea != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedTea.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { onTeaSearchQueryChanged("") }) {
                        Text("Change")
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = teaSearchQuery,
                onValueChange = onTeaSearchQueryChanged,
                label = { Text("Search teas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show filtered tea list
            val filteredTeas = if (teaSearchQuery.isBlank()) {
                availableTeas.take(5)
            } else {
                availableTeas.filter {
                    it.name.contains(teaSearchQuery, ignoreCase = true)
                }.take(5)
            }

            if (filteredTeas.isEmpty() && teaSearchQuery.isNotBlank()) {
                Text(
                    text = "No teas found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredTeas, key = { it.id }) { tea ->
                        TextButton(
                            onClick = { onTeaSelected(tea.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = tea.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vessel Selection
        VesselSelector(
            vessels = availableVessels,
            selectedVessel = selectedVessel,
            onVesselSelected = { vessel -> onVesselSelected(vessel.id) },
            label = "Brewing Vessel",
            volumeUnit = userPreferences.volumeUnit,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier.weight(1f),
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun Step2Content(
    selectedTea: Tea?,
    teaQuantityGrams: String,
    temperatureCelsius: String,
    waterQuantityMl: String,
    waterType: WaterType,
    prefillSource: PrefillSource,
    userPreferences: UserPreferences,
    onTeaQuantityChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val isValid = temperatureCelsius.isNotBlank() && waterQuantityMl.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Brewing Parameters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Step 2 of 2: Review brewing parameters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-fill Banner
        if (prefillSource != PrefillSource.None && selectedTea != null) {
            PrefillBanner(
                source = prefillSource,
                teaName = selectedTea.name,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tea Quantity
        OutlinedTextField(
            value = teaQuantityGrams,
            onValueChange = onTeaQuantityChanged,
            label = { Text("Tea Quantity") },
            supportingText = { Text("Optional - leave empty for tea bags") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text("g") },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature
        OutlinedTextField(
            value = temperatureCelsius,
            onValueChange = onTemperatureChanged,
            label = { Text("Temperature") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text(TemperatureFormatter.getUnitSymbol(userPreferences.temperatureUnit)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Water Quantity
        OutlinedTextField(
            value = waterQuantityMl,
            onValueChange = onWaterQuantityChanged,
            label = { Text("Water Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text(userPreferences.volumeUnit.symbol) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Water Type
        WaterTypeSelector(
            selectedWaterType = waterType,
            onWaterTypeSelected = onWaterTypeSelected,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back")
            }

            Button(
                onClick = onDone,
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text("Done")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Step1ContentPreview() {
    LeafLogTheme {
        Step1Content(
            selectedTea = null,
            selectedVessel = null,
            availableTeas = emptyList(),
            availableVessels = emptyList(),
            teaSearchQuery = "",
            userPreferences = UserPreferences(),
            onTeaSearchQueryChanged = {},
            onTeaSelected = {},
            onVesselSelected = {},
            onNext = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Step1ContentWithSelectionPreview() {
    LeafLogTheme {
        Step1Content(
            selectedTea = Tea(
                id = "tea-1",
                name = "Dragon Well",
                teaTypeId = "green",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                syncStatus = dev.jketterer.leaflog.domain.models.SyncStatus.LOCAL_ONLY,
            ),
            selectedVessel = BrewingVessel(
                id = "gaiwan",
                name = "Gaiwan",
                iconName = "gaiwan",
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ),
            availableTeas = emptyList(),
            availableVessels = emptyList(),
            teaSearchQuery = "",
            userPreferences = UserPreferences(),
            onTeaSearchQueryChanged = {},
            onTeaSelected = {},
            onVesselSelected = {},
            onNext = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Step2ContentPreview() {
    LeafLogTheme {
        Step2Content(
            selectedTea = Tea(
                id = "tea-1",
                name = "Dragon Well",
                teaTypeId = "green",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                syncStatus = dev.jketterer.leaflog.domain.models.SyncStatus.LOCAL_ONLY,
            ),
            teaQuantityGrams = "5",
            temperatureCelsius = "80",
            waterQuantityMl = "200",
            waterType = WaterType.FILTERED,
            prefillSource = PrefillSource.SavedConfig,
            userPreferences = UserPreferences(),
            onTeaQuantityChanged = {},
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
            onWaterTypeSelected = {},
            onBack = {},
            onDone = {},
        )
    }
}
