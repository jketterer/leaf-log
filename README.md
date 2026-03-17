# Leaf Log

A tea brewing companion for Android and iOS, built entirely with **Kotlin Multiplatform** and
**Compose Multiplatform**. Track your teas, time your steeps, and explore your brewing habits — all
from a single shared codebase.

This project demonstrates a full-scale mobile application with **Clean Architecture**, the **MVI
pattern**, and idiomatic KMP practices.

## Features

**Brew Timer** — Countdown timer with circular progress ring, quick-adjust buttons, and
pause/resume. Timer state persists across app backgrounding and process death via a singleton
`TimerService` that saves and restores state through dedicated use cases. On Android, an ongoing
notification with a custom layout shows a live countdown and progress bar. On iOS, an active brew
shows on the Lock Screen and Dynamic Island via Live Activities.

**Multi-Steep Tracking** — Gongfu-style session support where each steep is a child of a parent
session, with per-steep ratings, notes, and an automatically calculated average rating across all
steeps.

**Quick Timer** — Start a countdown immediately and fill in session details during or after brewing.

**Tea Collection** — Manage your tea library with search, filters (favorites, by type), sort
options,
and detailed tea profiles including origin, producer, and brewing defaults.

**Brewing Vessels** — Track teapots, gaiwans, and other vessels with capacity info.

**Brewing Configurations** — Save and reuse brewing parameters (temperature, water quantity, brew
time, tea quantity) for specific tea + vessel combinations. Configurations can be learned from
previous sessions.

**Session History** — Browse and filter past sessions by tea type, specific tea, date range, or
rating.

**Analytics** — Charts and insights including brewing trends, tea type distribution, top-rated teas,
vessel usage, and a brewing activity heatmap.

**Data Export/Import** — Full JSON export and import of all user data.

## Architecture

The project follows **Clean Architecture** with strict layer separation and the **MVI
(Model-View-Intent)** pattern for all UI state management.

```
┌─────────────────────────────────────────────────────────┐
│  Presentation (MVI)                                     │
│  Screen → Intent → ViewModel → State → Screen           │
│                        │                                │
│                  ┌─────┴──────┐                         │
│                  ▼            ▼                         │
│             Use Cases    Repositories                   │
│             (domain)      (domain)                      │
│                  │            │                         │
│                  ▼            ▼                         │
│              Domain       Data Layer                    │
│              Models    (Room, DataStore)                 │
└─────────────────────────────────────────────────────────┘
```

### MVI Pattern

Every screen follows a consistent MVI structure with four files:

| File           | Role                                                                   |
|----------------|------------------------------------------------------------------------|
| `Screen.kt`    | Composable UI — observes state, dispatches intents                     |
| `ViewModel.kt` | Processes intents, delegates to use cases, emits state via `StateFlow` |
| `State.kt`     | Immutable data class representing the complete UI state                |
| `Intent.kt`    | Sealed interface defining every possible user action                   |

One-time navigation events flow through a `Channel` exposed as a `Flow`, keeping them separate from
persistent UI state.

### Use Cases

Business logic lives in dedicated use cases organized by domain (session, timer, vessel,
configuration, data). Each use case has a single responsibility and returns `Result<T>` for
structured error
handling. Use cases are only introduced when they encapsulate meaningful logic — simple repository
calls go directly through the ViewModel.

Examples:

- `UpdateAverageRatingUseCase` — aggregates ratings across all steeps and calculates a truncated
  average
- `SaveTimerStateUseCase` / `RestoreTimerStateUseCase` — persist and restore timer state for
  backgrounding and process death
- `ExportDataUseCase` / `ImportDataUseCase` — serialize and deserialize the full user dataset

### Repository Pattern

Repository interfaces are defined in the domain layer with implementations in the data layer. The
domain layer has zero dependencies on Android, Room, or any framework — only pure Kotlin.

### Dependency Injection

[Koin](https://insert-koin.io/) modules are split by concern:

| Module             | Contents                                                 |
|--------------------|----------------------------------------------------------|
| `AppModule`        | ViewModels, `TimerService` singleton, coroutine scope   |
| `RepositoryModule` | Repository implementations (`singleOf` + `bind`)        |
| `UseCaseModule`    | Use cases (`factoryOf` for per-call instantiation)       |
| `PlatformModule`   | `expect`/`actual` for platform-specific dependencies     |

## Project Structure

```
shared/
└── src/
    ├── commonMain/              # All shared code
    │   ├── presentation/
    │   │   ├── ui/screens/      # Feature screens (MVI quad-file pattern)
    │   │   ├── ui/components/   # Reusable Compose components
    │   │   ├── ui/navigation/   # Navigation 3 routes and graph
    │   │   └── ui/theme/        # Material 3 theming
    │   ├── domain/
    │   │   ├── models/          # Domain entities
    │   │   ├── repositories/    # Repository interfaces
    │   │   ├── usecases/        # Use cases by domain area
    │   │   └── services/        # Domain services (TimerService)
    │   ├── data/
    │   │   ├── local/database/  # Room database, DAOs, entities, migrations
    │   │   ├── local/preferences/ # DataStore user preferences
    │   │   ├── repositories/    # Repository implementations
    │   │   └── mappers/         # Entity ↔ Domain model converters
    │   └── di/                  # Koin modules
    ├── androidMain/             # Android platform (notifications, services)
    └── iosMain/                 # iOS platform (notifications, Live Activity bridge)

androidApp/                      # Android app entry point
iosApp/                          # iOS app (SwiftUI entry point, Live Activity widget)
```

## Tech Stack

| Layer         | Technology                              |
|---------------|-----------------------------------------|
| Language      | Kotlin (Multiplatform)         |
| UI            | Compose Multiplatform (Material 3) |
| Architecture  | Clean Architecture + MVI       |
| Database      | Room                           |
| DI            | Koin                           |
| Navigation    | Compose Navigation 3           |
| Preferences   | DataStore                      |
| Date/Time     | kotlinx.datetime               |
| Image Loading | Coil                           |
| Build         | Gradle with version catalogs   |

**Targets:** Android (API 24+), iOS (arm64 + simulator)

## Building

### Prerequisites

- JDK 17+
- Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin
- Xcode 16+ (for iOS builds)

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / IntelliJ IDEA.

### iOS

Open `iosApp/` in Xcode and run, or use the KMP run configuration in Android Studio.

## License

All rights reserved.
