package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TeaDao
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTea
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class TeaRepositoryImpl(
    private val teaDao: TeaDao,
) : TeaRepository {
    override fun getAllFlow(): Flow<List<Tea>> {
        return teaDao.getAllFlow().map { entities ->
            entities.map { it.toTea() }
        }
    }

    override suspend fun getAll(): List<Tea> {
        return teaDao.getAll().map { it.toTea() }
    }

    override suspend fun getById(id: String): Tea? {
        return teaDao.getById(id)?.toTea()
    }

    override fun getByIdFlow(id: String): Flow<Tea?> {
        return teaDao.getByIdFlow(id).map { it?.toTea() }
    }

    override fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>> {
        return teaDao.getByTypeFlow(teaTypeId).map { entities ->
            entities.map { it.toTea() }
        }
    }

    override fun getFavoritesFlow(): Flow<List<Tea>> {
        return teaDao.getFavoritesFlow().map { entities ->
            entities.map { it.toTea() }
        }
    }

    override fun getRecentlyBrewedFlow(limit: Int): Flow<List<Tea>> {
        return teaDao.getRecentlyBrewedFlow(limit).map { entities ->
            entities.map { it.toTea() }
        }
    }

    override suspend fun search(query: String): List<Tea> {
        return teaDao.search(query).map { it.toTea() }
    }

    override suspend fun upsert(tea: Tea) {
        teaDao.upsert(tea.toEntity())
    }

    override suspend fun delete(id: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        teaDao.softDelete(id, timestamp)
    }

    override fun getDistinctProducersFlow(): Flow<List<String>> {
        return teaDao.getDistinctProducersFlow()
    }
}