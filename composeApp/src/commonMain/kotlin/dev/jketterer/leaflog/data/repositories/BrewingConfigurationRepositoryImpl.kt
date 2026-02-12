package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.BrewingConfigurationDao
import dev.jketterer.leaflog.data.mappers.toBrewingConfiguration
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class BrewingConfigurationRepositoryImpl(
    private val brewingConfigurationDao: BrewingConfigurationDao,
) : BrewingConfigurationRepository {

    override suspend fun getAll(): List<BrewingConfiguration> {
        return brewingConfigurationDao.getAll().map { it.toBrewingConfiguration() }
    }

    override suspend fun getByTeaAndVessel(
        teaId: String,
        vesselId: String
    ): List<BrewingConfiguration> {
        return brewingConfigurationDao.getByTeaAndVessel(teaId, vesselId)
            .map { it.toBrewingConfiguration() }
    }

    override suspend fun getBestByTeaAndVessel(
        teaId: String,
        vesselId: String
    ): BrewingConfiguration? {
        return brewingConfigurationDao.getBestByTeaAndVessel(teaId, vesselId)
            ?.toBrewingConfiguration()
    }

    override suspend fun getByTeaId(teaId: String): List<BrewingConfiguration> {
        return brewingConfigurationDao.getByTeaId(teaId)
            .map { it.toBrewingConfiguration() }
    }

    override fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>> {
        return brewingConfigurationDao.getByTeaIdFlow(teaId)
            .map { entities -> entities.map { it.toBrewingConfiguration() } }
    }

    override suspend fun getById(id: String): BrewingConfiguration? {
        return brewingConfigurationDao.getById(id)?.toBrewingConfiguration()
    }

    override fun getByIdFlow(id: String): Flow<BrewingConfiguration?> {
        return brewingConfigurationDao.getByIdFlow(id)
            .map { it?.toBrewingConfiguration() }
    }

    override suspend fun upsert(configuration: BrewingConfiguration) {
        brewingConfigurationDao.upsert(configuration.toEntity())
    }

    override suspend fun delete(id: String) {
        brewingConfigurationDao.delete(id)
    }

    override suspend fun incrementTimesUsed(id: String, timestamp: Instant) {
        brewingConfigurationDao.incrementTimesUsed(id, timestamp.toEpochMilliseconds())
    }

    override suspend fun setActive(id: String, isActive: Boolean) {
        brewingConfigurationDao.setActive(id, isActive)
    }
}
