package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.DeleteBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.PinBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.GenerateInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingActivityUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsDateRangeUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingTrendsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSessionReminderDelayUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSteepInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTeaTypeDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTimeInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopRatedTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetVesselDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.DeleteSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.RecomputeStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.ReopenSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.session.aggregate.StartSessionUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
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
    factoryOf(::GetBrewingParametersPrefillUseCase)
    factoryOf(::GetInProgressSessionInfoUseCase)
    factoryOf(::GetSessionReminderDelayUseCase)

    // Session aggregate writes
    factoryOf(::RecomputeStatsUseCase)
    factoryOf(::StartSessionUseCase)
    factoryOf(::AddSteepUseCase)
    factoryOf(::EditSteepUseCase)
    factoryOf(::EditSessionUseCase)
    factoryOf(::DeleteSteepUseCase)
    factoryOf(::FinishSessionUseCase)
    factoryOf(::ReopenSessionUseCase)
    factoryOf(::DiscardSteepUseCase)
    factoryOf(::DeleteSessionUseCase)
    factoryOf(::SaveTimerStateUseCase)

    // Home screen use cases
    factoryOf(::GetDailyStatsUseCase)

    // Analytics use cases
    factoryOf(::GetAnalyticsUseCase)
    factoryOf(::GenerateInsightsUseCase)
    factoryOf(::GetBrewingTrendsUseCase)
    factoryOf(::GetAnalyticsDateRangeUseCase)
    factoryOf(::GetTeaTypeDistributionUseCase)
    factoryOf(::GetTopTeasUseCase)
    factoryOf(::GetBrewingActivityUseCase)
    factoryOf(::GetSteepInsightsUseCase)
    factoryOf(::GetTopRatedTeasUseCase)
    factoryOf(::GetVesselDistributionUseCase)
    factoryOf(::GetTimeInsightsUseCase)

    // Timer use cases
    factoryOf(::StartTimerUseCase)
    factoryOf(::PauseTimerUseCase)
    factoryOf(::ResumeTimerUseCase)
    factoryOf(::AdjustTimeUseCase)
    factoryOf(::RestoreTimerStateUseCase)

    // Brewing vessel use cases
    factoryOf(::CreateBrewingVesselUseCase)
    factoryOf(::DeleteBrewingVesselUseCase)
    factoryOf(::UpdateBrewingVesselUseCase)

    // Brewing configuration use cases
    factoryOf(::CheckDuplicateConfigurationUseCase)
    factoryOf(::GenerateConfigurationLabelUseCase)
    factoryOf(::SaveBrewingConfigurationUseCase)
    factoryOf(::CreateBrewingConfigurationUseCase)
    factoryOf(::UpdateBrewingConfigurationUseCase)
    factoryOf(::DeleteBrewingConfigurationUseCase)
    factoryOf(::PinBrewingConfigurationUseCase)
}