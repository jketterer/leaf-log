package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeaTypeDao {

    @Query(
        """
        SELECT * FROM tea_type
        WHERE deletedAt IS NULL
        ORDER BY displayOrder ASC, name ASC
    """
    )
    fun getAllFlow(): Flow<List<TeaTypeEntity>>

    @Query(
        """
        SELECT * FROM tea_type
        WHERE deletedAt IS NULL
        ORDER BY displayOrder ASC, name ASC
    """
    )
    suspend fun getAll(): List<TeaTypeEntity>

    @Query("SELECT * FROM tea_type WHERE id = :id")
    suspend fun getById(id: String): TeaTypeEntity?

    @Query("SELECT * FROM tea_type WHERE id = :id")
    fun getByIdFlow(id: String): Flow<TeaTypeEntity?>

    @Upsert
    suspend fun upsert(teaType: TeaTypeEntity)

    @Upsert
    suspend fun upsertAll(teaTypes: List<TeaTypeEntity>)

    @Query("UPDATE tea_type SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query(
        """
        DELETE FROM tea_type
        WHERE deletedAt IS NOT NULL
        AND deletedAt < :cutoffTimestamp
    """
    )
    suspend fun deleteSyncedOldItems(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM tea_type WHERE deletedAt IS NULL")
    suspend fun count(): Int
}