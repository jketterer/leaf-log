package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewingConfigurationDao {
    @Query("SELECT * FROM brewing_configurations WHERE tea_id = :teaId AND vessel_id = :vesselId AND is_active = 1 ORDER BY rating DESC, times_used DESC, last_used_at DESC")
    suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfigurationEntity>

    @Query("SELECT * FROM brewing_configurations WHERE tea_id = :teaId AND vessel_id = :vesselId AND is_active = 1 ORDER BY rating DESC, times_used DESC, last_used_at DESC LIMIT 1")
    suspend fun getBestByTeaAndVessel(teaId: String, vesselId: String): BrewingConfigurationEntity?

    @Query("SELECT * FROM brewing_configurations WHERE tea_id = :teaId ORDER BY rating DESC, times_used DESC")
    suspend fun getByTeaId(teaId: String): List<BrewingConfigurationEntity>

    @Query("SELECT * FROM brewing_configurations WHERE tea_id = :teaId ORDER BY rating DESC, times_used DESC")
    fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfigurationEntity>>

    @Query("SELECT * FROM brewing_configurations WHERE id = :id")
    suspend fun getById(id: String): BrewingConfigurationEntity?

    @Query("SELECT * FROM brewing_configurations WHERE id = :id")
    fun getByIdFlow(id: String): Flow<BrewingConfigurationEntity?>

    @Upsert
    suspend fun upsert(configuration: BrewingConfigurationEntity)

    @Query("SELECT * FROM brewing_configurations")
    suspend fun getAll(): List<BrewingConfigurationEntity>

    @Upsert
    suspend fun upsertAll(configurations: List<BrewingConfigurationEntity>)

    @Query("DELETE FROM brewing_configurations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE brewing_configurations SET times_used = times_used + 1, last_used_at = :timestamp WHERE id = :id")
    suspend fun incrementTimesUsed(id: String, timestamp: Long)

    @Query("UPDATE brewing_configurations SET is_active = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("SELECT * FROM brewing_configurations WHERE is_active = 1 AND times_used > 0 ORDER BY times_used DESC, last_used_at DESC LIMIT :limit")
    fun getMostUsedFlow(limit: Int): Flow<List<BrewingConfigurationEntity>>

    @Query("""
        SELECT * FROM brewing_configurations
        WHERE is_active = 1
        ORDER BY is_pinned DESC,
                 CASE WHEN is_pinned = 1 THEN pinned_sort_order END ASC,
                 times_used DESC, last_used_at DESC
    """)
    fun getAllActiveFlow(): Flow<List<BrewingConfigurationEntity>>

    @Query("SELECT MAX(pinned_sort_order) FROM brewing_configurations WHERE is_pinned = 1 AND is_active = 1")
    suspend fun getMaxPinnedSortOrder(): Int?

    @Query("UPDATE brewing_configurations SET is_pinned = :isPinned, pinned_sort_order = :pinnedSortOrder WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, pinnedSortOrder: Int)

    @Query("UPDATE brewing_configurations SET pinned_sort_order = :pinnedSortOrder WHERE id = :id")
    suspend fun updatePinnedSortOrder(id: String, pinnedSortOrder: Int)
}
