package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository

class InitializeDefaultsUseCase(
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
) {
    suspend operator fun invoke() {
        teaTypeRepository.initializeDefaults()
        brewingVesselRepository.initializeDefaults()
    }
}