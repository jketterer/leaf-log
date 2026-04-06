package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository

/**
 * Use case to pre-fill brewing parameters based on saved brewing configurations.
 *
 * Priority order:
 * 1. Saved brewing configuration for this exact tea + vessel
 * 2. Saved brewing configuration for another tea of the same type + vessel
 * 3. No pre-fill
 */
class GetBrewingParametersPrefillUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    suspend operator fun invoke(tea: Tea, vessel: BrewingVessel): BrewingParametersPrefill {
        // 1. Saved brewing configuration for this exact tea + vessel
        val exactConfig = brewingConfigurationRepository.getBestByTeaAndVessel(tea.id, vessel.id)
        if (exactConfig != null) {
            return BrewingParametersPrefill(
                teaQuantityGrams = exactConfig.teaQuantityGrams,
                waterQuantityMl = exactConfig.waterQuantityMl,
                temperatureCelsius = exactConfig.temperatureCelsius,
                brewingTime = exactConfig.brewingTime,
                waterType = exactConfig.waterType,
                source = PrefillSource.SavedConfig,
                rating = exactConfig.rating,
            )
        }

        // 2. Saved brewing configuration for another tea of the same type + this vessel
        val teaType = teaTypeRepository.getById(tea.teaTypeId)
        if (teaType != null) {
            val sameTypeTeas = teaRepository.getAll()
                .filter { it.teaTypeId == tea.teaTypeId && it.id != tea.id }
            for (otherTea in sameTypeTeas) {
                val typeConfig =
                    brewingConfigurationRepository.getBestByTeaAndVessel(otherTea.id, vessel.id)
                if (typeConfig != null) {
                    return BrewingParametersPrefill(
                        teaQuantityGrams = typeConfig.teaQuantityGrams,
                        waterQuantityMl = typeConfig.waterQuantityMl,
                        temperatureCelsius = typeConfig.temperatureCelsius,
                        brewingTime = typeConfig.brewingTime,
                        waterType = typeConfig.waterType,
                        source = PrefillSource.SameTypeConfig(teaName = otherTea.name),
                        rating = typeConfig.rating,
                    )
                }
            }
        }

        // 3. No pre-fill available
        return BrewingParametersPrefill(
            teaQuantityGrams = null,
            waterQuantityMl = null,
            temperatureCelsius = null,
            brewingTime = null,
            waterType = null,
            rating = null,
            source = PrefillSource.None,
        )
    }
}
