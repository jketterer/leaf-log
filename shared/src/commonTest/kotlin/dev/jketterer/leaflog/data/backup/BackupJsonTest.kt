package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BackupJsonTest {

    @Test
    fun `a version 2 backup reads back as written`() {
        val backup = decodeBackup(V1).copy(exportedAt = Instant.fromEpochSeconds(1_790_000_000L))

        assertEquals(backup, decodeBackup(encodeBackup(backup)))
    }

    @Test
    fun `a session's source and rinse and note and tags survive a round trip`() {
        val backup = decodeBackup(V1).let {
            it.copy(
                steeps = it.steeps.mapIndexed { i, steep ->
                    if (i == 0) {
                        steep.copy(
                            planSessionId = "earlier",
                            rinsed = true,
                            sessionNotes = "Rainy morning",
                            notes = "Floral",
                            tags = listOf(BackupSteepTag("floral", confirmed = true)),
                        )
                    } else steep
                },
                tastingTags = listOf(
                    BackupTastingTag("floral", key = "floral", isCustom = false, displayOrder = 0),
                    BackupTastingTag("t1", name = "Orchid", isCustom = true, displayOrder = 23),
                ),
            )
        }

        assertEquals(backup, decodeBackup(encodeBackup(backup)))
    }

    @Test
    fun `a version 2 backup reads with no rinses or tags`() {
        val backup = decodeBackup(V1)
        val v2 = encodeBackup(backup).asVersion(2)

        assertEquals(backup, decodeBackup(v2))
        assertFalse(decodeBackup(v2).steeps.any { it.rinsed })
        assertEquals(emptyList(), decodeBackup(v2).tastingTags)
        assertEquals(emptyList(), decodeBackup(v2).steeps.flatMap { it.tags })
    }

    @Test
    fun `a version 3 backup reads steep 1's note as the session's`() {
        val backup = decodeBackup(V1).let {
            it.copy(steeps = it.steeps.mapIndexed { i, steep -> steep.copy(notes = "note ${i + 1}") })
        }
        val v3 = encodeBackup(backup).asVersion(3)

        val (parent, child) = decodeBackup(v3).steeps

        assertEquals("note 1", parent.sessionNotes)
        assertNull(parent.notes)
        assertNull(child.sessionNotes)
        assertEquals("note 2", child.notes)
    }

    @Test
    fun `a backup without a version is version 1 and reads as the current version`() {
        val backup = decodeBackup(V1)

        assertEquals(BACKUP_VERSION, backup.version)
        assertEquals(Instant.fromEpochMilliseconds(1_790_000_000_000L), backup.exportedAt)
        assertEquals(listOf("green"), backup.teaTypes.map { it.id })
        assertEquals(listOf("gaiwan"), backup.vessels.map { it.id })
        assertEquals(listOf("tieguanyin"), backup.teas.map { it.id })
        assertEquals(20.seconds, backup.steeps.first().brewingTime)
    }

    @Test
    fun `a version 1 steep 2 loses the session fields it used to copy from its parent`() {
        val (parent, child) = decodeBackup(V1).steeps

        assertEquals("gaiwan", parent.vesselId)
        assertEquals(WaterType.FILTERED, parent.waterType)
        assertNull(child.vesselId)
        assertNull(child.waterType)
        assertNull(child.teaQuantityGrams)
        assertEquals(SessionStatus.COMPLETED, child.status)
    }

    @Test
    fun `a version 4 session on a method replays the session the method was saved from`() {
        val steeps = decodeBackup(v4(v4Steep("source"), v4Steep("brewed", method = "m1"))).steeps

        assertEquals("source", steeps.last().planSessionId)
    }

    @Test
    fun `a version 4 session a method was saved from stays without a source`() {
        val backup = v4(v4Steep("source", method = "m1"), v4Steep("brewed", method = "m1"))

        assertNull(decodeBackup(backup).steeps.first().planSessionId)
    }

    @Test
    fun `a version 4 method saved from a deleted session leaves its sessions without a source`() {
        val backup = v4(v4Steep("source", deleted = true), v4Steep("brewed", method = "m1"))

        assertNull(decodeBackup(backup).steeps.last().planSessionId)
    }

    @Test
    fun `a version 4 session brewed again keeps its source`() {
        val backup = v4(v4Steep("source"), v4Steep("earlier"), v4Steep("brewed", method = "m1", plan = "earlier"))

        assertEquals("earlier", decodeBackup(backup).steeps.last().planSessionId)
    }

    @Test
    fun `settings backed up without a hemisphere leave it following the region`() {
        val preferences = PREFERENCES.copy(hemisphere = Hemisphere.SOUTHERN)
        val backup = encodeBackup(decodeBackup(V1).copy(preferences = preferences))
        val withoutIt = backup.replace(Regex(""",\s*"hemisphere": "SOUTHERN""""), "")

        assertEquals(preferences.copy(hemisphere = null), decodeBackup(withoutIt).preferences)
    }

    @Test
    fun `settings backed up with a period since removed import it as this week`() {
        val backup = encodeBackup(decodeBackup(V1).copy(preferences = PREFERENCES))
            .replace(""""analyticsPeriod": "THIS_MONTH"""", """"analyticsPeriod": "LAST_30_DAYS"""")

        val preferences = decodeBackup(backup).preferences!!.toDomain()

        assertEquals(AnalyticsPeriod.THIS_WEEK, preferences.analyticsPeriod)
    }

    @Test
    fun `a backup from a newer app is refused with a way forward`() {
        assertFailsWith<DomainFailure.NewerBackup> {
            decodeBackup("""{"version": ${BACKUP_VERSION + 1}}""")
        }
    }
}

/** An export from schema 7, backup version 1: entities as they were, extra fields and all. */
private val PREFERENCES = BackupPreferences(
    temperatureUnit = TemperatureUnit.CELSIUS,
    volumeUnit = VolumeUnit.MILLILITERS,
    weightUnit = WeightUnit.GRAMS,
    teaSortOption = TeaSortOption.NAME_ASC,
    analyticsPeriod = "THIS_MONTH",
    defaultWaterType = WaterType.FILTERED,
    timerCompletionNotificationsEnabled = true,
    sessionReminderNotificationsEnabled = true,
)

private val V1 = """
{
  "exportedAt": 1790000000000,
  "teaTypes": [{
    "id": "green", "name": "Green", "defaultTemperatureCelsius": 75.0, "colorHex": "#4CAF50",
    "isSystemDefault": true, "displayOrder": 0, "userId": null,
    "createdAt": "2026-09-01T10:00:00Z", "updatedAt": "2026-09-01T10:00:00Z"
  }],
  "brewingVessels": [{
    "id": "gaiwan", "name": "Gaiwan", "iconName": "gaiwan", "capacityMl": 120,
    "isSystemDefault": true, "displayOrder": 0, "syncStatus": "LOCAL_ONLY",
    "createdAt": "2026-09-01T10:00:00Z", "updatedAt": "2026-09-01T10:00:00Z"
  }],
  "teas": [{
    "id": "tieguanyin", "name": "Tieguanyin", "teaTypeId": "green", "purchasePrice": 12.5,
    "stockAmount": 3, "photos": ["images/tea_photos/1.jpg"], "totalSessions": 1,
    "averageRating": 4.0, "createdAt": "2026-09-01T10:00:00Z", "updatedAt": "2026-09-02T10:00:00Z"
  }],
  "teaSessions": [
    {
      "id": "s1", "teaId": "tieguanyin", "parentSessionId": null, "steepNumber": 1,
      "status": "COMPLETED", "teaQuantityGrams": 5.0, "vesselId": "gaiwan",
      "waterType": "FILTERED", "location": null, "rating": 4.0, "averageRating": 4.0,
      "usedConfigurationId": null, "timestamp": "2026-09-02T09:00:00Z", "brewingTime": "PT20S",
      "temperatureCelsius": 95.0, "waterQuantityMl": 100.0, "notes": null,
      "photos": ["images/session_photos/0.jpg"], "userId": null, "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-09-02T09:00:00Z", "updatedAt": "2026-09-02T09:00:00Z", "deletedAt": null
    },
    {
      "id": "s2", "teaId": "tieguanyin", "parentSessionId": "s1", "steepNumber": 2,
      "status": "COMPLETED", "teaQuantityGrams": 5.0, "vesselId": "gaiwan",
      "waterType": "FILTERED", "location": null, "rating": null, "averageRating": null,
      "usedConfigurationId": null, "timestamp": "2026-09-02T09:01:00Z", "brewingTime": "PT25S",
      "temperatureCelsius": 95.0, "waterQuantityMl": 100.0, "notes": null, "photos": [],
      "userId": null, "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-09-02T09:01:00Z", "updatedAt": "2026-09-02T09:01:00Z", "deletedAt": null
    }
  ],
  "brewingConfigurations": [
    {
      "id": "m1", "teaId": "tieguanyin", "vesselId": "gaiwan", "teaQuantityGrams": 5.0,
      "waterQuantityMl": 100.0, "temperatureCelsius": 95.0, "brewingTime": "PT20S",
      "waterType": "FILTERED", "sourceSessionId": "s1", "rating": 4.0, "timesUsed": 3,
      "lastUsedAt": null, "label": null, "isActive": true,
      "createdAt": "2026-09-02T09:05:00Z", "updatedAt": "2026-09-02T09:05:00Z"
    },
    {
      "id": "m2", "teaId": "tieguanyin", "vesselId": "gaiwan", "teaQuantityGrams": null,
      "waterQuantityMl": 300.0, "temperatureCelsius": 90.0, "brewingTime": "PT3M",
      "waterType": "TAP", "sourceSessionId": null, "rating": null, "timesUsed": 0,
      "lastUsedAt": null, "label": "Mug", "isActive": false,
      "createdAt": "2026-09-03T09:05:00Z", "updatedAt": "2026-09-04T09:05:00Z"
    }
  ]
}
"""

/** A backup of version 2, 3 or 4 is version 5 with methods. */
private fun String.asVersion(version: Int) =
    replace("\"version\": $BACKUP_VERSION", "\"version\": $version, \"methods\": []")

/** A version 4 backup whose method `m1` was saved from session `source`. */
private fun v4(vararg steeps: String) = """
{
  "version": 4, "exportedAt": "2026-10-01T09:00:00Z",
  "teaTypes": [], "vessels": [], "teas": [],
  "steeps": [${steeps.joinToString()}],
  "methods": [{
    "id": "m1", "teaId": "tieguanyin", "vesselId": "gaiwan", "waterQuantityMl": 100.0,
    "temperatureCelsius": 95.0, "brewingTime": "PT20S", "waterType": "FILTERED",
    "sourceSessionId": "source", "label": "Mine",
    "createdAt": "2026-09-02T09:05:00Z", "updatedAt": "2026-09-02T09:05:00Z"
  }]
}
"""

private fun v4Steep(id: String, method: String? = null, plan: String? = null, deleted: Boolean = false) = """
{
  "id": "$id", "teaId": "tieguanyin", "steepNumber": 1, "status": "COMPLETED",
  "vesselId": "gaiwan", "waterType": "FILTERED", "usedConfigurationId": ${method?.quoted()},
  "planSessionId": ${plan?.quoted()}, "timestamp": "2026-09-02T09:00:00Z", "brewingTime": "PT20S",
  "temperatureCelsius": 95.0, "waterQuantityMl": 100.0, "createdAt": "2026-09-02T09:00:00Z",
  "updatedAt": "2026-09-02T09:00:00Z", "deletedAt": ${"2026-09-03T09:00:00Z".quoted().takeIf { deleted }}
}
"""

private fun String.quoted() = "\"$this\""
