package dev.jketterer.leaflog.data.repositories

import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.data.backup.BACKUP_VERSION
import dev.jketterer.leaflog.data.backup.Backup
import dev.jketterer.leaflog.data.backup.LocalRow
import dev.jketterer.leaflog.data.backup.LocalRows
import dev.jketterer.leaflog.data.backup.decodeBackup
import dev.jketterer.leaflog.data.backup.encodeBackup
import dev.jketterer.leaflog.data.backup.mapPhotos
import dev.jketterer.leaflog.data.backup.planMerge
import dev.jketterer.leaflog.data.backup.tagEntities
import dev.jketterer.leaflog.data.backup.summarize
import dev.jketterer.leaflog.data.backup.toBackup
import dev.jketterer.leaflog.data.backup.toDomain
import dev.jketterer.leaflog.data.backup.toEntity
import dev.jketterer.leaflog.data.backup.withoutOrphans
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipWriter
import dev.jketterer.leaflog.data.local.database.dao.BrewingConfigurationDao
import dev.jketterer.leaflog.data.local.database.dao.BrewingVesselDao
import dev.jketterer.leaflog.data.local.database.dao.TastingTagDao
import dev.jketterer.leaflog.data.local.database.dao.TeaDao
import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.ImportSummary
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.BackupRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
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
import kotlin.time.Instant

/** Where restored photos go, named by content so a re-import never duplicates one. */
private const val PHOTO_DIR = "photos"

/** Every directory photos have been saved to, earlier imports' `tea_photos` included. */
private val PHOTO_DIRS = listOf(PHOTO_DIR, "tea_images", "session_images", "vessel_images", "tea_photos")

private val DATA = "/data.json".toPath()

private const val EXPORT_PREFIX = "leaf-log-export-"

private val exportTimestamp = LocalDateTime.Format {
    year(); monthNumber(); day(); char('-'); hour(); minute(); second()
}

/** The name an export is saved or shared under: `leaf-log-export-20261015-143005.zip`. */
internal fun exportFileName(at: Instant, tz: TimeZone) =
    "$EXPORT_PREFIX${exportTimestamp.format(at.toLocalDateTime(tz))}.zip"


class BackupRepositoryImpl(
    private val teaTypeDao: TeaTypeDao,
    private val vesselDao: BrewingVesselDao,
    private val teaDao: TeaDao,
    private val sessionDao: TeaSessionDao,
    private val methodDao: BrewingConfigurationDao,
    private val tastingTagDao: TastingTagDao,
    private val transactor: Transactor,
    private val imageStorage: ImageStorage,
    private val fileSystem: FileSystem,
    private val preferencesRepository: PreferencesRepository,
) : BackupRepository {

    override suspend fun export(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val now = Clock.System.now()
            // One transaction, so a steep saved mid-export can't land half in the snapshot
            val snapshot = transactor {
                val tags = sessionDao.getAllTags().groupBy { it.steepId }
                Backup(
                    version = BACKUP_VERSION,
                    exportedAt = now,
                    teaTypes = teaTypeDao.getAllIncludingDeleted().map { it.toBackup() },
                    vessels = vesselDao.getAllIncludingDeleted().map { it.toBackup() },
                    teas = teaDao.getAllIncludingDeleted().map { it.toBackup() },
                    steeps = sessionDao.getAllIncludingDeleted().map { it.toBackup(tags[it.id].orEmpty()) },
                    methods = methodDao.getAllIncludingDeleted().map { it.toBackup() },
                    tastingTags = tastingTagDao.getAllIncludingDeleted().map { it.toBackup() },
                    preferences = preferencesRepository.getPreferences().toBackup(),
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

            // Each export is a copy of every photo; one the app was killed before delivering
            // would otherwise stay in the cache
            val dir = imageStorage.getTempDir().toPath()
            fileSystem.list(dir).filter { it.name.startsWith(EXPORT_PREFIX) }
                .forEach { fileSystem.delete(it) }

            val zipPath = dir / exportFileName(now, TimeZone.currentSystemDefault())
            ZipWriter(fileSystem, zipPath).use { zip ->
                zip.add("data.json", encodeBackup(backup).encodeToByteArray())
                entries.forEach { (name, file) -> zip.add(name, file) }
            }
            zipPath.toString()
        }.ioAsDomainFailure()
    }

    override suspend fun preview(filePath: String): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            val path = filePath.toPath()
            runCatching {
                val (planned, skipped, local) = plan(openBackup(path))
                planned.summarize(local, skipped)
            }.ioAsDomainFailure().onFailure {
                fileSystem.delete(path)
            }
        }

    override suspend fun import(filePath: String): Result<ImportSummary> = withContext(Dispatchers.IO) {
        val path = filePath.toPath()
        val created = mutableListOf<Path>()
        runCatching {
            val zip = openBackup(path)
            val (planned, skipped) = plan(zip)

            // Files can't roll back, so they go first and are deleted if the transaction fails.
            // Only rows that will be written need theirs
            val restored = mutableMapOf<String, String?>()
            val withPhotos = planned.mapPhotos { name ->
                restored.getOrPut(name) { restorePhoto(zip, name, created) }
            }

            val summary = transactor {
                // Planned again: a row this device changed while the photos copied keeps its change
                val local = localRows()
                val backup = planMerge(withPhotos, local)
                teaTypeDao.upsertAll(backup.teaTypes.map { it.toEntity() })
                vesselDao.upsertAll(backup.vessels.map { it.toEntity() })
                teaDao.upsertAll(backup.teas.map { it.toEntity() })
                tastingTagDao.insertMissing(backup.tastingTags.map { it.toEntity() })
                // Parents first: children point at them
                sessionDao.upsertAll(backup.steeps.sortedBy { it.parentSessionId != null }.map { it.toEntity() })
                // A steep that's written brings its tags, replacing those it had here
                // Chunked to stay under SQLite's limit on bound variables
                backup.steeps.map { it.id }.chunked(1_000).forEach { sessionDao.deleteTags(it) }
                sessionDao.insertTags(backup.steeps.flatMap { it.tagEntities() })
                methodDao.upsertAll(backup.methods.map { it.toEntity() })

                sessionDao.restoreTeasWithLiveSessions()
                sessionDao.restoreVesselsWithLiveSessions()
                sessionDao.restoreTypesWithLiveTeas()
                sessionDao.deleteMethodsOfDeletedTeasAndVessels()
                sessionDao.recomputeTeaStats()
                sessionDao.recomputeConfigurationStats()

                backup.summarize(local, skipped)
            }

            // After the commit: a rolled-back import must not have changed the settings
            if (summary.restoresSettings) {
                planned.preferences?.let { restorePreferences(it.toDomain()) }
            }
            summary
        }.ioAsDomainFailure().onFailure {
            created.forEach { fileSystem.delete(it) }
        }.also {
            fileSystem.delete(path)
        }
    }

    /** Settings aren't the data being restored: one that fails to save doesn't fail the import. */
    private suspend fun restorePreferences(preferences: UserPreferences) {
        val results = with(preferences) {
            listOf(
                preferencesRepository.updateTemperatureUnit(temperatureUnit),
                preferencesRepository.updateVolumeUnit(volumeUnit),
                preferencesRepository.updateWeightUnit(weightUnit),
                preferencesRepository.updateTeaSortOption(teaSortOption),
                preferencesRepository.updateAnalyticsPeriod(analyticsPeriod),
                preferencesRepository.updateDefaultWaterType(defaultWaterType),
                preferencesRepository
                    .updateTimerCompletionNotificationsEnabled(timerCompletionNotificationsEnabled),
                preferencesRepository
                    .updateSessionReminderNotificationsEnabled(sessionReminderNotificationsEnabled),
                preferencesRepository.updateThemeMode(themeMode),
                preferencesRepository.updateHemisphere(hemisphere),
            )
        }
        results.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { error ->
            Logger.w(error, tag = "Backup") { "Couldn't restore every setting" }
        }
    }

    override suspend fun photoStorageBytes(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            PHOTO_DIRS.map { imageStorage.resolveImagePath(it).toPath() }
                .filter { fileSystem.exists(it) }
                .flatMap { fileSystem.listRecursively(it).toList() }
                .sumOf { file ->
                    fileSystem.metadataOrNull(file)?.takeIf { it.isRegularFile }?.size ?: 0L
                }
        }
    }

    override suspend fun discard(filePath: String) = withContext(Dispatchers.IO) {
        fileSystem.delete(filePath.toPath())
    }

    private fun openBackup(path: Path): FileSystem {
        val zip = try {
            fileSystem.openZip(path)
        } catch (_: IOException) {
            throw DomainFailure.NotABackup()
        }
        if (!zip.exists(DATA)) throw DomainFailure.NotABackup()
        return zip
    }

    /**
     * The rows importing [zip] would write, photos not yet copied; how many orphans were left out;
     * and the local rows it was planned over.
     */
    private suspend fun plan(zip: FileSystem): Triple<Backup, Int, LocalRows> {
        val local = localRows()
        val (kept, skipped) = decodeBackup(zip.read(DATA) { readUtf8() }).withoutOrphans(local)
        return Triple(planMerge(kept, local), skipped, local)
    }

    private suspend fun localRows(): LocalRows {
        val steeps = sessionDao.getAllIncludingDeleted()
        return LocalRows(
            teaTypes = teaTypeDao.getAllIncludingDeleted()
                .associate { it.id to LocalRow(it.updatedAt, it.deletedAt != null) },
            vessels = vesselDao.getAllIncludingDeleted()
                .associate { it.id to LocalRow(it.updatedAt, it.deletedAt != null) },
            teas = teaDao.getAllIncludingDeleted()
                .associate { it.id to LocalRow(it.updatedAt, it.deletedAt != null) },
            steeps = steeps.associate { it.id to LocalRow(it.updatedAt, it.deletedAt != null) },
            methods = methodDao.getAllIncludingDeleted()
                .associate { it.id to LocalRow(it.updatedAt, it.deletedAt != null) },
            inProgressSessions = steeps
                .filter { it.parentSessionId == null && it.deletedAt == null }
                .filter { it.status == SessionStatus.IN_PROGRESS }
                .mapTo(mutableSetOf()) { it.id },
        )
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

/** A file that couldn't be read or written fails as [DomainFailure.Io]. */
private fun <T> Result<T>.ioAsDomainFailure(): Result<T> =
    exceptionOrNull().let { if (it is IOException) Result.failure(DomainFailure.Io(it)) else this }
