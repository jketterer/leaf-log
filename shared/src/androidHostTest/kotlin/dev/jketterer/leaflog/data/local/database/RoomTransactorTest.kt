package dev.jketterer.leaflog.data.local.database

import dev.jketterer.leaflog.domain.models.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// The in-memory database has a single connection, so a DAO call in the block that asked for its
// own would wait on the transaction forever instead of joining it
class RoomTransactorTest {

    private val database = inMemoryDatabase()
    private val dao = database.teaSessionDao()
    private val transactor = RoomTransactor(database)

    @BeforeTest
    fun seed() = runTest { database.seedForeignKeys() }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `DAO calls in the block see its writes and they land when it returns`() = runTest {
        val seen = transactor {
            dao.upsert(session("a"))
            dao.getById("a")
        }

        assertEquals("a", seen?.id)
        assertEquals("a", dao.getById("a")?.id)
    }

    @Test
    fun `a failure in the block rolls back every DAO write made in it`() = runTest {
        dao.upsert(session("kept"))

        assertFailsWith<IllegalStateException> {
            transactor {
                dao.upsert(session("new"))
                dao.softDelete("kept", timestamp = 1)
                error("stats failed")
            }
        }

        assertNull(dao.getById("new"))
        assertNull(dao.getById("kept")?.deletedAt)
    }

    private fun session(id: String) = row(
        id = id,
        parentSessionId = null,
        steepNumber = 1,
        status = SessionStatus.IN_PROGRESS,
        vesselId = "vessel",
        timestamp = at(0),
    )
}
