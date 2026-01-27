package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Eye
import compose.icons.fontawesomeicons.solid.Filter
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * History Screen - displays all brewing sessions.
 */
@Composable
fun HistoryScreen(
    onNavigateToSession: (String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    HistoryContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToSession = onNavigateToSession
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit,
    onNavigateToSession: (String) -> Unit
) {
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { query ->
                        onIntent(HistoryIntent.SearchQueryChanged(query))
                    },
                    onSearch = {},
                    active = true,
                    onActiveChange = { active ->
                        if (!active) {
                            showSearchBar = false
                            onIntent(HistoryIntent.SearchQueryChanged(""))
                        }
                    },
                    placeholder = { Text("Search sessions...") },
                    modifier = Modifier.fillMaxWidth()
                ) {}
            } else {
                TopAppBar(
                    title = { Text("History") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = FontAwesomeIcons.Solid.Eye,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { onIntent(HistoryIntent.ShowFilterSheet) }) {
                            Icon(
                                imageVector = FontAwesomeIcons.Solid.Filter,
                                contentDescription = "Filter"
                            )
                        }
                    }
                )
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

            state.isEmpty -> {
                EmptyState(
                    message = if (state.showDraftsOnly) {
                        "No draft sessions"
                    } else {
                        "No sessions logged yet"
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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

                            SessionCard(
                                session = session,
                                teaName = tea?.name ?: "Unknown Tea",
                                teaTypeName = teaType?.name ?: "Unknown Type",
                                teaPhotoUrl = tea?.photos?.firstOrNull(),
                                onSessionClick = {
                                    onIntent(HistoryIntent.SessionClicked(session.id))
                                    onNavigateToSession(session.id)
                                },
                                onBrewAgainClick = {
                                    onIntent(HistoryIntent.BrewAgain(session.id))
                                },
                                onDeleteClick = {
                                    onIntent(HistoryIntent.DeleteSession(session.id))
                                }
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
                            temperatureCelsius = 95,
                            waterQuantityMl = 400,
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
                            temperatureCelsius = 95,
                            waterQuantityMl = 400,
                            photos = emptyList(),
                            syncStatus = SyncStatus.LOCAL_ONLY,
                        ),
                    )
                )
            ),
            onIntent = {},
            onNavigateToSession = {},
        )
    }
}