package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.dao.TastingTagDao
import dev.jketterer.leaflog.data.local.database.entities.TastingTagEntity
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.Uuid

class TastingTagRepositoryImpl(
    private val tastingTagDao: TastingTagDao,
    private val clock: Clock,
) : TastingTagRepository {
    override suspend fun getAll(): List<TastingTag> = tastingTagDao.getAll().map { it.toTastingTag() }

    override fun getAllFlow(): Flow<List<TastingTag>> =
        tastingTagDao.getAllFlow().map { tags -> tags.map { it.toTastingTag() } }

    override suspend fun createCustom(name: String): TastingTag {
        val tag = TastingTagEntity(
            id = Uuid.random().toString(),
            key = null,
            name = name.trim(),
            isCustom = true,
            displayOrder = (tastingTagDao.maxDisplayOrder() ?: -1) + 1,
            updatedAt = clock.now(),
        )
        tastingTagDao.insert(tag)
        return tag.toTastingTag()
    }

    override suspend fun rename(id: String, name: String) =
        tastingTagDao.rename(id, name.trim(), clock.now().toEpochMilliseconds())

    override suspend fun copyToSteeps(fromId: String, toId: String) {
        tastingTagDao.touchSteepsWith(fromId, clock.now().toEpochMilliseconds())
        tastingTagDao.copySteepTags(fromId, toId)
    }

    override suspend fun delete(id: String) {
        val timestamp = clock.now().toEpochMilliseconds()
        tastingTagDao.touchSteepsWith(id, timestamp)
        tastingTagDao.deleteSteepTags(id)
        tastingTagDao.softDelete(id, timestamp)
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
