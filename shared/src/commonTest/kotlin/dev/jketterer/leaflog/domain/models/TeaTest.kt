package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TeaTest {

    private val tz = TimeZone.of("America/New_York")

    private fun tea(
        purchaseDate: LocalDate? = null,
        finishedAt: Instant? = null,
        lastBrewedAt: Instant? = null,
    ) = Tea(
        id = "tea",
        name = "Tea",
        teaTypeId = "green",
        purchaseDate = purchaseDate,
        finishedAt = finishedAt,
        lastBrewedAt = lastBrewedAt,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    @Test
    fun `cellar age runs from the purchase date to today`() {
        val age = tea(LocalDate(2024, 3, 10)).cellarAge(today = LocalDate(2026, 5, 25), tz)
        assertEquals(DatePeriod(years = 2, months = 2, days = 15), age)
    }

    @Test
    fun `a finished tea stops aging on the day it was finished in local time`() {
        // 01:00 UTC on Jun 1 is still May 31 in New York
        val finished = LocalDateTime.parse("2025-05-31T21:00").toInstant(tz)
        val age = tea(LocalDate(2025, 1, 31), finished).cellarAge(today = LocalDate(2026, 10, 22), tz)
        assertEquals(DatePeriod(months = 4), age)
    }

    @Test
    fun `a tea bought today is zero days old`() {
        val today = LocalDate(2026, 10, 22)
        assertEquals(DatePeriod(), tea(today).cellarAge(today, tz))
    }

    @Test
    fun `no purchase date has no cellar age`() {
        assertNull(tea(purchaseDate = null).cellarAge(LocalDate(2026, 10, 22), tz))
    }

    @Test
    fun `a purchase date still ahead has no cellar age`() {
        assertNull(tea(LocalDate(2026, 11, 1)).cellarAge(LocalDate(2026, 10, 22), tz))
    }

    private val now = Instant.parse("2026-10-23T12:00:00Z")

    @Test
    fun `a tea last brewed 60 days ago is dormant`() {
        assertTrue(tea(lastBrewedAt = now - 60.days).isDormant(now))
    }

    @Test
    fun `a tea brewed just inside 60 days is not dormant`() {
        assertFalse(tea(lastBrewedAt = now - 60.days + 1.seconds).isDormant(now))
    }

    @Test
    fun `a finished tea is never dormant`() {
        assertFalse(tea(lastBrewedAt = now - 200.days, finishedAt = now - 100.days).isDormant(now))
    }

    @Test
    fun `a tea never brewed is not dormant`() {
        assertFalse(tea(lastBrewedAt = null).isDormant(now))
    }
}
