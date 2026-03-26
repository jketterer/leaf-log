package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Tea(
    val id: String,
    val name: String,
    val teaTypeId: String,

    val origin: String? = null,
    val producer: String? = null,
    val purchaseDate: LocalDate? = null,
    val purchasePrice: Double? = null,
    val stockAmount: Int? = null,
    val defaultTemperatureCelsius: Double? = null,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val totalSessions: Int = 0,
    val averageRating: Float? = null,
    val lastBrewedAt: Instant? = null,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)