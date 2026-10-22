package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.days_ago
import leaf_log.shared.generated.resources.months_ago
import leaf_log.shared.generated.resources.today
import leaf_log.shared.generated.resources.weeks_ago
import leaf_log.shared.generated.resources.years_ago
import leaf_log.shared.generated.resources.yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** The instant @Preview data is built around, so a preview renders the same on every day. */
internal val PreviewInstant = Instant.fromEpochSeconds(1_790_000_000L)

/** Now, or [PreviewInstant] in a preview, so "today" and "3 days ago" don't move its screenshot. */
@Composable
internal fun currentInstant(): Instant =
    if (LocalInspectionMode.current) PreviewInstant else Clock.System.now()

/** How long ago [instant] was in calendar days, roughly: "today", "3 days ago", "2 months ago". */
@Composable
internal fun timeAgo(instant: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val days = instant.toLocalDateTime(tz).date.daysUntil(currentInstant().toLocalDateTime(tz).date)
    return when {
        days == 0 -> stringResource(Res.string.today)
        days == 1 -> stringResource(Res.string.yesterday)
        days < 7 -> pluralStringResource(Res.plurals.days_ago, days, days)
        days < 30 -> pluralStringResource(Res.plurals.weeks_ago, days / 7, days / 7)
        days < 365 -> pluralStringResource(Res.plurals.months_ago, days / 30, days / 30)
        else -> pluralStringResource(Res.plurals.years_ago, days / 365, days / 365)
    }
}
