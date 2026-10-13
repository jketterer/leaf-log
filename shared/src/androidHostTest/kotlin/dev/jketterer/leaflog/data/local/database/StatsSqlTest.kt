package dev.jketterer.leaflog.data.local.database

import androidx.room.useWriterConnection
import dev.jketterer.leaflog.data.repositories.TeaSessionRepositoryImpl
import dev.jketterer.leaflog.domain.models.SessionStats
import dev.jketterer.leaflog.domain.models.SessionStatus.COMPLETED
import dev.jketterer.leaflog.domain.models.SessionStatus.IN_PROGRESS
import dev.jketterer.leaflog.domain.models.stats
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The SQL the v7 migration and import rebuild stats with, against the Kotlin rules it copies. */
class StatsSqlTest {

    private val database = inMemoryDatabase()
    private val dao = database.teaSessionDao()
    private val repository = TeaSessionRepositoryImpl(dao)

    @AfterTest
    fun close() = database.close()

    @Test
    fun `tea and method stats rebuilt in SQL match the Kotlin rules`() = runTest {
        database.seedForeignKeys()
        database.useWriterConnection { connection ->
            connection.usePrepared(
                "INSERT INTO brewing_configurations (id, tea_id, vessel_id, water_quantity_ml, " +
                    "temperature_celsius, brewing_time, water_type, times_used, is_active, " +
                    "created_at, updated_at) " +
                    "VALUES ('method', 'tea', 'vessel', 100, 95, 20000, 'FILTERED', 99, 1, 0, 0)",
            ) { it.step() }
        }
        dao.upsertAll(
            listOf(
                // One steep, rated
                row("single", null, 1, COMPLETED, "vessel", at(0)).copy(rating = 4f),
                // Gongfu with a deleted middle steep: 5 and 2 count, the unrated one doesn't
                row("gongfu", null, 1, COMPLETED, "vessel", at(10))
                    .copy(rating = 5f, usedConfigurationId = "method"),
                child("gongfu-2", "gongfu", 2, at(11)).copy(rating = 1f, deletedAt = at(12)),
                child("gongfu-3", "gongfu", 3, at(12)).copy(rating = 2f),
                child("gongfu-4", "gongfu", 4, at(13)),
                // Counts, but has no rating to pull the average
                row("unrated", null, 1, COMPLETED, "vessel", at(20))
                    .copy(usedConfigurationId = "method"),
                // Still brewing: no count, no rating, not the last brew
                row("brewing", null, 1, IN_PROGRESS, "vessel", at(30))
                    .copy(rating = 1f, usedConfigurationId = "method"),
                // Deleted with its steep
                row("deleted", null, 1, COMPLETED, "vessel", at(40))
                    .copy(rating = 1f, deletedAt = at(41)),
                child("deleted-2", "deleted", 2, at(41)).copy(rating = 1f, deletedAt = at(41)),
            ),
        )

        dao.recomputeTeaStats()
        dao.recomputeConfigurationStats()

        val sessions = repository.getSessionsByTea("tea")
        val tea = database.teaDao().getById("tea")!!
        val method = database.brewingConfigurationDao().getById("method")!!

        val teaStats = sessions.stats()
        val methodStats = sessions.filter { it.session.usedConfigurationId == "method" }.stats()

        assertEquals(SessionStats(count = 3, averageRating = 3.75f, lastAt = at(20)), teaStats)
        assertEquals(teaStats, SessionStats(tea.totalSessions, tea.averageRating, tea.lastBrewedAt))
        assertEquals(methodStats.count to methodStats.lastAt, method.timesUsed to method.lastUsedAt)
    }

    private fun child(id: String, parentId: String, number: Int, timestamp: Instant) =
        row(id, parentId, number, COMPLETED, "vessel", timestamp).copy(
            teaQuantityGrams = null,
            vesselId = null,
            waterType = null,
        )
}
