package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlin.time.Instant

class GetTeaTypeDistributionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): List<TeaTypeDistribution> {
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
        val completed = sessions.filter { it.session.status == SessionStatus.COMPLETED }

        if (completed.isEmpty()) return emptyList()

        val teas = teaRepository.getAll().associateBy { it.id }
        val teaTypes = teaTypeRepository.getAll().associateBy { it.id }

        val countsByTeaType = completed
            .mapNotNull { session -> teas[session.session.teaId]?.teaTypeId }
            .groupingBy { it }
            .eachCount()

        val total = completed.size.toFloat()

        return countsByTeaType
            .mapNotNull { (teaTypeId, count) ->
                val teaType = teaTypes[teaTypeId] ?: return@mapNotNull null
                TeaTypeDistribution(
                    teaType = teaType,
                    sessionCount = count,
                    percentage = count / total * 100f,
                )
            }
            .sortedByDescending { it.sessionCount }
    }
}
