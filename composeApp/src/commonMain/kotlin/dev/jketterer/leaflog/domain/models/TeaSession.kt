package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

data class TeaSession(
    val id: String,
    val teaId: String,

    val parentSessionId: String? = null,
    val steepNumber: Int,
    val status: SessionStatus,

    val teaQuantityGrams: Float? = null,
    val vesselId: String,
    val waterType: WaterType,
    val location: String? = null,
    val rating: Float? = null,

    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Int,
    val waterQuantityMl: Int,
    val notes: String? = null,
    val photos: List<String>,

    val userId: String? = null,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)