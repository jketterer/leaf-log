package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.defaultBrew
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository

/**
 * Pre-fills brewing parameters for a tea in a vessel: the pair's default brew, else the tea's own
 * default temperature, else its type's, else nothing. A default carries only a temperature.
 */
class GetBrewingParametersPrefillUseCase(
    private val teaTypeRepository: TeaTypeRepository,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(tea: Tea, vessel: BrewingVessel): BrewingParametersPrefill {
        val brew = teaSessionRepository.getSessionsByTea(tea.id)
            .filter { it.session.vesselId == vessel.id }
            .defaultBrew()
        if (brew != null) return BrewingParametersPrefill.of(brew)

        val teaType = teaTypeRepository.getById(tea.teaTypeId)
        val (temperature, source) = tea.defaultTemperatureCelsius?.let { it to PrefillSource.TeaDefault }
            ?: teaType?.defaultTemperatureCelsius?.let { it to PrefillSource.TypeDefault(teaType.name) }
            ?: (null to PrefillSource.None)
        return BrewingParametersPrefill(
            teaQuantityGrams = null,
            waterQuantityMl = null,
            temperatureCelsius = temperature,
            brewingTime = null,
            waterType = null,
            source = source,
        )
    }
}
