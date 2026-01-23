package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
@Entity(tableName = "tea_session", indices = [Index(value = ["teaId"])])
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