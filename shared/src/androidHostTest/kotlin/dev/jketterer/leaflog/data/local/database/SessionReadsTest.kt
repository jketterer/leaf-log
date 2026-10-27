package dev.jketterer.leaflog.data.local.database

import dev.jketterer.leaflog.data.repositories.TeaSessionRepositoryImpl
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

class SessionReadsTest {

    private val database = inMemoryDatabase()
    private val dao = database.teaSessionDao()
    private val repository = TeaSessionRepositoryImpl(dao, Clock.System)

    @BeforeTest
    fun seed() = runTest { database.seedForeignKeys() }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `the recent limit counts sessions rather than steeps`() = runTest {
        insertSession("old", startMinute = 0)
        insertSession("new", startMinute = 10, steeps = 3)
        insertSession("middle", startMinute = 5)

        val recent = repository.getRecentSessionsFlow(limit = 2).first()

        assertEquals(listOf("new", "middle"), recent.ids())
        assertEquals(3, recent.first().steeps.size)
    }

    @Test
    fun `a session is found by its id with all its steeps`() = runTest {
        insertSession("a", steeps = 3)

        assertEquals(listOf(1, 2, 3), repository.getSession("a")!!.steeps.map { it.number })
    }

    @Test
    fun `a steep's own id doesn't find the session`() = runTest {
        insertSession("a", steeps = 2)

        assertNull(repository.getSession("a-2"))
    }

    @Test
    fun `any steep's id finds the session it belongs to`() = runTest {
        insertSession("a", steeps = 3)
        insertSession("b", steeps = 2)
        dao.softDelete("a-3", timestamp = 1)

        assertEquals(listOf("a", "a-2"), repository.getSessionOfSteep("a-2")!!.steeps.map { it.id })
        assertEquals("a", repository.getSessionOfSteep("a")!!.session.id)
        assertNull(repository.getSessionOfSteep("a-3"))
    }

    @Test
    fun `deleted steeps are left out and deleted sessions aren't found`() = runTest {
        insertSession("kept", steeps = 3)
        insertSession("gone", steeps = 2)
        dao.softDelete("kept-2", timestamp = 1)
        dao.softDelete("gone", timestamp = 1)
        dao.softDeleteChildSteeps("gone", timestamp = 1)

        assertEquals(listOf(1, 3), repository.getSession("kept")!!.steeps.map { it.number })
        assertNull(repository.getSession("gone"))
        assertEquals(listOf("kept"), repository.getAllSessions().ids())
    }

    @Test
    fun `filters look at the session row and not at what its steeps carry`() = runTest {
        insertSession("a", steeps = 2, vesselId = "gaiwan", childVesselId = "mug")

        assertEquals(listOf("a"), repository.getSessionsByVessel("gaiwan").ids())
        assertEquals(emptyList(), repository.getSessionsByVessel("mug").ids())
    }

    @Test
    fun `a session is in progress when its session row is`() = runTest {
        insertSession("brewing", steeps = 2, status = SessionStatus.IN_PROGRESS)
        insertSession("done", status = SessionStatus.COMPLETED)

        val inProgress = repository.getInProgressSessionsFlow().first()

        assertEquals(listOf("brewing"), inProgress.ids())
        assertEquals(2, inProgress.single().steeps.size)
    }

    @Test
    fun `sessions come newest first by start time`() = runTest {
        insertSession("early-long", startMinute = 0, steeps = 3, steepGap = 30)
        insertSession("later", startMinute = 10)

        assertEquals(listOf("later", "early-long"), repository.getSessionsByTea("tea").ids())
    }

    private suspend fun insertSession(
        id: String,
        startMinute: Int = 0,
        steeps: Int = 1,
        steepGap: Int = 1,
        status: SessionStatus = SessionStatus.COMPLETED,
        vesselId: String = "vessel",
        childVesselId: String = vesselId,
    ) {
        (1..steeps).forEach { number ->
            dao.upsert(
                row(
                    id = if (number == 1) id else "$id-$number",
                    parentSessionId = id.takeIf { number > 1 },
                    steepNumber = number,
                    status = status,
                    vesselId = if (number == 1) vesselId else childVesselId,
                    timestamp = at(startMinute + (number - 1) * steepGap),
                ),
            )
        }
    }
}

private fun List<SessionWithSteeps>.ids() = map { it.session.id }
