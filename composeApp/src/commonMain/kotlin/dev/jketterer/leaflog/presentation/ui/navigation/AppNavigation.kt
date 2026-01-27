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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.regular.Building
import compose.icons.fontawesomeicons.regular.Clock
import compose.icons.fontawesomeicons.regular.CommentDots
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionScreen
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryScreen
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeScreen
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaScreen

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
                currentRoute is NavRoute.AnalyticsRoute ||
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
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            entryProvider = { entry ->
                when (entry) {
                    is NavRoute.HomeRoute -> NavEntry(entry) {
                        HomeScreen(
                            onNavigateToLogTea = { backStack.add(NavRoute.LogTeaRoute) },
                            onNavigateToSession = { sessionId ->
                                backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                            },
                            onNavigateToHistory = { backStack.add(NavRoute.HistoryRoute) },
                            onNavigateToSettings = { backStack.add(NavRoute.SettingsRoute) },
                        )
                    }

                    is NavRoute.CollectionRoute -> NavEntry(entry) {
                        TeaCollectionScreen(
                            onNavigateToAddTea = { backStack.add(NavRoute.EditTeaRoute(null)) },
                            onNavigateToTeaDetail = { teaId ->
                                backStack.add(NavRoute.TeaDetailsRoute(teaId))
                            }
                        )
                    }

                    is NavRoute.HistoryRoute -> NavEntry(entry) {
                        HistoryScreen(
                            onNavigateToSession = { sessionId ->
                                backStack.add(NavRoute.SessionDetailsRoute(sessionId))
                            },
                        )
                    }

                    is NavRoute.LogTeaRoute -> NavEntry(entry) {
                        LogTeaScreen(
                            onNavigateBack = { backStack.removeLast() },
                            onNavigateToTimer = { backStack.add(NavRoute.TimerRoute) },
                        )
                    }

                    is NavRoute.EditTeaRoute -> NavEntry(entry) {
                    }

                    else -> NavEntry(entry) { Text("ugh") }
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
                    imageVector = FontAwesomeIcons.Regular.Building,
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
                    imageVector = FontAwesomeIcons.Regular.Bookmark,
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
                    imageVector = FontAwesomeIcons.Regular.Clock,
                    contentDescription = ""
                )
            },
            label = { Text("History") },
            selected = currentRoute is NavRoute.HistoryRoute,
            onClick = { onNavigate(NavRoute.HistoryRoute) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = FontAwesomeIcons.Regular.CommentDots,
                    contentDescription = ""
                )
            },
            label = { Text("More") },
            selected = currentRoute is NavRoute.SettingsRoute,
            onClick = { onNavigate(NavRoute.SettingsRoute) }
        )
    }
}