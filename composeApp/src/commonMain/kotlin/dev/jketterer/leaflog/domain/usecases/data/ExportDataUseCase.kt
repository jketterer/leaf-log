package dev.jketterer.leaflog.domain.usecases.data

import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipArchiver
import dev.jketterer.leaflog.data.local.ZipEntry
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.models.LeafLogExportData
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ExportDataUseCase(
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val imageStorage: ImageStorage,
    private val zipArchiver: ZipArchiver,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Exports all data to a ZIP file containing data.json and image files.
     * Returns the path to the temp ZIP file.
     */
    suspend operator fun invoke(): Result<String> = runCatching {
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

        val jsonString = json.encodeToString(LeafLogExportData.serializer(), exportData)

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
}
