package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Heart
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Star
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SteepCurve
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.cellarAge
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.domain.models.formatRelativeTimestamp
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.FullscreenImageViewer
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.PastBrewCard
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.BrewAgainSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.HeartFilled
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.best_brew
import leaf_log.shared.generated.resources.cellar_age
import leaf_log.shared.generated.resources.cellar_age_years_months
import leaf_log.shared.generated.resources.day_count
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import leaf_log.shared.generated.resources.delete_tea_text
import leaf_log.shared.generated.resources.delete_tea_title
import leaf_log.shared.generated.resources.header_recent_sessions
import leaf_log.shared.generated.resources.log_a_brew
import leaf_log.shared.generated.resources.month_count
import leaf_log.shared.generated.resources.tea_no_sessions
import leaf_log.shared.generated.resources.tea_not_found
import leaf_log.shared.generated.resources.view_all
import leaf_log.shared.generated.resources.year_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import leaf_log.shared.generated.resources.navigate_up

/**
 * Tea Detail Screen - displays detailed information about a specific tea.
 */
@Composable
fun TeaDetailScreen(
    route: NavRoute.TeaDetailsRoute,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToLogTea: (NavRoute.LogTeaRoute) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: TeaDetailViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is TeaDetailNavigationEvent.NavigateBack -> {
                    onNavigateBack()
                }

                is TeaDetailNavigationEvent.NavigateToEditTea -> {
                    onNavigateToEdit(route.teaId)
                }

                is TeaDetailNavigationEvent.NavigateToSession -> {
                    onNavigateToSession(event.sessionId)
                }

                is TeaDetailNavigationEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(event.route)
                }

                is TeaDetailNavigationEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is TeaDetailNavigationEvent.NavigateToHistory -> {
                    onNavigateToHistory(event.teaId)
                }
            }
        }
    }

    TeaDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        imageStorage = koinInject(),
    )
}

@Composable
private fun TeaDetailContent(
    state: TeaDetailState,
    onIntent: (TeaDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    imageStorage: ImageStorage? = null,
) {
    var expandedPhotoIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val nameHeadingVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = !nameHeadingVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = state.tea?.name ?: "Tea Details",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TeaDetailIntent.BackClicked) }) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = stringResource(Res.string.navigate_up),
                        )
                    }
                },
                actions = {
                    // Favorite button
                    IconButton(onClick = { onIntent(TeaDetailIntent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.tea?.isFavorite == true) {
                                FeatherIcons.HeartFilled
                            } else {
                                FeatherIcons.Heart
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
                            text = {
                                Text(
                                    if (state.tea?.finishedAt == null) "Mark Finished" else "Back in Stock",
                                )
                            },
                            onClick = {
                                showMenu = false
                                onIntent(TeaDetailIntent.ToggleFinished)
                            },
                        )
                        // Only a tea that was never brewed can be deleted
                        if (state.recentSessions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Delete Tea") },
                                onClick = {
                                    showMenu = false
                                    onIntent(TeaDetailIntent.DeleteTeaClicked)
                                },
                            )
                        }
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
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(key = "name") {
                            Text(
                                text = state.tea.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        // Photo gallery
                        if (state.tea.photos.isNotEmpty() && imageStorage != null) {
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
                                    if (state.tea.finishedAt != null) {
                                        DetailRow(
                                            label = "Finished",
                                            value = TimeFormatter.formatRelativeTimestamp(
                                                state.tea.finishedAt,
                                                currentInstant()
                                            ),
                                        )
                                    }

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

                                    val cellarAge = state.tea.cellarAge(currentDate(), TimeZone.currentSystemDefault())
                                    if (cellarAge != null) {
                                        DetailRow(
                                            label = stringResource(Res.string.cellar_age),
                                            value = formatCellarAge(cellarAge),
                                        )
                                    }

                                    // Brewing parameters
                                    if (state.tea.defaultTemperatureCelsius != null) {
                                        DetailRow(
                                            label = "Temperature",
                                            value = formatTemperature(state.tea.defaultTemperatureCelsius),
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
                                            value = formatRating(state.tea.averageRating),
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

                        state.bestBrew?.let { best ->
                            item(key = "best_brew") {
                                val sessionId = best.session.id
                                PastBrewCard(
                                    title = stringResource(Res.string.best_brew),
                                    session = best,
                                    tags = best.dominantTags(state.tagVocabulary),
                                    onClick = { onIntent(TeaDetailIntent.SessionClicked(sessionId)) },
                                    onBrewAgain = { onIntent(TeaDetailIntent.BrewAgainClicked(sessionId)) },
                                )
                            }
                        }

                        state.flavorProfile?.let { profile ->
                            item(key = "flavor_profile") { FlavorProfileSection(profile) }
                        }

                        state.steepCurve?.let { curve ->
                            item(key = "steep_curve") { SteepCurveSection(curve) }
                        }

                        // Recent sessions
                        item(key = "sessions_header") {
                            SectionHeader(stringResource(Res.string.header_recent_sessions)) {
                                if (state.recentSessions.isNotEmpty()) {
                                    TextButton(onClick = { onIntent(TeaDetailIntent.ViewAllSessionsClicked) }) {
                                        Text(stringResource(Res.string.view_all))
                                    }
                                }
                            }
                        }

                        if (state.recentSessions.isEmpty()) {
                            item(key = "no_sessions") {
                                EmptyState(
                                    message = stringResource(Res.string.tea_no_sessions),
                                    actionText = stringResource(Res.string.log_a_brew),
                                    onActionClick = { onIntent(TeaDetailIntent.BrewThisTeaClicked(vesselId = null)) },
                                    // EmptyState fills and scrolls, so it needs a bounded height in a list
                                    modifier = Modifier.heightIn(max = 200.dp),
                                )
                            }
                        } else {
                            items(
                                items = state.recentSessions,
                                key = { it.session.id },
                            ) { session ->
                                val sessionId = session.session.id
                                SessionCard(
                                    session = session,
                                    teaName = state.tea.name,
                                    tags = session.dominantTags(state.tagVocabulary),
                                    onSessionClick = {
                                        onIntent(TeaDetailIntent.SessionClicked(sessionId))
                                    },
                                    onBrewAgainClick = {
                                        onIntent(TeaDetailIntent.BrewAgainClicked(sessionId))
                                    },
                                    onDeleteClick = {
                                        onIntent(TeaDetailIntent.DeleteSessionClicked(sessionId))
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                else -> {
                    EmptyState(message = stringResource(Res.string.tea_not_found))
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

        if (expandedPhotoIndex >= 0 && state.tea != null && imageStorage != null) {
            FullscreenImageViewer(
                photos = state.tea.photos.map { imageStorage.resolveImagePath(it) },
                initialIndex = expandedPhotoIndex,
                onDismiss = { expandedPhotoIndex = -1 },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )

        // Delete session confirmation dialog
        if (state.sessionPendingDelete != null) {
            ConfirmDestructiveDialog(
                title = stringResource(Res.string.delete_session_title),
                text = stringResource(Res.string.delete_session_text),
                confirmLabel = stringResource(Res.string.delete),
                onConfirm = { onIntent(TeaDetailIntent.ConfirmDeleteSession) },
                onDismiss = { onIntent(TeaDetailIntent.CancelDeleteSession) },
            )
        }

        state.brewAgainSheet?.let { sheet ->
            val onBrewAgain = { intent: BrewAgainIntent -> onIntent(TeaDetailIntent.BrewAgain(intent)) }
            BrewAgainSheet(
                teaName = sheet.teaName,
                teaTypeColorHex = sheet.teaTypeColorHex,
                vesselName = sheet.vesselName,
                brew = sheet.brew,
                tagVocabulary = sheet.tagVocabulary,
                onStart = { onBrewAgain(BrewAgainIntent.Start) },
                onAdjust = { onBrewAgain(BrewAgainIntent.Adjust) },
                onDismiss = { onBrewAgain(BrewAgainIntent.Dismiss) },
            )
        }

        // In-progress session conflict dialog
        state.inProgressDialogState?.let { dialogState ->
            SessionInProgressDialog(
                teaName = dialogState.teaName,
                vesselName = dialogState.vesselName,
                session = dialogState.session,
                isProcessing = dialogState.isProcessing,
                onResume = { onIntent(TeaDetailIntent.ResumeInProgress) },
                onCompleteAndContinue = { onIntent(TeaDetailIntent.CompleteInProgressAndContinue) },
                onDiscardAndContinue = { onIntent(TeaDetailIntent.DiscardInProgressAndContinue) },
                onDismiss = { onIntent(TeaDetailIntent.DismissInProgressDialog) },
            )
        }

        // Delete tea confirmation dialog
        if (state.showDeleteConfirmation) {
            ConfirmDestructiveDialog(
                title = stringResource(Res.string.delete_tea_title),
                text = stringResource(Res.string.delete_tea_text),
                confirmLabel = stringResource(Res.string.delete),
                onConfirm = { onIntent(TeaDetailIntent.ConfirmDelete) },
                onDismiss = { onIntent(TeaDetailIntent.CancelDelete) },
            )
        }

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
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
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

/** "2 years, 3 months", "5 months" or "12 days": the largest units, never down to days past a month. */
@Composable
private fun formatCellarAge(age: DatePeriod): String {
    val years = pluralStringResource(Res.plurals.year_count, age.years, age.years)
    val months = pluralStringResource(Res.plurals.month_count, age.months, age.months)
    return when {
        age.years > 0 && age.months > 0 -> stringResource(Res.string.cellar_age_years_months, years, months)
        age.years > 0 -> years
        age.months > 0 -> months
        else -> pluralStringResource(Res.plurals.day_count, age.days, age.days)
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
                    name = "Dragon Well Imperial Grade Long Jing of Yunnan",
                    teaTypeId = "green",
                    origin = "Hangzhou, China",
                    producer = "West Lake Tea Company",
                    purchaseDate = LocalDate(2025, 3, 2),
                    defaultTemperatureCelsius = 80.0,
                    description = "Premium Dragon Well green tea with a sweet, nutty flavor and chesnut aroma.",
                    isFavorite = true,
                    totalSessions = 12,
                    averageRating = 4.5f,
                    finishedAt = PreviewInstant,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                teaType = TeaType(
                    id = "green",
                    name = "Green",
                    defaultTemperatureCelsius = 75.0,
                    colorHex = "#4CAF50",
                    isSystemDefault = true,
                    displayOrder = 0,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                recentSessions = listOf(previewBest),
                sessionsById = mapOf(previewBest.session.id to previewBest),
                bestBrew = previewBest,
                steepCurve = SteepCurve(meanRatings = mapOf(1 to 4f, 2 to 4.5f, 3 to 3.5f), peak = 2),
            ),
            onIntent = {},
        )
    }
}

private val previewBest = previewSession(
    ratings = listOf(5f, 4.5f),
    status = SessionStatus.COMPLETED,
    brewingTime = 2.minutes + 30.seconds,
)

@Preview(showBackground = true)
@Composable
private fun TeaDetailScreenNeverBrewedPreview() {
    LeafLogTheme {
        TeaDetailContent(
            state = TeaDetailState(
                tea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
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
