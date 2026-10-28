package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import compose.icons.FeatherIcons
import compose.icons.feathericons.BarChart2
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Home
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.presentation.ui.screens.analytics.AnalyticsScreen
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.tab_analytics
import leaf_log.shared.generated.resources.tab_collection
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.tab_home
import dev.jketterer.leaflog.presentation.ui.screens.collection.EditTeaScreen
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionScreen
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailScreen
import dev.jketterer.leaflog.presentation.ui.screens.configuration.CreateBrewingConfigurationScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.EditSessionScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.SessionDetailScreen
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeScreen
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaScreen
import dev.jketterer.leaflog.presentation.ui.screens.recap.RecapScreen
import dev.jketterer.leaflog.presentation.ui.screens.settings.SettingsScreen
import dev.jketterer.leaflog.presentation.ui.screens.settings.teatype.EditTeaTypeScreen
import dev.jketterer.leaflog.presentation.ui.screens.settings.teatype.TeaTypeListScreen
import dev.jketterer.leaflog.presentation.ui.screens.sessionreview.SessionReviewScreen
import dev.jketterer.leaflog.presentation.ui.screens.steepcomplete.SteepCompleteScreen
import dev.jketterer.leaflog.presentation.ui.screens.timer.TimerScreen
import dev.jketterer.leaflog.presentation.ui.screens.vessel.EditVesselScreen
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselDetailScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock

@Composable
fun AppNavigation() {
    val navigator = rememberNavigator()

    // Handle deep links from notification taps. A tap on one left over from a session since
    // finished or discarded just opens the app, and a Timer tap for a steep that has run out (the
    // Live Activity sits at 0:00) opens Steep Complete
    val sessions: TeaSessionRepository = koinInject()
    val clock: Clock = koinInject()
    val pendingRoute by DeepLinkHandler.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        val sessionId = route.brewSessionId
        if (sessionId == null) {
            navigator.openOnHome(route)
        } else {
            val session = sessions.getSessionOfSteep(sessionId)
            if (session?.session?.status == SessionStatus.IN_PROGRESS) {
                val finished = route is NavRoute.TimerRoute &&
                    session.currentSteep.resolvedTimerStatus(clock.now()) == TimerStatus.COMPLETE
                navigator.openOnHome(
                    if (finished) NavRoute.SteepCompleteRoute(session.session.id) else route,
                )
            }
        }
        DeepLinkHandler.consume()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        bottomBar = {
            if (!navigator.current.hidesBottomBar()) {
                BottomNavigationBar(
                    currentTab = navigator.tab,
                    onSelectTab = navigator::selectTab,
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            entries = navigator.entries(appEntryProvider(navigator)),
            onBack = navigator::goBack,
            modifier = Modifier
                .padding(bottom = paddingValues.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = paddingValues.calculateBottomPadding()))
                .imePadding()
                .fillMaxSize(),
            transitionSpec = {
                if (isTabSwitch()) fadeThrough()
                else slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
            },
            popTransitionSpec = {
                if (isTabSwitch()) fadeThrough()
                else slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
        )
    }
}

@Composable
private fun appEntryProvider(navigator: Navigator) = remember(navigator) {
    entryProvider<NavKey> {
        entry<NavRoute.HomeRoute> { route ->
            HomeScreen(
                route = route,
                onNavigateToLogTea = { teaId, vesselId ->
                    navigator.navigate(NavRoute.LogTeaRoute(teaId, vesselId))
                },
                onNavigateToSession = { sessionId ->
                    navigator.navigate(NavRoute.SessionDetailsRoute(sessionId))
                },
                onNavigateToHistory = { filterDateStart, filterDateEnd ->
                    navigator.navigate(
                        NavRoute.HistoryRoute(
                            filterDateStart = filterDateStart,
                            filterDateEnd = filterDateEnd,
                        )
                    )
                },
                onNavigateToCollection = { navigator.navigate(NavRoute.CollectionRoute) },
                onNavigateToSettings = { navigator.navigate(NavRoute.SettingsRoute) },
                onNavigateToTimer = { sessionId ->
                    navigator.openOnHome(NavRoute.TimerRoute(sessionId))
                },
                onNavigateToSteepComplete = { sessionId ->
                    // An equal HomeRoute would reuse this entry's state, so finishing
                    // again after Undo would never offer Undo
                    navigator.replace(route, NavRoute.HomeRoute())
                    navigator.openOnHome(NavRoute.SteepCompleteRoute(sessionId))
                },
                onNavigateToQuickTimer = { navigator.openOnHome(NavRoute.TimerRoute()) },
                onNavigateToAnalytics = { navigator.navigate(NavRoute.AnalyticsRoute) },
                onNavigateToEditBrewingMethod = { teaId, configId ->
                    navigator.navigate(NavRoute.CreateBrewingConfigurationRoute(teaId, configId))
                }
            )
        }

        entry<NavRoute.CollectionRoute> { route ->
            TeaCollectionScreen(
                onNavigateToTeaDetail = { teaId ->
                    navigator.navigate(NavRoute.TeaDetailsRoute(teaId))
                },
                onNavigateToAddTea = {
                    navigator.navigate(NavRoute.EditTeaRoute())
                },
                onNavigateToVesselDetail = { vesselId ->
                    navigator.navigate(NavRoute.VesselDetailRoute(vesselId))
                },
                onNavigateToAddVessel = {
                    navigator.navigate(NavRoute.EditVesselRoute())
                },
            )
        }

        entry<NavRoute.HistoryRoute> { route ->
            HistoryScreen(
                route = route,
                onNavigateToSession = { sessionId ->
                    navigator.navigate(NavRoute.SessionDetailsRoute(sessionId))
                },
                onNavigateToTimer = { sessionId ->
                    navigator.openOnHome(NavRoute.TimerRoute(sessionId))
                },
                onNavigateToLogTea = {
                    navigator.navigate(NavRoute.LogTeaRoute())
                },
                onNavigateUp = if (route == Tab.HISTORY.root) null else ({ navigator.navigateUp(route) }),
            )
        }

        entry<NavRoute.AnalyticsRoute> { route ->
            AnalyticsScreen(
                onNavigateToLogTea = {
                    navigator.navigate(NavRoute.LogTeaRoute())
                },
                onNavigateToHistory = { navigator.navigate(it) },
                onNavigateToVessel = { vesselId ->
                    navigator.navigate(NavRoute.VesselDetailRoute(vesselId))
                },
                onNavigateToRecap = { navigator.navigate(it) },
            )
        }

        entry<NavRoute.RecapRoute> { route ->
            RecapScreen(
                route = route,
                onClose = { navigator.navigateUp(route) },
                onNavigateToHistory = { navigator.navigate(it) },
                onNavigateToTea = { teaId -> navigator.navigate(NavRoute.TeaDetailsRoute(teaId)) },
                onNavigateToSession = { sessionId -> navigator.navigate(NavRoute.SessionDetailsRoute(sessionId)) },
            )
        }

        entry<NavRoute.SettingsRoute> { route ->
            SettingsScreen(
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToTeaTypes = {
                    navigator.navigate(NavRoute.TeaTypeListRoute)
                },
            )
        }

        // =========================================================
        // Tea Type Management Destinations
        // =========================================================

        entry<NavRoute.TeaTypeListRoute> { route ->
            TeaTypeListScreen(
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToEditTeaType = { teaTypeId ->
                    navigator.navigate(NavRoute.EditTeaTypeRoute(teaTypeId))
                },
                onNavigateToAddTeaType = {
                    navigator.navigate(NavRoute.EditTeaTypeRoute())
                },
            )
        }

        entry<NavRoute.EditTeaTypeRoute> { route ->
            EditTeaTypeScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }

        // =========================================================
        // Tea Collection Destinations
        // =========================================================

        entry<NavRoute.TeaDetailsRoute> { route ->
            TeaDetailScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToEdit = { teaId ->
                    navigator.navigate(NavRoute.EditTeaRoute(teaId))
                },
                onNavigateToSession = { sessionId ->
                    navigator.navigate(NavRoute.SessionDetailsRoute(sessionId))
                },
                onNavigateToLogTea = { teaId, vesselId ->
                    navigator.navigate(NavRoute.LogTeaRoute(teaId, vesselId))
                },
                onNavigateToTimer = { sessionId ->
                    navigator.openOnHome(NavRoute.TimerRoute(sessionId))
                },
                onNavigateToHistory = { teaId ->
                    navigator.navigate(NavRoute.HistoryRoute(filterTeaId = teaId))
                },
                onNavigateToCreateConfig = { teaId ->
                    navigator.navigate(NavRoute.CreateBrewingConfigurationRoute(teaId))
                },
                onNavigateToEditConfig = { configId, teaId ->
                    navigator.navigate(NavRoute.CreateBrewingConfigurationRoute(teaId, configId))
                },
            )
        }

        entry<NavRoute.CreateBrewingConfigurationRoute> { route ->
            CreateBrewingConfigurationScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }

        entry<NavRoute.EditTeaRoute> { route ->
            EditTeaScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }

        // =========================================================
        // Vessel Management Destinations
        // =========================================================

        entry<NavRoute.VesselDetailRoute> { route ->
            VesselDetailScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToEditVessel = { vesselId ->
                    navigator.navigate(NavRoute.EditVesselRoute(vesselId))
                },
            )
        }

        entry<NavRoute.EditVesselRoute> { route ->
            EditVesselScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }

        // =========================================================
        // Session Logging Destinations
        // =========================================================

        entry<NavRoute.LogTeaRoute> { route ->
            LogTeaScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToTimer = { sessionId ->
                    navigator.navigateUp(route)
                    navigator.openOnHome(NavRoute.TimerRoute(sessionId))
                },
                onNavigateToReview = { sessionId ->
                    navigator.replace(route, NavRoute.SessionReviewRoute(sessionId, logged = true))
                },
                onNavigateToSession = { sessionId ->
                    navigator.navigate(NavRoute.SessionDetailsRoute(sessionId))
                },
            )
        }

        entry<NavRoute.SessionDetailsRoute> { route ->
            SessionDetailScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToEdit = { sessionId ->
                    navigator.navigate(NavRoute.EditSessionRoute(sessionId))
                },
                onNavigateToTea = { teaId ->
                    navigator.navigate(NavRoute.TeaDetailsRoute(teaId))
                },
                onNavigateToTimer = { sessionId ->
                    navigator.navigateUp(route)
                    navigator.openOnHome(NavRoute.TimerRoute(sessionId))
                },
            )
        }

        entry<NavRoute.EditSessionRoute> { route ->
            EditSessionScreen(
                route = route,
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }

        // =========================================================
        // Timer Destinations
        // =========================================================

        entry<NavRoute.TimerRoute> { route ->
            TimerScreen(
                route = route,
                // Timer keeps running in background via TimerService
                onNavigateBack = { navigator.navigateUp(route) },
                onNavigateToSteepComplete = { sessionId ->
                    navigator.replace(route, NavRoute.SteepCompleteRoute(sessionId))
                },
                onNavigateToSessionTimer = { sessionId ->
                    navigator.replace(route, NavRoute.TimerRoute(sessionId))
                },
            )
        }

        entry<NavRoute.SessionReviewRoute> { route ->
            SessionReviewScreen(
                route = route,
                onNavigateToHome = { finishedSessionId ->
                    if (route.logged) navigator.navigateUp(route)
                    else navigator.resetHome(NavRoute.HomeRoute(finishedSessionId))
                },
            )
        }

        entry<NavRoute.SteepCompleteRoute> { route ->
            SteepCompleteScreen(
                route = route,
                onNavigateToHome = { finishedSessionId ->
                    navigator.resetHome(NavRoute.HomeRoute(finishedSessionId))
                },
                onNavigateToReview = { sessionId ->
                    navigator.replace(route, NavRoute.SessionReviewRoute(sessionId))
                },
                onNavigateToTimer = { sessionId ->
                    navigator.replace(route, NavRoute.TimerRoute(sessionId))
                },
                onNavigateBack = { navigator.navigateUp(route) },
            )
        }
    }
}

/** Editors and the brew loop take the whole screen. */
private fun NavKey.hidesBottomBar() = when (this) {
    is NavRoute.LogTeaRoute,
    is NavRoute.EditTeaRoute,
    is NavRoute.EditSessionRoute,
    is NavRoute.EditVesselRoute,
    is NavRoute.EditTeaTypeRoute,
    is NavRoute.CreateBrewingConfigurationRoute,
    is NavRoute.TimerRoute,
    is NavRoute.SteepCompleteRoute,
    is NavRoute.SessionReviewRoute,
    is NavRoute.RecapRoute,
    -> true
    else -> false
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTabSwitch() =
    initialState.metadata[TAB_METADATA] != targetState.metadata[TAB_METADATA]

private fun fadeThrough() =
    fadeIn(tween(durationMillis = 210, delayMillis = 90)) togetherWith fadeOut(tween(90))

@Composable
private fun BottomNavigationBar(
    currentTab: Tab,
    onSelectTab: (Tab) -> Unit,
) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                selected = tab == currentTab,
                onClick = { onSelectTab(tab) },
            )
        }
    }
}

private val Tab.icon
    get() = when (this) {
        Tab.HOME -> FeatherIcons.Home
        Tab.COLLECTION -> FeatherIcons.Bookmark
        Tab.HISTORY -> FeatherIcons.Clock
        Tab.ANALYTICS -> FeatherIcons.BarChart2
    }

private val Tab.label
    get() = when (this) {
        Tab.HOME -> Res.string.tab_home
        Tab.COLLECTION -> Res.string.tab_collection
        Tab.HISTORY -> Res.string.tab_history
        Tab.ANALYTICS -> Res.string.tab_analytics
    }
