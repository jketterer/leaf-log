package dev.jketterer.leaflog.presentation.ui.screens.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.VolumeInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun CreateBrewingConfigurationScreen(
    teaId: String,
    configurationId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateBrewingConfigurationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId, configurationId) {
        viewModel.onIntent(CreateBrewingConfigurationIntent.LoadData(teaId, configurationId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is CreateBrewingConfigurationNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    CreateBrewingConfigurationContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBrewingConfigurationContent(
    state: CreateBrewingConfigurationState,
    onIntent: (CreateBrewingConfigurationIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Brewing Method" else "Add Brewing Method") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CreateBrewingConfigurationIntent.NavigateBack) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onIntent(CreateBrewingConfigurationIntent.Save) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = state.canSave && !state.isSaving,
                ) {
                    Text("Save")
                }
            }
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Vessel selector
            item(key = "vessel") {
                VesselSelector(
                    vessels = state.vessels,
                    selectedVessel = state.selectedVessel,
                    onVesselSelected = { vessel ->
                        onIntent(CreateBrewingConfigurationIntent.SelectVessel(vessel.id))
                    },
                    label = "Brewing Vessel *",
                    volumeUnit = state.userPreferences.volumeUnit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Label (optional)
            item(key = "label") {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = { onIntent(CreateBrewingConfigurationIntent.UpdateLabel(it)) },
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Brewing Time
            item(key = "brewing_time") {
                val brewingTimePresets = remember {
                    listOf(
                        Preset("0:30", 30.seconds),
                        Preset("0:45", 45.seconds),
                        Preset("1:00", 1.minutes),
                        Preset("1:30", 1.minutes + 30.seconds),
                        Preset("2:00", 2.minutes),
                        Preset("3:00", 3.minutes),
                        Preset("4:00", 4.minutes),
                        Preset("5:00", 5.minutes),
                    )
                }

                Column {
                    DurationPicker(
                        duration = state.brewingTime,
                        onDurationChange = { duration ->
                            duration?.let {
                                onIntent(
                                    CreateBrewingConfigurationIntent.UpdateBrewingTime(
                                        it
                                    )
                                )
                            }
                        },
                        label = "Brewing Time *",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = brewingTimePresets,
                        currentValue = state.brewingTime,
                        onSelect = { onIntent(CreateBrewingConfigurationIntent.UpdateBrewingTime(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Temperature
            item(key = "temperature") {
                val displayValue = state.temperatureDisplay.ifEmpty {
                    state.temperatureCelsius.toDoubleOrNull()?.let { celsius ->
                        state.userPreferences.temperatureUnit.fromCelsius(celsius).toString()
                    } ?: state.temperatureCelsius
                }
                val tempUnit = state.userPreferences.temperatureUnit

                val tempPresets = remember(tempUnit) {
                    when (tempUnit) {
                        TemperatureUnit.CELSIUS -> listOf(60, 70, 75, 80, 85, 90, 95, 100)
                        TemperatureUnit.FAHRENHEIT -> listOf(140, 160, 170, 175, 185, 195, 200, 212)
                    }.map { Preset("$it${tempUnit.symbol}", it.toString()) }
                }

                Column {
                    TemperatureInputField(
                        value = displayValue,
                        onValueChange = {
                            onIntent(
                                CreateBrewingConfigurationIntent.UpdateTemperature(
                                    it
                                )
                            )
                        },
                        currentUnit = tempUnit,
                        onToggleUnit = { onIntent(CreateBrewingConfigurationIntent.ToggleTemperatureUnit) },
                        label = { Text("Temperature *") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = tempPresets,
                        currentValue = displayValue,
                        isSelected = { preset, current ->
                            preset.toFloatOrNull() == current?.toFloatOrNull()
                        },
                        onSelect = { onIntent(CreateBrewingConfigurationIntent.UpdateTemperature(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Water Quantity
            item(key = "water_quantity") {
                val displayValue = state.waterQuantityDisplay.ifEmpty {
                    state.waterQuantityMl.toDoubleOrNull()?.let { ml ->
                        state.userPreferences.volumeUnit.fromMilliliters(ml).toString()
                    } ?: state.waterQuantityMl
                }
                val volUnit = state.userPreferences.volumeUnit

                val waterPresets = remember(volUnit) {
                    when (volUnit) {
                        VolumeUnit.MILLILITERS -> listOf(100, 150, 200, 250, 300, 400, 500)
                        VolumeUnit.FLUID_OUNCES -> listOf(4, 6, 8, 10, 12, 16)
                    }.map { Preset("$it ${volUnit.symbol}", it.toString()) }
                }

                Column {
                    VolumeInputField(
                        value = displayValue,
                        onValueChange = {
                            onIntent(
                                CreateBrewingConfigurationIntent.UpdateWaterQuantity(
                                    it
                                )
                            )
                        },
                        currentUnit = volUnit,
                        onToggleUnit = { onIntent(CreateBrewingConfigurationIntent.ToggleVolumeUnit) },
                        label = { Text("Water Quantity *") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = waterPresets,
                        currentValue = displayValue,
                        isSelected = { preset, current ->
                            preset.toFloatOrNull() == current?.toFloatOrNull()
                        },
                        onSelect = {
                            onIntent(
                                CreateBrewingConfigurationIntent.UpdateWaterQuantity(
                                    it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Tea Quantity (optional)
            item(key = "tea_quantity") {
                val teaQtyPresets = remember {
                    listOf("1", "2", "3", "4", "5", "7", "10").map {
                        Preset("${it}g", it)
                    }
                }

                Column {
                    OutlinedTextField(
                        value = state.teaQuantityGrams,
                        onValueChange = {
                            onIntent(
                                CreateBrewingConfigurationIntent.UpdateTeaQuantity(
                                    it
                                )
                            )
                        },
                        label = { Text("Tea Quantity (g)") },
                        supportingText = { Text("Optional - leave empty for tea bags") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    PresetChips(
                        presets = teaQtyPresets,
                        currentValue = state.teaQuantityGrams,
                        isSelected = { preset, current ->
                            preset.toFloatOrNull() == current?.toFloatOrNull()
                        },
                        onSelect = { onIntent(CreateBrewingConfigurationIntent.UpdateTeaQuantity(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Water Type
            item(key = "water_type") {
                WaterTypeSelector(
                    selectedWaterType = state.waterType,
                    onWaterTypeSelected = {
                        onIntent(
                            CreateBrewingConfigurationIntent.SelectWaterType(
                                it
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateBrewingConfigurationScreenPreview() {
    LeafLogTheme {
        CreateBrewingConfigurationContent(
            state = CreateBrewingConfigurationState(
                vessels = listOf(
                    BrewingVessel(
                        id = "vessel-1",
                        name = "Gaiwan (100ml)",
                        iconName = null,
                        capacityMl = 100,
                        isSystemDefault = true,
                        displayOrder = 0,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                ),
                selectedVessel = BrewingVessel(
                    id = "vessel-1",
                    name = "Gaiwan (100ml)",
                    iconName = null,
                    capacityMl = 100,
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                temperatureCelsius = "90",
                waterQuantityMl = "200",
                brewingTime = 3.minutes,
                userPreferences = UserPreferences(
                    temperatureUnit = TemperatureUnit.CELSIUS,
                    volumeUnit = VolumeUnit.MILLILITERS,
                ),
            ),
            onIntent = {},
        )
    }
}
