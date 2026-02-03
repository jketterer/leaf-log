package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Home
import compose.icons.feathericons.MoreHorizontal
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.screens.collection.EditTeaScreen
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionScreen
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.EditSessionScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.SessionDetailScreen
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeScreen
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaScreen
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.settings.SettingsScreen
import dev.jketterer.leaflog.presentation.ui.screens.timer.TimerScreen
import dev.jketterer.leaflog.presentation.ui.screens.vessel.EditVesselScreen
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselDetailScreen
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(
        configuration = navSerializationConfig,
        elements = arrayOf(NavRoute.HomeRoute)
    )

    Scaffold(
        bottomBar = {
            val currentRoute = backStack.lastOrNull()
            if (currentRoute is NavRoute.HomeRoute ||
                currentRoute is NavRoute.CollectionRoute ||
                currentRoute is NavRoute.HistoryRoute ||
                currentRoute is NavRoute.SettingsRoute
            ) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        backStack.clear()
                        backStack.add(route)
                    }
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<NavRoute.HomeRoute> {
                    HomeScreen(
                        onNavigateToLogTea = { teaId, vesselId ->
                            backStack.add(NavRoute.LogTeaRoute(teaId, vesselId))
                        },
                        onNavigateToSession = { sessionId ->
                            backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                        },
                        onNavigateToHistory = { backStack.add(NavRoute.HistoryRoute()) },
                        onNavigateToSettings = { backStack.add(NavRoute.SettingsRoute) },
                    )
                }

                entry<NavRoute.CollectionRoute> {
                    TeaCollectionScreen(
                        onNavigateToTeaDetail = { teaId ->
                            backStack.add(NavRoute.TeaDetailsRoute(teaId))
                        },
                        onNavigateToAddTea = {
                            backStack.add(NavRoute.EditTeaRoute())
                        },
                        onNavigateToManageVessels = {
                            backStack.add(NavRoute.VesselListRoute)
                        },
                    )
                }

                entry<NavRoute.HistoryRoute> { route ->
                    HistoryScreen(
                        showDraftsOnly = route.showDraftsOnly,
                        onNavigateToSession = { sessionId ->
                            backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                        },
                    )
                }

                entry<NavRoute.AnalyticsRoute> {
                    EmptyState(
                        message = "Analytics screen will be implemented in Phase 7",
                        onActionClick = { backStack.removeLast() },
                    )
                }

                entry<NavRoute.SettingsRoute> {
                    SettingsScreen()
                }

                // =========================================================
                // Tea Collection Destinations
                // =========================================================

                entry<NavRoute.TeaDetailsRoute> { route ->
                    TeaDetailScreen(
                        teaId = route.teaId,
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToEdit = { teaId ->
                            backStack.add(NavRoute.EditTeaRoute(teaId))
                        },
                        onNavigateToSession = { sessionId ->
                            backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                        },
                        onNavigateToLogTea = { teaId, vesselId ->
                            backStack.add(NavRoute.LogTeaRoute(teaId, vesselId))
                        },
                    )
                }

                entry<NavRoute.EditTeaRoute> { entry ->
                    EditTeaScreen(
                        teaId = entry.teaId,
                        onNavigateBack = { backStack.removeLast() },
                    )
                }

                // =========================================================
                // Vessel Management Destinations
                // =========================================================

                entry<NavRoute.VesselListRoute> {
                    VesselListScreen(
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToVesselDetail = { vesselId ->
                            backStack.add(NavRoute.VesselDetailRoute(vesselId))
                        },
                        onNavigateToAddVessel = {
                            backStack.add(NavRoute.EditVesselRoute())
                        },
                    )
                }

                entry<NavRoute.VesselDetailRoute> { route ->
                    VesselDetailScreen(
                        vesselId = route.vesselId,
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToEditVessel = { vesselId ->
                            backStack.add(NavRoute.EditVesselRoute(vesselId))
                        },
                    )
                }

                entry<NavRoute.EditVesselRoute> { route ->
                    EditVesselScreen(
                        vesselId = route.vesselId,
                        onNavigateBack = { backStack.removeLast() },
                    )
                }

                // =========================================================
                // Session Logging Destinations
                // =========================================================

                entry<NavRoute.LogTeaRoute> { route ->
                    LogTeaScreen(
                        teaId = route.teaId,
                        vesselId = route.vesselId,
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToTimer = { sessionId ->
                            backStack.add(NavRoute.TimerRoute(sessionId))
                        },
                        viewModel = koinViewModel<LogTeaViewModel>()
                    )
                }

                entry<NavRoute.SessionDetailsRoute> { entry ->
                    SessionDetailScreen(
                        sessionId = entry.sessionId,
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToEdit = { sessionId, editFullSession ->
                            backStack.add(NavRoute.EditSessionRoute(sessionId, editFullSession))
                        },
                        onNavigateToTea = { teaId ->
                            backStack.add(NavRoute.TeaDetailsRoute(teaId))
                        },
                        onNavigateToTimer = { sessionId ->
                            backStack.clear()
                            backStack.add(NavRoute.TimerRoute(sessionId))
                            backStack.add(0, NavRoute.HomeRoute)
                        },
                    )
                }

                entry<NavRoute.EditSessionRoute> { route ->
                    EditSessionScreen(
                        sessionId = route.sessionId,
                        editFullSession = route.editFullSession,
                        onNavigateBack = { backStack.removeLast() },
                    )
                }

                // =========================================================
                // Timer Destinations
                // =========================================================

                entry<NavRoute.TimerRoute> { entry ->
                    TimerScreen(
                        sessionId = entry.sessionId,
                        onNavigateBack = {
                            // Timer keeps running in background via TimerService
                            backStack.removeLast()
                        },
                        onNavigateToNextSteep = { parentSessionId ->
                            // do nothing?
                        },
                        onNavigateToComplete = { sessionId ->
                            backStack.clear()
                            backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                            backStack.add(0, NavRoute.HomeRoute)
                        },
                    )
                }
            }
        )
    }

}

@Composable
private fun BottomNavigationBar(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = FeatherIcons.Home,
                    contentDescription = ""
                )
            },
            label = { Text("Home") },
            selected = currentRoute is NavRoute.HomeRoute,
            onClick = { onNavigate(NavRoute.HomeRoute) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = FeatherIcons.Bookmark,
                    contentDescription = ""
                )
            },
            label = { Text("Collection") },
            selected = currentRoute is NavRoute.CollectionRoute,
            onClick = { onNavigate(NavRoute.CollectionRoute) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = FeatherIcons.Clock,
                    contentDescription = ""
                )
            },
            label = { Text("History") },
            selected = currentRoute is NavRoute.HistoryRoute,
            onClick = { onNavigate(NavRoute.HistoryRoute()) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = FeatherIcons.MoreHorizontal,
                    contentDescription = ""
                )
            },
            label = { Text("More") },
            selected = currentRoute is NavRoute.SettingsRoute,
            onClick = { onNavigate(NavRoute.SettingsRoute) }
        )
    }
}