package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.DeleteBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.data.ExportDataUseCase
import dev.jketterer.leaflog.domain.usecases.data.ImportDataUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.ExportAnalyticsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GenerateInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingActivityUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingTrendsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSteepInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTeaTypeDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopRatedTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetVesselDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateAverageRatingUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateTeaStatsUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CancelTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CompleteTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
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
    factoryOf(::UpdateTeaStatsUseCase)
    factoryOf(::DeleteSessionUseCase)
    factoryOf(::CompleteSessionUseCase)
    factoryOf(::AddSteepUseCase)
    factoryOf(::BrewAgainUseCase)
    factoryOf(::GetBrewingParametersPrefillUseCase)

    // Home screen use cases
    factoryOf(::GetDailyStatsUseCase)

    // Analytics use cases
    factoryOf(::GetAnalyticsUseCase)
    factoryOf(::GenerateInsightsUseCase)
    factoryOf(::GetBrewingTrendsUseCase)
    factoryOf(::GetTeaTypeDistributionUseCase)
    factoryOf(::GetTopTeasUseCase)
    factoryOf(::ExportAnalyticsUseCase)
    factoryOf(::GetBrewingActivityUseCase)
    factoryOf(::GetSteepInsightsUseCase)
    factoryOf(::GetTopRatedTeasUseCase)
    factoryOf(::GetVesselDistributionUseCase)

    // Timer use cases
    factoryOf(::StartTimerUseCase)
    factoryOf(::PauseTimerUseCase)
    factoryOf(::ResumeTimerUseCase)
    factoryOf(::AdjustTimeUseCase)
    factoryOf(::CompleteTimerUseCase)
    factoryOf(::CancelTimerUseCase)
    factoryOf(::SaveTimerStateUseCase)
    factoryOf(::RestoreTimerStateUseCase)

    // Brewing vessel use cases
    factoryOf(::CreateBrewingVesselUseCase)
    factoryOf(::DeleteBrewingVesselUseCase)
    factoryOf(::UpdateBrewingVesselUseCase)

    // Data export/import use cases
    factoryOf(::ExportDataUseCase)
    factoryOf(::ImportDataUseCase)

    // Brewing configuration use cases
    factoryOf(::GenerateConfigurationLabelUseCase)
    factoryOf(::SaveBrewingConfigurationUseCase)
    factoryOf(::CreateBrewingConfigurationUseCase)
    factoryOf(::UpdateBrewingConfigurationUseCase)
    factoryOf(::DeleteBrewingConfigurationUseCase)
}