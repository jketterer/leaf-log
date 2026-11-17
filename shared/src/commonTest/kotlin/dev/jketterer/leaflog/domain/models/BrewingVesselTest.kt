package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BrewingVesselTest {

    @Test
    fun `a vessel with no completed session has no profile`() {
        assertNull(listOf(session(SHENG_A, status = SessionStatus.IN_PROGRESS)).vesselProfile(TEAS, TYPES))
    }

    @Test
    fun `only completed sessions count`() {
        val profile = listOf(
            session(SHENG_A, day = 1),
            session(SHOU, day = 2, status = SessionStatus.IN_PROGRESS),
        ).vesselProfile(TEAS, TYPES)!!

        assertEquals(1, profile.sessions)
        assertEquals(listOf("sheng"), profile.types.map { it.teaType.id })
        assertEquals(listOf(SHENG_A), profile.teas.map { it.tea.id })
        assertEquals(at(1), profile.lastBrewed)
    }

    @Test
    fun `three sessions all of one type make it the only type`() {
        val profile = List(3) { session(SHENG_A, day = it) }.vesselProfile(TEAS, TYPES)!!

        assertEquals("sheng", profile.dominantType?.id)
        assertTrue(profile.onlyType)
    }

    @Test
    fun `two sessions are too few for a dominant type`() {
        val profile = List(2) { session(SHENG_A, day = it) }.vesselProfile(TEAS, TYPES)!!

        assertNull(profile.dominantType)
    }

    @Test
    fun `a type in exactly 75 percent of sessions is dominant`() {
        val profile = (List(3) { session(SHENG_A, day = it) } + session(SHOU, day = 3))
            .vesselProfile(TEAS, TYPES)!!

        assertEquals("sheng", profile.dominantType?.id)
        assertFalse(profile.onlyType)
    }

    @Test
    fun `a type under 75 percent of sessions is not dominant`() {
        val profile = (List(2) { session(SHENG_A, day = it) } + session(SHOU, day = 2))
            .vesselProfile(TEAS, TYPES)!!

        assertNull(profile.dominantType)
    }

    @Test
    fun `types rank by session count across their teas`() {
        val profile = listOf(
            session(SHOU, day = 1),
            session(SHENG_A, day = 2),
            session(SHENG_B, day = 3),
        ).vesselProfile(TEAS, TYPES)!!

        assertEquals(listOf("sheng" to 2, "shou" to 1), profile.types.map { it.teaType.id to it.sessionCount })
    }

    @Test
    fun `teas are ordered by the last time each was brewed in it`() {
        val profile = listOf(
            session(SHENG_A, day = 1),
            session(SHOU, day = 2),
            session(SHENG_A, day = 3),
        ).vesselProfile(TEAS, TYPES)!!

        assertEquals(
            listOf(
                VesselTea(TEAS.getValue(SHENG_A), TYPES.getValue("sheng"), 2, at(3)),
                VesselTea(TEAS.getValue(SHOU), TYPES.getValue("shou"), 1, at(2)),
            ),
            profile.teas,
        )
    }

    private var nextId = 0

    private fun session(
        teaId: String,
        day: Int = 0,
        status: SessionStatus = SessionStatus.COMPLETED,
    ): SessionWithSteeps {
        val id = "s${nextId++}"
        return SessionWithSteeps(
            session = Session(
                id = id,
                teaId = teaId,
                status = status,
                vesselId = "pot",
                waterType = WaterType.FILTERED,
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = listOf(
                Steep(
                    id = id,
                    number = 1,
                    timestamp = at(day),
                    brewingTime = 1.minutes,
                    temperatureCelsius = 95.0,
                    waterQuantityMl = 100.0,
                    createdAt = EPOCH,
                    updatedAt = EPOCH,
                ),
            ),
        )
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        fun at(day: Int) = EPOCH + day.days

        const val SHENG_A = "yiwu"
        const val SHENG_B = "bulang"
        const val SHOU = "menghai"

        val TYPES = listOf(TeaType(id = "sheng", name = "Sheng"), TeaType(id = "shou", name = "Shou"))
            .associateBy { it.id }

        val TEAS = listOf(SHENG_A to "sheng", SHENG_B to "sheng", SHOU to "shou")
            .associate { (id, type) ->
                id to Tea(id = id, name = id, teaTypeId = type, createdAt = EPOCH, updatedAt = EPOCH)
            }
    }
}
