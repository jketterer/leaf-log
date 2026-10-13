package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.mappers.toAggregates
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

class TeaSessionRepositoryImpl(
    private val teaSessionDao: TeaSessionDao,
) : TeaSessionRepository {
    override fun getAllSessionsFlow(): Flow<List<SessionWithSteeps>> =
        teaSessionDao.getAllSessionsFlow().map { it.toAggregates() }

    override suspend fun getAllSessions(): List<SessionWithSteeps> =
        teaSessionDao.getAllSessions().toAggregates()

    override fun getRecentSessionsFlow(limit: Int): Flow<List<SessionWithSteeps>> =
        teaSessionDao.getRecentSessionsFlow(limit).map { it.toAggregates() }

    override suspend fun getSession(id: String): SessionWithSteeps? =
        teaSessionDao.getSession(id).toAggregates().singleOrNull()

    override fun getSessionFlow(id: String): Flow<SessionWithSteeps?> =
        teaSessionDao.getSessionFlow(id).map { it.toAggregates().singleOrNull() }

    override suspend fun getSessionsByTea(teaId: String): List<SessionWithSteeps> =
        teaSessionDao.getSessionsByTea(teaId).toAggregates()

    override fun getSessionsByTeaFlow(teaId: String): Flow<List<SessionWithSteeps>> =
        teaSessionDao.getSessionsByTeaFlow(teaId).map { it.toAggregates() }

    override suspend fun getSessionsByVessel(vesselId: String): List<SessionWithSteeps> =
        teaSessionDao.getSessionsByVessel(vesselId).toAggregates()

    override fun getInProgressSessionsFlow(): Flow<List<SessionWithSteeps>> =
        teaSessionDao.getInProgressSessionsFlow().map { it.toAggregates() }

    override suspend fun getSessionsStartedBetween(
        start: Instant,
        end: Instant,
    ): List<SessionWithSteeps> = teaSessionDao.getSessionsStartedBetween(
        startTimestamp = start.toEpochMilliseconds(),
        endTimestamp = end.toEpochMilliseconds(),
    ).toAggregates()

    override suspend fun getSessionOfSteep(steepId: String): SessionWithSteeps? =
        teaSessionDao.getSessionOfSteep(steepId).toAggregates().singleOrNull()

    override suspend fun saveSession(session: SessionWithSteeps) = teaSessionDao.upsertAll(
        session.steeps.map { steep ->
            val row = steep.toEntity(session.session)
            // Screens still reading TeaSession show the stored average until v7 drops the column
            if (row.parentSessionId == null) row.copy(averageRating = session.effectiveRating) else row
        },
    )

    override suspend fun deleteSteep(steepId: String) =
        teaSessionDao.softDelete(steepId, Clock.System.now().toEpochMilliseconds())

    override suspend fun deleteSession(sessionId: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        teaSessionDao.softDeleteChildSteeps(sessionId, timestamp)
        teaSessionDao.softDelete(sessionId, timestamp)
    }
}
