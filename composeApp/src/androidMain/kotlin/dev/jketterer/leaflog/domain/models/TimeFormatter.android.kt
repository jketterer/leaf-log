package dev.jketterer.leaflog.domain.models

import java.text.DateFormat
import java.util.Calendar

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }
}
