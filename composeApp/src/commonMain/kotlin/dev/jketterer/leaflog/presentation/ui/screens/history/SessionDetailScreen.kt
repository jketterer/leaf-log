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
import compose.icons.FeatherIcons
import compose.icons.FontAwesomeIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Trash2
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Star
import compose.icons.fontawesomeicons.solid.Star
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.session.BrewingParameterDisplay
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Session Detail Screen - displays complete session information.
 */
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, Boolean) -> Unit,
    onNavigateToTea: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: SessionDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(SessionDetailIntent.LoadSession(sessionId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is SessionDetailNavEvent.NavigateToEditSession -> {
                    onNavigateToEdit(event.sessionId, event.editFullSession)
                }

                is SessionDetailNavEvent.NavigateToTeaDetails -> {
                    onNavigateToTea(event.teaId)
                }

                is SessionDetailNavEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is SessionDetailNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    SessionDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailContent(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SessionDetailIntent.BackClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(SessionDetailIntent.EditSessionClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.Edit2,
                            contentDescription = "Edit",
                        )
                    }
                    IconButton(onClick = { onIntent(SessionDetailIntent.DeleteSessionClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.Trash2,
                            contentDescription = "Delete",
                        )
                    }
                }
            )
            // Brew Again button
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.parentSession != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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

                                    // Date/Time
                                    val localDateTime = state.parentSession.timestamp
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                    val dateTimeString = buildString {
                                        append(localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() })
                                        append(" ${localDateTime.dayOfMonth}, ${localDateTime.year}")
                                        append(" at ")
                                        val hour = if (localDateTime.hour == 0) 12 else if (localDateTime.hour > 12) localDateTime.hour - 12 else localDateTime.hour
                                        val amPm = if (localDateTime.hour < 12) "AM" else "PM"
                                        val minute = localDateTime.minute.toString().padStart(2, '0')
                                        append("$hour:$minute $amPm")
                                    }
                                    Text(
                                        text = dateTimeString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Show average rating for multi-steep sessions, otherwise show rating
                                    val displayRating = state.parentSession.averageRating
                                        ?: state.parentSession.rating

                                    if (displayRating != null) {
                                        RatingDisplay(displayRating)
                                    }
                                }
                            }
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

                                    if (state.parentSession.teaQuantityGrams != null) {
                                        BrewingParameterDisplay(
                                            label = "Tea Amount",
                                            value = "${state.parentSession.teaQuantityGrams}g"
                                        )
                                    }
                                    BrewingParameterDisplay(
                                        label = "Water Amount",
                                        value = VolumeFormatter.format(
                                            state.parentSession.waterQuantityMl,
                                            state.userPreferences.volumeUnit
                                        )
                                    )
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

                        // All Steeps
                        items(
                            items = state.allSteeps,
                            key = { it.id }
                        ) { steep ->
                            SteepCard(
                                steep = steep,
                                steepNumber = steep.steepNumber,
                                temperatureUnit = state.userPreferences.temperatureUnit,
                                onEditClick = { onIntent(SessionDetailIntent.EditSteepClicked(steep.id)) },
                                onDeleteClick = {
                                    onIntent(
                                        SessionDetailIntent.DeleteSteepClicked(
                                            steep.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                else -> {
                    EmptyState(
                        message = "Session not found",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Button(
                onClick = { onIntent(SessionDetailIntent.BrewAgainClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = FeatherIcons.Coffee,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Brew Again")
            }
        }
    }

    // Delete session confirmation dialog
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

    // Delete steep confirmation dialog
    if (state.showDeleteSteepConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(SessionDetailIntent.CancelDeleteSteep) },
            title = { Text("Delete Steep?") },
            text = { Text("This will delete this steep. Remaining steeps will be renumbered. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(SessionDetailIntent.ConfirmDeleteSteep) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SessionDetailIntent.CancelDeleteSteep) }) {
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
    temperatureUnit: TemperatureUnit,
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
                            imageVector = FeatherIcons.Edit,
                            contentDescription = "Edit steep"
                        )
                    }
                    if (steepNumber > 1) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = FeatherIcons.Trash2,
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
                value = TemperatureFormatter.format(
                    steep.temperatureCelsius,
                    temperatureUnit,
                )
            )

            if (steep.rating != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < steep.rating.toInt()) {
                                    FontAwesomeIcons.Solid.Star
                                } else {
                                    FontAwesomeIcons.Regular.Star
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${steep.rating})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }

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
                userPreferences = UserPreferences(
                    temperatureUnit = TemperatureUnit.FAHRENHEIT,
                    volumeUnit = VolumeUnit.FLUID_OUNCES,
                ),
                parentSession = TeaSession(
                    id = "0",
                    teaId = "1",
                    steepNumber = 1,
                    vesselId = "1",
                    rating = 5f,
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
        )
    }
}