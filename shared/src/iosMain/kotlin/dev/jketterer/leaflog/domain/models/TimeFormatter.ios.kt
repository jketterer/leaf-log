package dev.jketterer.leaflog.domain.models

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(0u, fromDate = calendar.startOfDayForDate(
            NSDate()
        ))
        components.setHour(hour.toLong())
        components.setMinute(minute.toLong())
        val date = calendar.dateFromComponents(components) ?: return "$hour:${minute.toString().padStart(2, '0')}"

        val formatter = NSDateFormatter()
        formatter.timeStyle = NSDateFormatterShortStyle
        formatter.dateStyle = 0u // NSDateFormatterNoStyle
        return formatter.stringFromDate(date)
    }

    actual fun formatRelativeTimestamp(instant: Instant): String {
        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        val formatter = NSDateFormatter()
        return when (relativeDayOf(instant, Clock.System.now(), TimeZone.currentSystemDefault())) {
            RelativeDay.TODAY -> {
                formatter.timeStyle = NSDateFormatterShortStyle
                formatter.dateStyle = 0u // NSDateFormatterNoStyle
                formatter.stringFromDate(date)
            }
            RelativeDay.YESTERDAY -> "Yesterday"
            RelativeDay.THIS_YEAR -> {
                formatter.setDateFormat("MMM d")
                formatter.stringFromDate(date)
            }
            RelativeDay.OLDER -> {
                formatter.setDateFormat("MMM d, yyyy")
                formatter.stringFromDate(date)
            }
        }
    }
}
