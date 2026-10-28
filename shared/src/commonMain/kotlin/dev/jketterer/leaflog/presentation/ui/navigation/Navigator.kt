package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import kotlinx.serialization.Serializable

@Serializable
enum class Tab(val root: NavRoute) {
    HOME(NavRoute.HomeRoute()),
    COLLECTION(NavRoute.CollectionRoute),
    HISTORY(NavRoute.HistoryRoute()),
    ANALYTICS(NavRoute.AnalyticsRoute),
}

/**
 * One back stack per [Tab]. What's shown is Home's stack, then the current tab's, so back from a
 * tab's root lands on Home and back from Home exits. Hidden stacks stay put, ViewModels and all.
 */
class Navigator(
    currentTab: MutableState<Tab>,
    val stacks: Map<Tab, MutableList<NavKey>>,
) {
    var tab by currentTab
        private set

    val current: NavKey get() = stack.last()

    private val stack get() = stacks.getValue(tab)

    /** A tab's root switches to that tab; anything else pushes onto the current one. */
    fun navigate(route: NavRoute) {
        val target = Tab.entries.firstOrNull { it.root == route }
        if (target != null) tab = target else push(route)
    }

    /** A bottom bar tap. Re-tapping the current tab pops it to its root. */
    fun selectTab(target: Tab) {
        if (target == tab) stack.subList(1, stack.size).clear() else tab = target
    }

    /** Pops [from], unless it's a root or no longer on top, so a double tap can't pop twice. */
    fun navigateUp(from: NavKey) {
        if (stack.size > 1 && stack.last() == from) stack.removeAt(stack.lastIndex)
    }

    /** Replaces [from] with [to], with [navigateUp]'s guard. */
    fun replace(from: NavKey, to: NavRoute) {
        if (stack.last() == from) stack[stack.lastIndex] = to
    }

    /** System back. */
    fun goBack() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else tab = Tab.HOME
    }

    /**
     * The brew loop and notification taps live on Home's stack; the origin tab keeps its own. A
     * brew screen for the session already on top takes its place, so a tap racing the Timer's own
     * hand-off to Steep Complete can't stack one on the other.
     */
    fun openOnHome(route: NavRoute) {
        tab = Tab.HOME
        val sessionId = route.brewSessionId
        if (sessionId != null && stack.last().brewSessionId == sessionId) {
            stack[stack.lastIndex] = route
        } else {
            push(route)
        }
    }

    /** Leaves the brew loop for a fresh Home. */
    fun resetHome(home: NavRoute.HomeRoute) {
        tab = Tab.HOME
        stack.clear()
        stack.add(home)
    }

    private fun push(route: NavRoute) {
        if (stack.last() != route) stack.add(route)
    }
}

/** The session a brew loop screen shows, or null for any other screen and the Quick Timer. */
internal val NavKey.brewSessionId: String?
    get() = when (this) {
        is NavRoute.TimerRoute -> sessionId
        is NavRoute.SteepCompleteRoute -> sessionId
        else -> null
    }

@Composable
fun rememberNavigator(): Navigator {
    val tab = rememberSerializable(serializer = MutableStateSerializer(Tab.serializer())) {
        mutableStateOf(Tab.HOME)
    }
    val stacks = Tab.entries.associateWith { tab ->
        key(tab) { rememberNavBackStack(navSerializationConfig, tab.root) }
    }
    return remember { Navigator(tab, stacks) }
}

/** Home's entries, then the current tab's, each stack with its own state and ViewModels. */
@Composable
fun Navigator.entries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
    val decorated = stacks.mapValues { (tab, stack) ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = { route ->
                    val entry = entryProvider(route)
                    // The same route can sit in two stacks at once (a tea opened from Home and
                    // from Collection); NavDisplay needs their content keys distinct
                    NavEntry(
                        key = route,
                        contentKey = "${tab.name}/${entry.contentKey}",
                        metadata = entry.metadata + (TAB_METADATA to tab),
                    ) { entry.Content() }
                },
            )
        }
    }
    return if (tab == Tab.HOME) decorated.getValue(Tab.HOME)
    else decorated.getValue(Tab.HOME) + decorated.getValue(tab)
}

/** The [Tab] whose stack a [NavEntry] came from, so transitions can tell a switch from a push. */
const val TAB_METADATA = "tab"
