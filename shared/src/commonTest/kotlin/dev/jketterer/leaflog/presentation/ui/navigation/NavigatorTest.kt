package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {

    private val navigator = Navigator(
        currentTab = mutableStateOf(Tab.HOME),
        stacks = Tab.entries.associateWith { mutableListOf<NavKey>(it.root) },
    )

    private val tea = NavRoute.TeaDetailsRoute("tie-guan-yin")
    private val session = NavRoute.SessionDetailsRoute("s1")

    private fun stack(tab: Tab) = navigator.stacks.getValue(tab)

    @Test
    fun `a tab's root switches tabs and leaves stacks alone`() {
        navigator.navigate(NavRoute.CollectionRoute)

        assertEquals(Tab.COLLECTION, navigator.tab)
        assertEquals(listOf<NavKey>(Tab.HOME.root), stack(Tab.HOME))
        assertEquals(listOf<NavKey>(Tab.COLLECTION.root), stack(Tab.COLLECTION))
    }

    @Test
    fun `a route with args pushes onto the current tab`() {
        val filtered = NavRoute.HistoryRoute(filterTeaId = "tie-guan-yin")
        navigator.navigate(filtered)

        assertEquals(Tab.HOME, navigator.tab)
        assertEquals(listOf<NavKey>(Tab.HOME.root, filtered), stack(Tab.HOME))
        assertEquals(listOf<NavKey>(Tab.HISTORY.root), stack(Tab.HISTORY))
    }

    @Test
    fun `a hidden tab keeps its stack`() {
        navigator.selectTab(Tab.COLLECTION)
        navigator.navigate(tea)
        navigator.selectTab(Tab.HISTORY)
        navigator.selectTab(Tab.COLLECTION)

        assertEquals(tea, navigator.current)
    }

    @Test
    fun `back from a tab's root returns Home`() {
        navigator.navigate(NavRoute.SettingsRoute)
        navigator.selectTab(Tab.COLLECTION)

        navigator.goBack()

        assertEquals(Tab.HOME, navigator.tab)
        assertEquals(NavRoute.SettingsRoute, navigator.current)
    }

    @Test
    fun `back pops within a tab before leaving it`() {
        navigator.selectTab(Tab.COLLECTION)
        navigator.navigate(tea)

        navigator.goBack()

        assertEquals(Tab.COLLECTION, navigator.tab)
        assertEquals(Tab.COLLECTION.root, navigator.current)
    }

    @Test
    fun `re-tapping the current tab pops it to its root`() {
        navigator.selectTab(Tab.COLLECTION)
        navigator.navigate(tea)
        navigator.navigate(session)

        navigator.selectTab(Tab.COLLECTION)

        assertEquals(listOf<NavKey>(Tab.COLLECTION.root), stack(Tab.COLLECTION))
    }

    @Test
    fun `a second up from the same entry doesn't pop twice`() {
        navigator.navigate(tea)
        navigator.navigate(session)

        navigator.navigateUp(session)
        navigator.navigateUp(session)

        assertEquals(tea, navigator.current)
    }

    @Test
    fun `up never pops a root`() {
        navigator.navigateUp(Tab.HOME.root)

        assertEquals(listOf<NavKey>(Tab.HOME.root), stack(Tab.HOME))
    }

    @Test
    fun `a double tap doesn't push twice`() {
        navigator.navigate(tea)
        navigator.navigate(tea)

        assertEquals(listOf<NavKey>(Tab.HOME.root, tea), stack(Tab.HOME))
    }

    @Test
    fun `the brew loop opens on Home and the origin tab keeps its stack`() {
        val timer = NavRoute.TimerRoute("s1")
        navigator.selectTab(Tab.COLLECTION)
        navigator.navigate(tea)

        navigator.openOnHome(timer)

        assertEquals(Tab.HOME, navigator.tab)
        assertEquals(listOf<NavKey>(Tab.HOME.root, timer), stack(Tab.HOME))
        assertEquals(listOf<NavKey>(Tab.COLLECTION.root, tea), stack(Tab.COLLECTION))
    }

    @Test
    fun `replace swaps only the entry that asked`() {
        val timer = NavRoute.TimerRoute("s1")
        val steepComplete = NavRoute.SteepCompleteRoute("s1")
        navigator.openOnHome(timer)

        navigator.replace(timer, steepComplete)
        navigator.replace(timer, steepComplete)

        assertEquals(listOf<NavKey>(Tab.HOME.root, steepComplete), stack(Tab.HOME))
    }

    @Test
    fun `a brew screen for the session on top takes its place`() {
        val timer = NavRoute.TimerRoute("s1")
        val steepComplete = NavRoute.SteepCompleteRoute("s1")
        navigator.openOnHome(timer)
        navigator.replace(timer, steepComplete)

        navigator.openOnHome(timer)

        assertEquals(listOf<NavKey>(Tab.HOME.root, timer), stack(Tab.HOME))
    }

    @Test
    fun `leaving the brew loop resets Home only`() {
        val finished = NavRoute.HomeRoute(finishedSessionId = "s1")
        navigator.selectTab(Tab.COLLECTION)
        navigator.navigate(tea)
        navigator.openOnHome(NavRoute.SteepCompleteRoute("s1"))

        navigator.resetHome(finished)

        assertEquals(listOf<NavKey>(finished), stack(Tab.HOME))
        assertEquals(listOf<NavKey>(Tab.COLLECTION.root, tea), stack(Tab.COLLECTION))
    }
}
