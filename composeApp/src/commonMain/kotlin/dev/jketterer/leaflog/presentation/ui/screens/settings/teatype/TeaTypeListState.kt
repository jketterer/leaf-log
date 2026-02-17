package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences

data class TeaTypeListState(
    val teaTypes: List<TeaType> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
