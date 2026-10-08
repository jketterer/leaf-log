package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeDayTest {

    private val tz = TimeZone.of("America/New_York")

    private fun at(iso: String) = LocalDateTime.parse(iso).toInstant(tz)

    private fun relativeDay(then: String, now: String) = relativeDayOf(at(then), at(now), tz)

    @Test
    fun `earlier the same day is today`() {
        assertEquals(RelativeDay.TODAY, relativeDay("2026-09-23T00:01", now = "2026-09-23T23:59"))
    }

    @Test
    fun `the same day of an earlier month is not today`() {
        assertEquals(RelativeDay.THIS_YEAR, relativeDay("2026-08-23T09:00", now = "2026-09-23T09:00"))
    }

    @Test
    fun `late last night is yesterday`() {
        assertEquals(RelativeDay.YESTERDAY, relativeDay("2026-09-22T23:59", now = "2026-09-23T00:01"))
    }

    @Test
    fun `two days ago is this year rather than yesterday`() {
        assertEquals(RelativeDay.THIS_YEAR, relativeDay("2026-09-21T12:00", now = "2026-09-23T12:00"))
    }

    @Test
    fun `new year's eve is yesterday on new year's day`() {
        assertEquals(RelativeDay.YESTERDAY, relativeDay("2025-12-31T22:00", now = "2026-01-01T08:00"))
    }

    @Test
    fun `earlier last year is older`() {
        assertEquals(RelativeDay.OLDER, relativeDay("2025-12-30T22:00", now = "2026-01-01T08:00"))
    }

    @Test
    fun `days are decided in the given time zone rather than UTC`() {
        // 01:00 UTC on the 24th is still the evening of the 23rd in New York
        val then = LocalDateTime.parse("2026-09-24T01:00").toInstant(TimeZone.UTC)
        assertEquals(RelativeDay.TODAY, relativeDayOf(then, at("2026-09-23T08:00"), tz))
    }
}
