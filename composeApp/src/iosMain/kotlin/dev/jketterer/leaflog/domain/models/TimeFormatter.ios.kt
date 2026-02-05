package dev.jketterer.leaflog.domain.models

import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(0u, fromDate = calendar.startOfDayForDate(
            platform.Foundation.NSDate()
        ))
        components.setHour(hour.toLong())
        components.setMinute(minute.toLong())
        val date = calendar.dateFromComponents(components) ?: return "$hour:${minute.toString().padStart(2, '0')}"

        val formatter = NSDateFormatter()
        formatter.timeStyle = NSDateFormatterShortStyle
        formatter.dateStyle = 0u // NSDateFormatterNoStyle
        return formatter.stringFromDate(date)
    }
}
