package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.TeaType
import kotlinx.coroutines.flow.Flow

interface TeaTypeRepository {
    fun getAllFlow(): Flow<List<TeaType>>
    suspend fun getAll(): List<TeaType>
    suspend fun getById(id: String): TeaType?
    fun getByIdFlow(id: String): Flow<TeaType?>
    suspend fun upsert(teaType: TeaType)
    suspend fun delete(id: String)
    suspend fun initializeDefaults()
}