package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.BrewingVesselDao
import dev.jketterer.leaflog.data.mappers.toBrewingVessel
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class BrewingVesselRepositoryImpl(
    private val brewingVesselDao: BrewingVesselDao,
) : BrewingVesselRepository {
    override fun getAllFlow(): Flow<List<BrewingVessel>> {
        return brewingVesselDao.getAllFlow().map { entities ->
            entities.map { it.toBrewingVessel() }
        }
    }

    override suspend fun getAll(): List<BrewingVessel> {
        return brewingVesselDao.getAll().map { it.toBrewingVessel() }
    }

    override suspend fun getById(id: String): BrewingVessel? {
        return brewingVesselDao.getById(id)?.toBrewingVessel()
    }

    override fun getByIdFlow(id: String): Flow<BrewingVessel?> {
        return brewingVesselDao.getByIdFlow(id).map { it?.toBrewingVessel() }
    }

    override suspend fun upsert(vessel: BrewingVessel) {
        brewingVesselDao.upsert(vessel.toEntity())
    }

    override suspend fun delete(id: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        brewingVesselDao.softDelete(id, timestamp)
    }

    override suspend fun initializeDefaults() {
        val count = brewingVesselDao.count()
        if (count > 0) return

        val defaults = getDefaultVessels()
        brewingVesselDao.upsertAll(defaults.map { it.toEntity() })
    }

    private fun getDefaultVessels(): List<BrewingVessel> {
        val now = Clock.System.now()
        return listOf(
            BrewingVessel(
                id = "gaiwan",
                name = "Gaiwan",
                iconName = "gaiwan",
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = now,
                updatedAt = now
            ),
            BrewingVessel(
                id = "teapot",
                name = "Teapot",
                iconName = "teapot",
                isSystemDefault = true,
                displayOrder = 1,
                createdAt = now,
                updatedAt = now
            ),
            BrewingVessel(
                id = "kyusu",
                name = "Kyusu",
                iconName = "kyusu",
                isSystemDefault = true,
                displayOrder = 2,
                createdAt = now,
                updatedAt = now
            ),
            BrewingVessel(
                id = "mug",
                name = "Mug",
                iconName = "mug",
                isSystemDefault = true,
                displayOrder = 3,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}