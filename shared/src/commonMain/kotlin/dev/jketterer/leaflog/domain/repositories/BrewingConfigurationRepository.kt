package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import kotlinx.coroutines.flow.Flow

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
     * Deletes [teaId]'s configurations, stamped as the tea was so they can be restored with it.
     * Call after deleting the tea.
     */
    suspend fun deleteWithTea(teaId: String)

    /** As [deleteWithTea], for [vesselId]'s configurations. Call after deleting the vessel. */
    suspend fun deleteWithVessel(vesselId: String)

    /**
     * Get the most frequently used active configurations as a reactive flow
     */
    fun getMostUsedFlow(limit: Int): Flow<List<BrewingConfiguration>>

    /**
     * Get all active configurations as a reactive flow, sorted by pinned first (in pinned order),
     * then by usage frequency
     */
    fun getAllActiveFlow(): Flow<List<BrewingConfiguration>>

    /**
     * Get the highest pinnedSortOrder value among currently pinned active configs
     */
    suspend fun getMaxPinnedSortOrder(): Int?

    /**
     * Set or clear the pinned status of a configuration
     */
    suspend fun setPinned(id: String, isPinned: Boolean, pinnedSortOrder: Int)

    /**
     * Reorder all pinned configurations. orderedIds defines the new order
     * (index 0 = first, index 1 = second, etc.)
     */
    suspend fun reorderPinnedConfigurations(orderedIds: List<String>)
}
