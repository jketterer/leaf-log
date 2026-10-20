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
./gradlew :androidApp:assembleDebug
```

Every `@Preview` is a smoke test: `PreviewRenderTest` renders it light, dark and at 2× font scale,
in inspection mode as in the IDE, and fails if it crashes. Build preview data around
`PreviewInstant`, and read the clock in composables through `currentInstant()`, so a preview looks
the same every day.

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
├── models/               # Domain entities (Tea, SessionWithSteeps, etc.)
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
- Return `Result<T>` when the operation can fail (writes, validation, state transitions). Reads and
  pure calculations that cannot fail return the value directly
- Complex business logic (calculations, validations, multi-step operations) belongs in use cases,
  NOT ViewModels
- Should NEVER be simple wrappers for a lone single repository function
    - ViewModels can use repository calls directly instead
- Example: `RecomputeStatsUseCase` rebuilds a tea's and a method's stats from their sessions

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

**Room Database (Version 10)**

- Entities in `data/local/database/entities/`
- DAOs in `data/local/database/dao/`

**Key Entities**

- `TeaEntity`: Tea types and details
- `TeaSessionEntity`: Brewing sessions
    - `parentSessionId`: Links child steeps to parent session
    - `steepNumber`: Order of steeps (1 = parent/first steep)
    - `rating`: Individual steep rating
    - Session fields (vessel, water type, leaf, location, `usedConfigurationId`): parent row only,
      NULL on children
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
- The domain reads them as `SessionWithSteeps(session, steeps)`; `Session.id` is steep 1's id
- A session's rating is `effectiveRating`, computed from its steeps and never stored
- Every session write goes through a use case in `usecases/session/`, inside a `Transactor`

## Platform-Specific Code

Code in `commonMain/` is shared across platforms. Platform-specific implementations:

- `androidMain/`: Android-specific (notifications, etc.)
- `iosMain/`: iOS-specific (notifications, etc.)

Use `expect`/`actual` pattern for platform differences.

## Important Conventions

- Always use trailing commas for lists
- Always include at least one @Preview composable function when creating UI components/screens
- UI text you write or edit goes in `composeResources/values/strings.xml`, read with
  `stringResource`; anything with a count is a `<plurals>` read with `pluralStringResource`, never
  `"(s)"` or an `if (n == 1)`
- Dates shown to the user go through `TimeFormatter.formatDate` with a `DateSkeleton`, never a
  pattern like `"MMM d"` or a month-name array
- Corners come from `MaterialTheme.shapes` (4/8/12/16/28dp), never a `RoundedCornerShape` literal;
  circles use `CircleShape`
- An action that loses data asks through `ConfirmDestructiveDialog`. A form with unsaved changes
  wraps its content in `DiscardChangesGuardDialog`, which guards the toolbar arrow and system back
  alike
- A group of content is titled by `SectionHeader`, written sentence case in `strings.xml` (it
  uppercases). Missing content, a failed lookup or a search with no hits shows `EmptyState`
- A rating is shown by `RatingDisplay` (null reads "Unrated") and chosen with `RatingSelector`. A
  bare rating number goes through `formatRating`

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
- A screen with route args takes its route and builds its ViewModel with
  `koinViewModel { parametersOf(route) }`; the ViewModel takes the route as a constructor param
  and loads in `init`, never from a `LaunchedEffect` that sends a load intent. Nav3 disposes an
  entry's composition while keeping its ViewModel, so such an effect re-runs on every return
- Snackbars go through the ViewModel's `UiMessages`, shown by one `UiMessageEffect` per screen; a
  failed operation calls `messages.failure(e, Res.string.couldnt_…)`, never puts `e.message`
  in State. A failure the user should hear the reason for is a `DomainFailure`
- State should be immutable data classes

### Composable Organization

A screen file holds the screen: the scaffold, state wiring, and the list of sections it renders. It
should NOT accumulate private composables until it becomes unreadable.

When a screen grows past a few hundred lines, extract:

- **Dialogs and widgets that aren't specific to one screen** into
  `presentation/ui/components/[area]/Name.kt` as public composables, each with a
  `modifier: Modifier = Modifier` parameter and its own @Preview
- **Sections that only make sense for one feature** into `screens/[feature]/[Feature]Sections.kt` as
  `internal` composables (top-level `private` is file-scoped and won't cross files)
- **Dialog call-sites** into a single private `[Feature]Dialogs(state, onIntent)` composable

Guidelines:

- Sections take the specific values they render, not the whole State - this keeps them previewable
  and avoids recomposing on unrelated state changes
- A temperature, volume or weight is shown with `formatTemperature`, `formatVolume` or
  `formatWeight`, which read Settings' units from `LocalUnits`; State never carries units for
  display. A form passes its unit to `UnitInputField`, and a screen that lets the user switch a
  shown unit holds the switch locally behind a `UnitSwitchChip` (or a `UnitSwitchIcon` in a stat
  tile's corner, where the value already shows the unit), never writing Settings
- Presets, `remember` blocks, and `LocalFocusManager` belong to the section that uses them
- Extraction should be a pure move - behavior does not change
- A preview of an `AlertDialog` renders blank, because the dialog is hosted in its own window that
  the preview renderer does not capture. Put the dialog body in a private `[Name]DialogContent`
  composable and preview that inside `DialogPreviewContainer` (`components/common/`)

Example: `screens/log/` splits into `LogTeaScreen.kt` (scaffold + dialogs) and `LogTeaSections.kt`
(form sections), with the tea search, quick-add, and completion dialogs living in `components/`.

### State Ownership in Composables

**A composable may own state it originates. It may not own state seeded from a parameter.**

*Originated* state - a menu's expanded flag, a search bar's visibility, an animation trigger, a
sheet's current mode - has no correct value other than what the user just did, and nothing outside
the composable can observe it. Keep it local with `remember`. Hoisting it into a ViewModel buys a
state field, an intent, and a `when` branch in exchange for nothing.

*Derived* state - `remember { mutableStateOf(someParameter) }` - has two sources of truth that
silently diverge, because the local copy cannot track the parameter after first composition. Fix it
one of two ways:

1. **Key the remember** so it re-seeds: `remember(source) { mutableStateOf(source) }`. Pick the key
   by identity, not by the field being edited - `remember(session.id)`, NOT
   `remember(session.notes)`, or a re-emit of the same session wipes what the user is typing
2. **Hoist** to the owner: take `value` + `onValueChange` and let the caller hold it

Prefer keying. Hoist when the state is genuinely the caller's.

Do NOT hoist text-field state that a domain type cannot round-trip. `""` and `"0"` are different to
a typist but identical as a `Duration`, so `DurationPicker` keeps its field text local and exposes
the parsing as pure `internal` functions (`parseDurationFields`, `durationToFields`) instead.
Extract the logic, not the state - the pure functions are the part worth testing.

Symptoms that this rule is being broken: a `syncKey` parameter, a `...Synced` latch, or a
`LaunchedEffect` whose only job is copying a parameter into local state.

### Database Changes

When modifying entities:

1. Update entity in `data/local/database/entities/`
2. Update mapper in `data/mappers/`
3. Update domain model in `domain/models/`

### Code Organization

- One feature per package (e.g., `screens/timer/` contains all timer-related files)
- Group related files together (Screen, ViewModel, State, Intent)
- Use descriptive names: `RecomputeStatsUseCase` over `UpdateStatsUseCase`
