package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface BrewingConfigurationRepository {
    suspend fun getAll(): List<BrewingConfiguration>

    /**
     * Get all configurations for a tea + vessel combination
     * Sorted by rating, times used, and last used
     */
    suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfiguration>

    /**
     * Get the best (highest rated, most used) configuration for a tea + vessel
     */
    suspend fun getBestByTeaAndVessel(teaId: String, vesselId: String): BrewingConfiguration?

    /**
     * Get all configurations for a tea
     */
    suspend fun getByTeaId(teaId: String): List<BrewingConfiguration>

    /**
     * Get all configurations for a tea as a flow
     */
    fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>>

    /**
     * Get a configuration by ID
     */
    suspend fun getById(id: String): BrewingConfiguration?

    /**
     * Get a configuration by ID as a flow
     */
    fun getByIdFlow(id: String): Flow<BrewingConfiguration?>

    /**
     * Save or update a configuration
     */
    suspend fun upsert(configuration: BrewingConfiguration)

    /**
     * Delete a configuration
     */
    suspend fun delete(id: String)

    /**
     * Increment the times used counter and update last used timestamp
     */
    suspend fun incrementTimesUsed(id: String, timestamp: Instant)

    /**
     * Set the active status of a configuration
     */
    suspend fun setActive(id: String, isActive: Boolean)
}
