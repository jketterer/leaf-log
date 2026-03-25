package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewingVesselDao {

    @Query(
        """
        SELECT * FROM brewing_vessel
        WHERE deletedAt IS NULL
        ORDER BY displayOrder ASC, name ASC
    """
    )
    fun getAllFlow(): Flow<List<BrewingVesselEntity>>

    @Query(
        """
        SELECT * FROM brewing_vessel
        WHERE deletedAt IS NULL
        ORDER BY displayOrder ASC, name ASC
    """
    )
    suspend fun getAll(): List<BrewingVesselEntity>

    @Query("SELECT * FROM brewing_vessel WHERE id = :id")
    suspend fun getById(id: String): BrewingVesselEntity?

    @Query("SELECT * FROM brewing_vessel WHERE id = :id")
    fun getByIdFlow(id: String): Flow<BrewingVesselEntity?>

    @Upsert
    suspend fun upsert(vessel: BrewingVesselEntity)

    @Upsert
    suspend fun upsertAll(vessels: List<BrewingVesselEntity>)

    @Query("UPDATE brewing_vessel SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query(
        """
        DELETE FROM brewing_vessel
        WHERE deletedAt IS NOT NULL
        AND deletedAt < :cutoffTimestamp
        AND syncStatus = 'SYNCED'
    """
    )
    suspend fun deleteSyncedOldItems(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM brewing_vessel WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM brewing_vessel
        WHERE deletedAt IS NULL AND isArchived = 0
        ORDER BY displayOrder ASC, name ASC
    """
    )
    fun getActiveFlow(): Flow<List<BrewingVesselEntity>>

    @Query(
        """
        SELECT * FROM brewing_vessel
        WHERE deletedAt IS NULL AND isArchived = 0
        ORDER BY displayOrder ASC, name ASC
    """
    )
    suspend fun getActive(): List<BrewingVesselEntity>

    @Query("SELECT COUNT(*) FROM brewing_vessel WHERE deletedAt IS NULL AND isArchived = 0")
    suspend fun countActive(): Int
}