package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.TastingTag

interface TastingTagRepository {
    /** Tags in display order: the curated vocabulary, then custom tags as they were made. */
    suspend fun getAll(): List<TastingTag>

    /** Appends a custom tag named [name] after every other tag. */
    suspend fun createCustom(name: String): TastingTag

    /** Adds any curated tag missing from the database. */
    suspend fun initializeDefaults()
}
