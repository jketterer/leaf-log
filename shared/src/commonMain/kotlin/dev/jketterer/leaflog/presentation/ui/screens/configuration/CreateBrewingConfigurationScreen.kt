package dev.jketterer.leaflog.presentation.ui.screens.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_tea_quantity
import leaf_log.shared.generated.resources.field_tea_quantity_hint
import leaf_log.shared.generated.resources.field_temperature_required
import leaf_log.shared.generated.resources.field_water_quantity_required
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
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
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

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
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun CreateBrewingConfigurationContent(
    state: CreateBrewingConfigurationState,
    onIntent: (CreateBrewingConfigurationIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusManager = LocalFocusManager.current

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

                item(key = "temperature") {
                    UnitInputField(
                        value = state.temperatureCelsius,
                        onValueChange = { onIntent(CreateBrewingConfigurationIntent.UpdateTemperature(it)) },
                        unit = state.userPreferences.temperatureUnit,
                        label = stringResource(Res.string.field_temperature_required),
                        presets = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "water_quantity") {
                    UnitInputField(
                        value = state.waterQuantityMl,
                        onValueChange = { onIntent(CreateBrewingConfigurationIntent.UpdateWaterQuantity(it)) },
                        unit = state.userPreferences.volumeUnit,
                        label = stringResource(Res.string.field_water_quantity_required),
                        presets = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "tea_quantity") {
                    UnitInputField(
                        value = state.teaQuantityGrams,
                        onValueChange = { onIntent(CreateBrewingConfigurationIntent.UpdateTeaQuantity(it)) },
                        unit = state.userPreferences.weightUnit,
                        label = stringResource(Res.string.field_tea_quantity),
                        supportingText = stringResource(Res.string.field_tea_quantity_hint),
                        presets = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        createdAt = PreviewInstant,
                        updatedAt = PreviewInstant,
                    ),
                ),
                selectedVessel = BrewingVessel(
                    id = "vessel-1",
                    name = "Gaiwan (100ml)",
                    iconName = null,
                    capacityMl = 100,
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                temperatureCelsius = 90.0,
                waterQuantityMl = 200.0,
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
