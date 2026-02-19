package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository

/**
 * Use case to intelligently pre-fill brewing parameters based on user's history.
 *
 * Implements smart fallback logic:
 * 1. Saved brewing configuration (Phase 2)
 * 2. This tea + this vessel session (rated >= 3 stars)
 * 3. Same tea TYPE + this vessel session (rated >= 3 stars)
 * 4. Tea defaults (with vessel capacity fallback for water quantity)
 * 5. Vessel capacity only (when no tea defaults exist)
 * 6. Empty (no pre-fill)
 */
class GetBrewingParametersPrefillUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
) {
    /**
     * Get pre-fill parameters for a tea + vessel combination
     *
     * @param tea The selected tea
     * @param vessel The selected vessel
     * @return Pre-fill parameters with source information
     */
    suspend operator fun invoke(tea: Tea, vessel: BrewingVessel): BrewingParametersPrefill {
        val vesselId = vessel.id
        // 1. Try: Saved brewing configuration (Phase 2)
        val savedConfig = brewingConfigurationRepository.getBestByTeaAndVessel(tea.id, vesselId)
        if (savedConfig != null) {
            // If there's a saved configuration, use it
            // We'll treat this as a DirectSession source for now
            // In the future, we could add a new PrefillSource.SavedConfiguration type
            return BrewingParametersPrefill(
                teaQuantityGrams = savedConfig.teaQuantityGrams,
                waterQuantityMl = savedConfig.waterQuantityMl,
                temperatureCelsius = savedConfig.temperatureCelsius,
                brewingTime = savedConfig.brewingTime,
                waterType = savedConfig.waterType,
                source = PrefillSource.DirectSession(
                    sessionId = savedConfig.sourceSessionId,
                    rating = savedConfig.rating,
                    timestamp = savedConfig.lastUsedAt ?: savedConfig.createdAt,
                )
            )
        }

        // 2. Try: This tea + this vessel session (rated >= 3 stars)
        val directSession = teaSessionRepository.getByTeaId(tea.id)
            .filter { it.vesselId == vesselId }
            .filter { it.status == SessionStatus.COMPLETED }
            .filter { it.rating != null && it.rating >= 3f }
            .maxByOrNull { session ->
                // Prioritize by rating, then recency
                (session.rating ?: 0f) * 100 + (session.timestamp.epochSeconds % 100)
            }

        if (directSession != null) {
            return BrewingParametersPrefill(
                teaQuantityGrams = directSession.teaQuantityGrams,
                waterQuantityMl = directSession.waterQuantityMl,
                temperatureCelsius = directSession.temperatureCelsius,
                brewingTime = directSession.brewingTime,
                waterType = directSession.waterType,
                source = PrefillSource.DirectSession(
                    sessionId = directSession.id,
                    rating = directSession.rating!!,
                    timestamp = directSession.timestamp,
                )
            )
        }

        // 3. Fallback: Same tea TYPE + this vessel
        val teaType = teaTypeRepository.getById(tea.teaTypeId)
        if (teaType != null) {
            // Get all sessions for this vessel, then filter by tea type
            val vesselSessions = teaSessionRepository.getByVesselId(vesselId)
            val sameTypeTeas = teaRepository.getAll().filter { it.teaTypeId == tea.teaTypeId }
            val sameTypeTeasIds = sameTypeTeas.map { it.id }.toSet()

            val typeSession = vesselSessions
                .filter { it.teaId in sameTypeTeasIds }
                .filter { it.status == SessionStatus.COMPLETED }
                .filter { it.rating != null && it.rating >= 3f }
                .maxByOrNull { session ->
                    (session.rating ?: 0f) * 100 + (session.timestamp.epochSeconds % 100)
                }

            if (typeSession != null) {
                val sourceTea = teaRepository.getById(typeSession.teaId)
                return BrewingParametersPrefill(
                    teaQuantityGrams = null, // Don't pre-fill tea quantity from different tea
                    waterQuantityMl = typeSession.waterQuantityMl,
                    temperatureCelsius = (tea.defaultTemperatureCelsius
                        ?: teaType.defaultTemperatureCelsius)?.toDouble(),
                    brewingTime = typeSession.brewingTime,
                    waterType = typeSession.waterType,
                    source = PrefillSource.TeaTypeFallback(
                        sessionId = typeSession.id,
                        teaName = sourceTea?.name ?: "Unknown",
                        rating = typeSession.rating!!,
                    )
                )
            }
        }

        // 4. Fallback: Tea defaults
        val hasDefaults = tea.defaultTemperatureCelsius != null || tea.defaultBrewingTime != null
        if (hasDefaults) {
            val teaTypeDefaults = teaType?.let { type ->
                type.defaultTemperatureCelsius to type.defaultBrewingTime
            }

            return BrewingParametersPrefill(
                teaQuantityGrams = null,
                waterQuantityMl = (tea.defaultQuantity ?: vessel.capacityMl)?.toDouble(),
                temperatureCelsius = (tea.defaultTemperatureCelsius
                    ?: teaTypeDefaults?.first)?.toDouble(),
                brewingTime = tea.defaultBrewingTime
                    ?: teaTypeDefaults?.second,
                waterType = null,
                source = PrefillSource.TeaDefaults,
            )
        }

        // 5. Fallback: Vessel capacity only (when no other defaults exist)
        if (vessel.capacityMl != null) {
            return BrewingParametersPrefill(
                teaQuantityGrams = null,
                waterQuantityMl = vessel.capacityMl?.toDouble(),
                temperatureCelsius = null,
                brewingTime = null,
                waterType = null,
                source = PrefillSource.None,
            )
        }

        // 6. No pre-fill available
        return BrewingParametersPrefill(
            teaQuantityGrams = null,
            waterQuantityMl = null,
            temperatureCelsius = null,
            brewingTime = null,
            waterType = null,
            source = PrefillSource.None,
        )
    }
}
