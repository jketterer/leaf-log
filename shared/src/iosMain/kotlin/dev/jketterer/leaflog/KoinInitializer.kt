package dev.jketterer.leaflog

import dev.jketterer.leaflog.di.appModule
import dev.jketterer.leaflog.di.platformModule
import dev.jketterer.leaflog.di.repositoryModule
import dev.jketterer.leaflog.di.useCaseModule
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initKoin() {
    startKoin {
        modules(
            appModule,
            platformModule(),
            repositoryModule,
            useCaseModule,
        )
    }

    MainScope().launch {
        KoinPlatform.getKoin().get<InitializeDefaultsUseCase>().invoke()
    }
}