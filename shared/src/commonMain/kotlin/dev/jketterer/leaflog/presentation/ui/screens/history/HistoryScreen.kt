package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Filter
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import kotlinx.datetime.LocalDate
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.history.HistoryFilterSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * History Screen - displays all brewing sessions.
 */
@Composable
fun HistoryScreen(
    filterTeaTypeId: String? = null,
    filterTeaId: String? = null,
    filterDateStart: String? = null,
    filterDateEnd: String? = null,
    onNavigateToSession: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(filterTeaTypeId) {
        if (filterTeaTypeId != null) {
            viewModel.onIntent(HistoryIntent.FilterByTeaType(filterTeaTypeId))
        }
    }

    LaunchedEffect(filterTeaId) {
        if (filterTeaId != null) {
            viewModel.onIntent(HistoryIntent.FilterByTea(filterTeaId))
        }
    }

    LaunchedEffect(filterDateStart, filterDateEnd) {
        if (filterDateStart != null || filterDateEnd != null) {
            val start = filterDateStart?.let { LocalDate.parse(it) }
            val end = filterDateEnd?.let { LocalDate.parse(it) }
            viewModel.onIntent(HistoryIntent.FilterByDateRange(start, end))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HistoryNavEvent.NavigateToSession -> {
                    onNavigateToSession(event.sessionId)
                }
                is HistoryNavEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }
            }
        }
    }

    HistoryContent(
        state = state,
        onIntent = viewModel::onIntent,
        imageStorage = koinInject(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit,
    imageStorage: ImageStorage? = null,
) {
    var showSearchBar by remember { mutableStateOf(false) }
    val filterSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            searchFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSearchBar) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                showSearchBar = false
                                onIntent(HistoryIntent.SearchQueryChanged(""))
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = FeatherIcons.ArrowLeft,
                                contentDescription = "Close search"
                            )
                        }
                    },
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { query ->
                                onIntent(HistoryIntent.SearchQueryChanged(query))
                            },
                            placeholder = { Text("Search sessions...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                        )
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onIntent(HistoryIntent.SearchQueryChanged("")) }
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.X,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("History") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = FeatherIcons.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { onIntent(HistoryIntent.ShowFilterSheet) }) {
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) {
                                        Badge { Text(state.activeFilterCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.Filter,
                                    contentDescription = "Filter",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.isEmpty -> {
                    val (message, actionText, onAction) = when {
                        state.hasActiveFilters -> Triple(
                            "No sessions match filters",
                            "Clear Filters",
                        ) { onIntent(HistoryIntent.ClearFilters) }

                        else -> Triple(
                            "No sessions logged yet",
                            null,
                            null,
                        )
                    }
                    EmptyState(
                        message = message,
                        actionText = actionText,
                        onActionClick = onAction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        state.groupedSessions.forEach { (period, sessions) ->
                            // Period header
                            item(key = "header_$period") {
                                Text(
                                    text = period,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Sessions in this period
                            items(
                                items = sessions,
                                key = { it.id }
                            ) { session ->
                                val tea = state.teas[session.teaId]
                                val teaType = tea?.let { state.teaTypes[it.teaTypeId] }
                                val vessel = state.vessels[session.vesselId]

                                SessionCard(
                                    session = session,
                                    teaName = tea?.name ?: "Unknown Tea",
                                    teaTypeName = teaType?.name ?: "Unknown Type",
                                    vesselName = vessel?.name ?: "Unknown Vessel",
                                    teaPhotoUrl = tea?.photos?.firstOrNull(),
                                    userPrefs = state.userPreferences,
                                    onSessionClick = {
                                        if (session.status == SessionStatus.IN_PROGRESS) {
                                            onIntent(HistoryIntent.CompleteInProgress(session.id))
                                        } else {
                                            onIntent(HistoryIntent.SessionClicked(session.id))
                                        }
                                    },
                                    onBrewAgainClick = {
                                        onIntent(HistoryIntent.BrewAgain(session.id))
                                    },
                                    onDeleteClick = {
                                        onIntent(HistoryIntent.DeleteSession(session.id))
                                    },
                                    imageStorage = imageStorage,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }

            // Error snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { onIntent(HistoryIntent.ClearError) }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        // In-progress session conflict dialog
        state.inProgressDialogState?.let { dialogState ->
            SessionInProgressDialog(
                teaName = dialogState.teaName,
                vesselName = dialogState.vesselName,
                session = dialogState.session,
                userPreferences = state.userPreferences,
                isProcessing = dialogState.isProcessing,
                onResume = { onIntent(HistoryIntent.ResumeInProgress) },
                onCompleteAndContinue = { onIntent(HistoryIntent.CompleteInProgressAndContinue) },
                onDiscardAndContinue = { onIntent(HistoryIntent.DiscardInProgressAndContinue) },
                onDismiss = { onIntent(HistoryIntent.DismissInProgressDialog) },
            )
        }

        // Delete session confirmation dialog
        if (state.sessionPendingDelete != null) {
            AlertDialog(
                onDismissRequest = { onIntent(HistoryIntent.CancelDeleteSession) },
                title = { Text("Delete Session?") },
                text = { Text("This will delete the session and all steeps. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { onIntent(HistoryIntent.ConfirmDeleteSession) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(HistoryIntent.CancelDeleteSession) }) {
                        Text("Cancel")
                    }
                },
            )
        }

        // Filter bottom sheet
        if (state.showFilterSheet) {
            HistoryFilterSheet(
                state = state,
                onIntent = onIntent,
                sheetState = filterSheetState,
            )
        }
    }
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    LeafLogTheme {
        HistoryContent(
            state = HistoryState(
                teas = mapOf(
                    "1" to Tea(
                        id = "1",
                        name = "Test Tea",
                        teaTypeId = "",
                        createdAt = Instant.fromEpochMilliseconds(1),
                        updatedAt = Instant.fromEpochMilliseconds(1),
                    ),
                ),
                groupedSessions = mapOf(
                    "Today" to listOf(
                        TeaSession(
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
                            temperatureCelsius = 95.0,
                            waterQuantityMl = 400.0,
                            photos = emptyList(),
                            syncStatus = SyncStatus.LOCAL_ONLY,
                        ),
                        TeaSession(
                            id = "1",
                            teaId = "1",
                            steepNumber = 1,
                            vesselId = "1",
                            waterType = WaterType.FILTERED,
                            timestamp = Instant.fromEpochMilliseconds(1000000),
                            status = SessionStatus.COMPLETED,
                            brewingTime = 200.toDuration(DurationUnit.SECONDS),
                            updatedAt = Instant.fromEpochMilliseconds(1),
                            deletedAt = Instant.fromEpochMilliseconds(1),
                            createdAt = Instant.fromEpochMilliseconds(1),
                            temperatureCelsius = 95.0,
                            waterQuantityMl = 400.0,
                            photos = emptyList(),
                            syncStatus = SyncStatus.LOCAL_ONLY,
                        ),
                    )
                )
            ),
            onIntent = {},
        )
    }
}