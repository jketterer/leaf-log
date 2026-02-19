package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

data class TeaSession(
    val id: String,
    val teaId: String,

    val parentSessionId: String? = null,
    val steepNumber: Int = 1,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,

    val teaQuantityGrams: Float? = null,
    val vesselId: String,
    val waterType: WaterType,
    val location: String? = null,
    val rating: Float? = null,
    val averageRating: Float? = null, // Average rating across all steeps (parent sessions only)
    val usedConfigurationId: String? = null, // Configuration that was used for this session

    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Double,
    val waterQuantityMl: Double,
    val notes: String? = null,
    val photos: List<String> = emptyList(),

    val userId: String? = null,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,

    // Timer state persistence fields (null = timer not started)
    val timerStatus: TimerStatus? = null,
    val timerStartedAt: Instant? = null,
    val timerPausedAt: Instant? = null,
    val timerRemainingMs: Long? = null,
)