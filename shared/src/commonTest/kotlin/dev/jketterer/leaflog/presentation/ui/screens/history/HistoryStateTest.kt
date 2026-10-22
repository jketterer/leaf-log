package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryStateTest {

    private val tz = TimeZone.of("America/New_York")
    private val today = LocalDate(2026, 10, 22)

    private fun groups(vararg sessions: SessionWithSteeps) =
        sessions.toList().groupByDay(today, tz)
            .map { group -> group.period to group.sessions.map { it.session.id } }

    @Test
    fun `a session past midnight belongs to the day it started`() {
        assertEquals(
            listOf(
                HistoryPeriod.Day(LocalDate(2026, 10, 22)) to listOf("morning"),
                HistoryPeriod.Day(LocalDate(2026, 10, 21)) to listOf("late", "evening"),
            ),
            groups(
                session("morning", "2026-10-22T08:00"),
                session("late", "2026-10-21T23:50", steeps = 6),
                session("evening", "2026-10-21T19:00"),
            ),
        )
    }

    @Test
    fun `the last 30 days counting today are headed by day and the day before by its month`() {
        assertEquals(
            listOf(
                HistoryPeriod.Day(LocalDate(2026, 9, 23)) to listOf("thirtieth"),
                HistoryPeriod.Month(LocalDate(2026, 9, 1)) to listOf("thirty-first"),
            ),
            groups(
                session("thirtieth", "2026-09-23T00:00"),
                session("thirty-first", "2026-09-22T23:59"),
            ),
        )
    }

    @Test
    fun `older sessions group by month and a year apart kept apart`() {
        assertEquals(
            listOf(
                HistoryPeriod.Month(LocalDate(2026, 8, 1)) to listOf("aug-30", "aug-2"),
                HistoryPeriod.Month(LocalDate(2026, 7, 1)) to listOf("jul"),
                HistoryPeriod.Month(LocalDate(2025, 8, 1)) to listOf("last-aug"),
            ),
            groups(
                session("aug-30", "2026-08-30T10:00"),
                session("aug-2", "2026-08-02T10:00"),
                session("jul", "2026-07-31T23:00"),
                session("last-aug", "2025-08-15T10:00"),
            ),
        )
    }

    private fun session(id: String, startedAt: String, steeps: Int = 1): SessionWithSteeps {
        val start = LocalDateTime.parse(startedAt).toInstant(tz)
        return SessionWithSteeps(
            session = Session(
                id = id,
                teaId = "tea",
                status = SessionStatus.COMPLETED,
                vesselId = "gaiwan",
                waterType = WaterType.FILTERED,
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = List(steeps) { index ->
                Steep(
                    id = "$id-$index",
                    number = index + 1,
                    timestamp = start + 5.minutes * index,
                    brewingTime = 20.seconds,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 100.0,
                    createdAt = start,
                    updatedAt = start,
                )
            },
        )
    }
}
