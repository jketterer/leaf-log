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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.UnitSwitchIcon
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.header_todays_stats
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.sessions_label
import leaf_log.shared.generated.resources.teas_label
import leaf_log.shared.generated.resources.view_all
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Section displaying daily statistics with three cards and a View All action.
 */
@Composable
fun DailyStatsSection(
    stats: DailyStats,
    onSessionsCardClick: () -> Unit,
    onTeasCardClick: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionHeader(
            title = stringResource(Res.string.header_todays_stats),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            TextButton(onClick = onViewAllClick) {
                Text(stringResource(Res.string.view_all))
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
                label = pluralStringResource(Res.plurals.sessions_label, stats.sessionCount),
                icon = FeatherIcons.Coffee,
                onClick = onSessionsCardClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            var volumeSwitched by rememberSaveable { mutableStateOf(false) }
            val volume = LocalUnits.current.volume.let { if (volumeSwitched) it.toggle() else it }
            DailyStatsCard(
                value = formatVolume(stats.totalWaterMl, volume),
                label = "Water Used",
                icon = FeatherIcons.Droplet,
                onClick = { volumeSwitched = !volumeSwitched },
                action = {
                    UnitSwitchIcon(
                        unit = volume,
                        switched = volumeSwitched,
                        onClick = { volumeSwitched = !volumeSwitched },
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = stats.differentTeasCount.toString(),
                label = pluralStringResource(Res.plurals.teas_label, stats.differentTeasCount),
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
                    totalWaterMl = 946.0,
                    differentTeasCount = 2,
                ),
                onSessionsCardClick = {},
                onTeasCardClick = {},
                onViewAllClick = {},
            )

            // Empty state
            DailyStatsSection(
                stats = DailyStats(),
                onSessionsCardClick = {},
                onTeasCardClick = {},
                onViewAllClick = {},
            )
        }
    }
}
