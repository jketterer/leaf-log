package dev.jketterer.leaflog.presentation.ui.screens.recap

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopTea
import kotlinx.datetime.LocalDate

data class RecapState(
    val season: Season,
    /** Null until chosen in Settings, when it follows the device's region. */
    val hemisphere: Hemisphere? = null,
    /** Only the pages with something to show, in order. */
    val pages: List<RecapPage> = emptyList(),
)

sealed interface RecapPage {
    data class Cover(val sessions: Int, val teas: Int) : RecapPage

    /** The season's days, and its busiest month by its first day. */
    data class Rhythm(
        val cells: List<ActivityCell>,
        val busiestMonth: LocalDate,
        val busiestMonthSessions: Int,
    ) : RecapPage

    /** The season's top 3 teas, and every tea type's share of its sessions. */
    data class MostBrewed(
        val teas: List<TopTea>,
        val teaTypes: List<TeaTypeDistribution>,
    ) : RecapPage

    /** Teas first brewed in the season, in the order they were. */
    data class Discoveries(val teas: List<Tea>) : RecapPage

    /** The season's flavor profile, and the ids of its tags missing from the season before's. */
    data class Flavors(
        val profile: FlavorProfile,
        val newTagIds: Set<String>,
    ) : RecapPage
}
