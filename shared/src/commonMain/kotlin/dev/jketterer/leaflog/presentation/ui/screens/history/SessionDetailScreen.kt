package dev.jketterer.leaflog.presentation.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.FullscreenImageViewer
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.configuration.SaveConfigurationDialog
import dev.jketterer.leaflog.presentation.ui.components.session.BrewingParameterDisplay
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.components.timer.NextSteepParameterDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.date_at_time
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import leaf_log.shared.generated.resources.delete_steep_text
import leaf_log.shared.generated.resources.delete_steep_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

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
    viewModel: SessionDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

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
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SessionDetailContent(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.session != null -> {
                    val session = state.session.session
                    val first = state.session.steeps.first()
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
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (state.tea != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.clickable(enabled = state.tea != null) {
                                            onIntent(SessionDetailIntent.ViewTeaClicked(state.tea!!.id))
                                        },
                                    )
                                    Text(
                                        text = state.teaType?.name ?: "Unknown Type",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Date/Time
                                    val localDateTime = state.session.startedAt
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                    val dateTimeString = stringResource(
                                        Res.string.date_at_time,
                                        TimeFormatter.formatDate(
                                            localDateTime.date,
                                            DateSkeleton.MONTH_DAY_YEAR,
                                        ),
                                        TimeFormatter.formatClockTime(
                                            localDateTime.hour,
                                            localDateTime.minute,
                                        ),
                                    )
                                    Text(
                                        text = dateTimeString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    RatingDisplay(state.session.effectiveRating)
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

                                    if (state.usedConfigurationLabel != null) {
                                        BrewingParameterDisplay(
                                            label = "Method",
                                            value = state.usedConfigurationLabel,
                                        )
                                    }

                                    if (session.teaQuantityGrams != null) {
                                        BrewingParameterDisplay(
                                            label = "Tea Amount",
                                            value = formatWeight(session.teaQuantityGrams)
                                        )
                                    }
                                    BrewingParameterDisplay(
                                        label = "Water Amount",
                                        value = formatVolume(first.waterQuantityMl)
                                    )
                                    BrewingParameterDisplay(
                                        label = "Vessel",
                                        value = state.vessel?.name ?: "Unknown"
                                    )
                                    BrewingParameterDisplay(
                                        label = "Water Type",
                                        value = session.waterType.displayName
                                    )
                                    if (session.location != null) {
                                        BrewingParameterDisplay(
                                            label = "Location",
                                            value = session.location
                                        )
                                    }
                                }
                            }
                        }

                        // All Steeps
                        items(
                            items = state.session.steeps,
                            key = { it.id }
                        ) { steep ->
                            SteepCard(
                                steep = steep,
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

                        // Recording a forgotten steep; an in-progress session adds steeps as it brews
                        if (session.status == SessionStatus.COMPLETED) item {
                            OutlinedButton(
                                onClick = { onIntent(SessionDetailIntent.AddSteepClicked) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.Plus,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Steep")
                            }
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Surface(shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.session != null && state.usedConfigurationLabel == null) {
                    OutlinedButton(
                        onClick = { onIntent(SessionDetailIntent.SaveAsConfigurationClicked) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save as Method")
                    }
                }
                Button(
                    onClick = { onIntent(SessionDetailIntent.BrewAgainClicked) },
                    modifier = Modifier.weight(1f),
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
    }

    // In-progress session conflict dialog
    state.inProgressDialogState?.let { dialogState ->
        SessionInProgressDialog(
            teaName = dialogState.teaName,
            vesselName = dialogState.vesselName,
            session = dialogState.session,
            isProcessing = dialogState.isProcessing,
            onResume = { onIntent(SessionDetailIntent.ResumeInProgress) },
            onCompleteAndContinue = { onIntent(SessionDetailIntent.CompleteInProgressAndContinue) },
            onDiscardAndContinue = { onIntent(SessionDetailIntent.DiscardInProgressAndContinue) },
            onDismiss = { onIntent(SessionDetailIntent.DismissInProgressDialog) },
        )
    }

    // Delete session confirmation dialog
    if (state.showDeleteConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_session_title),
            text = stringResource(Res.string.delete_session_text),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(SessionDetailIntent.ConfirmDelete) },
            onDismiss = { onIntent(SessionDetailIntent.CancelDelete) },
        )
    }

    // Add steep dialog
    val lastSteep = state.session?.currentSteep
    if (state.showAddSteepDialog && lastSteep != null) {
        NextSteepParameterDialog(
            nextSteepNumber = lastSteep.number + 1,
            duration = state.nextSteepDuration,
            temperatureCelsius = state.nextSteepTemperature,
            temperatureUnit = LocalUnits.current.temperature,
            onDurationChange = { onIntent(SessionDetailIntent.UpdateNextSteepDuration(it)) },
            onTemperatureChange = { onIntent(SessionDetailIntent.UpdateNextSteepTemperature(it)) },
            onConfirm = { onIntent(SessionDetailIntent.ConfirmAddSteep) },
            onDismiss = { onIntent(SessionDetailIntent.CancelAddSteep) },
        )
    }

    // Save as configuration dialog
    if (state.showSaveConfigurationDialog && state.session != null) {
        val first = state.session.steeps.first()
        SaveConfigurationDialog(
            teaName = state.tea?.name ?: "",
            vesselName = state.vessel?.name ?: "",
            teaQuantityGrams = state.session.session.teaQuantityGrams,
            waterQuantityMl = first.waterQuantityMl,
            temperatureCelsius = first.temperatureCelsius,
            brewingTimeSeconds = first.brewingTime.inWholeSeconds.toInt(),
            rating = state.session.effectiveRating,
            suggestedLabel = state.suggestedConfigurationLabel,
            onSave = { label -> onIntent(SessionDetailIntent.ConfirmSaveConfiguration(label)) },
            onDismiss = { onIntent(SessionDetailIntent.DismissSaveConfiguration) },
        )
    }

    // Delete steep confirmation dialog
    if (state.showDeleteSteepConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_steep_title),
            text = stringResource(Res.string.delete_steep_text),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(SessionDetailIntent.ConfirmDeleteSteep) },
            onDismiss = { onIntent(SessionDetailIntent.CancelDeleteSteep) },
        )
    }
}

@Composable
private fun SteepCard(
    steep: Steep,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Null in a preview, where no Koin application is running and resolving would throw.
    val imageStorage: ImageStorage? =
        if (LocalInspectionMode.current) null else koinInject()
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
                    text = "Steep ${steep.number}",
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = FeatherIcons.Edit,
                            contentDescription = "Edit steep"
                        )
                    }
                    if (steep.number > 1) {
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
                value = formatTemperature(steep.temperatureCelsius)
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
                    RatingDisplay(steep.rating)
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

            if (steep.photos.isNotEmpty() && imageStorage != null) {
                var expandedPhotoIndex by remember { mutableIntStateOf(-1) }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(steep.photos.size) { index ->
                        AsyncImage(
                            model = imageStorage.resolveImagePath(steep.photos[index]),
                            contentDescription = "Session photo",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { expandedPhotoIndex = index },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                if (expandedPhotoIndex >= 0) {
                    FullscreenImageViewer(
                        photos = steep.photos.map { imageStorage.resolveImagePath(it) },
                        initialIndex = expandedPhotoIndex,
                        onDismiss = { expandedPhotoIndex = -1 },
                    )
                }
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
                session = previewSession(
                    ratings = listOf(5f, 4f),
                    status = SessionStatus.COMPLETED,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 400.0,
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