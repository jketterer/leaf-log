package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.SyncStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
@Entity(tableName = "tea", indices = [Index(value = ["teaTypeId"])])
data class TeaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val teaTypeId: String,

    val origin: String? = null,
    val producer: String? = null,
    val purchaseDate: LocalDate? = null,
    val purchasePrice: Double? = null,
    val stockAmount: Int? = null,
    val defaultBrewingTime: Duration? = null,
    val defaultTemperatureCelsius: Int? = null,
    val defaultQuantity: Int? = null,
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