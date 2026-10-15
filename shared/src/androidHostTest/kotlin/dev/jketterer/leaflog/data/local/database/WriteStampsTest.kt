package dev.jketterer.leaflog.data.local.database

import androidx.room.useReaderConnection
import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Import keeps whichever copy of a row has the later `updatedAt`, so every write must stamp it. */
class WriteStampsTest {

    private val database = inMemoryDatabase()

    @BeforeTest
    fun seed() = runTest {
        database.seedForeignKeys()
        database.teaSessionDao().upsertAll(
            listOf(
                row("parent", null, 1, SessionStatus.COMPLETED, "vessel", at(0)),
                row("child", "parent", 2, SessionStatus.COMPLETED, "vessel", at(1)),
            ),
        )
        database.brewingConfigurationDao().upsertAll(listOf(method("method"), method("other")))
    }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `soft-deleting a tea stamps it`() = runTest {
        database.teaDao().softDelete("tea", timestamp = 5)

        assertEquals(5L, updatedAt("tea", "updatedAt", "tea"))
    }

    @Test
    fun `soft-deleting a tea type stamps it`() = runTest {
        database.teaTypeDao().softDelete("type", timestamp = 5)

        assertEquals(5L, updatedAt("tea_type", "updatedAt", "type"))
    }

    @Test
    fun `soft-deleting a vessel stamps it`() = runTest {
        database.brewingVesselDao().softDelete("vessel", timestamp = 5)

        assertEquals(5L, updatedAt("brewing_vessel", "updatedAt", "vessel"))
    }

    @Test
    fun `soft-deleting a steep stamps it`() = runTest {
        database.teaSessionDao().softDelete("child", timestamp = 5)

        assertEquals(5L, updatedAt("tea_session", "updatedAt", "child"))
    }

    @Test
    fun `soft-deleting a session's steeps stamps each of them`() = runTest {
        database.teaSessionDao().softDeleteChildSteeps("parent", timestamp = 5)

        assertEquals(5L, updatedAt("tea_session", "updatedAt", "child"))
    }

    @Test
    fun `soft-deleting a method stamps it`() = runTest {
        database.brewingConfigurationDao().softDelete("method", timestamp = 5)

        assertEquals(5L, updatedAt("brewing_configurations", "updated_at", "method"))
    }

    @Test
    fun `a method deleted with its tea takes the tea's stamp`() = runTest {
        database.teaDao().softDelete("tea", timestamp = 5)

        database.brewingConfigurationDao().softDeleteWithTea("tea")

        assertEquals(5L, updatedAt("brewing_configurations", "updated_at", "method"))
    }

    @Test
    fun `a method deleted with its vessel takes the vessel's stamp`() = runTest {
        database.brewingVesselDao().softDelete("vessel", timestamp = 5)

        database.brewingConfigurationDao().softDeleteWithVessel("vessel")

        assertEquals(5L, updatedAt("brewing_configurations", "updated_at", "method"))
    }

    @Test
    fun `pinning a method stamps it`() = runTest {
        database.brewingConfigurationDao().setPinned("method", true, pinnedSortOrder = 1, updatedAt = 5)

        assertEquals(5L, updatedAt("brewing_configurations", "updated_at", "method"))
    }

    @Test
    fun `reordering a pinned method stamps only that method`() = runTest {
        database.brewingConfigurationDao().updatePinnedSortOrder("method", 2, updatedAt = 5)

        assertEquals(5L, updatedAt("brewing_configurations", "updated_at", "method"))
        assertEquals(0L, updatedAt("brewing_configurations", "updated_at", "other"))
    }

    private suspend fun updatedAt(table: String, column: String, id: String) =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT $column FROM $table WHERE id = '$id'") {
                it.step()
                it.getLong(0)
            }
        }

    private fun method(id: String) = BrewingConfigurationEntity(
        id = id,
        teaId = "tea",
        vesselId = "vessel",
        teaQuantityGrams = null,
        waterQuantityMl = 100.0,
        temperatureCelsius = 95.0,
        brewingTime = 20.seconds,
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = null,
        createdAt = at(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
}
