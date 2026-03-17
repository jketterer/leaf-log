package dev.jketterer.leaflog.domain.models

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970
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
        val now = NSDate()
        val calendar = NSCalendar.currentCalendar

        val nowComponents = calendar.components(NSCalendarUnitYear or NSCalendarUnitDay, fromDate = now)
        val thenComponents = calendar.components(NSCalendarUnitYear or NSCalendarUnitDay, fromDate = date)

        val isToday = nowComponents.year == thenComponents.year &&
                nowComponents.day == thenComponents.day

        val yesterday = calendar.dateByAddingUnit(
            NSCalendarUnitDay,
            value = -1,
            toDate = now,
            options = 0u
        ) ?: now
        val yesterdayComponents = calendar.components(NSCalendarUnitYear or NSCalendarUnitDay, fromDate = yesterday)
        val isYesterday = yesterdayComponents.year == thenComponents.year &&
                yesterdayComponents.day == thenComponents.day

        val isSameYear = nowComponents.year == thenComponents.year

        val formatter = NSDateFormatter()
        return when {
            isToday -> {
                formatter.timeStyle = NSDateFormatterShortStyle
                formatter.dateStyle = 0u // NSDateFormatterNoStyle
                formatter.stringFromDate(date)
            }
            isYesterday -> "Yesterday"
            isSameYear -> {
                formatter.setDateFormat("MMM d")
                formatter.stringFromDate(date)
            }
            else -> {
                formatter.setDateFormat("MMM d, yyyy")
                formatter.stringFromDate(date)
            }
        }
    }
}
