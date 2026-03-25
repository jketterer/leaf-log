package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
@Entity(
    tableName = "brewing_configurations",
    foreignKeys = [
        ForeignKey(
            entity = TeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tea_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BrewingVesselEntity::class,
            parentColumns = ["id"],
            childColumns = ["vessel_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tea_id"),
        Index("vessel_id"),
        Index(value = ["tea_id", "vessel_id"]), // Composite index for lookups
        Index("rating"),
        Index("times_used"),
        Index("last_used_at"),
        Index("is_pinned"),
    ],
)
data class BrewingConfigurationEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "tea_id")
    val teaId: String,

    @ColumnInfo(name = "vessel_id")
    val vesselId: String,

    // Brewing parameters (from a successful session)
    @ColumnInfo(name = "tea_quantity_grams")
    val teaQuantityGrams: Float?,

    @ColumnInfo(name = "water_quantity_ml")
    val waterQuantityMl: Double,

    @ColumnInfo(name = "temperature_celsius")
    val temperatureCelsius: Double,

    @ColumnInfo(name = "brewing_time")
    val brewingTime: Duration, // TypeConverter: Duration ↔ Long

    @ColumnInfo(name = "water_type")
    val waterType: WaterType, // TypeConverter: WaterType ↔ String

    // Metadata
    @ColumnInfo(name = "source_session_id")
    val sourceSessionId: String?, // Session this was learned from (null for manually created)

    val rating: Float?, // Rating of that session (null for manually created)

    @ColumnInfo(name = "times_used")
    val timesUsed: Int, // How often user has used this config

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Instant?, // TypeConverter: Instant ↔ Long

    // User customization
    val label: String?, // e.g., "My Gong-fu Method", "Quick Western"

    @ColumnInfo(name = "is_active")
    val isActive: Boolean, // User can disable configs without deleting

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false, // User can pin configs to the top of the Quick Brew section

    @ColumnInfo(name = "pinned_sort_order", defaultValue = "0")
    val pinnedSortOrder: Int = 0, // Order of pinned configs (lower = first)

    @ColumnInfo(name = "created_at")
    val createdAt: Instant, // TypeConverter: Instant ↔ Long

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant, // TypeConverter: Instant ↔ Long
)
