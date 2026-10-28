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

    @Test
    fun `the timeline gives steep 1 every parameter`() {
        val first = aggregate(null).timeline().single()

        assertEquals(30.seconds, first.brewedTime)
        assertEquals(95.0, first.temperatureCelsius)
        assertEquals(100.0, first.waterQuantityMl)
        assertNull(first.breakBefore)
    }

    @Test
    fun `the timeline gives a later steep only what changed from the steep before`() {
        val session = aggregate(null, null, null).editSteeps { index, steep ->
            when (index) {
                1 -> steep.copy(brewingTime = 45.seconds)
                2 -> steep.copy(brewingTime = 45.seconds, temperatureCelsius = 90.0)
                else -> steep
            }
        }

        val (_, second, third) = session.timeline()
        assertEquals(TimelineSteep(session.steeps[1], 45.seconds, null, null, null), second)
        assertEquals(TimelineSteep(session.steeps[2], null, 90.0, null, null), third)
    }

    @Test
    fun `the timeline compares how long a steep actually brewed`() {
        val session = aggregate(null, null).editSteeps { index, steep ->
            if (index == 1) steep.copy(brewingTime = 45.seconds, actualTime = 30.seconds) else steep
        }

        assertNull(session.timeline()[1].brewedTime)
    }

    @Test
    fun `a rest of more than 2 minutes beyond the brew before is a break`() {
        val session = aggregate(null, null, steepGap = 30.seconds + 2.minutes + 1.seconds)

        assertEquals(2.minutes + 1.seconds, session.timeline()[1].breakBefore)
    }

    @Test
    fun `a rest of 2 minutes beyond the brew before is no break`() {
        val session = aggregate(null, null, steepGap = 30.seconds + 2.minutes)

        assertNull(session.timeline()[1].breakBefore)
    }

    @Test
    fun `total brew time adds how long each steep brewed`() {
        val session = aggregate(null, null).editSteeps { index, steep ->
            if (index == 1) steep.copy(actualTime = 50.seconds) else steep
        }

        assertEquals(80.seconds, session.totalBrewTime)
    }

    @Test
    fun `the best brew is the highest-rated completed session`() {
        val best = aggregate(4f)
        val sessions = listOf(aggregate(3f), best, aggregate(null), aggregate(5f, status = SessionStatus.IN_PROGRESS))

        assertEquals(best, sessions.best())
    }

    @Test
    fun `a pair's default brew is its last completed session`() {
        val last = aggregate(3f, startedAt = at(10))
        val sessions = listOf(
            aggregate(5f, startedAt = at(0)),
            last,
            aggregate(4f, status = SessionStatus.IN_PROGRESS, startedAt = at(20)),
        )

        assertEquals(last, sessions.defaultBrew())
    }

    @Test
    fun `a last brew rated under 3 stars gives way to the best`() {
        val best = aggregate(4f, startedAt = at(0))

        assertEquals(best, listOf(best, aggregate(2.5f, startedAt = at(10))).defaultBrew())
    }

    @Test
    fun `an unrated last brew stays the default`() {
        val last = aggregate(null, startedAt = at(10))

        assertEquals(last, listOf(aggregate(5f, startedAt = at(0)), last).defaultBrew())
    }

    @Test
    fun `with no rated brew the default is the last`() {
        val last = aggregate(null, startedAt = at(10))

        assertEquals(last, listOf(aggregate(null, startedAt = at(0)), last).defaultBrew())
    }

    @Test
    fun `the steep curve is the mean rating at each steep`() {
        val curve = listOf(aggregate(3f, 4f, 5f), aggregate(4f, 5f, 4f)).steepCurve()!!

        assertEquals(mapOf(1 to 3.5f, 2 to 4.5f, 3 to 4.5f), curve.meanRatings)
        assertEquals(2, curve.peak)
    }

    @Test
    fun `a steep shows on the curve only when 2 sessions rated it`() {
        val curve = listOf(aggregate(3f, 4f, 5f), aggregate(4f, 5f, null), aggregate(null, null)).steepCurve()!!

        assertEquals(listOf(1, 2), curve.meanRatings.keys.toList())
    }

    @Test
    fun `there is no curve until 2 sessions rated a steep past 1`() {
        assertNull(listOf(aggregate(4f, 5f), aggregate(4f, null), aggregate(5f)).steepCurve())
    }

    @Test
    fun `in-progress sessions are left out of the curve`() {
        assertNull(listOf(aggregate(4f, 5f), aggregate(4f, 5f, status = SessionStatus.IN_PROGRESS)).steepCurve())
    }
}

private fun SessionWithSteeps.editSteeps(edit: (Int, Steep) -> Steep) =
    copy(steeps = steeps.mapIndexed(edit))

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
