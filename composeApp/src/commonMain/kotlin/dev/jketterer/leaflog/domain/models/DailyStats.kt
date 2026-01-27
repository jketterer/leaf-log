package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class DailyStats(
    val sessionCount: Int = 0,
    val totalWaterQuantityMl: Int = 0, // in ml
    val differentTeasCount: Int = 0,
    val date: Instant = Instant.DISTANT_FUTURE
) {
    /**
     * Formatted water quantity in user's preferred unit.
     * TODO: Get unit preference from UserPreferences
     */
    fun getFormattedWaterQuantity(useMetric: Boolean = true): String {
        return if (useMetric) {
            "${totalWaterQuantityMl}ml"
        } else {
            val oz = (totalWaterQuantityMl / 29.5735).toInt()
            "${oz}oz"
        }
    }
}