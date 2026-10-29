package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Settings
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.home.BrewAgainRow
import dev.jketterer.leaflog.presentation.ui.components.home.DailyStatsSection
import dev.jketterer.leaflog.presentation.ui.components.home.ExpandableFAB
import dev.jketterer.leaflog.presentation.ui.components.home.GreetingHeader
import dev.jketterer.leaflog.presentation.ui.components.home.InProgressSessionsBanner
import dev.jketterer.leaflog.presentation.ui.components.quicktimer.QuickTimerDurationSheet
import dev.jketterer.leaflog.presentation.ui.components.session.BrewAgainSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIcons
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session_text
import leaf_log.shared.generated.resources.delete_session_title
import leaf_log.shared.generated.resources.header_recent_sessions
import leaf_log.shared.generated.resources.home_empty_action
import leaf_log.shared.generated.resources.home_empty_body
import leaf_log.shared.generated.resources.home_empty_title
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.view_all
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import leaf_log.shared.generated.resources.settings_title
import org.koin.core.parameter.parametersOf

private val SECTION_SPACING = 16.dp

/**
 * Home Screen - main dashboard with stats and recent sessions.
 */
@Composable
fun HomeScreen(
    route: NavRoute.HomeRoute,
    onNavigateToLogTea: (NavRoute.LogTeaRoute) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToHistory: (filterDateStart: String?, filterDateEnd: String?) -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToSteepComplete: (String) -> Unit,
    onNavigateToQuickTimer: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: HomeViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(HomeIntent.Resumed)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HomeNavEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(event.route)
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
                    onNavigateToSteepComplete(event.sessionId)
                }

                is HomeNavEvent.NavigateToQuickTimer -> {
                    onNavigateToQuickTimer()
                }

                is HomeNavEvent.NavigateToAnalytics -> {
                    onNavigateToAnalytics()
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState, viewModel::onIntent)

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
                            contentDescription = stringResource(Res.string.settings_title)
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
                    EmptyState(
                        message = stringResource(Res.string.home_empty_title),
                        icon = VesselIcons.TeaCup,
                        body = stringResource(Res.string.home_empty_body),
                        actionText = stringResource(Res.string.home_empty_action),
                        onActionClick = { onIntent(HomeIntent.LogTeaClicked) },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
                    ) {
                        // Greeting
                        item(key = "greeting") {
                            // GreetingHeader reads the date itself, so a new day recomposes it
                            key(state.today) {
                                GreetingHeader(
                                    greeting = state.greeting,
                                    userName = null,  // TODO: Get from user preferences
                                )
                            }
                        }

                        // In-progress sessions banner
                        if (state.shouldShowInProgressBanner) {
                            item(key = "in_progress_banner") {
                                InProgressSessionsBanner(
                                    inProgressInfo = state.mostRecentInProgress,
                                    totalInProgressCount = state.inProgressSessionsCount,
                                    resolvedTimerStatus = state.inProgressTimerStatus,
                                    timerProgress = state.inProgressTimerProgress,
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
                                onTeasCardClick = { onIntent(HomeIntent.DailyStatsTeasCardClicked) },
                                onViewAllClick = { onIntent(HomeIntent.ViewAllStatsClicked) },
                            )
                        }

                        if (state.brewAgainCards.isNotEmpty()) {
                            item(key = "brew_again") {
                                BrewAgainRow(
                                    cards = state.brewAgainCards,
                                    onCardClick = { card ->
                                        onIntent(HomeIntent.BrewAgainCardClicked(card.teaId, card.vesselId))
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        // Recent sessions header
                        item(key = "recent_header") {
                            SectionHeader(
                                title = stringResource(Res.string.header_recent_sessions),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    // Takes back the list's spacing, so the cards sit right under it as
                                    // in the sections above
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        val spacing = SECTION_SPACING.roundToPx()
                                        layout(placeable.width, placeable.height - spacing) {
                                            placeable.place(0, 0)
                                        }
                                    },
                            ) {
                                TextButton(
                                    onClick = {
                                        onIntent(HomeIntent.ViewAllSessionsClicked)
                                    },
                                ) {
                                    Text(stringResource(Res.string.view_all))
                                }
                            }
                        }

                        // Recent sessions list
                        items(
                            items = state.recentSessionsWithTea,
                            key = { it.session.session.id },
                        ) { sessionData ->
                            SessionCard(
                                teaName = sessionData.teaName,
                                tags = sessionData.tags,
                                session = sessionData.session,
                                onSessionClick = {
                                    onIntent(HomeIntent.SessionClicked(sessionData.session.session.id))
                                },
                                onDeleteClick = {
                                    onIntent(HomeIntent.DeleteSessionClicked(sessionData.session.session.id))
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 16.dp),
                            )
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
            visible = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    // Delete session confirmation dialog
    if (state.sessionPendingDelete != null) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_session_title),
            text = stringResource(Res.string.delete_session_text),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(HomeIntent.ConfirmDeleteSession) },
            onDismiss = { onIntent(HomeIntent.CancelDeleteSession) },
        )
    }

    // In-progress session conflict dialog
    state.inProgressDialogState?.let { dialogState ->
        SessionInProgressDialog(
            teaName = dialogState.teaName,
            vesselName = dialogState.vesselName,
            session = dialogState.session,
            isProcessing = dialogState.isProcessing,
            onResume = { onIntent(HomeIntent.ResumeInProgressFromDialog) },
            onCompleteAndContinue = { onIntent(HomeIntent.CompleteInProgressAndContinue) },
            onDiscardAndContinue = { onIntent(HomeIntent.DiscardInProgressAndContinue) },
            onDismiss = { onIntent(HomeIntent.DismissInProgressDialog) },
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

    state.brewAgainSheet?.let { sheet ->
        val onBrewAgain = { intent: BrewAgainIntent -> onIntent(HomeIntent.BrewAgain(intent)) }
        BrewAgainSheet(
            teaName = sheet.teaName,
            teaTypeColorHex = sheet.teaTypeColorHex,
            vesselName = sheet.vesselName,
            brew = sheet.brew,
            tagVocabulary = sheet.tagVocabulary,
            onStart = { onBrewAgain(BrewAgainIntent.Start) },
            onAdjust = { onBrewAgain(BrewAgainIntent.Adjust) },
            onDismiss = { onBrewAgain(BrewAgainIntent.Dismiss) },
            last = sheet.last,
            best = sheet.best,
            onSelect = { onBrewAgain(BrewAgainIntent.Select(it)) },
        )
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
                    totalWaterMl = 946.0,
                    differentTeasCount = 2,
                ),
                brewAgainCards = listOf(
                    BrewAgainCardData(
                        teaId = "tea-1",
                        vesselId = "gaiwan",
                        teaName = "Tie Guan Yin",
                        teaTypeColorHex = "#FF9800",
                        vesselName = "Gaiwan",
                        brew = previewSession(
                            id = "brew-1",
                            ratings = listOf(4f, 5f),
                            status = SessionStatus.COMPLETED,
                            brewingTime = 30.seconds,
                            temperatureCelsius = 95.0,
                        ),
                    ),
                    BrewAgainCardData(
                        teaId = "tea-2",
                        vesselId = "mug",
                        teaName = "Sencha",
                        teaTypeColorHex = "#4CAF50",
                        vesselName = "Mug",
                        brew = previewSession(
                            id = "brew-2",
                            status = SessionStatus.COMPLETED,
                            brewingTime = 3.minutes,
                        ),
                    ),
                ),
                recentSessionsWithTea = listOf(
                    SessionWithTeaData(
                        teaName = "Test Tea",
                        session = previewSession(
                            id = "1",
                            status = SessionStatus.COMPLETED,
                            brewingTime = 2.minutes + 30.seconds,
                        ),
                    ),
                    SessionWithTeaData(
                        teaName = "Dragonwell",
                        session = previewSession(
                            id = "2",
                            ratings = listOf(4f, 3.5f),
                            status = SessionStatus.COMPLETED,
                            temperatureCelsius = 75.0,
                            waterQuantityMl = 150.0,
                        ),
                    ),
                ),
                inProgressSessionsCount = 1,
                mostRecentInProgress = InProgressSessionDetails(
                    session = previewSession(id = "in-progress-1", ratings = listOf(4f, null)),
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