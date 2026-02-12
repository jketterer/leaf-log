package dev.jketterer.leaflog.domain.usecases.data

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
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(jsonContent: String): Result<ImportResult> = runCatching {
        val exportData = json.decodeFromString(LeafLogExportData.serializer(), jsonContent)

        // 1. Tea types (no FKs)
        exportData.teaTypes.forEach { entity ->
            teaTypeRepository.upsert(entity.toTeaType())
        }

        // 2. Brewing vessels (no FKs)
        exportData.brewingVessels.forEach { entity ->
            brewingVesselRepository.upsert(entity.toBrewingVessel())
        }

        // 3. Teas (FK → TeaType)
        exportData.teas.forEach { entity ->
            teaRepository.upsert(entity.toTea())
        }

        // 4. Tea sessions — parents first, then children
        val (parents, children) = exportData.teaSessions.partition { it.parentSessionId == null }
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
