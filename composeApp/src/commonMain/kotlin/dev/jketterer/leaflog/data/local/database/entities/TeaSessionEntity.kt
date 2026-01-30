package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
@Entity(
    tableName = "tea_session",
    foreignKeys = [
        ForeignKey(
            entity = TeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["teaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BrewingVesselEntity::class,
            parentColumns = ["id"],
            childColumns = ["vesselId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TeaSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["teaId"]),
        Index(value = ["vesselId"]),
        Index(value = ["parentSessionId"]),
        Index(value = ["steepNumber"]),
        Index(value = ["status"]),
        Index(value = ["timestamp"]),
        Index(value = ["userId"]),
        Index(value = ["deletedAt"]),
    ]
)
data class TeaSessionEntity(
    @PrimaryKey val id: String,
    val teaId: String,

    val parentSessionId: String?,
    val steepNumber: Int,
    val status: SessionStatus,

    val teaQuantityGrams: Float?,
    val vesselId: String,
    val waterType: WaterType,
    val location: String?,
    val rating: Float?,
    val averageRating: Float?,

    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Int,
    val waterQuantityMl: Int,
    val notes: String?,
    val photos: List<String>,

    val userId: String?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)