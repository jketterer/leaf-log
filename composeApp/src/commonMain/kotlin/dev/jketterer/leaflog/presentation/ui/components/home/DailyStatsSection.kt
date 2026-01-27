package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Section displaying daily statistics with three cards.
 */
@Composable
fun DailyStatsSection(
    stats: DailyStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "TODAY'S STATS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DailyStatsCard(
                value = stats.sessionCount.toString(),
                label = if (stats.sessionCount == 1) "Session" else "Sessions",
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = stats.getFormattedWaterQuantity(useMetric = true),
                label = "Water",
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = stats.differentTeasCount.toString(),
                label = if (stats.differentTeasCount == 1) "Tea" else "Teas",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyStatsSectionPreview() {
    LeafLogTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // With data
            DailyStatsSection(
                stats = DailyStats(
                    sessionCount = 3,
                    totalWaterQuantityMl = 450,
                    differentTeasCount = 2,
                ),
            )

            // Empty state
            DailyStatsSection(
                stats = DailyStats(),
            )
        }
    }
}