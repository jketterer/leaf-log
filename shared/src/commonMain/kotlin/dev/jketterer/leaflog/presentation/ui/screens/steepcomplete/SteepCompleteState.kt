package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class SteepCompleteState(
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val vessel: BrewingVessel? = null,
    val vessels: List<BrewingVessel> = emptyList(),
    val rating: Float = 0f,
    val notes: String = "",
    val vocabulary: List<TastingTag> = emptyList(),
    // Next steep: what the primary button starts
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Double? = null,
    val finishLeads: Boolean = false,
    val showAdjustSheet: Boolean = false,
    // Edit parameters sheet
    val showEditParametersSheet: Boolean = false,
    val editBrewingTime: Duration = Duration.ZERO,
    val editTemperatureCelsius: Double? = null,
    val editWaterQuantityMl: Double? = null,
    val editTeaQuantityGrams: Double? = null,
    val editIsTeaBag: Boolean = false,
    val editWaterType: WaterType? = null,
    val editVessel: BrewingVessel? = null,
    // Discard
    val showDiscardConfirmation: Boolean = false,
    val isLoading: Boolean = true,
    val userPreferences: UserPreferences = UserPreferences(),
)
