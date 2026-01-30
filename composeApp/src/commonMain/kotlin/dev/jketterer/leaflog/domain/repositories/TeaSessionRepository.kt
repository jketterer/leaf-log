package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.TeaSession
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface TeaSessionRepository {
    fun getAllFlow(): Flow<List<TeaSession>>
    fun getRecentFlow(limit: Int): Flow<List<TeaSession>>
    suspend fun getRecent(limit: Int): List<TeaSession>
    suspend fun getById(id: String): TeaSession?
    fun getByIdFlow(id: String): Flow<TeaSession?>
    fun getByTeaIdFlow(teaId: String): Flow<List<TeaSession>>
    suspend fun getByTeaId(teaId: String, limit: Int? = null): List<TeaSession>
    suspend fun getByVesselId(vesselId: String): List<TeaSession>
    fun getByVesselIdFlow(vesselId: String): Flow<List<TeaSession>>
    suspend fun getChildSteeps(parentId: String): List<TeaSession>
    fun getDraftsFlow(): Flow<List<TeaSession>>
    fun getDraftsCountFlow(): Flow<Int>
    suspend fun getByDateRange(start: Instant, end: Instant): List<TeaSession>
    suspend fun upsert(session: TeaSession)
    suspend fun delete(id: String)
    suspend fun countByDateRange(start: Instant, end: Instant): Int
}