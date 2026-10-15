package dev.jketterer.leaflog.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

internal fun encodeBackup(backup: Backup): String = json.encodeToString(backup)

/** Reads any version this app knows, as the current one. */
internal fun decodeBackup(text: String): Backup =
    when (val version = json.decodeFromString<Header>(text).version) {
        1 -> json.decodeFromString<BackupV1>(text).toBackup()
        BACKUP_VERSION -> json.decodeFromString<Backup>(text)
        else -> throw IllegalArgumentException(
            if (version > BACKUP_VERSION) {
                "This backup is from a newer version of Leaf Log. Update the app to restore it."
            } else {
                "This file isn't a Leaf Log backup."
            },
        )
    }

@Serializable
private class Header(val version: Int = 1)

/**
 * Version 1: the Room entities serialized as they were, schema 6 (March 2026) to 8. Only the
 * names that differ from version 2 are declared; the rest decode into version 2's types.
 */
@Serializable
private class BackupV1(
    val exportedAt: Long,
    val teaTypes: List<BackupTeaType>,
    val brewingVessels: List<BackupVessel>,
    val teas: List<BackupTea>,
    val teaSessions: List<BackupSteep>,
    /** Kept raw for `isActive`, which schema 8 replaced with `deletedAt`. */
    val brewingConfigurations: List<JsonObject>,
)

private fun BackupV1.toBackup() = Backup(
    version = BACKUP_VERSION,
    exportedAt = Instant.fromEpochMilliseconds(exportedAt),
    teaTypes = teaTypes,
    vessels = brewingVessels,
    teas = teas,
    // Before schema 7 every steep carried the session's fields; they belong to the parent only
    steeps = teaSessions.map {
        if (it.parentSessionId == null) {
            it
        } else {
            it.copy(
                teaQuantityGrams = null,
                vesselId = null,
                waterType = null,
                location = null,
                usedConfigurationId = null,
            )
        }
    },
    methods = brewingConfigurations.map { raw ->
        val method = json.decodeFromJsonElement<BackupMethod>(raw)
        val isActive = raw["isActive"]?.jsonPrimitive?.booleanOrNull ?: true
        // The v8 migration's mapping
        if (isActive || method.deletedAt != null) method else method.copy(deletedAt = method.updatedAt)
    },
)
