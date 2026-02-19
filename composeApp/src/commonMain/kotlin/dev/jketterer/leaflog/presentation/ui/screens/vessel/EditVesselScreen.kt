package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import compose.icons.feathericons.X
import dev.jketterer.leaflog.presentation.ui.components.common.EditCapacityField
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoPickerButton
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIconHelper
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVesselScreen(
    vesselId: String?,
    onNavigateBack: () -> Unit,
    viewModel: EditVesselViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load vessel on initial composition
    LaunchedEffect(vesselId) {
        viewModel.onIntent(EditVesselIntent.LoadVessel(vesselId))
    }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is EditVesselNavigationEvent.NavigateBack,
            is EditVesselNavigationEvent.NavigateBackAfterSave -> {
                onNavigateBack()
                viewModel.onNavigationEventHandled()
            }

            null -> {}
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Discard changes dialog
    if (state.showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = { viewModel.onIntent(EditVesselIntent.ConfirmDiscard) },
            onDismiss = { viewModel.onIntent(EditVesselIntent.CancelDiscard) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Vessel" else "Add Vessel") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(EditVesselIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(EditVesselIntent.SaveClicked) },
                        enabled = state.isValid && !state.isSaving
                    ) {
                        Icon(FeatherIcons.Save, contentDescription = "Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name field
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onIntent(EditVesselIntent.NameChanged(it)) },
                        label = { Text("Name") },
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isSaving
                    )

                    // Capacity field
                    // Convert from storage (mL) to display unit for showing
                    val displayCapacity = state.capacity.toIntOrNull()?.let { ml ->
                        state.userPreferences.volumeUnit.fromMilliliters(ml.toDouble()).toString()
                    } ?: state.capacity

                    EditCapacityField(
                        value = displayCapacity,
                        onValueChange = { viewModel.onIntent(EditVesselIntent.CapacityChanged(it)) },
                        isError = state.capacityError != null,
                        supportingText = state.capacityError
                            ?: "Optional - auto-fills water quantity when selected",
                        enabled = !state.isSaving,
                        volumeUnit = state.userPreferences.volumeUnit,
                        onToggleUnit = {
                            viewModel.onIntent(EditVesselIntent.ToggleVolumeUnit)
                        },
                    )

                    // Photo section
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    VesselPhotoSection(
                        imagePath = state.imagePath,
                        onPhotoPicked = { bytes ->
                            viewModel.onIntent(EditVesselIntent.PhotoSelected(bytes))
                        },
                        onRemovePhoto = {
                            viewModel.onIntent(EditVesselIntent.RemovePhoto)
                        },
                        enabled = !state.isSaving,
                    )

                    // Icon selector section
                    Text(
                        text = "Select Icon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        userScrollEnabled = false
                    ) {
                        items(
                            items = VesselIconHelper.getAllIcons(),
                            key = { it.iconName }
                        ) { iconOption ->
                            VesselIconCard(
                                iconOption = iconOption,
                                isSelected = state.selectedIconName == iconOption.iconName,
                                onClick = {
                                    viewModel.onIntent(EditVesselIntent.IconSelected(iconOption.iconName))
                                },
                                enabled = !state.isSaving
                            )
                        }
                    }
                }
            }

            // Saving overlay
            if (state.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun VesselIconCard(
    iconOption: dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = iconOption.imageVector,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = iconOption.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VesselPhotoSection(
    imagePath: String?,
    onPhotoPicked: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
    enabled: Boolean,
) {
    val scope = rememberCoroutineScope()

    if (imagePath != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Vessel photo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                PhotoPickerButton(
                    onPhotoPicked = { file ->
                        scope.launch { onPhotoPicked(file.readBytes()) }
                    },
                    text = "Change Photo",
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onRemovePhoto,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = FeatherIcons.X,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    } else {
        PhotoPickerButton(
            onPhotoPicked = { file ->
                scope.launch { onPhotoPicked(file.readBytes()) }
            },
            text = "Add Photo",
        )
    }
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
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
        }
    )
}
