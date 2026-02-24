package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Coffee
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.PrefillBanner
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.VolumeInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.components.configuration.ChooseMethodDialog
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
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
    var showOptionalFields by remember { mutableStateOf(false) }

    val hasOptionalData = state.teaQuantityGrams.isNotEmpty() ||
            state.selectedWaterType != WaterType.FILTERED ||
            state.location.isNotEmpty()

    LaunchedEffect(hasOptionalData) {
        if (hasOptionalData) {
            showOptionalFields = true
        }
    }

    val optionalSummaryParts = buildList {
        if (state.teaQuantityGrams.isNotEmpty()) add("${state.teaQuantityGrams}g tea")
        if (state.selectedWaterType != WaterType.FILTERED) add(state.selectedWaterType.displayName)
        if (state.location.isNotEmpty()) add(state.location)
    }
    val optionalSummary = optionalSummaryParts.joinToString(" · ")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Tea Session") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LogTeaIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
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
                        val accentColor = state.selectedTeaType?.colorHex?.hexToColor()
                            ?: MaterialTheme.colorScheme.primary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(accentColor),
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.selectedTea.name,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        if (state.selectedTeaType != null) {
                                            Text(
                                                text = state.selectedTeaType.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        val brewStats = buildList {
                                            if (state.selectedTea.totalSessions > 0) {
                                                add("${state.selectedTea.totalSessions} brews")
                                            }
                                            state.selectedTea.averageRating?.let { rating ->
                                                add("★ ${rating.formatOneDecimal()}")
                                            }
                                        }
                                        if (brewStats.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = brewStats.joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    TextButton(onClick = { onIntent(LogTeaIntent.ShowTeaSearchDialog) }) {
                                        Text("Change")
                                    }
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

            // Prompt when tea or vessel not yet selected
            if (state.selectedTea == null || state.selectedVessel == null) {
                item(key = "parameters_hint") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Text(
                            text = "Select a tea and vessel",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "to set up your brew parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Brewing Parameters Section (gated behind tea + vessel selection)
            if (state.selectedTea != null && state.selectedVessel != null) {
            item(key = "parameters_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "BREWING PARAMETERS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "* Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Pre-fill Banner
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
                            duration?.let { onIntent(LogTeaIntent.BrewingTimeChanged(it)) }
                        },
                        label = "Brewing Time *",
                        isError = state.brewingTimeError != null,
                        errorMessage = state.brewingTimeError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = brewingTimePresets,
                        currentValue = state.brewingTime,
                        onSelect = { onIntent(LogTeaIntent.BrewingTimeChanged(it)) },
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
                        onValueChange = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
                        currentUnit = tempUnit,
                        onToggleUnit = {
                            onIntent(LogTeaIntent.ToggleTemperatureUnit)
                        },
                        label = { Text("Temperature *") },
                        isError = state.temperatureError != null,
                        supportingText = state.temperatureError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = tempPresets,
                        currentValue = displayValue,
                        isSelected = { preset, current ->
                            preset.toFloatOrNull() == current?.toFloatOrNull()
                        },
                        onSelect = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
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
                        onValueChange = { onIntent(LogTeaIntent.WaterQuantityChanged(it)) },
                        currentUnit = volUnit,
                        onToggleUnit = {
                            onIntent(LogTeaIntent.ToggleVolumeUnit)
                        },
                        label = { Text("Water Quantity *") },
                        isError = state.waterQuantityError != null,
                        supportingText = state.waterQuantityError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        presets = waterPresets,
                        currentValue = displayValue,
                        isSelected = { preset, current ->
                            preset.toFloatOrNull() == current?.toFloatOrNull()
                        },
                        onSelect = { onIntent(LogTeaIntent.WaterQuantityChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Optional Details Section
            item(key = "optional_section") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOptionalFields = !showOptionalFields }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OPTIONAL DETAILS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            if (!showOptionalFields && optionalSummary.isNotEmpty()) {
                                Text(
                                    text = optionalSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showOptionalFields) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                            contentDescription = if (showOptionalFields) "Collapse optional fields" else "Expand optional fields",
                        )
                    }

                    AnimatedVisibility(
                        visible = showOptionalFields,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Spacer(modifier = Modifier.height(0.dp))

                            // Tea Quantity
                            val teaQtyPresets = remember {
                                listOf("1", "2", "3", "4", "5", "7", "10").map {
                                    Preset("${it}g", it)
                                }
                            }
                            Column {
                                OutlinedTextField(
                                    value = state.teaQuantityGrams,
                                    onValueChange = { onIntent(LogTeaIntent.TeaQuantityChanged(it)) },
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
                                    onSelect = { onIntent(LogTeaIntent.TeaQuantityChanged(it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            // Water Type Selector
                            WaterTypeSelector(
                                selectedWaterType = state.selectedWaterType,
                                onWaterTypeSelected = { waterType ->
                                    onIntent(LogTeaIntent.WaterTypeSelected(waterType))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Location
                            OutlinedTextField(
                                value = state.location,
                                onValueChange = { onIntent(LogTeaIntent.LocationChanged(it)) },
                                label = { Text("Location (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                        }
                    }
                }
            }

            // Action Buttons
            item(key = "actions") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            onIntent(LogTeaIntent.StartTimerClicked)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("Start Session")
                    }

                    OutlinedButton(
                        onClick = { onIntent(LogTeaIntent.SaveAsCompleted) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("Complete Session")
                    }
                }
            }
            } // end parameters gate
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

    // Complete Session Dialog
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
private fun CompleteSessionDialog(
    notes: String,
    rating: Float,
    isSaving: Boolean,
    onNotesChanged: (String) -> Unit,
    onRatingChanged: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How was this brew?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Rating",
                    style = MaterialTheme.typography.labelMedium,
                )
                RatingSelector(
                    rating = rating,
                    onRatingChange = onRatingChanged,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
            ) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun Float.formatOneDecimal(): String {
    val tenths = (this * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Preview(showBackground = true)
@Composable
private fun CompleteSessionDialogPreview() {
    LeafLogTheme {
        CompleteSessionDialog(
            notes = "",
            rating = 3f,
            isSaving = false,
            onNotesChanged = {},
            onRatingChanged = {},
            onConfirm = {},
            onDismiss = {},
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
