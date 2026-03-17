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
}
