package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.recap.recapTitle
import dev.jketterer.leaflog.presentation.ui.components.recap.seasonLabel
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_months
import leaf_log.shared.generated.resources.recap_sessions
import leaf_log.shared.generated.resources.recap_teas
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CoverPage(season: Season, hemisphere: Hemisphere?, cover: RecapPage.Cover) {
    RecapPageColumn {
        Text(
            text = seasonLabel(season, hemisphere),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = recapTitle(season, hemisphere),
            style = MaterialTheme.typography.displaySmall,
        )
        val sameYear = season.start.year == season.lastMonth.year
        Text(
            text = stringResource(
                Res.string.recap_months,
                TimeFormatter.formatDate(
                    season.start,
                    if (sameYear) DateSkeleton.MONTH else DateSkeleton.MONTH_YEAR,
                ),
                TimeFormatter.formatDate(season.lastMonth, DateSkeleton.MONTH_YEAR),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = cover.sessions.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_sessions, cover.sessions),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = cover.teas.toString(),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_teas, cover.teas),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Preview
@Composable
private fun CoverPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 0) {
            CoverPage(Season(LocalDate(2026, 6, 1)), Hemisphere.NORTHERN, RecapPage.Cover(sessions = 64, teas = 17))
        }
    }
}

@Preview
@Composable
private fun CoverPageAcrossNewYearPreview() {
    LeafLogTheme {
        RecapPageSurface(index = 0) {
            CoverPage(Season(LocalDate(2025, 12, 1)), Hemisphere.SOUTHERN, RecapPage.Cover(sessions = 1, teas = 1))
        }
    }
}
