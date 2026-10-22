package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Formats clock times and dates according to the device's locale settings.
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

    /** Formats [date] with the fields of [skeleton], in the locale's order and punctuation. */
    fun formatDate(date: LocalDate, skeleton: DateSkeleton): String
}

/** The fields a formatted date shows. Examples are en-US. */
enum class DateSkeleton(val pattern: String) {
    /** "Jan" */
    MONTH("MMM"),

    /** "Jan 15" */
    MONTH_DAY("MMMd"),

    /** "1/15" */
    NUMERIC_MONTH_DAY("Md"),

    /** "Jan 2024" */
    MONTH_YEAR("yMMM"),

    /** "Jan 15, 2024" */
    MONTH_DAY_YEAR("yMMMd"),

    /** "Monday, January 15" */
    WEEKDAY_MONTH_DAY("EEEEMMMMd"),
}

/** [instant]'s clock time in the device's time zone: "2:30 PM". */
fun TimeFormatter.formatClockTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return formatClockTime(local.hour, local.minute)
}

/**
 * Formats a timestamp relative to [now]:
 * - Today: "2:30 PM"
 * - Yesterday: "Yesterday"
 * - This year: "Jan 15"
 * - Previous years: "Jan 15, 2024"
 */
fun TimeFormatter.formatRelativeTimestamp(instant: Instant, now: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val local = instant.toLocalDateTime(tz)
    return when (relativeDayOf(instant, now, tz)) {
        RelativeDay.TODAY -> formatClockTime(local.hour, local.minute)
        RelativeDay.YESTERDAY -> "Yesterday"
        RelativeDay.THIS_YEAR -> formatDate(local.date, DateSkeleton.MONTH_DAY)
        RelativeDay.OLDER -> formatDate(local.date, DateSkeleton.MONTH_DAY_YEAR)
    }
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
