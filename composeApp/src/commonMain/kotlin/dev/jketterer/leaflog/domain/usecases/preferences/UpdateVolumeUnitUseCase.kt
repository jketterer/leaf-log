package dev.jketterer.leaflog.domain.usecases.preferences

import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository

class UpdateVolumeUnitUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(unit: VolumeUnit): Result<Unit> {
        return preferencesRepository.updateVolumeUnit(unit)
    }
}
