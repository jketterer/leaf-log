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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.history.HistoryFilterSheet
import dev.jketterer.leaflog.presentation.ui.components.session.BrewAgainSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.clear_filters
import leaf_log.shared.generated.resources.clear_search
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import leaf_log.shared.generated.resources.history_empty
import leaf_log.shared.generated.resources.history_no_filter_match
import leaf_log.shared.generated.resources.history_no_search_match
import leaf_log.shared.generated.resources.log_a_brew
import leaf_log.shared.generated.resources.navigate_up
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

/**
 * History Screen - displays all brewing sessions.
 */
@Composable
fun HistoryScreen(
    route: NavRoute.HistoryRoute,
    onNavigateToSession: (String) -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToLogTea: (sourceSessionId: String?) -> Unit,
    /** Null at the History tab's root; set when pushed with a filter from elsewhere. */
    onNavigateUp: (() -> Unit)?,
    viewModel: HistoryViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HistoryNavEvent.NavigateToSession -> {
                    onNavigateToSession(event.sessionId)
                }

                is HistoryNavEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is HistoryNavEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(event.sourceSessionId)
                }
            }
        }
    }

    HistoryContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNavigateToLogTea = { onNavigateToLogTea(null) },
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit,
    onNavigateToLogTea: () -> Unit = {},
    onNavigateUp: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Saveable: the query lives in the ViewModel, which outlives this composition on a tab switch
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // A bar restored on return to the screen shouldn't pop the keyboard; one opened here should
    var openedHere by remember { mutableStateOf(false) }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar && openedHere) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
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
                    navigationIcon = {
                        if (onNavigateUp != null) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = FeatherIcons.ArrowLeft,
                                    contentDescription = stringResource(Res.string.navigate_up),
                                )
                            }
                        }
                    },
                    title = { Text("History") },
                    actions = {
                        IconButton(onClick = { openedHere = true; showSearchBar = true }) {
                            Icon(
                                imageVector = FeatherIcons.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { onIntent(HistoryIntent.ShowFilterSheet) }) {
                            BadgedBox(
                                badge = {
                                    if (state.filters.isActive) {
                                        Badge { Text(state.filters.activeCount.toString()) }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.isEmpty && state.filters.isActive -> EmptyState(
                    message = stringResource(Res.string.history_no_filter_match),
                    actionText = stringResource(Res.string.clear_filters),
                    onActionClick = { onIntent(HistoryIntent.ClearFilters) },
                )

                state.isEmpty && state.searchQuery.isNotBlank() -> EmptyState(
                    message = stringResource(
                        Res.string.history_no_search_match,
                        state.searchQuery.trim()
                    ),
                    actionText = stringResource(Res.string.clear_search),
                    onActionClick = { onIntent(HistoryIntent.SearchQueryChanged("")) },
                )

                state.isEmpty -> EmptyState(
                    message = stringResource(Res.string.history_empty),
                    actionText = stringResource(Res.string.log_a_brew),
                    onActionClick = onNavigateToLogTea,
                )

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        state.groups.forEach { group ->
                            group.period?.let { period ->
                                item(key = "header_$period") {
                                    HistoryGroupHeader(
                                        period = period,
                                        sessions = group.sessions,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }

                            // Sessions in this period
                            items(
                                items = group.sessions,
                                key = { it.session.id }
                            ) { session ->
                                val sessionId = session.session.id
                                val tea = state.teas[session.session.teaId]

                                SessionCard(
                                    session = session,
                                    teaName = tea?.name ?: "Unknown Tea",
                                    tags = session.dominantTags(state.tagVocabulary),
                                    onSessionClick = {
                                        if (session.session.status == SessionStatus.IN_PROGRESS) {
                                            onIntent(HistoryIntent.CompleteInProgress(sessionId))
                                        } else {
                                            onIntent(HistoryIntent.SessionClicked(sessionId))
                                        }
                                    },
                                    onBrewAgainClick = {
                                        onIntent(HistoryIntent.BrewAgainClicked(sessionId))
                                    },
                                    onDeleteClick = {
                                        onIntent(HistoryIntent.DeleteSession(sessionId))
                                    },
                                    timeOnly = group.period is HistoryPeriod.Day,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }

        state.brewAgainSheet?.let { sheet ->
            val onBrewAgain =
                { intent: BrewAgainIntent -> onIntent(HistoryIntent.BrewAgain(intent)) }
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
                onResume = { onIntent(HistoryIntent.ResumeInProgress) },
                onCompleteAndContinue = { onIntent(HistoryIntent.CompleteInProgressAndContinue) },
                onDiscardAndContinue = { onIntent(HistoryIntent.DiscardInProgressAndContinue) },
                onDismiss = { onIntent(HistoryIntent.DismissInProgressDialog) },
            )
        }

        // Delete session confirmation dialog
        if (state.sessionPendingDelete != null) {
            ConfirmDestructiveDialog(
                title = stringResource(Res.string.delete_session_title),
                text = stringResource(Res.string.delete_session_text),
                confirmLabel = stringResource(Res.string.delete),
                onConfirm = { onIntent(HistoryIntent.ConfirmDeleteSession) },
                onDismiss = { onIntent(HistoryIntent.CancelDeleteSession) },
            )
        }

        // Filter bottom sheet
        if (state.showFilterSheet) {
            HistoryFilterSheet(
                filters = state.filters,
                teaTypes = state.teaTypes.values.toList(),
                brewedTeas = state.brewedTeas,
                vessels = state.vessels.values.toList(),
                onFiltersChange = { onIntent(HistoryIntent.FiltersChanged(it)) },
                onClear = { onIntent(HistoryIntent.ClearFilters) },
                onDismiss = { onIntent(HistoryIntent.HideFilterSheet) },
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
                    "tea-1" to Tea(
                        id = "tea-1",
                        name = "Test Tea",
                        teaTypeId = "",
                        createdAt = Instant.fromEpochMilliseconds(1),
                        updatedAt = Instant.fromEpochMilliseconds(1),
                    ),
                ),
                groups = listOf(
                    HistoryGroup(
                        period = HistoryPeriod.Day(
                            PreviewInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                        ),
                        sessions = listOf(
                            previewSession(
                                id = "0",
                                ratings = listOf(4f, 4.5f, null),
                                status = SessionStatus.COMPLETED,
                                temperatureCelsius = 95.0,
                                waterQuantityMl = 400.0,
                            ),
                            previewSession(
                                id = "1",
                                status = SessionStatus.COMPLETED,
                                temperatureCelsius = 95.0,
                                waterQuantityMl = 400.0,
                            ),
                        ),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}