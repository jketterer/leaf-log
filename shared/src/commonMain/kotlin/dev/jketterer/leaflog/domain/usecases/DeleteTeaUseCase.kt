package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/**
 * Deletes a tea that was never brewed, with its methods. A tea with sessions, in progress or not,
 * is refused: the journal must keep its name, so it is marked finished instead.
 */
class DeleteTeaUseCase(
    private val transactor: Transactor,
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(teaId: String): Result<Unit> = runCatching {
        transactor {
            check(teaSessionRepository.getSessionsByTea(teaId).isEmpty()) {
                "A tea with sessions can't be deleted"
            }
            teaRepository.delete(teaId)
            brewingConfigurationRepository.deleteWithTea(teaId)
        }
    }
}
