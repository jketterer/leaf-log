package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.analytics.ActivityHeatmap
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_busiest_month
import org.jetbrains.compose.resources.pluralStringResource

@Composable
internal fun RhythmPage(rhythm: RecapPage.Rhythm, onTapDay: (LocalDate) -> Unit) {
    RecapPageColumn {
        Text(
            text = rhythm.busiestMonthSessions.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(
                Res.plurals.recap_busiest_month,
                rhythm.busiestMonthSessions,
                TimeFormatter.formatDate(rhythm.busiestMonth, DateSkeleton.MONTH),
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        // On surface, so tea type colors read as they do in Analytics
        Surface(shape = MaterialTheme.shapes.large) {
            ActivityHeatmap(
                cells = rhythm.cells,
                period = AnalyticsPeriod.THIS_SEASON,
                onTapCell = onTapDay,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun RhythmPagePreview() {
    val start = LocalDate(2026, 6, 1)
    LeafLogTheme {
        RecapPageSurface(index = 1) {
            RhythmPage(
                rhythm = RecapPage.Rhythm(
                    cells = List(92) { index ->
                        val sessions = (index * 7 % 5).coerceAtMost(3)
                        ActivityCell(
                            date = start.plus(index, DateTimeUnit.DAY),
                            dominantTeaTypeColorHex = if (sessions > 0) "#4CAF50" else null,
                            sessionCount = sessions,
                        )
                    },
                    busiestMonth = LocalDate(2026, 7, 1),
                    busiestMonthSessions = 28,
                ),
                onTapDay = {},
            )
        }
    }
}
