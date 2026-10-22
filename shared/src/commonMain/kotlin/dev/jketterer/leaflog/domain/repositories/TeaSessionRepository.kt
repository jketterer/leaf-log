package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface TeaSessionRepository {
    // Sessions with their steeps, newest first. Filters apply to the session, not its steeps.
    fun getAllSessionsFlow(): Flow<List<SessionWithSteeps>>
    suspend fun getAllSessions(): List<SessionWithSteeps>
    /** Every session's id, oldest first. */
    fun getSessionIdsFlow(): Flow<List<String>>
    fun getRecentSessionsFlow(limit: Int): Flow<List<SessionWithSteeps>>
    suspend fun getSession(id: String): SessionWithSteeps?
    fun getSessionFlow(id: String): Flow<SessionWithSteeps?>
    suspend fun getSessionsByTea(teaId: String): List<SessionWithSteeps>
    fun getSessionsByTeaFlow(teaId: String): Flow<List<SessionWithSteeps>>
    suspend fun getSessionsByVessel(vesselId: String): List<SessionWithSteeps>
    fun getInProgressSessionsFlow(): Flow<List<SessionWithSteeps>>

    /** Sessions that started in [start, end], inclusive. */
    suspend fun getSessionsStartedBetween(start: Instant, end: Instant): List<SessionWithSteeps>

    /** The session [steepId] belongs to. Steep 1's id is the session's own. */
    suspend fun getSessionOfSteep(steepId: String): SessionWithSteeps?

    // Writes for the session use cases only, made inside their Transactor block

    /** Writes every steep of [session]. Steeps it no longer holds are left as they are. */
    suspend fun saveSession(session: SessionWithSteeps)
    suspend fun deleteSteep(steepId: String)
    suspend fun deleteSession(sessionId: String)
}