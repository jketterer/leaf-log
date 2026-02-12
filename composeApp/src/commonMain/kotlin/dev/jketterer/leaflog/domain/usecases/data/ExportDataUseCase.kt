package dev.jketterer.leaflog.domain.usecases.data

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
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(): Result<String> = runCatching {
        val teaTypes = teaTypeRepository.getAll().map { it.toEntity() }
        val vessels = brewingVesselRepository.getAll().map { it.toEntity() }
        val teas = teaRepository.getAll().map { it.toEntity() }
        val sessions = teaSessionRepository.getAll().map { it.toEntity() }
        val configurations = brewingConfigurationRepository.getAll().map { it.toEntity() }

        val exportData = LeafLogExportData(
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            teaTypes = teaTypes,
            brewingVessels = vessels,
            teas = teas,
            teaSessions = sessions,
            brewingConfigurations = configurations,
        )

        json.encodeToString(LeafLogExportData.serializer(), exportData)
    }
}
