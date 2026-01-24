package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeaDao {

    @Query(
        """
        SELECT * FROM tea
        WHERE deletedAt IS NULL
        ORDER BY name ASC
    """
    )
    fun getAllFlow(): Flow<List<TeaEntity>>

    @Query(
        """
        SELECT * FROM tea
        WHERE deletedAt IS NULL
        ORDER BY name ASC
    """
    )
    suspend fun getAll(): List<TeaEntity>

    @Query("SELECT * FROM tea WHERE id = :id")
    suspend fun getById(id: String): TeaEntity?

    @Query("SELECT * FROM tea WHERE id = :id")
    fun getByIdFlow(id: String): Flow<TeaEntity?>

    @Query(
        """
        SELECT * FROM tea
        WHERE deletedAt IS NULL
        AND teaTypeId = :teaTypeId
        ORDER BY name ASC
    """
    )
    fun getByTypeFlow(teaTypeId: String): Flow<List<TeaEntity>>

    @Query(
        """
        SELECT * FROM tea
        WHERE deletedAt IS NULL
        AND isFavorite = 1
        ORDER BY name ASC
    """
    )
    fun getFavoritesFlow(): Flow<List<TeaEntity>>

    @Query(
        """
        SELECT * FROM tea
        WHERE deletedAt IS NULL
        AND name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """
    )
    suspend fun search(query: String): List<TeaEntity>

    @Upsert
    suspend fun upsert(tea: TeaEntity)

    @Upsert
    suspend fun upsertAll(teas: List<TeaEntity>)

    @Query("UPDATE tea SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Delete
    suspend fun delete(tea: TeaEntity)

    @Query(
        """
        DELETE FROM tea
        WHERE deletedAt IS NOT NULL
        AND deletedAt < :cutoffTimestamp
        AND syncStatus = 'SYNCED'
    """
    )
    suspend fun deleteSyncedOldItems(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM tea WHERE deletedAt IS NULL")
    suspend fun count(): Int
}