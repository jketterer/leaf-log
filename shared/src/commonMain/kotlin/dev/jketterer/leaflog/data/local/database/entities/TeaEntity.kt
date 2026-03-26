package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SyncStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@Entity(
    tableName = "tea",
    foreignKeys = [
        ForeignKey(
            entity = TeaTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["teaTypeId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [
        Index(value = ["teaTypeId"]),
        Index(value = ["name"]),
        Index(value = ["isFavorite"]),
        Index(value = ["userId"]),
        Index(value = ["deletedAt"]),
        Index(value = ["lastBrewedAt"]),
    ]
)
data class TeaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val teaTypeId: String,

    val origin: String? = null,
    val producer: String? = null,
    val purchaseDate: LocalDate? = null,
    val purchasePrice: Double? = null,
    val stockAmount: Int? = null,
    val defaultTemperatureCelsius: Double? = null,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val totalSessions: Int = 0,
    val averageRating: Float? = null,
    val lastBrewedAt: Instant? = null,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)