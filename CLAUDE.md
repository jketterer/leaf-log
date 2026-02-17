# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

**Leaf Log** is a Kotlin Multiplatform tea brewing tracker for Android and iOS. It uses Compose
Multiplatform for UI, Room for local storage, and follows Clean Architecture with MVI pattern.

## Build Commands

### Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug
```

## Architecture

### Clean Architecture Layers

```
presentation/ (UI Layer - MVI Pattern)
├── ui/
│   ├── screens/           # Screen-level composables
│   │   ├── [feature]/     # Feature-specific screens
│   │   │   ├── Screen.kt       # Composable UI
│   │   │   ├── ViewModel.kt    # State management
│   │   │   ├── State.kt        # UI state data class
│   │   │   └── Intent.kt       # User actions/events
│   ├── components/        # Reusable UI components
│   ├── navigation/        # Navigation setup
│   └── theme/            # Material 3 theming

domain/ (Business Logic Layer)
├── models/               # Domain entities (Tea, TeaSession, etc.)
├── repositories/         # Repository interfaces
├── usecases/            # Business logic operations
│   ├── session/         # Session-related use cases
│   ├── timer/           # Timer-related use cases
│   └── vessel/          # Vessel-related use cases
└── services/            # Domain services (TimerService, etc.)

data/ (Data Layer)
├── local/
│   └── database/
│       ├── dao/         # Room DAOs
│       ├── entities/    # Database entities
│       ├── Migrations.kt # Database migrations
│       └── LeafLogDatabase.kt
├── mappers/             # Entity ↔ Domain model mappers
└── repositories/        # Repository implementations

di/ (Dependency Injection)
├── AppModule.kt         # ViewModels and services
├── RepositoryModule.kt  # Repository implementations
├── UseCaseModule.kt     # Use case definitions
└── PlatformModule.kt    # Platform-specific (DAOs, DB)
```

### Key Patterns

**MVI (Model-View-Intent)**

- **State**: Immutable data class representing UI state
- **Intent**: Sealed interface of user actions
- **ViewModel**: Processes intents, updates state via StateFlow
- ViewModels expose `state: StateFlow<State>` and `onIntent(intent: Intent)` function

**Use Cases**

- Single responsibility: one business operation per use case
- Return `Result<T>` for error handling
- Complex business logic (calculations, validations, multi-step operations) belongs in use cases,
  NOT ViewModels
- Should NEVER be simple wrappers for a lone single repository function
    - ViewModels can use repository calls directly instead
- Example: `UpdateAverageRatingUseCase` handles rating calculations across steeps

**Repository Pattern**

- Interfaces in `domain/repositories/`
- Implementations in `data/repositories/`
- Handle data persistence and retrieval

**Navigation**

- Uses Compose Navigation 3
- Routes defined in `presentation/ui/navigation/NavRoute.kt` as serializable sealed interface
- Navigation setup in `AppNavigation.kt`

**Conventions**

### Database

**Room Database (Version 1)**

- Entities in `data/local/database/entities/`
- DAOs in `data/local/database/dao/`
- When adding fields to entities, don't worry about modifying database versions

**Key Entities**

- `TeaEntity`: Tea types and details
- `TeaSessionEntity`: Brewing sessions
    - `parentSessionId`: Links child steeps to parent session
    - `steepNumber`: Order of steeps (1 = parent/first steep)
    - `rating`: Individual steep rating
    - `averageRating`: Calculated average across all steeps (parent sessions only)
- `TeaTypeEntity`: Tea categories
- `BrewingVesselEntity`: Brewing vessels

### Dependency Injection (Koin)

Modules are split by concern:

- `AppModule`: ViewModels, singleton services
- `RepositoryModule`: Repository implementations
- `UseCaseModule`: Use case factories
- `PlatformModule`: Platform-specific (Android/iOS)

Use `factoryOf(::ClassName)` for use cases and repositories, `viewModelOf(::ViewModelClass)` for
ViewModels.

### Multi-Steep Sessions

Tea sessions can have multiple steeps:

- First steep = parent session (`parentSessionId = null`, `steepNumber = 1`)
- Subsequent steeps are children (`parentSessionId = parent.id`, `steepNumber = 2, 3, ...`)
- Parent sessions track `averageRating` (truncated to 1 decimal)
- Use `UpdateAverageRatingUseCase` to recalculate averages when steeps change

## Platform-Specific Code

Code in `commonMain/` is shared across platforms. Platform-specific implementations:

- `androidMain/`: Android-specific (notifications, etc.)
- `iosMain/`: iOS-specific (notifications, etc.)

Use `expect`/`actual` pattern for platform differences.

## Important Conventions

- Always use trailing commas for lists
- Always include at least one @Preview composable function when creating UI components/screens

### When Creating Use Cases

Extract complex business logic into use cases when:

- Logic involves calculations, multi-step operations, or business rules
- Logic is reused across multiple ViewModels
- Logic needs to be testable in isolation

Do NOT extract simple repository calls into use cases. If it doesn't add value, let the ViewModel
call the repository directly.

Example: Rating calculations, session completion flows, steep management.

### ViewModel Best Practices

- ViewModels manage UI state, NOT business logic
- Keep ViewModels thin - delegate to use cases
- Use sealed interfaces for Intents and Navigation Events
- State should be immutable data classes

### Database Changes

When modifying entities:

1. Update entity in `data/local/database/entities/`
2. Update mapper in `data/mappers/`
3. Update domain model in `domain/models/`

### Code Organization

- One feature per package (e.g., `screens/timer/` contains all timer-related files)
- Group related files together (Screen, ViewModel, State, Intent)
- Use descriptive names: `UpdateAverageRatingUseCase` over `UpdateRatingUseCase`
