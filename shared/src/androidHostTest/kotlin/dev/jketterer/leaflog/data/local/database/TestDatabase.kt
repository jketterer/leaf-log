package dev.jketterer.leaflog.data.local.database

import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Room only asks the context for system services, and copes with none
internal fun inMemoryDatabase(): LeafLogDatabase = Room.inMemoryDatabaseBuilder<LeafLogDatabase>(
    object : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? = null
    },
).setDriver(BundledSQLiteDriver()).build()

/** The tea, tea type and vessels that [row]s point at. */
internal suspend fun LeafLogDatabase.seedForeignKeys() {
    useWriterConnection { connection ->
        listOf(
            "INSERT INTO tea_type (id, name, colorHex, isSystemDefault, displayOrder, " +
                "createdAt, updatedAt) VALUES ('type', 'Oolong', '#000000', 0, 0, 0, 0)",
            "INSERT INTO tea (id, name, teaTypeId, photos, isFavorite, totalSessions, " +
                "createdAt, updatedAt, syncStatus) " +
                "VALUES ('tea', 'Tieguanyin', 'type', '[]', 0, 0, 0, 0, 'LOCAL_ONLY')",
        ).plus(
            listOf("vessel", "gaiwan", "mug").map {
                "INSERT INTO brewing_vessel (id, name, isSystemDefault, isArchived, " +
                    "displayOrder, createdAt, updatedAt, syncStatus) " +
                    "VALUES ('$it', '$it', 0, 0, 0, 0, 0, 'LOCAL_ONLY')"
            },
        ).forEach { sql -> connection.usePrepared(sql) { it.step() } }
    }
}

internal fun at(minute: Int) = Instant.fromEpochSeconds(1_790_000_000L) + minute.minutes

internal fun row(
    id: String,
    parentSessionId: String?,
    steepNumber: Int,
    status: SessionStatus,
    vesselId: String,
    timestamp: Instant,
) = TeaSessionEntity(
    id = id,
    teaId = "tea",
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = status,
    teaQuantityGrams = 5f,
    vesselId = vesselId,
    waterType = WaterType.FILTERED,
    location = null,
    rating = null,
    usedConfigurationId = null,
    timestamp = timestamp,
    brewingTime = 20.seconds,
    temperatureCelsius = 95.0,
    waterQuantityMl = 100.0,
    notes = null,
    photos = emptyList(),
    userId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    createdAt = timestamp,
    updatedAt = timestamp,
    deletedAt = null,
)
