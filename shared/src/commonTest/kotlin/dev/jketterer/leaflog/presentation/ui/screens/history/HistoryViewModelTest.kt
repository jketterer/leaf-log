package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryViewModelTest {

    @Test
    fun `a route with no filters opens unfiltered`() {
        assertEquals(HistoryState(), initialHistoryState(NavRoute.HistoryRoute()))
    }

    @Test
    fun `a route's filters are the state it opens with`() {
        val state = initialHistoryState(
            NavRoute.HistoryRoute(
                filterTeaTypeId = "oolong",
                filterTeaId = "tie-guan-yin",
                filterDateStart = "2026-10-01",
                filterDateEnd = "2026-10-18",
            ),
        )

        assertEquals("oolong", state.selectedTeaTypeId)
        assertEquals("tie-guan-yin", state.selectedTeaId)
        assertEquals(LocalDate(2026, 10, 1), state.dateRangeStart)
        assertEquals(LocalDate(2026, 10, 18), state.dateRangeEnd)
    }

    @Test
    fun `an open-ended date range keeps its open end`() {
        val state = initialHistoryState(NavRoute.HistoryRoute(filterDateStart = "2026-10-01"))

        assertEquals(LocalDate(2026, 10, 1), state.dateRangeStart)
        assertEquals(null, state.dateRangeEnd)
    }
}
