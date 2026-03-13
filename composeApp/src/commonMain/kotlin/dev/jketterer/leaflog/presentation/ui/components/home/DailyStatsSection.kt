package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.composeapp.generated.resources.Res
import leaf_log.composeapp.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource

/**
 * Section displaying daily statistics with three cards and a View All action.
 */
@Composable
fun DailyStatsSection(
    stats: DailyStats,
    onSessionsCardClick: () -> Unit,
    onWaterCardClick: () -> Unit,
    onTeasCardClick: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY'S STATS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onViewAllClick) {
                Text("View All")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DailyStatsCard(
                value = stats.sessionCount.toString(),
                label = if (stats.sessionCount == 1) "Session" else "Sessions",
                icon = FeatherIcons.Coffee,
                onClick = onSessionsCardClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = stats.formattedWaterQuantity,
                label = "Water Used",
                icon = FeatherIcons.Droplet,
                onClick = onWaterCardClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = stats.differentTeasCount.toString(),
                label = if (stats.differentTeasCount == 1) "Tea" else "Teas",
                icon = vectorResource(Res.drawable.ic_tea_leaf),
                onClick = onTeasCardClick,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                    formattedWaterQuantity = "32 fl oz",
                    differentTeasCount = 2,
                ),
                onSessionsCardClick = {},
                onWaterCardClick = {},
                onTeasCardClick = {},
                onViewAllClick = {},
            )

            // Empty state
            DailyStatsSection(
                stats = DailyStats(),
                onSessionsCardClick = {},
                onWaterCardClick = {},
                onTeasCardClick = {},
                onViewAllClick = {},
            )
        }
    }
}
