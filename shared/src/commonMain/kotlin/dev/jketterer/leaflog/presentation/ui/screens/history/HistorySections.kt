package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatClockTime
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.history_tea_count
import leaf_log.shared.generated.resources.history_today
import leaf_log.shared.generated.resources.history_yesterday
import leaf_log.shared.generated.resources.time_range
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.hours

/**
 * The day or month over a run of [sessions]. A day with two or more adds how many teas, and the
 * clock times from its first steep to its last.
 */
@Composable
internal fun HistoryGroupHeader(
    period: HistoryPeriod,
    sessions: List<SessionWithSteeps>,
    modifier: Modifier = Modifier,
) {
    val tz = TimeZone.currentSystemDefault()
    val today = currentInstant().toLocalDateTime(tz).date
    val title = when (period) {
        is HistoryPeriod.Day -> when (period.date) {
            today -> stringResource(Res.string.history_today)
            today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.history_yesterday)
            else -> TimeFormatter.formatDate(period.date, DateSkeleton.WEEKDAY_MONTH_DAY)
        }
        is HistoryPeriod.Month -> TimeFormatter.formatDate(period.firstDay, DateSkeleton.MONTH_YEAR)
    }

    FlowRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (period is HistoryPeriod.Day && sessions.size > 1) {
            val teaCount = sessions.distinctBy { it.session.teaId }.size
            val start = sessions.minOf { it.startedAt }
            val end = sessions.maxOf { it.steeps.last().let { steep -> steep.timestamp + steep.brewedTime } }
            val metaStyle = MaterialTheme.typography.bodySmall
            Text(
                text = pluralStringResource(Res.plurals.history_tea_count, teaCount, teaCount),
                style = metaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FeatherIcons.Clock,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        Res.string.time_range,
                        TimeFormatter.formatClockTime(start),
                        TimeFormatter.formatClockTime(end),
                    ),
                    style = metaStyle.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun HistoryGroupHeaderDayPreview() {
    val morning = previewSession(id = "1", ratings = listOf(4f, 4f, 3f), status = SessionStatus.COMPLETED)
    val afternoon = previewSession(id = "2", status = SessionStatus.COMPLETED).let {
        it.copy(
            session = it.session.copy(teaId = "tea-2"),
            steeps = it.steeps.map { steep -> steep.copy(timestamp = steep.timestamp + 5.hours) },
        )
    }
    LeafLogTheme {
        HistoryGroupHeader(
            period = HistoryPeriod.Day(PreviewInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date),
            sessions = listOf(afternoon, morning),
        )
    }
}

@Preview
@Composable
private fun HistoryGroupHeaderMonthPreview() {
    val date = PreviewInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    LeafLogTheme {
        HistoryGroupHeader(
            period = HistoryPeriod.Month(LocalDate(date.year, date.month, 1)),
            sessions = listOf(previewSession(status = SessionStatus.COMPLETED)),
        )
    }
}
