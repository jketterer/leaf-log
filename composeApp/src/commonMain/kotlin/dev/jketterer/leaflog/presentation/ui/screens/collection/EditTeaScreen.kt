package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Save
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Add/Edit Tea Screen - create or edit a tea in the collection.
 */
@Composable
fun EditTeaScreen(
    teaId: String?,
    onNavigateBack: () -> Unit,
    viewModel: EditTeaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId) {
        viewModel.onIntent(EditTeaIntent.LoadTea(teaId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditTeaNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    EditTeaContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeaContent(
    state: EditTeaState,
    onIntent: (EditTeaIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Tea" else "Add Tea") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(EditTeaIntent.BackClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onIntent(EditTeaIntent.SaveClicked) },
                        enabled = state.hasChanges && state.isValid && !state.isSaving,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Save,
                            contentDescription = "Save",
                        )
                    }
                },
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Name
                item(key = "name") {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onIntent(EditTeaIntent.NameChanged(it)) },
                        label = { Text("Tea Name *") },
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }

                // Tea Type
                item(key = "tea_type") {
                    var showTypeMenu by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = showTypeMenu,
                        onExpandedChange = { showTypeMenu = it }
                    ) {
                        OutlinedTextField(
                            value = state.availableTeaTypes.find { it.id == state.selectedTeaTypeId }?.name
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tea Type *") },
                            isError = state.teaTypeError != null,
                            supportingText = state.teaTypeError?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(showTypeMenu)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                        ) {
                            state.availableTeaTypes.forEach { teaType ->
                                DropdownMenuItem(
                                    text = { Text(teaType.name) },
                                    onClick = {
                                        onIntent(EditTeaIntent.TeaTypeSelected(teaType.id))
                                        showTypeMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                // Origin
                item(key = "origin") {
                    OutlinedTextField(
                        value = state.origin,
                        onValueChange = { onIntent(EditTeaIntent.OriginChanged(it)) },
                        label = { Text("Origin") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Producer (combobox with suggestions)
                item(key = "producer") {
                    var showProducerMenu by remember { mutableStateOf(false) }
                    val filteredProducers = remember(state.producer, state.availableProducers) {
                        if (state.producer.isBlank()) {
                            state.availableProducers
                        } else {
                            state.availableProducers.filter {
                                it.contains(state.producer, ignoreCase = true)
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = showProducerMenu && filteredProducers.isNotEmpty(),
                        onExpandedChange = { showProducerMenu = it },
                    ) {
                        OutlinedTextField(
                            value = state.producer,
                            onValueChange = {
                                onIntent(EditTeaIntent.ProducerChanged(it))
                                showProducerMenu = true
                            },
                            label = { Text("Producer/Brand") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )

                        ExposedDropdownMenu(
                            expanded = showProducerMenu && filteredProducers.isNotEmpty(),
                            onDismissRequest = { showProducerMenu = false },
                        ) {
                            filteredProducers.forEach { producer ->
                                DropdownMenuItem(
                                    text = { Text(producer) },
                                    onClick = {
                                        onIntent(EditTeaIntent.ProducerChanged(producer))
                                        showProducerMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                // Description
                item(key = "description") {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { onIntent(EditTeaIntent.DescriptionChanged(it)) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }

                // Collapsible Brewing Parameters
                item(key = "brewing_params") {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(EditTeaIntent.ToggleBrewingParams) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Default Brewing Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector = if (state.showBrewingParams) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                                contentDescription = if (state.showBrewingParams) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(
                            visible = state.showBrewingParams,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                DurationPicker(
                                    duration = state.defaultBrewingTime,
                                    onDurationChange = { duration ->
                                        onIntent(EditTeaIntent.BrewingTimeChanged(duration))
                                    },
                                    label = "Default Brewing Time",
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                OutlinedTextField(
                                    value = state.defaultTemperatureCelsius,
                                    onValueChange = { onIntent(EditTeaIntent.TemperatureChanged(it)) },
                                    label = { Text("Default Temperature (°C)") },
                                    isError = state.temperatureError != null,
                                    supportingText = state.temperatureError?.let { { Text(it) } }
                                        ?: { Text("Temperature for brewing this tea") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    suffix = { Text("°C") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                )

                                OutlinedTextField(
                                    value = state.defaultQuantity,
                                    onValueChange = { onIntent(EditTeaIntent.QuantityChanged(it)) },
                                    label = { Text("Default Tea Quantity (g)") },
                                    isError = state.quantityError != null,
                                    supportingText = state.quantityError?.let { { Text(it) } }
                                        ?: { Text("Grams of tea per session") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    suffix = { Text("g") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                )
                            }
                        }
                    }
                }

                // Photos
                item(key = "photos") {
                    Column {
                        Text(
                            text = "Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        PhotoGrid(
                            photos = state.photos,
                            onAddPhoto = { imageBytes ->
                                onIntent(EditTeaIntent.PhotoSelected(imageBytes))
                            },
                            onRemovePhoto = { photoPath ->
                                onIntent(EditTeaIntent.PhotoRemoved(photoPath))
                            },
                        )
                    }
                }
            }
        }
    }

    // Discard changes dialog
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(EditTeaIntent.CancelDiscard) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(EditTeaIntent.ConfirmDiscard)
                    },
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(EditTeaIntent.CancelDiscard) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaScreenAddModePreview() {
    LeafLogTheme {
        EditTeaContent(
            state = EditTeaState(
                isEditMode = false,
                name = "",
                availableTeaTypes = listOf(
                    TeaType(
                        id = "green",
                        name = "Green",
                        defaultTemperatureCelsius = 75,
                        defaultBrewingTime = 2.minutes,
                        colorHex = "#4CAF50",
                        isSystemDefault = true,
                        displayOrder = 0,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    TeaType(
                        id = "black",
                        name = "Black",
                        defaultTemperatureCelsius = 95,
                        defaultBrewingTime = 4.minutes,
                        colorHex = "#795548",
                        isSystemDefault = true,
                        displayOrder = 1,
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
private fun EditTeaScreenEditModePreview() {
    LeafLogTheme {
        EditTeaContent(
            state = EditTeaState(
                isEditMode = true,
                name = "Dragon Well",
                selectedTeaTypeId = "green",
                origin = "Hangzhou, China",
                producer = "West Lake Tea Company",
                defaultTemperatureCelsius = "80",
                description = "Premium Dragon Well green tea",
                showBrewingParams = true,
                availableTeaTypes = listOf(
                    TeaType(
                        id = "green",
                        name = "Green",
                        defaultTemperatureCelsius = 75,
                        defaultBrewingTime = 2.minutes,
                        colorHex = "#4CAF50",
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
