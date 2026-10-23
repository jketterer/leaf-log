package dev.jketterer.leaflog.presentation.ui.components.recap

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.SeasonName
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_title_autumn
import leaf_log.shared.generated.resources.recap_title_spring
import leaf_log.shared.generated.resources.recap_title_summer
import leaf_log.shared.generated.resources.recap_title_winter
import leaf_log.shared.generated.resources.season_autumn
import leaf_log.shared.generated.resources.season_label
import leaf_log.shared.generated.resources.season_spring
import leaf_log.shared.generated.resources.season_summer
import leaf_log.shared.generated.resources.season_winter
import org.jetbrains.compose.resources.stringResource

/** "Summer 2026", or "Winter 2025–26" for the one crossing a new year. */
@Composable
fun seasonLabel(season: Season, hemisphere: Hemisphere?): String {
    val first = season.start.year
    val last = season.lastMonth.year
    val years = if (first == last) "$first" else "$first–${(last % 100).toString().padStart(2, '0')}"
    val name = when (season.nameIn(hemisphere)) {
        SeasonName.WINTER -> Res.string.season_winter
        SeasonName.SPRING -> Res.string.season_spring
        SeasonName.SUMMER -> Res.string.season_summer
        SeasonName.AUTUMN -> Res.string.season_autumn
    }
    return stringResource(Res.string.season_label, stringResource(name), years)
}

/** "Your summer in tea". */
@Composable
fun recapTitle(season: Season, hemisphere: Hemisphere?): String = stringResource(
    when (season.nameIn(hemisphere)) {
        SeasonName.WINTER -> Res.string.recap_title_winter
        SeasonName.SPRING -> Res.string.recap_title_spring
        SeasonName.SUMMER -> Res.string.recap_title_summer
        SeasonName.AUTUMN -> Res.string.recap_title_autumn
    },
)

/** Unset, the hemisphere is the device region's. */
@Composable
private fun Season.nameIn(hemisphere: Hemisphere?) =
    name(hemisphere ?: Hemisphere.forRegion(Locale.current.region))
