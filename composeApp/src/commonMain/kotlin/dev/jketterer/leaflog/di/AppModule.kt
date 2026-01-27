package dev.jketterer.leaflog.di

import dev.jketterer.leaflog.presentation.ui.screens.collection.EditTeaViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaCollectionViewModel
import dev.jketterer.leaflog.presentation.ui.screens.collection.TeaDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryViewModel
import dev.jketterer.leaflog.presentation.ui.screens.history.SessionDetailViewModel
import dev.jketterer.leaflog.presentation.ui.screens.home.HomeViewModel
import dev.jketterer.leaflog.presentation.ui.screens.log.LogTeaViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::TeaCollectionViewModel)
    viewModelOf(::TeaDetailViewModel)
    viewModelOf(::EditTeaViewModel)

    viewModelOf(::LogTeaViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SessionDetailViewModel)

    viewModelOf(::HomeViewModel)
}