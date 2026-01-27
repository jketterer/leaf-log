package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.home.DailyStatsSection
import dev.jketterer.leaflog.presentation.ui.components.home.DraftSessionsBanner
import dev.jketterer.leaflog.presentation.ui.components.home.EmptyHomeState
import dev.jketterer.leaflog.presentation.ui.components.home.GreetingHeader
import dev.jketterer.leaflog.presentation.ui.components.home.RecentSessionCard
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Home Screen - main dashboard with stats and recent sessions.
 *
 * Note: Uses emoji/text icons instead of Material Icons (not available in Compose Multiplatform).
 */
@Composable
fun HomeScreen(
    onNavigateToLogTea: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToLogTea = onNavigateToLogTea,
        onNavigateToSession = onNavigateToSession,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToLogTea: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🍵")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leaf Log")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onIntent(HomeIntent.SettingsClicked)
                            onNavigateToSettings()
                        },
                    ) {
                        Text(
                            text = "⚙️",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onIntent(HomeIntent.LogTeaClicked)
                    onNavigateToLogTea()
                },
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.shouldShowEmptyState -> {
                EmptyHomeState(
                    onGetStartedClick = onNavigateToLogTea,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Greeting
                    item(key = "greeting") {
                        GreetingHeader(
                            greeting = state.greeting,
                            userName = null,  // TODO: Get from user preferences
                        )
                    }

                    // Draft sessions banner
                    if (state.shouldShowDraftBanner) {
                        item(key = "draft_banner") {
                            DraftSessionsBanner(
                                draftCount = state.draftSessionsCount,
                                onBannerClick = {
                                    onIntent(HomeIntent.DraftBannerClicked)
                                    onNavigateToHistory()
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    // Daily stats
                    item(key = "daily_stats") {
                        DailyStatsSection(
                            stats = state.dailyStats,
                        )
                    }

                    // Recent sessions header
                    item(key = "recent_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "RECENT SESSIONS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(
                                onClick = {
                                    onIntent(HomeIntent.ViewAllSessionsClicked)
                                    onNavigateToHistory()
                                },
                            ) {
                                Text("View All")
                            }
                        }
                    }

                    // Recent sessions list
                    items(
                        items = state.recentSessionsWithTea,
                        key = { it.session.id },
                    ) { sessionData ->
                        RecentSessionCard(
                            teaName = sessionData.teaName,
                            teaTypeName = sessionData.teaTypeName,
                            session = sessionData.session,
                            teaPhotoUrl = sessionData.teaPhotoUrl,
                            onSessionClick = {
                                onIntent(HomeIntent.SessionClicked(sessionData.session.id))
                                onNavigateToSession(sessionData.session.id)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // Bottom padding for FAB
                    item(key = "bottom_padding") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { onIntent(HomeIntent.ClearError) }) {
                        Text("Dismiss")
                    }
                },
            ) {
                Text(error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LeafLogTheme {
        HomeContent(
            state = HomeState(
                greeting = "Good morning",
                dailyStats = DailyStats(
                    sessionCount = 3,
                    totalWaterQuantityMl = 450,
                    differentTeasCount = 2,
                ),
                recentSessionsWithTea = listOf(
                    SessionWithTeaData(
                        teaName = "Test Tea",
                        session =
                            TeaSession(
                                id = "1",
                                teaId = "tea-1",
                                vesselId = "gaiwan",
                                waterType = WaterType.FILTERED,
                                timestamp = Clock.System.now(),
                                brewingTime = 2.minutes + 30.seconds,
                                temperatureCelsius = 80,
                                waterQuantityMl = 200,
                                status = SessionStatus.COMPLETED,
                                syncStatus = SyncStatus.LOCAL_ONLY,
                                createdAt = Clock.System.now(),
                                updatedAt = Clock.System.now(),
                            )
                    ),
                    SessionWithTeaData(
                        teaName = "Dragonwell",
                        session =
                    TeaSession(
                        id = "2",
                        teaId = "tea-2",
                        vesselId = "kyusu",
                        waterType = WaterType.FILTERED,
                        timestamp = Clock.System.now(),
                        brewingTime = 2.minutes,
                        temperatureCelsius = 75,
                        waterQuantityMl = 150,
                        status = SessionStatus.COMPLETED,
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )),
                ),
                draftSessionsCount = 2,
                isEmpty = false,
            ),
            onIntent = {},
            onNavigateToLogTea = {},
            onNavigateToSession = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    LeafLogTheme {
        HomeContent(
            state = HomeState(
                greeting = "Good morning",
                isEmpty = true,
            ),
            onIntent = {},
            onNavigateToLogTea = {},
            onNavigateToSession = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    LeafLogTheme {
        HomeContent(
            state = HomeState(
                greeting = "Good morning",
                isLoading = true,
            ),
            onIntent = {},
            onNavigateToLogTea = {},
            onNavigateToSession = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
        )
    }
}