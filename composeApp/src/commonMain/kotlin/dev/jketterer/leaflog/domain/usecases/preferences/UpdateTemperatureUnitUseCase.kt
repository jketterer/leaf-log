package dev.jketterer.leaflog.domain.usecases.preferences

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository

class UpdateTemperatureUnitUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(unit: TemperatureUnit): Result<Unit> {
        return preferencesRepository.updateTemperatureUnit(unit)
    }
}
