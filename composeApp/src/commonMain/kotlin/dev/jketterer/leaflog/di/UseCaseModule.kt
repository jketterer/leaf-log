package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.DeleteBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateAverageRatingUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CancelTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CompleteTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import dev.jketterer.leaflog.domain.usecases.preferences.GetPreferencesUseCase
import dev.jketterer.leaflog.domain.usecases.preferences.UpdateTemperatureUnitUseCase
import dev.jketterer.leaflog.domain.usecases.preferences.UpdateVolumeUnitUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.CreateBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.DeleteBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.UpdateBrewingVesselUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    // Initialization
    factoryOf(::InitializeDefaultsUseCase)

    // Tea use cases
    factoryOf(::CreateTeaUseCase)
    factoryOf(::DeleteTeaUseCase)
    factoryOf(::EditTeaUseCase)
    factoryOf(::SearchTeasUseCase)
    factoryOf(::ToggleFavoriteUseCase)

    // Session use cases
    factoryOf(::CreateSessionUseCase)
    factoryOf(::UpdateSessionUseCase)
    factoryOf(::UpdateAverageRatingUseCase)
    factoryOf(::DeleteSessionUseCase)
    factoryOf(::CompleteSessionUseCase)
    factoryOf(::AddSteepUseCase)
    factoryOf(::BrewAgainUseCase)
    factoryOf(::GetBrewingParametersPrefillUseCase)

    // Home screen use cases
    factoryOf(::GetDailyStatsUseCase)

    // Timer use cases
    factoryOf(::StartTimerUseCase)
    factoryOf(::PauseTimerUseCase)
    factoryOf(::ResumeTimerUseCase)
    factoryOf(::AdjustTimeUseCase)
    factoryOf(::CompleteTimerUseCase)
    factoryOf(::CancelTimerUseCase)

    // Brewing vessel use cases
    factoryOf(::CreateBrewingVesselUseCase)
    factoryOf(::DeleteBrewingVesselUseCase)
    factoryOf(::UpdateBrewingVesselUseCase)

    // Brewing configuration use cases
    factoryOf(::GenerateConfigurationLabelUseCase)
    factoryOf(::SaveBrewingConfigurationUseCase)
    factoryOf(::UpdateBrewingConfigurationUseCase)
    factoryOf(::DeleteBrewingConfigurationUseCase)

    // Preferences use cases
    factoryOf(::GetPreferencesUseCase)
    factoryOf(::UpdateTemperatureUnitUseCase)
    factoryOf(::UpdateVolumeUnitUseCase)
}