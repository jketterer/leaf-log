# Leaf Log

A tea brewing tracker for Android and iOS, built with Kotlin Multiplatform and Compose
Multiplatform.

Log brewing sessions, time your steeps, manage your tea collection, and analyze your brewing habits.

## Features

- **Session Logging** — Record brewing parameters (temperature, water quantity, brewing time, tea
  quantity) with full pre-fill from saved configurations or previous sessions
- **Brew Timer** — Countdown timer with circular progress ring, quick-adjust buttons, and
  pause/resume. Persists across app backgrounding and process death
- **Multi-Steep Tracking** — Gongfu-style support with per-steep ratings and notes, building a
  steep-by-steep tasting journal
- **Quick Timer** — Start a countdown immediately and optionally fill in session details during or
  after
- **Tea Collection** — Manage your tea library with search, filters (favorites, by type), and sort
  options
- **Brewing Vessels** — Track your teapots, gaiwans, and other vessels with capacity info
- **Brewing Configurations** — Save and reuse brewing parameters for specific tea + vessel
  combinations
- **Session History** — Browse and filter past sessions by tea type, specific tea, date range, or
  rating
- **Analytics** — Charts and insights including brewing trends, tea type distribution, top teas,
  vessel usage, and a brewing activity heatmap (requires 10+ sessions)
- **iOS Live Activity** — Shows the active brew timer on the Lock Screen and Dynamic Island
- **Data Export/Import** — Full JSON export and import of all data

## Tech Stack

| Layer        | Technology                         |
|--------------|------------------------------------|
| UI           | Compose Multiplatform (Material 3) |
| Architecture | Clean Architecture + MVI           |
| Database     | Room (local)                       |
| DI           | Koin                               |
| Navigation   | Compose Navigation 3               |
| Language     | Kotlin Multiplatform               |

## Project Structure

```
composeApp/
├── src/
│   ├── commonMain/       # Shared code (UI, business logic, data layer)
│   ├── androidMain/      # Android-specific (notifications, platform services)
│   └── iosMain/          # iOS-specific (notifications, Live Activity bridge)
iosApp/                   # iOS app entry point and SwiftUI/Swift code
```

The shared code in `commonMain` follows Clean Architecture:

```
presentation/             # MVI pattern: Screen, ViewModel, State, Intent
domain/                   # Models, repository interfaces, use cases
data/                     # Room database, DAOs, entities, mappers, repository implementations
di/                       # Koin dependency injection modules
```

## Building

### Prerequisites

- JDK 17+
- Android Studio or IntelliJ IDEA with Kotlin Multiplatform plugin
- Xcode (for iOS builds)

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / IntelliJ IDEA.

### iOS

Open the `iosApp/` directory in Xcode and run from there, or use the run configuration in Android
Studio with the KMP plugin.

## License

All rights reserved.
