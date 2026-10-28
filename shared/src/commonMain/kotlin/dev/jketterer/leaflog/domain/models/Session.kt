package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
    val rinsed: Boolean = false,
    /** The session Brew Again copied; its steeps are this one's plan when no method is used. */
    val planSessionId: String? = null,
    /** The note on the session as a whole; each steep has its own. */
    val notes: String? = null,

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
    val tags: List<SteepTag> = emptyList(),

    val createdAt: Instant,
    val updatedAt: Instant,

    // Timer state persistence fields (null = timer not started)
    val timerStatus: TimerStatus? = null,
    val timerStartedAt: Instant? = null,
    val timerPausedAt: Instant? = null,
    val timerRemainingMs: Long? = null,
    val timerTotalMs: Long? = null,

    // How long it brewed once the timer completed: adjustments in, pauses out. Null without a timer
    val actualTime: Duration? = null,
) {
    /** What to repeat to brew this steep again: how long it actually brewed, if known. */
    val brewedTime: Duration get() = actualTime ?: brewingTime

    /** Corrects how long this steep brewed, in whichever field [brewedTime] reads. */
    fun withBrewedTime(time: Duration): Steep =
        if (actualTime != null) copy(actualTime = time) else copy(brewingTime = time)

    /**
     * The timer status as of [now], rather than as of the last time it was written.
     *
     * [timerStatus] is only persisted at start, pause, adjust and completion, so a steep whose
     * brew ran out while the app was closed is still stored as RUNNING. Every in-progress
     * indicator should resolve through here so the label, the button and the screen it opens all
     * agree on whether the brew is finished.
     */
    fun resolvedTimerStatus(now: Instant): TimerStatus? {
        if (timerStatus != TimerStatus.RUNNING) return timerStatus

        val startedAt = timerStartedAt ?: return timerStatus
        val totalMs = timerTotalMs ?: brewingTime.inWholeMilliseconds
        val elapsedMs = (now - startedAt).inWholeMilliseconds

        return if (elapsedMs >= totalMs) TimerStatus.COMPLETE else TimerStatus.RUNNING
    }
}

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

    /** How long the leaves sat in water, every steep together. */
    val totalBrewTime: Duration
        get() = steeps.fold(Duration.ZERO) { total, steep -> total + steep.brewedTime }
}

/**
 * A steep as the timeline reads it: each parameter only when it changed from the steep before, so
 * steep 1 has all three, and the rest before it when that ran long enough to be a break.
 */
data class TimelineSteep(
    val steep: Steep,
    val brewedTime: Duration?,
    val temperatureCelsius: Double?,
    val waterQuantityMl: Double?,
    val breakBefore: Duration?,
)

/** A rest between steeps longer than this, beyond the brew before it, reads as a break. */
private val BREAK_AFTER = 2.minutes

fun SessionWithSteeps.timeline(): List<TimelineSteep> = steeps.mapIndexed { index, steep ->
    val previous = steeps.getOrNull(index - 1)
    TimelineSteep(
        steep = steep,
        brewedTime = steep.brewedTime.takeIf { it != previous?.brewedTime },
        temperatureCelsius = steep.temperatureCelsius.takeIf { it != previous?.temperatureCelsius },
        waterQuantityMl = steep.waterQuantityMl.takeIf { it != previous?.waterQuantityMl },
        breakBefore = previous?.let { steep.timestamp - it.timestamp - it.brewedTime }
            ?.takeIf { it > BREAK_AFTER },
    )
}

/** The most recent completed session, or null when none is: "last time" for a tea. */
fun List<SessionWithSteeps>.lastCompleted(): SessionWithSteeps? =
    filter { it.session.status == SessionStatus.COMPLETED }.maxByOrNull { it.startedAt }

/** The highest-rated completed session, the most recent on a tie; null when none is rated. */
fun List<SessionWithSteeps>.best(): SessionWithSteeps? =
    filter { it.session.status == SessionStatus.COMPLETED && it.effectiveRating != null }
        .maxWithOrNull(compareBy({ it.effectiveRating }, { it.startedAt }))

/** A rated brew under this isn't one to repeat by default. */
private const val REPEATABLE_RATING = 3f

/**
 * What brewing a pair, one tea in one vessel, again replays from its sessions: the last completed
 * one, or the [best] when the last is rated under 3★. Null when none is completed.
 */
fun List<SessionWithSteeps>.defaultBrew(): SessionWithSteeps? {
    val last = lastCompleted() ?: return null
    val rating = last.effectiveRating ?: return last
    return if (rating < REPEATABLE_RATING) best() else last
}

/** A tea's mean rating by steep number, and the steep where it peaks. */
data class SteepCurve(
    val meanRatings: Map<Int, Float>,
    val peak: Int,
)

/**
 * Mean rating at each steep number two or more completed sessions rated, in steep order. Null
 * unless one is past steep 1, so a tea only ever brewed western has none. A tie peaks earlier.
 */
fun List<SessionWithSteeps>.steepCurve(): SteepCurve? {
    val means = filter { it.session.status == SessionStatus.COMPLETED }
        .flatMap { it.steeps }
        .mapNotNull { steep -> steep.rating?.let { steep.number to it } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size >= 2 }
        .mapValues { it.value.average().toFloat() }
        .entries.sortedBy { it.key }
        .associate { it.key to it.value }
    if (means.keys.none { it > 1 }) return null
    return SteepCurve(means, peak = means.maxBy { it.value }.key)
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
