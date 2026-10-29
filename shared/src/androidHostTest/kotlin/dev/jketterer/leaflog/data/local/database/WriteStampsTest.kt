package dev.jketterer.leaflog.data.local.database

import androidx.room.useReaderConnection
import dev.jketterer.leaflog.domain.models.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private suspend fun updatedAt(table: String, column: String, id: String) =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT $column FROM $table WHERE id = '$id'") {
                it.step()
                it.getLong(0)
            }
        }
}
