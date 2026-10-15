package dev.jketterer.leaflog.data.repositories

import android.content.ContextWrapper
import dev.jketterer.leaflog.data.backup.BACKUP_VERSION
import dev.jketterer.leaflog.data.backup.Backup
import dev.jketterer.leaflog.data.backup.mapPhotos
import dev.jketterer.leaflog.data.backup.toBackup
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.data.local.database.RoomTransactor
import dev.jketterer.leaflog.data.local.database.at
import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.data.local.database.inMemoryDatabase
import dev.jketterer.leaflog.data.local.database.row
import dev.jketterer.leaflog.data.local.database.seedForeignKeys
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BackupRepositoryImplTest {

    private val fileSystem = FileSystem.SYSTEM
    private val root = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "backup-test-${Random.nextLong()}")
        .also { fileSystem.createDirectories(it) }
    private val photo = Random(1).nextBytes(5_000)
    private val photoName = "photos/${photo.toByteString().sha256().hex()}.jpg"
    private val databases = mutableListOf<LeafLogDatabase>()

    @AfterTest
    fun cleanUp() {
        databases.forEach { it.close() }
        fileSystem.deleteRecursively(root)
    }

    @Test
    fun `a full export restores onto a fresh install`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val zip = phone.repository.export().getOrThrow()

        val fresh = Device("fresh")
        val result = fresh.repository.import(zip).getOrThrow()

        val expected = phone.snapshot().mapPhotos { photoName }
        assertEquals(expected, fresh.snapshot())
        assertContentEquals(photo, fresh.read(photoName))
        assertEquals(0, result.skipped)
        // Stats are recomputed rather than carried
        assertEquals(1, fresh.database.teaDao().getById("tea")?.totalSessions)
        assertEquals(1, fresh.database.brewingConfigurationDao().getById("method")?.timesUsed)
    }

    @Test
    fun `a preview changes nothing and counts what the import then does`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val zip = phone.repository.export().getOrThrow()
        val fresh = Device("fresh")

        val preview = fresh.repository.preview(zip).getOrThrow()

        assertEquals(emptyList(), fresh.database.teaDao().getAllIncludingDeleted())
        assertTrue(fileSystem.exists(zip.toPath()), "kept for the import")
        // Type, tea, two vessels, two steeps and the method; the mug arrives deleted
        assertEquals(7, preview.added)
        assertEquals(preview, fresh.repository.import(zip).getOrThrow())
    }

    @Test
    fun `a discarded preview deletes the picked file`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val zip = phone.repository.export().getOrThrow()

        phone.repository.preview(zip).getOrThrow()
        phone.repository.discard(zip)

        assertFalse(fileSystem.exists(zip.toPath()))
    }

    @Test
    fun `importing the same backup twice changes nothing`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val fresh = Device("fresh")
        fresh.repository.import(phone.repository.export().getOrThrow()).getOrThrow()
        val once = fresh.snapshot()

        val result = fresh.repository.import(phone.repository.export().getOrThrow()).getOrThrow()

        assertEquals(once, fresh.snapshot())
        assertTrue(result.changesNothing)
    }

    @Test
    fun `an older backup doesn't revert what changed since`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val old = phone.repository.export().getOrThrow()
        val tea = assertNotNull(phone.database.teaDao().getById("tea"))
        phone.database.teaDao().upsert(tea.copy(name = "Renamed", updatedAt = at(5)))
        phone.database.teaSessionDao().softDelete("s2", timestamp = at(5).toEpochMilliseconds())

        phone.repository.import(old).getOrThrow()

        assertEquals("Renamed", phone.database.teaDao().getById("tea")?.name)
        assertNotNull(phone.database.teaSessionDao().getById("s2")?.deletedAt)
    }

    @Test
    fun `a newer backup's delete lands`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val fresh = Device("fresh")
        fresh.repository.import(phone.repository.export().getOrThrow()).getOrThrow()
        phone.database.brewingConfigurationDao()
            .softDelete("method", timestamp = at(5).toEpochMilliseconds())

        fresh.repository.import(phone.repository.export().getOrThrow()).getOrThrow()

        assertNull(fresh.database.brewingConfigurationDao().getById("method"))
    }

    @Test
    fun `a session being brewed here is left alone`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val s1 = assertNotNull(phone.database.teaSessionDao().getById("s1"))
        phone.database.teaSessionDao().upsert(s1.copy(notes = "edited", updatedAt = at(5)))
        val brewing = Device("brewing")
        brewing.seedJournal()
        brewing.database.teaSessionDao().upsert(s1.copy(status = SessionStatus.IN_PROGRESS))

        brewing.repository.import(phone.repository.export().getOrThrow()).getOrThrow()

        val kept = assertNotNull(brewing.database.teaSessionDao().getById("s1"))
        assertEquals(SessionStatus.IN_PROGRESS, kept.status)
        assertNull(kept.notes)
    }

    @Test
    fun `a finished tea comes back finished with its sessions named`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        phone.database.teaDao().setFinishedAt("tea", finishedAt = 9, updatedAt = 9)

        val fresh = Device("fresh")
        fresh.repository.import(phone.repository.export().getOrThrow()).getOrThrow()

        val tea = assertNotNull(fresh.database.teaDao().getById("tea"))
        assertNotNull(tea.finishedAt)
        assertEquals("tea", fresh.database.teaSessionDao().getAllSessions().first().teaId)
    }

    @Test
    fun `a version 1 backup restores the sessions of a tea this device deleted and skips the rest`() =
        runTest {
            val device = Device("device")
            device.database.seedForeignKeys()
            device.database.teaDao().softDelete("tea", timestamp = 5)
            val zip = device.zip(
                "data.json" to V1.encodeToByteArray(),
                "images/session_photos/0.jpg" to photo,
            )

            val result = device.repository.import(zip).getOrThrow()

            // The tea was deleted with live sessions: it comes back finished
            val tea = assertNotNull(device.database.teaDao().getById("tea"))
            assertEquals(5L, tea.finishedAt?.toEpochMilliseconds())
            assertEquals(listOf(photoName), device.database.teaSessionDao().getById("s1")?.photos)
            // The other tea is in neither the backup nor the app
            assertNull(device.database.teaSessionDao().getById("orphan"))
            assertEquals(1, result.skipped)
        }

    @Test
    fun `a backup from a newer app is refused and changes nothing`() = runTest {
        val device = Device("device")
        val zip = device.zip("data.json" to """{"version": ${BACKUP_VERSION + 1}}""".encodeToByteArray())

        val result = device.repository.import(zip)

        assertTrue("newer version" in result.exceptionOrNull()?.message.orEmpty())
        assertEquals(emptyList(), device.database.teaDao().getAllIncludingDeleted())
        assertFalse(fileSystem.exists(zip.toPath()), "the picker's copy is cleaned up")
    }

    @Test
    fun `a file that isn't a zip is refused`() = runTest {
        val device = Device("device")
        val file = device.cache / "notes.txt"
        fileSystem.write(file) { writeUtf8("hello") }

        val result = device.repository.import(file.toString())

        assertTrue("isn't a Leaf Log backup" in result.exceptionOrNull()?.message.orEmpty())
    }

    @Test
    fun `a failed import leaves the database and photo files as they were`() = runTest {
        val phone = Device("phone")
        phone.seedJournal()
        val zip = phone.repository.export().getOrThrow()
        val fresh = Device("fresh") { real ->
            object : Transactor {
                override suspend fun <T> invoke(block: suspend () -> T): T =
                    real { block(); error("disk full") }
            }
        }

        val result = fresh.repository.import(zip)

        assertEquals("disk full", result.exceptionOrNull()?.message)
        assertEquals(emptyList(), fresh.database.teaDao().getAllIncludingDeleted())
        assertEquals(emptyList(), fileSystem.listOrNull(fresh.files / "photos").orEmpty())
    }

    @Test
    fun `an entry named to escape the archive lands in the photo directory`() = runTest {
        val device = Device("device")
        device.database.seedForeignKeys()
        val escape = "../../escape.jpg"
        val zip = device.zip(
            "data.json" to v1WithSessionPhoto(escape).encodeToByteArray(),
            escape to photo,
        )

        device.repository.import(zip).getOrThrow()

        assertEquals(listOf(photoName), device.database.teaSessionDao().getById("s1")?.photos)
        assertContentEquals(photo, device.read(photoName))
        assertEquals(listOf("device"), fileSystem.list(root).map { it.name })
    }

    /** An app install: its own database, files and cache directories. */
    private inner class Device(
        name: String,
        transactor: (Transactor) -> Transactor = { it },
    ) {
        val database = inMemoryDatabase().also { databases += it }
        val files: Path = root / name / "files"
        val cache: Path = root / name / "cache"
        private val storage = ImageStorage(
            object : ContextWrapper(null) {
                override fun getFilesDir() = files.toFile().apply { mkdirs() }
                override fun getCacheDir() = cache.toFile().apply { mkdirs() }
            },
        ).also { fileSystem.createDirectories(cache) }

        val repository = BackupRepositoryImpl(
            teaTypeDao = database.teaTypeDao(),
            vesselDao = database.brewingVesselDao(),
            teaDao = database.teaDao(),
            sessionDao = database.teaSessionDao(),
            methodDao = database.brewingConfigurationDao(),
            transactor = transactor(RoomTransactor(database)),
            imageStorage = storage,
            fileSystem = fileSystem,
        )

        fun read(stored: String) = fileSystem.read(files / stored) { readByteArray() }

        /** A gaiwan session of two steeps with a photo, its method, and a deleted vessel. */
        suspend fun seedJournal() {
            database.seedForeignKeys()
            fileSystem.createDirectories(files / "session_images")
            fileSystem.write(files / "session_images" / "a.jpg") { write(photo) }
            database.teaSessionDao().upsertAll(
                listOf(
                    row("s1", null, 1, SessionStatus.COMPLETED, "gaiwan", at(0))
                        .copy(photos = listOf("session_images/a.jpg"), usedConfigurationId = "method"),
                    row("s2", "s1", 2, SessionStatus.COMPLETED, "gaiwan", at(1)).copy(
                        teaQuantityGrams = null,
                        vesselId = null,
                        waterType = null,
                    ),
                ),
            )
            database.brewingConfigurationDao().upsert(
                BrewingConfigurationEntity(
                    id = "method",
                    teaId = "tea",
                    vesselId = "gaiwan",
                    teaQuantityGrams = 5f,
                    waterQuantityMl = 100.0,
                    temperatureCelsius = 95.0,
                    brewingTime = 20.seconds,
                    waterType = WaterType.FILTERED,
                    sourceSessionId = "s1",
                    rating = null,
                    timesUsed = 0,
                    lastUsedAt = null,
                    label = null,
                    createdAt = at(0),
                    updatedAt = at(0),
                ),
            )
            database.teaSessionDao().recomputeTeaStats()
            database.teaSessionDao().recomputeConfigurationStats()
            database.brewingVesselDao().softDelete("mug", timestamp = 3)
        }

        /** Every row, deleted ones too, as it would be backed up. */
        suspend fun snapshot() = Backup(
            version = BACKUP_VERSION,
            exportedAt = at(0),
            teaTypes = database.teaTypeDao().getAllIncludingDeleted().map { it.toBackup() }
                .sortedBy { it.id },
            vessels = database.brewingVesselDao().getAllIncludingDeleted().map { it.toBackup() }
                .sortedBy { it.id },
            teas = database.teaDao().getAllIncludingDeleted().map { it.toBackup() }
                .sortedBy { it.id },
            steeps = database.teaSessionDao().getAllIncludingDeleted().map { it.toBackup() }
                .sortedBy { it.id },
            methods = database.brewingConfigurationDao().getAllIncludingDeleted()
                .map { it.toBackup() }
                .sortedBy { it.id },
        )

        /** Writes a ZIP with java.util.zip's defaults: DEFLATE, as Android wrote backup version 1. */
        fun zip(vararg entries: Pair<String, ByteArray>): String {
            val path = cache / "import.zip"
            ZipOutputStream(fileSystem.sink(path).buffer().outputStream()).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                }
            }
            return path.toString()
        }
    }
}

/** Sessions for the seeded tea, and one for a tea in neither this backup nor the app. */
private val V1 = """
{
  "exportedAt": 1790000000000,
  "teaTypes": [], "brewingVessels": [], "teas": [],
  "teaSessions": [
    ${v1Session("s1", "tea", "images/session_photos/0.jpg")},
    ${v1Session("orphan", "gone", null)}
  ],
  "brewingConfigurations": []
}
"""

private fun v1WithSessionPhoto(photo: String) = """
{
  "exportedAt": 1790000000000,
  "teaTypes": [], "brewingVessels": [], "teas": [],
  "teaSessions": [${v1Session("s1", "tea", photo)}],
  "brewingConfigurations": []
}
"""

private fun v1Session(id: String, teaId: String, photo: String?) = """
    {
      "id": "$id", "teaId": "$teaId", "parentSessionId": null, "steepNumber": 1,
      "status": "COMPLETED", "teaQuantityGrams": 5.0, "vesselId": "gaiwan",
      "waterType": "FILTERED", "rating": 4.0, "timestamp": "2026-09-02T09:00:00Z",
      "brewingTime": "PT20S", "temperatureCelsius": 95.0, "waterQuantityMl": 100.0,
      "photos": [${photo?.let { "\"$it\"" } ?: ""}], "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-09-02T09:00:00Z", "updatedAt": "2026-09-02T09:00:00Z"
    }
"""
