package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.Tea
import kotlinx.coroutines.flow.Flow

interface TeaRepository {
    fun getAllFlow(): Flow<List<Tea>>
    suspend fun getAll(): List<Tea>
    suspend fun getById(id: String): Tea?
    fun getByIdFlow(id: String): Flow<Tea?>
    fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>>
    fun getFavoritesFlow(): Flow<List<Tea>>
    suspend fun search(query: String): List<Tea>
    suspend fun upsert(tea: Tea)
    suspend fun delete(id: String)
    fun getDistinctProducersFlow(): Flow<List<String>>
}