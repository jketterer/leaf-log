package dev.jketterer.leaflog

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.jketterer.leaflog.di.appModule
import dev.jketterer.leaflog.di.databaseModule
import dev.jketterer.leaflog.di.repositoryModule
import dev.jketterer.leaflog.di.useCaseModule
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LeafLogApplication : Application() {

    private val initializeDefaults: InitializeDefaultsUseCase by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@LeafLogApplication)
            modules(
                appModule,
                databaseModule,
                repositoryModule,
                useCaseModule,
            )
        }

        // TODO: i don't like this, but it might be idiomatic? gotta check
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            initializeDefaults()
        }
    }
}