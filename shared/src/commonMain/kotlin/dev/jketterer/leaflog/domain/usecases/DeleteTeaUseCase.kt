package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/**
 * Deletes a tea that was never brewed. A tea with sessions, in progress or not, is refused: the
 * journal must keep its name, so it is marked finished instead.
 */
class DeleteTeaUseCase(
    private val transactor: Transactor,
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(teaId: String): Result<Unit> = runCatching {
        transactor {
            val sessions = teaSessionRepository.getSessionsByTea(teaId).size
            if (sessions > 0) throw DomainFailure.InUse(sessions, DomainFailure.Dependents.SESSIONS)
            teaRepository.delete(teaId)
        }
    }
}
