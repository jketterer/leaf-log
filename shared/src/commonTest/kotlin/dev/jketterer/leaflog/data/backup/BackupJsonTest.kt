package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
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
    fun `a method's plan and rinse and a session's source, rinse and tags survive a round trip`() {
        val backup = decodeBackup(V1).let {
            it.copy(
                methods = listOf(
                    METHOD.copy(steepIncrement = 5.seconds, steepOverrides = mapOf(4 to 45.seconds), rinse = true),
                ),
                steeps = it.steeps.mapIndexed { i, steep ->
                    if (i == 0) {
                        steep.copy(
                            planSessionId = "earlier",
                            rinsed = true,
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
    fun `a version 2 backup reads with no plans, rinses or tags`() {
        val backup = decodeBackup(V1).copy(methods = listOf(METHOD))
        val v2 = encodeBackup(backup).replace("\"version\": 3", "\"version\": 2")

        assertEquals(backup.copy(version = 2), decodeBackup(v2))
        assertNull(decodeBackup(v2).methods.single().steepIncrement)
        assertFalse(decodeBackup(v2).methods.single().rinse)
        assertEquals(emptyList(), decodeBackup(v2).tastingTags)
        assertEquals(emptyList(), decodeBackup(v2).steeps.flatMap { it.tags })
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
    fun `a version 1 inactive method arrives deleted at its last update`() {
        val (active, inactive) = decodeBackup(V1).methods

        assertNull(active.deletedAt)
        assertEquals(inactive.updatedAt, inactive.deletedAt)
    }

    @Test
    fun `a backup from a newer app is refused with a way forward`() {
        assertFailsWith<DomainFailure.NewerBackup> {
            decodeBackup("""{"version": ${BACKUP_VERSION + 1}}""")
        }
    }
}

/** An export from schema 7, backup version 1: entities as they were, extra fields and all. */
private val METHOD = BackupMethod(
    id = "method",
    teaId = "tieguanyin",
    vesselId = "gaiwan",
    waterQuantityMl = 100.0,
    temperatureCelsius = 95.0,
    brewingTime = 20.seconds,
    waterType = WaterType.FILTERED,
    createdAt = Instant.fromEpochSeconds(0),
    updatedAt = Instant.fromEpochSeconds(0),
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
