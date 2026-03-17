package dev.jketterer.leaflog.domain.models

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    actual fun formatRelativeTimestamp(instant: Instant): String {
        val date = Date(instant.toEpochMilliseconds())
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { time = date }

        val isToday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

        val isSameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)

        return when {
            isToday -> DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
            isYesterday -> "Yesterday"
            isSameYear -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    }
}
