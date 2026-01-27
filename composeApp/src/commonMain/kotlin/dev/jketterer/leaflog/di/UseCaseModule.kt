package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.DeleteTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateSessionUseCase
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
    factoryOf(::DeleteSessionUseCase)
    factoryOf(::CompleteSessionUseCase)
    factoryOf(::AddSteepUseCase)
    factoryOf(::BrewAgainUseCase)

    // Home screen use cases
    factoryOf(::GetDailyStatsUseCase)
}