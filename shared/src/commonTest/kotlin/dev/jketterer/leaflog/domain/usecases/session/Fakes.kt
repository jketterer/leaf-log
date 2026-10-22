package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.data.local.database.entities.SteepRow
import dev.jketterer.leaflog.data.mappers.toAggregates
import dev.jketterer.leaflog.data.mappers.toRow
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
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

    override suspend fun getAll(): List<Tea> = teas.values.filter { it.deletedAt == null }
    override suspend fun getById(id: String): Tea? = teas[id]?.takeIf { it.deletedAt == null }
    override suspend fun delete(id: String) {
        teas[id] = teas.getValue(id).copy(deletedAt = DELETED_AT)
    }
    override suspend fun upsert(tea: Tea) {
        teas[tea.id] = tea
    }
    override suspend fun reassignType(fromTypeId: String, toTypeId: String) {
        teas.values.filter { it.teaTypeId == fromTypeId }
            .forEach { teas[it.id] = it.copy(teaTypeId = toTypeId) }
    }
    override suspend fun setFinished(id: String, finished: Boolean) {
        teas[id] = teas.getValue(id).copy(finishedAt = FINISHED_AT.takeIf { finished })
    }

    override fun getAllFlow(): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<Tea?> = TODO("unused by these tests")
    override fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getRecentlyBrewedFlow(limit: Int): Flow<List<Tea>> = TODO("unused by these tests")
    override fun getDistinctProducersFlow(): Flow<List<String>> = TODO("unused by these tests")
}

class FakeTeaTypeRepository(teaTypes: List<TeaType> = emptyList()) : TeaTypeRepository {
    private val teaTypes = teaTypes.associateBy { it.id }.toMutableMap()

    override suspend fun getAll(): List<TeaType> = teaTypes.values.filter { it.deletedAt == null }
    override suspend fun getById(id: String): TeaType? = teaTypes[id]?.takeIf { it.deletedAt == null }
    override suspend fun delete(id: String) {
        teaTypes[id] = teaTypes.getValue(id).copy(deletedAt = DELETED_AT)
    }

    override fun getAllFlow(): Flow<List<TeaType>> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<TeaType?> = TODO("unused by these tests")
    override suspend fun upsert(teaType: TeaType) = TODO("unused by these tests")
    override suspend fun initializeDefaults() = TODO("unused by these tests")
}

/** Aggregates are kept as rows, the way Room stores them. */
class FakeBrewingVesselRepository(vessels: List<BrewingVessel> = emptyList()) :
    BrewingVesselRepository {
    private val vessels = vessels.associateBy { it.id }.toMutableMap()

    override suspend fun getById(id: String): BrewingVessel? =
        vessels[id]?.takeIf { it.deletedAt == null }

    override suspend fun countActive(): Int =
        vessels.values.count { it.deletedAt == null && !it.isArchived }

    override suspend fun delete(id: String) {
        vessels[id] = vessels.getValue(id).copy(deletedAt = DELETED_AT)
    }

    override fun getAllFlow(): Flow<List<BrewingVessel>> = TODO("unused by these tests")
    override suspend fun getAll(): List<BrewingVessel> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<BrewingVessel?> = TODO("unused by these tests")
    override suspend fun upsert(vessel: BrewingVessel) = TODO("unused by these tests")
    override suspend fun initializeDefaults() = TODO("unused by these tests")
    override fun getActiveFlow(): Flow<List<BrewingVessel>> = TODO("unused by these tests")
    override suspend fun getActive(): List<BrewingVessel> =
        vessels.values.filter { it.deletedAt == null && !it.isArchived }
}

class FakeTeaSessionRepository(
    sessions: List<SessionWithSteeps> = emptyList(),
) : TeaSessionRepository {
    private val rows = mutableMapOf<String, SteepRow>()
    private val liveRows get() = rows.values.filter { it.steep.deletedAt == null }

    init {
        sessions.forEach { saveRows(it) }
    }

    private fun saveRows(session: SessionWithSteeps) {
        session.steeps.forEach { rows[it.id] = it.toRow(session.session) }
    }

    private fun SteepRow.deleted() = copy(steep = steep.copy(deletedAt = DELETED_AT))

    override suspend fun getAllSessions(): List<SessionWithSteeps> = liveRows.toAggregates()

    override suspend fun getSessionsStartedBetween(
        start: Instant,
        end: Instant,
    ): List<SessionWithSteeps> = getAllSessions().filter { it.startedAt in start..end }

    override suspend fun getSession(id: String): SessionWithSteeps? =
        liveRows.filter { (it.steep.parentSessionId ?: it.steep.id) == id }.toAggregates().singleOrNull()

    override suspend fun getSessionOfSteep(steepId: String): SessionWithSteeps? =
        liveRows.find { it.steep.id == steepId }?.let { getSession(it.steep.parentSessionId ?: it.steep.id) }

    override suspend fun getSessionsByTea(teaId: String): List<SessionWithSteeps> =
        liveRows.filter { it.steep.teaId == teaId }.toAggregates()

    override suspend fun getSessionsByVessel(vesselId: String): List<SessionWithSteeps> =
        getAllSessions().filter { it.session.vesselId == vesselId }

    override suspend fun saveSession(session: SessionWithSteeps) = saveRows(session)

    override suspend fun deleteSteep(steepId: String) {
        rows[steepId] = rows.getValue(steepId).deleted()
    }

    override suspend fun deleteSession(sessionId: String) {
        rows.values.filter { (it.steep.parentSessionId ?: it.steep.id) == sessionId }
            .forEach { rows[it.steep.id] = it.deleted() }
    }

    override fun getAllSessionsFlow(): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override fun getRecentSessionsFlow(limit: Int): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override fun getSessionFlow(id: String): Flow<SessionWithSteeps?> = TODO("unused by these tests")
    override fun getSessionsByTeaFlow(teaId: String): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")
    override fun getInProgressSessionsFlow(): Flow<List<SessionWithSteeps>> = TODO("unused by these tests")

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
        configurations.values.filter { it.teaId == teaId && it.deletedAt == null }

    override suspend fun getById(id: String): BrewingConfiguration? =
        configurations[id]?.takeIf { it.deletedAt == null }

    override suspend fun deleteWithTea(teaId: String) = deleteWhere { it.teaId == teaId }
    override suspend fun deleteWithVessel(vesselId: String) = deleteWhere { it.vesselId == vesselId }

    private fun deleteWhere(predicate: (BrewingConfiguration) -> Boolean) {
        configurations.values.filter { it.deletedAt == null && predicate(it) }
            .forEach { configurations[it.id] = it.copy(deletedAt = DELETED_AT) }
    }
    override suspend fun upsert(configuration: BrewingConfiguration) {
        configurations[configuration.id] = configuration
    }

    override suspend fun getAll(): List<BrewingConfiguration> = TODO("unused by these tests")
    override suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfiguration> =
        configurations.values.filter { it.teaId == teaId && it.vesselId == vesselId && it.deletedAt == null }
    override fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override fun getByIdFlow(id: String): Flow<BrewingConfiguration?> = TODO("unused by these tests")
    override suspend fun delete(id: String) = TODO("unused by these tests")
    override fun getMostUsedFlow(limit: Int): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override fun getAllActiveFlow(): Flow<List<BrewingConfiguration>> = TODO("unused by these tests")
    override suspend fun getMaxPinnedSortOrder(): Int? = TODO("unused by these tests")
    override suspend fun setPinned(id: String, isPinned: Boolean, pinnedSortOrder: Int) = TODO("unused by these tests")
    override suspend fun reorderPinnedConfigurations(orderedIds: List<String>) = TODO("unused by these tests")
}

private val DELETED_AT = Instant.fromEpochMilliseconds(1)
private val FINISHED_AT = Instant.fromEpochMilliseconds(2)

/** Runs the block with no rollback: what a rollback undoes is RoomTransactorTest's to prove. */
class FakeTransactor : Transactor {
    override suspend fun <T> invoke(block: suspend () -> T): T = block()
}
