package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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

        assertEquals(
            HistoryFilters(
                teaTypeIds = setOf("oolong"),
                teaId = "tie-guan-yin",
                dateRangeStart = LocalDate(2026, 10, 1),
                dateRangeEnd = LocalDate(2026, 10, 18),
            ),
            state.filters,
        )
    }

    @Test
    fun `an open-ended date range keeps its open end`() {
        val state = initialHistoryState(NavRoute.HistoryRoute(filterDateStart = "2026-10-01"))

        assertEquals(LocalDate(2026, 10, 1), state.filters.dateRangeStart)
        assertEquals(null, state.filters.dateRangeEnd)
    }

    @Test
    fun `several tea types or vessels match a session in any of them`() {
        val teas = listOf(
            tea("sencha", type = "green"),
            tea("da-hong-pao", type = "oolong"),
            tea("assam", type = "black"),
        ).associateBy { it.id }
        val sessions = listOf(
            session("1", teaId = "sencha", vesselId = "kyusu"),
            session("2", teaId = "da-hong-pao", vesselId = "gaiwan"),
            session("3", teaId = "assam", vesselId = "gaiwan"),
            session("4", teaId = "sencha", vesselId = "mug"),
        )

        val byType = sessions.filteredBy(HistoryFilters(teaTypeIds = setOf("green", "oolong")), "", teas)
        val byVessel = sessions.filteredBy(HistoryFilters(vesselIds = setOf("kyusu", "mug")), "", teas)
        val byBoth = sessions.filteredBy(
            HistoryFilters(teaTypeIds = setOf("green", "oolong"), vesselIds = setOf("gaiwan", "mug")),
            "",
            teas,
        )

        assertEquals(listOf("1", "2", "4"), byType.map { it.session.id })
        assertEquals(listOf("1", "4"), byVessel.map { it.session.id })
        assertEquals(listOf("2", "4"), byBoth.map { it.session.id })
    }

    private fun tea(id: String, type: String) = Tea(
        id = id,
        name = id,
        teaTypeId = type,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    private fun session(id: String, teaId: String, vesselId: String) = SessionWithSteeps(
        session = Session(
            id = id,
            teaId = teaId,
            status = SessionStatus.COMPLETED,
            vesselId = vesselId,
            waterType = WaterType.FILTERED,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = listOf(
            Steep(
                id = id,
                number = 1,
                timestamp = Instant.fromEpochSeconds(0),
                brewingTime = 20.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 100.0,
                createdAt = Instant.fromEpochSeconds(0),
                updatedAt = Instant.fromEpochSeconds(0),
            ),
        ),
    )
}
