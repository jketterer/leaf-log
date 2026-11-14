package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The scope NavDisplay animates in, for an element that holds still while its screen changes
 * around it. Null outside NavDisplay, as in a preview, where such an element is drawn plainly.
 */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Marks the brew flow's screens: moving between two of them crossfades rather than slides. */
const val BREW_FLOW_METADATA = "brewFlow"

val BrewFlowMetadata = mapOf(BREW_FLOW_METADATA to true)
