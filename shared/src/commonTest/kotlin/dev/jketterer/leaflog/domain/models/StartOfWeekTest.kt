package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StartOfWeekTest {

    @Test
    fun `a Monday starts its own week`() {
        assertEquals(LocalDate(2026, 9, 21), LocalDate(2026, 9, 21).startOfWeek())
    }

    @Test
    fun `a Sunday belongs to the week that started the Monday before`() {
        assertEquals(LocalDate(2026, 9, 21), LocalDate(2026, 9, 27).startOfWeek())
    }

    @Test
    fun `a week can start in the previous month`() {
        assertEquals(LocalDate(2026, 9, 28), LocalDate(2026, 10, 1).startOfWeek())
    }

    @Test
    fun `a week can start in the previous year`() {
        assertEquals(LocalDate(2025, 12, 29), LocalDate(2026, 1, 1).startOfWeek())
    }
}
