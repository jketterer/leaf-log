package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AnalyticsTest {

    @Test
    fun `this week on a Sunday runs Monday to Monday and includes today`() {
        val window = AnalyticsPeriod.THIS_WEEK.window(at("2026-09-27T20:00"), tz).current

        assertEquals(midnight("2026-09-21"), window.start)
        assertEquals(midnight("2026-09-28"), window.end)
    }

    @Test
    fun `this week on a Monday starts today`() {
        val window = AnalyticsPeriod.THIS_WEEK.window(at("2026-09-28T08:00"), tz).current

        assertEquals(midnight("2026-09-28"), window.start)
        assertEquals(midnight("2026-10-05"), window.end)
    }

    @Test
    fun `this week is compared against the same elapsed part of last week`() {
        val previous = AnalyticsPeriod.THIS_WEEK.window(at("2026-09-23T14:30"), tz).previous!!

        assertEquals(midnight("2026-09-14"), previous.start)
        assertEquals(at("2026-09-16T14:30"), previous.end)
    }

    @Test
    fun `this month is compared against the same elapsed part of last month`() {
        val previous = AnalyticsPeriod.THIS_MONTH.window(at("2026-09-23T14:30"), tz).previous!!

        assertEquals(midnight("2026-08-01"), previous.start)
        assertEquals(at("2026-08-23T14:30"), previous.end)
    }

    @Test
    fun `this year is compared against the same elapsed part of last year`() {
        val previous = AnalyticsPeriod.THIS_YEAR.window(at("2026-09-23T14:30"), tz).previous!!

        assertEquals(midnight("2025-01-01"), previous.start)
        assertEquals(at("2025-09-23T14:30"), previous.end)
    }

    @Test
    fun `this month on its last evening runs to the first of the next`() {
        val window = AnalyticsPeriod.THIS_MONTH.window(at("2026-09-30T23:59"), tz).current

        assertEquals(midnight("2026-09-01"), window.start)
        assertEquals(midnight("2026-10-01"), window.end)
    }

    @Test
    fun `this season on the last evening of August is still summer`() {
        val window = AnalyticsPeriod.THIS_SEASON.window(at("2026-08-31T23:59"), tz).current

        assertEquals(midnight("2026-06-01"), window.start)
        assertEquals(midnight("2026-09-01"), window.end)
    }

    @Test
    fun `this season at midnight on the first of September is autumn`() {
        val window = AnalyticsPeriod.THIS_SEASON.window(at("2026-09-01T00:00"), tz).current

        assertEquals(midnight("2026-09-01"), window.start)
        assertEquals(midnight("2026-12-01"), window.end)
    }

    @Test
    fun `winter runs from December across the new year to March`() {
        for (day in listOf("2026-12-01T00:00", "2027-01-15T12:00", "2027-02-28T23:59")) {
            val window = AnalyticsPeriod.THIS_SEASON.window(at(day), tz).current

            assertEquals(midnight("2026-12-01"), window.start, day)
            assertEquals(midnight("2027-03-01"), window.end, day)
        }
    }

    @Test
    fun `this season is compared against the same elapsed part of the season before`() {
        val previous = AnalyticsPeriod.THIS_SEASON.window(at("2026-10-23T14:30"), tz).previous!!

        assertEquals(midnight("2026-06-01"), previous.start)
        assertEquals(at("2026-07-23T14:30"), previous.end)
    }

    @Test
    fun `this year starts over at midnight on New Year's Day`() {
        val lastEvening = AnalyticsPeriod.THIS_YEAR.window(at("2026-12-31T23:59"), tz).current
        val newYear = AnalyticsPeriod.THIS_YEAR.window(at("2027-01-01T00:00"), tz).current

        assertEquals(midnight("2026-01-01"), lastEvening.start)
        assertEquals(midnight("2027-01-01"), newYear.start)
    }

    @Test
    fun `all time has nothing to compare against`() {
        assertNull(AnalyticsPeriod.ALL_TIME.window(at("2026-09-23T14:30"), tz).previous)
    }

    @Test
    fun `a gongfu session counts once and its water per steep and its leaf once`() {
        val data = listOf(gongfu(), western()).summary(WEEK)

        assertEquals(2, data.totalSessions)
        assertEquals(4 * 100.0 + 300.0, data.totalWaterMl)
        assertEquals(5.0 + 3.0, data.totalTeaGrams)
        assertEquals(8.minutes, data.totalBrewingTime)
    }

    @Test
    fun `the average rating weighs each session once whatever its steep count`() {
        val data = listOf(gongfu(), western()).summary(WEEK)

        // gongfu's steeps average 4, western's is 2
        assertEquals(3f, data.averageRating)
        assertEquals(2, data.ratedSessionsCount)
    }

    @Test
    fun `sessions still brewing are left out`() {
        val data = listOf(gongfu(status = SessionStatus.IN_PROGRESS), western()).summary(WEEK)

        assertEquals(1, data.totalSessions)
        assertEquals(2f, data.averageRating)
    }

    @Test
    fun `a session belongs to the window it started in along with all its steeps`() {
        // Ends after steep 1 started but before steep 2 did
        val window = DateWindow(at(NOON), at(NOON) + 30.seconds, tz)

        val data = listOf(gongfu()).summary(window)

        assertEquals(1, data.totalSessions)
        assertEquals(400.0, data.totalWaterMl)
    }

    @Test
    fun `steeps per session are counted from each session's own steeps`() {
        assertEquals(2.5, listOf(gongfu(), western()).summary(WEEK).averageSteeps)
    }

    @Test
    fun `a tea first brewed before the window is not new in it`() {
        val sessions = listOf(
            session("2026-09-01T10:00", teaId = "old"),
            session("2026-09-22T10:00", teaId = "old"),
            session("2026-09-23T10:00", teaId = "new"),
            session("2026-09-24T10:00", teaId = "new"),
        )

        assertEquals(1, sessions.summary(WEEK).newTeasCount)
    }

    @Test
    fun `a period is compared as the percent change from the one before`() {
        val current = listOf(session(), session(), session()).summary(WEEK)
        val previous = listOf(session(), session()).summary(WEEK)

        assertEquals(50f, current.comparedTo(previous)!!.percentageChangeSessions)
    }

    @Test
    fun `there is no comparison against a period without sessions`() {
        val current = listOf(session()).summary(WEEK)

        assertNull(current.comparedTo(emptyList<SessionWithSteeps>().summary(WEEK)))
    }

    @Test
    fun `a week has a cell per day colored by its most brewed tea type`() {
        val sessions = listOf(
            session("2026-09-22T09:00", teaId = "green"),
            session("2026-09-22T12:00", teaId = "green"),
            session("2026-09-22T15:00", teaId = "black"),
        )

        val cells = sessions.activity(WEEK, byWeek = false, TEAS, TEA_TYPES)

        assertEquals((21..27).map { LocalDate(2026, 9, it) }, cells.map { it.date })
        assertEquals(ActivityCell(LocalDate(2026, 9, 22), "#4CAF50", sessionCount = 3), cells[1])
        assertEquals(3, cells.sumOf { it.sessionCount })
    }

    @Test
    fun `weekly cells start on the window's first day and then on Mondays`() {
        val window = DateWindow(midnight("2026-09-23"), at("2026-10-11T12:00"), tz)

        val cells = listOf(session("2026-09-29T10:00")).activity(window, byWeek = true, TEAS, TEA_TYPES)

        assertEquals(
            listOf(LocalDate(2026, 9, 23), LocalDate(2026, 9, 28), LocalDate(2026, 10, 5)),
            cells.map { it.date },
        )
        assertEquals(listOf(0, 1, 0), cells.map { it.sessionCount })
    }

    @Test
    fun `a month trends by day from its first session`() {
        val window = DateWindow(midnight("2026-09-01"), midnight("2026-10-01"), tz)
        val sessions = listOf(session("2026-09-05T09:00"), session("2026-09-05T15:00"))

        val trends = sessions.trends(window)

        assertEquals(TrendGranularity.DAILY, trends.granularity)
        assertEquals(TrendPoint(LocalDate(2026, 9, 5), 2), trends.points.first())
    }

    @Test
    fun `a year trends by month`() {
        val window = DateWindow(midnight("2026-01-01"), at("2026-09-23T12:00"), tz)
        val sessions = listOf(
            session("2026-02-10T10:00"),
            session("2026-02-20T10:00"),
            session("2026-05-01T10:00"),
        )

        val trends = sessions.trends(window)

        assertEquals(TrendGranularity.MONTHLY, trends.granularity)
        assertEquals(listOf(2, 0, 0, 1), trends.points.take(4).map { it.sessionCount })
        assertEquals(LocalDate(2026, 2, 1), trends.points.first().date)
    }

    @Test
    fun `tea types are shared out by session count`() {
        val sessions = listOf(
            session(teaId = "green"),
            session(teaId = "green"),
            session(teaId = "green"),
            session(teaId = "black"),
        )

        val distribution = sessions.teaTypeDistribution(WEEK, TEAS, TEA_TYPES)

        assertEquals(listOf("green" to 75f, "black" to 25f), distribution.map { it.teaType.id to it.percentage })
    }

    @Test
    fun `vessels are shared out over the sessions that recorded one`() {
        val sessions = listOf(session(vesselId = "gaiwan"), session(vesselId = ""))

        val distribution = sessions.vesselDistribution(WEEK, mapOf("gaiwan" to vessel("gaiwan")))

        assertEquals(100f, distribution.single().percentage)
    }

    @Test
    fun `the top teas are the five most brewed`() {
        val sessions = (1..6).flatMap { n -> List(n) { session(teaId = "tea$n") } }
        val teas = (1..6).associate { "tea$it" to tea("tea$it", "green") }

        val top = sessions.topTeas(WEEK, teas)

        assertEquals(listOf(6, 5, 4, 3, 2), top.map { it.sessionCount })
    }

    @Test
    fun `the top rated teas rank by mean session rating and leave unrated sessions out`() {
        val sessions = listOf(
            session(teaId = "green", ratings = listOf(3f)),
            session(teaId = "green", ratings = listOf(null)),
            session(teaId = "black", ratings = listOf(5f)),
            session(teaId = "black", ratings = listOf(4f)),
        )

        val top = sessions.topRatedTeas(WEEK, TEAS)

        assertEquals(
            listOf(TopRatedTea(TEAS.getValue("black"), 4.5f, 2), TopRatedTea(TEAS.getValue("green"), 3f, 1)),
            top,
        )
    }

    private fun gongfu(status: SessionStatus = SessionStatus.COMPLETED) = session(
        NOON,
        ratings = listOf(3f, 5f, null, 4f),
        status = status,
        brewingTime = 1.minutes,
        waterQuantityMl = 100.0,
        teaQuantityGrams = 5f,
    )

    private fun western() = session(
        NOON,
        ratings = listOf(2f),
        brewingTime = 4.minutes,
        waterQuantityMl = 300.0,
        teaQuantityGrams = 3f,
    )

    private var nextId = 0

    /** Its steeps one brewing time apart. */
    private fun session(
        startedAt: String = NOON,
        teaId: String = "green",
        ratings: List<Float?> = listOf(null),
        status: SessionStatus = SessionStatus.COMPLETED,
        vesselId: String = "gaiwan",
        brewingTime: Duration = 1.minutes,
        waterQuantityMl: Double = 100.0,
        teaQuantityGrams: Float? = null,
    ): SessionWithSteeps {
        val id = "s${nextId++}"
        val start = at(startedAt)
        return SessionWithSteeps(
            session = Session(
                id = id,
                teaId = teaId,
                status = status,
                vesselId = vesselId,
                waterType = WaterType.FILTERED,
                teaQuantityGrams = teaQuantityGrams,
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = ratings.mapIndexed { index, rating ->
                Steep(
                    id = if (index == 0) id else "$id-${index + 1}",
                    number = index + 1,
                    timestamp = start + brewingTime * index,
                    brewingTime = brewingTime,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = waterQuantityMl,
                    rating = rating,
                    createdAt = start,
                    updatedAt = start,
                )
            },
        )
    }

    private companion object {
        val tz = TimeZone.of("America/New_York")

        fun at(dateTime: String) = LocalDateTime.parse(dateTime).toInstant(tz)
        fun midnight(date: String) = LocalDate.parse(date).atStartOfDayIn(tz)

        const val NOON = "2026-09-23T12:00"

        /** Monday 2026-09-21 to the next Monday. */
        val WEEK = DateWindow(midnight("2026-09-21"), midnight("2026-09-28"), tz)

        fun tea(id: String, teaTypeId: String) = Tea(
            id = id,
            name = id,
            teaTypeId = teaTypeId,
            createdAt = at(NOON),
            updatedAt = at(NOON),
            syncStatus = SyncStatus.LOCAL_ONLY,
        )

        fun vessel(id: String) = BrewingVessel(
            id = id,
            name = id,
            iconName = null,
            isSystemDefault = false,
            displayOrder = 0,
            createdAt = at(NOON),
            updatedAt = at(NOON),
        )

        val TEAS = mapOf("green" to tea("green", "green"), "black" to tea("black", "black"))
        val TEA_TYPES = mapOf(
            "green" to TeaType(id = "green", name = "Green", colorHex = "#4CAF50"),
            "black" to TeaType(id = "black", name = "Black", colorHex = "#795548"),
        )
    }
}
