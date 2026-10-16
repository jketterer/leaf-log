package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/** Deletes a tea type no tea uses. The last type is kept; a type in use is merged instead. */
class DeleteTeaTypeUseCase(
    private val transactor: Transactor,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    suspend operator fun invoke(teaTypeId: String): Result<Unit> = runCatching {
        transactor {
            if (teaTypeRepository.getAll().size <= 1) {
                throw DomainFailure.LastRemaining("tea type")
            }
            val teas = teaRepository.getAll().count { it.teaTypeId == teaTypeId }
            if (teas > 0) throw DomainFailure.InUse(teas, DomainFailure.Dependents.TEAS)
            teaTypeRepository.delete(teaTypeId)
        }
    }
}
