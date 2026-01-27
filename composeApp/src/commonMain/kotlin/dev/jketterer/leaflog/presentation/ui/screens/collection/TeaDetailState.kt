package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType

data class TeaDetailState(
    val tea: Tea? = null,
    val teaType: TeaType? = null,
    val recentSessions: List<TeaSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
)