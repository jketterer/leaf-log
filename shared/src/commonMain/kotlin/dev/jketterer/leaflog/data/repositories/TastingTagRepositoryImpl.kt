package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TastingTagDao
import dev.jketterer.leaflog.data.local.database.entities.TastingTagEntity
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import kotlin.uuid.Uuid

class TastingTagRepositoryImpl(
    private val tastingTagDao: TastingTagDao,
) : TastingTagRepository {
    override suspend fun getAll(): List<TastingTag> = tastingTagDao.getAll().map { it.toTastingTag() }

    override suspend fun createCustom(name: String): TastingTag {
        val tag = TastingTagEntity(
            id = Uuid.random().toString(),
            key = null,
            name = name.trim(),
            isCustom = true,
            displayOrder = (tastingTagDao.maxDisplayOrder() ?: -1) + 1,
        )
        tastingTagDao.insert(tag)
        return tag.toTastingTag()
    }

    override suspend fun initializeDefaults() = tastingTagDao.insertMissing(
        CURATED_TAG_KEYS.mapIndexed { i, key ->
            TastingTagEntity(id = key, key = key, name = null, isCustom = false, displayOrder = i)
        },
    )
}

private fun TastingTagEntity.toTastingTag() = TastingTag(
    id = id,
    key = key,
    name = name,
    isCustom = isCustom,
    displayOrder = displayOrder,
)
