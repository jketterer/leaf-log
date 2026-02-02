package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class DailyStats(
    val sessionCount: Int = 0,
    val formattedWaterQuantity: String = "",
    val differentTeasCount: Int = 0,
    val date: Instant = Instant.DISTANT_FUTURE,
)