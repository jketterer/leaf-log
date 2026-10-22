package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.Tea
import kotlinx.coroutines.flow.Flow

interface TeaRepository {
    fun getAllFlow(): Flow<List<Tea>>
    suspend fun getAll(): List<Tea>
    suspend fun getById(id: String): Tea?
    fun getByIdFlow(id: String): Flow<Tea?>
    fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>>
    fun getRecentlyBrewedFlow(limit: Int): Flow<List<Tea>>
    suspend fun upsert(tea: Tea)
    suspend fun delete(id: String)

    /** Moves every tea of [fromTypeId] to [toTypeId], deleted teas included. */
    suspend fun reassignType(fromTypeId: String, toTypeId: String)

    /** Stamps [id] finished now, or clears it when [finished] is false. */
    suspend fun setFinished(id: String, finished: Boolean)
    fun getDistinctProducersFlow(): Flow<List<String>>
}