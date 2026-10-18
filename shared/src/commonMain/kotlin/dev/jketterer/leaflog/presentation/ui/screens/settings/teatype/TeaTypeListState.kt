package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import dev.jketterer.leaflog.domain.models.TeaType

data class TeaTypeListState(
    val teaTypes: List<TeaType> = emptyList(),
    val isLoading: Boolean = false,
)
