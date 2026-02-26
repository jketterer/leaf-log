package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Edit
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIconHelper
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesselDetailScreen(
    vesselId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditVessel: (String) -> Unit,
    viewModel: VesselDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    // Load vessel on initial composition
    LaunchedEffect(vesselId) {
        viewModel.onIntent(VesselDetailIntent.LoadVessel(vesselId))
    }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is VesselDetailNavigationEvent.NavigateToEditVessel -> {
                onNavigateToEditVessel(event.vesselId)
                viewModel.onNavigationEventHandled()
            }
            is VesselDetailNavigationEvent.NavigateBack,
            is VesselDetailNavigationEvent.NavigateBackAfterDelete -> {
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

    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            vesselName = state.vessel?.name ?: "",
            canDelete = state.canDelete,
            sessionCount = state.sessionCount,
            totalVesselCount = state.totalVesselCount,
            onConfirm = { viewModel.onIntent(VesselDetailIntent.ConfirmDelete) },
            onDismiss = { viewModel.onIntent(VesselDetailIntent.CancelDelete) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.vessel?.name ?: "Vessel Details") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(VesselDetailIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(VesselDetailIntent.EditVesselClicked) }) {
                        Icon(FeatherIcons.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(FeatherIcons.MoreVertical, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                viewModel.onIntent(VesselDetailIntent.DeleteVesselClicked)
                            },
                            leadingIcon = {
                                Icon(FeatherIcons.Trash2, contentDescription = null)
                            },
                            enabled = state.canDelete
                        )
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
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.vessel == null -> {
                    Text(
                        text = "Vessel not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Vessel info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Large image or icon
                                if (state.vessel!!.imagePath != null) {
                                    AsyncImage(
                                        model = state.vessel!!.imagePath,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        painter = VesselIconHelper.getIconForVessel(state.vessel!!.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Vessel name
                                Text(
                                    text = state.vessel!!.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )

                            }
                        }

                        // Vessel details card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Capacity
                                if (state.vessel!!.capacityMl != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Capacity",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = VolumeFormatter.format(
                                                milliliters = state.vessel!!.capacityMl!!.toDouble(),
                                                unit = state.volumeUnit,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Sessions count
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Sessions",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = state.sessionCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Delete constraints info (if applicable)
                        if (!state.canDelete) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Cannot Delete",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )

                                    val reason = when {
                                        state.totalVesselCount <= 1 ->
                                            "This is the last vessel. At least one vessel must remain."
                                        state.sessionCount > 0 ->
                                            "${state.sessionCount} session(s) are using this vessel."
                                        else -> "Unknown reason"
                                    }

                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    vesselName: String,
    canDelete: Boolean,
    sessionCount: Int,
    totalVesselCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Vessel?") },
        text = {
            Column {
                if (canDelete) {
                    Text("Are you sure you want to delete \"$vesselName\"? This action cannot be undone.")
                } else {
                    val reason = when {
                        totalVesselCount <= 1 ->
                            "Cannot delete the last vessel. At least one vessel must remain."
                        sessionCount > 0 ->
                            "Cannot delete vessel. $sessionCount session(s) are using this vessel."
                        else -> "This vessel cannot be deleted."
                    }
                    Text(reason)
                }
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
        }
    )
}
