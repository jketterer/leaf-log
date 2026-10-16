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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Settings
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.home.DailyStatsSection
import dev.jketterer.leaflog.presentation.ui.components.home.ExpandableFAB
import dev.jketterer.leaflog.presentation.ui.components.home.GreetingHeader
import dev.jketterer.leaflog.presentation.ui.components.home.InProgressSessionsBanner
import dev.jketterer.leaflog.presentation.ui.components.home.ManageQuickBrewSheet
import dev.jketterer.leaflog.presentation.ui.components.home.QuickBrewSection
import dev.jketterer.leaflog.presentation.ui.components.quicktimer.QuickTimerDurationSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIcons
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Home Screen - main dashboard with stats and recent sessions.
 */
@Composable
fun HomeScreen(
    finishedSessionId: String?,
    onNavigateToLogTea: (teaId: String?, vesselId: String?, configurationId: String?) -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToHistory: (filterDateStart: String?, filterDateEnd: String?) -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimer: (String) -> Unit,
    onNavigateToSteepComplete: (String) -> Unit,
    onNavigateToQuickTimer: (Int) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToEditBrewingMethod: (String, String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HomeNavEvent.NavigateToLogTea -> {
                    onNavigateToLogTea(event.teaId, event.vesselId, event.configurationId)
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
                    onNavigateToQuickTimer(event.durationSeconds)
                }

                is HomeNavEvent.NavigateToAnalytics -> {
                    onNavigateToAnalytics()
                }

                is HomeNavEvent.NavigateToEditBrewingMethod -> {
                    onNavigateToEditBrewingMethod(event.teaId, event.configurationId)
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState, viewModel::onIntent)

    // Offered once: saved with the entry, so returning to Home doesn't offer it again
    var undoOffered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(finishedSessionId) {
        if (finishedSessionId == null || undoOffered) return@LaunchedEffect
        undoOffered = true
        viewModel.onIntent(HomeIntent.OfferUndoFinish(finishedSessionId))
    }

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        imageStorage = koinInject()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    imageStorage: ImageStorage? = null,
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
                                onWaterCardClick = { onIntent(HomeIntent.DailyStatsWaterCardClicked) },
                                onTeasCardClick = { onIntent(HomeIntent.DailyStatsTeasCardClicked) },
                                onViewAllClick = { onIntent(HomeIntent.ViewAllStatsClicked) },
                            )
                        }

                        // Quick brew configurations
                        if (state.quickBrewConfigurations.isNotEmpty()) {
                            item(key = "quick_brew") {
                                QuickBrewSection(
                                    configurations = state.quickBrewConfigurations,
                                    userPreferences = state.userPreferences,
                                    onConfigClick = { data ->
                                        onIntent(
                                            HomeIntent.QuickBrewClicked(
                                                configurationId = data.configuration.id,
                                                teaId = data.configuration.teaId,
                                                vesselId = data.configuration.vesselId,
                                            )
                                        )
                                    },
                                    onEditClick = { data ->
                                        onIntent(
                                            HomeIntent.EditQuickBrewClicked(
                                                teaId = data.configuration.teaId,
                                                configurationId = data.configuration.id
                                            )
                                        )
                                    },
                                    onManageClick = { onIntent(HomeIntent.ManageQuickBrewClicked) },
                                )
                            }
                        }

                        // Recent sessions header
                        item(key = "recent_header") {
                            SectionHeader(
                                title = stringResource(Res.string.header_recent_sessions),
                                modifier = Modifier.padding(horizontal = 16.dp),
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
                                teaTypeName = sessionData.teaTypeName,
                                vesselName = sessionData.vesselName,
                                session = sessionData.session,
                                teaPhotoUrl = sessionData.teaPhotoUrl,
                                userPrefs = state.userPreferences,
                                onSessionClick = {
                                    onIntent(HomeIntent.SessionClicked(sessionData.session.session.id))
                                },
                                onBrewAgainClick = {
                                    onIntent(HomeIntent.BrewAgainClicked(sessionData.session.session.id))
                                },
                                onDeleteClick = {
                                    onIntent(HomeIntent.DeleteSessionClicked(sessionData.session.session.id))
                                },
                                canBrewAgain = true,
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 16.dp),
                                imageStorage = imageStorage,
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
            userPreferences = state.userPreferences,
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

    // Manage Quick Brew Sheet
    if (state.showManageQuickBrewSheet) {
        ManageQuickBrewSheet(
            configurations = state.allBrewingConfigurations,
            onPinToggled = { id, isPinned ->
                onIntent(HomeIntent.PinConfigurationToggled(id, isPinned))
            },
            onReorder = { orderedIds ->
                onIntent(HomeIntent.PinnedConfigurationsReordered(orderedIds))
            },
            onDismiss = { onIntent(HomeIntent.DismissManageQuickBrewSheet) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val now = PreviewInstant
    LeafLogTheme {
        HomeContent(
            state = HomeState(
                greeting = "Good morning",
                dailyStats = DailyStats(
                    sessionCount = 3,
                    formattedWaterQuantity = "32 fl oz",
                    differentTeasCount = 2,
                ),
                quickBrewConfigurations = listOf(
                    QuickBrewCardData(
                        configuration = BrewingConfiguration(
                            id = "config-1",
                            teaId = "tea-1",
                            vesselId = "gaiwan",
                            teaQuantityGrams = 5f,
                            waterQuantityMl = 100.0,
                            temperatureCelsius = 95.0,
                            brewingTime = 30.seconds,
                            waterType = WaterType.FILTERED,
                            sourceSessionId = null,
                            rating = null,
                            label = "Gaiwan · Gong-fu",
                            timesUsed = 24,
                            lastUsedAt = null,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        teaName = "Tie Guan Yin",
                        teaTypeName = "Oolong",
                        teaTypeColorHex = "#FF9800",
                        vesselName = "Gaiwan",
                    ),
                    QuickBrewCardData(
                        configuration = BrewingConfiguration(
                            id = "config-2",
                            teaId = "tea-2",
                            vesselId = "mug",
                            teaQuantityGrams = 2f,
                            waterQuantityMl = 350.0,
                            temperatureCelsius = 80.0,
                            brewingTime = 3.minutes,
                            waterType = WaterType.FILTERED,
                            sourceSessionId = null,
                            rating = null,
                            label = "Mug · Western",
                            timesUsed = 8,
                            lastUsedAt = null,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        teaName = "Sencha",
                        teaTypeName = "Green",
                        teaTypeColorHex = "#4CAF50",
                        vesselName = "Mug",
                    ),
                    QuickBrewCardData(
                        configuration = BrewingConfiguration(
                            id = "config-3",
                            teaId = "tea-3",
                            vesselId = "kyusu",
                            teaQuantityGrams = null,
                            waterQuantityMl = 250.0,
                            temperatureCelsius = 100.0,
                            brewingTime = 5.minutes,
                            waterType = WaterType.SPRING,
                            sourceSessionId = null,
                            rating = null,
                            label = "Kyusu · Bag Method",
                            timesUsed = 3,
                            lastUsedAt = null,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        teaName = "English Breakfast",
                        teaTypeName = "Black",
                        teaTypeColorHex = "#795548",
                        vesselName = "Kyusu",
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