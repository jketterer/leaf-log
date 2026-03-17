package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository

class SearchTeasUseCase(
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(query: String): List<Tea> {
        if (query.isBlank()) return emptyList()
        return teaRepository.search(query.trim())
    }
}