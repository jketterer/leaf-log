package dev.jketterer.leaflog.presentation.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that routes notification taps to the correct screen.
 * Platform code calls [setRoute], and [AppNavigation] collects [pendingRoute]
 * to push the destination onto the back stack.
 */
object DeepLinkHandler {
    private val _pendingRoute = MutableStateFlow<NavRoute?>(null)
    val pendingRoute: StateFlow<NavRoute?> = _pendingRoute.asStateFlow()

    fun setRoute(route: NavRoute) {
        _pendingRoute.value = route
    }

    fun consume() {
        _pendingRoute.value = null
    }
}
