package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::InitializeDefaultsUseCase)
}