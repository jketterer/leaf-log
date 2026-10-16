package dev.jketterer.leaflog.domain.models

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

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

    actual fun formatDate(date: LocalDate, skeleton: DateSkeleton): String {
        val formatter = NSDateFormatter()
        formatter.setLocalizedDateFormatFromTemplate(skeleton.pattern)
        val start = date.atStartOfDayIn(TimeZone.currentSystemDefault())
        return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(start.epochSeconds.toDouble()))
    }
}
