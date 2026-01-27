package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.mappers.toEntity
import dev.jketterer.leaflog.data.mappers.toTeaSession
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

class TeaSessionRepositoryImpl(
    private val teaSessionDao: TeaSessionDao,
) : TeaSessionRepository {
    override fun getAllFlow(): Flow<List<TeaSession>> {
        return teaSessionDao.getAllFlow().map { entities ->
            entities.map { it.toTeaSession() }
        }
    }

    override fun getRecentFlow(limit: Int): Flow<List<TeaSession>> {
        return teaSessionDao.getRecentFlow(limit).map { entities ->
            entities.map { it.toTeaSession() }
        }
    }

    override suspend fun getRecent(limit: Int): List<TeaSession> {
        return teaSessionDao.getRecent(limit).map { it.toTeaSession() }
    }

    override suspend fun getById(id: String): TeaSession? {
        return teaSessionDao.getById(id)?.toTeaSession()
    }

    override fun getByIdFlow(id: String): Flow<TeaSession?> {
        return teaSessionDao.getByIdFlow(id).map { it?.toTeaSession() }
    }

    override fun getByTeaIdFlow(teaId: String): Flow<List<TeaSession>> {
        return teaSessionDao.getByTeaIdFlow(teaId).map { entities ->
            entities.map { it.toTeaSession() }
        }
    }

    override suspend fun getByTeaId(teaId: String, limit: Int?): List<TeaSession> {
        val sessions = teaSessionDao.getByTeaId(teaId).map { it.toTeaSession() }
        return if (limit != null) {
            sessions.take(limit)
        } else {
            sessions
        }
    }

    override suspend fun getChildSteeps(parentId: String): List<TeaSession> {
        return teaSessionDao.getChildSteeps(parentId).map { it.toTeaSession() }
    }

    override fun getDraftsFlow(): Flow<List<TeaSession>> {
        return teaSessionDao.getByStatusFlow(status = SessionStatus.DRAFT.name).map { entities ->
            entities.map { it.toTeaSession() }
        }
    }

    override fun getDraftsCountFlow(): Flow<Int> {
        return teaSessionDao.getCountByStatusFlow(SessionStatus.DRAFT.name)
    }

    override suspend fun getByDateRange(start: Instant, end: Instant): List<TeaSession> {
        return teaSessionDao.getByDateRange(
            startTimestamp = start.toEpochMilliseconds(),
            endTimestamp = end.toEpochMilliseconds()
        ).map { it.toTeaSession() }
    }

    override suspend fun upsert(session: TeaSession) {
        teaSessionDao.upsert(session.toEntity())
    }

    override suspend fun delete(id: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()

        // Check if this is a parent session with child steeps
        val session = teaSessionDao.getById(id)
        if (session != null && session.parentSessionId == null) {
            // Delete child steeps first
            teaSessionDao.softDeleteChildSteeps(id, timestamp)
        }

        // Delete the session itself
        teaSessionDao.softDelete(id, timestamp)
    }

    override suspend fun countByDateRange(start: Instant, end: Instant): Int {
        return teaSessionDao.countByDateRange(
            startTimestamp = start.toEpochMilliseconds(),
            endTimestamp = end.toEpochMilliseconds()
        )
    }
}