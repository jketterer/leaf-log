# Architecture

Technical architecture and infrastructure for the Leaf Log KMP project.

## Tech Stack & Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Kotlin | 2.3.10 | Language |
| Compose Multiplatform | 1.10.2 | UI framework |
| Compose Material 3 | 1.10.0-alpha05 | Design system |
| AGP (Android Gradle Plugin) | 9.1.0 | Android build |
| KSP | 2.3.2 | Annotation processing |
| Room | 2.8.4 | Database ORM |
| SQLite (bundled) | 2.6.2 | Database engine |
| DataStore | 1.2.1 | Preferences persistence |
| Koin | 4.2.0-RC2 | Dependency injection |
| Navigation 3 UI | 1.0.0-alpha06 | Multiplatform navigation |
| Lifecycle ViewModel | 2.9.6 | ViewModel compose integration |
| Lifecycle Runtime Compose | 2.9.6 | Lifecycle-aware compose |
| Lifecycle Process | 2.10.0 | Process lifecycle |
| Lifecycle ViewModel Nav3 | 2.10.0-beta01 | ViewModel + Nav3 integration |
| Lifecycle (KMP) | 2.10.0 | Multiplatform lifecycle |
| kotlinx-serialization JSON | 1.10.0 | JSON serialization |
| kotlinx-datetime | 0.7.1 | Multiplatform date/time |
| Coil | 3.4.0 | Image loading (with Ktor3 network) |
| Compose Icons (Feather) | 1.1.1 | Feather icon set |
| Compose Icons (Font Awesome) | 1.1.1 | Font Awesome icon set |
| FileKit | 0.13.0 | File picker dialogs |
| Kermit | 2.0.8 | Multiplatform logging |
| AndroidX Activity Compose | 1.13.0 | Activity integration |
| AndroidX AppCompat | 1.7.1 | Backward compat |
| AndroidX Core | 1.18.0 | Core utilities |

### Testing Dependencies

| Dependency | Version |
|---|---|
| JUnit | 4.13.2 |
| Kotlin Test | 2.3.10 |
| AndroidX Espresso | 3.7.0 |
| AndroidX Test Ext | 1.3.0 |
| Koin Test | 4.2.0-RC2 |

## Module Structure

```
leaf-log/
├── shared/                    # Kotlin Multiplatform library (all shared code)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/        # Shared across all platforms
│       ├── androidMain/       # Android-specific implementations
│       └── iosMain/           # iOS-specific implementations
├── androidApp/                # Android application module
│   ├── build.gradle.kts
│   └── src/main/
├── iosApp/                    # Xcode project for iOS
│   ├── iosApp.xcodeproj/
│   └── iosApp/
├── gradle/
│   └── libs.versions.toml     # Centralized version catalog
└── docs/
```

All shared Kotlin code lives under `shared/src/commonMain/kotlin/dev/jketterer/leaflog/`.

## Build Configuration

### Gradle Plugins

- `kotlin-multiplatform` (2.3.10)
- `compose-multiplatform` (1.10.2)
- `compose-compiler` (2.3.10)
- `android-application` (9.1.0) - applied to androidApp
- `android-library` (9.1.0) - applied to shared
- `ksp` (2.3.2) - Room code generation
- `androidx-room` (2.8.4) - Room schema management
- `kotlinx-serialization` (2.3.10) - Navigation route serialization

### Android Targets

- Compile SDK: 36
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- Java Target: JVM 11

### iOS Targets

- `iosArm64` - Physical devices
- `iosSimulatorArm64` - Simulator

Framework output: `ComposeApp` (static framework).

### KSP Configuration

Room uses KSP for code generation on all platforms:
- `kspAndroid`
- `kspIosSimulatorArm64`
- `kspIosArm64`

Room schema directory: `$projectDir/schemas`

### Compiler Options

```
freeCompilerArgs: -Xexpect-actual-classes
```

## Clean Architecture Layers

```
shared/src/commonMain/kotlin/dev/jketterer/leaflog/

├── data/                              # Data Layer
│   ├── local/
│   │   ├── database/
│   │   │   ├── LeafLogDatabase.kt     # Room database (expect constructor)
│   │   │   ├── Converters.kt          # Type converters
│   │   │   ├── Migrations.kt          # Schema migrations
│   │   │   ├── dao/                   # 5 DAOs
│   │   │   └── entities/              # 5 entity classes
│   │   ├── preferences/
│   │   │   └── PreferencesDataStore.kt  # expect class
│   │   ├── ImageStorage.kt            # expect class
│   │   ├── ZipArchiver.kt             # expect class
│   │   └── FileOperations.kt          # expect class
│   ├── mappers/                       # 5 entity <-> domain mappers
│   ├── models/
│   │   └── LeafLogExportData.kt       # Serializable export format
│   └── repositories/                  # 7 repository implementations
│
├── domain/                            # Domain Layer
│   ├── models/                        # Domain entities, enums, formatters
│   ├── repositories/                  # 7 repository interfaces
│   ├── usecases/                      # 43 use cases in subdirectories
│   │   ├── (root)                     # Tea use cases + InitializeDefaults
│   │   ├── session/                   # Session + analytics use cases
│   │   ├── timer/                     # Timer use cases
│   │   ├── vessel/                    # Vessel use cases
│   │   └── configuration/            # Brewing configuration use cases
│   └── services/                      # TimerService, TimerNotificationService
│
├── presentation/                      # Presentation Layer
│   └── ui/
│       ├── navigation/                # Nav routes, config, deep links
│       ├── screens/                   # Feature screens (MVI)
│       │   ├── home/
│       │   ├── log/
│       │   ├── collection/
│       │   ├── history/
│       │   ├── timer/
│       │   ├── quicktimer/
│       │   ├── steepcomplete/
│       │   ├── vessel/
│       │   ├── settings/
│       │   ├── analytics/
│       │   ├── configuration/
│       │   └── teatype/
│       ├── components/                # Reusable UI components
│       │   ├── common/
│       │   ├── home/
│       │   ├── collection/
│       │   ├── session/
│       │   ├── timer/
│       │   ├── vessel/
│       │   ├── configuration/
│       │   ├── history/
│       │   └── analytics/
│       └── theme/                     # Material 3 theming
│
└── di/                                # Dependency Injection
    ├── AppModule.kt                   # ViewModels + TimerService
    ├── RepositoryModule.kt            # Repository bindings
    ├── UseCaseModule.kt               # Use case factories
    └── PlatformModule.kt              # expect function
```

## MVI Pattern

Each feature screen follows Model-View-Intent:

### State

Immutable data class representing all UI state for a screen.

```kotlin
data class HomeState(
    val greeting: String = "Good day",
    val dailyStats: DailyStats = DailyStats(),
    val recentSessionsWithTea: List<SessionWithTeaData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // ...
)
```

### Intent

Sealed interface defining all user actions.

```kotlin
sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data object Refresh : HomeIntent
    data class SessionClicked(val sessionId: String) : HomeIntent
    // ...
}
```

### ViewModel

Extends `androidx.lifecycle.ViewModel`. Processes intents and updates state via `StateFlow`.

```kotlin
class HomeViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _navEvents = Channel<HomeNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
            // ...
        }
    }
}
```

### Conventions

- **State updates**: `_state.update { it.copy(...) }`
- **Coroutines**: `viewModelScope` for all async work
- **Navigation**: Emitted via `Channel<NavEvent>` + `receiveAsFlow()` pattern
- **Screens collect** nav events in `LaunchedEffect`, triggering navigation callbacks

## Dependency Injection (Koin)

### AppModule

Registers ViewModels and singleton services.

```kotlin
val appModule = module {
    // Application-level scope (survives ViewModels)
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Singleton timer service
    single { TimerService(get(), get(), get(), get()) }

    // ViewModels (23 total)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LogTeaViewModel)
    viewModelOf(::TeaCollectionViewModel)
    viewModelOf(::TeaDetailViewModel)
    viewModelOf(::EditTeaViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SessionDetailViewModel)
    viewModelOf(::EditSessionViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::QuickTimerViewModel)
    viewModelOf(::SteepCompleteViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::VesselListViewModel)
    viewModelOf(::VesselDetailViewModel)
    viewModelOf(::EditVesselViewModel)
    viewModelOf(::TeaTypeListViewModel)
    viewModelOf(::EditTeaTypeViewModel)
    viewModelOf(::CreateBrewingConfigurationViewModel)
    // ...
}
```

### RepositoryModule

Binds implementations to interfaces using `singleOf`.

```kotlin
val repositoryModule = module {
    singleOf(::TeaTypeRepositoryImpl) bind TeaTypeRepository::class
    singleOf(::BrewingVesselRepositoryImpl) bind BrewingVesselRepository::class
    singleOf(::BrewingConfigurationRepositoryImpl) bind BrewingConfigurationRepository::class
    singleOf(::TeaRepositoryImpl) bind TeaRepository::class
    singleOf(::TeaSessionRepositoryImpl) bind TeaSessionRepository::class
    singleOf(::PreferencesRepositoryImpl) bind PreferencesRepository::class
    singleOf(::DataExportRepositoryImpl) bind DataExportRepository::class
}
```

### UseCaseModule

Registers all 43+ use cases as factories using `factoryOf`.

```kotlin
val useCaseModule = module {
    factoryOf(::InitializeDefaultsUseCase)
    factoryOf(::CreateTeaUseCase)
    factoryOf(::CreateSessionUseCase)
    factoryOf(::StartTimerUseCase)
    factoryOf(::SaveBrewingConfigurationUseCase)
    // ... (43 total)
}
```

### PlatformModule

`expect fun platformModule(): Module` — implemented per platform.

**Android** (`androidMain`):
- Room database construction via `Room.databaseBuilder` with `BundledSQLiteDriver` + `Dispatchers.IO`
- Database file: `leaflog.db`
- 5 DAO singletons from database
- `TimerNotificationServiceImpl(context)` — Android notifications
- `TimerLifecycleHandler(context)` — Foreground service bridge
- `ImageStorage(context)`, `ZipArchiver()`, `PreferencesDataStore(context)`

**iOS** (`iosMain`):
- Room database at `NSHomeDirectory() + "/Documents/leaflog.db"`
- Same 5 DAO singletons
- `TimerNotificationServiceImpl()` — NSUserNotifications + Live Activities
- `TimerLifecycleHandler()` — No-op (iOS handles background differently)
- `ImageStorage()`, `ZipArchiver()`, `PreferencesDataStore()` (NSUserDefaults)

## Platform-Specific Code (expect/actual)

| Class | Purpose | Android | iOS |
|---|---|---|---|
| `LeafLogDatabaseConstructor` | Room DB construction | KSP-generated | KSP-generated |
| `PreferencesDataStore` | User preferences | DataStore<Preferences> | NSUserDefaults |
| `TimerLifecycleHandler` | Foreground service bridge | Starts/stops foreground service | No-op |
| `TimerNotificationServiceImpl` | Timer notifications | NotificationCompat + channels | UNUserNotificationCenter + Live Activities |
| `ImageStorage` | Image file I/O | App internal files dir | Documents directory |
| `ZipArchiver` | ZIP compression | java.util.zip | Platform ZIP impl |
| `TimeFormatter` | Locale-aware formatting | JVM DateTimeFormatter | NSDateFormatter |
| `FileOperations` | File picker integration | Platform-specific | Platform-specific |
| `platformModule()` | DI module | Android Context-based | NSHomeDirectory-based |

## Navigation System

### Navigation 3

Uses `androidx.navigation3` with `NavKey` serialization.

**Routes** defined as a serializable sealed interface in `NavRoute.kt`:

| Route | Parameters | Description |
|---|---|---|
| `HomeRoute` | — | Main dashboard |
| `CollectionRoute` | — | Tea & vessel collection |
| `HistoryRoute` | `filterTeaTypeId?`, `filterTeaId?`, `filterDateStart?`, `filterDateEnd?` | Session history with filters |
| `AnalyticsRoute` | — | Statistics & insights |
| `MoreRoute` | — | More menu / settings hub |
| `SettingsRoute` | — | App preferences |
| `LogTeaRoute` | `teaId?`, `vesselId?`, `configurationId?` | Log session wizard |
| `TeaDetailsRoute` | `teaId` | Tea detail view |
| `EditTeaRoute` | `teaId?` | Create/edit tea |
| `SessionDetailsRoute` | `sessionId` | View session details |
| `EditSessionRoute` | `sessionId`, `editFullSession` | Edit session/steep |
| `TimerRoute` | `sessionId` | Brewing timer |
| `QuickTimerRoute` | `durationSeconds` | Quick timer |
| `SteepCompleteRoute` | `sessionId` | Post-steep completion |
| `VesselListRoute` | — | Vessel collection |
| `VesselDetailRoute` | `vesselId` | Vessel details |
| `EditVesselRoute` | `vesselId?` | Create/edit vessel |
| `TeaTypeListRoute` | — | Tea type management |
| `EditTeaTypeRoute` | `teaTypeId?` | Create/edit tea type |
| `CreateBrewingConfigurationRoute` | `teaId`, `configurationId?` | Save brewing method |

**Total: 20 routes.**

### NavConfig.kt

Registers serializers for all route types via `SavedStateConfiguration` with `SerializersModule` polymorphic registration.

### AppNavigation.kt

- Uses `rememberNavBackStack()` with `navSerializationConfig`
- Initial route: `HomeRoute`
- Entry point: `NavDisplay` composable with `entryProvider` DSL
- Transitions: Horizontal slide animations (in/out)
- Bottom navigation bar shown for 4 main tabs (Home, Collection, History, Analytics)
- Navigation events passed via lambdas to screen composables

### Deep Link Handling

`DeepLinkHandler` singleton using `StateFlow`. Called from notification taps to navigate to timer/session screens. Ensures `HomeRoute` is at the bottom of the back stack.

## Data Persistence

### Room Database

**Version**: 2 | **Schema export**: enabled

5 entities, 5 DAOs. See [DATA_MODEL.md](DATA_MODEL.md) for complete schema.

Database file: `leaflog.db`

Driver: `BundledSQLiteDriver` with `Dispatchers.IO` query context.

### DataStore (User Preferences)

**Android**: `androidx.datastore.preferences` with `DataStore<Preferences>`

**iOS**: `NSUserDefaults` wrapped in `PreferencesDataStore`

**Preference Keys**: `TEMPERATURE_UNIT`, `VOLUME_UNIT`, `TEA_SORT_OPTION`, `ANALYTICS_PERIOD`, `DEFAULT_WATER_TYPE`

### Image Storage

Platform-specific file storage for photos:
- Subdirectories: `vessel_images/`, `tea_photos/`, `session_images/`
- **Android**: App's internal files directory
- **iOS**: Documents directory
- Images referenced by absolute path in entity photo lists
- Import/export remaps paths between absolute and archive-relative

## Timer System

### TimerService (Domain Service)

Singleton managing timer state and countdown. Lives in application-level `CoroutineScope` (survives ViewModels).

```kotlin
class TimerService(
    coroutineScope: CoroutineScope,
    notificationService: TimerNotificationService,
    saveTimerStateUseCase: SaveTimerStateUseCase,
    lifecycleHandler: TimerLifecycleHandler,
)
```

**State**: `StateFlow<TimerState>` — exposes current timer to all observers.

**Countdown Loop** (100ms ticks):
1. Calculate elapsed from `startedAt` using `Clock.System.now()`
2. Remaining = `totalDuration - elapsed` (clamped to zero)
3. Update state
4. Show notification (~per second throttle)
5. If remaining == 0: mark COMPLETE, persist state, show completion notification

### Timer Notifications

**Android**:
- **Channels**: `timer_running` (IMPORTANCE_LOW, ongoing) and `timer_complete` (IMPORTANCE_HIGH)
- **Foreground notification**: Chronometer widget, progress bar, tea name, steep number
- **Completion notification**: High priority alert ("TeaName is ready!")
- **Foreground service**: `TimerForegroundService` keeps process alive

**iOS**:
- **Live Activities**: Dynamic Island countdown display via `LiveActivityServiceHolder`
- **Scheduled notification**: `UNTimeIntervalNotificationTrigger` fires after remaining seconds
- **No foreground service**: Scheduled notification fires independently from app process
- **Tap handling**: `didReceiveNotificationResponse` deep links to timer screen

### Timer State Persistence

Timer state (status, startedAt, pausedAt, remainingMs) is persisted to the session entity via `SaveTimerStateUseCase`. On app relaunch, `RestoreTimerStateUseCase` rehydrates the timer, recalculating remaining time if the timer was running during suspension.

## Import/Export System

### Export

1. Query all data from all repositories
2. Collect images: walk session/tea/vessel photos, build path remap
3. Remap entity photo paths to archive-relative paths (`images/[category]/[index].[ext]`)
4. Serialize to `LeafLogExportData` JSON (pretty-printed)
5. Create ZIP containing `data.json` + image files
6. Return temp ZIP file path

### Import

1. Detect format: ZIP magic bytes (`0x50 0x4B`) vs legacy JSON
2. **ZIP**: Extract entries, restore images to device storage, build reverse path remap
3. **JSON (legacy)**: Parse directly, clear photo paths (no images available)
4. Remap entity photo paths from archive-relative to new absolute paths
5. Upsert entities in dependency order: tea types → vessels → teas → sessions (parents then children) → configurations
6. Return `ImportResult` with counts per entity type

### Export Data Format

```kotlin
@Serializable
data class LeafLogExportData(
    val version: Int = 1,
    val exportedAt: Long,
    val teaTypes: List<TeaTypeEntity>,
    val brewingVessels: List<BrewingVesselEntity>,
    val teas: List<TeaEntity>,
    val teaSessions: List<TeaSessionEntity>,
    val brewingConfigurations: List<BrewingConfigurationEntity>,
)
```

## Key Conventions

### Soft Deletes

All entities have `deletedAt: Instant?`. Logical deletion sets this timestamp instead of removing rows. DAO queries filter `WHERE deletedAt IS NULL`. Supports future cloud sync reconciliation.

### Unit Storage

- **Temperature**: Always stored in Celsius. Converted to/from Fahrenheit for display using `TemperatureUnit`.
- **Volume**: Always stored in milliliters. Converted to/from fluid ounces for display using `VolumeUnit`.
- **User preferences**: `TemperatureUnit` and `VolumeUnit` persisted in DataStore.
- **Default units**: Fahrenheit / fluid ounces (imperial preset).

### Sync Infrastructure

All entities have a `syncStatus: SyncStatus` field with values: `SYNCED`, `PENDING`, `SYNCING`, `CONFLICT`, `ERROR`, `LOCAL_ONLY`. Currently all records are `LOCAL_ONLY`. Infrastructure is in place for future cloud sync without schema changes.

### Error Handling

- Use cases return `Result<T>` for error wrapping
- ViewModels catch errors and update state with `error: String?`
- Non-critical operations (like tea stats updates) log failures instead of throwing

### ID Generation

- Entities use `UUID.randomUUID().toString()` for IDs
- Configurations use `"config_{epochMs}_{random0-9999}"` format

### Trailing Commas

All list/argument definitions use trailing commas per Kotlin convention.
