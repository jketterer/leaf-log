package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

data class TeaSession(
    val id: String,
    val teaId: String,

    val parentSessionId: String?,
    val steepNumber: Int,
    val status: SessionStatus,

    val teaQuantityGrams: Float?,
    val vesselId: String,
    val waterType: WaterType,
    val location: String?,
    val rating: Float?,

    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Int,
    val waterQuantityMl: Int,
    val notes: String?,
    val photos: List<String>,

    val userId: String?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)