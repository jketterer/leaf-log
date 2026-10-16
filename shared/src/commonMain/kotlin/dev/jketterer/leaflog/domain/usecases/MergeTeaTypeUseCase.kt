package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/**
 * Moves every tea of [sourceId], deleted ones included, to [targetId], then deletes [sourceId].
 * Teas keep their own temperatures.
 */
class MergeTeaTypeUseCase(
    private val transactor: Transactor,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    suspend operator fun invoke(sourceId: String, targetId: String): Result<Unit> = runCatching {
        require(sourceId != targetId) { "A tea type can't be merged into itself" }
        transactor {
            teaTypeRepository.getById(targetId) ?: throw DomainFailure.NotFound("tea type $targetId")
            teaRepository.reassignType(fromTypeId = sourceId, toTypeId = targetId)
            teaTypeRepository.delete(sourceId)
        }
    }
}
