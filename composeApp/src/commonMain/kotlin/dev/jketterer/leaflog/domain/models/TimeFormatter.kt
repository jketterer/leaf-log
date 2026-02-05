package dev.jketterer.leaflog.domain.models

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
}
