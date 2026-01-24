package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.BrewingVessel
import kotlinx.coroutines.flow.Flow

interface BrewingVesselRepository {
    fun getAllFlow(): Flow<List<BrewingVessel>>
    suspend fun getAll(): List<BrewingVessel>
    suspend fun getById(id: String): BrewingVessel?
    fun getByIdFlow(id: String): Flow<BrewingVessel?>
    suspend fun upsert(vessel: BrewingVessel)
    suspend fun delete(id: String)
    suspend fun initializeDefaults()
}