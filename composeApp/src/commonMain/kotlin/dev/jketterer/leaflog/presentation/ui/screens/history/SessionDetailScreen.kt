package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.Circle
import compose.icons.fontawesomeicons.solid.Edit
import compose.icons.fontawesomeicons.solid.Trash
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.session.BrewingParameterDisplay
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * Session Detail Screen - displays complete session information.
 */
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToTea: (String) -> Unit,
    viewModel: SessionDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(SessionDetailIntent.LoadSession(sessionId))
    }

    SessionDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToTea = onNavigateToTea
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailContent(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToTea: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SessionDetailIntent.BackClicked) }) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(SessionDetailIntent.EditSessionClicked) }) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = { onIntent(SessionDetailIntent.DeleteSessionClicked) }) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Trash,
                            contentDescription = "Delete"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Brew Again button
            state.parentSession?.let {
                Surface(
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = { onIntent(SessionDetailIntent.BrewAgainClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Brew Again")
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.parentSession != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tea Information
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = state.tea?.name ?: "Unknown Tea",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = state.teaType?.name ?: "Unknown Type",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (state.parentSession.rating != null) {
                                    Row {
                                        repeat(5) { index ->
                                            Text(
                                                text = if (index < state.parentSession.rating.toInt()) "⭐" else "☆",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${state.parentSession.rating})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // All Steeps
                    items(
                        items = state.allSteeps,
                        key = { it.id }
                    ) { steep ->
                        SteepCard(
                            steep = steep,
                            steepNumber = steep.steepNumber,
                            onEditClick = { onIntent(SessionDetailIntent.EditSteepClicked(steep.id)) },
                            onDeleteClick = { onIntent(SessionDetailIntent.DeleteSteep(steep.id)) }
                        )
                    }

                    // Session Details
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Session Details",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                BrewingParameterDisplay(
                                    label = "Vessel",
                                    value = state.vessel?.name ?: "Unknown"
                                )
                                BrewingParameterDisplay(
                                    label = "Water Type",
                                    value = state.parentSession.waterType.displayName
                                )
                                if (state.parentSession.location != null) {
                                    BrewingParameterDisplay(
                                        label = "Location",
                                        value = state.parentSession.location
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                EmptyState(
                    message = "Session not found",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(SessionDetailIntent.CancelDelete) },
            title = { Text("Delete Session?") },
            text = { Text("This will delete the session and all steeps. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(SessionDetailIntent.ConfirmDelete) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SessionDetailIntent.CancelDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SteepCard(
    steep: TeaSession,
    steepNumber: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Steep $steepNumber",
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Edit,
                            contentDescription = "Edit steep"
                        )
                    }
                    if (steepNumber > 1) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = FontAwesomeIcons.Solid.Trash,
                                contentDescription = "Delete steep"
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            BrewingParameterDisplay(
                label = "Duration",
                value = steep.brewingTime.toString()
            )
            BrewingParameterDisplay(
                label = "Temperature",
                value = "${steep.temperatureCelsius}°C"
            )
            if (steep.teaQuantityGrams != null) {
                BrewingParameterDisplay(
                    label = "Tea Amount",
                    value = "${steep.teaQuantityGrams}g"
                )
            }
            BrewingParameterDisplay(
                label = "Water Amount",
                value = "${steep.waterQuantityMl}ml"
            )

            if (steep.notes != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = steep.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (steep.photos.isNotEmpty()) {
                Text(
                    text = "${steep.photos.size} photo(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun SessionDetailScreenPreview() {
    LeafLogTheme {
        SessionDetailContent(
            state = SessionDetailState(
                parentSession = TeaSession(
                    id = "0",
                    teaId = "1",
                    steepNumber = 1,
                    vesselId = "1",
                    waterType = WaterType.FILTERED,
                    timestamp = Instant.fromEpochMilliseconds(1),
                    status = SessionStatus.COMPLETED,
                    brewingTime = 200.toDuration(DurationUnit.SECONDS),
                    updatedAt = Instant.fromEpochMilliseconds(1),
                    deletedAt = Instant.fromEpochMilliseconds(1),
                    createdAt = Instant.fromEpochMilliseconds(1),
                    temperatureCelsius = 95,
                    waterQuantityMl = 400,
                    photos = emptyList(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                vessel = BrewingVessel(
                    id = "",
                    name = "Teapot",
                    isSystemDefault = false,
                    displayOrder = 1,
                    updatedAt = Instant.fromEpochMilliseconds(1),
                    createdAt = Instant.fromEpochMilliseconds(1),
                    iconName = "id"
                ),
                tea = Tea(
                    id = "1",
                    name = "Test Tea",
                    teaTypeId = "",
                    createdAt = Instant.fromEpochMilliseconds(1),
                    updatedAt = Instant.fromEpochMilliseconds(1),
                ),
                teaType = TeaType(
                    id = "",
                    name = "Black Tea",
                    colorHex = "",
                    createdAt = Instant.fromEpochMilliseconds(1),
                    updatedAt = Instant.fromEpochMilliseconds(1),
                    isSystemDefault = false,
                )
            ),
            onIntent = {},
            onNavigateBack = {},
            onNavigateToTea = {},
            onNavigateToEdit = {},
        )
    }
}