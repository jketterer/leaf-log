package dev.jketterer.leaflog.domain.models

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

actual object TimeFormatter {
    actual fun formatClockTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    // ICU rather than java.text, which can't parse the standalone fields ("LLL") ICU's patterns use
    actual fun formatDate(date: LocalDate, skeleton: DateSkeleton): String =
        android.icu.text.DateFormat.getInstanceForSkeleton(skeleton.pattern, Locale.getDefault())
            .format(Date(date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()))
}
