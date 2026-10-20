package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.jketterer.leaflog.data.local.database.entities.TastingTagEntity

@Dao
interface TastingTagDao {

    @Query("SELECT * FROM tasting_tag WHERE deletedAt IS NULL ORDER BY displayOrder ASC")
    suspend fun getAll(): List<TastingTagEntity>

    /** Deleted rows too: a backup carries them. */
    @Query("SELECT * FROM tasting_tag")
    suspend fun getAllIncludingDeleted(): List<TastingTagEntity>

    @Query("SELECT MAX(displayOrder) FROM tasting_tag")
    suspend fun maxDisplayOrder(): Int?

    @Insert
    suspend fun insert(tag: TastingTagEntity)

    /** Tags have no edit to merge, so one already here is kept as it is. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(tags: List<TastingTagEntity>)
}
