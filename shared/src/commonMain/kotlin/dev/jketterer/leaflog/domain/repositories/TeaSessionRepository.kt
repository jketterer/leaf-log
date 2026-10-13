package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TeaSession
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface TeaSessionRepository {
    fun getAllFlow(): Flow<List<TeaSession>>
    suspend fun getAll(): List<TeaSession>
    fun getRecentFlow(limit: Int): Flow<List<TeaSession>>
    suspend fun getRecent(limit: Int): List<TeaSession>
    suspend fun getById(id: String): TeaSession?
    fun getByIdFlow(id: String): Flow<TeaSession?>
    fun getByTeaIdFlow(teaId: String): Flow<List<TeaSession>>
    suspend fun getByTeaId(teaId: String, limit: Int? = null): List<TeaSession>
    suspend fun getByVesselId(vesselId: String): List<TeaSession>
    fun getByVesselIdFlow(vesselId: String): Flow<List<TeaSession>>
    suspend fun getChildSteeps(parentId: String): List<TeaSession>
    fun getInProgressFlow(): Flow<List<TeaSession>>
    fun getInProgressCountFlow(): Flow<Int>
    suspend fun getByDateRange(start: Instant, end: Instant): List<TeaSession>
    suspend fun upsert(session: TeaSession)
    suspend fun delete(id: String)
    suspend fun countByDateRange(start: Instant, end: Instant): Int
    suspend fun hasInProgressSession(): Boolean

    // Sessions with their steeps, newest first. Filters apply to the session, not its steeps.
    fun getAllSessionsFlow(): Flow<List<SessionWithSteeps>>
    suspend fun getAllSessions(): List<SessionWithSteeps>
    fun getRecentSessionsFlow(limit: Int): Flow<List<SessionWithSteeps>>
    suspend fun getSession(id: String): SessionWithSteeps?
    fun getSessionFlow(id: String): Flow<SessionWithSteeps?>
    suspend fun getSessionsByTea(teaId: String): List<SessionWithSteeps>
    fun getSessionsByTeaFlow(teaId: String): Flow<List<SessionWithSteeps>>
    suspend fun getSessionsByVessel(vesselId: String): List<SessionWithSteeps>
    fun getInProgressSessionsFlow(): Flow<List<SessionWithSteeps>>

    /** Sessions that started in [start, end], inclusive. */
    suspend fun getSessionsStartedBetween(start: Instant, end: Instant): List<SessionWithSteeps>
}