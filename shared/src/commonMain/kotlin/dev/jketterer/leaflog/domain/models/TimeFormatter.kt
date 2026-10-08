package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Formats clock times according to the device's locale settings (12h vs 24h).
 */
expect object TimeFormatter {
    /**
     * Formats an hour and minute into a locale-appropriate clock time string.
     *
     * @param hour Hour of the day (0-23)
     * @param minute Minute of the hour (0-59)
     * @return Formatted time string (e.g., "2:30 PM" or "14:30" depending on locale)
     */
    fun formatClockTime(hour: Int, minute: Int): String

    /**
     * Formats a timestamp relative to now:
     * - Today: "2:30 PM"
     * - Yesterday: "Yesterday"
     * - This year: "Jan 15"
     * - Previous years: "Jan 15, 2024"
     */
    fun formatRelativeTimestamp(instant: Instant): String
}

internal enum class RelativeDay { TODAY, YESTERDAY, THIS_YEAR, OLDER }

/** Which bucket of [TimeFormatter.formatRelativeTimestamp] [instant] falls in, as seen from [now] in [tz]. */
internal fun relativeDayOf(instant: Instant, now: Instant, tz: TimeZone): RelativeDay {
    val date = instant.toLocalDateTime(tz).date
    val today = now.toLocalDateTime(tz).date
    return when {
        date == today -> RelativeDay.TODAY
        date == today.minus(1, DateTimeUnit.DAY) -> RelativeDay.YESTERDAY
        date.year == today.year -> RelativeDay.THIS_YEAR
        else -> RelativeDay.OLDER
    }
}
