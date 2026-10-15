package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipWriter
import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.mappers.toBrewingConfiguration
import dev.jketterer.leaflog.data.mappers.toBrewingVessel
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTea
import dev.jketterer.leaflog.data.mappers.toTeaType
import dev.jketterer.leaflog.data.models.LeafLogExportData
import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.DataExportRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip
import okio.use
import kotlin.time.Clock

class DataExportRepositoryImpl(
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaRepository: TeaRepository,
    private val teaSessionDao: TeaSessionDao,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val imageStorage: ImageStorage,
    private val fileSystem: FileSystem,
) : DataExportRepository {

    private val exportJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val importJson = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun exportAll(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val teaTypes = teaTypeRepository.getAll().map { it.toEntity() }
        val vessels = brewingVesselRepository.getAll().map { it.toEntity() }
        val teas = teaRepository.getAll().map { it.toEntity() }
        val sessions = teaSessionDao.getAllSessions()
        val configurations = brewingConfigurationRepository.getAll().map { it.toEntity() }

        // Collect all image paths and build a remap: absolutePath → archivePath
        val pathRemap = mutableMapOf<String, String>()
        var imageIndex = 0

        for ((_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, photos) in sessions) {
            for (photo in photos) {
                if (photo.isNotBlank() && photo !in pathRemap) {
                    val ext = photo.substringAfterLast('.', "jpg")
                    pathRemap[photo] = "images/session_photos/${imageIndex++}.$ext"
                }
            }
        }

        for ((_, _, _, _, _, _, _, _, photos) in teas) {
            for (photo in photos) {
                if (photo.isNotBlank() && photo !in pathRemap) {
                    val ext = photo.substringAfterLast('.', "jpg")
                    pathRemap[photo] = "images/tea_photos/${imageIndex++}.$ext"
                }
            }
        }

        for ((_, _, _, path) in vessels) {
            if (!path.isNullOrBlank() && path !in pathRemap) {
                val ext = path.substringAfterLast('.', "jpg")
                pathRemap[path] = "images/vessel_images/${imageIndex++}.$ext"
            }
        }

        // Remap paths in the entities for the export JSON
        val remappedSessions = sessions.map { session ->
            session.copy(photos = session.photos.map { pathRemap[it] ?: it })
        }
        val remappedTeas = teas.map { tea ->
            tea.copy(photos = tea.photos.map { pathRemap[it] ?: it })
        }
        val remappedVessels = vessels.map { vessel ->
            if (vessel.imagePath != null) {
                vessel.copy(imagePath = pathRemap[vessel.imagePath] ?: vessel.imagePath)
            } else {
                vessel
            }
        }

        val exportData = LeafLogExportData(
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            teaTypes = teaTypes,
            brewingVessels = remappedVessels,
            teas = remappedTeas,
            teaSessions = remappedSessions,
            brewingConfigurations = configurations,
        )

        val jsonString = exportJson.encodeToString(LeafLogExportData.serializer(), exportData)

        val zipPath = "${imageStorage.getTempDir()}/leaflog_export.zip"
        ZipWriter(fileSystem, zipPath.toPath()).use { zip ->
            zip.add("data.json", jsonString.encodeToByteArray())
            for ((storedPath, archivePath) in pathRemap) {
                val file = imageStorage.resolveImagePath(storedPath).toPath()
                if (fileSystem.exists(file)) zip.add(archivePath, file)
            }
        }
        zipPath
    } }

    override suspend fun importFrom(filePath: String): Result<ImportResult> =
        withContext(Dispatchers.IO) { runCatching {
        val path = filePath.toPath()
        // "PK", read little-endian
        val isZip = fileSystem.read(path) { request(2) && readShortLe().toInt() == 0x4B50 }

        val exportData: LeafLogExportData
        val imageRemap: Map<String, String>

        if (isZip) {
            val zip = fileSystem.openZip(path)
            val dataJson = "/data.json".toPath()
            if (!zip.exists(dataJson)) throw IllegalStateException("ZIP does not contain data.json")
            exportData = importJson.decodeFromString(
                LeafLogExportData.serializer(),
                zip.read(dataJson) { readUtf8() },
            )

            // Restore images and build remap: archivePath → stored path
            val remap = mutableMapOf<String, String>()
            val images = zip.listRecursively("/".toPath())
                .filter { zip.metadata(it).isRegularFile }
                .toList()
            for (entry in images) {
                val name = entry.toString().removePrefix("/")
                if (!name.startsWith("images/")) continue
                val subdirectory = when {
                    name.startsWith("images/session_photos/") -> "session_images"
                    name.startsWith("images/tea_photos/") -> "tea_photos"
                    name.startsWith("images/vessel_images/") -> "vessel_images"
                    else -> "imported_images"
                }
                // The entry's last segment only, so a crafted name can't escape the directory
                val storedPath = "$subdirectory/${name.substringAfterLast('/')}"
                val target = imageStorage.resolveImagePath(storedPath).toPath()
                fileSystem.createDirectories(target.parent!!)
                zip.source(entry).use { source ->
                    fileSystem.write(target) { writeAll(source) }
                }
                remap[name] = storedPath
            }
            imageRemap = remap
        } else {
            // Legacy JSON format
            exportData = importJson.decodeFromString(
                LeafLogExportData.serializer(),
                fileSystem.read(path) { readUtf8() },
            )
            // Clear photo paths since images won't exist on this device
            imageRemap = emptyMap()
        }

        // Apply image remap to entities
        val remappedSessions = exportData.teaSessions.map { session ->
            if (isZip) {
                session.copy(photos = session.photos.mapNotNull {
                    imageRemap[it] ?: it.takeIf { p -> !p.startsWith("images/") }
                })
            } else {
                session.copy(photos = emptyList())
            }
        }
        val remappedTeas = exportData.teas.map { tea ->
            if (isZip) {
                tea.copy(photos = tea.photos.mapNotNull {
                    imageRemap[it] ?: it.takeIf { p -> !p.startsWith("images/") }
                })
            } else {
                tea.copy(photos = emptyList())
            }
        }
        val remappedVessels = exportData.brewingVessels.map { vessel ->
            if (isZip && vessel.imagePath != null) {
                vessel.copy(
                    imagePath = imageRemap[vessel.imagePath]
                        ?: vessel.imagePath.takeIf { !it.startsWith("images/") })
            } else if (!isZip) {
                vessel.copy(imagePath = null)
            } else {
                vessel
            }
        }

        // 1. Tea types (no FKs)
        exportData.teaTypes.forEach { entity ->
            teaTypeRepository.upsert(entity.toTeaType())
        }

        // 2. Brewing vessels (no FKs)
        remappedVessels.forEach { entity ->
            brewingVesselRepository.upsert(entity.toBrewingVessel())
        }

        // 3. Teas (FK → TeaType)
        remappedTeas.forEach { entity ->
            teaRepository.upsert(entity.toTea())
        }

        // 4. Tea sessions: parents first, then children. Backups before schema v7 copied the
        // session's fields onto every steep; they live on the parent only.
        val (parents, children) = remappedSessions.partition { it.parentSessionId == null }
        teaSessionDao.upsertAll(
            parents + children.map {
                it.copy(
                    teaQuantityGrams = null,
                    vesselId = null,
                    waterType = null,
                    location = null,
                    usedConfigurationId = null,
                )
            },
        )

        // 5. Brewing configurations (FK → Tea, FK → Vessel)
        exportData.brewingConfigurations.forEach { entity ->
            brewingConfigurationRepository.upsert(entity.toBrewingConfiguration())
        }

        // 6. Stats, rebuilt from the sessions now in the database rather than taken on trust
        teaSessionDao.recomputeTeaStats()
        teaSessionDao.recomputeConfigurationStats()

        ImportResult(
            teaTypesImported = exportData.teaTypes.size,
            brewingVesselsImported = exportData.brewingVessels.size,
            teasImported = exportData.teas.size,
            teaSessionsImported = exportData.teaSessions.size,
            configurationsImported = exportData.brewingConfigurations.size,
        )
    } }
}
