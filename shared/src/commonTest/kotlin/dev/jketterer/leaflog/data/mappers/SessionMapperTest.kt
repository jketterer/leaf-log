package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.SteepRow
import dev.jketterer.leaflog.data.local.database.entities.SteepTagEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionMapperTest {

    @Test
    fun `steeps are grouped under their parent in steep order`() {
        val rows = listOf(row("b", "a", 3), row("a"), row("c", "a", 2))

        val aggregate = rows.toAggregates().single()

        assertEquals("a", aggregate.session.id)
        assertEquals(listOf("a", "c", "b"), aggregate.steeps.map { it.id })
        assertEquals(listOf(1, 2, 3), aggregate.steeps.map { it.number })
    }

    @Test
    fun `sessions keep the order of their parent rows`() {
        val rows = listOf(row("new"), row("old-2", "old", 2), row("old"), row("new-2", "new", 2))

        assertEquals(listOf("new", "old"), rows.toAggregates().map { it.session.id })
    }

    @Test
    fun `a steep without its parent row is dropped`() {
        assertEquals(emptyList(), listOf(row("orphan", "missing", 2)).toAggregates())
    }

    @Test
    fun `steeps written back reproduce the rows they were read from`() {
        val rows = listOf(
            row("a", rating = 4f).copy(tags = listOf(SteepTagEntity("a", "floral", confirmed = true))),
            row("a-2", "a", 2, rating = null).copy(
                tags = listOf(SteepTagEntity("a-2", "floral", confirmed = false)),
            ),
        )

        val aggregate = rows.toAggregates().single()

        assertEquals(rows, aggregate.steeps.map { it.toRow(aggregate.session) })
    }

    @Test
    fun `a later steep is written without the session's columns which stay on the parent`() {
        val aggregate = listOf(row("a"), row("a-2", "a", 2)).toAggregates().single()

        val child = aggregate.steeps[1].toEntity(aggregate.session)

        val sessionColumns = with(child) {
            listOf(teaQuantityGrams, vesselId, waterType, location, usedConfigurationId)
        }
        assertEquals(List(5) { null }, sessionColumns)
        assertEquals("vessel", aggregate.session.vesselId)
    }
}

private val start = Instant.fromEpochSeconds(1_790_000_000L)

private fun row(
    id: String,
    parentSessionId: String? = null,
    steepNumber: Int = 1,
    rating: Float? = null,
) = SteepRow(steep = entity(id, parentSessionId, steepNumber, rating), tags = emptyList())

private fun entity(
    id: String,
    parentSessionId: String?,
    steepNumber: Int,
    rating: Float?,
) = TeaSessionEntity(
    id = id,
    teaId = "tea",
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = SessionStatus.COMPLETED,
    // Session columns are the parent's alone
    teaQuantityGrams = 5f.takeIf { parentSessionId == null },
    vesselId = "vessel".takeIf { parentSessionId == null },
    waterType = WaterType.FILTERED.takeIf { parentSessionId == null },
    location = "desk".takeIf { parentSessionId == null },
    rating = rating,
    usedConfigurationId = "config".takeIf { parentSessionId == null },
    timestamp = start + (steepNumber * 60).seconds,
    brewingTime = 20.seconds,
    temperatureCelsius = 95.0,
    waterQuantityMl = 100.0,
    notes = "note $id",
    photos = emptyList(),
    userId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    createdAt = start,
    updatedAt = start,
    deletedAt = null,
    actualBrewingTimeMs = 23_000L.takeIf { parentSessionId == null },
)
