package dev.jketterer.leaflog

import dev.jketterer.leaflog.di.appModule
import dev.jketterer.leaflog.di.platformModule
import dev.jketterer.leaflog.di.repositoryModule
import dev.jketterer.leaflog.di.useCaseModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            appModule,
            platformModule(),
            repositoryModule,
            useCaseModule,
        )
    }
}