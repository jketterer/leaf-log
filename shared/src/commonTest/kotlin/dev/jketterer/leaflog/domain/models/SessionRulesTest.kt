package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionRulesTest {

    @Test
    fun `a session's rating is the unrounded mean of its rated steeps`() {
        assertEquals(13f / 3, aggregate(4f, 4f, 5f).effectiveRating!!, 0.0001f)
    }

    @Test
    fun `unrated steeps are left out of the session rating`() {
        assertEquals(4.5f, aggregate(4f, null, 5f).effectiveRating)
    }

    @Test
    fun `a session with no rated steeps has no rating`() {
        assertNull(aggregate(null, null).effectiveRating)
    }

    @Test
    fun `stats count each completed session once whatever its steep count`() {
        val stats = listOf(aggregate(5f, 5f, 5f, 5f), aggregate(3f)).stats()

        assertEquals(2, stats.count)
        assertEquals(4f, stats.averageRating)
    }

    @Test
    fun `in-progress sessions are left out of the stats`() {
        val stats = listOf(
            aggregate(2f, startedAt = at(0)),
            aggregate(5f, status = SessionStatus.IN_PROGRESS, startedAt = at(10)),
        ).stats()

        assertEquals(SessionStats(count = 1, averageRating = 2f, lastAt = at(0)), stats)
    }

    @Test
    fun `unrated sessions count but don't pull the average`() {
        val stats = listOf(aggregate(4f), aggregate(null)).stats()

        assertEquals(2, stats.count)
        assertEquals(4f, stats.averageRating)
    }

    @Test
    fun `the last session is the one that started last rather than the one with the latest steep`() {
        val longSession = aggregate(null, null, null, startedAt = at(0), steepGap = 30.minutes)
        val laterShortSession = aggregate(null, startedAt = at(10))

        assertEquals(at(10), listOf(longSession, laterShortSession).stats().lastAt)
    }

    @Test
    fun `no completed sessions means empty stats`() {
        assertEquals(
            SessionStats(count = 0, averageRating = null, lastAt = null),
            emptyList<SessionWithSteeps>().stats(),
        )
    }
}

private fun at(minute: Int) = Instant.fromEpochSeconds(1_790_000_000L + minute * 60L)

private var nextId = 0

private fun aggregate(
    vararg ratings: Float?,
    status: SessionStatus = SessionStatus.COMPLETED,
    startedAt: Instant = at(0),
    steepGap: Duration = 1.minutes,
): SessionWithSteeps {
    val id = "s${nextId++}"
    return SessionWithSteeps(
        session = Session(
            id = id,
            teaId = "tea",
            status = status,
            vesselId = "vessel",
            waterType = WaterType.FILTERED,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = ratings.mapIndexed { index, rating ->
            Steep(
                id = if (index == 0) id else "$id-${index + 1}",
                number = index + 1,
                timestamp = startedAt + steepGap * index,
                brewingTime = 30.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 100.0,
                rating = rating,
                createdAt = startedAt,
                updatedAt = startedAt,
            )
        },
    )
}
