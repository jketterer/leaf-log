package dev.jketterer.leaflog.domain.usecases.data

import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipArchiver
import dev.jketterer.leaflog.data.mappers.toBrewingConfiguration
import dev.jketterer.leaflog.data.mappers.toBrewingVessel
import dev.jketterer.leaflog.data.mappers.toTea
import dev.jketterer.leaflog.data.mappers.toTeaSession
import dev.jketterer.leaflog.data.mappers.toTeaType
import dev.jketterer.leaflog.data.models.LeafLogExportData
import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.serialization.json.Json

class ImportDataUseCase(
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val imageStorage: ImageStorage,
    private val zipArchiver: ZipArchiver,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Imports data from a file at [filePath].
     * Supports both ZIP (new format with images) and plain JSON (legacy format).
     */
    suspend operator fun invoke(filePath: String): Result<ImportResult> = runCatching {
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
            exportData = json.decodeFromString(LeafLogExportData.serializer(), jsonString)

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
            exportData = json.decodeFromString(LeafLogExportData.serializer(), jsonString)
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
