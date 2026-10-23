package dev.jketterer.leaflog.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinGraphTest {

    @Test
    fun `every definition can resolve its constructor dependencies`() {
        module {
            includes(appModule, repositoryModule, useCaseModule, platformModule())
        }.verify(
            extraTypes = listOf(
                // Supplied by androidContext() at startup
                Context::class,
                // Supplied by each nav entry's ViewModel store
                SavedStateHandle::class,
                // Supplied by each screen through parametersOf(route)
                NavRoute.HomeRoute::class,
                NavRoute.LogTeaRoute::class,
                NavRoute.TeaDetailsRoute::class,
                NavRoute.EditTeaRoute::class,
                NavRoute.HistoryRoute::class,
                NavRoute.SessionDetailsRoute::class,
                NavRoute.EditSessionRoute::class,
                NavRoute.TimerRoute::class,
                NavRoute.VesselDetailRoute::class,
                NavRoute.EditVesselRoute::class,
                NavRoute.EditTeaTypeRoute::class,
                NavRoute.CreateBrewingConfigurationRoute::class,
                NavRoute.SteepCompleteRoute::class,
                NavRoute.SessionReviewRoute::class,
                NavRoute.RecapRoute::class,
            ),
        )
    }
}
