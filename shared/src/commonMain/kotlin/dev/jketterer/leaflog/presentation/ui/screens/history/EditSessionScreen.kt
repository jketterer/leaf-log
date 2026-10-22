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
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VesselSelector
import dev.jketterer.leaflog.presentation.ui.components.common.RinseSwitch
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.edit_session
import leaf_log.shared.generated.resources.field_tea_quantity
import leaf_log.shared.generated.resources.field_water_quantity_required
import leaf_log.shared.generated.resources.header_notes
import leaf_log.shared.generated.resources.header_session_details
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Edits how a session was set up and its note; each steep is edited from Session Detail. */
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_session)) },
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
                item(key = "session_details_header") {
                    SectionHeader(stringResource(Res.string.header_session_details))
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

                item(key = "rinse") {
                    RinseSwitch(
                        checked = state.rinsed,
                        onCheckedChange = { onIntent(EditSessionIntent.RinsedChanged(it)) },
                    )
                }

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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSessionScreenPreview() {
    LeafLogTheme {
        EditSessionContent(
            state = EditSessionState(
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
