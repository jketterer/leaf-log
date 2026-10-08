# Domain Logic

All use cases, business rules, and domain services for Leaf Log.

## Use Cases Overview

43 use cases organized across 6 domains. All registered as `factoryOf(::ClassName)` in Koin.

---

## Tea Use Cases

### InitializeDefaultsUseCase
- **Dependencies**: `TeaTypeRepository`, `BrewingVesselRepository`
- **Signature**: `suspend operator fun invoke()`
- **Logic**: Calls `initializeDefaults()` on both repositories to seed default tea types and vessels on first app launch (when tables are empty).

### SearchTeasUseCase
- **Dependencies**: `TeaRepository`
- **Signature**: `suspend operator fun invoke(query: String): List<Tea>`
- **Logic**: Returns empty list if query is blank. Otherwise trims query and delegates to repository LIKE search on name.

### CreateTeaUseCase
- **Dependencies**: `TeaRepository`
- **Signature**: `suspend operator fun invoke(name, teaTypeId, origin?, producer?, purchaseDate?, defaultTemperatureCelsius?, description?, photos): Result<Tea>`
- **Validations**:
  - `name` must not be blank
  - `teaTypeId` must not be blank
  - `defaultTemperatureCelsius` if provided must be 0-100
- **Logic**: Generates UUID, trims string fields, sets `isFavorite = false`, `totalSessions = 0`, `averageRating = null`, `syncStatus = LOCAL_ONLY`, timestamps to `Clock.System.now()`.

### EditTeaUseCase
- **Dependencies**: `TeaRepository`
- **Signature**: `suspend operator fun invoke(existingTea, name?, teaTypeId?, origin?, producer?, purchaseDate?, defaultTemperatureCelsius?, description?, photos?, isFavorite?): Result<Tea>`
- **Validations**: Same as CreateTeaUseCase for non-null fields.
- **Logic**: Merges provided fields with existing tea, updates `updatedAt`.

### ToggleFavoriteUseCase
- **Dependencies**: `TeaRepository`
- **Signature**: `suspend operator fun invoke(tea: Tea): Result<Tea>`
- **Logic**: Inverts `isFavorite`, updates `updatedAt`.

### DeleteTeaUseCase
- **Dependencies**: `TeaRepository`
- **Signature**: `suspend operator fun invoke(teaId: String): Result<String>`
- **Validations**: `teaId` must not be blank.
- **Logic**: Deletes tea, returns tea ID. Cascades to sessions and configurations via FK.

---

## Session Use Cases

### CreateSessionUseCase
- **Dependencies**: `TeaSessionRepository`, `UpdateTeaStatsUseCase`
- **Signature**: `suspend operator fun invoke(teaId, teaQuantityGrams?, vesselId, waterType, location?, brewingTime, temperatureCelsius, waterQuantityMl, notes?, rating?, photos, status = IN_PROGRESS, usedConfigurationId?): Result<TeaSession>`
- **Validations**:
  - `teaId` and `vesselId` must not be blank
  - `temperatureCelsius` must be 0-100
  - `waterQuantityMl` must be > 0
  - `teaQuantityGrams` if provided must be > 0
  - `brewingTime` must be > 0 seconds
  - If status is IN_PROGRESS: checks no session already in progress
- **Logic**: Generates UUID, sets `steepNumber = 1`, `parentSessionId = null`. If COMPLETED, calls `UpdateTeaStatsUseCase`.

### UpdateSessionUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(existingSession, teaQuantityGrams?, vesselId?, waterType?, location?, rating?, brewingTime?, temperatureCelsius?, waterQuantityMl?, notes?, photos?): Result<TeaSession>`
- **Validations**: Same as create (for non-null fields). `rating` if provided must be 0-5.
- **Logic**: Merges provided fields with existing session.

### CompleteSessionUseCase
- **Dependencies**: `TeaSessionRepository`, `UpdateTeaStatsUseCase`
- **Signature**: `suspend operator fun invoke(session, rating?, finalNotes?): Result<TeaSession>`
- **Validations**: `rating` if provided must be 0-5.
- **Logic**: Sets status to COMPLETED, updates rating, calls `UpdateTeaStatsUseCase`.

### DeleteSessionUseCase
- **Dependencies**: `TeaSessionRepository`, `UpdateTeaStatsUseCase`, `TimerService`
- **Signature**: `suspend operator fun invoke(sessionId: String): Result<Unit>`
- **Validations**: `sessionId` must not be blank.
- **Logic**: If session has active timer, stops it. Deletes session, updates tea stats.

### AddSteepUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(parentSession, brewingTime, temperatureCelsius, waterQuantityMl, notes?, photos): Result<TeaSession>`
- **Validations**:
  - Rejects if `parentSession.parentSessionId != null` (can't add steep to a child)
  - Same parameter validations as CreateSessionUseCase
- **Multi-Steep Logic**:
  - Gets all existing child steeps
  - Calculates `nextSteepNumber = max(children.steepNumber) + 1` (or 2 if no children)
  - Inherits tea/vessel/waterType/location from parent
  - Creates new session with `parentSessionId = parent.id`

### UpdateAverageRatingUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(parentSessionId: String): Result<TeaSession>`
- **Rating Calculation**:
  1. Get parent session and all child steeps
  2. Collect all non-null ratings (parent + children)
  3. Average: `allRatings.average()`
  4. **Truncate to 1 decimal**: `(rawAverage * 10).toInt() / 10.0f`
  5. If no ratings: set `averageRating = null`
  6. Update parent session

### BrewAgainUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(sourceSession: TeaSession): Result<TeaSession>`
- **Validations**: No in-progress session allowed.
- **Logic**: Creates new IN_PROGRESS session cloning source parameters. Resets rating, notes, photos.

### UpdateTeaStatsUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`
- **Signature**: `suspend operator fun invoke(teaId: String)`
- **Statistics Calculated** (from COMPLETED parent sessions only):
  - `totalSessions`: Count
  - `averageRating`: Average of rated sessions (NaN-safe)
  - `lastBrewedAt`: Max timestamp
- **Error Handling**: Non-critical — logs failures instead of throwing.

### GetBrewingParametersPrefillUseCase
- **Dependencies**: `BrewingConfigurationRepository`, `TeaRepository`, `TeaTypeRepository`
- **Signature**: `suspend operator fun invoke(tea: Tea, vessel: BrewingVessel): BrewingParametersPrefill`
- **Prefill Priority**:
  1. **Exact match**: Saved config for this tea + this vessel → `PrefillSource.SavedConfig`
  2. **Same type match**: Config for another tea of same type + this vessel → `PrefillSource.SameTypeConfig(sourcTeaName)`
  3. **No match**: Empty prefill → `PrefillSource.None`

### GetDailyStatsUseCase
- **Dependencies**: `TeaSessionRepository`, `PreferencesRepository`
- **Signature**: `operator fun invoke(): Flow<DailyStats>`
- **Reactive Logic**:
  - Combines session and preferences flows
  - Filters to today's COMPLETED sessions
  - `sessionCount`: Parent sessions only (steeps excluded)
  - `formattedWaterQuantity`: Sum of ALL steeps (including children), formatted per user unit
  - `differentTeasCount`: Unique teas from parent sessions only

---

## Analytics Use Cases

### GetAnalyticsUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(start: Instant, end: Instant): AnalyticsData`
- **Calculations** (COMPLETED sessions in date range):
  - `totalSessions`: Parent sessions only
  - `totalBrewingTime`: Sum of all steeps (including children)
  - `totalWaterMl`: Sum of all steeps (including children)
  - `uniqueTeasCount`: Distinct teaIds from parent sessions
  - `averageRating`: Average of parent session ratings (null if none rated)

### GetBrewingTrendsUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(start: Instant, end: Instant): BrewingTrends`
- **Granularity Selection**:
  - ≤45 days: DAILY
  - ≤180 days: WEEKLY
  - >180 days: MONTHLY
- **Trend Points**:
  - **Daily**: One point per calendar day, empty days show 0
  - **Weekly**: Monday-based weeks, aggregated counts
  - **Monthly**: 1st-of-month buckets, aggregated counts
- **Edge Case**: Uses earliest session date (not `Instant.DISTANT_PAST`) to avoid iterating millions of days.

### GetTopTeasUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`
- **Signature**: `suspend operator fun invoke(start, end): List<TopTea>`
- **Logic**: COMPLETED parent sessions → group by teaId → count → sort desc → top 5.

### GetTopRatedTeasUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`
- **Signature**: `suspend operator fun invoke(start, end): List<TopRatedTea>`
- **Logic**: COMPLETED parent sessions with rating/averageRating → group by teaId → average rating → sort desc → top 5.

### GetTeaTypeDistributionUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`, `TeaTypeRepository`
- **Signature**: `suspend operator fun invoke(start, end): List<TeaTypeDistribution>`
- **Logic**: COMPLETED parent sessions → map to teaTypeId via tea → count per type → calculate percentage (`count / total * 100f`) → sort desc.

### GetVesselDistributionUseCase
- **Dependencies**: `TeaSessionRepository`, `BrewingVesselRepository`
- **Signature**: `suspend operator fun invoke(start, end): List<VesselDistribution>`
- **Logic**: Same pattern as TeaTypeDistribution but for vessel IDs.

### GetBrewingActivityUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`, `TeaTypeRepository`
- **Signature**: `suspend operator fun invoke(start, end, period: AnalyticsPeriod): Result<List<ActivityCell>>`
- **Heatmap Logic**:
  - ALL_TIME: Returns empty list
  - LAST_90_DAYS, THIS_YEAR: Weekly granularity
  - Other periods: Daily granularity
- **Activity Cells**: Each cell has date, sessionCount, and `dominantTeaTypeColorHex` (most frequent non-empty tea type color for that period).
- **Edge Case**: Exclusive end date — midnight of a day excludes that day.

### GetSteepInsightsUseCase
- **Dependencies**: `TeaSessionRepository`, `TeaRepository`
- **Signature**: `suspend operator fun invoke(start, end): SteepInsights`
- **Calculations**:
  - `averageSteepsPerSession`: `(1 + total_child_steeps) / parent_count` (default 1.0)
  - `topReSteepedTea`: Tea with most steeps (requires ≥2 sessions, avg steeps ≥2)
  - `newTeaDiscoveries`: Teas whose earliest session timestamp falls within the period

### GenerateInsightsUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(current, topTeas, steepInsights, teaTypeDistribution, topRatedTeas, vesselDistribution, activityCells, period): List<Insight>`
- **Insight Generation Rules**:

| Insight Type | Condition | Text Format |
|---|---|---|
| `MOST_BREWED` | Top tea has >1 session and no ties | "{teaName} was your most brewed tea with {count} sessions" |
| `AVERAGE_RATING` | ≥5 rated sessions | "Your average rating was {rating} stars across {count} rated sessions" |
| `FAVORITE_TYPE` | Top type has >1 session | "{typeName} was your favorite type at {pct}% of sessions" |
| `RE_STEEP` | Top re-steeped tea exists | "You averaged {avg} steeps with {teaName}" |
| `TOP_RATED` | Top tea has ≥2 rated sessions | "{teaName} was your highest rated at {rating} stars" |
| `PREFERRED_VESSEL` | >1 vessel used, top has >1 session | "{vesselName} was your preferred vessel at {pct}% usage" |
| `CONSISTENCY` | ≥3 consecutive active days/weeks | "You had a {count} {day/week} brewing streak!" |

**Consistency Calculation**: Iterates sorted activity cells, counts consecutive cells with `sessionCount > 0`. Reports longest streak of 3+.

---

## Configuration Use Cases

### GenerateConfigurationLabelUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(vesselName?, teaQuantityGrams?, waterQuantityMl, brewingTime): String`
- **Label Generation Algorithm**:
  1. Calculate leaf ratio: `teaQuantityGrams / waterQuantityMl`
  2. Classify brewing style:

| Style | Condition |
|---|---|
| **Gong-fu** | `brewingTime ≤ 45s` AND `ratio > 0.03` |
| **Western** | `brewingTime ≥ 3 min` AND `waterQuantityMl ≥ 250 mL` |
| **Bag Method** | `teaQuantityGrams == null` |
| **Grandpa** | `brewingTime ≥ 2 min` AND `waterQuantityMl` 150-350 mL |
| **Quick Steep** | `brewingTime ≤ 1 min` |
| **Short Steep** | `brewingTime ≤ 2 min` |
| **Standard** | Everything else |

  3. Format: `"{vesselName} · {style}"` if vessel name provided, otherwise just `"{style}"`

### CheckDuplicateConfigurationUseCase
- **Dependencies**: `BrewingConfigurationRepository`
- **Signature**: `suspend operator fun invoke(session: TeaSession): Boolean`
- **Duplicate Detection Tolerances** (all must match):

| Parameter | Tolerance |
|---|---|
| Tea quantity | ±0.5g (both null counts as match) |
| Water volume | ±10 mL |
| Temperature | ±1°C |
| Brewing time | ±5 seconds |

- Checks existing configurations for same tea + vessel combination.

### SaveBrewingConfigurationUseCase
- **Dependencies**: `BrewingConfigurationRepository`, `GenerateConfigurationLabelUseCase`
- **Signature**: `suspend operator fun invoke(session, customLabel?, skipRatingCheck): Result<BrewingConfiguration>`
- **Validations**: If `skipRatingCheck = false`, requires `session.rating >= 3.0` (3+ stars).
- **Logic**:
  - Auto-generates label (without vessel name) if no custom label
  - Sets `timesUsed = 1`, `lastUsedAt = now`
  - Tracks `sourceSessionId`
  - ID format: `"config_{epochMs}_{random0-9999}"`

### CreateBrewingConfigurationUseCase
- **Dependencies**: `BrewingConfigurationRepository`, `GenerateConfigurationLabelUseCase`
- **Signature**: `suspend operator fun invoke(teaId, vesselId, teaQuantityGrams?, waterQuantityMl, temperatureCelsius, brewingTime, waterType, customLabel?): Result<BrewingConfiguration>`
- **Logic**: Uses custom label if non-blank, otherwise generates label WITH vessel name. Sets `sourceSessionId = null`, `rating = null`, `timesUsed = 0`.

### UpdateBrewingConfigurationUseCase
- **Dependencies**: `BrewingConfigurationRepository`
- **Signature**: `suspend operator fun invoke(configurationId, label?, vesselId?, teaQuantityGrams?, waterQuantityMl?, temperatureCelsius?, brewingTime?, waterType?, isActive?): Result<BrewingConfiguration>`
- **Logic**: Merges provided fields with existing configuration.

### DeleteBrewingConfigurationUseCase
- **Dependencies**: `BrewingConfigurationRepository`
- **Signature**: `suspend operator fun invoke(configurationId: String): Result<Unit>`
- **Logic**: Hard delete (configurations don't use soft delete).

---

## Vessel Use Cases

### CreateBrewingVesselUseCase
- **Dependencies**: `BrewingVesselRepository`
- **Signature**: `suspend operator fun invoke(name, iconName?, imagePath?, capacityMl?): Result<BrewingVessel>`
- **Validations**:
  - `name` must not be blank
  - `capacityMl` if provided must be > 0
- **Logic**: Generates UUID, calculates `displayOrder = maxOrder + 1`, sets `isSystemDefault = false`.

### UpdateBrewingVesselUseCase
- **Dependencies**: `BrewingVesselRepository`
- **Signature**: `suspend operator fun invoke(existingVessel, name?, iconName?, imagePath?, clearImage, capacityMl?): Result<BrewingVessel>`
- **Special Handling**:
  - `capacityMl = -1`: Clears capacity (sets to null)
  - `clearImage = true`: Sets imagePath to null regardless of imagePath param

### DeleteBrewingVesselUseCase
- **Dependencies**: `BrewingVesselRepository`, `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(vesselId: String): Result<Unit>`
- **Validations**:
  - `vesselId` must not be blank
  - At least one vessel must remain after deletion
  - No sessions can reference this vessel
- **Logic**: Checks session count via repository before allowing delete.

---

## Timer Use Cases

### StartTimerUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(session: TeaSession, duration: Duration): Result<TimerState>`
- **Validations**: `duration > 0`. Rejects if session is COMPLETED.
- **Logic**: Updates session status to IN_PROGRESS. Creates TimerState with `status = RUNNING`, `startedAt = now`, `remainingDuration = duration`.

### PauseTimerUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(currentState: TimerState): Result<TimerState>`
- **Validations**: Timer must be RUNNING with startedAt set.
- **Logic**: Calculates elapsed since startedAt. Remaining = `(totalDuration - elapsed).coerceAtLeast(Duration.ZERO)`. Returns paused state.

### ResumeTimerUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(currentState: TimerState): Result<TimerState>`
- **Validations**: Timer must be PAUSED.
- **Logic**: Adjusts `startedAt = now - alreadyElapsed` where `alreadyElapsed = totalDuration - remainingDuration`. This preserves progress ring accuracy after resume.

### AdjustTimeUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(currentState: TimerState, adjustment: Duration): Result<TimerState>`
- **Logic**:
  - If NOT_STARTED: Adjusts `totalDuration` directly
  - If RUNNING: `newRemaining = (currentRemaining + adjustment).coerceAtLeast(Duration.ZERO)`, adjusts total accordingly
  - Prevents negative remaining

### CompleteTimerUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(currentState: TimerState, session: TeaSession): Result<TimerState>`
- **Logic**: Updates session status (remains IN_PROGRESS — user must rate/finalize via SteepComplete).

### CancelTimerUseCase
- **Dependencies**: None (pure function)
- **Signature**: `operator fun invoke(currentState: TimerState): Result<TimerState>`
- **Logic**: Resets timer to NOT_STARTED, clears timing info. Session remains IN_PROGRESS.

### SaveTimerStateUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(timerState: TimerState): Result<Unit>`
- **Also**: `suspend fun clear(sessionId: String): Result<Unit>`
- **Logic**: Persists timer fields to session entity: `timerStatus`, `timerStartedAt`, `timerPausedAt`, `timerRemainingMs`. `clear()` nulls all timer fields. Used for app backgrounding.

### RestoreTimerStateUseCase
- **Dependencies**: `TeaSessionRepository`
- **Signature**: `suspend operator fun invoke(sessionId: String): Result<TimerState?>`
- **Restoration Logic**:
  - No saved state → returns null
  - RUNNING: Recalculates remaining from elapsed since startedAt. If completed while backgrounded → sets COMPLETE
  - PAUSED: Uses saved remainingMs directly
  - COMPLETE: Returns COMPLETE with 0 remaining
  - NOT_STARTED: Returns null

---

## Repository Interfaces

### TeaRepository
```kotlin
interface TeaRepository {
    fun getAllFlow(): Flow<List<Tea>>
    suspend fun getAll(): List<Tea>
    suspend fun getById(id: String): Tea?
    fun getByIdFlow(id: String): Flow<Tea?>
    fun getByTypeFlow(teaTypeId: String): Flow<List<Tea>>
    fun getFavoritesFlow(): Flow<List<Tea>>
    suspend fun search(query: String): List<Tea>
    suspend fun upsert(tea: Tea)
    suspend fun delete(id: String)
    fun getDistinctProducersFlow(): Flow<List<String>>
}
```

### TeaSessionRepository
```kotlin
interface TeaSessionRepository {
    fun getAllFlow(): Flow<List<TeaSession>>
    suspend fun getAll(): List<TeaSession>
    fun getRecentFlow(limit: Int): Flow<List<TeaSession>>
    suspend fun getRecent(limit: Int): List<TeaSession>
    suspend fun getById(id: String): TeaSession?
    fun getByIdFlow(id: String): Flow<TeaSession?>
    fun getByTeaIdFlow(teaId: String): Flow<List<TeaSession>>
    suspend fun getByTeaId(teaId: String, limit: Int? = null): List<TeaSession>
    suspend fun getByVesselId(vesselId: String): List<TeaSession>
    fun getByVesselIdFlow(vesselId: String): Flow<List<TeaSession>>
    suspend fun getChildSteeps(parentId: String): List<TeaSession>
    fun getInProgressFlow(): Flow<List<TeaSession>>
    fun getInProgressCountFlow(): Flow<Int>
    suspend fun getByDateRange(start: Instant, end: Instant): List<TeaSession>
    suspend fun upsert(session: TeaSession)
    suspend fun delete(id: String)
    suspend fun countByDateRange(start: Instant, end: Instant): Int
    suspend fun hasInProgressSession(): Boolean
}
```

### TeaTypeRepository
```kotlin
interface TeaTypeRepository {
    fun getAllFlow(): Flow<List<TeaType>>
    suspend fun getAll(): List<TeaType>
    suspend fun getById(id: String): TeaType?
    fun getByIdFlow(id: String): Flow<TeaType?>
    suspend fun upsert(teaType: TeaType)
    suspend fun delete(id: String)
    suspend fun initializeDefaults()
}
```

### BrewingVesselRepository
```kotlin
interface BrewingVesselRepository {
    fun getAllFlow(): Flow<List<BrewingVessel>>
    suspend fun getAll(): List<BrewingVessel>
    suspend fun getById(id: String): BrewingVessel?
    fun getByIdFlow(id: String): Flow<BrewingVessel?>
    suspend fun upsert(vessel: BrewingVessel)
    suspend fun delete(id: String)
    suspend fun initializeDefaults()
}
```

### BrewingConfigurationRepository
```kotlin
interface BrewingConfigurationRepository {
    suspend fun getAll(): List<BrewingConfiguration>
    suspend fun getByTeaAndVessel(teaId: String, vesselId: String): List<BrewingConfiguration>
    suspend fun getBestByTeaAndVessel(teaId: String, vesselId: String): BrewingConfiguration?
    suspend fun getByTeaId(teaId: String): List<BrewingConfiguration>
    fun getByTeaIdFlow(teaId: String): Flow<List<BrewingConfiguration>>
    suspend fun getById(id: String): BrewingConfiguration?
    fun getByIdFlow(id: String): Flow<BrewingConfiguration?>
    suspend fun upsert(configuration: BrewingConfiguration)
    suspend fun delete(id: String)
    suspend fun incrementTimesUsed(id: String, timestamp: Instant)
    suspend fun setActive(id: String, isActive: Boolean)
}
```

### PreferencesRepository
```kotlin
interface PreferencesRepository {
    fun getPreferencesFlow(): Flow<UserPreferences>
    suspend fun getPreferences(): UserPreferences
    suspend fun updateTemperatureUnit(unit: TemperatureUnit): Result<Unit>
    suspend fun updateVolumeUnit(unit: VolumeUnit): Result<Unit>
    suspend fun updateTeaSortOption(option: TeaSortOption): Result<Unit>
    suspend fun updateAnalyticsPeriod(period: AnalyticsPeriod): Result<Unit>
    suspend fun updateDefaultWaterType(waterType: WaterType): Result<Unit>
}
```

### DataExportRepository
```kotlin
interface DataExportRepository {
    suspend fun exportAll(): Result<String>            // Returns ZIP file path
    suspend fun importFrom(filePath: String): Result<ImportResult>
}
```

---

## Domain Services

### TimerService

Singleton managing timer state and countdown loop. Lives in application-level `CoroutineScope`.

```kotlin
class TimerService(
    coroutineScope: CoroutineScope,
    notificationService: TimerNotificationService,
    saveTimerStateUseCase: SaveTimerStateUseCase,
    lifecycleHandler: TimerLifecycleHandler,
)
```

**State**: `StateFlow<TimerState>` — shared across all observers.

**Key Methods**:
- `updateState(newState)`: Sets current timer state
- `startCountdown()`: Launches 100ms tick loop, schedules completion alarm, shows notifications
- `onTimeAdjusted(state)`: Reschedules alarm after time adjustment
- `cancelCountdown()`: Stops loop without changing state
- `stop()`: Complete cleanup, resets to default state

**Countdown Loop Algorithm**:
1. Verify timer is still RUNNING
2. Remaining = `(totalDuration - elapsed).coerceAtLeast(Duration.ZERO)` where elapsed is from `startedAt`
3. Update StateFlow
4. If remaining > 0: show notification (throttled to 1/sec), continue loop
5. If remaining == 0: mark COMPLETE, persist state, show completion notification, break

### TimerNotificationService

Platform-specific interface for timer notifications.

```kotlin
interface TimerNotificationService {
    fun showTimerRunning(state: TimerState)
    fun showTimerComplete(teaName: String, sessionId: String?)
    fun scheduleCompletionAlarm(teaName: String, remainingSeconds: Double, sessionId: String?)
    fun cancelCompletionAlarm()
    fun onTimerStopped()
}
```

---

## Business Rules Summary

| Rule | Implementation |
|---|---|
| Only one IN_PROGRESS session | `CreateSessionUseCase` validates via `hasInProgressSession()` |
| Multi-steep parent/child | Parent: `parentSessionId=null`, `steepNumber=1`; Child: `parentSessionId=id`, `steepNumber≥2` |
| Average rating truncation | `(average * 10).toInt() / 10.0f` — truncated (not rounded) to 1 decimal |
| Tea stats scope | Calculated from COMPLETED parent sessions only (steeps excluded) |
| Daily stats water sum | Sum across ALL steeps (including children) for complete picture |
| Config duplicate tolerances | ±0.5g quantity, ±10mL water, ±1°C temp, ±5s time |
| Config save rating gate | Minimum 3.0 stars (bypassable with `skipRatingCheck = true`) |
| Config label styles | 7 styles based on ratio + time + volume thresholds |
| Prefill priority | Exact tea+vessel → same type+vessel → none |
| Timer state persistence | Saved to session entity fields for backgrounding/restoration |
| Timer background completion | `RestoreTimerStateUseCase` detects elapsed > total and marks COMPLETE |
| Trend granularity | ≤45d: daily, ≤180d: weekly, >180d: monthly |
| Heatmap dominance | Most frequent non-empty tea type color in the cell's period |
| Vessel deletion | Must keep ≥1 vessel; no sessions may reference the vessel |
| Insight thresholds | ≥5 rated sessions for avg rating; ≥2 sessions for top rated; 3+ streak for consistency |
| Temperature/volume storage | Always Celsius/mL; converted for display via user preference |
| Soft deletes | `deletedAt` timestamp; DAO queries filter `WHERE deletedAt IS NULL` |
