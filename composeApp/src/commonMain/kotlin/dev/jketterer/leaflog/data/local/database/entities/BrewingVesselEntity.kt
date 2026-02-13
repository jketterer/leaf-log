package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SyncStatus
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@Entity(
    tableName = "brewing_vessel",
    indices = [
        Index("displayOrder"),
        Index("userId"),
        Index("deletedAt"),
    ]
)
data class BrewingVesselEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String? = null,
    val imagePath: String? = null,
    val capacityMl: Int? = null,
    val isSystemDefault: Boolean,
    val displayOrder: Int,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)