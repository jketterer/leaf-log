package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/**
 * Deletes a custom tag, taking it off every steep. With [mergeInto], those steeps get that tag
 * instead, confirmed where either was. Curated tags are kept: they're the shared vocabulary.
 */
class RemoveTastingTagUseCase(
    private val transactor: Transactor,
    private val tastingTagRepository: TastingTagRepository,
) {
    suspend operator fun invoke(tagId: String, mergeInto: String? = null): Result<Unit> = runCatching {
        require(tagId != mergeInto) { "A tag can't be merged into itself" }
        transactor {
            val tags = tastingTagRepository.getAll().associateBy { it.id }
            val tag = tags[tagId] ?: throw DomainFailure.NotFound("tasting tag $tagId")
            require(tag.isCustom) { "Curated tags can't be removed" }
            if (mergeInto != null) {
                if (mergeInto !in tags) throw DomainFailure.NotFound("tasting tag $mergeInto")
                tastingTagRepository.copyToSteeps(fromId = tagId, toId = mergeInto)
            }
            tastingTagRepository.delete(tagId)
        }
    }
}
