package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import kotlinx.coroutines.flow.Flow

// The stats rules of `List<SessionWithSteeps>.stats()` in SQL, for the v7 migration and import,
// where there are no aggregates to hand. MigrationTest checks the two agree.

/** The parent rows of completed, live sessions. */
private const val COMPLETED_SESSIONS = """
    FROM tea_session s
    WHERE s.parentSessionId IS NULL AND s.deletedAt IS NULL AND s.status = 'COMPLETED'
"""

/** Mean of the live, rated steeps of session `s`. */
private const val SESSION_RATING = """
    (SELECT AVG(x.rating) FROM tea_session x
     WHERE x.deletedAt IS NULL AND (x.id = s.id OR x.parentSessionId = s.id))
"""

internal const val RECOMPUTE_TEA_STATS = """
    UPDATE tea SET
        totalSessions = (SELECT COUNT(*) $COMPLETED_SESSIONS AND s.teaId = tea.id),
        averageRating = (SELECT AVG($SESSION_RATING) $COMPLETED_SESSIONS AND s.teaId = tea.id),
        lastBrewedAt = (SELECT MAX(s.timestamp) $COMPLETED_SESSIONS AND s.teaId = tea.id)
"""

internal const val RECOMPUTE_CONFIGURATION_STATS = """
    UPDATE brewing_configurations SET
        times_used = (
            SELECT COUNT(*) $COMPLETED_SESSIONS
            AND s.usedConfigurationId = brewing_configurations.id
            AND s.teaId = brewing_configurations.tea_id
        ),
        last_used_at = (
            SELECT MAX(s.timestamp) $COMPLETED_SESSIONS
            AND s.usedConfigurationId = brewing_configurations.id
            AND s.teaId = brewing_configurations.tea_id
        )
"""

@Dao
interface TeaSessionDao {

    @Query("SELECT * FROM tea_session WHERE id = :id")
    suspend fun getById(id: String): TeaSessionEntity?

    // Each read returns every live row of the matching sessions, newest session first. Filters
    // apply to the parent row: it holds the session's fields and its start time. Group the rows
    // with toAggregates().

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    fun getAllSessionsFlow(): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    suspend fun getAllSessions(): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            ORDER BY timestamp DESC
            LIMIT :limit
        )
        ORDER BY timestamp DESC
    """
    )
    fun getRecentSessionsFlow(limit: Int): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) = :id
    """
    )
    suspend fun getSession(id: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) = :id
    """
    )
    fun getSessionFlow(id: String): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            AND teaId = :teaId
        )
        ORDER BY timestamp DESC
    """
    )
    suspend fun getSessionsByTea(teaId: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            AND teaId = :teaId
        )
        ORDER BY timestamp DESC
    """
    )
    fun getSessionsByTeaFlow(teaId: String): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            AND vesselId = :vesselId
        )
        ORDER BY timestamp DESC
    """
    )
    suspend fun getSessionsByVessel(vesselId: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            AND status = 'IN_PROGRESS'
        )
        ORDER BY timestamp DESC
    """
    )
    fun getInProgressSessionsFlow(): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) IN (
            SELECT id FROM tea_session
            WHERE deletedAt IS NULL
            AND parentSessionId IS NULL
            AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        )
        ORDER BY timestamp DESC
    """
    )
    suspend fun getSessionsStartedBetween(
        startTimestamp: Long,
        endTimestamp: Long,
    ): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) = (
            SELECT COALESCE(parentSessionId, id) FROM tea_session
            WHERE deletedAt IS NULL
            AND id = :steepId
        )
    """
    )
    suspend fun getSessionOfSteep(steepId: String): List<TeaSessionEntity>

    @Upsert
    suspend fun upsert(teaSession: TeaSessionEntity)

    @Query(RECOMPUTE_TEA_STATS)
    suspend fun recomputeTeaStats()

    @Query(RECOMPUTE_CONFIGURATION_STATS)
    suspend fun recomputeConfigurationStats()

    @Upsert
    suspend fun upsertAll(teaSessions: List<TeaSessionEntity>)

    @Query("UPDATE tea_session SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query(
        """
        UPDATE tea_session
        SET deletedAt = :timestamp
        WHERE parentSessionId = :parentId
    """
    )
    suspend fun softDeleteChildSteeps(parentId: String, timestamp: Long)

    @Query(
        """
        DELETE FROM tea_session
        WHERE deletedAt IS NOT NULL
        AND deletedAt < :cutoffTimestamp
        AND syncStatus = 'SYNCED'
    """
    )
    suspend fun deleteSyncedOldItems(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM tea_session WHERE deletedAt IS NULL")
    suspend fun count(): Int
}
