package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.presentation.ui.screens.collection.EditTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.SessionDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeViewModel
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.timer.TimerViewModel
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
        )
    }

    viewModelOf(::TeaCollectionViewModel)
    viewModelOf(::TeaDetailViewModel)
    viewModelOf(::EditTeaViewModel)

    viewModelOf(::LogTeaViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SessionDetailViewModel)

    viewModelOf(::HomeViewModel)

    viewModelOf(::TimerViewModel)
}