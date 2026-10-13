package dev.jketterer.leaflog.data.local.database

import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.repositories.TeaSessionRepositoryImpl
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionReadsTest {

    // Room only asks the context for system services, and copes with none
    private val database = Room.inMemoryDatabaseBuilder<LeafLogDatabase>(
        object : ContextWrapper(null) {
            override fun getSystemService(name: String): Any? = null
        },
    ).setDriver(BundledSQLiteDriver()).build()

    private val dao = database.teaSessionDao()
    private val repository = TeaSessionRepositoryImpl(dao)

    @BeforeTest
    fun seedForeignKeys() = runTest {
        database.useWriterConnection { connection ->
            listOf(
                "INSERT INTO tea_type (id, name, colorHex, isSystemDefault, displayOrder, " +
                    "createdAt, updatedAt) VALUES ('type', 'Oolong', '#000000', 0, 0, 0, 0)",
                "INSERT INTO tea (id, name, teaTypeId, photos, isFavorite, totalSessions, " +
                    "createdAt, updatedAt, syncStatus) " +
                    "VALUES ('tea', 'Tieguanyin', 'type', '[]', 0, 0, 0, 0, 'LOCAL_ONLY')",
            ).plus(
                listOf("vessel", "gaiwan", "mug").map {
                    "INSERT INTO brewing_vessel (id, name, isSystemDefault, isArchived, " +
                        "displayOrder, createdAt, updatedAt, syncStatus) " +
                        "VALUES ('$it', '$it', 0, 0, 0, 0, 0, 'LOCAL_ONLY')"
                },
            ).forEach { sql -> connection.usePrepared(sql) { it.step() } }
        }
    }

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
    fun `a range holds the sessions that started in it along with all their steeps`() = runTest {
        insertSession("before", startMinute = 0, steeps = 3, steepGap = 10)
        insertSession("inside", startMinute = 15, steeps = 3, steepGap = 10)

        val sessions = repository.getSessionsStartedBetween(at(10), at(20))

        assertEquals(listOf("inside"), sessions.ids())
        assertEquals(3, sessions.single().steeps.size)
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

private fun at(minute: Int) = Instant.fromEpochSeconds(1_790_000_000L) + minute.minutes

private fun row(
    id: String,
    parentSessionId: String?,
    steepNumber: Int,
    status: SessionStatus,
    vesselId: String,
    timestamp: Instant,
) = TeaSessionEntity(
    id = id,
    teaId = "tea",
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = status,
    teaQuantityGrams = 5f,
    vesselId = vesselId,
    waterType = WaterType.FILTERED,
    location = null,
    rating = null,
    averageRating = null,
    usedConfigurationId = null,
    timestamp = timestamp,
    brewingTime = 20.seconds,
    temperatureCelsius = 95.0,
    waterQuantityMl = 100.0,
    notes = null,
    photos = emptyList(),
    userId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    createdAt = timestamp,
    updatedAt = timestamp,
    deletedAt = null,
)
