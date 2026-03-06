package dev.jketterer.leaflog.presentation.ui.screens.collection

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.FontAwesomeIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Star
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Heart
import compose.icons.fontawesomeicons.solid.Heart
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.FullscreenImageViewer
import dev.jketterer.leaflog.presentation.ui.components.configuration.SavedMethodsSection
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tea Detail Screen - displays detailed information about a specific tea.
 */
@Composable
fun TeaDetailScreen(
    teaId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToEditSession: (String) -> Unit = {},
    onNavigateToLogTea: (String, String?) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToCreateConfig: (String) -> Unit,
    onNavigateToEditConfig: (configId: String, teaId: String) -> Unit,
    viewModel: TeaDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId) {
        viewModel.onIntent(TeaDetailIntent.LoadTea(teaId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is TeaDetailNavigationEvent.NavigateBack -> {
                    onNavigateBack()
                }

                is TeaDetailNavigationEvent.NavigateToEditTea -> {
                    onNavigateToEdit(teaId)
                }

                is TeaDetailNavigationEvent.NavigateToSession -> {
                    onNavigateToSession(event.sessionId)
                }

                is TeaDetailNavigationEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(teaId, event.vesselId)
                }

                is TeaDetailNavigationEvent.NavigateToEditSession -> {
                    onNavigateToEditSession(event.sessionId)
                }

                is TeaDetailNavigationEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is TeaDetailNavigationEvent.NavigateToHistory -> {
                    onNavigateToHistory(event.teaId)
                }

                is TeaDetailNavigationEvent.NavigateToCreateConfig -> {
                    onNavigateToCreateConfig(event.teaId)
                }

                is TeaDetailNavigationEvent.NavigateToEditConfig -> {
                    onNavigateToEditConfig(event.configId, event.teaId)
                }
            }
        }
    }

    TeaDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaDetailContent(
    state: TeaDetailState,
    onIntent: (TeaDetailIntent) -> Unit,
) {
    val imageStorage: ImageStorage = koinInject()
    var expandedPhotoIndex by remember { mutableIntStateOf(-1) }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            TopAppBar(
                title = { Text(state.tea?.name ?: "Tea Details") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TeaDetailIntent.BackClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Favorite button
                    IconButton(onClick = { onIntent(TeaDetailIntent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.tea?.isFavorite == true) {
                                FontAwesomeIcons.Solid.Heart
                            } else {
                                FontAwesomeIcons.Regular.Heart
                            },
                            contentDescription = if (state.tea?.isFavorite == true) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Edit button
                    IconButton(onClick = { onIntent(TeaDetailIntent.EditTeaClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.Edit2,
                            contentDescription = "Edit tea",
                        )
                    }

                    // More menu
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = FeatherIcons.MoreVertical,
                            contentDescription = "More",
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Tea") },
                            onClick = {
                                showMenu = false
                                onIntent(TeaDetailIntent.DeleteTeaClicked)
                            },
                        )
                    }
                },
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.tea != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Photo gallery
                        if (state.tea.photos.isNotEmpty()) {
                            item(key = "photos") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(state.tea.photos.size) { index ->
                                        AsyncImage(
                                            model = imageStorage.resolveImagePath(state.tea.photos[index]),
                                            contentDescription = "Tea photo",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable { expandedPhotoIndex = index },
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }
                            }
                        }

                        // Tea information
                        item(key = "info") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Type
                                    DetailRow(
                                        label = "Type",
                                        value = state.teaType?.name ?: "Unknown",
                                    )

                                    // Origin
                                    if (state.tea.origin != null) {
                                        DetailRow(
                                            label = "Origin",
                                            value = state.tea.origin,
                                        )
                                    }

                                    // Producer
                                    if (state.tea.producer != null) {
                                        DetailRow(
                                            label = "Producer",
                                            value = state.tea.producer,
                                        )
                                    }

                                    // Brewing parameters
                                    if (state.tea.defaultTemperatureCelsius != null) {
                                        DetailRow(
                                            label = "Temperature",
                                            value = TemperatureFormatter.format(
                                                state.tea.defaultTemperatureCelsius.toDouble(),
                                                state.userPreferences.temperatureUnit
                                            ),
                                        )
                                    }

                                    // Statistics
                                    DetailRow(
                                        label = "Total Sessions",
                                        value = state.tea.totalSessions.toString(),
                                    )

                                    if (state.tea.averageRating != null) {
                                        DetailRow(
                                            label = "Average Rating",
                                            value = state.tea.averageRating.format(1),
                                            trailingIcon = FeatherIcons.Star,
                                        )
                                    }
                                }
                            }
                        }

                        // Description
                        if (state.tea.description != null) {
                            item(key = "description") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        Text(
                                            text = "Description",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.tea.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }

                        // Saved brewing methods
                        item(key = "saved_methods") {
                            SavedMethodsSection(
                                configurations = state.configurations,
                                getVesselName = { vesselId ->
                                    state.vessels.find { it.id == vesselId }?.name ?: "Unknown"
                                },
                                userPreferences = state.userPreferences,
                                onAdd = { onIntent(TeaDetailIntent.AddConfigurationClicked) },
                                onEdit = { configId ->
                                    onIntent(TeaDetailIntent.EditConfigurationClicked(configId))
                                },
                                onDelete = { configId ->
                                    onIntent(TeaDetailIntent.DeleteConfigurationClicked(configId))
                                },
                            )
                        }

                        // Recent sessions
                        if (state.recentSessions.isNotEmpty()) {
                            item(key = "sessions_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "RECENT SESSIONS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    TextButton(onClick = { onIntent(TeaDetailIntent.ViewAllSessionsClicked) }) {
                                        Text("View All")
                                    }
                                }
                            }

                            items(
                                items = state.recentSessions,
                                key = { it.id },
                            ) { session ->
                                val vesselName = state.vessels
                                    .firstOrNull { it.id == session.vesselId }
                                    ?.name ?: "Unknown"
                                SessionCard(
                                    session = session,
                                    teaName = state.tea.name,
                                    teaTypeName = state.teaType?.name ?: "",
                                    vesselName = vesselName,
                                    teaPhotoUrl = state.tea.photos.firstOrNull(),
                                    userPrefs = state.userPreferences,
                                    onSessionClick = {
                                        onIntent(TeaDetailIntent.SessionClicked(session.id))
                                    },
                                    onEditClick = {
                                        onIntent(TeaDetailIntent.EditSessionClicked(session.id))
                                    },
                                    onBrewAgainClick = {
                                        onIntent(TeaDetailIntent.BrewAgainClicked(session.id))
                                    },
                                    onDeleteClick = {
                                        onIntent(TeaDetailIntent.DeleteSessionClicked(session.id))
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tea not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Brew this tea button
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Button(
                onClick = {
                    onIntent(TeaDetailIntent.BrewThisTeaClicked(vesselId = null))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = FeatherIcons.Coffee,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Brew This Tea")
            }
        }

        if (expandedPhotoIndex >= 0 && state.tea != null) {
            FullscreenImageViewer(
                photos = state.tea.photos.map { imageStorage.resolveImagePath(it) },
                initialIndex = expandedPhotoIndex,
                onDismiss = { expandedPhotoIndex = -1 },
            )
        }

        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                action = {
                    TextButton(onClick = { onIntent(TeaDetailIntent.ClearError) }) {
                        Text("Dismiss")
                    }
                },
            ) {
                Text(error)
            }
        }

        // Delete session confirmation dialog
        if (state.sessionPendingDelete != null) {
            AlertDialog(
                onDismissRequest = { onIntent(TeaDetailIntent.CancelDeleteSession) },
                title = { Text("Delete Session?") },
                text = { Text("This will delete the session and all steeps. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { onIntent(TeaDetailIntent.ConfirmDeleteSession) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(TeaDetailIntent.CancelDeleteSession) }) {
                        Text("Cancel")
                    }
                },
            )
        }

        // Delete tea confirmation dialog
        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { onIntent(TeaDetailIntent.CancelDelete) },
                title = { Text("Delete Tea?") },
                text = { Text("This will delete the tea and all its information. Sessions will remain but show 'Unknown Tea'.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onIntent(TeaDetailIntent.ConfirmDelete)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(TeaDetailIntent.CancelDelete) }) {
                        Text("Cancel")
                    }
                },
            )
        }

    }
}

private fun Float.format(numOfDecimals: Int): String {
    val integerPart = this.toInt()

    if (numOfDecimals > 0) {
        val decimalPart = this - integerPart
        val decimalAsIntValue = (decimalPart * 10f.pow(numOfDecimals)).roundToInt()
        val formattedDecimalPart = decimalAsIntValue.toDouble() / 10f.pow(numOfDecimals)
        val formattedValue = integerPart + formattedDecimalPart

        return "$formattedValue"
    } else {
        return integerPart.toString()
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaDetailScreenPreview() {
    LeafLogTheme {
        TeaDetailContent(
            state = TeaDetailState(
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    origin = "Hangzhou, China",
                    producer = "West Lake Tea Company",
                    defaultTemperatureCelsius = 80,
                    description = "Premium Dragon Well green tea with a sweet, nutty flavor and chesnut aroma.",
                    isFavorite = true,
                    totalSessions = 12,
                    averageRating = 4.5f,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                teaType = TeaType(
                    id = "green",
                    name = "Green",
                    defaultTemperatureCelsius = 75,
                    colorHex = "#4CAF50",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                recentSessions = listOf(
                    TeaSession(
                        id = "session-1",
                        teaId = "tea-1",
                        vesselId = "gaiwan",
                        waterType = WaterType.FILTERED,
                        timestamp = Clock.System.now(),
                        brewingTime = 2.minutes + 30.seconds,
                        temperatureCelsius = 80.0,
                        waterQuantityMl = 200.0,
                        rating = 5f,
                        status = SessionStatus.COMPLETED,
                        syncStatus = SyncStatus.LOCAL_ONLY,
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
private fun TeaDetailScreenLoadingPreview() {
    LeafLogTheme {
        TeaDetailContent(
            state = TeaDetailState(isLoading = true),
            onIntent = {},
        )
    }
}
