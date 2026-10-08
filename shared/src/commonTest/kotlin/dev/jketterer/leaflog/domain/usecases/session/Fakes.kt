package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * In-memory repositories for use case tests.
 *
 * Only the members a use case actually calls are implemented; the rest throw, so a test that starts
 * depending on new data fails loudly instead of quietly reading an empty list.
 */

class FakeTeaRepository(private val teas: List<Tea> = emptyList()) : TeaRepository {
    override suspend fun getAll(): List<Tea> = teas

    override fun getAllFlow(): Flow<List<Tea>> = TODO("unused by these tests")
    override suspend fun getById(id: String): Tea? = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<Tea?> = TODO("unused by these tests")
    override fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getFavoritesFlow(): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getRecentlyBrewedFlow(limit: Int): Flow<List<Tea>> = TODO("unused by these tests")
    override suspend fun search(query: String): List<Tea> = TODO("unused by these tests")
    override suspend fun upsert(tea: Tea) = TODO("unused by these tests")
    override suspend fun delete(id: String) = TODO("unused by these tests")
    override fun getDistinctProducersFlow(): Flow<List<String>> = TODO("unused by these tests")
}

class FakeTeaTypeRepository(private val teaTypes: List<TeaType> = emptyList()) : TeaTypeRepository {
    override suspend fun getById(id: String): TeaType? = teaTypes.find { it.id == id }

    override fun getAllFlow(): Flow<List<TeaType>> = TODO("unused by these tests")
    override suspend fun getAll(): List<TeaType> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<TeaType?> = TODO("unused by these tests")
    override suspend fun upsert(teaType: TeaType) = TODO("unused by these tests")
    override suspend fun delete(id: String) = TODO("unused by these tests")
    override suspend fun initializeDefaults() = TODO("unused by these tests")
}

class FakeTeaSessionRepository(
    private val sessions: List<TeaSession> = emptyList(),
) : TeaSessionRepository {
    override suspend fun getByTeaId(teaId: String, limit: Int?): List<TeaSession> =
        sessions.filter { it.teaId == teaId }

    override fun getAllFlow(): Flow<List<TeaSession>> = TODO("unused by these tests")
    override suspend fun getAll(): List<TeaSession> = TODO("unused by these tests")
    override fun getRecentFlow(limit: Int): Flow<List<TeaSession>> = TODO("unused by these tests")
    override suspend fun getRecent(limit: Int): List<TeaSession> = TODO("unused by these tests")
    override suspend fun getById(id: String): TeaSession? = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<TeaSession?> = TODO("unused by these tests")
    override fun getByTeaIdFlow(teaId: String): Flow<List<TeaSession>> = TODO("unused by these tests")
    override suspend fun getByVesselId(vesselId: String): List<TeaSession> = TODO("unused by these tests")
    override fun getByVesselIdFlow(vesselId: String): Flow<List<TeaSession>> = TODO("unused by these tests")
    override suspend fun getChildSteeps(parentId: String): List<TeaSession> = TODO("unused by these tests")
    override fun getInProgressFlow(): Flow<List<TeaSession>> = TODO("unused by these tests")
    override fun getInProgressCountFlow(): Flow<Int> = TODO("unused by these tests")
    override suspend fun getByDateRange(start: Instant, end: Instant): List<TeaSession> = TODO("unused by these tests")
    override suspend fun upsert(session: TeaSession) {}
    override suspend fun delete(id: String) = TODO("unused by these tests")
    override suspend fun countByDateRange(start: Instant, end: Instant): Int = TODO("unused by these tests")
    override suspend fun hasInProgressSession(): Boolean = TODO("unused by these tests")
}

class FakeBrewingConfigurationRepository(
    private val configurations: List<BrewingConfiguration> = emptyList(),
) : BrewingConfigurationRepository {
    override suspend fun getBestByTeaAndVessel(
        teaId: String,
        vesselId: String,
    ): BrewingConfiguration? =
        configurations.firstOrNull { it.teaId == teaId && it.vesselId == vesselId }

    override suspend fun getAll(): List<BrewingConfiguration> = TODO("unused by these tests")
    override suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfiguration> = TODO("unused by these tests")
    override suspend fun getByTeaId(teaId: String): List<BrewingConfiguration> = TODO("unused by these tests")
    override fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override suspend fun getById(id: String): BrewingConfiguration? = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<BrewingConfiguration?> = TODO("unused by these tests")
    override suspend fun upsert(configuration: BrewingConfiguration) = TODO("unused by these tests")
    override suspend fun delete(id: String) = TODO("unused by these tests")
    override suspend fun incrementTimesUsed(id: String, timestamp: Instant) = TODO("unused by these tests")
    override suspend fun setActive(id: String, isActive: Boolean) = TODO("unused by these tests")
    override fun getMostUsedFlow(limit: Int): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override fun getAllActiveFlow(): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override suspend fun getMaxPinnedSortOrder(): Int? = TODO("unused by these tests")
    override suspend fun setPinned(id: String, isPinned: Boolean, pinnedSortOrder: Int) = TODO("unused by these tests")
    override suspend fun updatePinnedSortOrder(id: String, pinnedSortOrder: Int) = TODO("unused by these tests")
    override suspend fun reorderPinnedConfigurations(orderedIds: List<String>) = TODO("unused by these tests")
}
