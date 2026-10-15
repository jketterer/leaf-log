package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.backup.BACKUP_VERSION
import dev.jketterer.leaflog.data.backup.Backup
import dev.jketterer.leaflog.data.backup.LocalIds
import dev.jketterer.leaflog.data.backup.decodeBackup
import dev.jketterer.leaflog.data.backup.encodeBackup
import dev.jketterer.leaflog.data.backup.mapPhotos
import dev.jketterer.leaflog.data.backup.toBackup
import dev.jketterer.leaflog.data.backup.toEntity
import dev.jketterer.leaflog.data.backup.withoutOrphans
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipWriter
import dev.jketterer.leaflog.data.local.database.dao.BrewingConfigurationDao
import dev.jketterer.leaflog.data.local.database.dao.BrewingVesselDao
import dev.jketterer.leaflog.data.local.database.dao.TeaDao
import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.repositories.BackupRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import kotlin.random.Random
import kotlin.time.Clock

/** Where restored photos go, named by content so a re-import never duplicates one. */
private const val PHOTO_DIR = "photos"

class BackupRepositoryImpl(
    private val teaTypeDao: TeaTypeDao,
    private val vesselDao: BrewingVesselDao,
    private val teaDao: TeaDao,
    private val sessionDao: TeaSessionDao,
    private val methodDao: BrewingConfigurationDao,
    private val transactor: Transactor,
    private val imageStorage: ImageStorage,
    private val fileSystem: FileSystem,
) : BackupRepository {

    override suspend fun export(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // One transaction, so a steep saved mid-export can't land half in the snapshot
            val snapshot = transactor {
                Backup(
                    version = BACKUP_VERSION,
                    exportedAt = Clock.System.now(),
                    teaTypes = teaTypeDao.getAllIncludingDeleted().map { it.toBackup() },
                    vessels = vesselDao.getAllIncludingDeleted().map { it.toBackup() },
                    teas = teaDao.getAllIncludingDeleted().map { it.toBackup() },
                    steeps = sessionDao.getAllIncludingDeleted().map { it.toBackup() },
                    methods = methodDao.getAllIncludingDeleted().map { it.toBackup() },
                )
            }

            // Stored path → archive entry; a photo whose file is gone is dropped
            val entries = mutableMapOf<String, Path>()
            val names = mutableMapOf<String, String?>()
            val backup = snapshot.mapPhotos { stored ->
                names.getOrPut(stored) {
                    val file = imageStorage.resolveImagePath(stored).toPath()
                    if (!fileSystem.exists(file)) return@getOrPut null
                    "photos/${entries.size}.${extensionOf(stored)}".also { entries[it] = file }
                }
            }

            val zipPath = "${imageStorage.getTempDir()}/leaflog_export.zip"
            ZipWriter(fileSystem, zipPath.toPath()).use { zip ->
                zip.add("data.json", encodeBackup(backup).encodeToByteArray())
                entries.forEach { (name, file) -> zip.add(name, file) }
            }
            zipPath
        }
    }

    override suspend fun import(filePath: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        val path = filePath.toPath()
        val created = mutableListOf<Path>()
        runCatching {
            val zip = try {
                fileSystem.openZip(path)
            } catch (_: IOException) {
                throw IllegalArgumentException("This file isn't a Leaf Log backup.")
            }
            val data = "/data.json".toPath()
            if (!zip.exists(data)) throw IllegalArgumentException("This file isn't a Leaf Log backup.")

            // Ids only ever grow (nothing is purged), so reading them outside the transaction is safe
            val local = LocalIds(
                teaTypes = teaTypeDao.getAllIncludingDeleted().mapTo(mutableSetOf()) { it.id },
                vessels = vesselDao.getAllIncludingDeleted().mapTo(mutableSetOf()) { it.id },
                teas = teaDao.getAllIncludingDeleted().mapTo(mutableSetOf()) { it.id },
            )
            val (kept, skipped) = decodeBackup(zip.read(data) { readUtf8() }).withoutOrphans(local)

            // Files can't roll back, so they go first and are deleted if the transaction fails
            val restored = mutableMapOf<String, String?>()
            val backup = kept.mapPhotos { name ->
                restored.getOrPut(name) { restorePhoto(zip, name, created) }
            }

            transactor {
                teaTypeDao.upsertAll(backup.teaTypes.map { it.toEntity() })
                vesselDao.upsertAll(backup.vessels.map { it.toEntity() })
                teaDao.upsertAll(backup.teas.map { it.toEntity() })
                // Parents first: children point at them
                sessionDao.upsertAll(backup.steeps.sortedBy { it.parentSessionId != null }.map { it.toEntity() })
                methodDao.upsertAll(backup.methods.map { it.toEntity() })

                sessionDao.restoreTeasWithLiveSessions()
                sessionDao.restoreVesselsWithLiveSessions()
                sessionDao.restoreTypesWithLiveTeas()
                sessionDao.deleteMethodsOfDeletedTeasAndVessels()
                sessionDao.recomputeTeaStats()
                sessionDao.recomputeConfigurationStats()
            }

            ImportResult(
                teaTypesImported = backup.teaTypes.size,
                brewingVesselsImported = backup.vessels.size,
                teasImported = backup.teas.size,
                teaSessionsImported = backup.steeps.size,
                configurationsImported = backup.methods.size,
                skipped = skipped,
            )
        }.onFailure {
            created.forEach { fileSystem.delete(it) }
        }.also {
            fileSystem.delete(path)
        }
    }

    /**
     * Copies archive entry [name] to `photos/<sha256>.<ext>` and returns that stored path, or null
     * if the archive lacks it. The name never becomes a path on the device, so it can't escape.
     */
    private fun restorePhoto(zip: FileSystem, name: String, created: MutableList<Path>): String? {
        val entry = "/".toPath() / name
        if (zip.metadataOrNull(entry)?.isRegularFile != true) return null

        val dir = imageStorage.resolveImagePath(PHOTO_DIR).toPath()
        fileSystem.createDirectories(dir)
        val temp = dir / "import-${Random.nextLong().toULong()}.tmp"
        try {
            val hash = HashingSink.sha256(fileSystem.sink(temp)).use { sink ->
                zip.source(entry).buffer().use { it.readAll(sink) }
                sink.hash
            }
            val stored = "$PHOTO_DIR/${hash.hex()}.${extensionOf(name)}"
            val target = imageStorage.resolveImagePath(stored).toPath()
            if (!fileSystem.exists(target)) {
                fileSystem.atomicMove(temp, target)
                created += target
            }
            return stored
        } finally {
            fileSystem.delete(temp)
        }
    }

    private fun extensionOf(path: String) = path.substringAfterLast('.', "").lowercase()
        .takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
        ?: "jpg"
}
