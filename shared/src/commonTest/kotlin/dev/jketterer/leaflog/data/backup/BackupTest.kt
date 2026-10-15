package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BackupTest {

    @Test
    fun `a backup whose rows all resolve keeps everything`() {
        val (kept, dropped) = backup().withoutOrphans(LocalIds())

        assertEquals(backup(), kept)
        assertEquals(0, dropped)
    }

    @Test
    fun `a session whose tea is nowhere is dropped with its steeps`() {
        val backup = backup(teas = emptyList())

        val (kept, dropped) = backup.withoutOrphans(LocalIds())

        assertEquals(emptyList(), kept.steeps)
        assertEquals(emptyList(), kept.methods)
        assertEquals(3, dropped) // two steeps and the method
    }

    @Test
    fun `a tea already in the app keeps its sessions`() {
        val (kept, dropped) = backup(teas = emptyList()).withoutOrphans(LocalIds(teas = setOf("tea")))

        assertEquals(listOf("s1", "s2"), kept.steeps.map { it.id })
        assertEquals(0, dropped)
    }

    @Test
    fun `a session whose vessel is nowhere is dropped with its steeps`() {
        val (kept, dropped) = backup(vessels = emptyList()).withoutOrphans(LocalIds())

        assertEquals(emptyList(), kept.steeps)
        assertEquals(3, dropped)
    }

    @Test
    fun `a tea whose type is nowhere takes its sessions and methods with it`() {
        val (kept, dropped) = backup(teaTypes = emptyList()).withoutOrphans(LocalIds())

        assertEquals(emptyList(), kept.teas)
        assertEquals(4, dropped)
    }

    @Test
    fun `photos are rewritten everywhere and a null drops one`() {
        val mapped = backup().mapPhotos { if (it == "gone.jpg") null else "new/$it" }

        assertEquals(listOf("new/tea.jpg"), mapped.teas.single().photos)
        assertEquals(listOf("new/steep.jpg"), mapped.steeps.first().photos)
        assertEquals(null, mapped.vessels.single().imagePath)
    }
}

private val at = Instant.fromEpochSeconds(1_790_000_000L)

private fun backup(
    teaTypes: List<BackupTeaType> = listOf(
        BackupTeaType("type", "Oolong", null, "#000000", false, 0, at, at),
    ),
    vessels: List<BackupVessel> = listOf(
        BackupVessel(
            id = "vessel",
            name = "Gaiwan",
            imagePath = "gone.jpg",
            isSystemDefault = false,
            displayOrder = 0,
            createdAt = at,
            updatedAt = at,
        ),
    ),
    teas: List<BackupTea> = listOf(
        BackupTea("tea", "Tieguanyin", "type", photos = listOf("tea.jpg"), createdAt = at, updatedAt = at),
    ),
) = Backup(
    version = BACKUP_VERSION,
    exportedAt = at,
    teaTypes = teaTypes,
    vessels = vessels,
    teas = teas,
    steeps = listOf(
        steep("s1", null, vesselId = "vessel", photos = listOf("steep.jpg")),
        steep("s2", "s1"),
    ),
    methods = listOf(
        BackupMethod(
            id = "method",
            teaId = "tea",
            vesselId = "vessel",
            waterQuantityMl = 100.0,
            temperatureCelsius = 95.0,
            brewingTime = 20.seconds,
            waterType = WaterType.FILTERED,
            createdAt = at,
            updatedAt = at,
        ),
    ),
)

private fun steep(
    id: String,
    parentSessionId: String?,
    vesselId: String? = null,
    photos: List<String> = emptyList(),
) = BackupSteep(
    id = id,
    teaId = "tea",
    parentSessionId = parentSessionId,
    steepNumber = if (parentSessionId == null) 1 else 2,
    status = SessionStatus.COMPLETED,
    vesselId = vesselId,
    timestamp = at,
    brewingTime = 20.seconds,
    temperatureCelsius = 95.0,
    waterQuantityMl = 100.0,
    photos = photos,
    createdAt = at,
    updatedAt = at,
)
