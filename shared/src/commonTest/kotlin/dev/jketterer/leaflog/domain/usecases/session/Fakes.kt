package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.mappers.toAggregates
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * In-memory repositories for use case tests.
 *
 * Only the members a use case actually calls are implemented; the rest throw, so a test that starts
 * depending on new data fails loudly instead of quietly reading an empty list.
 */

class FakeTeaRepository(teas: List<Tea> = emptyList()) : TeaRepository {
    private val teas = teas.associateBy { it.id }.toMutableMap()

    override suspend fun getAll(): List<Tea> = teas.values.toList()
    override suspend fun getById(id: String): Tea? = teas[id]
    override suspend fun upsert(tea: Tea) {
        teas[tea.id] = tea
    }

    override fun getAllFlow(): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<Tea?> = TODO("unused by these tests")
    override fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getFavoritesFlow(): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getRecentlyBrewedFlow(limit: Int): Flow<List<Tea>> = TODO("unused by these tests")
    override suspend fun search(query: String): List<Tea> = TODO("unused by these tests")
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

/** Old reads come from [sessions]. Aggregates are kept as rows, the way Room stores them. */
class FakeTeaSessionRepository(
    private val sessions: List<TeaSession> = emptyList(),
) : TeaSessionRepository {
    private val rows = mutableMapOf<String, TeaSessionEntity>()
    private val liveRows get() = rows.values.filter { it.deletedAt == null }

    override suspend fun getByTeaId(teaId: String, limit: Int?): List<TeaSession> =
        sessions.filter { it.teaId == teaId }

    override suspend fun getSession(id: String): SessionWithSteeps? =
        liveRows.filter { (it.parentSessionId ?: it.id) == id }.toAggregates().singleOrNull()

    override suspend fun getSessionOfSteep(steepId: String): SessionWithSteeps? =
        liveRows.find { it.id == steepId }?.let { getSession(it.parentSessionId ?: it.id) }

    override suspend fun getSessionsByTea(teaId: String): List<SessionWithSteeps> =
        liveRows.filter { it.teaId == teaId }.toAggregates()

    override suspend fun saveSession(session: SessionWithSteeps) {
        session.steeps.forEach { rows[it.id] = it.toEntity(session.session) }
    }

    override suspend fun deleteSteep(steepId: String) {
        rows[steepId] = rows.getValue(steepId).copy(deletedAt = DELETED_AT)
    }

    override suspend fun delete(id: String) {
        rows.values.filter { (it.parentSessionId ?: it.id) == id }
            .forEach { rows[it.id] = it.copy(deletedAt = DELETED_AT) }
    }

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
    override suspend fun countByDateRange(start: Instant, end: Instant): Int = TODO("unused by these tests")
    override suspend fun hasInProgressSession(): Boolean = TODO("unused by these tests")
    override fun getAllSessionsFlow(): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override suspend fun getAllSessions(): List<SessionWithSteeps> = TODO("unused by these tests")
    override fun getRecentSessionsFlow(limit: Int): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override fun getSessionFlow(id: String): Flow<SessionWithSteeps?> = TODO("unused by these tests")
    override fun getSessionsByTeaFlow(teaId: String): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override suspend fun getSessionsByVessel(vesselId: String): List<SessionWithSteeps> = TODO("unused by these tests")
    override fun getInProgressSessionsFlow(): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override suspend fun getSessionsStartedBetween(start: Instant, end: Instant): List<SessionWithSteeps> = TODO("unused by these tests")

    private companion object {
        val DELETED_AT = Instant.fromEpochMilliseconds(1)
    }
}

class FakeBrewingConfigurationRepository(
    configurations: List<BrewingConfiguration> = emptyList(),
) : BrewingConfigurationRepository {
    private val configurations = configurations.associateBy { it.id }.toMutableMap()

    override suspend fun getBestByTeaAndVessel(
        teaId: String,
        vesselId: String,
    ): BrewingConfiguration? =
        configurations.values.firstOrNull { it.teaId == teaId && it.vesselId == vesselId }

    override suspend fun getByTeaId(teaId: String): List<BrewingConfiguration> =
        configurations.values.filter { it.teaId == teaId }

    override suspend fun getById(id: String): BrewingConfiguration? = configurations[id]
    override suspend fun upsert(configuration: BrewingConfiguration) {
        configurations[configuration.id] = configuration
    }

    override suspend fun getAll(): List<BrewingConfiguration> = TODO("unused by these tests")
    override suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfiguration> = TODO("unused by these tests")
    override fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<BrewingConfiguration?> = TODO("unused by these tests")
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

/** Runs the block with no rollback: what a rollback undoes is RoomTransactorTest's to prove. */
class FakeTransactor : Transactor {
    override suspend fun <T> invoke(block: suspend () -> T): T = block()
}
