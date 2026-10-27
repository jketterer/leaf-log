package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTeaType
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

class TeaTypeRepositoryImpl(
    private val teaTypeDao: TeaTypeDao,
    private val clock: Clock,
) : TeaTypeRepository {
    override fun getAllFlow(): Flow<List<TeaType>> {
        return teaTypeDao.getAllFlow().map { entities ->
            entities.map { it.toTeaType() }
        }
    }

    override suspend fun getAll(): List<TeaType> {
        return teaTypeDao.getAll().map { it.toTeaType() }
    }

    override suspend fun getById(id: String): TeaType? {
        return teaTypeDao.getById(id)?.toTeaType()
    }

    override fun getByIdFlow(id: String): Flow<TeaType?> {
        return teaTypeDao.getByIdFlow(id).map { it?.toTeaType() }
    }

    override suspend fun upsert(teaType: TeaType) {
        teaTypeDao.upsert(teaType.toEntity())
    }

    override suspend fun delete(id: String) {
        val timestamp = clock.now().toEpochMilliseconds()
        teaTypeDao.softDelete(id, timestamp)
    }

    override suspend fun initializeDefaults() {
        val count = teaTypeDao.count()
        if (count > 0) return  // Already initialized

        val defaults = getDefaultTeaTypes()
        teaTypeDao.upsertAll(defaults.map { it.toEntity() })
    }

    private fun getDefaultTeaTypes(): List<TeaType> {
        // Epoch 0, so any edit restored from a backup beats an untouched seed
        val now = Instant.fromEpochMilliseconds(0)
        return listOf(
            TeaType(
                id = "green",
                name = "Green",
                defaultTemperatureCelsius = 75.0,
                colorHex = "#6F8D02",
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "black",
                name = "Black",
                defaultTemperatureCelsius = 95.0,
                colorHex = "#8B1A2D",
                isSystemDefault = true,
                displayOrder = 1,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "white",
                name = "White",
                defaultTemperatureCelsius = 72.0,
                colorHex = "#87926A",
                isSystemDefault = true,
                displayOrder = 2,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "oolong",
                name = "Oolong",
                defaultTemperatureCelsius = 90.0,
                colorHex = "#A57707",
                isSystemDefault = true,
                displayOrder = 3,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "puerh",
                name = "Pu-erh",
                defaultTemperatureCelsius = 98.0,
                colorHex = "#7B4F3F",
                isSystemDefault = true,
                displayOrder = 4,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "herbal",
                name = "Herbal",
                defaultTemperatureCelsius = 100.0,
                colorHex = "#A171C2",
                isSystemDefault = true,
                displayOrder = 5,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}