package dev.jketterer.leaflog.di

import android.content.Context
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinGraphTest {

    @Test
    fun `every definition can resolve its constructor dependencies`() {
        module {
            includes(appModule, repositoryModule, useCaseModule, platformModule())
        }.verify(
            // Supplied by androidContext() at startup
            extraTypes = listOf(Context::class),
        )
    }
}
