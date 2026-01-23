package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@Entity(tableName = "brewing_vessel")
data class BrewingVesselEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String? = null,
    val isSystemDefault: Boolean,
    val displayOrder: Int,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)