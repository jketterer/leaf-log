# Leaf Log App - Architecture Plan
**Compose Multiplatform Application for Android & iOS**

---

## Executive Summary

**Leaf Log** is a medium-scale mobile application designed to help tea enthusiasts log, track, and analyze their tea brewing sessions. The app follows an offline-first approach with optional cloud synchronization, providing a seamless experience regardless of network connectivity.

### Key Highlights
- **Platforms**: Android & iOS using Compose Multiplatform
- **Architecture**: Clean Architecture with MVI (Model-View-Intent) pattern
- **Data Strategy**: Offline-first with Room database and optional Firebase sync
- **Scale**: 10-20 screens with comprehensive feature set
- **Core Features**: Session logging, tea collection, analytics, timers, notifications, widgets

---

## Technical Stack

### Shared (Kotlin Multiplatform)
| Category | Technology | Purpose |
|----------|-----------|---------|
| **Language** | Kotlin 2.1.0+ | Primary development language |
| **UI Framework** | Compose Multiplatform | Declarative UI across platforms |
| **Architecture** | Clean Architecture | Domain, Data, Presentation layers |
| **Dependency Injection** | Koin | Lightweight DI framework |
| **Database** | Room (KMP) | Local data persistence with encryption |
| **Serialization** | Kotlinx Serialization | JSON serialization/deserialization |
| **Networking** | Ktor Client | HTTP client for Firebase APIs |
| **Image Loading** | Coil3 | Multiplatform image loading |
| **Date/Time** | Kotlinx DateTime | Date and time utilities |
| **Coroutines** | Kotlinx Coroutines | Asynchronous programming |
| **Testing** | Kotlin Test, Mockk | Unit and integration testing |

### Platform-Specific

#### Android
| Category | Technology | Purpose |
|----------|-----------|---------|
| **Min SDK** | API 26 (Android 8.0) | Minimum supported version |
| **Target SDK** | API 35+ | Latest Android features |
| **Material Design** | Material 3 (Compose) | UI design system |
| **Navigation** | Compose Navigation | Screen navigation |
| **Work Manager** | WorkManager | Background tasks for sync |
| **Notifications** | NotificationManager | Brewing timers, reminders |
| **Widgets** | Glance | Home screen widgets |
| **Storage** | Android Storage Access Framework | Photo storage |

#### iOS
| Category | Technology | Purpose |
|----------|-----------|---------|
| **Min Version** | iOS 15+ | Minimum supported version |
| **Material Design** | Material 3 (Compose) | Consistent cross-platform UI |
| **Navigation** | Compose Navigation | Screen navigation |
| **Background Tasks** | BackgroundTasks framework | Background sync |
| **Notifications** | UserNotifications | Brewing timers, reminders |
| **Widgets** | WidgetKit (SwiftUI) | Home screen widgets |
| **Storage** | iOS file system | Photo storage |

### Backend Services (Optional)
| Service | Purpose | Priority |
|---------|---------|----------|
| **Firebase Authentication** | Optional user accounts for cloud sync | Medium |
| **Cloud Firestore** | Optional cloud data synchronization | Medium |
| **Firebase Storage** | Optional photo cloud backup | Low |
| **Firebase Analytics** | Optional usage analytics | Low |
| **Crashlytics** | Optional crash reporting | Low |

---

## Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER (MVI)                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  UI (Compose Multiplatform)                         │   │
│  │  - Screens (View)                                   │   │
│  │  - ViewModels                                       │   │
│  │  - UI State (Model)                                 │   │
│  │  - User Intents/Events (Intent)                     │   │
│  │  - Navigation                                       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▼ Uses
┌─────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Business Logic (Pure Kotlin)                       │   │
│  │  - Use Cases / Interactors                          │   │
│  │  - Domain Models                                    │   │
│  │  - Repository Interfaces                            │   │
│  │  - Business Rules                                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▼ Implemented by
┌─────────────────────────────────────────────────────────────┐
│                        DATA LAYER                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Data Sources & Repositories                        │   │
│  │  - Repository Implementations                       │   │
│  │  - Local Data Source (Room)                         │   │
│  │  - Remote Data Source (Firebase - Optional)         │   │
│  │  - Data Models / Entities                           │   │
│  │  - Data Mappers                                     │   │
│  │  - Sync Engine                                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▼ Persists to
┌─────────────────────────────────────────────────────────────┐
│                    PLATFORM LAYER                            │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │  Android Platform    │  │  iOS Platform        │        │
│  │  - Activities        │  │  - UIViewController  │        │
│  │  - Services          │  │  - AppDelegate       │        │
│  │  - Broadcast Rcvrs   │  │  - Background Tasks  │        │
│  │  - Widgets (Glance)  │  │  - Widgets (SwiftUI) │        │
│  │  - Notifications     │  │  - Notifications     │        │
│  └──────────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Key Architecture Principles

1. **Dependency Rule**: Dependencies point inward. Domain layer has no dependencies on outer layers.
2. **Single Source of Truth**: Room database is the primary data source; Firebase is secondary sync layer.
3. **Unidirectional Data Flow (MVI)**: User Intent → ViewModel → State Update → UI Render.
4. **Immutable State**: Each screen has a single immutable state object that represents the entire UI.
5. **Separation of Concerns**: Each layer has a specific responsibility and clear boundaries.
6. **Testability**: Each layer can be tested independently with mock implementations.

### MVI Pattern in Presentation Layer

The app uses **Model-View-Intent (MVI)** for state management:

```
User Action (Intent)
    ↓
ViewModel processes intent
    ↓
Use Cases execute business logic
    ↓
New State emitted
    ↓
UI observes and renders state
```

**Key MVI Components**:
- **Model (State)**: Single immutable data class representing UI state
- **View (Screen)**: Compose UI that renders state and emits intents
- **Intent (Events)**: Sealed class/interface representing user actions

**Example Pattern**:
```kotlin
// State - Single source of truth
data class HomeState(
    val recentSessions: List<TeaSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dailyStats: DailyStats? = null
)

// Intent - All possible user actions
sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data object LogTeaClicked : HomeIntent
    data object RefreshClicked : HomeIntent
    data class SessionClicked(val sessionId: String) : HomeIntent
}

// ViewModel - Processes intents and emits states
class HomeViewModel(
    private val getRecentSessionsUseCase: GetRecentSessionsUseCase,
    private val getDailyStatsUseCase: GetDailyStatsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
            is HomeIntent.LogTeaClicked -> navigateToLogTea()
            is HomeIntent.RefreshClicked -> refreshData()
            is HomeIntent.SessionClicked -> navigateToSession(intent.sessionId)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val sessions = getRecentSessionsUseCase()
                val stats = getDailyStatsUseCase()
                _state.update {
                    it.copy(
                        recentSessions = sessions,
                        dailyStats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }
}

// Screen - Observes state and sends intents
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit
) {
    Column {
        if (state.isLoading) {
            LoadingIndicator()
        }

        state.dailyStats?.let { stats ->
            DailyStatsCard(stats)
        }

        LazyColumn {
            items(state.recentSessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { onIntent(HomeIntent.SessionClicked(session.id)) }
                )
            }
        }

        FloatingActionButton(
            onClick = { onIntent(HomeIntent.LogTeaClicked) }
        ) {
            Icon(Icons.Default.Add, "Log Tea")
        }
    }
}
```

---

## Module Structure

```
leaf-log/
├── androidApp/                          # Android platform-specific code
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/
│   │       │   └── com.leaflog.android/
│   │       │       ├── MainActivity.kt
│   │       │       ├── LeafLogApplication.kt
│   │       │       ├── di/
│   │       │       │   └── AndroidModule.kt
│   │       │       ├── widgets/
│   │       │       │   ├── QuickLogWidget.kt
│   │       │       │   └── StatsWidget.kt
│   │       │       ├── notifications/
│   │       │       │   ├── NotificationHelper.kt
│   │       │       │   └── TimerNotificationService.kt
│   │       │       └── workers/
│   │       │           └── SyncWorker.kt
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── iosApp/                              # iOS platform-specific code
│   ├── iosApp/
│   │   ├── iOSApp.swift
│   │   ├── ContentView.swift
│   │   └── AppDelegate.swift
│   ├── iosWidgets/                      # iOS widgets (SwiftUI)
│   │   ├── QuickLogWidget.swift
│   │   └── StatsWidget.swift
│   └── iosAppUITests/
│
├── shared/                              # Shared Kotlin Multiplatform code
│   ├── src/
│   │   ├── commonMain/kotlin/com.leaflog/
│   │   │   │
│   │   │   ├── presentation/           # Presentation Layer
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── home/
│   │   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   │   │   ├── HomeState.kt
│   │   │   │   │   │   │   └── HomeIntent.kt
│   │   │   │   │   │   ├── log/
│   │   │   │   │   │   │   ├── LogTeaScreen.kt
│   │   │   │   │   │   │   ├── LogTeaViewModel.kt
│   │   │   │   │   │   │   ├── LogTeaState.kt
│   │   │   │   │   │   │   └── LogTeaIntent.kt
│   │   │   │   │   │   ├── library/
│   │   │   │   │   │   │   ├── TeaLibraryScreen.kt
│   │   │   │   │   │   │   ├── TeaLibraryViewModel.kt
│   │   │   │   │   │   │   ├── TeaLibraryState.kt
│   │   │   │   │   │   │   ├── TeaLibraryIntent.kt
│   │   │   │   │   │   │   ├── TeaDetailScreen.kt
│   │   │   │   │   │   │   ├── TeaDetailViewModel.kt
│   │   │   │   │   │   │   ├── TeaDetailState.kt
│   │   │   │   │   │   │   ├── TeaDetailIntent.kt
│   │   │   │   │   │   │   ├── AddEditTeaScreen.kt
│   │   │   │   │   │   │   ├── AddEditTeaViewModel.kt
│   │   │   │   │   │   │   ├── AddEditTeaState.kt
│   │   │   │   │   │   │   └── AddEditTeaIntent.kt
│   │   │   │   │   │   ├── timer/
│   │   │   │   │   │   │   ├── BrewingTimerScreen.kt
│   │   │   │   │   │   │   ├── TimerViewModel.kt
│   │   │   │   │   │   │   ├── TimerState.kt
│   │   │   │   │   │   │   └── TimerIntent.kt
│   │   │   │   │   │   ├── history/
│   │   │   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   │   │   ├── HistoryViewModel.kt
│   │   │   │   │   │   │   ├── HistoryState.kt
│   │   │   │   │   │   │   ├── HistoryIntent.kt
│   │   │   │   │   │   │   ├── SessionDetailScreen.kt
│   │   │   │   │   │   │   ├── SessionDetailViewModel.kt
│   │   │   │   │   │   │   ├── SessionDetailState.kt
│   │   │   │   │   │   │   ├── SessionDetailIntent.kt
│   │   │   │   │   │   │   └── FilterSheet.kt
│   │   │   │   │   │   ├── analytics/
│   │   │   │   │   │   │   ├── AnalyticsScreen.kt
│   │   │   │   │   │   │   ├── AnalyticsViewModel.kt
│   │   │   │   │   │   │   ├── AnalyticsState.kt
│   │   │   │   │   │   │   ├── AnalyticsIntent.kt
│   │   │   │   │   │   │   └── charts/
│   │   │   │   │   │   │       ├── BrewingTrendsChart.kt
│   │   │   │   │   │   │       ├── FavoriteTeaChart.kt
│   │   │   │   │   │   │       └── TemperatureDistribution.kt
│   │   │   │   │   │   ├── settings/
│   │   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   │   │   ├── SettingsState.kt
│   │   │   │   │   │   │   ├── SettingsIntent.kt
│   │   │   │   │   │   │   ├── AccountSettingsScreen.kt
│   │   │   │   │   │   │   ├── NotificationSettingsScreen.kt
│   │   │   │   │   │   │   └── BackupRestoreScreen.kt
│   │   │   │   │   │   └── auth/
│   │   │   │   │   │       ├── SignInScreen.kt
│   │   │   │   │   │       ├── AuthViewModel.kt
│   │   │   │   │   │       ├── AuthState.kt
│   │   │   │   │   │       └── AuthIntent.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── home/
│   │   │   │   │   │   │   ├── DailyStatsCard.kt
│   │   │   │   │   │   │   ├── QuickActionCard.kt
│   │   │   │   │   │   │   └── RecentSessionList.kt
│   │   │   │   │   │   ├── library/
│   │   │   │   │   │   │   ├── TeaCard.kt
│   │   │   │   │   │   │   ├── TeaListItem.kt
│   │   │   │   │   │   │   ├── TeaFilterChips.kt
│   │   │   │   │   │   │   ├── StockLevelIndicator.kt
│   │   │   │   │   │   │   └── TeaTypeSelector.kt
│   │   │   │   │   │   ├── session/
│   │   │   │   │   │   │   ├── SessionCard.kt
│   │   │   │   │   │   │   ├── SessionListItem.kt
│   │   │   │   │   │   │   ├── BrewingParameterInput.kt
│   │   │   │   │   │   │   ├── BrewingParameterDisplay.kt
│   │   │   │   │   │   │   └── VesselSelector.kt
│   │   │   │   │   │   ├── timer/
│   │   │   │   │   │   │   ├── TimerDisplay.kt
│   │   │   │   │   │   │   ├── CircularTimerRing.kt
│   │   │   │   │   │   │   ├── TimerControls.kt
│   │   │   │   │   │   │   └── QuickTimeAdjustButtons.kt
│   │   │   │   │   │   ├── analytics/
│   │   │   │   │   │   │   ├── StatCard.kt
│   │   │   │   │   │   │   ├── PeriodSelector.kt
│   │   │   │   │   │   │   └── InsightCard.kt
│   │   │   │   │   │   └── common/
│   │   │   │   │   │       ├── RatingBar.kt
│   │   │   │   │   │       ├── ImagePicker.kt
│   │   │   │   │   │       ├── EmptyState.kt
│   │   │   │   │   │       ├── LoadingIndicator.kt
│   │   │   │   │   │       ├── ErrorDisplay.kt
│   │   │   │   │   │       └── PhotoGallery.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   │   ├── Screen.kt
│   │   │   │   │   │   └── NavigationArgs.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Typography.kt
│   │   │   │   │       └── Shapes.kt
│   │   │   │   └── models/
│   │   │   │       ├── UiState.kt             # Base UI state sealed class
│   │   │   │       └── UiIntent.kt            # Base UI intent interface
│   │   │   │
│   │   │   ├── domain/                 # Domain Layer
│   │   │   │   ├── models/
│   │   │   │   │   ├── TeaSession.kt
│   │   │   │   │   ├── Tea.kt
│   │   │   │   │   ├── TeaType.kt
│   │   │   │   │   ├── BrewingVessel.kt
│   │   │   │   │   ├── WaterType.kt
│   │   │   │   │   ├── TeaStatistics.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── UserPreferences.kt
│   │   │   │   │   ├── SessionStatus.kt
│   │   │   │   │   └── SyncStatus.kt
│   │   │   │   ├── repositories/
│   │   │   │   │   ├── TeaSessionRepository.kt
│   │   │   │   │   ├── TeaRepository.kt
│   │   │   │   │   ├── TeaTypeRepository.kt
│   │   │   │   │   ├── BrewingVesselRepository.kt
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   ├── PreferencesRepository.kt
│   │   │   │   │   ├── SyncRepository.kt
│   │   │   │   │   └── ImageRepository.kt
│   │   │   │   └── usecases/
│   │   │   │       ├── session/
│   │   │   │       │   ├── CreateTeaSessionUseCase.kt
│   │   │   │       │   ├── GetTeaSessionsUseCase.kt
│   │   │   │       │   ├── GetSessionByIdUseCase.kt
│   │   │   │       │   ├── UpdateTeaSessionUseCase.kt
│   │   │   │       │   ├── DeleteTeaSessionUseCase.kt
│   │   │   │       │   └── SearchSessionsUseCase.kt
│   │   │   │       ├── tea/
│   │   │   │       │   ├── CreateTeaUseCase.kt
│   │   │   │       │   ├── GetAllTeasUseCase.kt
│   │   │   │       │   ├── GetTeaByIdUseCase.kt
│   │   │   │       │   ├── UpdateTeaUseCase.kt
│   │   │   │       │   ├── DeleteTeaUseCase.kt
│   │   │   │       │   ├── UpdateTeaStockUseCase.kt
│   │   │   │       │   └── SearchTeasUseCase.kt
│   │   │   │       ├── teatype/
│   │   │   │       │   ├── GetAllTeaTypesUseCase.kt
│   │   │   │       │   ├── CreateTeaTypeUseCase.kt
│   │   │   │       │   ├── UpdateTeaTypeUseCase.kt
│   │   │   │       │   ├── DeleteTeaTypeUseCase.kt
│   │   │   │       │   ├── ReorderTeaTypesUseCase.kt
│   │   │   │       │   └── InitializeDefaultTeaTypesUseCase.kt
│   │   │   │       ├── vessel/
│   │   │   │       │   ├── GetAllBrewingVesselsUseCase.kt
│   │   │   │       │   ├── CreateBrewingVesselUseCase.kt
│   │   │   │       │   ├── UpdateBrewingVesselUseCase.kt
│   │   │   │       │   ├── DeleteBrewingVesselUseCase.kt
│   │   │   │       │   ├── ReorderBrewingVesselsUseCase.kt
│   │   │   │       │   └── InitializeDefaultVesselsUseCase.kt
│   │   │   │       ├── analytics/
│   │   │   │       │   ├── GetBrewingStatisticsUseCase.kt
│   │   │   │       │   ├── GetFavoriteTeaUseCase.kt
│   │   │   │       │   ├── GetBrewingTrendsUseCase.kt
│   │   │   │       │   └── GetTemperatureDistributionUseCase.kt
│   │   │   │       ├── timer/
│   │   │   │       │   ├── StartBrewingTimerUseCase.kt
│   │   │   │       │   ├── PauseTimerUseCase.kt
│   │   │   │       │   ├── ResetTimerUseCase.kt
│   │   │   │       │   └── GetTimerStateUseCase.kt
│   │   │   │       ├── sync/
│   │   │   │       │   ├── SyncDataUseCase.kt
│   │   │   │       │   ├── EnableSyncUseCase.kt
│   │   │   │       │   └── DisableSyncUseCase.kt
│   │   │   │       ├── backup/
│   │   │   │       │   ├── ExportDataUseCase.kt
│   │   │   │       │   ├── ImportDataUseCase.kt
│   │   │   │       │   └── BackupToCloudUseCase.kt
│   │   │   │       └── auth/
│   │   │   │           ├── SignInUseCase.kt
│   │   │   │           ├── SignOutUseCase.kt
│   │   │   │           └── GetCurrentUserUseCase.kt
│   │   │   │
│   │   │   ├── data/                   # Data Layer
│   │   │   │   ├── repositories/
│   │   │   │   │   ├── TeaSessionRepositoryImpl.kt
│   │   │   │   │   ├── TeaRepositoryImpl.kt
│   │   │   │   │   ├── TeaTypeRepositoryImpl.kt
│   │   │   │   │   ├── BrewingVesselRepositoryImpl.kt
│   │   │   │   │   ├── UserRepositoryImpl.kt
│   │   │   │   │   ├── PreferencesRepositoryImpl.kt
│   │   │   │   │   ├── SyncRepositoryImpl.kt
│   │   │   │   │   └── ImageRepositoryImpl.kt
│   │   │   │   ├── local/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── LeafLogDatabase.kt
│   │   │   │   │   │   ├── entities/
│   │   │   │   │   │   │   ├── TeaSessionEntity.kt
│   │   │   │   │   │   │   ├── TeaEntity.kt
│   │   │   │   │   │   │   ├── TeaTypeEntity.kt
│   │   │   │   │   │   │   ├── BrewingVesselEntity.kt
│   │   │   │   │   │   │   ├── UserEntity.kt
│   │   │   │   │   │   │   └── SyncQueueEntity.kt
│   │   │   │   │   │   └── dao/
│   │   │   │   │   │       ├── TeaSessionDao.kt
│   │   │   │   │   │       ├── TeaDao.kt
│   │   │   │   │   │       ├── TeaTypeDao.kt
│   │   │   │   │   │       ├── BrewingVesselDao.kt
│   │   │   │   │   │       ├── UserDao.kt
│   │   │   │   │   │       └── SyncQueueDao.kt
│   │   │   │   │   ├── preferences/
│   │   │   │   │   │   └── AppPreferences.kt
│   │   │   │   │   └── datasource/
│   │   │   │   │       └── LocalDataSource.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── firebase/
│   │   │   │   │   │   ├── FirebaseAuthService.kt
│   │   │   │   │   │   ├── FirestoreService.kt
│   │   │   │   │   │   └── FirebaseStorageService.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── TeaSessionDto.kt
│   │   │   │   │   │   ├── TeaDto.kt
│   │   │   │   │   │   ├── TeaTypeDto.kt
│   │   │   │   │   │   ├── BrewingVesselDto.kt
│   │   │   │   │   │   └── UserDto.kt
│   │   │   │   │   └── datasource/
│   │   │   │   │       └── RemoteDataSource.kt
│   │   │   │   ├── sync/
│   │   │   │   │   ├── SyncEngine.kt
│   │   │   │   │   ├── SyncStrategy.kt
│   │   │   │   │   ├── ConflictResolver.kt
│   │   │   │   │   └── CleanupScheduler.kt
│   │   │   │   └── mappers/
│   │   │   │       ├── TeaSessionMapper.kt
│   │   │   │       ├── TeaMapper.kt
│   │   │   │       ├── TeaTypeMapper.kt
│   │   │   │       ├── BrewingVesselMapper.kt
│   │   │   │       └── UserMapper.kt
│   │   │   │
│   │   │   └── di/                     # Dependency Injection
│   │   │       ├── AppModule.kt
│   │   │       ├── DatabaseModule.kt
│   │   │       ├── RepositoryModule.kt
│   │   │       ├── UseCaseModule.kt
│   │   │       └── NetworkModule.kt
│   │   │
│   │   ├── androidMain/kotlin/com.leaflog/
│   │   │   ├── di/
│   │   │   │   └── AndroidPlatformModule.kt
│   │   │   ├── util/
│   │   │   │   ├── AndroidImageUtil.kt
│   │   │   │   └── AndroidFileUtil.kt
│   │   │   └── Platform.android.kt
│   │   │
│   │   ├── iosMain/kotlin/com.leaflog/
│   │   │   ├── di/
│   │   │   │   └── IosPlatformModule.kt
│   │   │   ├── util/
│   │   │   │   ├── IosImageUtil.kt
│   │   │   │   └── IosFileUtil.kt
│   │   │   └── Platform.ios.kt
│   │   │
│   │   ├── commonTest/kotlin/com.leaflog/
│   │   │   ├── domain/
│   │   │   │   └── usecases/
│   │   │   ├── data/
│   │   │   │   ├── repositories/
│   │   │   │   └── mappers/
│   │   │   └── util/
│   │   │       └── TestData.kt
│   │   │
│   │   ├── androidUnitTest/
│   │   └── iosTest/
│   │
│   └── build.gradle.kts
│
├── gradle/
│   ├── libs.versions.toml               # Version catalog
│   └── wrapper/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Data Models

### Core Domain Models

#### 1. TeaSession
Primary model representing a single tea brewing session or individual steep.

```kotlin
data class TeaSession(
    val id: String,
    val teaId: String,                    // Reference to Tea

    // Multi-steep support (Gong-fu brewing)
    val parentSessionId: String?,         // null = parent session, ID = child steep
    val steepNumber: Int,                 // 1, 2, 3, 4... (always 1 for single-steep sessions)
    val status: SessionStatus,            // DRAFT or COMPLETED

    // Session-level fields (only meaningful on parent, steep 1)
    val teaQuantityGrams: Int?,           // Amount of tea leaves (optional, tea bags)
    val vesselId: String,                 // Reference to BrewingVessel
    val waterType: WaterType,             // Type of water used
    val location: String?,                // Optional location
    val rating: Float?,                   // User rating 0.0-5.0 (only on parent)

    // Per-steep fields (can vary for each steep)
    val timestamp: Instant,               // When this steep was brewed
    val brewingTime: Duration,            // How long it steeped
    val temperatureCelsius: Int,          // Water temp (stored in Celsius, converted for display)
    val waterQuantityMl: Int,             // Water quantity in ml (converted to oz for display)
    val notes: String?,                   // Tasting notes (per steep)
    val photos: List<String>,             // Photo file paths/URIs (per steep)

    // Metadata
    val userId: String?,                  // For sync (optional)
    val syncStatus: SyncStatus,           // Sync state
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?               // Soft delete for sync
)
```

**Multi-Steep Session Design**:
- **Parent session**: `parentSessionId = null`, `steepNumber = 1`
- **Child steeps**: `parentSessionId = <parent ID>`, `steepNumber = 2, 3, 4...`
- **Session-level fields** (shared): `teaQuantityGrams`, `vesselId`, `waterType`, `location`, `rating` (only stored on parent)
- **Per-steep fields** (independent): `timestamp`, `brewingTime`, `temperatureCelsius`, `waterQuantityMl`, `notes`, `photos`
- **Single-steep sessions**: Just one record with `steepNumber = 1`, no children

**Session Status**:
```kotlin
enum class SessionStatus {
    DRAFT,      // Started timer/logging but not finished (no rating)
    COMPLETED   // Finished and saved with all details
}
```

**Draft Sessions**:
- Sessions remain in DRAFT status until user finishes them
- Drafts are kept forever (not auto-deleted)
- Shown on Home screen ("X draft sessions") and History screen (with badge)
- User can finish or discard drafts at any time

**Temperature Handling**:
- Stored internally in **Celsius** for consistency
- User preference (C/F) determines display unit
- Conversion utilities handle display/input transformations

**Tea Quantity Tracking**:
- `teaQuantityGrams`: Amount of tea leaves used (optional for tea bags/sachets)
- `waterQuantityMl`: Amount of water used per steep
- For analytics: sum water quantities across all steeps, count tea quantity only once (from parent)

#### 2. Tea
Represents a tea in the user's library/inventory.

```kotlin
data class Tea(
    val id: String,
    val name: String,
    val teaTypeId: String,                // Reference to TeaType
    val origin: String?,                  // Origin/region
    val producer: String?,                // Brand/producer
    val purchaseDate: LocalDate?,
    val purchasePrice: Double?,
    val stockAmount: Int?,                // Grams remaining
    val defaultBrewingTime: Duration?,    // Recommended brewing time
    val defaultTemperatureCelsius: Int?,  // Recommended temperature (stored in Celsius)
    val defaultQuantity: Int?,            // Recommended quantity
    val description: String?,
    val photos: List<String>,
    val isFavorite: Boolean,
    val totalSessions: Int,               // Computed: number of sessions
    val averageRating: Float?,            // Computed: average rating
    val lastBrewedAt: Instant?,          // Computed: last session timestamp
    val userId: String?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?
)
```

#### 3. TeaType
Represents a tea category/type. User-customizable with system defaults.

```kotlin
data class TeaType(
    val id: String,
    val name: String,                     // Display name (user can rename)
    val defaultTemperatureCelsius: Int?,  // Typical brewing temperature (Celsius)
    val defaultBrewingTime: Duration?,    // Typical brewing time
    val color: Color,                     // UI color for this type
    val isSystemDefault: Boolean,         // True for pre-installed types
    val displayOrder: Int,                // User can reorder types
    val userId: String?,                  // For sync (optional)
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?               // Soft delete for sync
)
```

**System Default Tea Types** (pre-populated on first launch):
- Green (70-80°C, 2-3 minutes)
- Black (90-100°C, 3-5 minutes)
- White (70-75°C, 4-5 minutes)
- Oolong (85-95°C, 3-5 minutes)
- Pu-erh (95-100°C, 3-5 minutes)
- Herbal (100°C, 5-7 minutes)
- Rooibos (100°C, 5-7 minutes)
- Yellow (75-80°C, 2-3 minutes)
- Dark (95-100°C, 3-5 minutes)
- Blended (varies)
- Other (varies)

**User Customization**:
- Users can rename types (e.g., "绿茶" instead of "Green")
- Users can add custom types
- Users can reorder types for their preference
- System defaults cannot be deleted, only hidden

#### 4. BrewingVessel
Represents a brewing vessel/method. User-customizable with system defaults.

```kotlin
data class BrewingVessel(
    val id: String,
    val name: String,                     // Display name (user can rename)
    val iconName: String?,                // Icon resource name
    val isSystemDefault: Boolean,         // True for pre-installed vessels
    val displayOrder: Int,                // User can reorder vessels
    val userId: String?,                  // For sync (optional)
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?               // Soft delete for sync
)
```

**System Default Brewing Vessels** (pre-populated on first launch):
- Gaiwan
- Teapot
- Kyusu (Japanese side-handle teapot)
- Yixing Pot (Chinese clay teapot)
- Glass Teapot
- Mug
- Travel Mug
- Grandpa Style (leaves in cup)
- Infuser Basket
- Tea Bag
- French Press
- Other

**User Customization**:
- Users can rename vessels (e.g., "盖碗" for Gaiwan)
- Users can add custom vessels
- Users can reorder vessels for their preference
- System defaults cannot be deleted, only hidden

#### 5. WaterType
Enum representing water types.

```kotlin
enum class WaterType {
    FILTERED,
    TAP,
    SPRING,
    DISTILLED,
    MINERAL,
    OTHER;

    val displayName: String
}
```

#### 6. TeaStatistics
Model for analytics and statistics.

```kotlin
data class TeaStatistics(
    val totalSessions: Int,
    val totalBrewingTime: Duration,
    val averageBrewingTime: Duration,
    val favoriteTeaTypeId: String?,
    val favoriteTeaId: String?,
    val mostUsedVesselId: String?,
    val averageTemperatureCelsius: Int,
    val sessionsThisWeek: Int,
    val sessionsThisMonth: Int,
    val sessionsThisYear: Int,
    val brewingTrends: List<TrendDataPoint>,
    val temperatureDistribution: Map<IntRange, Int>,
    val teaTypeDistribution: Map<String, Int>  // Map of teaTypeId to count
)

data class TrendDataPoint(
    val date: LocalDate,
    val count: Int
)
```

#### 7. User
Model representing the user (for optional Firebase sync).

```kotlin
data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val syncEnabled: Boolean,
    val lastSyncAt: Instant?,
    val createdAt: Instant
)
```

#### 8. UserPreferences
User preferences and app settings.

```kotlin
data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITERS,
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: LocalTime? = null,
    val autoCleanupEnabled: Boolean = true,
    val cleanupAfterDays: Int = 30,
    val lastCleanupAt: Instant? = null,
    val wifiOnlySyncEnabled: Boolean = true
)

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    fun toCelsius(value: Int): Int = when (this) {
        CELSIUS -> value
        FAHRENHEIT -> ((value - 32) * 5 / 9)
    }

    fun fromCelsius(celsius: Int): Int = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> ((celsius * 9 / 5) + 32)
    }
}

enum class VolumeUnit {
    MILLILITERS,
    FLUID_OUNCES;

    fun toMilliliters(value: Int): Int = when (this) {
        MILLILITERS -> value
        FLUID_OUNCES -> (value * 29.5735).toInt()
    }

    fun fromMilliliters(ml: Int): Int = when (this) {
        MILLILITERS -> ml
        FLUID_OUNCES -> (ml / 29.5735).toInt()
    }
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}
```

#### 9. SessionStatus
Enum representing tea session completion state.

```kotlin
enum class SessionStatus {
    DRAFT,      // Started but not finished (incomplete session)
    COMPLETED   // Finished and saved (with or without rating)
}
```

**Draft Session Behavior**:
- Sessions in DRAFT status are incomplete (user started but didn't finish)
- Drafts are kept forever (never auto-deleted)
- Displayed on Home screen: "X draft sessions to complete"
- Displayed in History screen: with "Draft" badge
- User can finish (add rating/notes) or discard drafts at any time
- Common scenarios for drafts:
  - Started timer but app crashed
  - Started multi-steep session but didn't finish all steeps
  - Quick logged but forgot to add details

#### 10. SyncStatus
Enum representing synchronization state.

```kotlin
enum class SyncStatus {
    SYNCED,           // Successfully synced
    PENDING,          // Waiting to sync
    SYNCING,          // Currently syncing
    CONFLICT,         // Sync conflict detected
    ERROR,            // Sync error
    LOCAL_ONLY        // Not synced (user offline or sync disabled)
}
```

---

### Soft Delete Cleanup Strategy

All major entities (Tea, TeaSession, TeaType, BrewingVessel) use **soft deletes** for sync safety. Items are marked as deleted but remain in the database until permanently cleaned up.

**Cleanup Rules**:
1. **Retention Period**: Keep deleted items for 30 days (configurable in UserPreferences)
2. **Sync Safety**: Only permanently delete items with `syncStatus == SYNCED`
3. **Automatic Cleanup**: Run cleanup after successful sync operations
4. **Manual Cleanup**: User can trigger immediate cleanup in Settings

**Cleanup Implementation**:
```kotlin
suspend fun cleanupSoftDeletedItems(
    olderThanDays: Int = 30,
    syncedOnly: Boolean = true
) {
    val cutoffDate = Clock.System.now().minus(olderThanDays.days)

    // Permanently delete soft-deleted items that are:
    // 1. Marked as deleted (deletedAt != null)
    // 2. Older than cutoff date
    // 3. Successfully synced OR sync disabled (if syncedOnly = true)

    if (syncedOnly) {
        // Safe cleanup: only synced items
        database.teaSessionDao().permanentlyDeleteSyncedOldItems(cutoffDate)
        database.teaDao().permanentlyDeleteSyncedOldItems(cutoffDate)
        database.teaTypeDao().permanentlyDeleteSyncedOldItems(cutoffDate)
        database.brewingVesselDao().permanentlyDeleteSyncedOldItems(cutoffDate)
    } else {
        // Aggressive cleanup: all old deleted items
        database.teaSessionDao().permanentlyDeleteOldItems(cutoffDate)
        database.teaDao().permanentlyDeleteOldItems(cutoffDate)
        database.teaTypeDao().permanentlyDeleteOldItems(cutoffDate)
        database.brewingVesselDao().permanentlyDeleteOldItems(cutoffDate)
    }
}
```

**Cleanup Triggers**:
- After successful background sync (automatic)
- When user opens Settings > Data > "Clean up deleted items" (manual)
- On app startup if last cleanup was >7 days ago (optional)

---

### Database Schema (Room Entities)

#### TeaSessionEntity
```kotlin
@Entity(
    tableName = "tea_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tea_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BrewingVesselEntity::class,
            parentColumns = ["id"],
            childColumns = ["vessel_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TeaSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_session_id"],
            onDelete = ForeignKey.CASCADE  // Delete child steeps if parent deleted
        )
    ],
    indices = [
        Index("tea_id"),
        Index("vessel_id"),
        Index("parent_session_id"),
        Index("steep_number"),
        Index("status"),
        Index("timestamp"),
        Index("sync_status")
    ]
)
data class TeaSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tea_id") val teaId: String,

    // Multi-steep support
    @ColumnInfo(name = "parent_session_id") val parentSessionId: String?,
    @ColumnInfo(name = "steep_number") val steepNumber: Int,
    val status: String,                   // "DRAFT" or "COMPLETED"

    // Session-level fields (only on parent)
    @ColumnInfo(name = "tea_quantity_grams") val teaQuantityGrams: Int?,
    @ColumnInfo(name = "vessel_id") val vesselId: String,
    @ColumnInfo(name = "water_type") val waterType: String,
    val location: String?,
    val rating: Float?,

    // Per-steep fields
    val timestamp: Long,
    @ColumnInfo(name = "brewing_time_seconds") val brewingTimeSeconds: Long,
    @ColumnInfo(name = "temperature_celsius") val temperatureCelsius: Int,
    @ColumnInfo(name = "water_quantity_ml") val waterQuantityMl: Int,
    val notes: String?,
    val photos: String,                   // JSON array

    // Metadata
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?
)
```

#### TeaEntity
```kotlin
@Entity(
    tableName = "teas",
    foreignKeys = [
        ForeignKey(
            entity = TeaTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["tea_type_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("name"),
        Index("tea_type_id"),
        Index("is_favorite"),
        Index("sync_status")
    ]
)
data class TeaEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "tea_type_id") val teaTypeId: String,
    val origin: String?,
    val producer: String?,
    @ColumnInfo(name = "purchase_date") val purchaseDate: String?,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double?,
    @ColumnInfo(name = "stock_amount") val stockAmount: Int?,
    @ColumnInfo(name = "default_brewing_time_seconds") val defaultBrewingTimeSeconds: Long?,
    @ColumnInfo(name = "default_temperature_celsius") val defaultTemperatureCelsius: Int?,
    @ColumnInfo(name = "default_quantity") val defaultQuantity: Int?,
    val description: String?,
    val photos: String,                   // JSON array
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?
)
```

#### TeaTypeEntity
```kotlin
@Entity(
    tableName = "tea_types",
    indices = [
        Index("display_order"),
        Index("is_system_default"),
        Index("sync_status")
    ]
)
data class TeaTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "default_temperature_celsius") val defaultTemperatureCelsius: Int?,
    @ColumnInfo(name = "default_brewing_time_seconds") val defaultBrewingTimeSeconds: Long?,
    val color: String,                    // Color hex string
    @ColumnInfo(name = "is_system_default") val isSystemDefault: Boolean,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?
)
```

#### BrewingVesselEntity
```kotlin
@Entity(
    tableName = "brewing_vessels",
    indices = [
        Index("display_order"),
        Index("is_system_default"),
        Index("sync_status")
    ]
)
data class BrewingVesselEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "icon_name") val iconName: String?,
    @ColumnInfo(name = "is_system_default") val isSystemDefault: Boolean,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?
)
```

#### SyncQueueEntity
```kotlin
@Entity(
    tableName = "sync_queue",
    indices = [Index("status"), Index("created_at")]
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entity_type") val entityType: String,  // "tea_session", "tea", "tea_type", "brewing_vessel"
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,                // "CREATE", "UPDATE", "DELETE"
    val status: String,                   // "PENDING", "SYNCING", "SYNCED", "ERROR"
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "retry_count") val retryCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
```

---

## Navigation Architecture

### Global Navigation Pattern

The app uses **Bottom Navigation + FAB** pattern for optimal mobile UX.

#### Bottom Navigation Bar (4 Items)
```
┌──────────┬──────────┬──────────┬──────────┐
│   Home   │Collection│  History │   More   │
│    🏠    │    📚    │    📜    │    ⋮     │
└──────────┴──────────┴──────────┴──────────┘
```

**Navigation Items**:
1. **Home**: Main dashboard with stats and recent sessions
2. **Collection**: Tea collection management (teas, types, vessels)
3. **History**: Past brewing sessions with search/filter
4. **More**: Secondary features
   - Analytics
   - Timer (when not actively running)
   - Settings
   - Backup & Restore

#### Floating Action Button (FAB)
- **Purpose**: Quick access to primary action (Log Tea)
- **Position**: Bottom-right corner, above navigation bar
- **Action**: Opens Log Tea Screen
- **Icon**: "+" (plus icon)

#### Persistent Timer Banner
When a brewing timer is active and user navigates away from the timer screen:
- **Position**: Top of screen, below app bar
- **Content**: Tea name, remaining time, pause/play control
- **Action**: Tap to return to full Timer Screen
- **Dismissal**: Automatically disappears when timer completes

#### Top App Bar
- **Left**: App title/logo
- **Right**: Sync status icon (if enabled), Settings icon
- **Elevation**: Scrolls with content or elevated based on screen

---

## Feature Specifications

### 1. Home Screen
**Purpose**: Main dashboard providing quick access to key features.

**Components**:
- **Top App Bar**:
  - App title/logo
  - Sync status indicator (if Firebase enabled)
  - Settings icon shortcut
- **Persistent Timer Banner** (when timer is active):
  - Mini timer display at top of screen
  - Tea name, remaining time
  - Tap to navigate to full timer screen
- **Welcome Message**:
  - Time-based greeting (Good morning/afternoon/evening/night)
  - User's display name (if available)
- **Daily Statistics** (3 cards):
  - Sessions today
  - Tea quantity brewed today (total ml/oz of water used)
  - Different teas brewed today
- **Recent Sessions** (3-5 sessions):
  - Tea photo thumbnail
  - Tea name and type
  - Temperature and brewing time
  - Timestamp
  - "View All" link to History screen
- **Empty State** (when no sessions):
  - Welcome tutorial
  - "Get Started" button
  - Quick tips for first-time users
- **Floating Action Button (FAB)**:
  - Prominent "+" button
  - Quick access to Log Tea Screen
- **Bottom Navigation Bar** (4 items):
  - Home (current)
  - Collection
  - History
  - More (Analytics, Timer, Settings, Backup)

**Actions**:
- Tap FAB (+) → Navigate to Log Tea Screen
- Tap persistent timer banner → Navigate to Brewing Timer Screen
- Tap session card → Navigate to Session Detail
- Tap "View All" → Navigate to History Screen
- Tap bottom nav items → Navigate to respective screens
- Pull to refresh → Trigger Firebase sync (if enabled)

**MVI State**:
```kotlin
data class HomeState(
    val greeting: String = "",
    val user: User? = null,
    val activeTimer: TimerInfo? = null,
    val dailyStats: DailyStats? = null,
    val recentSessions: List<TeaSession> = emptyList(),
    val draftSessionsCount: Int = 0,      // Number of incomplete sessions
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false
)

data class DailyStats(
    val sessionsToday: Int,               // Completed sessions only
    val totalWaterQuantityMl: Int,        // Total water used across all steeps
    val differentTeasCount: Int           // Unique teas brewed today
)

data class TimerInfo(
    val teaName: String,
    val remainingTime: Duration,
    val isRunning: Boolean
)
```

**Draft Sessions Display**:
When `draftSessionsCount > 0`, show banner on Home screen:
```
┌─────────────────────────────────┐
│ ⚠️ You have 2 incomplete        │
│    sessions. Tap to review.     │
└─────────────────────────────────┘
```

### 2. Log Tea Screen (Brew Setup)
**Purpose**: Set up and record a tea brewing session. Primary flow is timer-based (set parameters → start timer → save after brewing).

**Two Usage Modes**:
1. **Timer-First (Primary)**: Set parameters → Start timer → Save after brewing
2. **Retrospective Log**: Log session after brewing (without timer)

---

#### Input Fields

**Required**:
- **Tea Selection** (searchable dropdown with quick add):
  - Search tea collection by name
  - Select from list
  - "+" button to add new tea inline (without leaving screen)
  - Shows tea photo, name, type, last brewed date

**Optional Brewing Parameters**:
- **Tea Quantity**:
  - Number input with steppers (+/-)
  - Unit: grams
  - Placeholder: "e.g., 5g"
  - Note: "Leave empty for tea bags/sachets"

- **Water Quantity**:
  - Number input with steppers (+/-)
  - Unit: ml or oz (based on user preference)
  - Pre-filled with tea's default or last used value

- **Temperature**:
  - Number input with steppers (+/-)
  - Unit: °C or °F (based on user preference)
  - Smart preset buttons based on selected tea type:
    - Green: 70°C, 75°C, 80°C
    - Black: 90°C, 95°C, 100°C
    - White: 70°C, 75°C, 80°C
    - Oolong: 85°C, 90°C, 95°C
    - Etc.
  - Pre-filled with tea's default temperature

- **Brewing Time**:
  - Duration picker (minutes:seconds)
  - Pre-filled with tea's default time
  - Alternative: "Use Timer" button (primary action)

- **Brewing Vessel**:
  - Dropdown with icons
  - Shows vessel name and icon
  - Pre-filled with last used vessel for this tea

- **Water Type**:
  - Dropdown
  - Options: Filtered, Tap, Spring, Distilled, Mineral, Other
  - Pre-filled with last used water type

**Optional Session Details**:
- **Photos** (before brewing):
  - Image picker
  - Multiple photos supported
  - Shows preview thumbnails

- **Location**:
  - Auto-detect button
  - Manual text entry
  - Optional

---

#### Primary Flow: Timer-First Brewing

**Step 1: Setup Screen**
```
┌─────────────────────────────────┐
│ ← Brew Tea                      │
├─────────────────────────────────┤
│ Tea *                           │
│ [Longjing Green Tea        ▼]   │
│                                 │
│ Tea Quantity (optional)         │
│ [5] g                    [+][-] │
│                                 │
│ Water Quantity                  │
│ [150] ml                 [+][-] │
│                                 │
│ Temperature                     │
│ [80] °C                  [+][-] │
│ [70°] [75°] [80°] [85°]  ← presets│
│                                 │
│ Time                            │
│ [2:00] ⏱️                       │
│                                 │
│ Vessel                          │
│ [🫖 Gaiwan               ▼]    │
│                                 │
│ Water Type                      │
│ [Filtered                ▼]    │
│                                 │
│ 📷 Add Photos (optional)       │
│                                 │
│ ──────────────────────────────  │
│                                 │
│ [Start Timer & Brew]     ← Primary│
│ [Save Without Timer]            │
└─────────────────────────────────┘
```

**Step 2: Timer Running** (navigates to Timer Screen)
- See "Brewing Timer Screen" specification below

**Step 3: Timer Complete**
```
┌─────────────────────────────────┐
│ Steep 1 Complete! ☕            │
├─────────────────────────────────┤
│ Longjing Green                  │
│ 80°C • 150ml • 2:00             │
│                                 │
│ Add notes for this steep?       │
│ ┌─────────────────────────────┐ │
│ │ Light, sweet, slight vegetal│ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ 📷 Add photos of liquor/leaves?│
│ [+] [thumb] [thumb]             │
│                                 │
│ [Continue to Steep 2]           │
│ [Save & Finish Session]         │
└─────────────────────────────────┘
```

**Step 4a: Save & Finish**
```
┌─────────────────────────────────┐
│ Rate This Session               │
├─────────────────────────────────┤
│ ⭐⭐⭐⭐⭐                         │
│                                 │
│ Overall notes (optional)        │
│ ┌─────────────────────────────┐ │
│ │ Excellent session, perfect  │ │
│ │ parameters.                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ [Save Session]                  │
│ [Skip Rating]                   │
└─────────────────────────────────┘
```
- Session saved as COMPLETED
- Deduct tea quantity from stock (if tracked)
- Navigate to Session Detail or Home

**Step 4b: Continue to Steep 2** (Multi-Steep Flow)
```
┌─────────────────────────────────┐
│ ← Steep 2 Setup                 │
├─────────────────────────────────┤
│ Longjing Green (5g)      ← inherited│
│ 🫖 Gaiwan • Filtered     ← inherited│
│                                 │
│ Water Quantity                  │
│ [150] ml                 [+][-] │
│                                 │
│ Temperature                     │
│ [82] °C                  [+][-] │
│ [80°] [82°] [85°] [90°]         │
│                                 │
│ Time                            │
│ [2:30] ⏱️                       │
│                                 │
│ [+5°C] [-15s] [+30s]    ← Quick adjustments│
│                                 │
│ [Start Steep 2 Timer]           │
└─────────────────────────────────┘
```
- Inherits: tea, tea quantity, vessel, water type, location
- Editable: water quantity, temperature, time
- Suggestions buttons for common adjustments
- Repeat steps 2-4 for each steep

---

#### Alternative Flow: Retrospective Logging

**Used for**: Logging sessions after brewing (without timer), or entering pen-and-paper notes.

**Single-Steep Entry**:
```
┌─────────────────────────────────┐
│ ← Log Session                   │
├─────────────────────────────────┤
│ Tea *                           │
│ [Longjing Green          ▼]    │
│                                 │
│ [All brewing parameters...]     │
│                                 │
│ Notes                           │
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ 📷 Photos                       │
│ [+] [thumb] [thumb]             │
│                                 │
│ Rating                          │
│ ⭐⭐⭐⭐☆                         │
│                                 │
│ [Save Session]                  │
│ [Log Another Steep]      ← Multi-steep│
└─────────────────────────────────┘
```
- "Log Another Steep" creates child session
- Prompts for steep 2, 3, 4... parameters
- All steeps can be logged retrospectively

---

#### Pre-Fill Logic

**When user selects a tea**:
1. Load last session for THIS specific tea
2. Pre-fill editable fields with those values:
   - Water quantity
   - Temperature
   - Brewing time
   - Vessel
   - Water type
3. If no previous session, use tea's default values:
   - Tea quantity: empty
   - Water quantity: tea.defaultQuantity or empty
   - Temperature: tea.defaultTemperatureCelsius or type default
   - Time: tea.defaultBrewingTime or type default
   - Vessel: last used globally or first in list
   - Water type: last used globally or "Filtered"

---

#### Inline Quick Add Tea

**When user taps "+" in tea selection**:
```
┌─────────────────────────────────┐
│ Quick Add Tea                   │
├─────────────────────────────────┤
│ Name *                          │
│ [                           ]   │
│                                 │
│ Type *                          │
│ [Green                   ▼]    │
│                                 │
│ [Save & Continue]               │
│ [Cancel]                        │
└─────────────────────────────────┘
```
- Minimal required fields: name, type
- Saves to library immediately
- Returns to Log Tea screen with tea selected
- User can add more details later in Library

---

#### Draft Session Handling

**Scenarios that create drafts**:
1. User starts timer, app crashes
2. User starts timer, closes app, never completes
3. User starts multi-steep, completes steep 1, never continues

**Draft Recovery**:
- Drafts shown on Home screen: "2 incomplete sessions"
- Drafts shown in History with "Draft" badge
- Tapping draft reopens completion flow:
  - For timer-based: Show completion options
  - For multi-steep: Option to continue or finish

---

#### Validation Rules

**Required**:
- Tea selection (must pick or create tea)

**Optional but Validated**:
- If brewing time provided: must be > 0
- If temperature provided: must be 0-100°C (32-212°F)
- If water quantity provided: must be > 0
- If tea quantity provided: must be > 0

**No Validation** (all optional):
- Vessel
- Water type
- Photos
- Location
- Notes
- Rating

---

#### MVI State

```kotlin
data class LogTeaState(
    val selectedTea: Tea? = null,
    val teaQuantityGrams: Int? = null,
    val waterQuantityMl: Int = 0,
    val temperatureCelsius: Int = 0,
    val brewingTime: Duration = Duration.ZERO,
    val vesselId: String? = null,
    val waterType: WaterType = WaterType.FILTERED,
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val location: String? = null,
    val rating: Float? = null,

    // Multi-steep support
    val isMultiSteep: Boolean = false,
    val currentSteepNumber: Int = 1,
    val parentSessionId: String? = null,

    // UI state
    val temperaturePresets: List<Int> = emptyList(),  // Based on tea type
    val isLoading: Boolean = false,
    val error: String? = null,
    val showQuickAddTea: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

sealed interface LogTeaIntent {
    data class SelectTea(val tea: Tea) : LogTeaIntent
    data object ShowQuickAddTea : LogTeaIntent
    data class QuickAddTeaComplete(val tea: Tea) : LogTeaIntent

    data class UpdateTeaQuantity(val grams: Int?) : LogTeaIntent
    data class UpdateWaterQuantity(val ml: Int) : LogTeaIntent
    data class UpdateTemperature(val celsius: Int) : LogTeaIntent
    data class UpdateBrewingTime(val duration: Duration) : LogTeaIntent
    data class SelectVessel(val vesselId: String) : LogTeaIntent
    data class SelectWaterType(val type: WaterType) : LogTeaIntent
    data class UpdateNotes(val notes: String) : LogTeaIntent
    data class AddPhoto(val photoUri: String) : LogTeaIntent
    data class RemovePhoto(val photoUri: String) : LogTeaIntent
    data class UpdateLocation(val location: String?) : LogTeaIntent
    data class UpdateRating(val rating: Float?) : LogTeaIntent

    data object StartTimerAndBrew : LogTeaIntent
    data object SaveWithoutTimer : LogTeaIntent
    data object ContinueToNextSteep : LogTeaIntent
    data object SaveAndFinishSession : LogTeaIntent
    data object Cancel : LogTeaIntent
}
```

---

#### Actions

**Primary Actions**:
- **Start Timer & Brew** → Navigate to Timer Screen with parameters, create DRAFT session
- **Continue to Steep 2** → Set up next steep with inherited values
- **Save & Finish Session** → Mark session COMPLETED, deduct stock, navigate away
- **Save Without Timer** → Save session directly as COMPLETED (retrospective)

**Secondary Actions**:
- **Quick Add Tea** → Show inline tea creation dialog
- **Add Photos** → Open image picker (multiple selection)
- **Auto-detect Location** → Use device location services
- **Cancel** → Discard draft or unsaved changes, confirm if data entered

**Parameter Adjustment Actions**:
- **Temperature Preset Buttons** → Set temperature to preset value
- **Quick Adjustment Buttons** (steep 2+) → Apply common adjustments (+5°C, +30s, etc.)
- **Stepper Buttons** (+/-) → Increment/decrement values

---

#### Edge Cases

1. **No teas in library**: Show empty state with "Add Your First Tea" button
2. **Stock depleted**: Warn user if tea stock < session tea quantity
3. **Default values unavailable**: Use sensible app defaults (80°C, 150ml, 2:00)
4. **Rapid steep creation**: Support creating multiple steeps quickly (gong-fu style)
5. **Photo storage full**: Show error, allow continuing without photos
6. **Duplicate quick add**: Check if tea name exists before creating

### 3. Brewing Timer Screen
**Purpose**: Time tea brewing with live countdown, notifications, and seamless multi-steep support.

---

#### Screen States

**1. Pre-Start State** (brief, auto-starts)
```
┌─────────────────────────────────┐
│ ← Timer                         │
├─────────────────────────────────┤
│                                 │
│       Longjing Green            │
│       Steep 1                   │
│                                 │
│         ⏱️                      │
│        2:00                     │
│                                 │
│  80°C • 150ml • Gaiwan          │
│                                 │
│    [Start Brewing]              │
│                                 │
└─────────────────────────────────┘
```
- Shows brewing parameters
- Auto-starts after 1 second OR user taps "Start Brewing"
- Creates DRAFT session in database

**2. Running State** (active countdown)
```
┌─────────────────────────────────┐
│ ← Brewing                  [⋮]  │ ← Menu
├─────────────────────────────────┤
│                                 │
│       Longjing Green            │
│       Steep 1 of Gong-fu        │
│                                 │
│    ╭─────────────────╮          │
│   │   ●   1:47      │          │ ← Circular progress
│   │  ●     🍵     ● │          │    ring animation
│   │   ●         ●   │          │
│    ╰─────────────────╯          │
│                                 │
│  [-30s] [Pause] [+30s]          │
│  [-1m]          [+1m]           │
│                                 │
│  ─────────────────────────      │
│  80°C • 150ml • Gaiwan          │
│  Filtered Water                 │
│                                 │
└─────────────────────────────────┘
```
- Large countdown display (MM:SS)
- Circular progress ring (fills as time passes)
- Tea name and steep number
- Brewing parameters below
- Quick adjustment buttons (adds/subtracts time)
- Pause button
- Menu (⋮) for: Cancel, Edit parameters, View notes

**3. Paused State**
```
┌─────────────────────────────────┐
│ ← Paused                   [⋮]  │
├─────────────────────────────────┤
│                                 │
│       Longjing Green            │
│       Steep 1                   │
│                                 │
│    ╭─────────────────╮          │
│   │                  │          │
│   │      ⏸️ 1:23     │          │ ← Paused indicator
│   │                  │          │
│    ╰─────────────────╯          │
│                                 │
│  [Resume]  [Reset]  [Stop]      │
│                                 │
│  ─────────────────────────      │
│  80°C • 150ml • Gaiwan          │
│                                 │
└─────────────────────────────────┘
```
- Timer paused, time frozen
- Resume, Reset, and Stop buttons
- Notification updated to "Paused"

**4. Complete State** (timer finished)
```
┌─────────────────────────────────┐
│ Steep 1 Complete! ☕            │
├─────────────────────────────────┤
│       Longjing Green            │
│                                 │
│    ╭─────────────────╮          │
│   │                  │          │
│   │       ✓          │          │ ← Complete checkmark
│   │                  │          │
│    ╰─────────────────╯          │
│                                 │
│  80°C • 150ml • 2:00            │
│                                 │
│  Add notes for this steep?      │
│  ┌─────────────────────────────┐│
│  │                             ││
│  └─────────────────────────────┘│
│                                 │
│  📷 Add photos? [+]             │
│                                 │
│  [Continue to Steep 2]          │
│  [Save & Finish]                │
│  [Restart Timer]                │
└─────────────────────────────────┘
```
- Completion animation/sound
- Push notification sent
- Option to add notes and photos for this steep
- Three completion paths:
  - Continue to next steep (multi-steep)
  - Save and finish (complete session)
  - Restart timer (same parameters)

---

#### Background Operation

**When user navigates away from Timer Screen**:

**Persistent Mini Banner** (top of screen, all screens):
```
┌─────────────────────────────────┐
│ ┌─────────────────────────────┐ │
│ │ 🍵 Longjing • 1:23 [❚❚] [X]│ │ ← Brewing banner
│ └─────────────────────────────┘ │
│                                 │
│ [Home screen content...]        │
│                                 │
└─────────────────────────────────┘
```
- Shows tea name, remaining time
- Pause button [❚❚]
- Stop button [X]
- Tap banner → Navigate back to Timer Screen
- Banner disappears when timer completes or is stopped

**System Notification** (Android/iOS):
```
┌─────────────────────────────────┐
│ 🍵 Leaf Log                     │
│ Longjing Green - 1:23 remaining │
│ [Pause] [Stop]                  │
└─────────────────────────────────┘
```
- Ongoing notification while timer runs
- Shows remaining time (updates every second)
- Quick actions: Pause, Stop
- Tap notification → Open app to Timer Screen

**When timer completes** (app in background):
```
┌─────────────────────────────────┐
│ ✅ Leaf Log                     │
│ Longjing Green is ready!        │
│ Time to enjoy your tea ☕       │
│ [Open]                          │
└─────────────────────────────────┘
```
- High-priority notification
- Sound and vibration (user configurable)
- Tap → Open app to completion screen

---

#### Quick Adjustment Buttons

**Purpose**: Adjust timer while running without pausing.

**Buttons**:
- **-1m**: Subtract 60 seconds
- **-30s**: Subtract 30 seconds
- **+30s**: Add 30 seconds
- **+1m**: Add 60 seconds

**Behavior**:
- Immediate adjustment (no confirmation)
- Visual feedback (button press animation)
- Updated time shown in notification
- Cannot go below 0 (minimum is 0:00)
- Can extend indefinitely (useful if under-brewed)

---

#### Timer Controls

**Start/Resume**:
- Begins countdown
- Shows progress ring animation
- Creates/updates DRAFT session
- Starts system notification

**Pause**:
- Freezes countdown
- Updates notification to "Paused"
- Timer remains in memory
- User can resume or stop

**Reset**:
- Resets to original duration
- Available only when paused
- Prompts: "Reset to 2:00?" with confirm/cancel

**Stop**:
- Stops and discards timer
- Prompts: "Stop brewing? Session will be saved as draft"
- Options: Keep Draft, Discard Draft, Cancel

**Complete** (automatic when time reaches 0:00):
- Triggers completion animation
- Sends notification
- Plays completion sound
- Shows completion options screen

---

#### Completion Flow

**Option 1: Save & Finish** (single-steep or final steep)
```
1. Shows completion screen with notes/photos
2. User adds notes/photos (optional)
3. Navigates to rating screen:
   ┌─────────────────────────────────┐
   │ Rate This Session               │
   ├─────────────────────────────────┤
   │ ⭐⭐⭐⭐⭐                         │
   │                                 │
   │ Overall notes (optional)        │
   │ ┌─────────────────────────────┐ │
   │ │                             │ │
   │ └─────────────────────────────┘ │
   │                                 │
   │ [Save Session]                  │
   │ [Skip Rating]                   │
   └─────────────────────────────────┘
4. Session marked as COMPLETED
5. Tea stock deducted (if tracked)
6. Navigate to Session Detail or Home
```

**Option 2: Continue to Steep 2** (multi-steep)
```
1. Shows completion screen with notes/photos for steep 1
2. User adds notes/photos (optional)
3. Navigates to steep 2 setup:
   ┌─────────────────────────────────┐
   │ Steep 2 Setup                   │
   ├─────────────────────────────────┤
   │ Longjing Green (5g)     ← inherited│
   │ Gaiwan • Filtered       ← inherited│
   │                                 │
   │ Water Quantity                  │
   │ [150] ml                 [+][-] │
   │                                 │
   │ Temperature                     │
   │ [82] °C                  [+][-] │
   │                                 │
   │ Time                            │
   │ [2:30] ⏱️                       │
   │                                 │
   │ [Start Steep 2 Timer]           │
   └─────────────────────────────────┘
4. User adjusts parameters
5. Starts steep 2 timer
6. Repeats cycle
```

**Option 3: Restart Timer**
```
1. Resets timer to original duration
2. Starts countdown immediately
3. Same steep number (for re-brewing same steep)
4. Useful if: accidentally stopped, want to re-steep same parameters
```

---

#### Multi-Steep Indicators

**Visual cues for gong-fu brewing**:
- **Steep counter**: "Steep 2 of 4" or "Steep 3"
- **Previous steeps summary**: Swipe up to see steep 1, 2 details
- **Steep history bar**: Small timeline showing past steeps
  ```
  [1:✓] [2:✓] [3:⏱️] [+]
  ```
- Each completed steep shows checkmark
- Current steep shows timer icon
- "+" to add another steep

---

#### Parameter Display

**Always visible below timer**:
- Temperature (with unit preference)
- Water quantity (with unit preference)
- Vessel name (with icon)
- Water type (small text)
- Tea quantity (if tracked): "5g Longjing Green"

**Editable during timer**:
- Menu (⋮) → "Edit Parameters"
- Can change notes, add photos
- Cannot change time/temp while running (must pause first)

---

#### Error Handling & Edge Cases

1. **App crash during timer**:
   - Timer state persisted
   - On reopen: "You have a timer running. Resume?"
   - Can resume or cancel

2. **Phone reboot**:
   - Timer lost (OS limitation)
   - Session saved as DRAFT
   - User prompted on next launch: "Incomplete brewing session found"

3. **Low battery**:
   - Warning: "Low battery. Timer may not complete in background"
   - Suggest keeping app open or charging

4. **Notification permission denied**:
   - Warning on timer start
   - Still functional, but no alerts

5. **Very long timers** (>1 hour):
   - Confirmation: "Are you sure? 2h 30m is a long time"
   - Different notification strategy (hourly updates vs second-by-second)

6. **Zero-second timers**:
   - Not allowed
   - Minimum 1 second

7. **Timer exhaustion** (user keeps adding time):
   - Allow indefinitely
   - No maximum limit

---

#### MVI State

```kotlin
data class TimerState(
    val sessionId: String,                // Draft session ID
    val tea: Tea,
    val steepNumber: Int,
    val parentSessionId: String?,         // For multi-steep

    // Timer state
    val timerState: TimerRunState,
    val originalDuration: Duration,       // Starting time
    val remainingTime: Duration,          // Current countdown
    val elapsedTime: Duration,            // Time passed

    // Brewing parameters
    val temperatureCelsius: Int,
    val waterQuantityMl: Int,
    val vesselId: String,
    val waterType: WaterType,
    val teaQuantityGrams: Int?,

    // Steep completion data
    val steepNotes: String = "",
    val steepPhotos: List<String> = emptyList(),

    // UI state
    val showCompletionOptions: Boolean = false,
    val showSteepSetup: Boolean = false,
    val showRatingDialog: Boolean = false,
    val error: String? = null
)

enum class TimerRunState {
    PRE_START,      // Before timer begins
    RUNNING,        // Counting down
    PAUSED,         // User paused
    COMPLETED       // Timer reached 0:00
}

sealed interface TimerIntent {
    data object Start : TimerIntent
    data object Pause : TimerIntent
    data object Resume : TimerIntent
    data object Reset : TimerIntent
    data object Stop : TimerIntent

    data class AdjustTime(val seconds: Int) : TimerIntent  // +30, -30, +60, -60

    // Completion options
    data class AddSteepNotes(val notes: String) : TimerIntent
    data class AddSteepPhoto(val photoUri: String) : TimerIntent
    data object ContinueToNextSteep : TimerIntent
    data object SaveAndFinish : TimerIntent
    data object RestartTimer : TimerIntent

    // Rating
    data class SetRating(val rating: Float) : TimerIntent
    data class SetOverallNotes(val notes: String) : TimerIntent
    data object SaveSession : TimerIntent
    data object SkipRating : TimerIntent
}
```

---

#### Notifications

**Ongoing Notification** (while running):
- **Title**: "Leaf Log - Brewing"
- **Content**: "Longjing Green - 1:23 remaining"
- **Actions**: [Pause] [Stop]
- **Update frequency**: Every second
- **Priority**: Low (non-intrusive)

**Paused Notification** (when paused):
- **Title**: "Leaf Log - Paused"
- **Content**: "Longjing Green - 1:23 remaining"
- **Actions**: [Resume] [Stop]
- **Priority**: Low

**Completion Notification** (when done):
- **Title**: "Tea is Ready! ☕"
- **Content**: "Longjing Green is perfectly brewed"
- **Actions**: [Open]
- **Priority**: High
- **Sound**: Completion sound (user configurable)
- **Vibration**: Pattern (user configurable)

**Platform Differences**:
- **Android**: Uses NotificationManager with foreground service
- **iOS**: Uses UserNotifications framework, local notifications

---

#### Accessibility

- **Screen Reader**: Announces time remaining at intervals (every 30s, then every 10s in last minute)
- **Haptic Feedback**: Vibration when timer completes (even if silent)
- **Large Touch Targets**: All buttons minimum 48dp
- **Voice Control**: "Pause timer", "Add 30 seconds" (if supported)
- **High Contrast**: Timer display readable in all themes

---

#### Performance Considerations

1. **Timer precision**: Update UI every 100ms for smooth animation, but actual countdown based on elapsed time (not ticks)
2. **Battery optimization**: Reduce update frequency when app backgrounded
3. **Wake locks**: Keep screen on while timer is visible (user can disable)
4. **Memory**: Release resources when timer completes

---

#### Actions Summary

**Primary**:
- Start timer → Begin countdown, create draft, show notification
- Pause → Freeze countdown
- Resume → Continue countdown
- Complete → Show completion options

**Secondary**:
- Quick adjust (+30s, -30s, +1m, -1m) → Modify running timer
- Reset → Return to original duration (when paused)
- Stop → Cancel timer, save as draft
- Edit parameters → Modify brewing details

**Completion**:
- Add notes/photos → Capture steep details
- Continue to steep 2 → Multi-steep flow
- Save & finish → Complete session with rating
- Restart → Repeat same steep

### 4. Collection Screen
**Purpose**: Manage personal tea collection, tea types, and brewing vessels.

**Navigation Structure**: Three tabs for comprehensive collection management.

```
┌─────────────────────────────────┐
│ Collection            [🔍] [⋮]  │
├─────────────────────────────────┤
│ [Teas] [Types] [Vessels]        │ ← Tab bar
├─────────────────────────────────┤
│                                 │
│  [Tab content...]               │
│                                 │
│              [+ FAB]            │
└─────────────────────────────────┘
```

---

## Tab 1: Teas (Default)

**Purpose**: Main tea collection management.

### Components

**Search & Filter Bar**:
```
┌─────────────────────────────────┐
│ [🔍 Search teas...]      [≡]   │ ← Search + filter
└─────────────────────────────────┘
```
- **Search**: Name, origin, producer
- **Filter button** (≡): Opens filter sheet

**Filter Sheet**:
```
┌─────────────────────────────────┐
│ Filter Teas                     │
├─────────────────────────────────┤
│ Tea Type                        │
│ □ Green  □ Black  □ White      │
│ □ Oolong □ Pu-erh □ All        │
│                                 │
│ Status                          │
│ ○ All                           │
│ ○ Favorites only                │
│ ○ In stock                      │
│ ○ Low stock (< 10g)             │
│ ○ Out of stock                  │
│                                 │
│ Sort By                         │
│ ○ Name (A-Z)                    │
│ ○ Recently brewed               │
│ ○ Highest rated                 │
│ ○ Stock level                   │
│ ○ Date added (newest)           │
│                                 │
│ [Reset] [Apply]                 │
└─────────────────────────────────┘
```

**List/Grid View Toggle**:
- Icon button to switch between list and grid
- Preference saved per user

**Tea List View**:
```
┌─────────────────────────────────┐
│ ┌─────────────────────────────┐ │
│ │ [Photo] Longjing Green   ⭐ │ │
│ │         Green Tea            │ │
│ │         Hangzhou, China      │ │
│ │         45g • ⭐⭐⭐⭐⭐        │ │
│ │         Brewed 3 days ago    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ [Photo] Da Hong Pao      ⭐ │ │
│ │         Oolong Tea           │ │
│ │         Wuyi Mountains       │ │
│ │         12g • ⭐⭐⭐⭐☆        │ │
│ │         Brewed today         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**Tea Grid View**:
```
┌─────────────────────────────────┐
│ ┌──────────┐ ┌──────────┐      │
│ │ [Photo]  │ │ [Photo]  │      │
│ │ Longjing │ │ Da Hong  │      │
│ │ ⭐ 45g   │ │ ⭐ 12g   │      │
│ └──────────┘ └──────────┘      │
│ ┌──────────┐ ┌──────────┐      │
│ │ [Photo]  │ │ [Photo]  │      │
│ │ Silver   │ │ English  │      │
│ │ ⭐ 28g   │ │   5g     │      │
│ └──────────┘ └──────────┘      │
└─────────────────────────────────┘
```

**Card Content** (each tea):
- Photo thumbnail (or type color if no photo)
- Tea name
- Tea type name
- Origin
- Stock level (e.g., "45g", "0g" for out of stock)
- Favorite star (filled if favorite)
- Average rating (stars)
- Last brewed date (relative: "2 days ago", "today")

**Empty State** (no teas):
```
┌─────────────────────────────────┐
│                                 │
│         🍵                      │
│                                 │
│   Your Tea Collection is Empty  │
│                                 │
│   Add your first tea to start   │
│   tracking your brewing!        │
│                                 │
│   [Add Your First Tea]          │
│                                 │
└─────────────────────────────────┘
```

### Actions

**Primary**:
- **Tap tea card** → Navigate to Tea Detail Screen
- **FAB (+)** → Navigate to Add Tea Screen

**Swipe Actions** (left swipe):
```
[Tea Card] ← [⭐] [Edit] [🗑️]
```
- **⭐ Favorite**: Toggle favorite status
- **Edit**: Navigate to Edit Tea Screen
- **🗑️ Delete**: Show confirmation dialog

**Long Press**:
- **Multi-select mode** activated
- Checkboxes appear on cards
- Bottom action bar appears:
  ```
  [✓ 3 selected] [Favorite] [Delete] [Cancel]
  ```

**Menu Actions** (⋮ top right):
- Import teas (CSV)
- Export teas (CSV)
- Sort options
- View options (list/grid)

---

## Tab 2: Types

**Purpose**: Manage tea types (categories).

### Components

**Type List**:
```
┌─────────────────────────────────┐
│ Tea Types               [+]     │
├─────────────────────────────────┤
│ Drag to reorder ↕️               │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ [●] Green Tea         [⋮] │ │ ← Drag handle, color, name, menu
│ │    70-80°C • 2-3 min        │ │
│ │    12 teas                  │ │ ← Count
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ [●] Black Tea         [⋮] │ │
│ │    90-100°C • 3-5 min       │ │
│ │    8 teas                   │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ [●] White Tea         [⋮] │ │
│ │    70-75°C • 4-5 min        │ │
│ │    3 teas                   │ │
│ └─────────────────────────────┘ │
│                                 │
│ ─────── Custom Types ───────    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ [●] 绿茶 (Custom)      [⋮] │ │
│ │    75°C • 2 min             │ │
│ │    2 teas                   │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**Type Card Content**:
- **Drag handle** (≡): Reorder types
- **Color indicator** ([●]): Visual color for this type
- **Name**: Type display name
- **Defaults**: Temperature and brewing time defaults
- **Tea count**: Number of teas using this type
- **Menu** (⋮): Edit, Delete (if custom), Hide (if system)
- **System default badge**: Small "System" label for built-in types

**System vs Custom Types**:
- **System types** (11 pre-installed):
  - Can rename
  - Can change defaults
  - Cannot delete (can hide)
  - Badge: "System"
- **Custom types** (user-created):
  - Full control
  - Can delete (if no teas using it)
  - Badge: "Custom"

### Actions

**Primary**:
- **FAB (+)** → Add new custom type
- **Drag handle (≡)** → Reorder types (saved immediately)

**Type Menu (⋮)**:
- **Edit**: Edit name, color, defaults
- **Hide** (system only): Hide from tea selection dropdowns
- **Delete** (custom only): Delete type (warns if teas using it)
- **View teas**: Navigate to Teas tab filtered by this type

**Add Type Dialog**:
```
┌─────────────────────────────────┐
│ Add Tea Type                    │
├─────────────────────────────────┤
│ Name *                          │
│ [                           ]   │
│                                 │
│ Color                           │
│ [●][●][●][●][●][●][●][●]        │ ← Color picker
│                                 │
│ Default Temperature (optional)  │
│ [75] °C                  [+][-] │
│                                 │
│ Default Brewing Time (optional) │
│ [2:30] ⏱️                       │
│                                 │
│ [Save]  [Cancel]                │
└─────────────────────────────────┘
```

**Edit Type Dialog** (same as Add, with Delete button for custom types)

**Delete Confirmation**:
```
┌─────────────────────────────────┐
│ Delete "Matcha"?                │
├─────────────────────────────────┤
│ 2 teas are using this type.     │
│                                 │
│ Those teas will need a new      │
│ type assigned.                  │
│                                 │
│ [Cancel] [Delete Anyway]        │
└─────────────────────────────────┘
```
- If teas use this type, warn user
- Show count of affected teas
- After deletion, affected teas → "Other" type

**Empty State** (no custom types, only system):
```
┌─────────────────────────────────┐
│ System tea types are loaded.    │
│                                 │
│ Tap + to create custom types    │
│ with your own names and         │
│ brewing recommendations.        │
└─────────────────────────────────┘
```

---

## Tab 3: Vessels

**Purpose**: Manage brewing vessels.

### Components

**Vessel List** (same pattern as Types):
```
┌─────────────────────────────────┐
│ Brewing Vessels         [+]     │
├─────────────────────────────────┤
│ Drag to reorder ↕️               │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ 🫖 Gaiwan              [⋮] │ │ ← Drag, icon, name, menu
│ │    Used in 45 sessions      │ │ ← Usage count
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ 🫖 Teapot              [⋮] │ │
│ │    Used in 28 sessions      │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ 🫖 Kyusu               [⋮] │ │
│ │    Used in 12 sessions      │ │
│ └─────────────────────────────┘ │
│                                 │
│ ─────── Custom Vessels ──────   │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ≡ 🫖 盖碗 (Custom)        [⋮] │ │
│ │    Used in 8 sessions       │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**Vessel Card Content**:
- **Drag handle** (≡): Reorder vessels
- **Icon**: Vessel icon (emoji or custom)
- **Name**: Vessel display name
- **Usage count**: Number of sessions using this vessel
- **Menu** (⋮): Edit, Delete (if custom), Hide (if system)
- **System default badge**: "System" for built-in vessels

**System vs Custom Vessels**:
- **System vessels** (12 pre-installed):
  - Can rename (e.g., "Gaiwan" → "盖碗")
  - Cannot delete (can hide)
  - Badge: "System"
- **Custom vessels** (user-created):
  - Full control
  - Can delete (if no sessions using it)
  - Badge: "Custom"

### Actions

**Primary**:
- **FAB (+)** → Add new custom vessel
- **Drag handle (≡)** → Reorder vessels

**Vessel Menu (⋮)**:
- **Edit**: Edit name, icon
- **Hide** (system only): Hide from vessel dropdowns
- **Delete** (custom only): Delete vessel (warns if sessions using it)
- **View sessions**: Navigate to History filtered by this vessel

**Add Vessel Dialog**:
```
┌─────────────────────────────────┐
│ Add Brewing Vessel              │
├─────────────────────────────────┤
│ Name *                          │
│ [                           ]   │
│                                 │
│ Icon (optional)                 │
│ 🫖 🍵 ☕ 🥤 🫙 🍶 [Custom...]   │ ← Icon picker
│                                 │
│ [Save]  [Cancel]                │
└─────────────────────────────────┘
```

**Edit Vessel Dialog** (same as Add, with Delete for custom)

**Delete Confirmation** (same pattern as types):
```
┌─────────────────────────────────┐
│ Delete "My Clay Pot"?           │
├─────────────────────────────────┤
│ 8 sessions are using this       │
│ vessel.                         │
│                                 │
│ Those sessions will keep the    │
│ name, but the vessel will be    │
│ removed from the list.          │
│                                 │
│ [Cancel] [Delete Anyway]        │
└─────────────────────────────────┘
```
- Sessions using deleted vessel retain the vessel name (snapshot)
- Vessel removed from dropdown options

**Empty State** (no custom vessels):
```
┌─────────────────────────────────┐
│ System brewing vessels loaded.  │
│                                 │
│ Tap + to add your own vessels   │
│ with custom names.              │
└─────────────────────────────────┘
```

---

## Cross-Tab Features

### Search (Teas tab only)
- Real-time search as user types
- Searches: name, origin, producer
- Shows match count: "12 results"
- Clear button (X) to reset

### Reordering (Types & Vessels tabs)
- Drag and drop with handle (≡)
- Visual feedback during drag
- Saves immediately on drop
- Order persists across devices (sync)

### Bulk Operations (Teas tab only)
- Long press to enter multi-select
- Select multiple teas
- Bulk actions:
  - Mark as favorite
  - Delete multiple
  - Export selected (CSV)

---

## MVI State

```kotlin
data class LibraryState(
    val currentTab: LibraryTab = LibraryTab.TEAS,

    // Teas tab
    val teas: List<Tea> = emptyList(),
    val filteredTeas: List<Tea> = emptyList(),
    val teaSearchQuery: String = "",
    val teaFilters: TeaFilters = TeaFilters(),
    val teaSortOption: TeaSortOption = TeaSortOption.NAME_ASC,
    val teaViewMode: ViewMode = ViewMode.LIST,
    val selectedTeas: Set<String> = emptySet(),  // IDs for multi-select

    // Types tab
    val teaTypes: List<TeaType> = emptyList(),

    // Vessels tab
    val brewingVessels: List<BrewingVessel> = emptyList(),

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilterSheet: Boolean = false,
    val showTypeDialog: Boolean = false,
    val showVesselDialog: Boolean = false,
    val editingTypeId: String? = null,
    val editingVesselId: String? = null
)

enum class LibraryTab {
    TEAS,
    TYPES,
    VESSELS
}

data class TeaFilters(
    val teaTypeIds: Set<String> = emptySet(),  // Empty = all types
    val statusFilter: TeaStatusFilter = TeaStatusFilter.ALL,
    val sortOption: TeaSortOption = TeaSortOption.NAME_ASC
)

enum class TeaStatusFilter {
    ALL,
    FAVORITES,
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

enum class TeaSortOption {
    NAME_ASC,
    NAME_DESC,
    RECENTLY_BREWED,
    HIGHEST_RATED,
    STOCK_LEVEL,
    DATE_ADDED_NEWEST,
    DATE_ADDED_OLDEST
}

enum class ViewMode {
    LIST,
    GRID
}

sealed interface LibraryIntent {
    // Tab navigation
    data class SelectTab(val tab: LibraryTab) : LibraryIntent

    // Teas tab
    data class SearchTeas(val query: String) : LibraryIntent
    data class ApplyFilters(val filters: TeaFilters) : LibraryIntent
    data object ShowFilterSheet : LibraryIntent
    data object HideFilterSheet : LibraryIntent
    data class SetViewMode(val mode: ViewMode) : LibraryIntent
    data class SelectTea(val teaId: String) : LibraryIntent  // Navigate to detail
    data class ToggleTeaFavorite(val teaId: String) : LibraryIntent
    data class DeleteTea(val teaId: String) : LibraryIntent
    data object AddTea : LibraryIntent  // Navigate to add screen

    // Multi-select
    data class ToggleTeaSelection(val teaId: String) : LibraryIntent
    data object SelectAllTeas : LibraryIntent
    data object DeselectAllTeas : LibraryIntent
    data object BulkFavorite : LibraryIntent
    data object BulkDelete : LibraryIntent
    data object ExitMultiSelect : LibraryIntent

    // Types tab
    data object AddType : LibraryIntent
    data class EditType(val typeId: String) : LibraryIntent
    data class DeleteType(val typeId: String) : LibraryIntent
    data class ReorderTypes(val fromIndex: Int, val toIndex: Int) : LibraryIntent
    data class SaveType(val type: TeaType) : LibraryIntent

    // Vessels tab
    data object AddVessel : LibraryIntent
    data class EditVessel(val vesselId: String) : LibraryIntent
    data class DeleteVessel(val vesselId: String) : LibraryIntent
    data class ReorderVessels(val fromIndex: Int, val toIndex: Int) : LibraryIntent
    data class SaveVessel(val vessel: BrewingVessel) : LibraryIntent
}
```

---

## Actions Summary

### Teas Tab
- Search and filter tea collection
- View as list or grid
- Tap to view details
- Swipe to favorite/edit/delete
- Multi-select for bulk operations
- Add new tea with FAB

### Types Tab
- View all tea types (system + custom)
- Add custom types
- Edit names, colors, defaults
- Reorder types (affects dropdown order)
- Hide system types
- Delete custom types (with safeguards)

### Vessels Tab
- View all vessels (system + custom)
- Add custom vessels
- Edit names, icons
- Reorder vessels (affects dropdown order)
- Hide system vessels
- Delete custom vessels (with safeguards)

---

## Edge Cases

1. **Delete type with teas**: Warn user, reassign to "Other"
2. **Delete vessel with sessions**: Sessions keep name, vessel removed from list
3. **Rename system type**: Allowed, affects all teas using it
4. **Duplicate names**: Warn but allow (user might want "Gaiwan 1", "Gaiwan 2")
5. **No teas in library**: Show empty state on Teas tab
6. **Search with no results**: "No teas found" message
7. **Stock tracking disabled**: Don't show stock indicators
8. **Very long tea names**: Truncate with ellipsis in list view

### 5. Tea Detail Screen
**Purpose**: View comprehensive information about a specific tea, manage stock, and quick-access related brewing sessions.

**Navigation**: Accessed from Collection screen (tap tea card), Quick Actions (home screen), or Search results.

```
┌─────────────────────────────────┐
│ ← Tea Name             [⋮]      │ ← Back + Menu (Edit/Delete)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ │   [Photo Gallery]           │ │ ← Swipeable photos
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ 🍃 Green Tea            ⭐ 4.5  │ ← Type + Rating
│                                 │
│ Origin        Hangzhou, China   │
│ Producer      West Lake Tea Co. │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BREWING RECOMMENDATIONS         │
│                                 │
│ Temperature   80°C / 176°F      │
│ Time          2-3 minutes       │
│ Tea Amount    3g / tsp          │
│ Water Amount  200ml / 7oz       │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ STOCK & PURCHASE                │
│                                 │
│ Current Stock    45g            │
│               [- 5g] [+ 5g]     │ ← Quick adjust
│                                 │
│ Purchase Date    Jan 15, 2026   │
│ Purchase Price   $24.99 (50g)   │
│ Cost per Gram    $0.50          │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ NOTES                           │
│                                 │
│ Delicate vegetal notes with    │
│ slight nuttiness. Best in       │
│ spring. Avoid oversteeping.     │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BREWING STATISTICS              │
│                                 │
│ Total Sessions      24          │
│ Average Rating      4.5/5       │
│ Last Brewed         2 days ago  │
│                                 │
│ [Brewing History Chart]         │ ← 30-day frequency
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ RECENT SESSIONS                 │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Jan 20, 2026 • 2m • ⭐ 5    │ │
│ │ Perfect steep!              │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ Jan 18, 2026 • 2m 30s • ⭐ 4│ │
│ │ Slightly overbrewed         │ │
│ └─────────────────────────────┘ │
│                                 │
│ [View All 24 Sessions →]       │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ [🍵 Log Tea with This]         │ ← Primary action button
│                                 │
└─────────────────────────────────┘
```

---

## Components

### 1. Photo Gallery
**Display**:
- Horizontal swipeable photo carousel
- Page indicator dots (1/3, 2/3, etc.)
- Zoom on tap for full-screen view
- Fallback placeholder if no photos
- Edit button overlay (leads to Edit screen)

**Behavior**:
- Swipe left/right to navigate photos
- Tap photo → Full-screen gallery viewer
- In gallery viewer: pinch to zoom, swipe to dismiss

### 2. Tea Information Section
**Header**:
```
🍃 Green Tea            ⭐ 4.5
├─ Type icon/color     └─ Average rating
└─ Type name
```

**Basic Info** (2-column layout):
- **Origin**: Country/region
- **Producer**: Brand/supplier name
- Favorite indicator (⭐ heart icon in top-right)

**Editable**: Tap any field → Navigate to Edit screen with that field focused

### 3. Brewing Recommendations Section
**Purpose**: Display default/recommended brewing parameters for this tea.

**Fields**:
```
Temperature   80°C / 176°F          ← Based on user preference
Time          2-3 minutes           ← Range or exact
Tea Amount    3g / 1 tsp            ← Grams with volume equivalent
Water Amount  200ml / 7oz           ← Based on user preference
```

**Display Notes**:
- Show both units for temperature/volume based on UserPreferences
- Time can be range (2-3 min) or exact (2m 30s)
- Tea amount shows grams + approximate volume measure (tsp/tbsp)
- If any field is missing, show "Not specified"
- Tap section → Navigate to Edit screen

### 4. Stock & Purchase Section
**Stock Display**:
```
Current Stock    45g
              [- 5g] [+ 5g]
```

**Quick Adjust Buttons**:
- Tap `-5g` → Decrease stock by 5g
- Tap `+5g` → Increase stock by 5g
- Long press → Custom amount dialog
- Shows "Out of Stock" badge if stock ≤ 0
- Shows "Low Stock" warning if stock < 10g

**Purchase Info**:
- **Purchase Date**: Formatted date (e.g., "Jan 15, 2026", "3 months ago")
- **Purchase Price**: Amount and quantity (e.g., "$24.99 (50g)")
- **Cost per Gram**: Calculated (price ÷ quantity)
- If missing: Show "Not tracked" for each field
- Tap section → Navigate to Edit screen to add/update

**Stock Transaction Log** (Collapsed by default):
- Tap "View Stock History" → Expand to show:
  - Adjustments ("+50g - Restock", "-3g - Session on Jan 20")
  - Purchase history
  - Auto-deductions from completed sessions

### 5. Notes Section
**Display**:
- Multi-line text field (read-only)
- Expandable if content exceeds 4 lines ("Show more" / "Show less")
- Markdown support (basic: bold, italic, lists)
- Empty state: "No notes yet. Tap to add."

**Behavior**:
- Tap → Navigate to Edit screen with notes field focused

### 6. Brewing Statistics Section
**Purpose**: Show usage patterns and ratings for this tea.

**Statistics Cards** (2x2 grid):
```
┌──────────────┬──────────────┐
│ 24           │ ⭐ 4.5       │
│ Sessions     │ Avg Rating   │
├──────────────┼──────────────┤
│ 2 days ago   │ 72g          │
│ Last Brewed  │ Total Used   │
└──────────────┴──────────────┘
```

**Brewing History Chart**:
- **Type**: 30-day frequency chart (bar chart or heatmap)
- **X-axis**: Last 30 days
- **Y-axis**: Number of sessions per day
- **Interaction**: Tap date → Filter sessions for that day
- Shows trends (e.g., "You brew this tea most often on weekends")

**Calculations**:
- **Total Sessions**: Count of completed TeaSessions with this teaId (excluding drafts)
- **Average Rating**: Mean of all session ratings (only rated sessions)
- **Last Brewed**: Most recent session timestamp (relative time)
- **Total Used**: Sum of `teaQuantityGrams` from all completed sessions

### 7. Recent Sessions Section
**Display**:
- Shows 3 most recent completed sessions (sorted by timestamp DESC)
- Each session card shows:
  ```
  ┌─────────────────────────────┐
  │ Jan 20, 2026 • 2m • ⭐ 5    │ ← Date, time, rating
  │ Perfect steep!              │ ← Notes (1 line preview)
  │ [Photo thumb] 📷            │ ← Photo indicator if present
  └─────────────────────────────┘
  ```
- Tap session card → Navigate to Session Detail screen
- Shows steep indicator if multi-steep session (e.g., "Steep 1 of 3")

**"View All Sessions" Link**:
- Tap → Navigate to History screen with filter applied (this teaId)
- Shows count (e.g., "View All 24 Sessions →")

**Empty State** (if no sessions):
```
┌─────────────────────────────┐
│   No brewing sessions yet   │
│                             │
│   Start tracking by logging │
│   your first brew below!    │
└─────────────────────────────┘
```

### 8. Primary Action Button
```
┌─────────────────────────────┐
│    [🍵 Log Tea with This]   │ ← Full-width button
└─────────────────────────────┘
```

**Behavior**:
- Tap → Navigate to Log Tea Screen with this tea pre-selected
- Pre-fills brewing parameters from tea recommendations
- If user has previous sessions with this tea, pre-fills with last-used values (overriding recommendations)

---

## Top App Bar Menu (⋮)

**Options**:
1. **Edit** → Navigate to Edit Tea Screen
2. **Toggle Favorite** → Add/remove from favorites (⭐ icon changes)
3. **Duplicate** → Create copy with "(Copy)" suffix
4. **Delete** → Show confirmation dialog

**Future Enhancement** (deprioritized):
- **Share** → Share tea details as text or image

**Delete Confirmation**:
```
┌─────────────────────────────────┐
│ Delete "Dragon Well"?           │
├─────────────────────────────────┤
│                                 │
│ This tea has 24 brewing         │
│ sessions. Past sessions will    │
│ remain, but the tea will be     │
│ removed from your library.      │
│                                 │
│ This action cannot be undone.   │
│                                 │
│         [Cancel] [Delete]       │
└─────────────────────────────────┘
```

**Post-Delete**:
- Soft delete (set `deletedAt` timestamp)
- Navigate back to Collection screen
- Show snackbar: "Tea deleted" with UNDO action (5s timeout)
- UNDO → Clear `deletedAt`, restore tea
- Past sessions retain tea name/details (snapshot)

---

## Use Cases Required

```kotlin
// Domain layer
GetTeaByIdUseCase
GetTeaStatisticsUseCase
GetRecentSessionsForTeaUseCase
UpdateTeaStockUseCase
ToggleTeaFavoriteUseCase
DeleteTeaUseCase
GetStockTransactionHistoryUseCase

// Future Enhancement (deprioritized)
// ShareTeaDetailsUseCase

// Repository methods
suspend fun getTeaById(id: String): Flow<Tea>
suspend fun getTeaStatistics(teaId: String): TeaStatistics
suspend fun getRecentSessions(teaId: String, limit: Int): Flow<List<TeaSession>>
suspend fun adjustStock(teaId: String, delta: Int, reason: String)
suspend fun getStockHistory(teaId: String): Flow<List<StockTransaction>>
```

---

## MVI State Model

```kotlin
data class TeaDetailState(
    val teaId: String,
    val tea: Tea? = null,
    val statistics: TeaStatistics? = null,
    val recentSessions: List<TeaSession> = emptyList(),
    val stockHistory: List<StockTransaction> = emptyList(),

    // UI state
    val isLoading: Boolean = true,
    val error: String? = null,
    val showStockHistory: Boolean = false,
    val showFullNotes: Boolean = false,
    val showFullScreenGallery: Boolean = false,
    val selectedPhotoIndex: Int = 0,
    val showDeleteConfirmation: Boolean = false,
    val showStockAdjustDialog: Boolean = false,
    val pendingStockDelta: Int? = null,

    // Snackbar
    val snackbarMessage: String? = null,
    val showUndoDelete: Boolean = false
)

data class TeaStatistics(
    val totalSessions: Int,
    val averageRating: Float?,
    val lastBrewedAt: Instant?,
    val totalTeaUsedGrams: Int,
    val brewingFrequency: List<DailyBrewCount>,  // Last 30 days
    val favoriteBrewingTime: Duration?,
    val favoriteTemperature: Int?
)

data class DailyBrewCount(
    val date: LocalDate,
    val count: Int
)

data class StockTransaction(
    val id: String,
    val teaId: String,
    val delta: Int,                    // +50 (restock) or -3 (session)
    val reason: StockTransactionReason,
    val notes: String?,
    val timestamp: Instant
)

enum class StockTransactionReason {
    PURCHASE,           // Initial stock or restock
    MANUAL_ADJUSTMENT,  // User manually adjusted
    SESSION_LOGGED,     // Deducted from completed session
    EXPIRATION,         // Tea discarded
    OTHER
}

sealed interface TeaDetailIntent {
    data class LoadTea(val teaId: String) : TeaDetailIntent
    data object Refresh : TeaDetailIntent

    // Gallery
    data class SelectPhoto(val index: Int) : TeaDetailIntent
    data object OpenFullScreenGallery : TeaDetailIntent
    data object CloseFullScreenGallery : TeaDetailIntent
    // Future Enhancement (deprioritized)
    // data class SharePhoto(val photoUri: String) : TeaDetailIntent

    // Stock management
    data class AdjustStock(val delta: Int) : TeaDetailIntent
    data object ShowStockAdjustDialog : TeaDetailIntent
    data object HideStockAdjustDialog : TeaDetailIntent
    data class ConfirmStockAdjustment(val amount: Int, val reason: String) : TeaDetailIntent
    data object ToggleStockHistory : TeaDetailIntent

    // Actions
    data object ToggleFavorite : TeaDetailIntent
    data object NavigateToEdit : TeaDetailIntent
    data object NavigateToLogTea : TeaDetailIntent
    data object DuplicateTea : TeaDetailIntent
    // Future Enhancement (deprioritized)
    // data object ShareTea : TeaDetailIntent

    // Delete
    data object ShowDeleteConfirmation : TeaDetailIntent
    data object HideDeleteConfirmation : TeaDetailIntent
    data object ConfirmDelete : TeaDetailIntent
    data object UndoDelete : TeaDetailIntent

    // Sessions
    data class NavigateToSession(val sessionId: String) : TeaDetailIntent
    data object ViewAllSessions : TeaDetailIntent

    // Notes
    data object ToggleFullNotes : TeaDetailIntent

    // Snackbar
    data object DismissSnackbar : TeaDetailIntent
}
```

---

## Screen Behavior

### Initial Load
1. Receive `teaId` as navigation argument
2. Emit `LoadTea` intent
3. Show loading state (shimmer placeholders)
4. Load tea details, statistics, and recent sessions in parallel:
   ```kotlin
   combine(
       getTeaById(teaId),
       getTeaStatistics(teaId),
       getRecentSessions(teaId, limit = 3)
   ) { tea, stats, sessions ->
       TeaDetailState(
           teaId = teaId,
           tea = tea,
           statistics = stats,
           recentSessions = sessions,
           isLoading = false
       )
   }
   ```
5. Handle errors (tea not found, network issues)

### Stock Adjustment Flow
**Quick Adjust** (±5g buttons):
1. Tap `+5g` or `-5g`
2. Immediately update stock locally (optimistic update)
3. Show snackbar: "Stock updated to 50g"
4. Persist to database
5. Add stock transaction record (reason: MANUAL_ADJUSTMENT)
6. If error, revert and show error message

**Custom Adjust** (long press):
1. Long press any adjust button
2. Show dialog:
   ```
   ┌─────────────────────────────┐
   │ Adjust Stock                │
   ├─────────────────────────────┤
   │                             │
   │ Current: 45g                │
   │                             │
   │ Amount to add/remove:       │
   │ [_______] g                 │
   │                             │
   │ ○ Add (+)                   │
   │ ○ Remove (-)                │
   │                             │
   │ Reason (optional):          │
   │ □ Purchase                  │
   │ □ Expiration                │
   │ □ Other: [__________]       │
   │                             │
   │      [Cancel] [Save]        │
   └─────────────────────────────┘
   ```
3. Validate amount (can't remove more than current stock)
4. Save → Update stock + add transaction record

### Favorite Toggle
1. Tap heart icon in top-right or menu option
2. Optimistic update (toggle immediately)
3. Update in database
4. Show snackbar: "Added to favorites" / "Removed from favorites"

### Delete Flow
1. Tap Delete in menu
2. Show confirmation dialog (see mockup above)
3. If tea has sessions, warn user
4. Confirm → Soft delete tea (`deletedAt = now()`)
5. Navigate back to Collection
6. Show snackbar with UNDO (5 seconds)
7. If UNDO → Clear `deletedAt`, reload screen
8. If timeout → Tea remains soft-deleted (cleanup after 30 days)

### Navigate to Log Tea
1. Tap "Log Tea with This" button
2. Navigate to Log Tea Screen
3. Pre-fill `selectedTea` with this tea
4. Load last-used brewing parameters for this tea (if exists), else use tea recommendations
5. User can modify and start timer or log retrospectively

---

## Edge Cases

1. **Tea not found**: Show error state with "Tea no longer exists" message
2. **No photos**: Show placeholder image (generic tea icon or type-specific icon)
3. **No brewing recommendations**: Show "Not specified" for each field
4. **No purchase info**: Hide purchase section or show "Not tracked"
5. **No sessions**: Show empty state in Recent Sessions section
6. **No notes**: Show "No notes yet. Tap to add."
7. **Stock goes negative**: Warn user, allow save (user might owe tea to someone)
8. **Very long notes**: Collapse to 4 lines with "Show more" button
9. **Chart with no data**: Show "No brewing history yet" in statistics section
10. **Deleted tea with sessions**: Sessions retain tea name (snapshot), but tea can't be logged again
11. **Rapid stock adjustments**: Debounce updates, queue transactions
12. **Offline mode**: Show cached data, queue stock adjustments for sync

---

## Accessibility

- **Screen Reader**: Announce tea name, type, stock level, statistics
- **Photo Gallery**: "Photo 1 of 3, swipe to view more"
- **Stock Adjustment**: "Increase stock by 5 grams button", confirm with haptic feedback
- **Action Buttons**: Clear labels ("Log tea with Dragon Well")
- **Charts**: Provide data table alternative for screen readers
- **High Contrast**: Ensure chart colors, stock badges, and text readable
- **Dynamic Type**: Support user font size preferences

---

## Performance Considerations

1. **Lazy Loading**: Load stock history only when expanded
2. **Image Optimization**:
   - Thumbnail for photo carousel
   - Full-res only in full-screen gallery
   - Lazy load images as user scrolls
3. **Statistics Calculation**: Cache statistics, recalculate only on data change
4. **Chart Rendering**: Use efficient charting library, limit data points
5. **Database Queries**:
   - Single query for tea details
   - Separate optimized queries for statistics and recent sessions
   - Use indices on `teaId`, `timestamp`, `deletedAt`
6. **Memory**: Release full-screen gallery resources when closed

---

## Actions Summary

**Primary**:
- **Log Tea** → Pre-fill Log Tea Screen with this tea's details

**Secondary**:
- **Edit** → Navigate to Edit Tea Screen
- **Toggle Favorite** → Add/remove from favorites
- **Adjust Stock** → Quick adjust (±5g) or custom amount
- **View Sessions** → Navigate to filtered History screen

**Tertiary**:
- **View Photo** → Full-screen gallery
- **Duplicate** → Create copy of tea
- **Delete** → Soft delete with confirmation and undo
- **Expand Notes** → Show full notes if truncated
- **View Stock History** → Show transaction log

### 6. Add/Edit Tea Screen
**Purpose**: Create new tea entries or modify existing ones in the library.

**Navigation**:
- **Add Mode**: From Collection FAB, Quick Add inline (Log Tea), or "Duplicate" action
- **Edit Mode**: From Tea Detail menu, Collection screen "Edit" swipe action, or tap any field in Detail screen

**Mode Detection**:
- **Add Mode**: No `teaId` passed → Empty form, "Add Tea" title
- **Edit Mode**: `teaId` passed → Pre-filled form, "Edit Tea" title

```
┌─────────────────────────────────┐
│ ✕  Add Tea                 Save │ ← Cancel + Title + Save
├─────────────────────────────────┤
│                                 │
│ PHOTOS                          │
│                                 │
│ ┌───┐ ┌───┐ ┌───┐              │
│ │ + │ │📷 │ │📷 │              │ ← Add photo + existing photos
│ └───┘ └───┘ └───┘              │
│ Tap to add or reorder           │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BASIC INFORMATION              *│
│                                 │
│ Tea Name *                      │
│ [_________________________]     │
│                                 │
│ Tea Type *                      │
│ [Green Tea            ▼]       │
│ + Add custom type               │
│                                 │
│ Origin                          │
│ [_________________________]     │
│ e.g., Hangzhou, China           │
│                                 │
│ Producer                        │
│ [_________________________]     │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ PURCHASE & STOCK                │
│                                 │
│ Purchase Date                   │
│ [Jan 15, 2026         📅]      │
│                                 │
│ Purchase Price                  │
│ [$_________]  for  [___] g      │
│ Cost per gram: $0.50            │
│                                 │
│ Initial Stock                   │
│ [_________] g                   │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ DEFAULT BREWING PARAMETERS      │
│                                 │
│ Temperature                     │
│ [80] °C  /  [176] °F           │
│ (Auto-filled from tea type)     │
│                                 │
│ Brewing Time                    │
│ [2] min  [30] sec               │
│ (Auto-filled from tea type)     │
│                                 │
│ Tea Amount (per serving)        │
│ [3] g  ≈ 1 tsp                 │
│                                 │
│ Water Amount (per serving)      │
│ [200] ml  /  [7] oz            │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ NOTES                           │
│                                 │
│ [________________________       │
│  ________________________       │
│  ________________________       │
│  ________________________]      │
│                                 │
│ Add tasting notes, storage      │
│ tips, or other details...       │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ ⭐ Add to Favorites             │ ← Toggle switch
│                                 │
└─────────────────────────────────┘
```

---

## Components

### 1. Top App Bar
**Add Mode**:
- **Left**: ✕ Close button → Discard dialog (if changes made)
- **Center**: "Add Tea"
- **Right**: "Save" button (enabled only when valid)

**Edit Mode**:
- **Left**: ✕ Close button → Discard dialog (if changes made)
- **Center**: "Edit Tea"
- **Right**: "Save" button (enabled only when valid)

**Save Button States**:
- **Disabled** (gray): Required fields missing or invalid
- **Enabled** (primary color): Form is valid, tap to save
- **Loading** (spinner): Saving in progress

### 2. Photo Management Section
**Layout**: Horizontal scrollable row of photo slots (max 15 photos)

**Add Photo Button** (first slot):
```
┌───┐
│ + │ ← Dashed border, tap to add
└───┘
```

**Existing Photos** (draggable):
```
┌───┐
│📷 │ ← Tap to view, long press to reorder
│ ✕ │ ← Delete button (top-right corner)
└───┘
```

**Behavior**:
- Tap `+` → Photo picker (camera or gallery)
- Platform photo picker:
  - **Android**: System photo picker or camera intent
  - **iOS**: PHPickerViewController or camera
- Select photo → Compress and add to list
- Tap existing photo → Full-screen preview with delete option
- Long press + drag → Reorder photos
- Tap ✕ on photo → Remove (with confirm if only photo)
- Max 15 photos, hide `+` button when limit reached

**Image Handling**:
- Compress to max 1920x1920, 85% quality (JPEG)
- Store locally in app documents directory
- Upload to Firebase Storage when syncing (if enabled)
- Generate thumbnail (300x300) for list views

### 3. Basic Information Section
**Tea Name** (Required):
- Text input, max 100 characters
- Validation: Not empty, no leading/trailing spaces
- Error: "Tea name is required"
- Character counter (95/100) when approaching limit

**Tea Type** (Required):
- Dropdown picker showing all tea types (system + custom)
- Sorted by `displayOrder` (user-customizable)
- Color indicator next to each type
- "Add custom type" link below dropdown → Opens inline quick add:
  ```
  ┌─────────────────────────────┐
  │ Add Custom Type             │
  ├─────────────────────────────┤
  │ Name: [_____________]       │
  │ Color: [🎨 picker]         │
  │         [Cancel] [Add]      │
  └─────────────────────────────┘
  ```
- After adding, new type auto-selected

**Origin**:
- Text input, max 100 characters
- Optional, placeholder: "e.g., Hangzhou, China"
- Autocomplete suggestions based on existing teas (optional enhancement)

**Producer**:
- Text input, max 100 characters
- Optional, placeholder: "e.g., West Lake Tea Co."
- Autocomplete suggestions (optional enhancement)

### 4. Purchase & Stock Section
**Purchase Date**:
- Date picker input (tap to open calendar)
- Default: Today's date
- Display format: "MMM dd, yyyy" (e.g., "Jan 15, 2026")
- Optional, can be cleared

**Purchase Price**:
- Two-part input:
  1. **Amount**: Currency input (decimal, 2 places)
  2. **Quantity**: Integer + unit (g)
- Displays calculated "Cost per gram" below (read-only)
- Optional, both fields must be filled or both empty
- Validation: Price > 0, Quantity > 0

**Initial Stock** (Add mode only):
- Integer input (grams)
- Optional, default: empty (not tracked)
- If filled, creates initial stock transaction when tea created
- **Only visible in Add mode**: Stock adjustments in Edit mode happen from Tea Detail screen
- Note: "Stock can be adjusted anytime from Tea Detail screen"

### 5. Default Brewing Parameters Section
**Purpose**: Set recommended brewing parameters for this tea (used to pre-fill Log Tea screen)

**Temperature**:
- Dual input: Celsius and Fahrenheit (linked conversion)
- User edits in preferred unit (from UserPreferences)
- Other unit auto-updates
- Range: 0-100°C (32-212°F)
- Auto-fills from TeaType default when type selected (only if field is empty)

**Brewing Time**:
- Two inputs: Minutes (0-60) and Seconds (0-59)
- Auto-fills from TeaType default when type selected (only if field is empty)
- Display total: "2m 30s"

**Tea Amount**:
- Integer input (grams)
- Shows approximate volume equivalent: "≈ 1 tsp" (calculated: 1 tsp ≈ 2.5g)
- Optional

**Water Amount**:
- Dual input: Milliliters and Fluid Ounces (linked conversion)
- User edits in preferred unit
- Optional

**Smart Defaults Behavior**:
- When tea type is selected, temperature and brewing time auto-fill from type's defaults
- **Only fills empty fields**: Does not overwrite user-entered values
- User can manually change values anytime
- Changing tea type will update empty fields only

### 6. Notes Section
- Multi-line text input (4+ lines visible)
- Max 1000 characters
- Character counter when approaching limit (950/1000)
- Optional
- Placeholder: "Add tasting notes, storage tips, or other details..."
- Markdown support (optional future enhancement)

### 7. Favorite Toggle
- Switch component at bottom
- Label: "Add to Favorites"
- Default: OFF
- Independent of other fields

---

## Validation Rules

### Required Fields
1. **Tea Name**: Must not be empty
2. **Tea Type**: Must be selected

### Field-Specific Validation
- **Tea Name**: 1-100 characters, trimmed
- **Origin**: Max 100 characters
- **Producer**: Max 100 characters
- **Purchase Price**: If filled, must be > 0
- **Purchase Quantity**: If price filled, must be > 0
- **Initial Stock**: If filled, must be ≥ 0
- **Temperature**: 0-100°C (32-212°F)
- **Brewing Time**: Min 0s, Max 60m
- **Tea Amount**: If filled, must be > 0
- **Water Amount**: If filled, must be > 0
- **Notes**: Max 1000 characters

### Real-Time Validation
- Show error messages below invalid fields (red text)
- Disable Save button if any required fields missing or any field invalid
- Valid state → Enable Save button

### Example Validation Messages
- "Tea name is required"
- "Please select a tea type"
- "Temperature must be between 0-100°C"
- "Price and quantity must both be filled or both empty"
- "Notes cannot exceed 1000 characters"

---

## MVI State Model

```kotlin
data class AddEditTeaState(
    val mode: EditMode,
    val teaId: String?,

    // Form fields
    val name: String = "",
    val selectedTypeId: String? = null,
    val origin: String = "",
    val producer: String = "",
    val purchaseDate: LocalDate? = null,
    val purchasePrice: Double? = null,
    val purchaseQuantityGrams: Int? = null,
    val initialStock: Int? = null,
    val defaultTemperatureCelsius: Int? = null,
    val defaultBrewingTime: Duration? = null,
    val defaultTeaAmountGrams: Int? = null,
    val defaultWaterAmountMl: Int? = null,
    val notes: String = "",
    val photos: List<String> = emptyList(),  // Local URIs or paths
    val isFavorite: Boolean = false,

    // Available options
    val teaTypes: List<TeaType> = emptyList(),

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val showResumeDialog: Boolean = false,  // "Resume previous entry?" prompt
    val showPhotoPicker: Boolean = false,
    val showTypeQuickAdd: Boolean = false,
    val selectedPhotoIndex: Int? = null,
    val showPhotoPreview: Boolean = false,
    val error: String? = null,
    val hasAutoSavedData: Boolean = false,  // Local storage has saved form data

    // Calculated
    val costPerGram: Double? = null,  // purchasePrice / purchaseQuantityGrams
    val isValid: Boolean = false      // All validations pass
)

enum class EditMode {
    ADD,    // Creating new tea
    EDIT    // Modifying existing tea
}

sealed interface AddEditTeaIntent {
    data class Initialize(val teaId: String?) : AddEditTeaIntent  // null = Add mode
    data object LoadTeaTypes : AddEditTeaIntent

    // Form updates
    data class UpdateName(val name: String) : AddEditTeaIntent
    data class SelectType(val typeId: String) : AddEditTeaIntent
    data class UpdateOrigin(val origin: String) : AddEditTeaIntent
    data class UpdateProducer(val producer: String) : AddEditTeaIntent
    data class UpdatePurchaseDate(val date: LocalDate?) : AddEditTeaIntent
    data class UpdatePurchasePrice(val price: Double?) : AddEditTeaIntent
    data class UpdatePurchaseQuantity(val grams: Int?) : AddEditTeaIntent
    data class UpdateInitialStock(val grams: Int?) : AddEditTeaIntent
    data class UpdateTemperature(val celsius: Int?) : AddEditTeaIntent
    data class UpdateBrewingTime(val duration: Duration?) : AddEditTeaIntent
    data class UpdateTeaAmount(val grams: Int?) : AddEditTeaIntent
    data class UpdateWaterAmount(val ml: Int?) : AddEditTeaIntent
    data class UpdateNotes(val notes: String) : AddEditTeaIntent
    data class ToggleFavorite(val isFavorite: Boolean) : AddEditTeaIntent

    // Photo management
    data object ShowPhotoPicker : AddEditTeaIntent
    data class AddPhoto(val uri: String) : AddEditTeaIntent
    data class RemovePhoto(val index: Int) : AddEditTeaIntent
    data class ReorderPhotos(val fromIndex: Int, val toIndex: Int) : AddEditTeaIntent
    data class ShowPhotoPreview(val index: Int) : AddEditTeaIntent
    data object HidePhotoPreview : AddEditTeaIntent

    // Quick add type
    data object ShowTypeQuickAdd : AddEditTeaIntent
    data object HideTypeQuickAdd : AddEditTeaIntent
    data class CreateAndSelectType(val name: String, val color: Color) : AddEditTeaIntent

    // Actions
    data object Save : AddEditTeaIntent
    data object Cancel : AddEditTeaIntent

    // Resume previous entry
    data object ShowResumeDialog : AddEditTeaIntent
    data object ResumeFromAutoSave : AddEditTeaIntent
    data object StartFresh : AddEditTeaIntent
}
```

---

## Use Cases Required

```kotlin
// Domain layer
CreateTeaUseCase
UpdateTeaUseCase
GetTeaByIdUseCase
ValidateTeaDataUseCase
GetAllTeaTypesUseCase
CreateTeaTypeUseCase

// Repository methods
suspend fun createTea(tea: Tea): Result<String>  // Returns new teaId
suspend fun updateTea(tea: Tea): Result<Unit>
suspend fun getTeaById(id: String): Flow<Tea?>
suspend fun getTeaTypes(): Flow<List<TeaType>>
```

---

## Screen Behavior

### Initial Load

**Add Mode** (`teaId = null`):
1. Check local storage for auto-saved form data
2. If auto-saved data exists → Show resume dialog:
   ```
   ┌─────────────────────────────┐
   │ Resume Previous Entry?      │
   ├─────────────────────────────┤
   │ You have an unsaved tea     │
   │ entry. Would you like to    │
   │ continue where you left off?│
   │                             │
   │  [Start Fresh] [Resume]     │
   └─────────────────────────────┘
   ```
3. If Resume → Restore form fields from local storage
4. If Start Fresh → Clear local storage, show empty form
5. Load tea types from database
6. Set default purchase date to today (if not resuming)
7. Set temperature unit and volume unit from UserPreferences
8. Ready for input

**Edit Mode** (`teaId != null`):
1. Show loading state (shimmer or spinner)
2. Load tea by ID from database
3. Load tea types from database
4. Pre-fill form with tea data:
   ```kotlin
   state.copy(
       mode = EditMode.EDIT,
       teaId = tea.id,
       name = tea.name,
       selectedTypeId = tea.typeId,
       origin = tea.origin,
       producer = tea.producer,
       purchaseDate = tea.purchaseDate,
       purchasePrice = tea.purchasePrice,
       purchaseQuantityGrams = tea.purchaseQuantityGrams,
       // initialStock field hidden in Edit mode
       defaultTemperatureCelsius = tea.defaultTemperatureCelsius,
       defaultBrewingTime = tea.defaultBrewingTime,
       defaultTeaAmountGrams = tea.defaultTeaAmountGrams,
       defaultWaterAmountMl = tea.defaultWaterAmountMl,
       notes = tea.notes,
       photos = tea.photos,
       isFavorite = tea.isFavorite,
       isLoading = false
   )
   ```
5. Form data auto-saves as user edits

**Duplicate Mode** (Add mode with pre-filled data):
- Same as Add mode, but pre-fill fields from source tea
- Append " (Copy)" to name
- Clear `teaId` (creating new tea)
- Reset stock to empty (don't duplicate stock)

### Real-Time Validation

On each field update:
1. Update state with new value
2. Mark `hasUnsavedChanges = true`
3. Validate all fields:
   ```kotlin
   fun validate(state: AddEditTeaState): Map<String, String> {
       val errors = mutableMapOf<String, String>()

       if (state.name.isBlank()) {
           errors["name"] = "Tea name is required"
       } else if (state.name.length > 100) {
           errors["name"] = "Name cannot exceed 100 characters"
       }

       if (state.selectedTypeId == null) {
           errors["type"] = "Please select a tea type"
       }

       if (state.purchasePrice != null && state.purchaseQuantityGrams == null) {
           errors["purchaseQuantity"] = "Quantity required when price is specified"
       }

       if (state.defaultTemperatureCelsius != null) {
           if (state.defaultTemperatureCelsius!! < 0 || state.defaultTemperatureCelsius!! > 100) {
               errors["temperature"] = "Temperature must be between 0-100°C"
           }
       }

       // ... more validations

       return errors
   }
   ```
4. Update `validationErrors` map
5. Update `isValid = validationErrors.isEmpty() && name.isNotBlank() && selectedTypeId != null`
6. Enable/disable Save button based on `isValid`

### Auto-Save to Local Storage

On each field update:
1. User edits any field (name, type, photos, etc.)
2. Debounce 500ms after last change
3. Serialize current form state to JSON
4. Save to platform local storage (SharedPreferences/UserDefaults)
5. Set `hasAutoSavedData = true`
6. Continue accepting input

**Storage Key**: `add_tea_autosave` (cleared on successful save)

**What's Saved**:
- All form field values
- Photo URIs (validated on resume)
- Selected type ID
- Timestamp of last save

**What's NOT Saved**:
- Validation errors (recalculated on resume)
- UI state flags (photo picker open, etc.)

### Auto-Fill from Tea Type

When user selects a tea type:
1. Emit `SelectType(typeId)` intent
2. Lookup selected TeaType's `defaultTemperatureCelsius` and `defaultBrewingTime`
3. If temperature field is empty → Fill with type's default
4. If brewing time field is empty → Fill with type's default
5. If fields already have values → Don't overwrite (preserve user input)
6. User can manually edit filled values anytime

### Photo Management Flow

**Add Photo**:
1. Tap `+` button → Emit `ShowPhotoPicker` intent
2. Platform photo picker opens:
   - **Android**: `ActivityResultLauncher` with `PickVisualMedia`
   - **iOS**: `PHPickerViewController`
3. User selects photo or takes new photo
4. Receive URI → Emit `AddPhoto(uri)` intent
5. Process image:
   - Compress to 1920x1920, 85% quality
   - Save to app documents directory
   - Generate unique filename (UUID)
   - Add local path to `photos` list
6. Update state with new photo

**Remove Photo**:
1. Tap ✕ on photo → Emit `RemovePhoto(index)` intent
2. If only photo and form has other data, show confirm dialog:
   ```
   Remove this photo?
   [Cancel] [Remove]
   ```
3. Confirm → Remove from list, mark as deleted (cleanup on save)
4. Update state

**Reorder Photos**:
1. Long press photo → Enter drag mode
2. Drag to new position
3. Release → Emit `ReorderPhotos(from, to)` intent
4. Update `photos` list order

**Preview Photo**:
1. Tap photo → Emit `ShowPhotoPreview(index)` intent
2. Show full-screen photo with:
   - Swipe to navigate between photos
   - ✕ to close
   - 🗑️ to delete
3. Close → Emit `HidePhotoPreview` intent

### Quick Add Tea Type

**Flow**:
1. Tap "+ Add custom type" link → Emit `ShowTypeQuickAdd` intent
2. Show inline dialog:
   ```
   ┌─────────────────────────────┐
   │ Add Custom Type             │
   ├─────────────────────────────┤
   │ Name: [_____________]       │
   │ Color: [🎨 Color picker]   │
   │         [Cancel] [Add]      │
   └─────────────────────────────┘
   ```
3. User fills name and picks color
4. Tap Add → Emit `CreateAndSelectType(name, color)` intent
5. Create new TeaType:
   ```kotlin
   TeaType(
       id = UUID.randomUUID().toString(),
       name = name,
       color = color,
       isSystemDefault = false,
       displayOrder = teaTypes.maxOf { it.displayOrder } + 1,
       userId = currentUserId
   )
   ```
6. Save to database
7. Reload tea types
8. Auto-select new type in dropdown
9. Close dialog

### Save Flow

**Validation**:
1. User taps Save button → Emit `Save` intent
2. Final validation check (should already be valid if button enabled)
3. If invalid, show errors and prevent save

**Add Mode**:
1. Set `isSaving = true`, disable Save button
2. Create new Tea object:
   ```kotlin
   Tea(
       id = UUID.randomUUID().toString(),
       name = state.name.trim(),
       typeId = state.selectedTypeId!!,
       origin = state.origin.takeIf { it.isNotBlank() },
       producer = state.producer.takeIf { it.isNotBlank() },
       purchaseDate = state.purchaseDate,
       purchasePrice = state.purchasePrice,
       purchaseQuantityGrams = state.purchaseQuantityGrams,
       currentStock = state.initialStock ?: 0,
       defaultTemperatureCelsius = state.defaultTemperatureCelsius,
       defaultBrewingTime = state.defaultBrewingTime,
       defaultTeaAmountGrams = state.defaultTeaAmountGrams,
       defaultWaterAmountMl = state.defaultWaterAmountMl,
       notes = state.notes.takeIf { it.isNotBlank() },
       photos = state.photos,
       isFavorite = state.isFavorite,
       userId = currentUserId,
       createdAt = Clock.System.now(),
       updatedAt = Clock.System.now(),
       deletedAt = null
   )
   ```
3. Save to database via `CreateTeaUseCase`
4. If initial stock > 0, create stock transaction:
   ```kotlin
   StockTransaction(
       id = UUID.randomUUID().toString(),
       teaId = tea.id,
       delta = state.initialStock!!,
       reason = StockTransactionReason.PURCHASE,
       notes = "Initial stock",
       timestamp = Clock.System.now()
   )
   ```
5. On success:
   - Set `hasUnsavedChanges = false`
   - Show success snackbar: "Tea added"
   - Navigate back to Collection (or Detail screen if from Quick Add)
6. On error:
   - Show error message: "Failed to save tea. Please try again."
   - Keep form open, re-enable Save button

**Edit Mode**:
1. Set `isSaving = true`, disable Save button
2. Update Tea object with changed fields:
   ```kotlin
   existingTea.copy(
       name = state.name.trim(),
       typeId = state.selectedTypeId!!,
       origin = state.origin.takeIf { it.isNotBlank() },
       // ... all other fields
       updatedAt = Clock.System.now()
   )
   ```
3. Calculate stock delta if stock changed:
   ```kotlin
   val stockDelta = state.initialStock - existingTea.currentStock
   if (stockDelta != 0) {
       // Create stock transaction
   }
   ```
4. Update in database via `UpdateTeaUseCase`
5. On success:
   - Set `hasUnsavedChanges = false`
   - Show success snackbar: "Tea updated"
   - Navigate back to Detail screen
6. On error:
   - Show error message
   - Keep form open

### Auto-Save & Cancel Flow

**Auto-Save Behavior**:
- Form data is automatically persisted to local storage as user types
- Debounced (500ms after last keystroke)
- Allows user to leave and return without losing progress
- **No "draft" tea entities created**: Tea only created when Save is tapped with valid data

**Cancel Flow**:
1. User taps ✕ → Emit `Cancel` intent
2. Immediately navigate back
3. Auto-saved form data remains in local storage
4. Next time Add Tea is opened, prompt: "Resume previous entry?" [Yes] [Start Fresh]
5. If Yes → Restore form fields from local storage
6. If Start Fresh → Clear local storage, show empty form
7. Local storage cleared after successful save

---

## Edge Cases

1. **Tea type deleted while editing**: Detect missing type, show error, prevent save until type reselected
2. **Very long names**: Enforce 100 char limit with real-time counter
3. **Photos fail to load**: Show placeholder, allow user to retry or remove
4. **Duplicate tea names**: Allow (user might have multiple "Dragonwell" from different sources)
5. **Initial stock only in Add mode**: Stock field hidden in Edit mode (adjustments via Tea Detail)
6. **Purchase price without quantity**: Require both or neither
7. **Corrupt photo file**: Catch error, show "Failed to load photo", allow removal
8. **App closed while editing**: Auto-saved data persists, prompt to resume on next Add Tea
9. **Offline mode**: Save locally, queue for Firebase sync when online
10. **Form too long**: Scroll view with sticky Save button in app bar
11. **Type quick add fails**: Show error, keep dialog open for retry
12. **Max photos reached**: Hide + button, show "Max 15 photos"
13. **Type changed with existing defaults**: Only auto-fill empty fields, preserve user entries

---

## Accessibility

- **Screen Reader**: Announce required fields, validation errors, field hints
- **Labels**: Clear labels for all inputs ("Tea name, required", "Purchase date, optional")
- **Errors**: Announce validation errors immediately when field loses focus
- **Photo Actions**: "Add photo button", "Delete photo 1 of 3", "Reorder photos"
- **Focus Order**: Logical top-to-bottom order through form
- **Touch Targets**: Minimum 48dp for buttons, switches, photo tiles
- **Dynamic Type**: Support user font size preferences
- **High Contrast**: Clear distinction between enabled/disabled states

---

## Performance Considerations

1. **Image Compression**: Compress photos on background thread to avoid UI freeze
2. **Real-Time Validation**: Debounce validation on text fields (300ms)
3. **Photo Picker**: Lazy load thumbnails, full-res only on preview
4. **Database Queries**: Load tea types once, cache in state
5. **Auto-Save Form Data**: Debounced (500ms after last change), persists to local storage
6. **Memory**: Release photo resources when screen closed
7. **Large Photos**: Warn user if photo > 10MB, recommend compression

---

## Actions Summary

**Primary**:
- **Save** → Create or update tea in database
- **Cancel** → Navigate back (form data auto-saved to local storage)

**Secondary**:
- **Add Photo** → Pick from gallery or take new photo (max 15)
- **Remove Photo** → Delete photo with confirmation
- **Reorder Photos** → Drag to new position
- **Resume Entry** → Restore auto-saved form data

**Tertiary**:
- **Preview Photo** → Full-screen view with zoom
- **Quick Add Type** → Create custom tea type inline
- **Clear Field** → Reset optional field to empty
- **Auto-fill Defaults** → Temperature/time auto-fill when tea type selected

### 7. History Screen
**Purpose**: Browse, search, and manage complete brewing session history with powerful filtering and multiple view modes.

**Navigation**: Bottom nav "History" tab, or filtered from Tea Detail "View All Sessions".

```
┌─────────────────────────────────┐
│ History              [🔍] [⋮]  │ ← Search + Menu (export, view mode)
├─────────────────────────────────┤
│ [🗂️ Filter] [📅] [⭐ All]     │ ← Filter chips + Sort dropdown
├─────────────────────────────────┤
│                                 │
│ ┌─ Today ─────────────────────┐ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🍵 Dragon Well              │ │
│ │ Green Tea • 2:30 • 80°C     │ │
│ │ 4:15 PM • ⭐⭐⭐⭐⭐         │ │
│ │ [DRAFT] Perfect steep!      │ │ ← Draft indicator
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🍵 Tie Guan Yin (Steep 2/3) │ │ ← Multi-steep indicator
│ │ Oolong • 1:45 • 95°C        │ │
│ │ 2:30 PM • ⭐⭐⭐⭐           │ │
│ │ Second steep was smoother   │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─ Yesterday ──────────────────┐│
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🍵 Sencha                   │ │
│ │ Green Tea • 2:00 • 75°C     │ │
│ │ 7:30 AM • ⭐⭐⭐⭐⭐          │ │
│ │ Morning routine brew        │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─ This Week ──────────────────┐│
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🍵 Earl Grey                │ │
│ │ Black Tea • 3:00 • 100°C    │ │
│ │ Jan 19 • ⭐⭐⭐              │ │
│ │ A bit too strong            │ │
│ └─────────────────────────────┘ │
│                                 │
│         [Load More...]          │
│                                 │
└─────────────────────────────────┘
```

---

## View Modes

### 1. List View (Default)
**Layout**: Chronological list grouped by time periods

**Time Period Groups**:
- **Today**: All sessions from today (00:00 - 23:59)
- **Yesterday**: All sessions from previous day
- **This Week**: Mon-Sun of current week (excluding today/yesterday)
- **Last Week**: Previous week
- **This Month**: Current month (excluding weeks above)
- **[Month Name]**: Previous months (Jan 2026, Dec 2025, etc.)
- **[Year]**: Older years grouped by year

**Session Cards**: (See mockup above)
- Tea photo thumbnail (48x48dp, circular or rounded square)
- Tea name (bold, 1 line with ellipsis)
- Type • Brewing time • Temperature (secondary text, 1 line)
- Timestamp • Rating (tertiary text with star icons)
- Notes preview (1 line, gray text, optional if present)
- Draft badge (orange chip) if status = DRAFT
- Multi-steep indicator if part of session group

**Interaction**:
- Tap card → Navigate to Session Detail
- Swipe left → Show actions (Edit, Delete, Brew Again)
- Long press → Multi-select mode

### 2. Calendar View
**Layout**: Monthly calendar with session indicators

```
┌─────────────────────────────────┐
│     ← January 2026 →            │ ← Month navigation
├─────────────────────────────────┤
│ Su  Mo  Tu  We  Th  Fr  Sa     │
│                 1●● 2   3●      │ ← Dots indicate sessions
│ 4   5●  6   7   8●● 9   10     │
│ 11  12  13● 14  15● 16  17     │
│ 18  19● 20●●21  22  23  24     │
│ 25  26  27  28  29  30  31     │
├─────────────────────────────────┤
│ ┌─ January 20 (3 sessions) ────┐│
│                                 │
│ [Session cards for selected day]│
│                                 │
└─────────────────────────────────┘
```

**Behavior**:
- Default: Shows current month with session dots
- Dot colors: Green (1 session), Blue (2-3 sessions), Purple (4+ sessions)
  - **Note**: Colors will be revisited during theme/color design phase
- Tap date → Show sessions for that day below calendar
- Swipe month left/right → Navigate months
- Empty days (no dots) → Tap shows "No sessions on this day"
- Today's date: Highlighted with border

### 3. Compact View
**Layout**: Dense list without groupings, smaller cards

```
┌─────────────────────────────────┐
│ Dragon Well • Jan 20 • ⭐⭐⭐⭐⭐ │
│ 2:30 • 80°C • Green Tea         │
├─────────────────────────────────┤
│ Tie Guan Yin (2/3) • Jan 20 • ⭐⭐│
│ 1:45 • 95°C • Oolong            │
├─────────────────────────────────┤
│ Sencha • Jan 19 • ⭐⭐⭐⭐⭐      │
│ 2:00 • 75°C • Green Tea         │
└─────────────────────────────────┘
```

**Use Case**: Quickly scan many sessions, less visual weight

---

## Search & Filter

### Search Bar
**Location**: Top app bar, tap 🔍 to expand

```
┌─────────────────────────────────┐
│ [🔍 Search tea name or notes...] │
└─────────────────────────────────┘
```

**Search Behavior**:
- Real-time search as user types (debounced 300ms)
- Searches:
  - Tea name (partial match, case-insensitive)
  - Tea type name
  - Session notes
- Highlights matching text in results
- Shows "No results found" if no matches
- Clear button (✕) to reset search

### Filter Chips
**Layout**: Horizontal scrollable row below search

```
[🗂️ Filter] [📅 All Time] [⭐ All Ratings] [🍵 All Types]
```

**Active Filters**:
- Show count badge on Filter button (e.g., "Filter (3)")
- Each active filter shows as dismissible chip (with ✕)
- Tap chip → Edit that filter
- Tap ✕ on chip → Remove filter

### Filter Sheet
**Trigger**: Tap "Filter" button

```
┌─────────────────────────────────┐
│ Filter Sessions          [Reset]│
├─────────────────────────────────┤
│                                 │
│ DATE RANGE                      │
│ ○ All Time                      │
│ ○ Today                         │
│ ○ Last 7 Days                   │
│ ○ Last 30 Days                  │
│ ○ This Month                    │
│ ○ Last Month                    │
│ ○ This Year                     │
│ ○ Custom Range                  │
│   [Jan 1, 2026 - Jan 20, 2026] │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ TEA TYPE                        │
│ □ Green (24)                    │ ← Show count per type
│ □ Black (15)                    │
│ □ Oolong (8)                    │
│ □ White (5)                     │
│ □ All Types                     │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ RATING                          │
│ ○ All Ratings                   │
│ ○ ⭐⭐⭐⭐⭐ (5 stars only)       │
│ ○ ⭐⭐⭐⭐ (4+ stars)             │
│ ○ ⭐⭐⭐ (3+ stars)               │
│ ○ Unrated                       │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BREWING VESSEL                  │
│ □ Gaiwan (12)                   │
│ □ Teapot (18)                   │
│ □ Mug (22)                      │
│ □ All Vessels                   │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ TEMPERATURE                     │
│ [60] °C to [100] °C             │ ← Range sliders
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ SESSION STATUS                  │
│ ☑ Completed                     │
│ ☑ Draft                         │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ MULTI-STEEP                     │
│ ○ All Sessions                  │
│ ○ Single Steeps Only            │
│ ○ Multi-Steeps Only             │
│ ○ Parent Sessions Only          │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│         [Cancel] [Apply]        │
│                                 │
└─────────────────────────────────┘
```

**Filter Options**:

1. **Date Range**: Quick presets or custom range picker
2. **Tea Type**: Multi-select checkboxes with session counts
3. **Rating**: Single-select radio buttons (5★, 4★+, 3★+, unrated, all)
4. **Vessel**: Multi-select checkboxes with session counts
5. **Temperature**: Dual-thumb range slider (0-100°C or 32-212°F)
6. **Status**: Checkboxes for Completed/Draft
7. **Multi-Steep**: Filter parent sessions, child steeps, single steeps, or all

**Reset Button**: Clear all filters back to defaults (All Time, All Ratings, etc.)

**Apply Button**: Close sheet, apply filters, update results

### Sort Options
**Location**: Dropdown next to filter chips

```
[Sort: Newest First ▼]
```

**Sort Options**:
- **Newest First** (default): timestamp DESC
- **Oldest First**: timestamp ASC
- **Highest Rated**: rating DESC, then timestamp DESC
- **Lowest Rated**: rating ASC, then timestamp DESC
- **Longest Brew**: brewingTime DESC
- **Shortest Brew**: brewingTime ASC
- **Tea Name (A-Z)**: tea.name ASC
- **Tea Name (Z-A)**: tea.name DESC

---

## Session Cards (List View)

### Standard Session Card
```
┌─────────────────────────────────┐
│ 🍵 Dragon Well                  │ ← Photo + Name
│ Green Tea • 2:30 • 80°C         │ ← Type • Duration • Temp
│ 4:15 PM • ⭐⭐⭐⭐⭐             │ ← Time • Rating
│ Perfect steep! Notes here...    │ ← Notes preview (optional)
└─────────────────────────────────┘
```

**Components**:
- **Photo**: Circular thumbnail (48x48dp), tea's first photo or type icon
- **Name**: Tea name (bold, 1 line, ellipsis if long)
- **Metadata Line**: Type • Brewing time • Temperature
  - Type: Tea type name with color indicator
  - Time: Formatted duration (2:30, 1:45, 3:00)
  - Temp: In user's preferred unit (80°C or 176°F)
- **Timestamp & Rating**: Relative time (4:15 PM, Yesterday, Jan 19) • Star rating
  - If unrated, show no stars (not 0 stars)
- **Notes Preview**: First 50 characters of notes (if present), gray text

### Draft Session Card
```
┌─────────────────────────────────┐
│ 🍵 Sencha                    [DRAFT] │ ← Orange badge
│ Green Tea • 2:00 • 75°C         │
│ Started 3 hours ago             │ ← No rating (incomplete)
│ Forgot to finish logging...     │
└─────────────────────────────────┘
```

**Differences**:
- **Draft Badge**: Orange chip in top-right
- **Timestamp**: "Started X time ago" instead of completion time
- **No Rating**: Draft sessions don't have ratings yet
- **Tap Action**: Navigate to Log Tea screen in "finish draft" mode

### Multi-Steep Session Card
```
┌─────────────────────────────────┐
│ 🍵 Tie Guan Yin     [Steep 2/3] │ ← Steep indicator
│ Oolong • 1:45 • 95°C            │
│ 2:30 PM • ⭐⭐⭐⭐               │
│ Second steep was smoother       │
│ [View Session Group →]          │ ← Link to parent + all steeps
└─────────────────────────────────┘
```

**Differences**:
- **Steep Badge**: Blue chip showing "Steep N/Total"
- **Link**: "View Session Group" → Navigate to Session Detail showing parent + all steeps

**Group Display Options** (User Preference):
- **Show All Steeps**: Display each steep as individual card
- **Show Parent Only** (Default): Display only parent session with steep count
- **Group Steeps**: Display parent with expandable child list

---

## Empty States

### No Sessions Yet
```
┌─────────────────────────────────┐
│                                 │
│        ☕                       │
│                                 │
│   No Brewing Sessions Yet       │
│                                 │
│   Start tracking your tea       │
│   journey by logging your       │
│   first brew!                   │
│                                 │
│   [🍵 Log Tea]                 │
│                                 │
└─────────────────────────────────┘
```

### No Results (Filtered/Searched)
```
┌─────────────────────────────────┐
│                                 │
│        🔍                       │
│                                 │
│   No Sessions Found             │
│                                 │
│   Try adjusting your filters    │
│   or search terms.              │
│                                 │
│   [Clear Filters]               │
│                                 │
└─────────────────────────────────┘
```

---

## Actions & Interactions

### Swipe Actions (List View)
**Swipe Left** → Reveal action buttons:
```
┌─────────────────────────────────┐
│ Dragon Well  [♻️ Again] [✏️] [🗑️]│
└─────────────────────────────────┘
```

**Actions**:
1. **Brew Again** (♻️): Pre-fill Log Tea screen with this session's parameters
2. **Edit** (✏️): Navigate to Edit Session screen
3. **Delete** (🗑️): Show delete confirmation

**Delete Confirmation**:
```
┌─────────────────────────────────┐
│ Delete this session?            │
├─────────────────────────────────┤
│                                 │
│ This session will be removed    │
│ from your history.              │
│                                 │
│ This action cannot be undone.   │
│                                 │
│      [Cancel] [Delete]          │
└─────────────────────────────────┘
```

**Post-Delete**:
- Soft delete (set `deletedAt` timestamp)
- Animate card removal
- Show snackbar: "Session deleted" with UNDO (5s)
- UNDO → Clear `deletedAt`, restore session
- Stock NOT restored (stock was deducted when session completed)

### Multi-Select Mode
**Trigger**: Long press on any session card

**Behavior**:
1. Enter multi-select mode
2. Show checkboxes on all cards
3. Top app bar changes:
   ```
   ┌─────────────────────────────────┐
   │ ✕  3 Selected       [🗑️] [📤]  │ ← Cancel + Count + Actions
   └─────────────────────────────────┘
   ```
4. Tap cards to toggle selection
5. Bottom actions:
   - **Delete** (🗑️): Batch delete selected (with confirmation)
   - **Export** (📤): Export selected sessions to CSV/JSON

**Exit Multi-Select**:
- Tap ✕ → Deselect all, return to normal mode
- After action (delete/export) → Automatically exit

### Top Menu Actions (⋮)
**Options**:
1. **Change View** → List / Calendar / Compact
2. **Export All** → Export all (or filtered) sessions to CSV/JSON
3. **Sync Now** → Force Firebase sync (if enabled)

**Export Options**:
```
┌─────────────────────────────────┐
│ Export Sessions                 │
├─────────────────────────────────┤
│ Format:                         │
│ ○ CSV (spreadsheet)             │
│ ○ JSON (raw data)               │
│                                 │
│ Include:                        │
│ ☑ Tea details                   │
│ ☑ Brewing parameters            │
│ ☑ Ratings & notes               │
│ ☑ Photos (as file paths)        │
│                                 │
│       [Cancel] [Export]         │
└─────────────────────────────────┘
```

**CSV Format Example**:
```csv
Date,Time,Tea Name,Type,Brewing Time,Temperature,Water Amount,Rating,Notes,Steep
2026-01-20,16:15,Dragon Well,Green,2:30,80°C,200ml,5,Perfect steep!,1
2026-01-20,14:30,Tie Guan Yin,Oolong,1:45,95°C,150ml,4,Second steep,2/3
```

**Export Flow**:
1. User taps Export → Show options dialog
2. Select format and inclusions
3. Tap Export → Generate file
4. Platform share sheet (save to files, share via email, etc.)

---

## Use Cases Required

```kotlin
// Domain layer
GetAllSessionsUseCase
GetFilteredSessionsUseCase
SearchSessionsUseCase
DeleteSessionUseCase
BatchDeleteSessionsUseCase
ExportSessionsUseCase
GetSessionStatisticsUseCase

// Repository methods
suspend fun getAllSessions(): Flow<List<TeaSession>>
suspend fun getFilteredSessions(filters: SessionFilters): Flow<List<TeaSession>>
suspend fun searchSessions(query: String): Flow<List<TeaSession>>
suspend fun deleteSession(sessionId: String): Result<Unit>
suspend fun batchDeleteSessions(sessionIds: List<String>): Result<Unit>
suspend fun exportSessions(sessions: List<TeaSession>, format: ExportFormat): Result<String>
suspend fun getSessionStats(filters: SessionFilters): SessionStatistics
```

---

## MVI State Model

```kotlin
data class HistoryState(
    val viewMode: HistoryViewMode = HistoryViewMode.LIST,

    // Data
    val sessions: List<TeaSession> = emptyList(),
    val groupedSessions: Map<String, List<TeaSession>> = emptyMap(),  // For list view
    val calendarSessions: Map<LocalDate, List<TeaSession>> = emptyMap(),  // For calendar

    // Search & Filter
    val searchQuery: String = "",
    val filters: SessionFilters = SessionFilters(),
    val sortOption: SessionSortOption = SessionSortOption.NEWEST_FIRST,
    val activeFiltersCount: Int = 0,

    // Calendar state
    val selectedMonth: YearMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let { YearMonth(it.year, it.month) },
    val selectedDate: LocalDate? = null,

    // Multi-select
    val isMultiSelectMode: Boolean = false,
    val selectedSessionIds: Set<String> = emptySet(),

    // UI state
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showFilterSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val sessionToDelete: String? = null,

    // Statistics (for filter sheet counts)
    val teaTypeCounts: Map<String, Int> = emptyMap(),
    val vesselCounts: Map<String, Int> = emptyMap(),

    // Pagination
    val hasMore: Boolean = false,
    val page: Int = 0,

    // Snackbar
    val snackbarMessage: String? = null,
    val showUndoDelete: Boolean = false,
    val deletedSessionId: String? = null
)

enum class HistoryViewMode {
    LIST,       // Chronological list grouped by time
    CALENDAR,   // Monthly calendar view
    COMPACT     // Dense list without groupings
}

data class SessionFilters(
    val dateRange: DateRange = DateRange.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val teaTypeIds: Set<String> = emptySet(),  // Empty = all types
    val ratingFilter: RatingFilter = RatingFilter.ALL,
    val vesselIds: Set<String> = emptySet(),  // Empty = all vessels
    val temperatureRange: IntRange = 0..100,  // Celsius
    val includeCompleted: Boolean = true,
    val includeDraft: Boolean = true,
    val multiSteepFilter: MultiSteepFilter = MultiSteepFilter.ALL,
    val teaId: String? = null  // Filter by specific tea (from Tea Detail)
)

enum class DateRange {
    ALL_TIME,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    CUSTOM
}

enum class RatingFilter {
    ALL,
    FIVE_STARS,   // 5 only
    FOUR_PLUS,    // 4-5
    THREE_PLUS,   // 3-5
    UNRATED       // null rating
}

enum class MultiSteepFilter {
    ALL,                // All sessions
    SINGLE_ONLY,        // parentSessionId == null && steepNumber == 1
    MULTI_ONLY,         // parentSessionId != null || exists children
    PARENT_ONLY         // Show parent sessions, hide child steeps
}

enum class SessionSortOption {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_RATED,
    LOWEST_RATED,
    LONGEST_BREW,
    SHORTEST_BREW,
    TEA_NAME_ASC,
    TEA_NAME_DESC
}

enum class ExportFormat {
    CSV,
    JSON
}

data class SessionStatistics(
    val totalSessions: Int,
    val completedSessions: Int,
    val draftSessions: Int,
    val teaTypeCounts: Map<String, Int>,
    val vesselCounts: Map<String, Int>,
    val averageRating: Float?,
    val dateRange: Pair<Instant, Instant>?  // Earliest and latest session
)

sealed interface HistoryIntent {
    data object LoadSessions : HistoryIntent
    data object Refresh : HistoryIntent

    // View mode
    data class SetViewMode(val mode: HistoryViewMode) : HistoryIntent

    // Search & Filter
    data class Search(val query: String) : HistoryIntent
    data object ShowFilterSheet : HistoryIntent
    data object HideFilterSheet : HistoryIntent
    data class ApplyFilters(val filters: SessionFilters) : HistoryIntent
    data object ClearFilters : HistoryIntent
    data class SetSortOption(val option: SessionSortOption) : HistoryIntent

    // Calendar
    data class SelectMonth(val yearMonth: YearMonth) : HistoryIntent
    data class SelectDate(val date: LocalDate) : HistoryIntent

    // Session actions
    data class NavigateToSession(val sessionId: String) : HistoryIntent
    data class BrewAgain(val sessionId: String) : HistoryIntent  // Pre-fill Log Tea
    data class ShowDeleteConfirmation(val sessionId: String) : HistoryIntent
    data object HideDeleteConfirmation : HistoryIntent
    data object ConfirmDelete : HistoryIntent
    data object UndoDelete : HistoryIntent

    // Multi-select
    data class EnterMultiSelectMode(val sessionId: String) : HistoryIntent  // Long press
    data object ExitMultiSelectMode : HistoryIntent
    data class ToggleSessionSelection(val sessionId: String) : HistoryIntent
    data object SelectAllSessions : HistoryIntent
    data object DeselectAllSessions : HistoryIntent
    data object BatchDelete : HistoryIntent
    data object BatchExport : HistoryIntent

    // Export
    data object ShowExportDialog : HistoryIntent
    data object HideExportDialog : HistoryIntent
    data class ExportSessions(val format: ExportFormat, val includePhotos: Boolean) : HistoryIntent

    // Pagination
    data object LoadMore : HistoryIntent

    // Menu actions
    data object SyncNow : HistoryIntent

    // Snackbar
    data object DismissSnackbar : HistoryIntent
}
```

---

## Screen Behavior

### Initial Load
1. Emit `LoadSessions` intent
2. Show loading state (shimmer placeholders)
3. Load sessions with current filters (default: all sessions, newest first)
4. Group sessions by time period for list view:
   ```kotlin
   fun groupSessions(sessions: List<TeaSession>): Map<String, List<TeaSession>> {
       val now = Clock.System.now()
       val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

       return sessions.groupBy { session ->
           val sessionDate = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
           when {
               sessionDate == today -> "Today"
               sessionDate == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
               sessionDate >= today.minus(7, DateTimeUnit.DAY) -> "This Week"
               sessionDate >= today.minus(14, DateTimeUnit.DAY) -> "Last Week"
               sessionDate.month == today.month && sessionDate.year == today.year -> "This Month"
               else -> "${sessionDate.month.name} ${sessionDate.year}"  // "January 2026"
           }
       }
   }
   ```
5. Show results or empty state

### Search Flow
1. User types in search box → Emit `Search(query)` intent
2. Debounce 300ms
3. Filter sessions where:
   - Tea name contains query (case-insensitive)
   - Tea type contains query
   - Session notes contain query
4. Update display with matching sessions
5. If no matches, show "No results found" empty state

### Filter Flow
1. Tap Filter button → Emit `ShowFilterSheet` intent
2. Show filter sheet with current filter values pre-selected
3. User modifies filters (checkboxes, radio buttons, sliders)
4. Tap Apply → Emit `ApplyFilters(filters)` intent
5. Close sheet
6. Query database with filters:
   ```kotlin
   SELECT * FROM tea_sessions
   WHERE timestamp BETWEEN ? AND ?  -- Date range
   AND tea_id IN (SELECT id FROM teas WHERE type_id IN (?))  -- Tea types
   AND rating >= ?  -- Rating filter
   AND vessel_id IN (?)  -- Vessels
   AND temperature_celsius BETWEEN ? AND ?  -- Temperature range
   AND status IN (?, ?)  -- Completed, Draft
   AND (parentSessionId IS NULL OR ...)  -- Multi-steep filter
   ORDER BY ? -- Sort option
   ```
7. Update sessions list
8. Show active filter chips
9. Update filter count badge

### Calendar View Flow
1. Switch to calendar view → Emit `SetViewMode(CALENDAR)` intent
2. Load current month's sessions
3. Group sessions by date:
   ```kotlin
   val calendarSessions = sessions.groupBy {
       it.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
   }
   ```
4. Render calendar with dots on dates with sessions
5. Tap date → Emit `SelectDate(date)` intent
6. Show sessions for that date below calendar
7. Swipe month → Emit `SelectMonth(yearMonth)` intent
8. Load sessions for new month

### Delete Flow
**Single Delete**:
1. Swipe session left → Show delete button
2. Tap delete → Emit `ShowDeleteConfirmation(sessionId)` intent
3. Show confirmation dialog
4. Tap Delete → Emit `ConfirmDelete` intent
5. Soft delete session (set `deletedAt = now()`)
6. Animate card removal
7. Show snackbar with UNDO (5s)
8. If UNDO → Emit `UndoDelete` intent, clear `deletedAt`

**Batch Delete**:
1. Long press session → Emit `EnterMultiSelectMode(sessionId)` intent
2. Select multiple sessions
3. Tap delete icon → Emit `BatchDelete` intent
4. Show confirmation: "Delete X sessions?"
5. Confirm → Soft delete all selected
6. Exit multi-select mode
7. Show snackbar: "X sessions deleted" with UNDO

### Export Flow
1. Tap Export (menu or multi-select) → Emit `ShowExportDialog` intent
2. Show format selection dialog
3. Select CSV/JSON and options
4. Tap Export → Emit `ExportSessions(format, includePhotos)` intent
5. Generate file:
   - CSV: Convert sessions to CSV rows
   - JSON: Serialize sessions to JSON array
6. Save to temporary file
7. Open platform share sheet (save, email, etc.)
8. Show snackbar: "Exported X sessions"

### Pagination (Load More)
1. User scrolls to bottom of list
2. If `hasMore == true`, show "Load More" button
3. Tap or auto-load → Emit `LoadMore` intent
4. Increment `page`, fetch next batch (20 sessions)
5. Append to current list
6. Update `hasMore` based on response

---

## Edge Cases

1. **No sessions**: Show empty state with "Log Tea" button
2. **All sessions filtered out**: Show "No results" with "Clear Filters" button
3. **Draft sessions in history**: Show draft badge, allow finishing or deleting
4. **Multi-steep sessions**: Configurable display (all steeps, parent only, grouped)
5. **Deleted tea**: Session retains tea name (snapshot), show with archived indicator
6. **Very old sessions**: Group by year for efficient scrolling
7. **Rapid search typing**: Debounce to avoid excessive queries
8. **Export with no sessions**: Disable export button or show message
9. **Calendar month with 100+ sessions**: Show count, paginate day view
10. **Offline mode**: Show cached sessions, indicate sync status
11. **Failed delete**: Revert removal, show error message
12. **Photo paths broken**: Show placeholder, don't break card rendering

---

## Accessibility

- **Screen Reader**: Announce session count, filters, time groupings
- **Session Cards**: "Dragon Well, Green Tea, 2 minutes 30 seconds, 80 degrees, 5 stars"
- **Swipe Actions**: "Swipe left for actions: Brew Again, Edit, Delete"
- **Multi-Select**: "Multi-select mode active. 3 sessions selected."
- **Calendar**: "January 20, 3 sessions. Tap to view sessions."
- **Filter Sheet**: Clear labels, section headings, announce applied filters
- **Empty States**: Provide context and next action
- **Touch Targets**: Minimum 48dp for all interactive elements

---

## Performance Considerations

1. **Pagination**: Load sessions in batches (20-50 per page)
2. **Lazy Loading**: Use `LazyColumn` for efficient list rendering
3. **Image Caching**: Cache tea photo thumbnails, load asynchronously
4. **Database Queries**: Use indices on timestamp, teaId, typeId, rating
5. **Search Debouncing**: Wait 300ms after last keystroke before querying
6. **Calendar Rendering**: Only load visible month + adjacent months
7. **Filter Counts**: Cache type/vessel counts, recalculate only on data change
8. **Export**: Run on background thread, show progress indicator
9. **Memory**: Release resources when switching view modes
10. **Group Calculation**: Cache time period groupings, invalidate on date change

---

## Actions Summary

**Primary**:
- **View Session** → Navigate to Session Detail screen
- **Search** → Filter by tea name or notes
- **Filter** → Apply date, type, rating, vessel filters
- **Sort** → Change display order

**Secondary**:
- **Brew Again** → Pre-fill Log Tea with session parameters
- **Delete** → Soft delete with undo option
- **Export** → Export sessions to CSV/JSON
- **Change View** → Switch between List/Calendar/Compact

**Tertiary**:
- **Multi-Select** → Batch operations (delete, export)
- **Sync Now** → Force Firebase sync

### 8. Session Detail Screen
**Purpose**: View comprehensive details of a completed brewing session, including all steeps for multi-steep sessions.

**Navigation**:
- From History screen (tap session card)
- From Tea Detail "Recent Sessions" (tap session)
- From Home screen "Recent Sessions" (tap session)

**Important**: Draft sessions do NOT have a detail screen. Tapping a draft session redirects to Log Tea screen to finish logging.

```
┌─────────────────────────────────┐
│ ← Session                  [⋮]  │ ← Back + Menu (Edit/Delete/Brew Again)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ │   [Photo Gallery]           │ │ ← Swipeable photos
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ 🍵 Dragon Well                  │ ← Tea name (linked)
│ Green Tea                       │ ← Type (read-only)
│                                 │
│ ⭐⭐⭐⭐⭐ (5.0)                │ ← Overall rating
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BREWING TIMELINE                │
│                                 │
│ ┌─ Steep 1 ───────────────────┐│
│ │ Jan 20, 2026 • 4:15 PM      ││ ← Date/time
│ │                             ││
│ │ Duration      2:30          ││
│ │ Temperature   80°C / 176°F  ││
│ │ Tea Amount    3g            ││
│ │ Water Amount  200ml / 7oz   ││
│ │                             ││
│ │ Notes: Perfect steep!       ││
│ │ 📷 [2 photos]               ││
│ │                      [✏️] [🗑️]││ ← Edit/Delete this steep
│ └─────────────────────────────┘│
│                                 │
│ ┌─ Steep 2 ───────────────────┐│
│ │ Jan 20, 2026 • 4:18 PM      ││
│ │                             ││
│ │ Duration      1:45          ││
│ │ Temperature   85°C / 185°F  ││
│ │ Tea Amount    (same)        ││
│ │ Water Amount  200ml / 7oz   ││
│ │                             ││
│ │ Notes: Second steep smoother││
│ │ 📷 [1 photo]                ││
│ │                      [✏️] [🗑️]││
│ └─────────────────────────────┘│
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ SESSION DETAILS                 │
│                                 │
│ Vessel        Gaiwan            │
│ Water Type    Filtered          │
│ Location      Home              │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ [🍵 Brew Again]                 │ ← Primary action button
│                                 │
└─────────────────────────────────┘
```

---

## Components

### 1. Photo Gallery
**Display**:
- Horizontal swipeable carousel showing all photos from all steeps
- Page indicator (1/5, 2/5, etc.)
- Photos grouped by steep (visual separator or label)
- Tap to view full-screen

**Photo Management**:
- Tap + button to add more photos (assign to specific steep)
- Long press + drag to reorder photos
- Tap ✕ to delete photo
- Full-screen view with pinch-to-zoom

**Editable**: Yes - users can add, reorder, and delete photos after session completion

### 2. Tea Information Section
**Components**:
- **Tea Name** (with icon/photo): Linked to Tea Detail screen
- **Tea Type**: Read-only, displayed with color indicator
- **Overall Rating**: Session-level rating (only on parent session)
  - Displayed as stars + numeric value
  - Tap to edit rating

**Note**: Tea selection cannot be changed after session is saved (type is read-only)

### 3. Brewing Timeline
**Purpose**: Show all steeps in chronological order for multi-steep sessions

**Layout**: Vertical timeline/list of steep cards

**Single-Steep Session**:
- Shows one steep card labeled "Session Details" (no steep number)

**Multi-Steep Session**:
- Parent steep labeled "Steep 1"
- Child steeps labeled "Steep 2", "Steep 3", etc.
- Visual timeline connector between steeps

**Steep Card Contents**:
```
┌─ Steep N ─────────────────────┐
│ Date/Time: Jan 20, 2026 4:15 PM│
│                               │
│ Duration      2:30            │
│ Temperature   80°C / 176°F    │
│ Tea Amount    3g              │
│ Water Amount  200ml / 7oz     │
│                               │
│ Notes: [steep-specific notes] │
│ 📷 [N photos]                 │
│                        [✏️][🗑️]│
└───────────────────────────────┘
```

**Per-Steep Fields** (can vary between steeps):
- Date/time (timestamp)
- Duration (brewing time)
- Temperature (in user's preferred unit)
- Tea amount (grams) - shows "(same)" if unchanged from parent
- Water amount (in user's preferred unit)
- Notes (steep-specific)
- Photos (steep-specific)

**Actions Per Steep**:
- **Edit** (✏️): Edit this steep's parameters
- **Delete** (🗑️): Delete this steep with confirmation

### 4. Session Details Section
**Session-Level Fields** (shared across all steeps):
- **Vessel**: Brewing vessel name
- **Water Type**: Filtered, Tap, Spring, etc.
- **Location**: Optional text field (e.g., "Home", "Tea shop")
- **Sync Status**: Small cloud icon indicator (synced, pending, error)

**Display**:
- Simple 2-column layout
- Labels on left, values on right
- Sync status as small icon next to location

### 5. Primary Action Button
```
┌─────────────────────────────┐
│    [🍵 Brew Again]          │ ← Full-width button
└─────────────────────────────┘
```

**Behavior**:
- Pre-fills Log Tea screen with this session's parameters
- If multi-steep, pre-fills with Steep 1 parameters
- User can modify and start timer or log retrospectively

---

## Top App Bar Menu (⋮)

**Options**:
1. **Edit Session** → Navigate to Edit Session screen
2. **Delete Session** → Show confirmation dialog
3. **Brew Again** → Pre-fill Log Tea screen (same as button)

**Future Enhancement** (deprioritized):
- **Share** → Export session details as text or image

**Delete Confirmation**:
```
┌─────────────────────────────┐
│ Delete Session?             │
├─────────────────────────────┤
│ This will delete this       │
│ brewing session.            │
│                             │
│ [Show steep details]        │ ← If multi-steep
│                             │
│  [Cancel] [Delete]          │
└─────────────────────────────┘
```

**Multi-Steep Delete Confirmation**:
```
┌─────────────────────────────┐
│ Delete Steep 1?             │
├─────────────────────────────┤
│ This is the parent steep.   │
│                             │
│ Deleting it will also       │
│ delete all subsequent       │
│ steeps (Steep 2, Steep 3).  │
│                             │
│ This action cannot be       │
│ undone.                     │
│                             │
│  [Cancel] [Delete All]      │
└─────────────────────────────┘
```

**Post-Delete**:
- Soft delete (set `deletedAt` timestamp)
- Navigate back to History screen
- Show snackbar: "Session deleted" with UNDO (5s)
- UNDO → Clear `deletedAt`, restore session
- Stock NOT restored (was already deducted)

---

## Edit Session Screen

**Navigation**: From Session Detail menu or steep-level Edit button

**Mode Detection**:
- **Edit Session** (`sessionId` passed): Edit all session-level and steep-level fields
- **Edit Steep** (`sessionId` + `steepNumber` passed): Edit only that steep's fields

**Editable Fields**:

**Session-Level** (affects all steeps):
- Rating
- Vessel
- Water Type
- Location
- Photos (can add/remove/reorder across all steeps)

**Per-Steep** (only affects selected steep):
- Duration (brewing time)
- Temperature
- Tea amount
- Water amount
- Notes

**Non-Editable**:
- ❌ Tea selection (locked once session is saved)
- ❌ Timestamp (can't change when the steep was brewed)
- ❌ Steep number or parent relationship

**Layout**: Similar to Log Tea screen but in edit mode
- All fields pre-filled
- Tea selection dropdown disabled (grayed out)
- Save button enabled when changes detected
- Cancel with discard confirmation if unsaved changes

---

## Use Cases Required

```kotlin
// Domain layer
GetSessionByIdUseCase
GetSessionWithAllSteepsUseCase
UpdateSessionUseCase
DeleteSessionUseCase
DeleteSteepUseCase
AddPhotosToSessionUseCase
RemovePhotoFromSessionUseCase
ReorderSessionPhotosUseCase

// Repository methods
suspend fun getSessionById(id: String): Flow<TeaSession?>
suspend fun getSessionWithSteeps(parentId: String): Flow<SessionGroup>
suspend fun updateSession(session: TeaSession): Result<Unit>
suspend fun deleteSession(sessionId: String): Result<Unit>
suspend fun deleteSteep(sessionId: String, cascadeChildren: Boolean): Result<Unit>
suspend fun addPhotos(sessionId: String, photos: List<String>): Result<Unit>
suspend fun removePhoto(sessionId: String, photoUri: String): Result<Unit>
suspend fun reorderPhotos(sessionId: String, newOrder: List<String>): Result<Unit>

data class SessionGroup(
    val parent: TeaSession,
    val steeps: List<TeaSession>  // Ordered by steepNumber
)
```

---

## MVI State Model

```kotlin
data class SessionDetailState(
    val sessionId: String,
    val session: TeaSession? = null,
    val sessionGroup: SessionGroup? = null,  // For multi-steep sessions
    val tea: Tea? = null,

    // Photo management
    val allPhotos: List<SessionPhoto> = emptyList(),  // Photos from all steeps
    val showPhotoGallery: Boolean = false,
    val selectedPhotoIndex: Int = 0,

    // UI state
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val deleteTarget: DeleteTarget? = null,  // Which steep to delete
    val showPhotoPicker: Boolean = false,
    val assignPhotoToSteep: Int? = null,  // Which steep to assign new photo to

    // Snackbar
    val snackbarMessage: String? = null,
    val showUndoDelete: Boolean = false
)

data class SessionPhoto(
    val uri: String,
    val steepNumber: Int,
    val timestamp: Instant
)

sealed class DeleteTarget {
    data class EntireSession(val sessionId: String) : DeleteTarget()
    data class SingleSteep(
        val sessionId: String,
        val steepNumber: Int,
        val willCascade: Boolean  // True if deleting parent steep
    ) : DeleteTarget()
}

sealed interface SessionDetailIntent {
    data class LoadSession(val sessionId: String) : SessionDetailIntent
    data object Refresh : SessionDetailIntent

    // Navigation
    data object NavigateToTea : SessionDetailIntent
    data object NavigateToEditSession : SessionDetailIntent
    data class NavigateToEditSteep(val steepNumber: Int) : SessionDetailIntent
    data object NavigateToBrewAgain : SessionDetailIntent

    // Rating
    data class UpdateRating(val rating: Float) : SessionDetailIntent

    // Photo management
    data object ShowPhotoGallery : SessionDetailIntent
    data object HidePhotoGallery : SessionDetailIntent
    data class SelectPhoto(val index: Int) : SessionDetailIntent
    data class ShowPhotoPicker(val steepNumber: Int) : SessionDetailIntent
    data class AddPhoto(val uri: String, val steepNumber: Int) : SessionDetailIntent
    data class RemovePhoto(val photoUri: String) : SessionDetailIntent
    data class ReorderPhotos(val fromIndex: Int, val toIndex: Int) : SessionDetailIntent

    // Delete
    data object ShowDeleteSessionConfirmation : SessionDetailIntent
    data class ShowDeleteSteepConfirmation(val steepNumber: Int) : SessionDetailIntent
    data object HideDeleteConfirmation : SessionDetailIntent
    data object ConfirmDelete : SessionDetailIntent
    data object UndoDelete : SessionDetailIntent

    // Snackbar
    data object DismissSnackbar : SessionDetailIntent
}
```

---

## Screen Behavior

### Initial Load

**For Completed Sessions**:
1. Receive `sessionId` as navigation argument
2. Emit `LoadSession` intent
3. Show loading state (shimmer placeholders)
4. Determine if single or multi-steep:
   - Check if session has `parentSessionId == null && exists children`
   - If single steep → Load single session
   - If multi-steep → Load parent + all child steeps
5. Load related tea details
6. Group photos by steep
7. Display all information

**For Draft Sessions** (intercepted before reaching this screen):
1. Check session status in History screen or navigation layer
2. If `status == DRAFT` → Redirect to Log Tea screen with `sessionId`
3. Show message: "Finish logging this session"
4. Session Detail screen never loads for drafts

### Multi-Steep Display

**Timeline Construction**:
1. Query database for parent session and all children:
   ```kotlin
   val parent = getSessionById(sessionId)
   val children = getSessionsBySteepNumber(parentId = parent.id)
   val allSteeps = listOf(parent) + children.sortedBy { it.steepNumber }
   ```
2. Render timeline with visual connectors
3. Show steep-specific values, "(same)" for unchanged values

### Photo Management Flow

**View Photos**:
1. Tap gallery → Emit `ShowPhotoGallery` intent
2. Show full-screen carousel with all photos from all steeps
3. Photos grouped/labeled by steep
4. Swipe to navigate, pinch to zoom

**Add Photo**:
1. Tap + button → Show steep selector if multi-steep:
   ```
   ┌─────────────────────────────┐
   │ Add Photo to Which Steep?   │
   ├─────────────────────────────┤
   │ ○ Steep 1 (4:15 PM)        │
   │ ○ Steep 2 (4:18 PM)        │
   │ ○ Steep 3 (4:21 PM)        │
   │                             │
   │      [Cancel] [Select]      │
   └─────────────────────────────┘
   ```
2. Select steep → Emit `ShowPhotoPicker(steepNumber)` intent
3. Platform photo picker opens
4. Select photo → Emit `AddPhoto(uri, steepNumber)` intent
5. Compress and add to session
6. Update photo gallery

**Delete Photo**:
1. Tap ✕ on photo → Show confirmation:
   ```
   ┌─────────────────────────────┐
   │ Delete this photo?          │
   │                             │
   │  [Cancel] [Delete]          │
   └─────────────────────────────┘
   ```
2. Confirm → Emit `RemovePhoto(uri)` intent
3. Remove from session, update gallery

**Reorder Photos**:
1. Long press photo → Enter reorder mode
2. Drag to new position
3. Release → Emit `ReorderPhotos(from, to)` intent
4. Update photo order in database

### Delete Steep Flow

**Individual Steep (Not Parent)**:
1. Tap 🗑️ on steep card → Show confirmation:
   ```
   ┌─────────────────────────────┐
   │ Delete Steep 2?             │
   ├─────────────────────────────┤
   │ This will remove this steep │
   │ from the session.           │
   │                             │
   │ Steep 3 will remain.        │
   │                             │
   │  [Cancel] [Delete]          │
   └─────────────────────────────┘
   ```
2. Confirm → Soft delete steep
3. Update timeline, remove steep card
4. Show snackbar with UNDO

**Parent Steep (Cascade Delete)**:
1. Tap 🗑️ on Steep 1 → Show cascade warning (see mockup above)
2. Explain that all subsequent steeps will be deleted
3. Confirm → Delete parent + all children (cascade)
4. Navigate back to History
5. Show snackbar with UNDO

**Entire Session**:
1. Tap Delete in menu → Show confirmation
2. If single steep: Simple confirmation
3. If multi-steep: Show "This will delete all N steeps"
4. Confirm → Delete entire session group
5. Navigate back to History

### Edit Flow

**Edit Entire Session**:
1. Tap Edit in menu → Navigate to Edit Session screen
2. Show all editable fields (rating, vessel, water type, location, photos)
3. Show all steeps with editable per-steep fields
4. Save → Update database, return to Detail screen

**Edit Single Steep**:
1. Tap ✏️ on steep card → Navigate to Edit Session screen focused on that steep
2. Show only that steep's editable fields
3. Session-level fields also editable
4. Save → Update database, return to Detail screen

---

## Edge Cases

1. **Session deleted while viewing**: Show error, navigate back
2. **Tea deleted after session saved**: Session retains tea name (snapshot), link disabled
3. **Photos fail to load**: Show placeholder, allow retry or removal
4. **All steeps deleted**: Should not be possible (UI prevents deleting last steep)
5. **Editing parent steep after children exist**: Validate changes don't conflict
6. **Very long notes**: Expandable with "Show more" button
7. **No photos**: Show placeholder or hide photo gallery section
8. **Offline mode**: Show cached data, indicate sync pending
9. **Multiple users editing same session**: Last write wins (with sync conflict warning)
10. **Invalid steep relationships**: Detect and repair (or show error)

---

## Accessibility

- **Screen Reader**: Announce session details, steep count, actions
- **Timeline**: "Steep 1 of 3, brewed at 4:15 PM for 2 minutes 30 seconds"
- **Rating**: "5 stars out of 5, double tap to edit"
- **Photos**: "Photo 1 of 5, double tap to view full screen"
- **Delete**: Confirm action with clear warnings about cascade effects
- **Touch Targets**: Minimum 48dp for all buttons
- **High Contrast**: Ensure timeline connectors and steep cards readable

---

## Performance Considerations

1. **Photo Loading**: Lazy load thumbnails, full-res only in gallery
2. **Multi-Steep Query**: Single optimized query with JOIN for parent + children
3. **Timeline Rendering**: Efficient list view (LazyColumn) for many steeps
4. **Photo Gallery**: Use paging for large photo collections
5. **Database Queries**: Use indices on sessionId, parentSessionId, steepNumber
6. **Memory**: Release photo resources when gallery closed

---

## Actions Summary

**Primary**:
- **Brew Again** → Pre-fill Log Tea with this session's parameters

**Secondary**:
- **Edit Session** → Edit all session and steep fields (except tea type)
- **Edit Steep** → Edit individual steep's parameters
- **Delete Session** → Soft delete entire session with undo
- **Delete Steep** → Soft delete individual steep (or cascade if parent)
- **Update Rating** → Edit session rating

**Tertiary**:
- **View Tea** → Navigate to Tea Detail screen
- **Add Photos** → Add more photos after session completion
- **Reorder Photos** → Change photo order
- **Delete Photo** → Remove photo from session
- **View Full Photo** → Full-screen photo gallery with zoom

### 9. Analytics Screen
**Purpose**: Visualize brewing patterns and statistics over time with auto-generated insights.

**Navigation**: Accessed from "More" tab in bottom navigation.

**Minimum Data**: Requires at least 10 completed sessions to display analytics. Shows empty state otherwise.

```
┌─────────────────────────────────┐
│ Analytics            [⋮]        │ ← Menu (Export)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ ⌄ This Month            [▼] │ │ ← Period selector (dropdown)
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ SUMMARY                         │
│                                 │
│ ┌──────────┬──────────┐         │
│ │    45    │  3h 45m  │         │ ← Stats cards (2x2 grid)
│ │ Sessions │Total Time│         │
│ ├──────────┼──────────┤         │
│ │  9.5L    │    15    │         │
│ │Water Used│This Month│         │
│ └──────────┴──────────┘         │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ INSIGHTS                        │
│                                 │
│ 💡 You brewed 15% more this    │
│    month compared to last month│
│                                 │
│ 🍵 Dragon Well is your most    │
│    brewed tea (12 sessions)    │
│                                 │
│ ⭐ Your average rating this    │
│    month: 4.5/5                │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ BREWING TRENDS                  │
│                                 │
│ ┌─────────────────────────────┐│
│ │        [Line Chart]         ││ ← Sessions over time
│ │   📈                        ││
│ │                             ││
│ └─────────────────────────────┘│
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ TEA TYPE DISTRIBUTION           │
│                                 │
│ ┌─────────────────────────────┐│
│ │     [Pie Chart]             ││ ← Tap slice to filter
│ │  🥧                          ││
│ │  Green 45% • Black 30%      ││
│ │  Oolong 15% • Other 10%     ││
│ └─────────────────────────────┘│
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ FAVORITE TEAS                   │
│                                 │
│ ┌─────────────────────────────┐│
│ │ Dragon Well      ████████ 12││ ← Horizontal bars
│ │ Sencha          ██████ 8    ││   Tap to filter
│ │ Earl Grey       ████ 6      ││
│ │ Tie Guan Yin    ███ 5       ││
│ │ Silver Needle   ██ 4        ││
│ └─────────────────────────────┘│
│                                 │
└─────────────────────────────────┘
```

---

## Components

### 1. Time Period Selector
**Layout**: Dropdown button at top of screen

**Quick Options**:
- Last 7 Days
- Last 30 Days
- Last 90 Days
- This Week (Mon-Sun)
- This Month
- Last Month
- This Year
- All Time
- **Custom Range** → Opens date range picker

**Custom Range Picker**:
```
┌─────────────────────────────┐
│ Select Date Range           │
├─────────────────────────────┤
│ From:  [Jan 1, 2026  📅]   │
│ To:    [Jan 31, 2026 📅]   │
│                             │
│      [Cancel] [Apply]       │
└─────────────────────────────┘
```

**Behavior**:
- Selected period persists across app sessions
- Default: "This Month"
- Shows date range in selector button (e.g., "Jan 1-31, 2026")
- All statistics and charts update when period changes

### 2. Summary Cards (2x2 Grid)

**Card 1: Total Sessions**
- Count of completed sessions in period (excludes drafts)
- Change indicator vs previous period (e.g., "+5 from last month")

**Card 2: Total Brewing Time**
- Sum of all brewing durations in period
- Formatted as "Xh Ym" (e.g., "3h 45m")

**Card 3: Water Used**
- Sum of all `waterQuantityMl` from sessions in period
- Displayed in user's preferred unit (liters/gallons)
- Equivalent to "tea consumed"

**Card 4: Sessions This Period**
- Duplicate of Card 1 OR
- Could show "Different Teas Tried" (unique tea count)

**Display**:
- Clean card design with large numbers
- Icon or emoji for each metric
- Subtle comparison to previous period where relevant

### 3. Insights Section

**Purpose**: Auto-generated text insights based on data analysis

**Insight Types**:

1. **Period Comparison**:
   - "You brewed 15% more this month compared to last month"
   - "Your brewing activity decreased by 20% this week"
   - Only show if meaningful change (>10%)

2. **Most Brewed Tea**:
   - "Dragon Well is your most brewed tea (12 sessions)"
   - Always show if data available

3. **Average Rating**:
   - "Your average rating this month: 4.5/5"
   - Only show if at least 5 rated sessions

4. **Consistency**:
   - "You've logged sessions 6 days this week!"
   - "New personal record: 3 sessions in one day"

5. **Tea Variety**:
   - "You tried 4 new teas this month"
   - Only show if new teas added in period

6. **Brewing Patterns**:
   - "You brew most often in the morning (60% before noon)"
   - "Weekends are your favorite time to brew tea"

**Display**:
- 2-4 insights shown at a time
- Icon/emoji next to each insight
- Tap insight → Navigate to related data (if applicable)
- Refreshes when period changes

**Empty State** (not enough data for insights):
```
💡 Keep brewing to unlock insights!
   Log more sessions to see patterns
   and trends in your tea journey.
```

### 4. Brewing Trends Chart (Line Chart)

**Purpose**: Show session count over time

**X-Axis**:
- Last 7 Days: Daily
- Last 30/90 Days: Daily (may need to aggregate)
- This Month: Daily
- This Year: Monthly
- All Time: Monthly or yearly (adaptive)

**Y-Axis**: Number of sessions

**Interaction**:
- Tap data point → Navigate to History filtered to that day/period
- Pinch to zoom (optional)
- Swipe to pan through longer timelines

**Display**:
- Clean line graph with gridlines
- Highlight today/current period
- Show trend line (moving average) optionally

### 5. Tea Type Distribution (Pie Chart)

**Purpose**: Show percentage breakdown of sessions by tea type

**Data**: Count of sessions per tea type in selected period

**Display**:
- Pie chart with distinct colors per type
- Legend showing percentages and counts
- Largest slice highlighted
- Top 4-5 types shown, rest grouped as "Other"

**Interaction**:
- Tap slice → Navigate to History filtered by that tea type
- Shows tooltip with exact count on tap/hover

**Empty State** (only 1 type):
```
🍵 All your sessions used [Type]
   Try exploring different tea types!
```

### 6. Favorite Teas Chart (Horizontal Bar Chart)

**Purpose**: Show top 5 most-brewed teas

**Data**: Count of sessions per tea, sorted DESC, top 5

**Display**:
- Horizontal bars with tea names on left
- Session count on right
- Bars colored by tea type
- Optional: Show tea photo thumbnail

**Interaction**:
- Tap bar → Navigate to History filtered by that tea
- Tap tea name → Navigate to Tea Detail screen

**Empty State** (less than 5 teas):
```
Shows all teas with sessions
Keep exploring to build your
favorites list!
```

---

## Top Menu Actions (⋮)

**Options**:
1. **Export Statistics** → Export current period's stats to CSV
2. **Sync Now** → Force Firebase sync (if enabled)

**Future Enhancement** (deprioritized):
- **Share Charts** → Export charts as images

**Export Statistics**:
```
┌─────────────────────────────┐
│ Export Analytics            │
├─────────────────────────────┤
│ Period: This Month          │
│ (Jan 1-31, 2026)           │
│                             │
│ Include:                    │
│ ☑ Summary statistics        │
│ ☑ Tea type breakdown        │
│ ☑ Favorite teas list        │
│ ☑ Brewing trends data       │
│                             │
│      [Cancel] [Export]      │
└─────────────────────────────┘
```

**CSV Format Example**:
```csv
Analytics Export - January 2026
Generated: 2026-01-31

Summary Statistics
Total Sessions,45
Total Brewing Time,3h 45m
Water Used,9.5L
Average Rating,4.5

Tea Type Distribution
Type,Sessions,Percentage
Green,20,44%
Black,14,31%
Oolong,7,16%
White,4,9%

Favorite Teas
Tea Name,Sessions
Dragon Well,12
Sencha,8
Earl Grey,6
```

**Export Flow**:
1. Tap Export → Show options dialog
2. Select inclusions
3. Generate CSV file
4. Platform share sheet (save, email, etc.)

---

## Empty State (< 10 Sessions)

```
┌─────────────────────────────┐
│                             │
│         📊                  │
│                             │
│   Not Enough Data Yet       │
│                             │
│   You need at least 10      │
│   completed sessions to     │
│   see analytics.            │
│                             │
│   Current: 3 sessions       │
│                             │
│   [🍵 Log Tea]             │
│                             │
└─────────────────────────────┘
```

**Display**:
- Shows current session count
- Clear call-to-action to log more sessions
- Friendly, encouraging tone

---

## Use Cases Required

```kotlin
// Domain layer
GetAnalyticsForPeriodUseCase
GenerateInsightsUseCase
ExportAnalyticsUseCase
GetBrewingTrendsUseCase
GetTeaTypeDistributionUseCase
GetFavoriteTeasUseCase

// Repository methods
suspend fun getSessionStatistics(period: DateRange): AnalyticsData
suspend fun getBrewingTrends(period: DateRange): List<TrendPoint>
suspend fun getTeaTypeDistribution(period: DateRange): Map<TeaType, Int>
suspend fun getFavoriteTeas(period: DateRange, limit: Int): List<TeaWithCount>
suspend fun generateInsights(period: DateRange): List<Insight>

data class AnalyticsData(
    val totalSessions: Int,
    val totalBrewingTime: Duration,
    val totalWaterMl: Int,
    val averageRating: Float?,
    val uniqueTeas: Int,
    val comparisonToPrevious: PeriodComparison?
)

data class TrendPoint(
    val date: LocalDate,
    val sessionCount: Int
)

data class TeaWithCount(
    val tea: Tea,
    val sessionCount: Int
)

data class Insight(
    val type: InsightType,
    val text: String,
    val icon: String,
    val action: InsightAction?
)

enum class InsightType {
    PERIOD_COMPARISON,
    MOST_BREWED,
    AVERAGE_RATING,
    CONSISTENCY,
    VARIETY,
    BREWING_PATTERN
}

sealed class InsightAction {
    data class NavigateToHistory(val filter: SessionFilters) : InsightAction()
    data class NavigateToTea(val teaId: String) : InsightAction()
}
```

---

## MVI State Model

```kotlin
data class AnalyticsState(
    val selectedPeriod: DatePeriod = DatePeriod.THIS_MONTH,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,

    // Data
    val analytics: AnalyticsData? = null,
    val brewingTrends: List<TrendPoint> = emptyList(),
    val teaTypeDistribution: Map<TeaType, Int> = emptyMap(),
    val favoriteTeas: List<TeaWithCount> = emptyList(),
    val insights: List<Insight> = emptyList(),

    // UI state
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasMinimumData: Boolean = false,  // At least 10 sessions
    val showPeriodSelector: Boolean = false,
    val showDateRangePicker: Boolean = false,
    val showExportDialog: Boolean = false
)

enum class DatePeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

sealed interface AnalyticsIntent {
    data object LoadAnalytics : AnalyticsIntent
    data object Refresh : AnalyticsIntent

    // Period selection
    data object ShowPeriodSelector : AnalyticsIntent
    data object HidePeriodSelector : AnalyticsIntent
    data class SelectPeriod(val period: DatePeriod) : AnalyticsIntent
    data object ShowDateRangePicker : AnalyticsIntent
    data object HideDateRangePicker : AnalyticsIntent
    data class SelectCustomRange(val start: LocalDate, val end: LocalDate) : AnalyticsIntent

    // Chart interactions
    data class TapTrendPoint(val date: LocalDate) : AnalyticsIntent
    data class TapTeaType(val typeId: String) : AnalyticsIntent
    data class TapFavoriteTea(val teaId: String) : AnalyticsIntent
    data class TapInsight(val insight: Insight) : AnalyticsIntent

    // Export
    data object ShowExportDialog : AnalyticsIntent
    data object HideExportDialog : AnalyticsIntent
    data class ExportAnalytics(val inclusions: Set<ExportInclusion>) : AnalyticsIntent

    // Navigation
    data object NavigateToLogTea : AnalyticsIntent
}

enum class ExportInclusion {
    SUMMARY,
    TEA_TYPE_BREAKDOWN,
    FAVORITE_TEAS,
    BREWING_TRENDS
}
```

---

## Screen Behavior

### Initial Load

1. Check total session count
2. If < 10 sessions → Show empty state
3. If ≥ 10 sessions:
   - Load selected period (default: This Month)
   - Fetch analytics data in parallel:
     ```kotlin
     combine(
         getSessionStatistics(period),
         getBrewingTrends(period),
         getTeaTypeDistribution(period),
         getFavoriteTeas(period, limit = 5),
         generateInsights(period)
     ) { stats, trends, distribution, favorites, insights ->
         AnalyticsState(
             analytics = stats,
             brewingTrends = trends,
             teaTypeDistribution = distribution,
             favoriteTeas = favorites,
             insights = insights,
             hasMinimumData = true,
             isLoading = false
         )
     }
     ```
4. Render all components

### Period Change Flow

1. User selects new period from dropdown
2. Emit `SelectPeriod(period)` intent
3. Show loading indicator (optional, keep existing data visible)
4. Fetch new analytics data for period
5. Update all charts and insights
6. Persist selected period to preferences

**Custom Range**:
1. User selects "Custom Range" → Emit `ShowDateRangePicker` intent
2. Show date range picker dialog
3. User selects start and end dates
4. Tap Apply → Emit `SelectCustomRange(start, end)` intent
5. Fetch analytics for custom range
6. Update period selector to show "Jan 1-31, 2026"

### Chart Interaction Flow

**Tap Trend Point**:
1. User taps point on line chart (e.g., Jan 15)
2. Emit `TapTrendPoint(date)` intent
3. Navigate to History screen with date filter applied:
   ```kotlin
   navigateToHistory(
       filters = SessionFilters(
           dateRange = DateRange.CUSTOM,
           customStartDate = date,
           customEndDate = date
       )
   )
   ```

**Tap Tea Type Slice**:
1. User taps "Green" slice in pie chart
2. Emit `TapTeaType(typeId)` intent
3. Navigate to History with tea type filter

**Tap Favorite Tea Bar**:
1. User taps "Dragon Well" bar
2. Emit `TapFavoriteTea(teaId)` intent
3. Navigate to Tea Detail screen (not History)

**Tap Insight**:
1. User taps insight with action
2. Execute associated action (navigate to History or Tea Detail)

### Insight Generation Algorithm

```kotlin
fun generateInsights(data: AnalyticsData, period: DatePeriod): List<Insight> {
    val insights = mutableListOf<Insight>()

    // 1. Period comparison (if previous period data available)
    data.comparisonToPrevious?.let { comparison ->
        if (abs(comparison.percentageChange) > 10) {
            val verb = if (comparison.percentageChange > 0) "more" else "less"
            insights.add(Insight(
                type = PERIOD_COMPARISON,
                text = "You brewed ${abs(comparison.percentageChange)}% $verb this ${period.name} compared to last ${period.name}",
                icon = if (comparison.percentageChange > 0) "📈" else "📉"
            ))
        }
    }

    // 2. Most brewed tea
    data.mostBrewedTea?.let { tea ->
        insights.add(Insight(
            type = MOST_BREWED,
            text = "${tea.name} is your most brewed tea (${tea.count} sessions)",
            icon = "🍵",
            action = NavigateToTea(tea.id)
        ))
    }

    // 3. Average rating
    if (data.ratedSessionsCount >= 5) {
        insights.add(Insight(
            type = AVERAGE_RATING,
            text = "Your average rating this ${period.name}: ${data.averageRating}/5",
            icon = "⭐"
        ))
    }

    // 4. Consistency
    if (data.sessionDaysCount >= period.totalDays * 0.5) {
        insights.add(Insight(
            type = CONSISTENCY,
            text = "You've logged sessions ${data.sessionDaysCount} days this ${period.name}!",
            icon = "🔥"
        ))
    }

    // 5. Variety
    if (data.newTeasCount > 0) {
        insights.add(Insight(
            type = VARIETY,
            text = "You tried ${data.newTeasCount} new teas this ${period.name}",
            icon = "✨"
        ))
    }

    // Return top 4 insights
    return insights.take(4)
}
```

### Export Flow

1. Tap Export in menu → Emit `ShowExportDialog` intent
2. Show export options dialog
3. Select inclusions (all selected by default)
4. Tap Export → Emit `ExportAnalytics(inclusions)` intent
5. Generate CSV file with selected data
6. Platform share sheet
7. Show snackbar: "Analytics exported"

---

## Edge Cases

1. **Not enough data**: Show empty state with session count
2. **Only 1 tea type**: Pie chart shows 100%, encourage variety
3. **No rated sessions**: Skip rating insight
4. **Custom range > 1 year**: Aggregate trends by month instead of day
5. **Period with 0 sessions**: Show "No sessions in this period" message
6. **Previous period unavailable**: Skip comparison insight
7. **Very long tea names**: Truncate in favorite teas chart
8. **Charts fail to render**: Show error state with refresh option
9. **Export fails**: Show error message, allow retry

---

## Accessibility

- **Screen Reader**: Announce statistics, chart descriptions, insights
- **Charts**: Provide data table alternatives for screen readers
- **Period Selector**: "Currently showing This Month, January 2026"
- **Insights**: Read full insight text with associated icon
- **Summary Cards**: "Total sessions: 45, increased by 5 from last month"
- **Interactive Charts**: "Tap to filter history by this category"

---

## Performance Considerations

1. **Data Aggregation**: Compute statistics efficiently with database queries
2. **Chart Rendering**: Use lightweight charting library (e.g., Vico for Compose)
3. **Caching**: Cache analytics data for current period, invalidate on new session
4. **Background Calculation**: Generate insights on background thread
5. **Lazy Loading**: Load charts as user scrolls
6. **Debounce**: Debounce period changes to avoid excessive queries

---

## Actions Summary

**Primary**:
- **Change Period** → Update all analytics for selected time range
- **Tap Chart** → Navigate to History with filter applied

**Secondary**:
- **Export Analytics** → Generate CSV with statistics
- **Tap Insight** → Navigate to related data
- **Custom Range** → Select specific date range

**Tertiary**:
- **Tap Tea** → Navigate to Tea Detail (from favorite teas chart)
- **Sync Now** → Force Firebase sync
- **Log Tea** → Navigate to Log Tea screen (from empty state)

### 10. Settings Screen
**Purpose**: Configure app preferences, account, and data management.

**Navigation**: Accessed from "More" tab in bottom navigation.

```
┌─────────────────────────────────┐
│ Settings                        │
├─────────────────────────────────┤
│                                 │
│ ACCOUNT                         │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔒 Not signed in            │ │ ← Disabled state
│ │                             │ │
│ │ Sign in to sync your data   │ │
│ │ across devices              │ │
│ │                             │ │
│ │ [Sign In with Google]       │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ NOTIFICATIONS                   │
│                                 │
│ Timer Notifications      [✓]   │ ← Toggle
│ Completion Sound        [✓]   │
│ Completion Vibration    [✓]   │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ PREFERENCES                     │
│                                 │
│ Temperature Unit               │
│ [Celsius ○ Fahrenheit]         │ ← Segmented control
│                                 │
│ Volume Unit                     │
│ [Milliliters ○ Fluid Ounces]   │
│                                 │
│ Theme                           │
│ [Light ○ Dark ○ System]        │
│                                 │
│ Multi-Steep Display             │
│ ○ Show all steeps               │
│ ● Show parent only              │
│ ○ Group steeps                  │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ DATA                            │
│                                 │
│ Backup & Restore          [>]  │ ← Navigate to Feature 11
│ Export All Data           [>]  │
│ Import Data               [>]  │
│ Clear All Data            [>]  │ ← Red text, danger
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ ABOUT                           │
│                                 │
│ Version 1.0.0 (Build 42)        │
│                                 │
└─────────────────────────────────┘
```

---

## Sections

### 1. Account Section

**Not Signed In State**:
```
┌─────────────────────────────┐
│ 🔒 Not signed in            │
│                             │
│ Sign in to sync your data   │
│ across devices              │
│                             │
│ [Sign In with Google]       │
│                             │
│ [Continue without sign-in]  │ ← Dismisses/collapses section
└─────────────────────────────┘
```

**Signed In State**:
```
┌─────────────────────────────┐
│ 👤 john@example.com         │
│                             │
│ Sync Status         [✓]    │ ← Toggle to enable/disable
│ Last synced: 5 min ago      │
│                             │
│ [Sign Out]                  │
└─────────────────────────────┘
```

**Components**:
- Sign-in button (Firebase Auth with Google)
- Account email (when signed in)
- Sync toggle (enable/disable Firebase sync)
- Last sync timestamp
- Sign out button

**Behavior**:
- Section always visible (not hidden when signed out)
- Controls disabled/grayed out until signed in
- "Continue without sign-in" collapses section to single line
- Tapping email → Account details screen (optional)

### 2. Notifications Section

**Components**:
1. **Timer Notifications** (Toggle)
   - Shows ongoing notification while timer is running
   - Shows notification when timer completes
   - Requires system notification permission
   - Default: ON

2. **Completion Sound** (Toggle)
   - Plays sound when timer completes
   - Uses system notification sound or custom
   - Default: ON

3. **Completion Vibration** (Toggle)
   - Vibrates device when timer completes
   - Default: ON

**Permission Handling**:
```
┌─────────────────────────────┐
│ Enable Notifications        │
├─────────────────────────────┤
│ Leaf Log needs notification │
│ permission to alert you     │
│ when brewing timers         │
│ complete.                   │
│                             │
│  [Not Now] [Allow]          │
└─────────────────────────────┘
```

**Behavior**:
- If system notifications disabled → Show warning banner
- If permission denied → Disable toggles, show "Enable in System Settings" link
- Test button to preview notification (optional)

### 3. Preferences Section

**Temperature Unit** (Segmented Control):
- Options: Celsius / Fahrenheit
- Default: Based on device locale (Celsius for most, Fahrenheit for US)
- Affects all temperature displays and inputs throughout app

**Volume Unit** (Segmented Control):
- Options: Milliliters / Fluid Ounces
- Default: Based on device locale (ml for metric countries, oz for US)
- Affects all volume displays and inputs throughout app

**Theme** (Segmented Control or Dropdown):
- Options: Light / Dark / System
- Default: System (follows OS theme)
- Immediate visual feedback on selection

**Multi-Steep Display** (Radio Buttons):
- **Show all steeps**: Display each steep as individual card in History
- **Show parent only** (Default): Display only parent session with steep count
- **Group steeps**: Display parent with expandable child list
- Affects History screen display only

**Note**: Default brewing parameters removed (tea types already have this)

### 4. Data Section

**Options** (Navigation List):

1. **Backup & Restore** [>]
   - Navigate to Feature 11: Backup & Restore Screen
   - Shows last backup time as subtitle

2. **Export All Data** [>]
   - Navigate to export flow
   - Exports sessions, teas, types, vessels as ZIP

3. **Import Data** [>]
   - Navigate to import flow
   - Select ZIP file to import

4. **Clear All Data** [>]
   - Red/destructive styling
   - Shows confirmation dialog

**Clear All Data Confirmation**:
```
┌─────────────────────────────────┐
│ ⚠️ Clear All Data?              │
├─────────────────────────────────┤
│                                 │
│ This will permanently delete:   │
│ • All brewing sessions          │
│ • All teas in your collection   │
│ • All custom tea types          │
│ • All custom vessels            │
│ • All photos                    │
│ • All settings                  │
│                                 │
│ This action CANNOT be undone.   │
│                                 │
│ Type "DELETE" to confirm:       │
│ [___________]                   │
│                                 │
│     [Cancel] [Clear Data]       │
└─────────────────────────────────┘
```

**Clear Data Flow**:
1. User taps "Clear All Data"
2. Show warning dialog
3. Require typing "DELETE" to enable button
4. Confirm → Delete all local data
5. If signed in → Ask if they want to clear cloud data too
6. Reset app to first-run state
7. Navigate to onboarding/home

### 5. About Section

**Components**:
- **Version**: App version number + build number
  - Format: "Version 1.0.0 (Build 42)"
  - Tap 7 times → Enable developer mode (optional)

**Display**:
- Simple text display
- Non-interactive (except optional dev mode easter egg)
- Small, subtle text

---

## Export All Data Flow

**Trigger**: Tap "Export All Data" in Settings

**Export Dialog**:
```
┌─────────────────────────────┐
│ Export All Data             │
├─────────────────────────────┤
│ Include:                    │
│ ☑ All sessions              │
│ ☑ All teas                  │
│ ☑ Tea types & vessels       │
│ ☑ Photos                    │
│ ☑ Settings                  │
│                             │
│ Format:                     │
│ ○ ZIP (full backup)         │
│ ● JSON (data only)          │
│                             │
│   [Cancel] [Export]         │
└─────────────────────────────┘
```

**Export Process**:
1. Show progress indicator
2. Package data:
   - ZIP: Database + photos + settings JSON
   - JSON: All data serialized, photo paths only
3. Generate filename: `leaf-log-backup-2026-01-31.zip`
4. Platform share sheet (save, email, etc.)
5. Show snackbar: "Data exported"

### Import Data Flow

**Trigger**: Tap "Import Data" in Settings

**File Picker**:
1. Show platform file picker
2. Filter: .zip, .json files
3. User selects file

**Import Confirmation**:
```
┌─────────────────────────────┐
│ Import Data?                │
├─────────────────────────────┤
│ This will add:              │
│ • 42 sessions               │
│ • 15 teas                   │
│ • 3 custom types            │
│ • 2 custom vessels          │
│                             │
│ Existing data will NOT be   │
│ deleted. Duplicates will    │
│ be merged.                  │
│                             │
│  [Cancel] [Import]          │
└─────────────────────────────┘
```

**Import Process**:
1. Parse file (validate format)
2. Show preview of what will be imported
3. Confirm → Import data:
   - Merge sessions (by ID, skip duplicates)
   - Merge teas (by ID, skip duplicates)
   - Merge types/vessels (by name, skip duplicates)
   - Copy photos to app storage
4. Show success message with import summary
5. Refresh all screens

**Error Handling**:
- Invalid file format → "Unable to read file"
- Corrupted data → "File is corrupted or incomplete"
- Missing photos → "Photos could not be imported"

---

## Use Cases Required

```kotlin
// Domain layer
UpdateUserPreferencesUseCase
GetUserPreferencesUseCase
SignInUseCase
SignOutUseCase
EnableSyncUseCase
ExportAllDataUseCase
ImportDataUseCase
ClearAllDataUseCase
RequestNotificationPermissionUseCase

// Repository methods
suspend fun getUserPreferences(): Flow<UserPreferences>
suspend fun updatePreferences(preferences: UserPreferences): Result<Unit>
suspend fun signIn(provider: AuthProvider): Result<User>
suspend fun signOut(): Result<Unit>
suspend fun exportAllData(format: ExportFormat): Result<String>  // Returns file path
suspend fun importData(filePath: String): Result<ImportSummary>
suspend fun clearAllData(includingCloud: Boolean): Result<Unit>

data class ImportSummary(
    val sessionsImported: Int,
    val teasImported: Int,
    val typesImported: Int,
    val vesselsImported: Int,
    val photosImported: Int,
    val errors: List<String>
)
```

---

## MVI State Model

```kotlin
data class SettingsState(
    // Account
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isSyncEnabled: Boolean = false,
    val lastSyncTime: Instant? = null,
    val syncInProgress: Boolean = false,

    // Preferences
    val preferences: UserPreferences = UserPreferences(),

    // Notifications
    val notificationsPermissionGranted: Boolean = false,

    // UI state
    val isLoading: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val clearDataConfirmText: String = "",
    val showExportDialog: Boolean = false,
    val showImportPreview: Boolean = false,
    val importSummary: ImportSummary? = null,
    val error: String? = null,
    val snackbarMessage: String? = null
)

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITERS,
    val theme: AppTheme = AppTheme.SYSTEM,
    val multiSteepDisplay: MultiSteepDisplay = MultiSteepDisplay.SHOW_PARENT_ONLY,

    // Notifications
    val timerNotificationsEnabled: Boolean = true,
    val completionSoundEnabled: Boolean = true,
    val completionVibrationEnabled: Boolean = true
)

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class MultiSteepDisplay {
    SHOW_ALL_STEEPS,
    SHOW_PARENT_ONLY,
    GROUP_STEEPS
}

sealed interface SettingsIntent {
    data object LoadSettings : SettingsIntent

    // Account
    data object SignIn : SettingsIntent
    data object SignOut : SettingsIntent
    data class ToggleSync(val enabled: Boolean) : SettingsIntent
    data object ContinueWithoutSignIn : SettingsIntent

    // Preferences
    data class UpdateTemperatureUnit(val unit: TemperatureUnit) : SettingsIntent
    data class UpdateVolumeUnit(val unit: VolumeUnit) : SettingsIntent
    data class UpdateTheme(val theme: AppTheme) : SettingsIntent
    data class UpdateMultiSteepDisplay(val display: MultiSteepDisplay) : SettingsIntent

    // Notifications
    data object RequestNotificationPermission : SettingsIntent
    data class ToggleTimerNotifications(val enabled: Boolean) : SettingsIntent
    data class ToggleCompletionSound(val enabled: Boolean) : SettingsIntent
    data class ToggleCompletionVibration(val enabled: Boolean) : SettingsIntent

    // Data
    data object NavigateToBackupRestore : SettingsIntent
    data object ShowExportDialog : SettingsIntent
    data object HideExportDialog : SettingsIntent
    data class ExportData(val format: ExportFormat, val inclusions: Set<ExportInclusion>) : SettingsIntent
    data object ShowImportPicker : SettingsIntent
    data class ImportData(val filePath: String) : SettingsIntent
    data object ShowClearDataDialog : SettingsIntent
    data object HideClearDataDialog : SettingsIntent
    data class UpdateClearDataConfirmText(val text: String) : SettingsIntent
    data class ConfirmClearData(val includingCloud: Boolean) : SettingsIntent

    // Snackbar
    data object DismissSnackbar : SettingsIntent
}

enum class ExportFormat {
    ZIP,    // Full backup with photos
    JSON    // Data only
}

enum class ExportInclusion {
    SESSIONS,
    TEAS,
    TYPES_VESSELS,
    PHOTOS,
    SETTINGS
}
```

---

## Screen Behavior

### Initial Load

1. Load user preferences from local storage
2. Check authentication status (Firebase)
3. Check notification permission status
4. Update state with all preferences
5. Display settings screen

### Sign In Flow

1. Tap "Sign In with Google"
2. Emit `SignIn` intent
3. Launch Firebase Auth flow (Google Sign-In)
4. User authenticates
5. On success:
   - Update state with user email
   - Enable sync toggle
   - Show snackbar: "Signed in as [email]"
6. On error:
   - Show error message
   - Keep signed-out state

### Sign Out Flow

1. Tap "Sign Out"
2. Show confirmation:
   ```
   ┌─────────────────────────────┐
   │ Sign Out?                   │
   ├─────────────────────────────┤
   │ Your data will remain on    │
   │ this device, but will no    │
   │ longer sync to the cloud.   │
   │                             │
   │   [Cancel] [Sign Out]       │
   └─────────────────────────────┘
   ```
3. Confirm → Sign out from Firebase
4. Disable sync
5. Update UI to signed-out state

### Preference Changes

**Immediate Effect**:
- Temperature/Volume units → Recalculate all displayed values
- Theme → Apply theme immediately
- Multi-steep display → Update History screen
- Notifications → Update system notification settings

**Persistence**:
- Save to local storage immediately on change
- Sync to Firebase if enabled
- No "Save" button needed (auto-save)

### Clear All Data Flow

1. Tap "Clear All Data"
2. Show confirmation dialog with typed confirmation
3. User types "DELETE"
4. Confirm button enables
5. Tap Confirm → Emit `ConfirmClearData` intent
6. If signed in, ask about cloud data:
   ```
   ┌─────────────────────────────┐
   │ Clear Cloud Data Too?       │
   ├─────────────────────────────┤
   │ Also delete your data from  │
   │ Firebase?                   │
   │                             │
   │ [Keep Cloud Data] [Delete]  │
   └─────────────────────────────┘
   ```
7. Delete all local data:
   - Drop all database tables
   - Delete all photos
   - Clear preferences
   - Clear cache
8. If confirmed, delete Firebase data too
9. Sign out user
10. Navigate to onboarding/home (first-run experience)
11. Show toast: "All data cleared"

---

## Edge Cases

1. **Sign-in fails**: Show error, stay signed out
2. **Sync disabled by user**: Local data persists, no cloud sync
3. **Notification permission denied**: Show banner with "Open Settings" link
4. **Theme change**: Apply immediately without restart
5. **Import file corrupted**: Show error, don't import anything
6. **Import duplicates**: Skip duplicates by ID, show count in summary
7. **Export fails**: Show error message, allow retry
8. **Clear data while syncing**: Cancel sync first, then clear
9. **Sign out while syncing**: Complete sync, then sign out
10. **Multi-steep display change**: Update History immediately (re-query if needed)

---

## Accessibility

- **Screen Reader**: Announce section headers, setting names, values
- **Toggles**: "Timer notifications, enabled, toggle button"
- **Segmented Controls**: "Temperature unit, Celsius selected, 1 of 2"
- **Disabled State**: "Sign in required to enable sync"
- **Confirmation Dialogs**: Read full warning text
- **Text Input**: "Type DELETE to confirm clearing all data"

---

## Performance Considerations

1. **Preference Changes**: Debounce saves (300ms) to avoid excessive writes
2. **Theme Application**: Use efficient theme switching (no full app restart)
3. **Export Large Data**: Run on background thread with progress indicator
4. **Import Validation**: Validate file before showing preview
5. **Clear Data**: Run on background thread, show progress
6. **Sync Status**: Poll Firebase less frequently (every 5 minutes max)

---

## Actions Summary

**Primary**:
- **Sign In/Out** → Authenticate with Firebase
- **Update Preferences** → Change units, theme, display options
- **Toggle Sync** → Enable/disable Firebase sync

**Secondary**:
- **Export All Data** → Create ZIP/JSON backup
- **Import Data** → Restore from backup file
- **Clear All Data** → Nuclear option with safeguards

**Tertiary**:
- **Request Permissions** → Enable notification permissions
- **Navigate to Backup** → Open Backup & Restore screen
- **Continue Without Sign-In** → Dismiss account section

### 11. Backup & Restore Screen
**Purpose**: Export and import data for device transfer, backup, or sharing. Consolidates all export functionality.

**Navigation**: From Settings "Backup & Restore" or "More" tab.

**Design Philosophy**: Local backup/export is the PRIMARY feature. Cloud backup is automatic and transparent (just works in background when signed in).

```
┌─────────────────────────────────┐
│ ← Backup & Restore              │
├─────────────────────────────────┤
│                                 │
│ CLOUD BACKUP                    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ✓ All synced                │ │ ← Signed in state
│ │                             │ │
│ │ Last backup: 2 min ago      │ │
│ │                             │ │
│ │ Auto-backup      [✓]       │ │
│ │ WiFi only        [✓]       │ │
│ │ Include photos   [✓]       │ │
│ │                             │ │
│ │ [Backup Now]                │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ LOCAL BACKUP                    │
│                                 │
│ Export your data to transfer    │
│ to another device or create     │
│ a backup file.                  │
│                                 │
│ [📦 Export Backup]              │
│                                 │
│ Estimated size: 45 MB           │
│ (42 sessions, 15 teas, 87 photos)│
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ RESTORE                         │
│                                 │
│ Import data from a backup       │
│ file or another device.         │
│                                 │
│ [📥 Import Backup]              │
│                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                 │
│ EXPORT OPTIONS                  │
│                                 │
│ Export specific data types      │
│ for analysis or sharing.        │
│                                 │
│ [📊 Export Sessions (CSV)]     │
│ [📊 Export Analytics (CSV)]    │
│ [🍵 Export Teas (CSV)]         │
│                                 │
└─────────────────────────────────┘
```

---

## Cloud Backup Section

**When Signed In**:

**Status Indicator**:
- ✓ All synced (green)
- ⟳ Syncing... (with progress)
- ⚠️ Sync error (with error message)
- ⏸ Paused (WiFi only, waiting for WiFi)

**Auto-Backup Toggle**:
- Default: ON
- When enabled, automatically syncs:
  - After every session completion
  - After tea CRUD operations
  - After type/vessel changes
  - On app backgrounding (if changes pending)
- When disabled, user must tap "Backup Now" manually

**WiFi Only Toggle**:
- Default: ON
- When enabled:
  - Only sync when connected to WiFi
  - Show "Waiting for WiFi" status on cellular
  - Queue changes for later sync
- When disabled, sync on any connection

**Include Photos Toggle**:
- Default: ON
- When enabled: Upload photos to Firebase Storage
- When disabled: Only sync database (sessions, teas, etc. without photo files)
- Note: "Photos may be large. Disable to save bandwidth."

**Backup Now Button**:
- Triggers immediate sync
- Shows progress: "Syncing 42 sessions, 15 teas, 87 photos..."
- Disabled during active sync

**Not Signed In State**:
```
┌─────────────────────────────┐
│ 🔒 Cloud backup disabled    │
│                             │
│ Sign in from Settings to    │
│ enable automatic cloud      │
│ backup.                     │
│                             │
│ [Go to Settings]            │
└─────────────────────────────┘
```

---

## Local Backup Section

**Purpose**: Primary method for users to backup/transfer data without Firebase

**Export Backup Flow**:

1. Tap "Export Backup" button
2. Calculate estimated size:
   ```kotlin
   val dbSize = getDatabaseSize()
   val photoSize = getPhotosSize()
   val totalSize = dbSize + photoSize
   ```
3. Show export options dialog:
   ```
   ┌─────────────────────────────┐
   │ Export Backup               │
   ├─────────────────────────────┤
   │ Estimated size: 45 MB       │
   │ • 42 sessions               │
   │ • 15 teas                   │
   │ • 87 photos                 │
   │                             │
   │ Include:                    │
   │ ☑ Sessions                  │
   │ ☑ Teas                      │
   │ ☑ Tea types & vessels       │
   │ ☑ Photos                    │
   │ ☑ Preferences               │
   │                             │
   │ Photo Quality:              │
   │ ○ Original (45 MB)          │
   │ ● Compressed (12 MB)        │
   │ ○ Exclude photos            │
   │                             │
   │  [Cancel] [Export]          │
   └─────────────────────────────┘
   ```
4. If size > 100MB, show warning:
   ```
   ⚠️ Large backup (145 MB)
   Consider excluding photos or
   using compressed quality.
   ```
5. User confirms options
6. Show progress:
   ```
   ┌─────────────────────────────┐
   │ Creating Backup...          │
   │                             │
   │ ████████░░░░░░ 65%          │
   │                             │
   │ Packaging photos...         │
   │                             │
   │     [Cancel]                │
   └─────────────────────────────┘
   ```
7. Generate ZIP file:
   - `/backup/leaf-log-backup-2026-01-31.zip`
   - Contents:
     - `database.db` (SQLite database)
     - `photos/` (directory with all photos)
     - `metadata.json` (app version, export date, counts)
8. Platform share sheet (save to files, share, etc.)
9. Show snackbar: "Backup exported (45 MB)"

**Backup File Structure**:
```
leaf-log-backup-2026-01-31.zip
├── metadata.json          # Export info
├── database.db            # Full SQLite database
└── photos/
    ├── uuid-1.jpg
    ├── uuid-2.jpg
    └── ...
```

---

## Restore Section

**Import Backup Flow**:

1. Tap "Import Backup" button
2. Platform file picker (filter: .zip files)
3. User selects backup file
4. Validate file:
   - Check if valid ZIP
   - Check for required files (metadata.json, database.db)
   - Parse metadata
   - Calculate what will be imported
5. Show preview dialog:
   ```
   ┌─────────────────────────────┐
   │ Import Backup?              │
   ├─────────────────────────────┤
   │ File: leaf-log-backup-...   │
   │ Created: Jan 31, 2026       │
   │ Size: 45 MB                 │
   │                             │
   │ This backup contains:       │
   │ • 42 sessions               │
   │ • 15 teas                   │
   │ • 5 custom tea types        │
   │ • 2 custom vessels          │
   │ • 87 photos                 │
   │                             │
   │ Import:                     │
   │ ☑ Sessions                  │
   │ ☑ Teas                      │
   │ ☑ Types & vessels           │
   │ ☑ Photos                    │
   │ ☑ Preferences               │
   │                             │
   │ ⚠️ Conflicts:               │
   │ • Same session IDs will     │
   │   be overwritten            │
   │ • Teas with same names but  │
   │   different IDs will both   │
   │   be imported               │
   │                             │
   │  [Cancel] [Import]          │
   └─────────────────────────────┘
   ```
6. User selects what to import (all selected by default)
7. Tap Import → Start import process
8. Show progress:
   ```
   ┌─────────────────────────────┐
   │ Importing Backup...         │
   │                             │
   │ ████████░░░░░░ 65%          │
   │                             │
   │ Importing photos (52/87)... │
   │                             │
   │     [Cancel]                │
   └─────────────────────────────┘
   ```
9. Import process:
   - Extract ZIP to temp directory
   - Validate database schema compatibility
   - Merge sessions (overwrite by ID)
   - Merge teas (import both if name match but different ID)
   - Merge types/vessels (by name, skip exact duplicates)
   - Copy photos to app storage
   - Merge preferences (user chooses which settings to keep)
10. Show success summary:
    ```
    ┌─────────────────────────────┐
    │ ✓ Import Complete           │
    ├─────────────────────────────┤
    │ Imported:                   │
    │ • 42 sessions (38 new)      │
    │ • 15 teas (12 new)          │
    │ • 5 types (3 new)           │
    │ • 2 vessels (0 new)         │
    │ • 87 photos                 │
    │                             │
    │ Conflicts:                  │
    │ • 4 sessions overwritten    │
    │ • 3 duplicate teas added    │
    │                             │
    │        [Done]               │
    └─────────────────────────────┘
    ```
11. Refresh all screens

**New Device Setup Flow**:
When app is first launched and user signs in:
```
┌─────────────────────────────┐
│ Restore from Cloud?         │
├─────────────────────────────┤
│ We found cloud backup data  │
│ for your account.           │
│                             │
│ Last backup: Jan 31, 2026   │
│ • 42 sessions               │
│ • 15 teas                   │
│                             │
│ [Start Fresh] [Restore]     │
└─────────────────────────────┘
```

---

## Export Options Section

**Purpose**: Consolidate all export features in one place

**Export Sessions (CSV)**:
```
[📊 Export Sessions (CSV)]
```
- Tap → Navigate to session export options
- Same functionality as History screen export
- Allows filtering by date range, type, etc.
- Generates CSV with session data

**Export Analytics (CSV)**:
```
[📊 Export Analytics (CSV)]
```
- Tap → Navigate to analytics export options
- Same functionality as Analytics screen export
- Exports summary stats, trends, charts data

**Export Teas (CSV)**:
```
[🍵 Export Teas (CSV)]
```
- Tap → Export tea collection as CSV
- Includes: name, type, origin, producer, stock, defaults, notes
- For sharing tea list or importing to spreadsheet

**Consolidation Note**: These replace the separate export buttons in History, Analytics, and Collection screens. Those screens now have menu options that link here.

---

## Use Cases Required

```kotlin
// Domain layer
CreateCloudBackupUseCase
RestoreFromCloudUseCase
CreateLocalBackupUseCase
ImportLocalBackupUseCase
ValidateBackupFileUseCase
GetBackupSizeEstimateUseCase
ExportSessionsCsvUseCase
ExportAnalyticsCsvUseCase
ExportTeasCsvUseCase

// Repository methods
suspend fun createCloudBackup(includePhotos: Boolean): Result<BackupResult>
suspend fun restoreFromCloud(): Result<RestoreResult>
suspend fun createLocalBackup(options: BackupOptions): Result<String>  // Returns file path
suspend fun importBackup(filePath: String, options: ImportOptions): Result<ImportSummary>
suspend fun validateBackup(filePath: String): Result<BackupMetadata>
suspend fun getBackupSize(includePhotos: Boolean): BackupSizeEstimate

data class BackupOptions(
    val includeSessions: Boolean = true,
    val includeTeas: Boolean = true,
    val includeTypesVessels: Boolean = true,
    val includePhotos: Boolean = true,
    val includePreferences: Boolean = true,
    val photoQuality: PhotoQuality = PhotoQuality.COMPRESSED
)

enum class PhotoQuality {
    ORIGINAL,       // Full resolution
    COMPRESSED,     // 80% quality, max 1920x1920
    EXCLUDED        // No photos
}

data class BackupSizeEstimate(
    val totalBytes: Long,
    val databaseBytes: Long,
    val photosBytes: Long,
    val sessionCount: Int,
    val teaCount: Int,
    val photoCount: Int
)

data class BackupMetadata(
    val version: String,
    val exportDate: Instant,
    val sessionCount: Int,
    val teaCount: Int,
    val typeCount: Int,
    val vesselCount: Int,
    val photoCount: Int,
    val totalSize: Long
)

data class ImportOptions(
    val importSessions: Boolean = true,
    val importTeas: Boolean = true,
    val importTypesVessels: Boolean = true,
    val importPhotos: Boolean = true,
    val importPreferences: Boolean = true
)

data class ImportSummary(
    val sessionsImported: Int,
    val sessionsOverwritten: Int,
    val teasImported: Int,
    val teaDuplicatesAdded: Int,
    val typesImported: Int,
    val vesselsImported: Int,
    val photosImported: Int,
    val errors: List<String>
)

data class BackupResult(
    val success: Boolean,
    val bytesUploaded: Long,
    val itemsBackedUp: Int
)

data class RestoreResult(
    val success: Boolean,
    val itemsRestored: Int,
    val conflicts: Int
)
```

---

## MVI State Model

```kotlin
data class BackupRestoreState(
    // Cloud backup
    val isSignedIn: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val cloudAutoBackupEnabled: Boolean = true,
    val cloudWifiOnlyEnabled: Boolean = true,
    val cloudIncludePhotos: Boolean = true,
    val lastCloudBackupTime: Instant? = null,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.SYNCED,
    val cloudSyncProgress: Float = 0f,  // 0.0 - 1.0

    // Local backup
    val backupSizeEstimate: BackupSizeEstimate? = null,
    val showExportDialog: Boolean = false,
    val exportOptions: BackupOptions = BackupOptions(),
    val exportProgress: Float = 0f,
    val exportInProgress: Boolean = false,

    // Import/Restore
    val showImportPicker: Boolean = false,
    val selectedBackupFile: String? = null,
    val backupMetadata: BackupMetadata? = null,
    val showImportPreview: Boolean = false,
    val importOptions: ImportOptions = ImportOptions(),
    val importProgress: Float = 0f,
    val importInProgress: Boolean = false,
    val showImportSummary: Boolean = false,
    val importSummary: ImportSummary? = null,

    // New device restore
    val showCloudRestorePrompt: Boolean = false,
    val cloudBackupAvailable: Boolean = false,

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

enum class CloudSyncStatus {
    SYNCED,         // All data backed up
    SYNCING,        // Upload in progress
    PENDING,        // Changes waiting to sync
    PAUSED,         // WiFi only, on cellular
    ERROR,          // Sync failed
    DISABLED        // Auto-backup off or signed out
}

sealed interface BackupRestoreIntent {
    data object LoadBackupInfo : BackupRestoreIntent

    // Cloud backup
    data class ToggleAutoBackup(val enabled: Boolean) : BackupRestoreIntent
    data class ToggleWifiOnly(val enabled: Boolean) : BackupRestoreIntent
    data class ToggleIncludePhotos(val enabled: Boolean) : BackupRestoreIntent
    data object BackupNow : BackupRestoreIntent
    data object RestoreFromCloud : BackupRestoreIntent
    data object DismissCloudRestorePrompt : BackupRestoreIntent

    // Local backup (export)
    data object ShowExportDialog : BackupRestoreIntent
    data object HideExportDialog : BackupRestoreIntent
    data class UpdateExportOptions(val options: BackupOptions) : BackupRestoreIntent
    data object ConfirmExport : BackupRestoreIntent
    data object CancelExport : BackupRestoreIntent

    // Import
    data object ShowImportPicker : BackupRestoreIntent
    data class SelectBackupFile(val filePath: String) : BackupRestoreIntent
    data object HideImportPreview : BackupRestoreIntent
    data class UpdateImportOptions(val options: ImportOptions) : BackupRestoreIntent
    data object ConfirmImport : BackupRestoreIntent
    data object CancelImport : BackupRestoreIntent
    data object DismissImportSummary : BackupRestoreIntent

    // Export specific types
    data object ExportSessionsCsv : BackupRestoreIntent
    data object ExportAnalyticsCsv : BackupRestoreIntent
    data object ExportTeasCsv : BackupRestoreIntent

    // Navigation
    data object NavigateToSettings : BackupRestoreIntent

    // Snackbar
    data object DismissSnackbar : BackupRestoreIntent
}
```

---

## Screen Behavior

### Cloud Backup Behavior

**Automatic Sync** (when enabled):
1. User completes a session
2. Queue sync operation
3. Check WiFi status if "WiFi only" enabled
4. If conditions met:
   - Update sync status to SYNCING
   - Upload database changes to Firestore
   - Upload new photos to Firebase Storage (separate, background)
   - Update sync status to SYNCED
5. If WiFi required but on cellular:
   - Queue changes
   - Show status: PAUSED "Waiting for WiFi"
   - Auto-retry when WiFi detected

**Photo Backup** (separate queue):
- Photos uploaded separately from database
- Large files handled asynchronously
- Retry logic for failed uploads
- Shows progress in notification (optional)

**Backup Now** (manual trigger):
1. Tap "Backup Now"
2. Ignore WiFi-only setting (force upload)
3. Show inline progress
4. On complete: Update last backup time

### Local Export Behavior

**Size Calculation**:
```kotlin
fun calculateBackupSize(options: BackupOptions): BackupSizeEstimate {
    val db = getDatabaseSize()  // SQLite file size
    var photos = 0L

    if (options.includePhotos) {
        photos = when (options.photoQuality) {
            ORIGINAL -> getAllPhotosSize()
            COMPRESSED -> getAllPhotosSize() * 0.3  // ~30% of original
            EXCLUDED -> 0L
        }
    }

    return BackupSizeEstimate(
        totalBytes = db + photos,
        databaseBytes = db,
        photosBytes = photos,
        sessionCount = getSessionCount(),
        teaCount = getTeaCount(),
        photoCount = getPhotoCount()
    )
}
```

**Photo Compression** (if selected):
- Compress on-the-fly during export
- Max 1920x1920, 80% quality
- Preserve EXIF data (timestamp)
- Run on background thread

**Export Cancellation**:
- User can cancel during export
- Clean up partial ZIP file
- Show message: "Export cancelled"

### Import Validation

**File Validation**:
1. Check file exists and readable
2. Verify ZIP format
3. Extract metadata.json
4. Validate app version compatibility:
   ```kotlin
   if (backupVersion > currentAppVersion) {
       showError("This backup was created with a newer version of Leaf Log")
       return
   }
   ```
5. Check database schema version
6. Validate JSON structure
7. If all valid → Show preview
8. If invalid → Show specific error

**Import Conflicts**:

**Sessions**:
- Same ID → Overwrite local with backup version
- No user intervention needed

**Teas**:
- Same ID → Overwrite
- Same name but different ID → Import both (user may have named two teas the same)
- Result: May have duplicate names in collection

**Types & Vessels**:
- Same name → Skip (assume same entity)
- Different name → Import as new

**Photos**:
- Copy all photos to app storage
- Skip if file already exists with same name

**Preferences**:
- Optional: Show conflict dialog asking which to keep
- Or simple: Always keep local preferences unless user explicitly selects

### New Device Setup (Cloud Restore)

**Trigger**: First app launch after sign-in, cloud backup detected

**Flow**:
1. User signs in with Google
2. Check Firestore for existing user data
3. If found → Show restore prompt (see mockup above)
4. User choice:
   - **Start Fresh**: Don't restore, start with empty app
   - **Restore**: Download cloud data, populate local database
5. If Restore selected:
   - Show progress
   - Download Firestore data
   - Download photos from Storage (in background)
   - Populate local database
   - Navigate to Home screen
   - Show welcome message: "Restored X sessions and Y teas"

---

## Export Specific Types

### Export Sessions CSV

**Flow**:
1. Tap "Export Sessions (CSV)"
2. Show filter options (same as History screen):
   ```
   ┌─────────────────────────────┐
   │ Export Sessions             │
   ├─────────────────────────────┤
   │ Date Range:                 │
   │ ● All Time                  │
   │ ○ This Month                │
   │ ○ Custom Range              │
   │                             │
   │ Include:                    │
   │ ☑ Completed                 │
   │ ☑ Draft                     │
   │ ☑ All tea types             │
   │                             │
   │  [Cancel] [Export]          │
   └─────────────────────────────┘
   ```
3. Generate CSV (see History screen for format)
4. Platform share sheet

### Export Analytics CSV

**Flow**:
1. Tap "Export Analytics (CSV)"
2. Show period selector:
   ```
   ┌─────────────────────────────┐
   │ Export Analytics            │
   ├─────────────────────────────┤
   │ Period:                     │
   │ ● This Month                │
   │ ○ This Year                 │
   │ ○ All Time                  │
   │                             │
   │  [Cancel] [Export]          │
   └─────────────────────────────┘
   ```
3. Generate CSV (see Analytics screen for format)
4. Platform share sheet

### Export Teas CSV

**Flow**:
1. Tap "Export Teas (CSV)"
2. No options needed (exports entire collection)
3. Generate CSV:
   ```csv
   Tea Name,Type,Origin,Producer,Stock,Purchase Date,Purchase Price,Default Temp,Default Time,Notes
   Dragon Well,Green,Hangzhou,West Lake Tea Co.,45g,2026-01-15,$24.99,80°C,2:30,Delicate vegetal notes...
   Sencha,Green,Shizuoka,Ito En,28g,2025-12-20,$18.50,75°C,2:00,Fresh grassy flavor
   ```
4. Platform share sheet

---

## Edge Cases

1. **Cloud backup fails**: Show error, retry with exponential backoff
2. **WiFi disconnects during backup**: Pause, resume when WiFi reconnects
3. **Import file too large**: Warn user, allow proceeding or cancelling
4. **Import fails mid-process**: Rollback changes, show error
5. **Backup file from future version**: Show compatibility error
6. **Photos missing from backup**: Import other data, warn about missing photos
7. **Duplicate tea names**: Import both, user can clean up later in Collection
8. **Storage full**: Catch error, show "Not enough storage space"
9. **Export cancelled**: Clean up partial files
10. **Sign out during backup**: Cancel backup, then sign out
11. **Corrupted ZIP file**: Validate before import, show specific error
12. **Very old backup**: Show warning if > 1 year old

---

## Accessibility

- **Cloud Status**: "Cloud backup synced, last backup 2 minutes ago"
- **Toggles**: "Auto-backup enabled, toggle button"
- **Export Button**: "Export backup, estimated size 45 megabytes"
- **Progress**: "Creating backup, 65 percent complete"
- **Import Preview**: Read full list of what will be imported
- **File Picker**: Standard platform accessibility
- **Warnings**: Announce size warnings, conflict messages

---

## Performance Considerations

1. **Background Upload**: Use WorkManager (Android) / Background Tasks (iOS) for cloud backup
2. **Photo Compression**: Compress on background thread
3. **ZIP Creation**: Stream files to ZIP, don't load all in memory
4. **Import Batching**: Import in batches (100 sessions at a time) to avoid memory issues
5. **Progress Updates**: Throttle UI updates (every 100ms max)
6. **Database Transaction**: Wrap import in single transaction for rollback capability
7. **Photo Downloads**: Download in background, show placeholder until available

---

## Actions Summary

**Primary**:
- **Export Backup** → Create local ZIP/JSON backup file
- **Import Backup** → Restore from backup file
- **Backup Now** → Force immediate cloud backup

**Secondary**:
- **Export Sessions** → CSV export with filtering
- **Export Analytics** → CSV export for analysis
- **Export Teas** → CSV export of tea collection
- **Toggle Cloud Settings** → Auto-backup, WiFi-only, Include photos
- **Restore from Cloud** → Download cloud data (new device setup)

**Tertiary**:
- **Change Photo Quality** → Original/Compressed/Excluded
- **Selective Import** → Choose what to import from backup
- **Cancel Operations** → Stop in-progress export/import

### 12. Home Screen Widgets

**Purpose**: Provide quick access to app features and display brewing statistics directly on device home screen.

**Platform Implementation**:
- **Android**: Jetpack Glance for Compose
- **iOS**: WidgetKit with SwiftUI
- **Behavior**: Identical across both platforms

**Update Frequency**: Every 15 minutes (system-managed)

---

## Widget 1: Quick Log Widget

**Purpose**: One-tap access to log a tea session.

**Interaction**: Tap anywhere → Opens app to Log Tea Screen

### Small Size (2x2 grid)
```
┌─────────────┐
│ 🍵 Leaf Log │
│             │
│  Log Tea    │
│             │
└─────────────┘
```

**Components**:
- App icon/logo
- "Log Tea" text label
- Subtle background (app theme color)

**Layout**: Centered content, minimal design

### Medium Size (4x2 grid)
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│       [Log Tea]             │ ← Button-style
│                             │
│   3 sessions today          │ ← Session count
└─────────────────────────────┘
```

**Components**:
- App icon/logo (top-left)
- "Log Tea" button (centered)
- Sessions today count (bottom)

**Layout**: More spacious, shows daily activity

### Large Size (4x4 grid)
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│       [Log Tea]             │
│                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                             │
│ Today                       │
│ 3 sessions • 600ml          │
│                             │
│ Last brewed:                │
│ 🍵 Dragon Well              │ ← Last tea with icon
│ 2 hours ago                 │
└─────────────────────────────┘
```

**Components**:
- App icon/logo
- "Log Tea" button
- Today's stats (sessions + water quantity)
- Last brewed tea (name + relative time)

**Layout**: Rich information display while maintaining simplicity

---

## Widget 2: Stats Widget

**Purpose**: Display brewing statistics at a glance.

**Interaction**: Tap anywhere → Opens app to Analytics Screen

**Configuration**: User can choose which stats to display (in widget settings)

### Small Size (2x2 grid)
```
┌─────────────┐
│ 🍵          │
│             │
│  3          │ ← Configurable stat
│  Today      │
└─────────────┘
```

**Components**:
- App icon
- Single stat (user-selected):
  - Sessions today
  - Sessions this week
  - Water today
  - Favorite tea name

**Layout**: Focus on one key metric

### Medium Size (4x2 grid)
```
┌─────────────────────────────┐
│ 🍵 Leaf Log Stats           │
│                             │
│ Today      This Week        │
│ 3 brews    12 brews         │
│ 600ml      2.4L             │
└─────────────────────────────┘
```

**Components**:
- App icon/title
- Two-column layout:
  - **Today**: Sessions + water quantity
  - **This Week**: Sessions + water quantity

**Layout**: Side-by-side comparison

### Large Size (4x4 grid)
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│ ┌───────────┬─────────────┐ │
│ │     3     │     12      │ │ ← 2x2 stat grid
│ │  Today    │ This Week   │ │
│ ├───────────┼─────────────┤ │
│ │  600ml    │     4.5     │ │
│ │ Water     │ Avg Rating  │ │
│ └───────────┴─────────────┘ │
│                             │
│ Most Brewed                 │
│ 🍵 Dragon Well (5x)         │ ← Favorite tea
│                             │
└─────────────────────────────┘
```

**Components**:
- App icon/title
- 2x2 stats grid (user-configurable):
  - Sessions today
  - Sessions this week
  - Water quantity today
  - Average rating (this week or all time)
- Most brewed tea (bottom)

**Layout**: Dashboard-style with rich information

---

## Widget Configuration

**Access**: Long-press widget → "Edit Widget" (platform behavior)

**Configuration Screen**:
```
┌─────────────────────────────┐
│ Configure Stats Widget      │
├─────────────────────────────┤
│ Display Stats:              │
│ ☑ Sessions today            │
│ ☑ Sessions this week        │
│ ☑ Water quantity today      │
│ ☑ Average rating            │
│ □ Favorite tea              │
│ □ Total sessions            │
│                             │
│ Theme:                      │
│ ○ Light                     │
│ ○ Dark                      │
│ ● Match App                 │
│                             │
│        [Save]               │
└─────────────────────────────┘
```

**Options**:
- **Stats Selection**: Choose which metrics to show (adapts to widget size)
- **Theme**: Light, Dark, or Match App theme
- **Quick Log Widget**: No configuration needed (simple by design)

**Adaptive Display**:
- Small widget: Shows first selected stat
- Medium widget: Shows first 2-4 stats
- Large widget: Shows first 4 stats + favorite tea

---

## Technical Implementation

### Android (Jetpack Glance)

**Widget Provider**:
```kotlin
class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickLogWidget()
}

class QuickLogWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickLogWidgetContent(
                size = LocalSize.current
            )
        }
    }
}
```

**Update Worker**:
```kotlin
class WidgetUpdateWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        // Query latest stats from database
        val stats = getWidgetStats()

        // Update all widget instances
        updateAllWidgets(context, stats)

        // Schedule next update (15 minutes)
        scheduleNextUpdate()

        return Result.success()
    }
}
```

**Size Variants**:
- Use `LocalSize` to detect widget size
- Render appropriate layout per size
- Single codebase for all sizes

### iOS (WidgetKit)

**Widget Definition**:
```swift
@main
struct LeafLogWidgets: WidgetBundle {
    var body: some Widget {
        QuickLogWidget()
        StatsWidget()
    }
}

struct QuickLogWidget: Widget {
    let kind: String = "QuickLogWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            QuickLogWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Quick Log")
        .description("One-tap access to log tea")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
```

**Timeline Provider**:
```swift
struct Provider: TimelineProvider {
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        // Fetch stats from shared database
        let stats = fetchWidgetStats()

        // Create entry
        let entry = WidgetEntry(date: Date(), stats: stats)

        // Update every 15 minutes
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date())!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))

        completion(timeline)
    }
}
```

---

## Data Sharing (Cross-Process)

**Challenge**: Widgets run in separate process, need access to app data

**Android Solution**: Room database in shared directory
```kotlin
// In widget process
val db = Room.databaseBuilder(
    context.applicationContext,
    LeafLogDatabase::class.java,
    "leaf_log.db"
).build()

// Read-only access to query stats
val stats = db.sessionDao().getStatsForWidget()
```

**iOS Solution**: App Groups for shared container
```swift
// Enable App Groups in Xcode
// com.leaflog.shared

let sharedDefaults = UserDefaults(suiteName: "group.com.leaflog.shared")
let stats = sharedDefaults?.dictionary(forKey: "widgetStats")

// App updates shared defaults on data change
func updateWidgetData() {
    let stats = calculateWidgetStats()
    sharedDefaults?.set(stats, forKey: "widgetStats")
    WidgetCenter.shared.reloadAllTimelines()
}
```

---

## Widget Data Model

```kotlin
data class WidgetStats(
    val sessionsToday: Int,
    val sessionsThisWeek: Int,
    val waterQuantityTodayMl: Int,
    val averageRating: Float?,
    val favoriteTea: WidgetTea?,
    val lastBrewedTea: WidgetTea?,
    val lastUpdated: Instant
)

data class WidgetTea(
    val name: String,
    val photoUri: String?,
    val lastBrewedAt: Instant
)

data class WidgetConfiguration(
    val selectedStats: List<WidgetStat>,
    val theme: WidgetTheme
)

enum class WidgetStat {
    SESSIONS_TODAY,
    SESSIONS_THIS_WEEK,
    WATER_QUANTITY_TODAY,
    AVERAGE_RATING,
    FAVORITE_TEA,
    TOTAL_SESSIONS
}

enum class WidgetTheme {
    LIGHT,
    DARK,
    MATCH_APP
}
```

---

## Widget Update Strategy

**Triggers** (when to update widgets):

1. **App Data Changes**:
   - Session completed → Update immediately
   - Tea added/edited → Update if favorite tea changes
   - On app close → Update stats

2. **Scheduled Updates**:
   - Every 15 minutes (system timeline refresh)
   - On date change (midnight) → Reset "today" stats

3. **User Triggered**:
   - User taps widget → Update on app open
   - User edits widget config → Update immediately

**Implementation**:
```kotlin
// In ViewModel or Use Case, after session saved
fun onSessionCompleted() {
    // Save session to database
    saveSession(session)

    // Trigger widget update
    WidgetUpdateManager.updateAll(context)
}

object WidgetUpdateManager {
    fun updateAll(context: Context) {
        // Calculate latest stats
        val stats = calculateWidgetStats()

        // Update shared storage
        saveWidgetStats(stats)

        // Tell system to refresh widgets
        GlanceAppWidgetManager.updateAll(context)  // Android
        WidgetCenter.shared.reloadAllTimelines()   // iOS
    }
}
```

---

## Widget Behavior

### Quick Log Widget

**All Sizes**:
1. User taps widget
2. Open app to Log Tea Screen
3. If app already open → Navigate to Log Tea Screen
4. Deep link: `leaflog://log-tea`

**No State**: Static widget, always shows same content

### Stats Widget

**All Sizes**:
1. User taps widget
2. Open app to Analytics Screen
3. Deep link: `leaflog://analytics`

**Updates**:
- Refreshes every 15 minutes
- Shows latest stats from database
- Adapts layout based on size
- If no data (< 1 session) → Shows "Start brewing!"

### Widget Configuration Changes

**Flow**:
1. User long-presses Stats Widget
2. Platform config UI appears
3. User selects stats to display
4. Save → Update widget preferences
5. Recalculate layout for selected stats
6. Refresh widget immediately

---

## Size-Specific Layouts

### Quick Log Widget

**Small (2x2)**:
```
┌─────────────┐
│ 🍵 Leaf Log │
│             │
│  Log Tea    │
│             │
└─────────────┘
```
- Minimal: Icon + label
- Single tap target

**Medium (4x2)**:
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│       [Log Tea]             │
│                             │
│   3 sessions today          │
└─────────────────────────────┘
```
- Adds session count
- Button-style presentation

**Large (4x4)**:
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│       [Log Tea]             │
│                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                             │
│ Today                       │
│ 3 sessions • 600ml          │
│                             │
│ Last brewed:                │
│ 🍵 Dragon Well              │
│ 2 hours ago                 │
└─────────────────────────────┘
```
- Adds rich context
- Shows last brewed tea
- Daily summary stats

### Stats Widget

**Small (2x2)** - Shows 1 stat (first selected):
```
┌─────────────┐
│ 🍵          │
│             │
│  3          │ ← Configurable stat
│  Today      │
└─────────────┘
```

**Medium (4x2)** - Shows 2 stats (first two selected):
```
┌─────────────────────────────┐
│ 🍵 Leaf Log Stats           │
│                             │
│ Today      This Week        │
│ 3 brews    12 brews         │
│ 600ml      2.4L             │
└─────────────────────────────┘
```

**Large (4x4)** - Shows 4 stats + favorite:
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│ ┌───────────┬─────────────┐ │
│ │     3     │     12      │ │
│ │  Today    │ This Week   │ │
│ ├───────────┼─────────────┤ │
│ │  600ml    │     4.5     │ │
│ │ Water     │ Avg Rating  │ │
│ └───────────┴─────────────┘ │
│                             │
│ Most Brewed                 │
│ 🍵 Dragon Well (5x)         │
│                             │
└─────────────────────────────┘
```

**Configuration Impact**:
- If user selects 2 stats → Medium widget shows those 2
- If user selects 4 stats → Large widget shows those 4
- Small widget always shows first selected stat
- If "Favorite tea" selected → Shows in large widget only

---

## Widget State Management

### Empty State (No Sessions)
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│   Start Your Tea Journey    │
│                             │
│       [Log Tea]             │
│                             │
└─────────────────────────────┘
```

**Display**: Encouragement message instead of stats

### Loading State
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│       Loading...            │
│                             │
└─────────────────────────────┘
```

**Display**: Brief loading state on first install or data sync

### Error State
```
┌─────────────────────────────┐
│ 🍵 Leaf Log                 │
│                             │
│   Unable to load stats      │
│   Tap to open app           │
│                             │
└─────────────────────────────┘
```

**Display**: Graceful degradation if database unavailable

---

## Use Cases Required

```kotlin
// Domain layer
GetWidgetStatsUseCase
UpdateWidgetDataUseCase
GetWidgetConfigurationUseCase
SaveWidgetConfigurationUseCase

// Repository methods
suspend fun getWidgetStats(): WidgetStats
suspend fun saveWidgetConfig(config: WidgetConfiguration)
suspend fun getWidgetConfig(): WidgetConfiguration

// Widget-specific calculations
fun calculateSessionsToday(): Int
fun calculateSessionsThisWeek(): Int
fun calculateWaterQuantityToday(): Int  // In ml
fun getAverageRating(period: Period): Float?
fun getFavoriteTea(): WidgetTea?
fun getLastBrewedTea(): WidgetTea?
```

---

## Implementation Notes

### Android Specific

**Glance Composables**:
```kotlin
@Composable
fun QuickLogWidgetContent(size: DpSize) {
    when {
        size.height < 100.dp -> SmallQuickLogWidget()
        size.height < 200.dp -> MediumQuickLogWidget()
        else -> LargeQuickLogWidget()
    }
}

@Composable
fun SmallQuickLogWidget() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(actionStartActivity<MainActivity>(
                actionParametersOf(
                    "destination" to "log_tea"
                )
            )),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(provider = ImageProvider(R.drawable.ic_leaf_log))
        Text("Log Tea", style = TextStyle(...))
    }
}
```

**Update Worker**:
```kotlin
// Schedule periodic updates
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "widget_update",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build())
            .build()
    )
```

### iOS Specific

**Timeline Entry**:
```swift
struct WidgetEntry: TimelineEntry {
    let date: Date
    let stats: WidgetStats
    let configuration: WidgetConfiguration
}

struct WidgetStats {
    let sessionsToday: Int
    let sessionsThisWeek: Int
    let waterQuantityMl: Int
    let averageRating: Double?
    let favoriteTea: WidgetTea?
}
```

**Deep Linking**:
```swift
struct QuickLogWidgetView: View {
    var entry: Provider.Entry

    var body: some View {
        ZStack {
            // Widget content
            VStack {
                Image("leaf_log_icon")
                Text("Log Tea")
            }
        }
        .widgetURL(URL(string: "leaflog://log-tea"))  // Deep link
    }
}
```

**Size Families**:
```swift
.supportedFamilies([
    .systemSmall,   // 2x2
    .systemMedium,  // 4x2
    .systemLarge    // 4x4
])
```

---

## Widget Analytics

**Track Usage** (optional):
- Widget tap count
- Most used widget type
- Most popular widget size
- Helps inform future widget features

**Privacy**: Local analytics only (no external tracking)

---

## Edge Cases

1. **No sessions yet**: Show "Start brewing" message
2. **Database locked**: Show error state, retry on next update
3. **Photos not available**: Use tea type icon or placeholder
4. **Widget added while app not initialized**: Show loading state, update when app opens
5. **User changes theme**: Widgets update to match on next refresh
6. **Time zone change**: Recalculate "today" stats correctly
7. **Large photo in widget**: Compress/thumbnail for widget display
8. **Multiple widgets of same type**: All share same data, update together
9. **Widget removed by user**: Stop updating that widget instance
10. **App uninstalled**: System removes widgets automatically

---

## Accessibility

### Android
- **Content Description**: "Leaf Log Quick Log widget, tap to log tea"
- **Stats**: "3 sessions today, 600 milliliters water"
- **TalkBack**: Full widget content readable
- **Touch Target**: Entire widget is tappable (meets 48dp minimum)

### iOS
- **Accessibility Label**: "Quick Log widget"
- **Accessibility Value**: "3 sessions today"
- **VoiceOver**: Reads all stat values
- **Dynamic Type**: Widget text scales with system font size

---

## Performance Considerations

1. **Update Frequency**: 15-minute intervals balance freshness vs battery
2. **Database Queries**: Optimize widget stats queries (use pre-computed values)
3. **Image Loading**: Use cached thumbnails, async loading
4. **Memory**: Keep widget code minimal, shared with app
5. **Battery**: Use efficient update mechanisms (WorkManager, Timeline)
6. **Background Processing**: Limit computation time in widget provider
7. **Multiple Instances**: Share calculated stats across all widget instances

---

## Actions Summary

**Quick Log Widget**:
- **Tap Widget** → Open app to Log Tea Screen

**Stats Widget**:
- **Tap Widget** → Open app to Analytics Screen
- **Configure Widget** → Select displayed stats and theme

**System Actions**:
- **15-Minute Update** → Refresh stats from database
- **Midnight Rollover** → Reset daily stats
- **Data Change** → Immediate update on session save

---

## Navigation Flow

### Navigation Graph

```
Home Screen (Start Destination)
├── Log Tea Screen
│   ├── Brewing Timer Screen
│   │   └── Session Detail Screen (after completion)
│   └── Add Tea Screen (quick add)
│       └── Tea Detail Screen (after creation)
├── Brewing Timer Screen (from quick action)
│   └── Log Tea Screen (to save session)
├── Collection Screen
│   ├── Tea Detail Screen
│   │   ├── Edit Tea Screen
│   │   │   └── Tea Detail Screen (after save)
│   │   ├── Session Detail Screen (from related sessions)
│   │   └── Log Tea Screen (quick log)
│   └── Add Tea Screen
│       └── Tea Detail Screen (after creation)
├── History Screen
│   └── Session Detail Screen
│       ├── Edit Session Screen
│       │   └── Session Detail Screen (after save)
│       └── Log Tea Screen (brew again)
├── Analytics Screen
│   └── History Screen (filtered by chart tap)
└── Settings Screen
    ├── Account Settings Screen
    │   └── Sign In Screen (if not signed in)
    ├── Notification Settings Screen
    └── Backup & Restore Screen
```

### Navigation Implementation (Compose Navigation)

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LogTea : Screen("log_tea?teaId={teaId}&sessionId={sessionId}")
    object BrewingTimer : Screen("timer?teaId={teaId}&duration={duration}")
    object TeaLibrary : Screen("tea_library")
    object TeaDetail : Screen("tea_detail/{teaId}")
    object AddEditTea : Screen("add_edit_tea?teaId={teaId}")
    object History : Screen("history")
    object SessionDetail : Screen("session_detail/{sessionId}")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object SignIn : Screen("sign_in")
    object BackupRestore : Screen("backup_restore")
}
```

---

## Data Synchronization Strategy

### Offline-First Architecture

The app follows an offline-first approach where:
1. **Room database** is the single source of truth
2. **All operations** work offline immediately
3. **Firebase sync** is optional and asynchronous
4. **Sync queue** tracks pending changes

### Sync Flow

```
User Action
    ↓
Update Room Database (immediate)
    ↓
Add to Sync Queue (if sync enabled)
    ↓
Return Success to UI
    ↓
Background Sync (when online)
    ↓
Update Firebase (async)
    ↓
Mark as Synced in Room
    ↓
Remove from Sync Queue
```

### Sync Queue System

**Purpose**: Track operations that need to be synced to Firebase.

**Process**:
1. Any create/update/delete operation adds an entry to `sync_queue` table
2. Background sync worker periodically processes the queue
3. Each operation is attempted with retry logic
4. Successful syncs update the entity's `sync_status` and remove from queue
5. Failed syncs remain in queue with error information

**Conflict Resolution**:
- Last-write-wins strategy based on `updated_at` timestamp
- User can choose to keep local or remote version in case of conflict
- Conflicts are rare since the app is typically single-user

### Sync Implementation

**Android**: WorkManager with periodic work request
```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return syncRepository.syncPendingChanges()
    }
}
```

**iOS**: BackgroundTasks framework
```swift
BGTaskScheduler.shared.register(
    forTaskWithIdentifier: "com.leaflog.sync",
    using: nil
) { task in
    handleSync(task: task as! BGProcessingTask)
}
```

### Data Model Synchronization

**Firestore Collections**:
```
users/{userId}/
├── teas/
│   └── {teaId}
│       ├── id, name, type, origin, ...
│       └── updatedAt (for conflict resolution)
└── sessions/
    └── {sessionId}
        ├── id, teaId, timestamp, ...
        └── updatedAt
```

**Storage Structure** (for photos):
```
users/{userId}/
├── teas/
│   └── {teaId}/
│       └── {photoId}.jpg
└── sessions/
    └── {sessionId}/
        └── {photoId}.jpg
```

### Sync Considerations

1. **Photos**: Large files synced separately with lower priority
2. **Batch operations**: Group multiple changes into batches
3. **Network awareness**: Only sync on WiFi (configurable)
4. **Battery optimization**: Respect device battery state
5. **Exponential backoff**: Retry failed syncs with increasing delays

---

## Testing Strategy

### Testing Pyramid

```
                 ▲
                / \
               /   \
              /     \
             /  E2E  \
            /  Tests  \
           /___________\
          /             \
         /  Integration  \
        /     Tests       \
       /_________________\
      /                   \
     /     Unit Tests      \
    /_______________________\
```

### 1. Unit Tests (70% coverage target)

**Domain Layer** (Pure Kotlin, easy to test):
- **Use Cases**: Test business logic in isolation
  - Input validation
  - Business rules
  - Error handling
  - Edge cases

**Data Layer**:
- **Repositories**: Test with mock data sources
- **Mappers**: Test entity-to-domain conversions
- **Data Sources**: Mock Room DAOs and Firebase services

**Test Tools**:
- `kotlin.test` for assertions
- `kotlinx-coroutines-test` for testing coroutines
- `mockk` for mocking dependencies
- `turbine` for testing Flows

**Example Test**:
```kotlin
class CreateTeaSessionUseCaseTest {
    private val repository = mockk<TeaSessionRepository>()
    private val useCase = CreateTeaSessionUseCase(repository)

    @Test
    fun `should create tea session with valid data`() = runTest {
        // Given
        val session = TeaSession(...)
        coEvery { repository.createSession(session) } returns Result.success(session)

        // When
        val result = useCase(session)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(session, result.getOrNull())
        coVerify { repository.createSession(session) }
    }
}
```

### 2. Integration Tests (20% coverage target)

**Database Tests**:
- Test Room DAOs with in-memory database
- Test complex queries
- Test database migrations
- Test foreign key constraints

**Repository Integration Tests**:
- Test repository implementations with real Room database
- Test data mapping and transformation
- Test error scenarios

**Example Test**:
```kotlin
class TeaSessionRepositoryImplTest {
    private lateinit var database: LeafLogDatabase
    private lateinit var repository: TeaSessionRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            LeafLogDatabase::class.java
        ).build()
        repository = TeaSessionRepositoryImpl(database.teaSessionDao())
    }

    @Test
    fun `should retrieve sessions by tea id`() = runTest {
        // Given
        val tea = createTestTea()
        database.teaDao().insert(tea)
        val sessions = listOf(
            createTestSession(teaId = tea.id),
            createTestSession(teaId = tea.id)
        )
        sessions.forEach { database.teaSessionDao().insert(it) }

        // When
        val result = repository.getSessionsByTeaId(tea.id).first()

        // Then
        assertEquals(2, result.size)
    }
}
```

### 3. UI Tests (10% coverage target)

**Compose UI Tests**:
- Test critical user flows
- Test UI state rendering
- Test user interactions
- Test navigation

**Test Tools**:
- Compose UI Testing framework
- Screenshot testing (optional)

**Example Test**:
```kotlin
@Test
fun `should display tea sessions on home screen`() = runComposeUiTest {
    setContent {
        HomeScreen(
            state = HomeState(
                recentSessions = listOf(testSession1, testSession2)
            )
        )
    }

    onNodeWithText(testSession1.teaName).assertIsDisplayed()
    onNodeWithText(testSession2.teaName).assertIsDisplayed()
}
```

### 4. Test Data Utilities

**Shared Test Data**:
```kotlin
object TestData {
    fun createTestTea(
        id: String = "tea-1",
        name: String = "Test Tea",
        type: TeaType = TeaType.GREEN
    ) = Tea(...)

    fun createTestSession(
        id: String = "session-1",
        teaId: String = "tea-1",
        timestamp: Instant = Clock.System.now()
    ) = TeaSession(...)
}
```

### Testing CI/CD

**Automated Testing**:
- Run tests on every commit (GitHub Actions / CI)
- Separate jobs for Android and iOS tests
- Generate test coverage reports
- Block merges if tests fail

---

## Dependencies

### Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
kotlin = "2.1.0"
compose = "1.7.0"
koin = "4.0.0"
room = "2.7.0-alpha12"
ktor = "3.0.0"
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.6.1"
kotlinx-coroutines = "1.9.0"
coil = "3.0.0"
navigation = "2.8.0"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }

# Compose Multiplatform
compose-runtime = { module = "androidx.compose.runtime:runtime", version.ref = "compose" }
compose-foundation = { module = "androidx.compose.foundation:foundation", version.ref = "compose" }
compose-material3 = { module = "androidx.compose.material3:material3", version = "1.3.0" }
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "compose" }

# Navigation
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

# Dependency Injection
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }

# Database
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# Networking
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

# Date/Time
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

# Image Loading
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor", version.ref = "coil" }

# Firebase (Optional)
firebase-auth = { module = "dev.gitlive:firebase-auth", version = "2.1.0" }
firebase-firestore = { module = "dev.gitlive:firebase-firestore", version = "2.1.0" }
firebase-storage = { module = "dev.gitlive:firebase-storage", version = "2.1.0" }

# Testing
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
mockk = { module = "io.mockk:mockk", version = "1.13.12" }
turbine = { module = "app.cash.turbine:turbine", version = "1.1.0" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4", version.ref = "compose" }

# Android Specific
androidx-activity-compose = { module = "androidx.activity:activity-compose", version = "1.9.0" }
androidx-work-runtime = { module = "androidx.work:work-runtime-ktx", version = "2.9.0" }
androidx-glance = { module = "androidx.glance:glance-appwidget", version = "1.1.0" }
```

---

## Implementation Phases

### Phase 1: Project Setup & Foundation (Week 1-2)
**Goal**: Establish project structure and core architecture.

**Tasks**:
1. Initialize Compose Multiplatform project
2. Set up module structure (shared, androidApp, iosApp)
3. Configure Koin dependency injection
4. Set up Room database with initial schema
5. Create base domain models (Tea, TeaSession, enums)
6. Implement basic navigation structure
7. Set up Material 3 theming
8. Configure build scripts and version catalog
9. Set up Git repository and CI/CD pipeline

**Deliverable**: Project compiles and runs on Android and iOS with empty screens.

### Phase 2: Core Data Layer (Week 3-4)
**Goal**: Implement offline-first data persistence.

**Tasks**:
1. Create Room entities and DAOs
2. Implement repository interfaces in domain layer
3. Implement repository implementations in data layer
4. Create data mappers (Entity ↔ Domain)
5. Implement local data sources
6. Set up preferences storage
7. Write unit tests for repositories and mappers
8. Write integration tests for DAOs

**Deliverable**: Complete data layer with tested CRUD operations for teas and sessions.

### Phase 3: Core Features - Tea Collection (Week 5-6)
**Goal**: Implement tea collection management.

**Tasks**:
1. Implement Tea Collection use cases
2. Create Collection screen UI (3 tabs: Teas, Types, Vessels)
3. Create Tea Detail screen UI
4. Create Add/Edit Tea screen UI
5. Implement image picking and storage
6. Implement search and filtering
7. Implement ViewModels and state management
8. Write tests for use cases and ViewModels

**Deliverable**: Full tea collection feature with CRUD operations.

### Phase 4: Core Features - Session Logging (Week 7-8)
**Goal**: Implement tea session logging.

**Tasks**:
1. Implement session logging use cases
2. Create Log Tea screen UI
3. Create Session Detail screen UI
4. Implement History screen UI
5. Implement search and filtering for sessions
6. Implement session photos
7. Implement ViewModels and state management
8. Write tests

**Deliverable**: Complete session logging and history viewing.

### Phase 5: Home Screen & Dashboard (Week 9)
**Goal**: Create main app dashboard.

**Tasks**:
1. Implement home screen use cases
2. Create Home screen UI with summary cards
3. Implement quick actions
4. Display recent sessions
5. Implement navigation from home to features
6. Polish UI and animations

**Deliverable**: Functional home screen with navigation.

### Phase 6: Brewing Timer (Week 10-11)
**Goal**: Implement brewing timer feature.

**Tasks**:
1. Implement timer use cases and business logic
2. Create Brewing Timer screen UI
3. Implement platform-specific notifications (Android/iOS)
4. Implement timer state persistence
5. Implement background timer continuation
6. Add timer quick actions from home screen
7. Write tests for timer logic

**Deliverable**: Functional brewing timer with notifications.

### Phase 7: Analytics & Statistics (Week 12-13)
**Goal**: Implement analytics and visualizations.

**Tasks**:
1. Implement analytics use cases
2. Create statistics computation logic
3. Create Analytics screen UI
4. Implement charts (trends, distribution, etc.)
5. Implement time period filtering
6. Add export functionality
7. Write tests for analytics calculations

**Deliverable**: Complete analytics feature with visualizations.

### Phase 8: Firebase Integration (Week 14-15)
**Goal**: Add optional cloud sync.

**Tasks**:
1. Set up Firebase project (Firestore, Auth, Storage)
2. Implement Firebase services wrapper
3. Implement authentication use cases
4. Create Sign In screen
5. Implement sync engine and conflict resolution
6. Implement sync queue processing
7. Add Android WorkManager sync job
8. Add iOS background sync
9. Test sync scenarios
10. Add sync status UI indicators

**Deliverable**: Optional Firebase authentication and sync.

### Phase 9: Settings & Backup (Week 16)
**Goal**: Implement settings and data management.

**Tasks**:
1. Create Settings screen UI
2. Implement preferences management
3. Implement backup/export functionality
4. Implement restore/import functionality
5. Add data export to CSV
6. Add cloud backup (if authenticated)
7. Implement settings use cases

**Deliverable**: Complete settings and backup features.

### Phase 10: Widgets (Week 17)
**Goal**: Add home screen widgets.

**Tasks**:
1. Create Quick Log widget (Android Glance)
2. Create Stats widget (Android Glance)
3. Create Quick Log widget (iOS WidgetKit)
4. Create Stats widget (iOS WidgetKit)
5. Implement widget data providers
6. Test widget functionality

**Deliverable**: Home screen widgets for both platforms.

### Phase 11: Polish & Testing (Week 18-19)
**Goal**: Polish UI/UX and comprehensive testing.

**Tasks**:
1. UI/UX polish pass on all screens
2. Add animations and transitions
3. Implement error handling and loading states
4. Add empty states and placeholders
5. Comprehensive testing pass
6. Accessibility improvements
7. Performance optimization
8. Fix bugs from testing

**Deliverable**: Polished, tested app ready for beta.

### Phase 12: Beta & Launch Prep (Week 20)
**Goal**: Prepare for app store submission.

**Tasks**:
1. Beta testing with test users
2. Bug fixes from beta feedback
3. Create app store assets (screenshots, descriptions)
4. Prepare privacy policy and terms
5. Set up app analytics
6. Final QA pass
7. Submit to app stores

**Deliverable**: Apps submitted to Google Play and App Store.

---

## Additional Considerations

### Security

1. **Data Encryption**:
   - Use SQLCipher for Room database encryption (optional)
   - Encrypt sensitive data at rest
   - Use Android Keystore / iOS Keychain for credentials

2. **Network Security**:
   - HTTPS only for Firebase communication
   - Certificate pinning (optional, advanced)
   - Validate server responses

3. **Authentication**:
   - Firebase Authentication handles security
   - Token refresh and session management
   - Secure logout and data cleanup

### Performance

1. **Database**:
   - Proper indexing on frequently queried columns
   - Pagination for large lists
   - Lazy loading for images

2. **UI**:
   - Lazy lists for long scrollable content
   - Image compression and caching (Coil)
   - Debounce search inputs
   - Optimize recomposition in Compose

3. **Memory**:
   - Proper lifecycle management for ViewModels
   - Cancel coroutines when no longer needed
   - Release resources in onCleared()

### Accessibility

1. **Content Descriptions**: Provide descriptions for all images and icons
2. **Semantic Properties**: Use Compose semantics for screen readers
3. **Touch Targets**: Minimum 48dp touch targets
4. **Color Contrast**: Ensure WCAG AA compliance for text
5. **Font Scaling**: Support dynamic type / font scaling

### Localization (Future)

1. **String Resources**: Externalize all user-facing strings
2. **Date/Time Formatting**: Use locale-aware formatting
3. **Number Formatting**: Respect locale for numbers and currencies
4. **RTL Support**: Test with right-to-left languages

### Analytics (Optional)

1. **Events to Track**:
   - Session logging events
   - Feature usage (timer, analytics, etc.)
   - Sync events and errors
   - App launches and screen views

2. **Privacy**:
   - Opt-in analytics
   - No PII collection
   - Respect user privacy preferences

---

## Success Criteria

### Technical Criteria

- [ ] Compiles and runs on Android (API 26+) and iOS (15+)
- [ ] 70%+ unit test coverage
- [ ] Zero memory leaks
- [ ] Offline-first works without network
- [ ] Sync works reliably with conflict resolution
- [ ] App size < 50MB
- [ ] Startup time < 2 seconds
- [ ] No crashes in production

### Functional Criteria

- [ ] Users can log tea sessions with all specified parameters
- [ ] Users can manage tea collection (CRUD operations)
- [ ] Users can view history and search sessions
- [ ] Users can use brewing timer with notifications
- [ ] Users can view analytics and statistics
- [ ] Users can optionally sign in and sync data
- [ ] Users can backup and restore data
- [ ] Home screen widgets work on both platforms

### User Experience Criteria

- [ ] Material Design 3 adherence
- [ ] Consistent UI across screens
- [ ] Smooth animations and transitions
- [ ] Clear error messages and loading states
- [ ] Intuitive navigation
- [ ] Responsive to user input (< 100ms)
- [ ] Accessible to users with disabilities

---

## Open Questions & Future Enhancements

### Open Questions (to be decided)

1. **Temperature Tracking**: Should the app integrate with smart kettles or temperature sensors?
2. **Social Features**: Should users be able to share sessions or follow other users?
3. **Monetization**: Will this be a paid app, freemium, or ad-supported?
4. **Tea Discovery**: Should the app include a public tea database or recommendations?
5. **Multi-device**: Should one account support multiple devices simultaneously?

### Future Enhancements (v2.0+)

1. **Tea Recommendations**: ML-based suggestions based on history and preferences
2. **Tea Journal**: More detailed notes with markdown support and tags
3. **Weather Integration**: Correlate tea choices with weather conditions
4. **Health Tracking**: Track caffeine intake and health effects
5. **Community Features**: Share sessions, rate teas, follow other users
6. **Tea Vendor Integration**: Direct links to purchase teas
7. **Steeping Guides**: Built-in brewing guides for different tea types
8. **Tea Timer Presets**: Community-shared timer presets for specific teas
9. **Voice Commands**: "Hey Google/Siri, start a tea timer"
10. **Wearable Support**: Android Wear / Apple Watch complications and controls
11. **Desktop App**: Compose Multiplatform desktop app for Mac/Windows/Linux
12. **Web App**: Compose for Web dashboard for viewing stats

---

## Appendix A: Key Technology Choices Rationale

### Why Compose Multiplatform?
- **Single codebase**: Share UI and logic across Android and iOS
- **Modern declarative UI**: Better than XML layouts or UIKit
- **Growing ecosystem**: Strong support from JetBrains and community
- **Performance**: Native performance on both platforms

### Why Clean Architecture?
- **Testability**: Easy to test business logic in isolation
- **Maintainability**: Clear separation of concerns
- **Scalability**: Easy to add new features without breaking existing code
- **Team collaboration**: Clear boundaries for different team members

### Why Koin?
- **Lightweight**: Minimal overhead and simple API
- **KMP support**: Works well with Kotlin Multiplatform
- **Easy to learn**: Simpler than Dagger/Hilt
- **No code generation**: Faster build times

### Why Room?
- **Maturity**: Proven database solution for Android
- **KMP support**: Official KMP support in alpha
- **Type safety**: Compile-time verification of SQL queries
- **Reactive queries**: Flow-based reactive updates

### Why Firebase (Optional)?
- **Quick setup**: No backend development required
- **Real-time sync**: Built-in real-time data synchronization
- **Authentication**: Easy social login integration
- **Free tier**: Generous free tier for indie developers

### Why Offline-First?
- **User experience**: App works regardless of network
- **Performance**: Instant response to user actions
- **Reliability**: No dependency on network availability
- **Data ownership**: User data stored locally first

---

## Appendix B: Recommended Development Tools

### IDEs
- **Android Studio Ladybug (2024.2.1+)**: Primary IDE for development
- **Fleet**: Alternative lightweight IDE from JetBrains (optional)
- **Xcode 15+**: Required for iOS builds and testing

### Design Tools
- **Figma**: For UI/UX design and prototyping
- **Material Theme Builder**: For generating Material 3 color schemes

### Development Tools
- **Postman**: For testing Firebase REST APIs
- **Firebase Console**: For managing Firebase project
- **Android Studio Profiler**: For performance profiling
- **Xcode Instruments**: For iOS performance profiling

### Version Control
- **Git**: Version control
- **GitHub/GitLab**: Code hosting and CI/CD

### Project Management
- **Linear/Jira**: Task tracking and sprint planning
- **Notion**: Documentation and notes

### Testing Tools
- **Firebase Test Lab**: Cloud testing for Android
- **TestFlight**: Beta distribution for iOS
- **Crashlytics**: Crash reporting (optional)

---

## Appendix C: Code Style Guidelines

### Kotlin Coding Conventions
- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for automated formatting: `./gradlew ktlintFormat`
- Max line length: 150 characters

### Naming Conventions
- **Classes**: PascalCase (e.g., `TeaSession`, `LogTeaViewModel`)
- **Functions**: camelCase (e.g., `createSession()`, `getTeaById()`)
- **Variables**: camelCase (e.g., `teaName`, `brewingTime`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_BREWING_TIME`)
- **Composables**: PascalCase (e.g., `HomeScreen()`, `TeaCard()`)

### File Organization
- One class per file (unless nested classes)
- File name matches class name
- Group imports: stdlib → android → third-party → project

### Documentation
- KDoc for public APIs
- Inline comments for complex logic
- README.md in each module

---

## Contact & Support

For questions or clarifications about this architecture plan, please reach out to the development team.

---

**Document Version**: 1.0
**Last Updated**: 2026-01-21
**Status**: Ready for Review
