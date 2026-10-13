package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * One sitting with one tea. Stored as the first steep's row, so [id] is also steep 1's id.
 *
 * [status] means the session: it stays [SessionStatus.IN_PROGRESS] across steeps until Finish.
 */
data class Session(
    val id: String,
    val teaId: String,
    val status: SessionStatus,
    val vesselId: String,
    val waterType: WaterType,
    val teaQuantityGrams: Float? = null,
    val location: String? = null,
    val usedConfigurationId: String? = null,

    val userId: String? = null,
    val syncStatus: SyncStatus,
)

data class Steep(
    val id: String,
    val number: Int,
    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Double,
    val waterQuantityMl: Double,
    val rating: Float? = null,
    val notes: String? = null,
    val photos: List<String> = emptyList(),

    val createdAt: Instant,
    val updatedAt: Instant,

    // Timer state persistence fields (null = timer not started)
    val timerStatus: TimerStatus? = null,
    val timerStartedAt: Instant? = null,
    val timerPausedAt: Instant? = null,
    val timerRemainingMs: Long? = null,
    val timerTotalMs: Long? = null,
)

/** [steeps] are ordered by number and never empty: steep 1 is part of the session row. */
data class SessionWithSteeps(
    val session: Session,
    val steeps: List<Steep>,
) {
    /** A session belongs to the time it started: steep 1's timestamp. */
    val startedAt: Instant get() = steeps.first().timestamp

    /** The steep being brewed or reviewed while the session is in progress. */
    val currentSteep: Steep get() = steeps.last()

    /** Mean of the rated steeps, unrounded. Unrated steeps don't count; no ratings is null. */
    val effectiveRating: Float?
        get() = steeps.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
}

/** What a tea or a brewing configuration stores about the sessions that used it. */
data class SessionStats(
    val count: Int,
    val averageRating: Float?,
    val lastAt: Instant?,
)

/**
 * Stats over completed sessions only. Each session counts once, unrated ones are left out of
 * the average, and a session belongs to the time it started.
 */
fun List<SessionWithSteeps>.stats(): SessionStats {
    val completed = filter { it.session.status == SessionStatus.COMPLETED }
    return SessionStats(
        count = completed.size,
        averageRating = completed.mapNotNull { it.effectiveRating }
            .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        lastAt = completed.maxOfOrNull { it.startedAt },
    )
}
