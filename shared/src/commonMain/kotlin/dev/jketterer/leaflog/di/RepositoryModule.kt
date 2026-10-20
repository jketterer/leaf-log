package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.data.local.database.RoomTransactor
import dev.jketterer.leaflog.data.repositories.BackupRepositoryImpl
import dev.jketterer.leaflog.data.repositories.BrewingConfigurationRepositoryImpl
import dev.jketterer.leaflog.data.repositories.BrewingVesselRepositoryImpl
import dev.jketterer.leaflog.data.repositories.PreferencesRepositoryImpl
import dev.jketterer.leaflog.data.repositories.TastingTagRepositoryImpl
import dev.jketterer.leaflog.data.repositories.TeaRepositoryImpl
import dev.jketterer.leaflog.data.repositories.TeaSessionRepositoryImpl
import dev.jketterer.leaflog.data.repositories.TeaTypeRepositoryImpl
import dev.jketterer.leaflog.domain.repositories.BackupRepository
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::TeaTypeRepositoryImpl) bind TeaTypeRepository::class
    singleOf(::BrewingVesselRepositoryImpl) bind BrewingVesselRepository::class
    singleOf(::BrewingConfigurationRepositoryImpl) bind BrewingConfigurationRepository::class
    singleOf(::TeaRepositoryImpl) bind TeaRepository::class
    singleOf(::TeaSessionRepositoryImpl) bind TeaSessionRepository::class
    singleOf(::TastingTagRepositoryImpl) bind TastingTagRepository::class
    singleOf(::PreferencesRepositoryImpl) bind PreferencesRepository::class
    singleOf(::BackupRepositoryImpl) bind BackupRepository::class
    singleOf(::RoomTransactor) bind Transactor::class
}