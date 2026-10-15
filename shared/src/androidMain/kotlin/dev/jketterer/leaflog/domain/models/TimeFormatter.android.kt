package dev.jketterer.leaflog.domain.models

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    actual fun formatRelativeTimestamp(instant: Instant, now: Instant): String {
        val date = Date(instant.toEpochMilliseconds())
        return when (relativeDayOf(instant, now, TimeZone.currentSystemDefault())) {
            RelativeDay.TODAY -> DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
            RelativeDay.YESTERDAY -> "Yesterday"
            RelativeDay.THIS_YEAR -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            RelativeDay.OLDER -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    }
}
