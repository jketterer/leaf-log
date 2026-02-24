package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.presentation.ui.screens.collection.EditTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.EditSessionViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.SessionDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeViewModel
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.quicktimer.QuickTimerViewModel
import dev.jketterer.leaflog.presentation.ui.screens.analytics.AnalyticsViewModel
import dev.jketterer.leaflog.presentation.ui.screens.configuration.CreateBrewingConfigurationViewModel
import dev.jketterer.leaflog.presentation.ui.screens.settings.SettingsViewModel
import dev.jketterer.leaflog.presentation.ui.screens.settings.teatype.EditTeaTypeViewModel
import dev.jketterer.leaflog.presentation.ui.screens.settings.teatype.TeaTypeListViewModel
import dev.jketterer.leaflog.presentation.ui.screens.timer.TimerViewModel
import dev.jketterer.leaflog.presentation.ui.screens.vessel.EditVesselViewModel
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single {
        TimerService(
            coroutineScope = get(),
            notificationService = get(),
            saveTimerStateUseCase = get(),
            lifecycleHandler = get(),
        )
    }

    viewModelOf(::TeaCollectionViewModel)
    viewModelOf(::TeaDetailViewModel)
    viewModelOf(::EditTeaViewModel)
    viewModelOf(::CreateBrewingConfigurationViewModel)

    viewModelOf(::VesselListViewModel)
    viewModelOf(::VesselDetailViewModel)
    viewModelOf(::EditVesselViewModel)

    viewModelOf(::LogTeaViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SessionDetailViewModel)
    viewModelOf(::EditSessionViewModel)

    viewModelOf(::HomeViewModel)

    viewModelOf(::TimerViewModel)
    viewModelOf(::QuickTimerViewModel)

    viewModelOf(::SettingsViewModel)
    viewModelOf(::TeaTypeListViewModel)
    viewModelOf(::EditTeaTypeViewModel)

    viewModelOf(::AnalyticsViewModel)
}