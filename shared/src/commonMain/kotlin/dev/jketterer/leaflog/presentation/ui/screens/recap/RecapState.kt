package dev.jketterer.leaflog.presentation.ui.screens.recap

import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season

data class RecapState(
    val season: Season,
    /** Null until chosen in Settings, when it follows the device's region. */
    val hemisphere: Hemisphere? = null,
    /** Only the pages with something to show, in order. */
    val pages: List<RecapPage> = emptyList(),
)

sealed interface RecapPage {
    data class Cover(val sessions: Int, val teas: Int) : RecapPage
}
