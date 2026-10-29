package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselChipRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerSheet
import dev.jketterer.leaflog.presentation.ui.components.common.DatePickerField
import dev.jketterer.leaflog.presentation.ui.components.common.DiscardChangesGuardDialog
import dev.jketterer.leaflog.presentation.ui.components.common.LeafQuantityField
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RinseSwitch
import dev.jketterer.leaflog.presentation.ui.components.common.SectionDivider
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.TimePickerField
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.change
import leaf_log.shared.generated.resources.edit_session
import leaf_log.shared.generated.resources.field_date
import leaf_log.shared.generated.resources.field_location_optional
import leaf_log.shared.generated.resources.field_session_note
import leaf_log.shared.generated.resources.field_tea_quantity_required
import leaf_log.shared.generated.resources.field_time
import leaf_log.shared.generated.resources.field_water_quantity_required
import leaf_log.shared.generated.resources.header_notes
import leaf_log.shared.generated.resources.header_session_details
import leaf_log.shared.generated.resources.header_tea
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Edits a session's tea, when it was brewed, how it was set up and its note. Each steep is edited
 * from Session Detail.
 */
@Composable
fun EditSessionScreen(
    route: NavRoute.EditSessionRoute,
    onNavigateBack: () -> Unit,
    viewModel: EditSessionViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditSessionNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

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
    var showTeaPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_session)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = stringResource(Res.string.navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "tea_header") {
                SectionHeader(stringResource(Res.string.header_tea))
            }

            state.tea?.let { tea ->
                item(key = "tea") {
                    val teaType = state.teaTypes.firstOrNull { it.id == tea.teaTypeId }
                    TeaCard(
                        tea = tea,
                        teaTypeName = teaType?.name ?: "",
                        teaTypeColorHex = teaType?.colorHex,
                        onTeaClick = { showTeaPicker = true },
                        trailingContent = {
                            TextButton(onClick = { showTeaPicker = true }) {
                                Text(stringResource(Res.string.change))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.startedAt?.let { startedAt ->
                item(key = "started_at") {
                    val dateTime = startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatePickerField(
                            date = dateTime.date,
                            onDateChange = { date ->
                                date?.let { onIntent(EditSessionIntent.DateChanged(it)) }
                            },
                            label = stringResource(Res.string.field_date),
                            clearable = false,
                            modifier = Modifier.weight(1f),
                        )
                        TimePickerField(
                            hour = dateTime.hour,
                            minute = dateTime.minute,
                            onTimeChange = { hour, minute ->
                                onIntent(EditSessionIntent.TimeChanged(hour, minute))
                            },
                            label = stringResource(Res.string.field_time),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "session_details_header") {
                SectionDivider()
                SectionHeader(stringResource(Res.string.header_session_details))
            }

            item(key = "vessel") {
                VesselChipRow(
                    vessels = state.availableVessels,
                    selectedVessel = state.selectedVessel,
                    onVesselSelected = { vessel ->
                        onIntent(EditSessionIntent.VesselSelected(vessel))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "water_type") {
                WaterTypeSelector(
                    selectedWaterType = state.selectedWaterType,
                    onWaterTypeSelected = { waterType ->
                        onIntent(EditSessionIntent.WaterTypeSelected(waterType))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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

            item(key = "tea_quantity") {
                LeafQuantityField(
                    isTeaBag = state.isTeaBag,
                    teaQuantityGrams = state.teaQuantityGrams,
                    onTeaBagModeChanged = { onIntent(EditSessionIntent.TeaBagModeChanged(it)) },
                    onTeaQuantityChanged = { onIntent(EditSessionIntent.TeaQuantityChanged(it)) },
                    label = stringResource(Res.string.field_tea_quantity_required),
                    errorMessage = state.teaQuantityError,
                    weightUnit = state.userPreferences.weightUnit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "location") {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { onIntent(EditSessionIntent.LocationChanged(it)) },
                    label = { Text(stringResource(Res.string.field_location_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }

            item(key = "rinse") {
                RinseSwitch(
                    checked = state.rinsed,
                    onCheckedChange = { onIntent(EditSessionIntent.RinsedChanged(it)) },
                )
            }

            item(key = "notes_header") {
                SectionDivider()
                SectionHeader(stringResource(Res.string.header_notes))
            }

            item(key = "notes") {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onIntent(EditSessionIntent.NotesChanged(it)) },
                    label = { Text(stringResource(Res.string.field_session_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }

            item(key = "save") {
                Button(
                    onClick = { onIntent(EditSessionIntent.SaveClicked) },
                    enabled = state.hasChanges && state.isValid && !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }

    if (showTeaPicker) {
        // The picker's search and filter start fresh each time it opens
        var query by remember { mutableStateOf("") }
        var filter by remember { mutableStateOf<TeaPickerFilter>(TeaPickerFilter.All) }
        TeaPickerSheet(
            teas = state.availableTeas,
            recentTeas = state.recentTeas,
            teaTypes = state.teaTypes,
            query = query,
            filter = filter,
            selectedTeaId = state.tea?.id,
            onQueryChanged = { query = it },
            onFilterChanged = { filter = it },
            onTeaSelected = { tea ->
                onIntent(EditSessionIntent.TeaSelected(tea))
                showTeaPicker = false
            },
            onTeaCreated = null,
            onDismiss = { showTeaPicker = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSessionScreenPreview() {
    val gaiwan = BrewingVessel(
        id = "gaiwan",
        name = "Gaiwan",
        iconName = "gaiwan",
        isSystemDefault = true,
        displayOrder = 0,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    )
    LeafLogTheme {
        EditSessionContent(
            state = EditSessionState(
                tea = Tea(
                    id = "tea",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                startedAt = PreviewInstant,
                selectedVessel = gaiwan,
                selectedWaterType = WaterType.FILTERED,
                waterQuantityMl = 200.0,
                teaQuantityGrams = 5.0,
                isTeaBag = false,
                location = "Home",
                notes = "Excellent brew",
                availableVessels = listOf(gaiwan),
            ),
            onIntent = {},
        )
    }
}
