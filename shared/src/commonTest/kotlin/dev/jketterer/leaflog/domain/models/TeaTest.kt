package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TeaTest {

    private val tz = TimeZone.of("America/New_York")

    private fun tea(purchaseDate: LocalDate?, finishedAt: Instant? = null) = Tea(
        id = "tea",
        name = "Tea",
        teaTypeId = "green",
        purchaseDate = purchaseDate,
        finishedAt = finishedAt,
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
}
