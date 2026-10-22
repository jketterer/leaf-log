package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class Tea(
    val id: String,
    val name: String,
    val teaTypeId: String,

    val origin: String? = null,
    val producer: String? = null,
    val purchaseDate: LocalDate? = null,
    val defaultTemperatureCelsius: Double? = null,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val totalSessions: Int = 0,
    val averageRating: Float? = null,
    val lastBrewedAt: Instant? = null,
    val finishedAt: Instant? = null,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)

/**
 * How long the tea has been on the shelf: from [Tea.purchaseDate] to [today], or to the day it was
 * finished. Null without a purchase date, or when that date is still ahead.
 */
fun Tea.cellarAge(today: LocalDate, tz: TimeZone): DatePeriod? {
    val bought = purchaseDate ?: return null
    val end = finishedAt?.toLocalDateTime(tz)?.date ?: today
    return if (end < bought) null else bought.periodUntil(end)
}
