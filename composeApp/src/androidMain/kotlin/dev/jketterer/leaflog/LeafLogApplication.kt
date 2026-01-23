package dev.jketterer.leaflog

import android.app.Application
import dev.jketterer.leaflog.di.appModule
import dev.jketterer.leaflog.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LeafLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@LeafLogApplication)
            modules(
                appModule,
                databaseModule,
            )
        }
    }
}