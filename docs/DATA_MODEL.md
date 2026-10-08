# Data Model

Complete data model reference for Leaf Log.

## Database Entities

Database: Room, version 2, schema export enabled.

### TeaEntity

**Table**: `tea`

| Field | Type | Constraints | Default |
|---|---|---|---|
| `id` | `String` | `@PrimaryKey` | — |
| `name` | `String` | Required | — |
| `teaTypeId` | `String` | FK → `tea_type(id)` RESTRICT | — |
| `origin` | `String?` | — | `null` |
| `producer` | `String?` | — | `null` |
| `purchaseDate` | `LocalDate?` | Stored as epoch days (Long) | `null` |
| `purchasePrice` | `Double?` | — | `null` |
| `stockAmount` | `Int?` | — | `null` |
| `defaultTemperatureCelsius` | `Int?` | — | `null` |
| `description` | `String?` | — | `null` |
| `photos` | `List<String>` | Stored as JSON string | `emptyList()` |
| `isFavorite` | `Boolean` | — | `false` |
| `totalSessions` | `Int` | Cached count | `0` |
| `averageRating` | `Float?` | Aggregate from sessions | `null` |
| `lastBrewedAt` | `Instant?` | Stored as epoch ms (Long) | `null` |
| `userId` | `String?` | For multi-user sync | `null` |
| `createdAt` | `Instant` | Stored as epoch ms | — |
| `updatedAt` | `Instant` | Stored as epoch ms | — |
| `deletedAt` | `Instant?` | Soft delete | `null` |
| `syncStatus` | `SyncStatus` | Stored as String | `LOCAL_ONLY` |

**Indices**: `teaTypeId`, `name`, `isFavorite`, `userId`, `deletedAt`, `lastBrewedAt`

**Foreign Keys**: `teaTypeId` → `tea_type(id)` ON DELETE RESTRICT

---

### TeaSessionEntity

**Table**: `tea_session`

| Field | Type | Constraints | Default |
|---|---|---|---|
| `id` | `String` | `@PrimaryKey` | — |
| `teaId` | `String` | FK → `tea(id)` CASCADE | — |
| `parentSessionId` | `String?` | FK → `tea_session(id)` CASCADE | `null` |
| `steepNumber` | `Int` | 1 = parent/first steep | — |
| `status` | `SessionStatus` | Stored as String | — |
| `teaQuantityGrams` | `Float?` | — | `null` |
| `vesselId` | `String` | FK → `brewing_vessel(id)` RESTRICT | — |
| `waterType` | `WaterType` | Stored as String | — |
| `location` | `String?` | — | `null` |
| `rating` | `Float?` | Individual steep rating (0-5) | `null` |
| `averageRating` | `Float?` | Avg across steeps (parent only) | `null` |
| `usedConfigurationId` | `String?` | Config used for this session | `null` |
| `timestamp` | `Instant` | Session start time | — |
| `brewingTime` | `Duration` | Stored as ms (Long) | — |
| `temperatureCelsius` | `Double` | — | — |
| `waterQuantityMl` | `Double` | — | — |
| `notes` | `String?` | — | `null` |
| `photos` | `List<String>` | Stored as JSON string | `emptyList()` |
| `userId` | `String?` | — | `null` |
| `syncStatus` | `SyncStatus` | Stored as String | `LOCAL_ONLY` |
| `createdAt` | `Instant` | — | — |
| `updatedAt` | `Instant` | — | — |
| `deletedAt` | `Instant?` | Soft delete | `null` |
| `timerStatus` | `TimerStatus?` | Stored as String | `null` |
| `timerStartedAt` | `Instant?` | — | `null` |
| `timerPausedAt` | `Instant?` | — | `null` |
| `timerRemainingMs` | `Long?` | Persisted timer state | `null` |

**Indices**: `teaId`, `vesselId`, `parentSessionId`, `steepNumber`, `status`, `timestamp`, `userId`, `deletedAt`, `usedConfigurationId`

**Foreign Keys**:
- `teaId` → `tea(id)` ON DELETE CASCADE
- `vesselId` → `brewing_vessel(id)` ON DELETE RESTRICT
- `parentSessionId` → `tea_session(id)` ON DELETE CASCADE

---

### TeaTypeEntity

**Table**: `tea_type`

| Field | Type | Constraints | Default |
|---|---|---|---|
| `id` | `String` | `@PrimaryKey` | — |
| `name` | `String` | Required | — |
| `defaultTemperatureCelsius` | `Int?` | — | `null` |
| `colorHex` | `String` | Hex color (e.g., `"#4CAF50"`) | — |
| `isSystemDefault` | `Boolean` | — | `false` |
| `displayOrder` | `Int` | UI sorting | — |
| `userId` | `String?` | — | `null` |
| `createdAt` | `Instant` | — | — |
| `updatedAt` | `Instant` | — | — |
| `deletedAt` | `Instant?` | Soft delete | `null` |

**Indices**: `displayOrder`, `userId`, `deletedAt`

---

### BrewingVesselEntity

**Table**: `brewing_vessel`

| Field | Type | Constraints | Default |
|---|---|---|---|
| `id` | `String` | `@PrimaryKey` | — |
| `name` | `String` | Required | — |
| `iconName` | `String?` | Icon identifier | `null` |
| `imagePath` | `String?` | Path to vessel photo | `null` |
| `capacityMl` | `Int?` | Volume in mL | `null` |
| `isSystemDefault` | `Boolean` | — | `false` |
| `displayOrder` | `Int` | UI sorting | — |
| `userId` | `String?` | — | `null` |
| `createdAt` | `Instant` | — | — |
| `updatedAt` | `Instant` | — | — |
| `deletedAt` | `Instant?` | Soft delete | `null` |
| `syncStatus` | `SyncStatus` | — | `LOCAL_ONLY` |

**Indices**: `displayOrder`, `userId`, `deletedAt`

---

### BrewingConfigurationEntity

**Table**: `brewing_configurations`

| Field | Type | @ColumnInfo | Constraints | Default |
|---|---|---|---|---|
| `id` | `String` | — | `@PrimaryKey` | — |
| `teaId` | `String` | `tea_id` | FK → `tea(id)` CASCADE | — |
| `vesselId` | `String` | `vessel_id` | FK → `brewing_vessel(id)` CASCADE | — |
| `teaQuantityGrams` | `Float?` | `tea_quantity_grams` | — | `null` |
| `waterQuantityMl` | `Double` | `water_quantity_ml` | — | — |
| `temperatureCelsius` | `Double` | `temperature_celsius` | — | — |
| `brewingTime` | `Duration` | `brewing_time` | Stored as ms (Long) | — |
| `waterType` | `WaterType` | `water_type` | Stored as String | — |
| `sourceSessionId` | `String?` | `source_session_id` | Session this was learned from | `null` |
| `rating` | `Float?` | — | Rating of source session | `null` |
| `timesUsed` | `Int` | `times_used` | — | `0` |
| `lastUsedAt` | `Instant?` | `last_used_at` | — | `null` |
| `label` | `String?` | — | Custom name | `null` |
| `isActive` | `Boolean` | `is_active` | — | `true` |
| `createdAt` | `Instant` | `created_at` | — | — |
| `updatedAt` | `Instant` | `updated_at` | — | — |

**Indices**: `tea_id`, `vessel_id`, `(tea_id, vessel_id)` (composite), `rating`, `times_used`, `last_used_at`

**Foreign Keys**:
- `tea_id` → `tea(id)` ON DELETE CASCADE
- `vessel_id` → `brewing_vessel(id)` ON DELETE CASCADE

## Entity Relationships

```
TeaType ←───(RESTRICT)─── Tea ───(CASCADE)──→ TeaSession
                                                  │
                                                  ├── parentSessionId → TeaSession (CASCADE, self-ref)
                                                  └── vesselId → BrewingVessel (RESTRICT)

Tea ───(CASCADE)──→ BrewingConfiguration ←──(CASCADE)── BrewingVessel
```

- A Tea belongs to one TeaType (RESTRICT prevents deleting types with teas)
- Deleting a Tea cascades to all its sessions and configurations
- A session references a vessel (RESTRICT prevents deleting in-use vessels)
- Child steeps reference their parent session (CASCADE deletes children with parent)

## Domain Models

All domain models mirror their entity counterparts with identical fields. Located in `domain/models/`.

### Tea

Same fields as TeaEntity. Key computed/cached fields:
- `totalSessions`: Count of COMPLETED parent sessions
- `averageRating`: Average of all completed session ratings
- `lastBrewedAt`: Max timestamp of completed sessions

### TeaSession

Same fields as TeaSessionEntity. Multi-steep relationship:
- Parent session: `parentSessionId = null`, `steepNumber = 1`
- Child steeps: `parentSessionId = parent.id`, `steepNumber = 2, 3, ...`
- `averageRating`: Only set on parent sessions (average across all steeps)

### TeaType

Same fields as TeaTypeEntity. Default timestamps: `Instant.fromEpochMilliseconds(0)`.

### BrewingVessel

Same fields as BrewingVesselEntity.

### BrewingConfiguration

Same fields as BrewingConfigurationEntity.

## Enums

### SessionStatus

| Value | Description |
|---|---|
| `IN_PROGRESS` | Session is active (timer running or paused) |
| `COMPLETED` | Session finished |

### WaterType

| Value | `displayName` |
|---|---|
| `FILTERED` | "Filtered Water" |
| `TAP` | "Tap Water" |
| `SPRING` | "Spring Water" |
| `DISTILLED` | "Distilled Water" |
| `MINERAL` | "Mineral Water" |
| `ALKALINE` | "Alkaline Water" |
| `OTHER` | "Other" |

### SyncStatus

| Value | Description |
|---|---|
| `SYNCED` | In sync with server |
| `PENDING` | Local changes waiting to sync |
| `SYNCING` | Sync in progress |
| `CONFLICT` | Server/local conflict |
| `ERROR` | Sync error |
| `LOCAL_ONLY` | Never synced (current default) |

### TimerStatus

| Value |
|---|
| `NOT_STARTED` |
| `RUNNING` |
| `PAUSED` |
| `COMPLETE` |

### TemperatureUnit

| Value | `symbol` | Methods |
|---|---|---|
| `CELSIUS` | "°C" | `toggle()`, `toCelsius(Int): Double`, `fromCelsius(Double): Int` |
| `FAHRENHEIT` | "°F" | `toggle()`, `toCelsius(Int): Double`, `fromCelsius(Double): Int` |

### VolumeUnit

| Value | `symbol` | `largeSymbol` | Methods |
|---|---|---|---|
| `MILLILITERS` | "mL" | "L" | `toggle()`, `toMilliliters(Int): Double`, `fromMilliliters(Double): Int` |
| `FLUID_OUNCES` | "fl oz" | "gal" | `toggle()`, `toMilliliters(Int): Double`, `fromMilliliters(Double): Int` |

### TeaSortOption

| Value | `label` |
|---|---|
| `NAME_ASC` | "Name (A-Z)" |
| `RATING_DESC` | "Rating" |
| `TIMES_BREWED_DESC` | "Times Brewed" |
| `MOST_RECENT` | "Most Recent" |
| `OLDEST` | "Oldest" |

### AnalyticsPeriod

| Value | `label` |
|---|---|
| `LAST_7_DAYS` | "Last 7 Days" |
| `LAST_30_DAYS` | "Last 30 Days" |
| `LAST_90_DAYS` | "Last 90 Days" |
| `THIS_WEEK` | "This Week" |
| `THIS_MONTH` | "This Month" |
| `LAST_MONTH` | "Last Month" |
| `THIS_YEAR` | "This Year" |
| `ALL_TIME` | "All Time" |
| `CUSTOM` | "Custom Range" |

### TrendGranularity

| Value |
|---|
| `DAILY` |
| `WEEKLY` |
| `MONTHLY` |

### InsightType

| Value |
|---|
| `MOST_BREWED` |
| `AVERAGE_RATING` |
| `CONSISTENCY` |
| `FAVORITE_TYPE` |
| `RE_STEEP` |
| `TOP_RATED` |
| `PREFERRED_VESSEL` |

### PrefillSource

| Value | Properties |
|---|---|
| `SavedConfig` | — |
| `SameTypeConfig` | `teaName: String` |
| `None` | — |

## Mappers

All mappers are 1:1 bidirectional extension functions in `data/mappers/`. Direct field mapping with no transformations.

| Mapper File | Functions |
|---|---|
| `TeaMapper.kt` | `TeaEntity.toTea()`, `Tea.toEntity()` |
| `TeaSessionMapper.kt` | `TeaSessionEntity.toTeaSession()`, `TeaSession.toEntity()` |
| `TeaTypeMapper.kt` | `TeaTypeEntity.toTeaType()`, `TeaType.toEntity()` |
| `BrewingVesselMapper.kt` | `BrewingVesselEntity.toBrewingVessel()`, `BrewingVessel.toEntity()` |
| `BrewingConfigurationMapper.kt` | `BrewingConfigurationEntity.toBrewingConfiguration()`, `BrewingConfiguration.toEntity()` |

## Type Converters

All in `Converters` class, registered on `LeafLogDatabase`.

| Conversion | DB Type | Kotlin Type | Notes |
|---|---|---|---|
| `Instant` | `Long` (epoch ms) | `Instant` | via `toEpochMilliseconds()` / `fromEpochMilliseconds()` |
| `Duration` | `Long` (ms) | `Duration` | via `inWholeMilliseconds` / `toDuration(DurationUnit.MILLISECONDS)` |
| `LocalDate` | `Long` (epoch days) | `LocalDate` | via `toEpochDays()` / `fromEpochDays()` |
| `List<String>` | `String` (JSON) | `List<String>` | JSON array serialization |
| `WaterType` | `String` (enum name) | `WaterType` | via `name` / `valueOf()` |
| `SyncStatus` | `String` (enum name) | `SyncStatus` | via `name` / `valueOf()` |
| `SessionStatus` | `String` (enum name) | `SessionStatus` | Migration: `"DRAFT"` → `IN_PROGRESS` |
| `TimerStatus` | `String` (enum name) | `TimerStatus` | via `name` / `valueOf()` |

## Migration History

### MIGRATION_1_2 (version 1 → 2)

- Removed `defaultBrewingTime` field from `tea` table
- Removed `defaultBrewingTime` field from `tea_type` table
- Recreated both tables without the field
- Recreated all indices on both tables

## Analytics & Statistics Models

### AnalyticsData

| Field | Type | Description |
|---|---|---|
| `totalSessions` | `Int` | Count of parent sessions in period |
| `totalBrewingTime` | `Duration` | Sum of all steep times (including children) |
| `totalWaterMl` | `Double` | Sum of all water (including children) |
| `uniqueTeasCount` | `Int` | Distinct teas in parent sessions |
| `averageRating` | `Float?` | Average parent session rating |
| `ratedSessionsCount` | `Int` | Count of rated sessions |

### PeriodComparison

| Field | Type |
|---|---|
| `percentageChangeSessions` | `Float?` |
| `percentageChangeBrewTime` | `Float?` |
| `percentageChangeWater` | `Float?` |
| `percentageChangeUniqueTeas` | `Float?` |
| `percentageChangeAverageSteeps` | `Float?` |
| `percentageChangeNewTeas` | `Float?` |

### BrewingTrends

| Field | Type |
|---|---|
| `points` | `List<TrendPoint>` |
| `granularity` | `TrendGranularity` |

### TrendPoint

| Field | Type |
|---|---|
| `date` | `LocalDate` |
| `sessionCount` | `Int` |

### TeaTypeDistribution

| Field | Type |
|---|---|
| `teaType` | `TeaType` |
| `sessionCount` | `Int` |
| `percentage` | `Float` |

### VesselDistribution

| Field | Type |
|---|---|
| `vessel` | `BrewingVessel` |
| `sessionCount` | `Int` |
| `percentage` | `Float` |

### TopTea

| Field | Type |
|---|---|
| `tea` | `Tea` |
| `sessionCount` | `Int` |

### TopRatedTea

| Field | Type |
|---|---|
| `tea` | `Tea` |
| `averageRating` | `Float` |
| `ratedSessionCount` | `Int` |

### ActivityCell

| Field | Type | Description |
|---|---|---|
| `date` | `LocalDate` | Day start (daily) or Monday (weekly) |
| `dominantTeaTypeColorHex` | `String?` | Most frequent tea type color, null if no sessions |
| `sessionCount` | `Int` | Default: 0 |

### SteepInsights

| Field | Type |
|---|---|
| `averageSteepsPerSession` | `Float` |
| `topReSteepedTea` | `TopReSteepedTea?` |
| `newTeaDiscoveries` | `Int` |

### TopReSteepedTea

| Field | Type |
|---|---|
| `tea` | `Tea` |
| `averageSteeps` | `Float` |

### Insight

| Field | Type |
|---|---|
| `type` | `InsightType` |
| `text` | `String` |

## Utility Models

### TimerState

| Field | Type | Default |
|---|---|---|
| `sessionId` | `String?` | `null` |
| `teaId` | `String?` | `null` |
| `teaName` | `String` | `""` |
| `steepNumber` | `Int` | `1` |
| `totalDuration` | `Duration` | `Duration.ZERO` |
| `remainingDuration` | `Duration` | `Duration.ZERO` |
| `status` | `TimerStatus` | `NOT_STARTED` |
| `startedAt` | `Instant?` | `null` |
| `pausedAt` | `Instant?` | `null` |

**Computed Properties**:
- `progress: Float` — 0.0 to 1.0 (elapsed / total)
- `isRunning: Boolean` — status == RUNNING
- `canPause: Boolean` — status == RUNNING
- `canResume: Boolean` — status == PAUSED
- `isComplete: Boolean` — status == COMPLETE

### UserPreferences

| Field | Type | Default |
|---|---|---|
| `temperatureUnit` | `TemperatureUnit` | `FAHRENHEIT` |
| `volumeUnit` | `VolumeUnit` | `FLUID_OUNCES` |
| `teaSortOption` | `TeaSortOption` | `NAME_ASC` |
| `analyticsPeriod` | `AnalyticsPeriod` | `THIS_WEEK` |
| `defaultWaterType` | `WaterType` | `FILTERED` |

**Companion Presets**:
- `UserPreferences.IMPERIAL` — °F, fl oz
- `UserPreferences.METRIC` — °C, mL

### DailyStats

| Field | Type | Default |
|---|---|---|
| `sessionCount` | `Int` | `0` |
| `formattedWaterQuantity` | `String` | `""` |
| `differentTeasCount` | `Int` | `0` |
| `date` | `Instant` | `Instant.DISTANT_FUTURE` |

### ImportResult

| Field | Type |
|---|---|
| `teaTypesImported` | `Int` |
| `brewingVesselsImported` | `Int` |
| `teasImported` | `Int` |
| `teaSessionsImported` | `Int` |
| `configurationsImported` | `Int` |

## Formatters

### TemperatureFormatter

- `format(celsius: Double, unit: TemperatureUnit): String` — e.g., "80°C" or "176°F"
- `getUnitSymbol(unit: TemperatureUnit): String` — "°C" or "°F"
- `getInputLabel(unit: TemperatureUnit): String` — "Temperature (°C)" or "Temperature (°F)"

### VolumeFormatter

- `format(milliliters: Double, unit: VolumeUnit): String` — Auto-upgrades to L/gal when ≥1000mL/≥128fl oz
- `getInputLabel(unit: VolumeUnit): String` — Input field label

### UnitConverter

- `inputTemperatureToCelsius(inputValue: Int, inputUnit: TemperatureUnit): Double`
- `celsiusToDisplayTemperature(celsius: Double, displayUnit: TemperatureUnit): Int`
- `inputVolumeToMilliliters(inputValue: Int, inputUnit: VolumeUnit): Double`
- `millilitersToDisplayVolume(milliliters: Double, displayUnit: VolumeUnit): Int`

### TimeFormatter (expect/actual)

Platform-specific formatting:
- `formatClockTime(hour: Int, minute: Int): String` — Locale-appropriate 12h/24h
- `formatRelativeTimestamp(instant: Instant): String` — "Today 2:30 PM", "Yesterday", "Jan 15", etc.

## Default Seed Data

### Default Tea Types

| ID | Name | Default Temp °C | Color Hex | Display Order |
|---|---|---|---|---|
| `green` | Green | 75 | `#4CAF50` | 0 |
| `black` | Black | 95 | `#795548` | 1 |
| `white` | White | 72 | `#FFFDE7` | 2 |
| `oolong` | Oolong | 90 | `#FF9800` | 3 |
| `puerh` | Pu-erh | 98 | `#8D6E63` | 4 |
| `herbal` | Herbal | 100 | `#9C27B0` | 5 |

All seeded with `isSystemDefault = true`.

### Default Brewing Vessels

| ID | Name | Icon | Capacity mL | Display Order |
|---|---|---|---|---|
| `gaiwan` | Gaiwan | `gaiwan` | 120 | 0 |
| `teapot` | Teapot | `teapot` | 400 | 1 |
| `kyusu` | Kyusu | `kyusu` | 250 | 2 |
| `mug` | Mug | `mug` | 350 | 3 |

All seeded with `isSystemDefault = true`.

Seed data is inserted by `InitializeDefaultsUseCase` on first app launch when the respective tables are empty.

## Export Format

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

ZIP archive structure:
```
export.zip
├── data.json              # LeafLogExportData JSON
└── images/
    ├── session_photos/    # Session images
    ├── tea_photos/        # Tea images
    └── vessel_images/     # Vessel images
```
