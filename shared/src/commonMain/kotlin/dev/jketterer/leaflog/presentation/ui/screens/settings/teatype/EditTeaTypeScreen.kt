package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.presentation.ui.components.common.DiscardChangesGuardDialog
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.contentColorOn
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_default_temperature
import leaf_log.shared.generated.resources.field_optional
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Muted tea colours, spaced so no two look alike once toned for either theme
val colorPalette = listOf(
    "#6F8D02", // Matcha
    "#1B9C7F", // Jade
    "#3081A4", // Teal
    "#5F71C6", // Butterfly pea
    "#A171C2", // Lavender
    "#BB5696", // Hibiscus
    "#8B1A2D", // Maroon
    "#B85300", // Rooibos
    "#A57707", // Amber
    "#87926A", // Straw
    "#7B4F3F", // Pu-erh
    "#707084", // Earl Grey
)

@Composable
fun EditTeaTypeScreen(
    route: NavRoute.EditTeaTypeRoute,
    onNavigateBack: () -> Unit,
    viewModel: EditTeaTypeViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is EditTeaTypeNavigationEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onNavigationEventHandled()
            }

            null -> {}
        }
    }

    DiscardChangesGuardDialog(hasChanges = state.hasChanges, onDiscard = onNavigateBack) { onBack ->
        EditTeaTypeContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun EditTeaTypeContent(
    state: EditTeaTypeState,
    onIntent: (EditTeaTypeIntent) -> Unit,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusManager = LocalFocusManager.current

    if (state.showDeleteConfirmation) {
        DeleteTeaTypeDialog(
            teaTypeName = state.name,
            canDelete = state.canDelete,
            teaCount = state.teaCount,
            mergeTargets = state.mergeTargets,
            mergeTargetId = state.mergeTargetId,
            onMergeTargetSelected = { onIntent(EditTeaTypeIntent.MergeTargetSelected(it)) },
            onConfirmDelete = { onIntent(EditTeaTypeIntent.ConfirmDelete) },
            onConfirmMerge = { onIntent(EditTeaTypeIntent.ConfirmMerge) },
            onDismiss = { onIntent(EditTeaTypeIntent.CancelDelete) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Tea Type" else "Add Tea Type") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { onIntent(EditTeaTypeIntent.DeleteClicked) }) {
                            Icon(FeatherIcons.Trash2, contentDescription = "Delete")
                        }
                    }
                    IconButton(
                        onClick = { onIntent(EditTeaTypeIntent.SaveClicked) },
                        enabled = state.isValid && !state.isSaving,
                    ) {
                        Icon(FeatherIcons.Save, contentDescription = "Save")
                    }
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Name
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { onIntent(EditTeaTypeIntent.NameChanged(it)) },
                            label = { Text("Name") },
                            isError = state.nameError != null,
                            supportingText = state.nameError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            enabled = !state.isSaving,
                        )

                        // Color
                        Text(
                            text = "Color",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            contentPadding = PaddingValues(0.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            userScrollEnabled = false,
                        ) {
                            items(
                                items = colorPalette,
                                key = { it },
                            ) { hex ->
                                ColorSwatch(
                                    hex = hex,
                                    isSelected = state.colorHex == hex,
                                    onClick = { onIntent(EditTeaTypeIntent.ColorSelected(hex)) },
                                    enabled = !state.isSaving,
                                )
                            }
                        }

                        // Temperature
                        UnitInputField(
                            value = state.temperatureCelsius,
                            onValueChange = { onIntent(EditTeaTypeIntent.TemperatureChanged(it)) },
                            unit = state.userPreferences.temperatureUnit,
                            label = stringResource(Res.string.field_default_temperature),
                            isError = state.temperatureError != null,
                            supportingText = stringResource(state.temperatureError ?: Res.string.field_optional),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving,
                        )

                    }
                }

                if (state.isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ColorSwatch(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val color = teaTypeColor(hex)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(contentColorOn(color).copy(alpha = 0.7f)),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaTypeAddPreview() {
    LeafLogTheme {
        EditTeaTypeContent(
            state = EditTeaTypeState(
                isEditMode = false,
                name = "Pu-erh",
                colorHex = "#8B1A2D",
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaTypeEditPreview() {
    LeafLogTheme {
        EditTeaTypeContent(
            state = EditTeaTypeState(
                isEditMode = true,
                name = "Green",
                colorHex = "#4CAF50",
                temperatureCelsius = 80.0,
            ),
            onIntent = {},
        )
    }
}
