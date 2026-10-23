package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class SeasonTest {

    @Test
    fun `a recap waits for its season to end`() {
        val summer = List(10) { session("2026-08-${10 + it}T10:00") }

        assertEquals(emptyList(), summer.seasonRecaps(at("2026-08-31T23:59"), tz))
        assertEquals(listOf(SUMMER), summer.seasonRecaps(at("2026-09-01T00:00"), tz))
    }

    @Test
    fun `a season needs 10 completed sessions for a recap`() {
        val nine = List(9) { session("2026-07-${10 + it}T10:00") } +
            session("2026-07-30T10:00", SessionStatus.IN_PROGRESS)

        assertEquals(emptyList(), nine.seasonRecaps(NOW, tz))
        assertEquals(listOf(SUMMER), (nine + session("2026-07-31T10:00")).seasonRecaps(NOW, tz))
    }

    @Test
    fun `recaps run newest first and winter gathers sessions across the new year`() {
        val winter = List(5) { session("2025-12-${10 + it}T10:00") } +
            List(5) { session("2026-02-${10 + it}T10:00") }
        val summer = List(10) { session("2026-06-${10 + it}T10:00") }

        assertEquals(
            listOf(SUMMER, Season(LocalDate(2025, 12, 1))),
            (winter + summer).seasonRecaps(NOW, tz),
        )
    }

    @Test
    fun `the season from December is winter in the north and summer in the south`() {
        val december = Season(LocalDate(2025, 12, 1))

        assertEquals(SeasonName.WINTER, december.name(Hemisphere.NORTHERN))
        assertEquals(SeasonName.SUMMER, december.name(Hemisphere.SOUTHERN))
        assertEquals(SeasonName.SPRING, Season(LocalDate(2026, 9, 1)).name(Hemisphere.SOUTHERN))
    }

    @Test
    fun `a season starts on the first of December March June or September`() {
        assertEquals(LocalDate(2025, 12, 1), LocalDate(2026, 2, 28).startOfSeason())
        assertEquals(LocalDate(2026, 3, 1), LocalDate(2026, 3, 1).startOfSeason())
        assertEquals(LocalDate(2026, 6, 1), LocalDate(2026, 8, 31).startOfSeason())
    }

    private var nextId = 0

    private fun session(
        startedAt: String,
        status: SessionStatus = SessionStatus.COMPLETED,
    ): SessionWithSteeps {
        val id = "s${nextId++}"
        val start = at(startedAt)
        return SessionWithSteeps(
            session = Session(
                id = id,
                teaId = "green",
                status = status,
                vesselId = "gaiwan",
                waterType = WaterType.FILTERED,
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = listOf(
                Steep(
                    id = id,
                    number = 1,
                    timestamp = start,
                    brewingTime = 1.minutes,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 100.0,
                    createdAt = start,
                    updatedAt = start,
                ),
            ),
        )
    }

    private companion object {
        val tz = TimeZone.of("America/New_York")

        fun at(dateTime: String) = LocalDateTime.parse(dateTime).toInstant(tz)

        val NOW = at("2026-10-23T12:00")
        val SUMMER = Season(LocalDate(2026, 6, 1))
    }
}
