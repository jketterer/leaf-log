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
import dev.jketterer.leaflog.presentation.ui.screens.HomeScreen
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionScreen

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
                    is NavRoute.HomeRoute -> NavEntry(entry) { HomeScreen() }
                    is NavRoute.CollectionRoute -> NavEntry(entry) {
                        TeaCollectionScreen(
                            onNavigateToAddTea = {},
                            onNavigateToTeaDetail = {}
                        )
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