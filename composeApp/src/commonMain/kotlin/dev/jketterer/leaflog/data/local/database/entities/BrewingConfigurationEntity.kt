package dev.jketterer.leaflog.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.Duration

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
    val waterQuantityMl: Int,

    @ColumnInfo(name = "temperature_celsius")
    val temperatureCelsius: Int,

    @ColumnInfo(name = "brewing_time")
    val brewingTime: Duration, // TypeConverter: Duration ↔ Long

    @ColumnInfo(name = "water_type")
    val waterType: String, // TypeConverter: WaterType ↔ String

    // Metadata
    @ColumnInfo(name = "source_session_id")
    val sourceSessionId: String, // Session this was learned from

    val rating: Float, // Rating of that session

    @ColumnInfo(name = "times_used")
    val timesUsed: Int, // How often user has used this config

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long?, // TypeConverter: Instant ↔ Long

    // User customization
    val label: String?, // e.g., "My Gong-fu Method", "Quick Western"

    @ColumnInfo(name = "is_active")
    val isActive: Boolean, // User can disable configs without deleting

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // TypeConverter: Instant ↔ Long

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long, // TypeConverter: Instant ↔ Long
)
