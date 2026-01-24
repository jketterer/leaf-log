package dev.jketterer.leaflog.data.local.database

import androidx.room.TypeConverter
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun toTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun fromTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun toDuration(value: Long?): Duration? {
        return value?.milliseconds
    }

    @TypeConverter
    fun fromDuration(duration: Duration?): Long? {
        return duration?.inWholeMilliseconds
    }

    @TypeConverter
    fun fromLocalDate(localDate: LocalDate?): Long? {
        return localDate?.toEpochDays()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.fromEpochDays(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.let { json.decodeFromString(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromWaterType(waterType: WaterType?): String? {
        return waterType?.name
    }

    @TypeConverter
    fun toWaterType(value: String?): WaterType? {
        return WaterType.entries.firstOrNull { it.name == value }
    }

    @TypeConverter
    fun fromSyncStatus(syncStatus: SyncStatus?): String? {
        return syncStatus?.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? {
        return SyncStatus.entries.firstOrNull { it.name == value }
    }

    @TypeConverter
    fun fromSessionStatus(sessionStatus: SessionStatus?): String? {
        return sessionStatus?.name
    }

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus? {
        return SessionStatus.entries.firstOrNull { it.name == value }
    }
}