package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.SteepRow
import dev.jketterer.leaflog.data.local.database.entities.SteepTagEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import kotlinx.coroutines.flow.Flow

// The stats rules of `List<SessionWithSteeps>.stats()` in SQL, for the v7 migration and import,
// where there are no aggregates to hand. StatsSqlTest checks the two agree.

/** The parent rows of completed, live sessions. */
internal const val COMPLETED_SESSIONS = """
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

// Deletes before schema 8 ignored what still pointed at the row, and an old backup can bring such rows
// back. Run in this order, after the v8 migration and after import: teas before types, so a
// restored tea keeps its type. Derived, like stats, so none of them stamps updatedAt.

internal const val RESTORE_TEAS_WITH_LIVE_SESSIONS = """
    UPDATE tea SET finishedAt = COALESCE(finishedAt, deletedAt), deletedAt = NULL
    WHERE deletedAt IS NOT NULL AND EXISTS (
        SELECT 1 FROM tea_session s
        WHERE s.teaId = tea.id AND s.parentSessionId IS NULL AND s.deletedAt IS NULL
    )
"""

internal const val RESTORE_VESSELS_WITH_LIVE_SESSIONS = """
    UPDATE brewing_vessel SET deletedAt = NULL, isArchived = 1
    WHERE deletedAt IS NOT NULL AND EXISTS (
        SELECT 1 FROM tea_session s
        WHERE s.vesselId = brewing_vessel.id AND s.deletedAt IS NULL
    )
"""

internal const val RESTORE_TYPES_WITH_LIVE_TEAS = """
    UPDATE tea_type SET deletedAt = NULL
    WHERE deletedAt IS NOT NULL AND EXISTS (
        SELECT 1 FROM tea t WHERE t.teaTypeId = tea_type.id AND t.deletedAt IS NULL
    )
"""

internal val REPAIRS = listOf(
    RESTORE_TEAS_WITH_LIVE_SESSIONS,
    RESTORE_VESSELS_WITH_LIVE_SESSIONS,
    RESTORE_TYPES_WITH_LIVE_TEAS,
)

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
    @Transaction
    fun getAllSessionsFlow(): Flow<List<SteepRow>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    @Transaction
    suspend fun getAllSessions(): List<SteepRow>

    @Query(
        """
        SELECT id FROM tea_session
        WHERE deletedAt IS NULL AND parentSessionId IS NULL
        ORDER BY timestamp, id
    """
    )
    fun getSessionIdsFlow(): Flow<List<String>>

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
    @Transaction
    fun getRecentSessionsFlow(limit: Int): Flow<List<SteepRow>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) = :id
    """
    )
    @Transaction
    suspend fun getSession(id: String): List<SteepRow>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND COALESCE(parentSessionId, id) = :id
    """
    )
    @Transaction
    fun getSessionFlow(id: String): Flow<List<SteepRow>>

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
    @Transaction
    suspend fun getSessionsByTea(teaId: String): List<SteepRow>

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
    @Transaction
    fun getSessionsByTeaFlow(teaId: String): Flow<List<SteepRow>>

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
    @Transaction
    suspend fun getSessionsByVessel(vesselId: String): List<SteepRow>

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
    @Transaction
    fun getInProgressSessionsFlow(): Flow<List<SteepRow>>

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
    @Transaction
    suspend fun getSessionOfSteep(steepId: String): List<SteepRow>

    /** Deleted rows too: a backup carries them. */
    @Query("SELECT * FROM tea_session")
    suspend fun getAllIncludingDeleted(): List<TeaSessionEntity>

    @Upsert
    suspend fun upsert(teaSession: TeaSessionEntity)

    @Query(RECOMPUTE_TEA_STATS)
    suspend fun recomputeTeaStats()

    // The REPAIRS, one statement per query, called in the same order

    @Query(RESTORE_TEAS_WITH_LIVE_SESSIONS)
    suspend fun restoreTeasWithLiveSessions()

    @Query(RESTORE_VESSELS_WITH_LIVE_SESSIONS)
    suspend fun restoreVesselsWithLiveSessions()

    @Query(RESTORE_TYPES_WITH_LIVE_TEAS)
    suspend fun restoreTypesWithLiveTeas()

    @Upsert
    suspend fun upsertAll(teaSessions: List<TeaSessionEntity>)

    // A steep's tags are replaced whole: delete then insert, in the caller's transaction

    @Query("DELETE FROM steep_tag WHERE steepId IN (:steepIds)")
    suspend fun deleteTags(steepIds: List<String>)

    @Insert
    suspend fun insertTags(tags: List<SteepTagEntity>)

    /** Every steep's tags, deleted steeps' too: a backup carries them. */
    @Query("SELECT * FROM steep_tag")
    suspend fun getAllTags(): List<SteepTagEntity>

    @Query("UPDATE tea_session SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query(
        """
        UPDATE tea_session
        SET deletedAt = :timestamp, updatedAt = :timestamp
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
