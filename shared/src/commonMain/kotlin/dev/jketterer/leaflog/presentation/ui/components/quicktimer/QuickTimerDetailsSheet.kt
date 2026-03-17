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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.VolumeInputField
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
    isTeaBag: Boolean,
    temperatureDisplay: String,
    waterQuantityDisplay: String,
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
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onToggleVolumeUnit: () -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val isWaterQuantityEditable = selectedVessel?.capacityMl == null

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
                isTeaBag = isTeaBag,
                temperatureDisplay = temperatureDisplay,
                waterQuantityDisplay = waterQuantityDisplay,
                isWaterQuantityEditable = isWaterQuantityEditable,
                waterType = waterType,
                prefillSource = prefillSource,
                userPreferences = userPreferences,
                onTeaQuantityChanged = onTeaQuantityChanged,
                onTeaBagModeChanged = onTeaBagModeChanged,
                onTemperatureChanged = onTemperatureChanged,
                onToggleTemperatureUnit = onToggleTemperatureUnit,
                onWaterQuantityChanged = onWaterQuantityChanged,
                onToggleVolumeUnit = onToggleVolumeUnit,
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
    val focusManager = LocalFocusManager.current

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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (availableTeas.isEmpty() && teaSearchQuery.isNotBlank()) {
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
                    items(availableTeas, key = { it.id }) { tea ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2Content(
    selectedTea: Tea?,
    teaQuantityGrams: String,
    isTeaBag: Boolean,
    temperatureDisplay: String,
    waterQuantityDisplay: String,
    isWaterQuantityEditable: Boolean,
    waterType: WaterType,
    prefillSource: PrefillSource,
    userPreferences: UserPreferences,
    onTeaQuantityChanged: (String) -> Unit,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onToggleVolumeUnit: () -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val isValid = temperatureDisplay.isNotBlank() &&
            (!isWaterQuantityEditable || waterQuantityDisplay.isNotBlank()) &&
            (isTeaBag || teaQuantityGrams.isNotBlank())

    val tempUnit = userPreferences.temperatureUnit
    val volUnit = userPreferences.volumeUnit

    val tempPresets = remember(tempUnit) {
        when (tempUnit) {
            TemperatureUnit.CELSIUS -> listOf(60, 70, 75, 80, 85, 90, 95, 100)
            TemperatureUnit.FAHRENHEIT -> listOf(140, 160, 170, 175, 185, 195, 200, 208, 212)
        }.map { Preset("$it${tempUnit.symbol}", it.toString()) }
    }

    val waterPresets = remember(volUnit) {
        when (volUnit) {
            VolumeUnit.MILLILITERS -> listOf(100, 150, 200, 250, 300, 400, 500)
            VolumeUnit.FLUID_OUNCES -> listOf(4, 6, 8, 10, 12, 16)
        }.map { Preset("$it ${volUnit.symbol}", it.toString()) }
    }

    val teaQtyPresets = remember {
        listOf("1", "2", "3", "4", "5", "7", "10").map { Preset("${it}g", it) }
    }
    val focusManager = LocalFocusManager.current

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

        // Tea type selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onTeaBagModeChanged(false) },
                selected = !isTeaBag,
                label = { Text("Loose Leaf") },
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onTeaBagModeChanged(true) },
                selected = isTeaBag,
                label = { Text("Tea Bag") },
            )
        }

        if (!isTeaBag) {
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                OutlinedTextField(
                    value = teaQuantityGrams,
                    onValueChange = onTeaQuantityChanged,
                    label = { Text("Tea Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
                PresetChips(
                    presets = teaQtyPresets,
                    currentValue = teaQuantityGrams,
                    isSelected = { preset, current ->
                        preset.toFloatOrNull() == current?.toFloatOrNull()
                    },
                    onSelect = onTeaQuantityChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature
        Column {
            TemperatureInputField(
                value = temperatureDisplay,
                onValueChange = onTemperatureChanged,
                currentUnit = tempUnit,
                onToggleUnit = onToggleTemperatureUnit,
                label = { Text("Temperature") },
                modifier = Modifier.fillMaxWidth(),
            )
            PresetChips(
                presets = tempPresets,
                currentValue = temperatureDisplay,
                isSelected = { preset, current ->
                    preset.toFloatOrNull() == current?.toFloatOrNull()
                },
                onSelect = onTemperatureChanged,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Water Quantity — only shown when vessel has no capacity set
        if (isWaterQuantityEditable) {
            Spacer(modifier = Modifier.height(16.dp))

            Column {
                VolumeInputField(
                    value = waterQuantityDisplay,
                    onValueChange = onWaterQuantityChanged,
                    currentUnit = volUnit,
                    onToggleUnit = onToggleVolumeUnit,
                    label = { Text("Water Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                )
                PresetChips(
                    presets = waterPresets,
                    currentValue = waterQuantityDisplay,
                    isSelected = { preset, current ->
                        preset.toFloatOrNull() == current?.toFloatOrNull()
                    },
                    onSelect = onWaterQuantityChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

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
                capacityMl = 120,
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
private fun Step2ContentLooseLeafPreview() {
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
            isTeaBag = false,
            temperatureDisplay = "80",
            waterQuantityDisplay = "120",
            isWaterQuantityEditable = false,
            waterType = WaterType.FILTERED,
            prefillSource = PrefillSource.SavedConfig,
            userPreferences = UserPreferences(),
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onTemperatureChanged = {},
            onToggleTemperatureUnit = {},
            onWaterQuantityChanged = {},
            onToggleVolumeUnit = {},
            onWaterTypeSelected = {},
            onBack = {},
            onDone = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Step2ContentTeaBagPreview() {
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
            teaQuantityGrams = "",
            isTeaBag = true,
            temperatureDisplay = "80",
            waterQuantityDisplay = "",
            isWaterQuantityEditable = true,
            waterType = WaterType.FILTERED,
            prefillSource = PrefillSource.None,
            userPreferences = UserPreferences(),
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onTemperatureChanged = {},
            onToggleTemperatureUnit = {},
            onWaterQuantityChanged = {},
            onToggleVolumeUnit = {},
            onWaterTypeSelected = {},
            onBack = {},
            onDone = {},
        )
    }
}
