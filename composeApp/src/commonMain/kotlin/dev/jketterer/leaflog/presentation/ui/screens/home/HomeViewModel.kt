package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class HomeViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val getDailyStatsUseCase: GetDailyStatsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        onIntent(HomeIntent.LoadData)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
            is HomeIntent.Refresh -> refresh()
            is HomeIntent.LogTeaClicked -> {
                // navigation handled by UI
            }

            is HomeIntent.SessionClicked -> {
                // navigation handled by UI
            }

            is HomeIntent.ViewAllSessionsClicked -> {
                // navigation handled by UI
            }

            is HomeIntent.DraftBannerClicked -> {
                // navigation handled by UI - navigate to History with drafts filter
            }

            is HomeIntent.SettingsClicked -> {
                // navigation handled by UI
            }

            is HomeIntent.ClearError -> clearError()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Generate greeting
                val greeting = generateGreeting()
                _state.update { it.copy(greeting = greeting) }

                collectDailyStats()
                collectRecentSessions()
                collectDraftCount()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}",
                    )
                }
            }
        }
    }

    private fun collectDailyStats() = viewModelScope.launch {
        getDailyStatsUseCase()
            .catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load statistics: ${e.message}",
                    )
                }
            }
            .collect { stats ->
                _state.update { it.copy(dailyStats = stats) }
            }
    }

    private fun collectRecentSessions() = viewModelScope.launch {
        combine(
            teaSessionRepository.getRecentFlow(limit = 5),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
        ) { sessions, teas, types ->
            Triple(sessions, teas, types)
        }
            .catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load sessions: ${e.message}",
                    )
                }
            }
            .collect { (sessions, teas, types) ->
                // Create lookup maps for efficient access
                val teaMap = teas.associateBy { it.id }
                val typeMap = types.associateBy { it.id }

                _state.update {
                    it.copy(
                        recentSessions = sessions,
                        recentSessionsWithTea = sessions.map { session ->
                            val tea = teaMap[session.teaId]
                            val type = tea?.let { t -> typeMap[t.teaTypeId] }
                            SessionWithTeaData(
                                session = session,
                                teaName = tea?.name ?: "Unknown Tea",
                                teaTypeName = type?.name ?: "Unknown Type",
                                teaPhotoUrl = tea?.photos?.firstOrNull(),
                            )
                        },
                        isEmpty = sessions.isEmpty(),
                        isLoading = false,
                    )
                }
            }
    }

    private fun collectDraftCount() = viewModelScope.launch {
        teaSessionRepository.getDraftsCountFlow()
            .catch { e -> println("Failed to load draft count: ${e.message}") }
            .collect { count -> _state.update { it.copy(draftSessionsCount = count) } }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                // TODO: Trigger Firebase sync when implemented
                // For now, just reload local data
                loadData()
                _state.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = "Refresh failed: ${e.message}",
                    )
                }
            }
        }
    }

    private fun generateGreeting(): String {
        val now = Clock.System.now()
        val hour = now.toLocalDateTime(TimeZone.currentSystemDefault()).hour

        return when (hour) {
            in 0..4 -> "Good night"
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class SessionWithTeaData(
    val session: TeaSession,
    val teaName: String = "Unknown Tea",
    val teaTypeName: String = "Unknown Type",
    val teaPhotoUrl: String? = null,
)