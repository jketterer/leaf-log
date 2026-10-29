package dev.jketterer.leaflog.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.jketterer.leaflog.data.local.database.entities.TastingTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TastingTagDao {

    @Query("SELECT * FROM tasting_tag WHERE deletedAt IS NULL ORDER BY displayOrder ASC")
    suspend fun getAll(): List<TastingTagEntity>

    @Query("SELECT * FROM tasting_tag WHERE deletedAt IS NULL ORDER BY displayOrder ASC")
    fun getAllFlow(): Flow<List<TastingTagEntity>>

    /** Deleted rows too: a backup carries them. */
    @Query("SELECT * FROM tasting_tag")
    suspend fun getAllIncludingDeleted(): List<TastingTagEntity>

    @Query("SELECT MAX(displayOrder) FROM tasting_tag")
    suspend fun maxDisplayOrder(): Int?

    @Insert
    suspend fun insert(tag: TastingTagEntity)

    /** Seeds the curated tags: one already here is kept as it is, edited or deleted. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(tags: List<TastingTagEntity>)

    @Upsert
    suspend fun upsertAll(tags: List<TastingTagEntity>)

    @Query("UPDATE tasting_tag SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE tasting_tag SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    // Retagging changes a steep, so the steeps carrying the tag are stamped first, for a backup
    // to compare by. Deleted steeps too: a backup carries them

    @Query(
        """
        UPDATE tea_session SET updatedAt = :updatedAt
        WHERE id IN (SELECT steepId FROM steep_tag WHERE tagId = :tagId)
        """
    )
    suspend fun touchSteepsWith(tagId: String, updatedAt: Long)

    /** Puts [toId] on every steep with [fromId], confirmed if either was; [fromId]'s rows stay. */
    @Query(
        """
        INSERT INTO steep_tag (steepId, tagId, confirmed)
        SELECT steepId, :toId, confirmed FROM steep_tag WHERE tagId = :fromId
        ON CONFLICT (steepId, tagId) DO UPDATE SET confirmed = confirmed OR excluded.confirmed
        """
    )
    suspend fun copySteepTags(fromId: String, toId: String)

    @Query("DELETE FROM steep_tag WHERE tagId = :tagId")
    suspend fun deleteSteepTags(tagId: String)
}
