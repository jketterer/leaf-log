package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipArchiver
import dev.jketterer.leaflog.data.local.ZipEntry
import dev.jketterer.leaflog.data.mappers.toBrewingConfiguration
import dev.jketterer.leaflog.data.mappers.toBrewingVessel
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTea
import dev.jketterer.leaflog.data.mappers.toTeaSession
import dev.jketterer.leaflog.data.mappers.toTeaType
import dev.jketterer.leaflog.data.models.LeafLogExportData
import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.DataExportRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class DataExportRepositoryImpl(
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val imageStorage: ImageStorage,
    private val zipArchiver: ZipArchiver,
) : DataExportRepository {

    private val exportJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val importJson = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun exportAll(): Result<String> = runCatching {
        val teaTypes = teaTypeRepository.getAll().map { it.toEntity() }
        val vessels = brewingVesselRepository.getAll().map { it.toEntity() }
        val teas = teaRepository.getAll().map { it.toEntity() }
        val sessions = teaSessionRepository.getAll().map { it.toEntity() }
        val configurations = brewingConfigurationRepository.getAll().map { it.toEntity() }

        // Collect all image paths and build a remap: absolutePath → archivePath
        val pathRemap = mutableMapOf<String, String>()
        var imageIndex = 0

        for (session in sessions) {
            for (photo in session.photos) {
                if (photo.isNotBlank() && photo !in pathRemap) {
                    val ext = photo.substringAfterLast('.', "jpg")
                    pathRemap[photo] = "images/session_photos/${imageIndex++}.$ext"
                }
            }
        }

        for (tea in teas) {
            for (photo in tea.photos) {
                if (photo.isNotBlank() && photo !in pathRemap) {
                    val ext = photo.substringAfterLast('.', "jpg")
                    pathRemap[photo] = "images/tea_photos/${imageIndex++}.$ext"
                }
            }
        }

        for (vessel in vessels) {
            val path = vessel.imagePath
            if (path != null && path.isNotBlank() && path !in pathRemap) {
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

        // Build zip entries
        val zipEntries = mutableListOf<ZipEntry>()
        zipEntries.add(ZipEntry(name = "data.json", data = jsonString.encodeToByteArray()))

        // Add image entries
        for ((absolutePath, archivePath) in pathRemap) {
            val imageBytes = imageStorage.readImage(absolutePath)
            if (imageBytes != null) {
                zipEntries.add(ZipEntry(name = archivePath, data = imageBytes))
            }
        }

        // Write zip to temp file
        val tempDir = imageStorage.getTempDir()
        val zipPath = "$tempDir/leaflog_export.zip"
        zipArchiver.createZip(zipPath, zipEntries)

        zipPath
    }

    override suspend fun importFrom(filePath: String): Result<ImportResult> = runCatching {
        val header = zipArchiver.readFileHeader(filePath, 4)
        val isZip = header != null && header.size >= 4 &&
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

        val exportData: LeafLogExportData
        val imageRemap: Map<String, String>

        if (isZip) {
            val entries = zipArchiver.extractZip(filePath)
            val dataEntry = entries.find { it.name == "data.json" }
                ?: throw IllegalStateException("ZIP does not contain data.json")

            val jsonString = dataEntry.data.decodeToString()
            exportData = importJson.decodeFromString(LeafLogExportData.serializer(), jsonString)

            // Restore images and build remap: archivePath → newAbsolutePath
            val remap = mutableMapOf<String, String>()
            val imageEntries = entries.filter { it.name.startsWith("images/") }
            for (entry in imageEntries) {
                val fileName = entry.name.substringAfterLast('/')
                val subdirectory = when {
                    entry.name.startsWith("images/session_photos/") -> "session_images"
                    entry.name.startsWith("images/tea_photos/") -> "tea_photos"
                    entry.name.startsWith("images/vessel_images/") -> "vessel_images"
                    else -> "imported_images"
                }
                val newPath = imageStorage.saveImage(entry.data, fileName, subdirectory)
                remap[entry.name] = newPath
            }
            imageRemap = remap
        } else {
            // Legacy JSON format
            val fileBytes = zipArchiver.readFile(filePath)
                ?: throw IllegalStateException("Cannot read file: $filePath")
            val jsonString = fileBytes.decodeToString()
            exportData = importJson.decodeFromString(LeafLogExportData.serializer(), jsonString)
            // Clear photo paths since images won't exist on this device
            imageRemap = emptyMap()
        }

        // Apply image remap to entities
        val remappedSessions = exportData.teaSessions.map { session ->
            if (isZip) {
                session.copy(photos = session.photos.mapNotNull { imageRemap[it] ?: it.takeIf { p -> !p.startsWith("images/") } })
            } else {
                session.copy(photos = emptyList())
            }
        }
        val remappedTeas = exportData.teas.map { tea ->
            if (isZip) {
                tea.copy(photos = tea.photos.mapNotNull { imageRemap[it] ?: it.takeIf { p -> !p.startsWith("images/") } })
            } else {
                tea.copy(photos = emptyList())
            }
        }
        val remappedVessels = exportData.brewingVessels.map { vessel ->
            if (isZip && vessel.imagePath != null) {
                vessel.copy(imagePath = imageRemap[vessel.imagePath] ?: vessel.imagePath.takeIf { !it.startsWith("images/") })
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

        // 4. Tea sessions — parents first, then children
        val (parents, children) = remappedSessions.partition { it.parentSessionId == null }
        parents.forEach { entity ->
            teaSessionRepository.upsert(entity.toTeaSession())
        }
        children.forEach { entity ->
            teaSessionRepository.upsert(entity.toTeaSession())
        }

        // 5. Brewing configurations (FK → Tea, FK → Vessel)
        exportData.brewingConfigurations.forEach { entity ->
            brewingConfigurationRepository.upsert(entity.toBrewingConfiguration())
        }

        ImportResult(
            teaTypesImported = exportData.teaTypes.size,
            brewingVesselsImported = exportData.brewingVessels.size,
            teasImported = exportData.teas.size,
            teaSessionsImported = exportData.teaSessions.size,
            configurationsImported = exportData.brewingConfigurations.size,
        )
    }
}
