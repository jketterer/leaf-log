package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TimerStatus
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
        Index(value = ["usedConfigurationId"]),
    ]
)
data class TeaSessionEntity(
    @PrimaryKey val id: String,
    val teaId: String,

    val parentSessionId: String?,
    val steepNumber: Int,
    val status: SessionStatus,

    // Session fields: set on the parent row, NULL on its children
    val teaQuantityGrams: Float?,
    val vesselId: String?,
    val waterType: WaterType?,
    val location: String?,
    val usedConfigurationId: String?, // Configuration that was used for this session
    val planSessionId: String? = null, // Session Brew Again copied; no FK, it may be gone

    val rating: Float?,

    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Double,
    val waterQuantityMl: Double,
    val notes: String?,
    val photos: List<String>,

    val userId: String?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,

    // Timer state persistence fields (null = timer not started)
    val timerStatus: TimerStatus? = null,
    val timerStartedAt: Instant? = null,
    val timerPausedAt: Instant? = null,
    val timerRemainingMs: Long? = null,
    val timerTotalMs: Long? = null,

    // Brewed time once the timer completed; null without a timer record
    val actualBrewingTimeMs: Long? = null,
)