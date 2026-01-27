package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTeaType
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration

class TeaTypeRepositoryImpl(
    private val teaTypeDao: TeaTypeDao,
) : TeaTypeRepository {
    override fun getAllFlow(): Flow<List<TeaType>> {
        return teaTypeDao.getAllFlow().map { entities ->
            println("emission")
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
        val timestamp = Clock.System.now().toEpochMilliseconds()
        teaTypeDao.softDelete(id, timestamp)
    }

    override suspend fun initializeDefaults() {
        val count = teaTypeDao.count()
        if (count > 0) return  // Already initialized

        val defaults = getDefaultTeaTypes()
        teaTypeDao.upsertAll(defaults.map { it.toEntity() })
    }

    private fun getDefaultTeaTypes(): List<TeaType> {
        val now = Clock.System.now()
        return listOf(
            TeaType(
                id = "green",
                name = "Green",
                defaultTemperatureCelsius = 75,
                defaultBrewingTime = Duration.parse("2m30s"),
                colorHex = "#4CAF50",
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "black",
                name = "Black",
                defaultTemperatureCelsius = 95,
                defaultBrewingTime = Duration.parse("4m"),
                colorHex = "#795548",
                isSystemDefault = true,
                displayOrder = 1,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "white",
                name = "White",
                defaultTemperatureCelsius = 72,
                defaultBrewingTime = Duration.parse("4m30s"),
                colorHex = "#FFFDE7",
                isSystemDefault = true,
                displayOrder = 2,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "oolong",
                name = "Oolong",
                defaultTemperatureCelsius = 90,
                defaultBrewingTime = Duration.parse("4m"),
                colorHex = "#FF9800",
                isSystemDefault = true,
                displayOrder = 3,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "puerh",
                name = "Pu-erh",
                defaultTemperatureCelsius = 98,
                defaultBrewingTime = Duration.parse("4m"),
                colorHex = "#8D6E63",
                isSystemDefault = true,
                displayOrder = 4,
                createdAt = now,
                updatedAt = now
            ),
            TeaType(
                id = "herbal",
                name = "Herbal",
                defaultTemperatureCelsius = 100,
                defaultBrewingTime = Duration.parse("6m"),
                colorHex = "#9C27B0",
                isSystemDefault = true,
                displayOrder = 5,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}