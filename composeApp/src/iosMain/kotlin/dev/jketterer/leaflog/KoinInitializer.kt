package dev.jketterer.leaflog

import dev.jketterer.leaflog.di.appModule
import dev.jketterer.leaflog.di.databaseModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            appModule,
            databaseModule,
        )
    }
}