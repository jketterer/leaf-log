package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeaSessionDao {

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    fun getAllFlow(): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    suspend fun getAll(): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
        LIMIT :limit
    """
    )
    fun getRecentFlow(limit: Int): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        ORDER BY timestamp DESC
        LIMIT :limit
    """
    )
    suspend fun getRecent(limit: Int): List<TeaSessionEntity>

    @Query("SELECT * FROM tea_session WHERE id = :id")
    suspend fun getById(id: String): TeaSessionEntity?

    @Query("SELECT * FROM tea_session WHERE id = :id")
    fun getByIdFlow(id: String): Flow<TeaSessionEntity?>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND teaId = :teaId
        ORDER BY timestamp DESC
    """
    )
    suspend fun getByTeaId(teaId: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND teaId = :teaId
        ORDER BY timestamp DESC
    """
    )
    fun getByTeaIdFlow(teaId: String): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE vesselId = :vesselId
        AND deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    suspend fun getByVesselId(vesselId: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE vesselId = :vesselId
        AND deletedAt IS NULL
        ORDER BY timestamp DESC
    """
    )
    fun getByVesselIdFlow(vesselId: String): Flow<List<TeaSessionEntity>>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND parentSessionId = :parentId
        ORDER BY steepNumber ASC
    """
    )
    suspend fun getChildSteeps(parentId: String): List<TeaSessionEntity>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND status = :status
        ORDER BY timestamp DESC
    """
    )
    fun getByStatusFlow(status: String): Flow<List<TeaSessionEntity>>

    @Query("SELECT COUNT(*) FROM tea_session WHERE deletedAt IS NULL AND status = :status")
    fun getCountByStatusFlow(status: String): Flow<Int>

    @Query(
        """
        SELECT * FROM tea_session
        WHERE deletedAt IS NULL
        AND timestamp >= :startTimestamp
        AND timestamp <= :endTimestamp
        ORDER BY timestamp DESC
    """
    )
    suspend fun getByDateRange(startTimestamp: Long, endTimestamp: Long): List<TeaSessionEntity>

    @Upsert
    suspend fun upsert(teaSession: TeaSessionEntity)

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

    @Delete
    suspend fun delete(teaSession: TeaSessionEntity)

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

    @Query(
        """
        SELECT COUNT(*) FROM tea_session
        WHERE deletedAt IS NULL
        AND status = 'COMPLETED'
        AND timestamp >= :startTimestamp
        AND timestamp <= :endTimestamp
    """
    )
    suspend fun countByDateRange(startTimestamp: Long, endTimestamp: Long): Int
}