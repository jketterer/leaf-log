package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaTypeUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.domain.usecases.MergeTeaTypeUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.ShouldOfferSaveAsMethodUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.DeleteBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.FindMethodUpdateUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.PinBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateMethodFromSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.AttachTeaUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSessionReminderDelayUseCase
import dev.jketterer.leaflog.domain.usecases.session.RecomputeStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompareWithBestUseCase
import dev.jketterer.leaflog.domain.usecases.session.ResolvePlanUseCase
import dev.jketterer.leaflog.domain.usecases.session.ReopenSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
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
    factoryOf(::DeleteTeaTypeUseCase)
    factoryOf(::EditTeaUseCase)
    factoryOf(::MergeTeaTypeUseCase)
    factoryOf(::ToggleFavoriteUseCase)

    // Session use cases
    factoryOf(::GetBrewingParametersPrefillUseCase)
    factoryOf(::GetInProgressSessionInfoUseCase)
    factoryOf(::GetSessionReminderDelayUseCase)

    // Session aggregate writes
    factoryOf(::RecomputeStatsUseCase)
    factoryOf(::StartSessionUseCase)
    factoryOf(::AddSteepUseCase)
    factoryOf(::AttachTeaUseCase)
    factoryOf(::ResolvePlanUseCase)
    factoryOf(::CompareWithBestUseCase)
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
    factoryOf(::ShouldOfferSaveAsMethodUseCase)
    factoryOf(::GenerateConfigurationLabelUseCase)
    factoryOf(::SaveBrewingConfigurationUseCase)
    factoryOf(::FindMethodUpdateUseCase)
    factoryOf(::UpdateMethodFromSessionUseCase)
    factoryOf(::CreateBrewingConfigurationUseCase)
    factoryOf(::UpdateBrewingConfigurationUseCase)
    factoryOf(::DeleteBrewingConfigurationUseCase)
    factoryOf(::PinBrewingConfigurationUseCase)
}