package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import dev.jketterer.leaflog.domain.models.TeaType
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
                    )
                }

                // Tea Type
                item(key = "tea_type") {
                    var showTypeMenu by remember { mutableStateOf(false) }

                    Box {
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
                                .clickableWithoutRipple { showTypeMenu = true },
                            trailingIcon = {
                                Text(
                                    text = "▼",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                        )

                        DropdownMenu(
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

                // Producer
                item(key = "producer") {
                    OutlinedTextField(
                        value = state.producer,
                        onValueChange = { onIntent(EditTeaIntent.ProducerChanged(it)) },
                        label = { Text("Producer/Brand") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Stock Amount
                item(key = "stock") {
                    OutlinedTextField(
                        value = state.stockAmount,
                        onValueChange = { onIntent(EditTeaIntent.StockAmountChanged(it)) },
                        label = { Text("Stock Amount (g)") },
                        isError = state.stockAmountError != null,
                        supportingText = state.stockAmountError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Default Temperature
                item(key = "temperature") {
                    OutlinedTextField(
                        value = state.defaultTemperatureCelsius,
                        onValueChange = { onIntent(EditTeaIntent.TemperatureChanged(it)) },
                        label = { Text("Default Temperature (°C)") },
                        isError = state.temperatureError != null,
                        supportingText = state.temperatureError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Purchase Price
                item(key = "price") {
                    OutlinedTextField(
                        value = state.purchasePrice,
                        onValueChange = { onIntent(EditTeaIntent.PurchasePriceChanged(it)) },
                        label = { Text("Purchase Price") },
                        isError = state.purchasePriceError != null,
                        supportingText = state.purchasePriceError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("$") },
                    )
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
                    )
                }

                // Photos
                item(key = "photos_header") {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (state.photos.isNotEmpty()) {
                    item(key = "photos") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = state.photos,
                                key = { it },
                            ) { photoUri ->
                                Box {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Tea photo",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop,
                                    )

                                    // Remove button
                                    IconButton(
                                        onClick = { onIntent(EditTeaIntent.PhotoRemoved(photoUri)) },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        Text(
                                            text = "×",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "add_photo") {
                    OutlinedButton(
                        onClick = { onIntent(EditTeaIntent.AddPhotoClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add Photo")
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

/**
 * Modifier for clickable without ripple effect.
 */
@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.then(
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        ),
    )
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
                stockAmount = "50",
                defaultTemperatureCelsius = "80",
                description = "Premium Dragon Well green tea",
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
