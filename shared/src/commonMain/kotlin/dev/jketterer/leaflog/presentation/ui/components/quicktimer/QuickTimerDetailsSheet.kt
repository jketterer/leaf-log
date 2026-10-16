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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_tea_quantity
import leaf_log.shared.generated.resources.field_temperature
import leaf_log.shared.generated.resources.field_water_quantity
import leaf_log.shared.generated.resources.no_teas_found
import org.jetbrains.compose.resources.stringResource

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
    teaQuantityGrams: Double?,
    isTeaBag: Boolean,
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
    waterType: WaterType,
    prefillSource: PrefillSource,
    availableTeas: List<Tea>,
    availableVessels: List<BrewingVessel>,
    teaSearchQuery: String,
    userPreferences: UserPreferences,
    onTeaSearchQueryChanged: (String) -> Unit,
    onTeaSelected: (String) -> Unit,
    onVesselSelected: (String) -> Unit,
    onTeaQuantityChanged: (Double?) -> Unit,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTemperatureChanged: (Double?) -> Unit,
    onWaterQuantityChanged: (Double?) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    ),
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
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                isWaterQuantityEditable = isWaterQuantityEditable,
                waterType = waterType,
                prefillSource = prefillSource,
                userPreferences = userPreferences,
                onTeaQuantityChanged = onTeaQuantityChanged,
                onTeaBagModeChanged = onTeaBagModeChanged,
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
                EmptyState(
                    message = stringResource(Res.string.no_teas_found),
                    modifier = Modifier.height(150.dp),
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
    teaQuantityGrams: Double?,
    isTeaBag: Boolean,
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
    isWaterQuantityEditable: Boolean,
    waterType: WaterType,
    prefillSource: PrefillSource,
    userPreferences: UserPreferences,
    onTeaQuantityChanged: (Double?) -> Unit,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTemperatureChanged: (Double?) -> Unit,
    onWaterQuantityChanged: (Double?) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val isValid = temperatureCelsius != null &&
            (!isWaterQuantityEditable || waterQuantityMl != null) &&
            (isTeaBag || teaQuantityGrams != null)

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

            UnitInputField(
                value = teaQuantityGrams,
                onValueChange = onTeaQuantityChanged,
                unit = userPreferences.weightUnit,
                label = stringResource(Res.string.field_tea_quantity),
                presets = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        UnitInputField(
            value = temperatureCelsius,
            onValueChange = onTemperatureChanged,
            unit = userPreferences.temperatureUnit,
            label = stringResource(Res.string.field_temperature),
            presets = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Water Quantity — only shown when vessel has no capacity set
        if (isWaterQuantityEditable) {
            Spacer(modifier = Modifier.height(16.dp))

            UnitInputField(
                value = waterQuantityMl,
                onValueChange = onWaterQuantityChanged,
                unit = userPreferences.volumeUnit,
                label = stringResource(Res.string.field_water_quantity),
                presets = true,
                modifier = Modifier.fillMaxWidth(),
            )
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
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant,
                syncStatus = dev.jketterer.leaflog.domain.models.SyncStatus.LOCAL_ONLY,
            ),
            selectedVessel = BrewingVessel(
                id = "gaiwan",
                name = "Gaiwan",
                iconName = "gaiwan",
                capacityMl = 120,
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant,
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
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant,
                syncStatus = dev.jketterer.leaflog.domain.models.SyncStatus.LOCAL_ONLY,
            ),
            teaQuantityGrams = 5.0,
            isTeaBag = false,
            temperatureCelsius = 80.0,
            waterQuantityMl = 120.0,
            isWaterQuantityEditable = false,
            waterType = WaterType.FILTERED,
            prefillSource = PrefillSource.SavedConfig,
            userPreferences = UserPreferences(),
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
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
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant,
                syncStatus = dev.jketterer.leaflog.domain.models.SyncStatus.LOCAL_ONLY,
            ),
            teaQuantityGrams = null,
            isTeaBag = true,
            temperatureCelsius = 80.0,
            waterQuantityMl = null,
            isWaterQuantityEditable = true,
            waterType = WaterType.FILTERED,
            prefillSource = PrefillSource.None,
            userPreferences = UserPreferences(),
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
            onWaterTypeSelected = {},
            onBack = {},
            onDone = {},
        )
    }
}
