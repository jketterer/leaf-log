# Architecture & System Design Audit

## Audit Scope

**Date:** 2026-03-17
**Codebase:** Leaf Log — Kotlin Multiplatform tea brewing tracker (Android + iOS)

### Layers Reviewed

- **Domain layer** — models, repository interfaces, use cases, services
- **Data layer** — Room entities, DAOs, mappers, repository implementations
- **Presentation layer** — ViewModels, composable components, navigation
- **DI layer** — Koin module definitions

### Methodology

1. Exhaustive grep/search across all source files for anti-patterns
2. Cross-layer import analysis to verify architectural boundaries
3. Entity/model/mapper consistency review
4. Component file audit against project conventions

---

## Findings & Resolutions

| # | Severity | Finding | Files | Resolution |
|---|----------|---------|-------|------------|
| 1 | Low | 15 `println()` calls used for logging/debugging | 12 files across data, domain, presentation | Added Kermit (`co.touchlab:kermit:2.0.8`) dependency; replaced with `Logger.w()`. Deleted debug-only prints. |
| 2 | Low | Stale TODO comment in TeaTypeDao | `TeaTypeDao.kt` | Removed — the pattern it described is already in place. |
| 3 | High | `ExportDataUseCase` and `ImportDataUseCase` import 13 symbols from the data layer | `domain/usecases/data/` | Created `DataExportRepository` interface in domain, moved logic to `DataExportRepositoryImpl` in data. Deleted use cases. Updated DI and SettingsViewModel. |
| 4 | Medium | `BrewingConfigurationEntity` uses `Long` for timestamp fields while all other entities use `Instant` | `BrewingConfigurationEntity.kt`, `BrewingConfigurationMapper.kt` | Changed `lastUsedAt`, `createdAt`, `updatedAt` from `Long` to `Instant`. Simplified mapper to direct assignment. |
| 5 | Medium | `BrewingConfigurationEntity.waterType` is `String` while `TeaSessionEntity` uses `WaterType` enum | `BrewingConfigurationEntity.kt`, `BrewingConfigurationMapper.kt` | Changed to `WaterType` enum. Room TypeConverter handles serialization. Removed manual `.valueOf()`/`.name` in mapper. |
| 6 | Low | `BrewingVessel` domain model missing `syncStatus` field (present in `Tea` and `TeaSession`) | `BrewingVessel.kt`, `BrewingVesselMapper.kt` | Added `syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY` and updated mapper in both directions. |
| 7 | Low | Unused `@Delete` hard-delete methods in DAOs alongside active soft-delete pattern | `TeaDao.kt`, `TeaSessionDao.kt`, `TeaTypeDao.kt` | Verified unreferenced via grep, removed methods and unused imports. |
| 8 | Low | 11 composable component files missing `@Preview` functions (project convention) | 11 files under `presentation/ui/components/` | Added `@Preview` composables with realistic test data, light/dark variants where applicable. |

---

## Strengths

The codebase demonstrates strong architectural discipline:

- **Zero framework imports in the domain layer** (post-audit) — clean boundary between business logic and infrastructure
- **Consistent MVI pattern** across all 16 screens — every feature has Screen, ViewModel, State, Intent
- **47 use cases** with `Result<T>` return types — proper error handling without exceptions leaking
- **Repository pattern** with interfaces in domain, implementations in data — fully invertible dependencies
- **Proper `expect`/`actual`** for platform-specific code (notifications, file storage, database)
- **Room TypeConverters** for `Instant`, `Duration`, `WaterType`, `SyncStatus` — type-safe persistence
- **Koin DI** organized by concern: `AppModule`, `RepositoryModule`, `UseCaseModule`, `PlatformModule`
- **Multi-steep session model** with parent/child relationship and average rating propagation
- **Soft-delete pattern** consistently applied across all entities with `deletedAt` timestamps

---

## Deferred Items

| Item | Rationale |
|------|-----------|
| `@ColumnInfo` annotation normalization | Would require a database migration for each renamed column. Benefit does not justify the migration cost at this stage. |
| Status string literals in DAO `@Query` | Room KMP does not support enum parameters in raw SQL queries. The string literals (`'COMPLETED'`, `'IN_PROGRESS'`) are correct for the current Room version. |
| Error categorization for user-facing messages | Requires UX design decisions about which errors are user-actionable vs. silent. Deferred to a future feature initiative. |
| Accessibility improvements (content descriptions, semantics) | Separate initiative with its own scope. Not an architecture concern. |
