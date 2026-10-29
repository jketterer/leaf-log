package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@Entity(tableName = "tasting_tag")
data class TastingTagEntity(
    @PrimaryKey val id: String,
    /** Names a curated tag's string resource; null on a custom tag. */
    val key: String?,
    /** A custom tag's name; null on a curated tag. */
    val name: String?,
    val isCustom: Boolean,
    val displayOrder: Int,
    val deletedAt: Instant? = null,
    /** The epoch on a curated tag, which is never edited, and on rows from before tags were. */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Instant = Instant.fromEpochMilliseconds(0),
)

@Entity(
    tableName = "steep_tag",
    primaryKeys = ["steepId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TeaSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["steepId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TastingTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class SteepTagEntity(
    val steepId: String,
    val tagId: String,
    val confirmed: Boolean,
)

/** A steep's row with its tags, as the session reads return it. */
data class SteepRow(
    @Embedded val steep: TeaSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "steepId")
    val tags: List<SteepTagEntity>,
)
