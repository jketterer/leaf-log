package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.TastingTag
import kotlinx.coroutines.flow.Flow

interface TastingTagRepository {
    /** Tags in display order: the curated vocabulary, then custom tags as they were made. */
    suspend fun getAll(): List<TastingTag>

    /** [getAll], again on every change. */
    fun getAllFlow(): Flow<List<TastingTag>>

    /** Appends a custom tag named [name] after every other tag. */
    suspend fun createCustom(name: String): TastingTag

    suspend fun rename(id: String, name: String)

    /** Puts [toId] on every steep with [fromId], confirmed if either was. [fromId] stays too. */
    suspend fun copyToSteeps(fromId: String, toId: String)

    /** Takes the tag off every steep, then deletes it. */
    suspend fun delete(id: String)

    /** Adds any curated tag missing from the database. */
    suspend fun initializeDefaults()
}
