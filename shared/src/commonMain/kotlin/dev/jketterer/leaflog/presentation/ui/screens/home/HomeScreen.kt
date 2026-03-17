package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Settings
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.home.DailyStatsSection
import dev.jketterer.leaflog.presentation.ui.components.home.EmptyHomeState
import dev.jketterer.leaflog.presentation.ui.components.home.ExpandableFAB
import dev.jketterer.leaflog.presentation.ui.components.home.GreetingHeader
import dev.jketterer.leaflog.presentation.ui.components.home.InProgressSessionsBanner
import dev.jketterer.leaflog.presentation.ui.components.quicktimer.QuickTimerDurationSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource
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
    onNavigateToLogTea: (String?, String?) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToEditSession: (String) -> Unit,
    onNavigateToHistory: (filterDateStart: String?, filterDateEnd: String?) -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToQuickTimer: (Int) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HomeNavEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(event.teaId, event.vesselId)
                }

                is HomeNavEvent.NavigateToSession -> {
                    onNavigateToSession(event.sessionId)
                }

                is HomeNavEvent.NavigateToHistory -> {
                    onNavigateToHistory(
                        event.filterDateStart,
                        event.filterDateEnd,
                    )
                }

                is HomeNavEvent.NavigateToCollection -> {
                    onNavigateToCollection()
                }

                is HomeNavEvent.NavigateToSettings -> {
                    onNavigateToSettings()
                }

                is HomeNavEvent.NavigateToTimer -> {
                    onNavigateToTimer(event.sessionId)
                }

                is HomeNavEvent.CompleteSession -> {
                    // Navigate to timer screen which will show completion UI
                    onNavigateToTimer(event.sessionId)
                }

                is HomeNavEvent.NavigateToQuickTimer -> {
                    onNavigateToQuickTimer(event.durationSeconds)
                }

                is HomeNavEvent.NavigateToAnalytics -> {
                    onNavigateToAnalytics()
                }
            }
        }
    }

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            vectorResource(Res.drawable.ic_tea_leaf),
                            "Tea leaf",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leaf Log")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onIntent(HomeIntent.SettingsClicked)
                        },
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Settings,
                            contentDescription = "Settings"
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

                state.shouldShowEmptyState -> {
                    EmptyHomeState(
                        onGetStartedClick = { onIntent(HomeIntent.LogTeaClicked) },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Greeting
                        item(key = "greeting") {
                            GreetingHeader(
                                greeting = state.greeting,
                                userName = null,  // TODO: Get from user preferences
                            )
                        }

                        // In-progress sessions banner
                        if (state.shouldShowInProgressBanner) {
                            item(key = "in_progress_banner") {
                                InProgressSessionsBanner(
                                    inProgressInfo = state.mostRecentInProgress,
                                    totalInProgressCount = state.inProgressSessionsCount,
                                    timerProgress = state.liveTimerState?.progress,
                                    onResumeClick = {
                                        onIntent(HomeIntent.ResumeInProgressClicked)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }

                        // Daily stats
                        item(key = "daily_stats") {
                            DailyStatsSection(
                                stats = state.dailyStats,
                                onSessionsCardClick = { onIntent(HomeIntent.DailyStatsTodaySessionsClicked) },
                                onWaterCardClick = { onIntent(HomeIntent.DailyStatsWaterCardClicked) },
                                onTeasCardClick = { onIntent(HomeIntent.DailyStatsTeasCardClicked) },
                                onViewAllClick = { onIntent(HomeIntent.ViewAllStatsClicked) },
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
                            SessionCard(
                                teaName = sessionData.teaName,
                                teaTypeName = sessionData.teaTypeName,
                                vesselName = sessionData.vesselName,
                                session = sessionData.session,
                                teaPhotoUrl = sessionData.teaPhotoUrl,
                                userPrefs = state.userPreferences,
                                onSessionClick = {
                                    onIntent(HomeIntent.SessionClicked(sessionData.session.id))
                                },
                                onBrewAgainClick = {
                                    onIntent(HomeIntent.BrewAgainClicked(sessionData.session.id))
                                },
                                onDeleteClick = {
                                    onIntent(HomeIntent.DeleteSessionClicked(sessionData.session.id))
                                },
                                canBrewAgain = state.inProgressSessionsCount == 0,
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 16.dp),
                            )
                        }

                        // Bottom padding for FAB
                        item(key = "bottom_padding") {
                            Spacer(modifier = Modifier.height(70.dp))
                        }
                    }
                }
            }

        }

        if (state.isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onIntent(HomeIntent.FabExpandedChanged(false)) },
            )
        }

        ExpandableFAB(
            expanded = state.isFabExpanded,
            onExpandedChange = { onIntent(HomeIntent.FabExpandedChanged(it)) },
            onLogSessionClick = { onIntent(HomeIntent.LogTeaClicked) },
            onQuickTimerClick = { onIntent(HomeIntent.QuickTimerClicked) },
            visible = state.inProgressSessionsCount == 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )

        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
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

    // Delete session confirmation dialog
    if (state.sessionPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { onIntent(HomeIntent.CancelDeleteSession) },
            title = { Text("Delete Session?") },
            text = { Text("This will delete the session and all steeps. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(HomeIntent.ConfirmDeleteSession) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(HomeIntent.CancelDeleteSession) }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Quick Timer Duration Sheet
    if (state.showDurationSheet) {
        QuickTimerDurationSheet(
            onDismiss = { onIntent(HomeIntent.DismissDurationSheet) },
            onStartTimer = { durationSeconds ->
                onIntent(HomeIntent.StartQuickTimer(durationSeconds))
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        HomeContent(
            state = HomeState(
                greeting = "Good morning",
                dailyStats = DailyStats(
                    sessionCount = 3,
                    formattedWaterQuantity = "32 fl oz",
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
                                timestamp = now,
                                brewingTime = 2.minutes + 30.seconds,
                                temperatureCelsius = 80.0,
                                waterQuantityMl = 200.0,
                                status = SessionStatus.COMPLETED,
                                syncStatus = SyncStatus.LOCAL_ONLY,
                                createdAt = now,
                                updatedAt = now,
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
                                timestamp = now,
                                brewingTime = 2.minutes,
                                temperatureCelsius = 75.0,
                                waterQuantityMl = 150.0,
                                status = SessionStatus.COMPLETED,
                                syncStatus = SyncStatus.LOCAL_ONLY,
                                createdAt = now,
                                updatedAt = now,
                            )
                    ),
                ),
                inProgressSessionsCount = 1,
                mostRecentInProgress = InProgressSessionInfo(
                    session = TeaSession(
                        id = "in-progress-1",
                        teaId = "tea-3",
                        vesselId = "gaiwan",
                        waterType = WaterType.FILTERED,
                        timestamp = now,
                        brewingTime = 3.minutes,
                        temperatureCelsius = 85.0,
                        waterQuantityMl = 150.0,
                        status = SessionStatus.IN_PROGRESS,
                        syncStatus = SyncStatus.LOCAL_ONLY,
                        createdAt = now,
                        updatedAt = now,
                        steepNumber = 2,
                        timerStatus = TimerStatus.PAUSED,
                        timerRemainingMs = 90_000L,
                    ),
                    teaName = "Dragon Well Green",
                    vesselName = "Gaiwan",
                ),
                isEmpty = false,
            ),
            onIntent = {},
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
        )
    }
}