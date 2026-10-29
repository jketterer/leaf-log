package dev.jketterer.leaflog.presentation.ui.screens.home

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class HomeStateTest {

    @Test
    fun `each pair gets one card showing its last brew`() {
        val cards = cards(
            brew("oolong", "gaiwan", minute = 0),
            brew("oolong", "gaiwan", minute = 10),
            brew("oolong", "mug", minute = 5),
        )

        assertEquals(listOf("oolong/gaiwan" to "m10", "oolong/mug" to "m5"), cards.map { it.pair to it.brew.session.id })
    }

    @Test
    fun `a pair whose last brew is under 3 stars shows its best`() {
        val cards = cards(
            brew("oolong", "gaiwan", minute = 0, rating = 4f),
            brew("oolong", "gaiwan", minute = 10, rating = 2f),
        )

        assertEquals("m0", cards.single().brew.session.id)
    }

    @Test
    fun `pairs are ordered by their last completed brew`() {
        val cards = cards(
            brew("oolong", "gaiwan", minute = 0),
            brew("green", "gaiwan", minute = 20),
            brew("black", "mug", minute = 10),
            brew("oolong", "gaiwan", minute = 30, status = SessionStatus.IN_PROGRESS),
        )

        assertEquals(listOf("green/gaiwan", "black/mug", "oolong/gaiwan"), cards.map { it.pair })
    }

    @Test
    fun `a pair with no completed brew has no card`() {
        assertEquals(emptyList(), cards(brew("oolong", "gaiwan", minute = 0, status = SessionStatus.IN_PROGRESS)))
    }

    @Test
    fun `the row keeps the 8 most recent pairs`() {
        val cards = cards(*Array(10) { brew("tea$it", "gaiwan", minute = it) })

        assertEquals((9 downTo 2).map { "tea$it/gaiwan" }, cards.map { it.pair })
    }

    @Test
    fun `finished teas and archived vessels are left out`() {
        val cards = brewAgainCards(
            sessions = listOf(
                brew("oolong", "gaiwan", minute = 0),
                brew("finished", "gaiwan", minute = 1),
                brew("oolong", "archived", minute = 2),
            ),
            teas = listOf(tea("oolong"), tea("finished", finishedAt = at(0))),
            teaTypes = emptyList(),
            vessels = listOf(vessel("gaiwan"), vessel("archived", isArchived = true)),
        )

        assertEquals(listOf("oolong/gaiwan"), cards.map { it.pair })
    }
}

private val BrewAgainCardData.pair get() = "$teaId/$vesselId"

private fun cards(vararg sessions: SessionWithSteeps): List<BrewAgainCardData> = brewAgainCards(
    sessions = sessions.toList(),
    teas = sessions.map { tea(it.session.teaId) }.distinct(),
    teaTypes = emptyList(),
    vessels = sessions.map { vessel(it.session.vesselId) }.distinct(),
)

private fun at(minute: Int) = Instant.fromEpochSeconds(1_790_000_000L + minute * 60L)

private fun brew(
    teaId: String,
    vesselId: String,
    minute: Int,
    rating: Float? = null,
    status: SessionStatus = SessionStatus.COMPLETED,
): SessionWithSteeps {
    val id = "m$minute"
    return SessionWithSteeps(
        session = Session(
            id = id,
            teaId = teaId,
            status = status,
            vesselId = vesselId,
            waterType = WaterType.FILTERED,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = listOf(
            Steep(
                id = id,
                number = 1,
                timestamp = at(minute),
                brewingTime = 30.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 100.0,
                rating = rating,
                createdAt = at(minute),
                updatedAt = at(minute),
            ),
        ),
    )
}

private fun tea(id: String, finishedAt: Instant? = null) =
    Tea(id = id, name = id, teaTypeId = "oolong", finishedAt = finishedAt, createdAt = at(0), updatedAt = at(0))

private fun vessel(id: String, isArchived: Boolean = false) = BrewingVessel(
    id = id,
    name = id,
    iconName = null,
    isSystemDefault = false,
    isArchived = isArchived,
    displayOrder = 0,
    createdAt = at(0),
    updatedAt = at(0),
)
