package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The backup format, version 2: `data.json` in the archive. Frozen, like a migration: a change
 * here is version 3, with a reader for this one.
 *
 * It carries facts only. Stats are recomputed on import, and deleted rows travel so that what
 * points at them still restores. Photo paths name entries in the archive.
 */
@Serializable
data class Backup(
    // No default: a default isn't encoded, and a missing version reads as 1
    val version: Int,
    val exportedAt: Instant,
    val teaTypes: List<BackupTeaType>,
    val vessels: List<BackupVessel>,
    val teas: List<BackupTea>,
    /** Rows of `tea_session`: a session is its steep 1, the parent of steeps 2…n. */
    val steeps: List<BackupSteep>,
    val methods: List<BackupMethod>,
)

const val BACKUP_VERSION = 2

// Field names match the entities, so version 1 (the entities themselves) decodes into these.
// Defaults cover what version 1 lacked.

@Serializable
data class BackupTeaType(
    val id: String,
    val name: String,
    val defaultTemperatureCelsius: Double? = null,
    val colorHex: String,
    val isSystemDefault: Boolean,
    val displayOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

@Serializable
data class BackupVessel(
    val id: String,
    val name: String,
    val iconName: String? = null,
    val imagePath: String? = null,
    val capacityMl: Int? = null,
    val isSystemDefault: Boolean,
    val isArchived: Boolean = false,
    val displayOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

@Serializable
data class BackupTea(
    val id: String,
    val name: String,
    val teaTypeId: String,
    val origin: String? = null,
    val producer: String? = null,
    val purchaseDate: LocalDate? = null,
    val defaultTemperatureCelsius: Double? = null,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val finishedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

@Serializable
data class BackupSteep(
    val id: String,
    val teaId: String,
    val parentSessionId: String? = null,
    val steepNumber: Int,
    val status: SessionStatus,
    // Session fields: set on the parent row, null on its children
    val teaQuantityGrams: Float? = null,
    val vesselId: String? = null,
    val waterType: WaterType? = null,
    val location: String? = null,
    val usedConfigurationId: String? = null,
    val rating: Float? = null,
    val timestamp: Instant,
    val brewingTime: Duration,
    val temperatureCelsius: Double,
    val waterQuantityMl: Double,
    val notes: String? = null,
    val photos: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val timerStatus: TimerStatus? = null,
    val timerStartedAt: Instant? = null,
    val timerPausedAt: Instant? = null,
    val timerRemainingMs: Long? = null,
    val timerTotalMs: Long? = null,
    val actualBrewingTimeMs: Long? = null,
)

@Serializable
data class BackupMethod(
    val id: String,
    val teaId: String,
    val vesselId: String,
    val teaQuantityGrams: Float? = null,
    val waterQuantityMl: Double,
    val temperatureCelsius: Double,
    val brewingTime: Duration,
    val waterType: WaterType,
    val sourceSessionId: String? = null,
    val rating: Float? = null,
    val label: String? = null,
    val isPinned: Boolean = false,
    val pinnedSortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

/** Rewrites every photo path; a null drops that photo. */
internal fun Backup.mapPhotos(transform: (String) -> String?) = copy(
    vessels = vessels.map { it.copy(imagePath = it.imagePath?.let(transform)) },
    teas = teas.map { it.copy(photos = it.photos.mapNotNull(transform)) },
    steeps = steeps.map { it.copy(photos = it.photos.mapNotNull(transform)) },
)

/**
 * Leaves out rows whose tea, type or vessel is neither in the backup nor among [local]'s ids,
 * and what hangs off them. Version 1 never carried deleted rows, so it can hold sessions of a tea
 * deleted before it was made. Returns what's left and how many rows were dropped.
 */
internal fun Backup.withoutOrphans(local: LocalRows): Pair<Backup, Int> {
    val typeIds = local.teaTypes.keys + teaTypes.map { it.id }
    val vesselIds = local.vessels.keys + vessels.map { it.id }
    val keptTeas = teas.filter { it.teaTypeId in typeIds }
    val teaIds = local.teas.keys + keptTeas.map { it.id }
    val keptParents = steeps
        .filter { it.parentSessionId == null }
        .filter { it.teaId in teaIds && (it.vesselId == null || it.vesselId in vesselIds) }
        .map { it.id }
        .toSet()
    val keptSteeps = steeps.filter { (it.parentSessionId ?: it.id) in keptParents }
    val keptMethods = methods.filter { it.teaId in teaIds && it.vesselId in vesselIds }
    val kept = copy(teas = keptTeas, steeps = keptSteeps, methods = keptMethods)
    val dropped = (teas.size - keptTeas.size) + (steeps.size - keptSteeps.size) +
        (methods.size - keptMethods.size)
    return kept to dropped
}

/**
 * The rows an import writes: those newer than their copy here, or new to it. A tie is skipped, so
 * importing twice changes nothing. A session being brewed here is left alone, and one that was
 * being brewed in the backup lands completed, since its timer didn't come with it.
 */
internal fun planMerge(incoming: Backup, local: LocalRows): Backup {
    val brewing = incoming.steeps
        .filter { it.parentSessionId == null && it.status == SessionStatus.IN_PROGRESS }
        .mapTo(mutableSetOf()) { it.id }
    return incoming.copy(
        teaTypes = incoming.teaTypes.newerThan(local.teaTypes) { it.id to it.updatedAt },
        vessels = incoming.vessels.newerThan(local.vessels) { it.id to it.updatedAt },
        teas = incoming.teas.newerThan(local.teas) { it.id to it.updatedAt },
        steeps = incoming.steeps
            .filter { (it.parentSessionId ?: it.id) !in local.inProgressSessions }
            .newerThan(local.steeps) { it.id to it.updatedAt }
            .map { if ((it.parentSessionId ?: it.id) in brewing) it.landedCompleted() else it },
        methods = incoming.methods.newerThan(local.methods) { it.id to it.updatedAt },
    )
}

private inline fun <T> List<T>.newerThan(
    local: Map<String, Instant>,
    version: (T) -> Pair<String, Instant>,
) = filter { row ->
    val (id, updatedAt) = version(row)
    local[id]?.let { updatedAt > it } ?: true
}

/** A completed timer is a record of the steep; any other was running on the backed-up device. */
private fun BackupSteep.landedCompleted() = copy(status = SessionStatus.COMPLETED).let {
    if (timerStatus == TimerStatus.COMPLETE) it else it.copy(
        timerStatus = null,
        timerStartedAt = null,
        timerPausedAt = null,
        timerRemainingMs = null,
        timerTotalMs = null,
    )
}

/** Each row already in the database, deleted ones included, by id: its `updatedAt`. */
internal data class LocalRows(
    val teaTypes: Map<String, Instant> = emptyMap(),
    val vessels: Map<String, Instant> = emptyMap(),
    val teas: Map<String, Instant> = emptyMap(),
    val steeps: Map<String, Instant> = emptyMap(),
    val methods: Map<String, Instant> = emptyMap(),
    /** The ids of live sessions being brewed. */
    val inProgressSessions: Set<String> = emptySet(),
)
