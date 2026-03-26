package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@Entity(
    tableName = "tea_type",
    indices = [
        Index("displayOrder"),
        Index("userId"),
        Index("deletedAt"),
    ]
)
data class TeaTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultTemperatureCelsius: Double?,
    val colorHex: String,
    val isSystemDefault: Boolean,
    val displayOrder: Int,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)