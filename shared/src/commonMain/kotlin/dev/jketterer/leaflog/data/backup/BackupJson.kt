package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.domain.models.DomainFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

internal fun encodeBackup(backup: Backup): String = json.encodeToString(backup)

/** Reads any version this app knows, as the current one. */
internal fun decodeBackup(text: String): Backup =
    when (val version = json.decodeFromString<Header>(text).version) {
        1 -> json.decodeFromString<BackupV1>(text).toV4().toBackup().withSessionNotes()
        2, 3 -> json.decodeFromString<BackupV4>(text).toBackup().withSessionNotes()
        4 -> json.decodeFromString<BackupV4>(text).toBackup()
        // Version 5 lacked only tags' updatedAt, which defaults
        5 -> json.decodeFromString<Backup>(text).copy(version = BACKUP_VERSION)
        BACKUP_VERSION -> json.decodeFromString<Backup>(text)
        else -> throw if (version > BACKUP_VERSION) {
            DomainFailure.NewerBackup(version)
        } else {
            DomainFailure.NotABackup()
        }
    }

@Serializable
private class Header(val version: Int = 1)

/**
 * Version 4, the last with methods. Only what differs from version 5 is declared: steeps are kept
 * raw for `usedConfigurationId`, and of a method only what the conversion reads.
 */
@Serializable
private class BackupV4(
    val exportedAt: Instant,
    val teaTypes: List<BackupTeaType>,
    val vessels: List<BackupVessel>,
    val teas: List<BackupTea>,
    val steeps: List<JsonObject>,
    val methods: List<BackupV4Method>,
    val tastingTags: List<BackupTastingTag> = emptyList(),
    val preferences: BackupPreferences? = null,
)

@Serializable
private class BackupV4Method(val id: String, val sourceSessionId: String? = null)

/**
 * The v13 migration's mapping: a session on a method replays the session the method was saved
 * from, when that session is live in the file and isn't this one.
 */
private fun BackupV4.toBackup(): Backup {
    val sources = methods.associate { it.id to it.sourceSessionId }
    val decoded = steeps.map {
        json.decodeFromJsonElement<BackupSteep>(it) to it.text("usedConfigurationId")
    }
    val live = decoded.map { it.first }
        .filter { it.parentSessionId == null && it.deletedAt == null }
        .mapTo(mutableSetOf()) { it.id }
    return Backup(
        version = BACKUP_VERSION,
        exportedAt = exportedAt,
        teaTypes = teaTypes,
        vessels = vessels,
        teas = teas,
        steeps = decoded.map { (steep, method) ->
            val source = method?.let(sources::get)?.takeIf { it in live && it != steep.id }
            if (source == null || steep.planSessionId != null) {
                steep
            } else {
                steep.copy(planSessionId = source)
            }
        },
        tastingTags = tastingTags,
        preferences = preferences,
    )
}

/**
 * Version 1: the Room entities serialized as they were, schema 6 (March 2026) to 8. Only the
 * names that differ from version 4 are declared; the rest decode into version 4's types.
 */
@Serializable
private class BackupV1(
    val exportedAt: Long,
    val teaTypes: List<BackupTeaType>,
    val brewingVessels: List<BackupVessel>,
    val teas: List<BackupTea>,
    val teaSessions: List<JsonObject>,
    val brewingConfigurations: List<BackupV4Method>,
)

/** What schema 7 moved to the parent row. */
private val SESSION_FIELDS =
    listOf("teaQuantityGrams", "vesselId", "waterType", "location", "usedConfigurationId")

private fun BackupV1.toV4() = BackupV4(
    exportedAt = Instant.fromEpochMilliseconds(exportedAt),
    teaTypes = teaTypes,
    vessels = brewingVessels,
    teas = teas,
    // Before schema 7 every steep carried the session's fields; they belong to the parent only
    steeps = teaSessions.map {
        if (it.text("parentSessionId") == null) it else JsonObject(it - SESSION_FIELDS)
    },
    methods = brewingConfigurations,
)

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull

/** Before version 4, steep 1's note was the session's. The v12 migration's move. */
private fun Backup.withSessionNotes() = copy(
    steeps = steeps.map {
        if (it.parentSessionId == null) it.copy(sessionNotes = it.notes, notes = null) else it
    },
)
