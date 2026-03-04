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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

val colorPalette = listOf(
    "#4CAF50", // Green
    "#795548", // Brown
    "#FFFDE7", // Cream
    "#FF9800", // Orange
    "#8D6E63", // Tan
    "#9C27B0", // Purple
    "#F44336", // Red
    "#2196F3", // Blue
    "#00BCD4", // Cyan
    "#FFEB3B", // Yellow
    "#E91E63", // Pink
    "#607D8B", // Blue Grey
)

@Composable
fun EditTeaTypeScreen(
    teaTypeId: String?,
    onNavigateBack: () -> Unit,
    viewModel: EditTeaTypeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    LaunchedEffect(teaTypeId) {
        viewModel.onIntent(EditTeaTypeIntent.LoadTeaType(teaTypeId))
    }

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is EditTeaTypeNavigationEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onNavigationEventHandled()
            }
            null -> {}
        }
    }

    EditTeaTypeContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeaTypeContent(
    state: EditTeaTypeState,
    onIntent: (EditTeaTypeIntent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    if (state.showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = { onIntent(EditTeaTypeIntent.ConfirmDiscard) },
            onDismiss = { onIntent(EditTeaTypeIntent.CancelDiscard) },
        )
    }

    if (state.showDeleteConfirmation) {
        DeleteTeaTypeDialog(
            teaTypeName = state.name,
            canDelete = state.canDelete,
            teaCount = state.teaCount,
            onConfirm = { onIntent(EditTeaTypeIntent.ConfirmDelete) },
            onDismiss = { onIntent(EditTeaTypeIntent.CancelDelete) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Tea Type" else "Add Tea Type") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(EditTeaTypeIntent.BackClicked) }) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
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
                    OutlinedTextField(
                        value = state.temperature,
                        onValueChange = { onIntent(EditTeaTypeIntent.TemperatureChanged(it)) },
                        label = { Text("Default Temperature") },
                        suffix = { Text(state.userPreferences.temperatureUnit.symbol) },
                        isError = state.temperatureError != null,
                        supportingText = state.temperatureError?.let { { Text(it) } }
                            ?: { Text("Optional") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
}

@Composable
private fun ColorSwatch(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val color = hex.hexToColor()
    val isLight = hex == "#FFFDE7" || hex == "#FFEB3B"

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (isLight) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
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
                    .background(
                        if (isLight) Color.Black.copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.8f)
                    ),
            )
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Changes?") },
        text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Discard")
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
private fun DeleteTeaTypeDialog(
    teaTypeName: String,
    canDelete: Boolean,
    teaCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Tea Type") },
        text = {
            if (canDelete) {
                Text("Are you sure you want to delete \"$teaTypeName\"?")
            } else {
                Text(
                    "Cannot delete \"$teaTypeName\" because it is used by $teaCount " +
                            "${if (teaCount == 1) "tea" else "teas"}. " +
                            "Remove or reassign those teas first."
                )
            }
        },
        confirmButton = {
            if (canDelete) {
                TextButton(onClick = onConfirm) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (canDelete) "Cancel" else "OK")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EditTeaTypeAddPreview() {
    LeafLogTheme {
        EditTeaTypeContent(
            state = EditTeaTypeState(
                isEditMode = false,
                name = "Pu-erh",
                colorHex = "#8D6E63",
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
                temperature = "80",
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteDialogCanDeletePreview() {
    LeafLogTheme {
        DeleteTeaTypeDialog(
            teaTypeName = "Herbal",
            canDelete = true,
            teaCount = 0,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteDialogCannotDeletePreview() {
    LeafLogTheme {
        DeleteTeaTypeDialog(
            teaTypeName = "Green",
            canDelete = false,
            teaCount = 5,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
