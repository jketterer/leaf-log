# Leaf Log - Product Requirements Document

> **Last updated:** 2026-03-05
> **Status:** Living document - updated as development progresses

---

## 1. Product Overview

### 1.1 What is Leaf Log?

Leaf Log is a Kotlin Multiplatform tea brewing tracker for Android and iOS. It helps tea
enthusiasts log, time, and analyze their brewing sessions with precision. The app supports
multi-steep tracking, reusable brewing configurations, detailed analytics, and a growing tea
collection manager.

### 1.2 Target Audience

- Tea enthusiasts who brew loose-leaf tea regularly
- Users who practice gongfu-style or multi-steep brewing
- Anyone who wants to track and improve their brewing parameters over time

### 1.3 Platforms

| Platform | Technology |
|----------|-----------|
| Android | Kotlin / Compose Multiplatform |
| iOS | Kotlin / Compose Multiplatform (shared UI) |

### 1.4 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform (Material 3) |
| Architecture | Clean Architecture + MVI |
| Database | Room (local) |
| DI | Koin |
| Navigation | Compose Navigation 3 |
| Language | Kotlin Multiplatform |

---

## 2. Information Architecture

### 2.1 Bottom Navigation

The app uses a 4-tab bottom navigation bar:

| Tab | Icon | Destination |
|-----|------|------------|
| Home | Home | Dashboard with daily stats, recent sessions, and quick actions |
| Collection | Bookmark | Tea and vessel library |
| History | Clock | Full session history with filters |
| Analytics | BarChart2 | Brewing analytics and insights |

Additional screens are accessible via navigation from these tabs (e.g., Settings from Home, Tea
Detail from Collection).

### 2.2 Screen Map

```
Home
├── Log Tea (session creation form)
│   └── Timer (brew timer)
│       └── Steep Complete (post-brew tasting & review)
├── Quick Timer (lightweight timer without pre-logging)
│   └── Steep Complete (post-brew tasting & review)
├── Session Detail (view past session)
│   └── Edit Session
├── Settings
│   ├── Tea Type Management
│   │   └── Edit Tea Type
│   ├── Export Data
│   └── Import Data

Collection
├── Tea Detail
│   ├── Edit Tea
│   ├── Brewing Configurations
│   │   └── Create/Edit Brewing Configuration
│   └── Log Tea (pre-filled with this tea)
├── Edit Tea (create new)
├── Vessel Detail
│   └── Edit Vessel
└── Edit Vessel (create new)

History
├── Session Detail
│   ├── Edit Session
│   ├── Add Steep
│   └── Save as Method (dialog, in-place)
└── Filter Sheet

Analytics
└── Period Selector
```

---

## 3. Core Domain Models

### 3.1 Tea

A tea in the user's collection.

| Field | Type | Description |
|-------|------|------------|
| id | String | Unique identifier |
| name | String | Tea name |
| teaTypeId | String | Reference to TeaType |
| origin | String? | Country/region of origin |
| producer | String? | Tea producer or brand |
| purchaseDate | LocalDate? | When the tea was purchased |
| purchasePrice | Double? | Price paid — stored in domain model, not yet surfaced in UI |
| stockAmount | Int? | Remaining stock quantity — stored in domain model, not yet surfaced in UI |
| defaultTemperatureCelsius | Int? | Suggested temperature |
| description | String? | User notes about the tea |
| photos | List\<String\> | Photo file paths |
| isFavorite | Boolean | Favorited by user |
| totalSessions | Int | Computed: number of completed sessions |
| averageRating | Float? | Computed: average rating across sessions |
| lastBrewedAt | Instant? | Computed: most recent session timestamp |

### 3.2 Tea Type

Categories of tea with a default temperature.

| Field | Type | Description |
|-------|------|------------|
| id | String | Unique identifier |
| name | String | e.g., "Green", "Oolong", "Pu-erh" |
| defaultTemperatureCelsius | Int? | Suggested temperature for this type |
| colorHex | String | Display color (e.g., "#4CAF50") |
| isSystemDefault | Boolean | Whether this is a built-in type |
| displayOrder | Int | Sort order |

### 3.3 Tea Session

A single brewing session or steep within a multi-steep session.

| Field | Type | Description |
|-------|------|------------|
| id | String | Unique identifier |
| teaId | String | Reference to Tea |
| parentSessionId | String? | If this is a child steep, references parent session |
| steepNumber | Int | Order within multi-steep (1 = first/parent) |
| status | SessionStatus | IN_PROGRESS or COMPLETED |
| teaQuantityGrams | Float? | Amount of tea used; `null` means Tea Bag |
| vesselId | String | Reference to BrewingVessel |
| waterType | WaterType | Type of water used |
| location | String? | Where the session took place |
| rating | Float? | User rating for this steep (integer 1–5; null = unrated; tapping the active star clears it) |
| averageRating | Float? | Average across all steeps (parent only) |
| usedConfigurationId | String? | Configuration used, if any |
| timestamp | Instant | When the session occurred |
| brewingTime | Duration | How long the tea was brewed |
| temperatureCelsius | Double | Water temperature |
| waterQuantityMl | Double | Water volume |
| notes | String? | User notes |
| photos | List\<String\> | Photo file paths |
| timerStatus | TimerStatus? | Timer state for persistence |
| timerStartedAt | Instant? | When timer was started |
| timerPausedAt | Instant? | When timer was paused |
| timerRemainingMs | Long? | Remaining time in ms |

**Multi-steep model:**

- First steep = parent session (`parentSessionId = null`, `steepNumber = 1`)
- Subsequent steeps = children (`parentSessionId = parent.id`, `steepNumber = 2, 3, ...`)
- Parent session tracks `averageRating` (truncated to 1 decimal)

**Constraint:** Only one session may be `IN_PROGRESS` at a time. The FAB and "Brew Again" are
hidden while a session is in progress, preventing creation of a second concurrent session. There
is no explicit message explaining why the FAB is hidden; the in-progress sessions banner on Home
provides implicit context.

**Delete cascade:** Deleting a parent session soft-deletes all child steeps. Deleting a single
child steep renumbers the remaining siblings and triggers `UpdateAverageRatingUseCase` to
recalculate the parent's average. All deletes use a soft-delete pattern (`deletedAt` timestamp)
for sync compatibility.

### 3.4 Brewing Vessel

A container the user brews tea in.

| Field | Type | Description |
|-------|------|------------|
| id | String | Unique identifier |
| name | String | e.g., "Yixing Teapot", "Glass Gaiwan" |
| iconName | String? | Icon identifier for display |
| imagePath | String? | Custom photo path |
| capacityMl | Int? | Vessel capacity |
| isSystemDefault | Boolean | Built-in vs user-created |
| displayOrder | Int | Sort order |

### 3.5 Brewing Configuration

A saved, reusable set of brewing parameters for a tea + vessel combination.

| Field | Type | Description |
|-------|------|------------|
| id | String | Unique identifier |
| teaId | String | Reference to Tea |
| vesselId | String | Reference to BrewingVessel |
| teaQuantityGrams | Float? | Tea amount; `null` means Tea Bag (no loose leaf quantity) |
| waterQuantityMl | Double | Water volume |
| temperatureCelsius | Double | Water temperature |
| brewingTime | Duration | Steep duration |
| waterType | WaterType | Water type |
| sourceSessionId | String? | Session this was learned from (null if manual) |
| rating | Float? | Rating of source session |
| timesUsed | Int | Usage count |
| lastUsedAt | Instant? | Most recent use |
| label | String? | User or auto-generated name |
| isActive | Boolean | Can be disabled without deleting |

### 3.6 Supporting Enums

| Enum | Values |
|------|--------|
| SessionStatus | IN_PROGRESS, COMPLETED |
| WaterType | FILTERED, TAP, SPRING, DISTILLED, MINERAL, ALKALINE, OTHER |
| TemperatureUnit | CELSIUS, FAHRENHEIT |
| VolumeUnit | MILLILITERS, FLUID_OUNCES |
| TeaSortOption | NAME_ASC, RATING_DESC, TIMES_BREWED_DESC, MOST_RECENT, OLDEST |
| SyncStatus | SYNCED, PENDING, SYNCING, CONFLICT, ERROR, LOCAL_ONLY |

### 3.7 User Preferences

| Field | Type | Default |
|-------|------|---------|
| temperatureUnit | TemperatureUnit | CELSIUS |
| volumeUnit | VolumeUnit | MILLILITERS |
| teaSortOption | TeaSortOption | NAME_ASC |
| analyticsPeriod | AnalyticsPeriod | THIS_WEEK |
| defaultWaterType | WaterType | FILTERED |

---

## 4. Features (Current)

### 4.1 Home Screen

**Purpose:** Dashboard providing an at-a-glance summary and quick access to primary actions.

**Components:**

- **Greeting header** - Time-of-day greeting
- **Daily stats section** - Cards showing today's session count, water consumed, and unique teas
  brewed. Each card is tappable (navigates to filtered history or analytics).
- **In-progress sessions banner** - Shows the current in-progress session (at most one at a time)
  with timer progress and a button to resume or finish it. When this banner is present, the FAB
  is silently hidden (no tooltip or explanatory text).
- **Recent sessions list** - Last few completed sessions with tea name, type, rating, and
  timestamp. Each session supports brew-again, edit, and delete actions.
- **Empty state** - Welcoming message for new users with no sessions
- **Expandable FAB** - Two actions:
    - **Log Tea** - Full session creation flow
    - **Quick Timer** - Lightweight timer with duration picker

**Navigation from Home:**

- Settings (via icon in greeting header)
- Log Tea
- Quick Timer
- Session Detail (tap a recent session)
- History (tap "View All" on recent sessions)
- Analytics (tap "View All" on daily stats)
- Timer (resume an in-progress session)

### 4.2 Log Tea (Session Creation)

**Purpose:** Create a new brewing session with full parameter entry.

**Flow:**

1. **Select tea** - Search dialog with full tea list
2. **Select vessel** - Vessel picker from user's collection
3. **Set brewing parameters** (required; revealed after tea + vessel are selected):
    - Brewing time (duration picker)
    - Water temperature (supports C/F based on preferences)
    - Water quantity (supports mL/fl oz based on preferences)
    - **Tea type** (Loose Leaf / Tea Bag segmented button — always visible):
        - If **Loose Leaf**: tea quantity grams field + preset chips shown (optional value)
        - If **Tea Bag**: quantity field hidden; `teaQuantityGrams = null` stored
4. **Optional details** (collapsible section):
    - Water type (dropdown, default: user's preferred water type from Settings)
    - Location (text field)
5. **Photos** - Attach photos via picker
6. **Pre-fill system** - Parameters can be pre-filled from (in priority order):
    1. A saved `BrewingConfiguration` for the exact same tea + vessel combination
    2. A saved `BrewingConfiguration` for the same tea type + vessel combination
    3. Previous session with the same tea ("Brew Again")
    - `Tea.defaultTemperatureCelsius` and `TeaType.defaultTemperatureCelsius` are **not** used
      in the automatic pre-fill chain. They are available as user-visible defaults but are not
      applied programmatically during session creation.
    - When pre-filling from a configuration, Tea Bag mode is activated only if the source
      explicitly has `teaQuantityGrams = null`. A missing prefill never implicitly activates
      Tea Bag mode — Loose Leaf is always the default.
7. **Complete or start timer:**
    - **Save as draft** - Creates an IN_PROGRESS session and navigates to the Timer screen
    - **Complete session** - Dialog to add notes and rating, then saves as COMPLETED

**Validation:** Tea and vessel are required. Temperature, water quantity, and brewing time have
range validation.

**Can be pre-filled via route params:** `teaId`, `vesselId`, `configurationId`

### 4.3 Timer

**Purpose:** Countdown timer for an active brewing session.

**Features:**

- **Circular progress ring** - Visual countdown with remaining time in MM:SS
- **Controls:** Start, Pause/Resume, Reset
- **Quick adjust buttons** - Add/subtract time in increments while running
- **Session info display** - Tea name, vessel, temperature, water quantity
- **Edit sheet** - Modify temperature, water quantity, tea type (Loose Leaf / Tea Bag), tea
  quantity, and water type mid-session
- **Completion:** When the timer completes (or the user stops early), the app transitions to the
  **Steep Complete** screen (section 4.3.1). The Timer screen itself no longer handles rating,
  notes, next steep, or save configuration — those responsibilities move to Steep Complete.
  There is no in-app sound or haptic when the timer completes in the foreground; the screen
  transition itself is the signal.
- **Discard option** - Cancel without saving
- **Timer persistence** - Timer state is saved to the session entity so it survives app
  backgrounding and process death. If the timer finishes while the app is backgrounded, the
  app navigates directly to Steep Complete on next open (no "finished X minutes ago" message).
- **Stop/Reset confirmations** - Confirmation dialogs before destructive actions
- **iOS Live Activity / Dynamic Island** - While a timer is active on iOS, a Live Activity
  shows the countdown on the Lock Screen and in the Dynamic Island without requiring the app
  to be open. The expanded view shows tea name, steep number, and remaining time. The compact
  trailing shows the countdown (or a paused indicator). The Live Activity ends when the timer
  completes or is discarded. See section 4.15 for full details.

### 4.3.1 Steep Complete

**Purpose:** Post-brew companion screen where the user pours, drinks, and reviews their steep at
their own pace. Bridges the gap between "the tea is done steeping" (a brewing event) and "I'm
done with this session" (an experience event).

**Design philosophy:** The timer completing means "stop steeping the leaves," not "the experience
is over." This screen gives the user agency to rate, take notes, start the next steep, or finish
the session — all on their own timeline, with nothing demanded up front.

**Layout:**

- **Steep header** - Tea name, vessel, steep number, and brewing parameters (time, temperature,
  water quantity) for the just-completed steep. An **Edit** button opens a bottom sheet so the user
  can correct any parameter that didn't match what actually happened (e.g., steeped longer than
  intended). Editable fields: brewing time, temperature, water quantity, tea type (Loose Leaf / Tea
  Bag), tea quantity, and water type. Edits are persisted immediately.
- **Rating selector** - Star rating for this steep; available but not required. Tappable at any
  time.
- **Notes field** - Free-text notes for this steep; available but not required. Tappable at any
  time.
- **Photos** - Option to attach photos for this steep
- **Start Next Steep** - Prominently placed button to begin another steep with adjustable
  parameters (duration, temperature, water quantity). Creates a new child session (`status =
  IN_PROGRESS`) with incremented steep number and returns to the Timer screen. The parent
  session remains `COMPLETED` — only the new child steep is in progress.
- **Previous steeps** - Scrollable summary of earlier steeps in this session, showing each steep's
  number, duration, rating, and notes. Previous steeps remain editable (tap to update rating or
  notes) so the user can refine impressions as the session progresses.
- **Finish Session** - Explicit user action to end the session. Only appears when the user is
  ready. On tap:
    1. Current steep is marked `COMPLETED` (with whatever rating/notes have been provided)
    2. Average rating is recalculated across all steeps (if multi-steep)
    3. **Save configuration prompt** - Optionally save the brewing parameters as a reusable
       configuration. Suppressed if an identical configuration already exists for this tea + vessel
       combination (parameters match within tolerance). Requires a rating of 3+ stars.
    4. Session status transitions to `COMPLETED`
    5. Navigate to Home (updated stats and recent sessions)

**Behavioral notes:**

- The session remains `IN_PROGRESS` while on this screen. The user is still engaged with their
  tea — they just aren't actively timing a steep.
- Rating, notes, and parameter edits are all persisted immediately as the user enters them (not
  deferred to "Finish Session"), so data isn't lost if the app is backgrounded or killed.
- For **gongfu / multi-steep sessions**: This screen is the per-steep tasting journal. The natural
  rhythm is: steep completes → pour small cup → drink → rate and note how this steep differs from
  the last → start next steep. The previous steeps list builds into a narrative of how the tea
  evolved across infusions.
- For **western / single-steep sessions**: The screen is simpler — no previous steeps section, and
  "Start Next Steep" is less prominent. The user drinks their mug at leisure, rates when ready,
  and finishes when done.
- **Discard option** - Available to cancel the session without saving (with confirmation dialog).

### 4.4 Quick Timer

**Purpose:** Start a timer without pre-logging session details. Ideal for casual use or when the
user wants to time first and log details later.

**Flow:**

1. Select duration from a bottom sheet (launched from Home FAB)
2. Timer starts immediately
3. During or after the timer:
    - A **details sheet** (2-step) lets the user optionally add tea, vessel, temperature, and tea
      type (Loose Leaf / Tea Bag)
    - **Water quantity** is auto-derived from the vessel's `capacityMl` when set; the field is
      hidden in this case. If the vessel has no capacity, an editable water quantity field is shown
    - Preset chips are available for temperature, water quantity (when editable), and tea quantity
      (when Loose Leaf is selected)
    - Temperature and volume units can be toggled inline (°C/°F, mL/fl oz)
    - Pre-fill from saved configurations or tea defaults is supported; saved config water quantity
      takes priority over vessel capacity
4. On completion:
    - If required details are filled in (tea + vessel + temp + water qty), an IN_PROGRESS session
      is auto-saved and the app transitions to the **Steep Complete** screen (section 4.3.1) —
      same experience as the full Timer flow
    - If details are not filled in, the timer can still be used as a simple countdown without
      logging a session (no Steep Complete screen; timer simply resets)

### 4.5 Tea Collection

**Purpose:** Browse and manage the user's tea library.

**Tabs:**

- **Teas tab** - Grid/list of teas with:
    - Filter chips: All, Favorites, By Type (then select a specific type)
    - Search bar
    - Sort options: Name (A-Z), Rating, Times Brewed, Most Recent, Oldest
- **Vessels tab** - List of brewing vessels

**Tea Card:** Shows tea name, type color indicator, origin, rating, and session count.

**Actions:** Tap to view Tea Detail.

### 4.6 Tea Detail

**Purpose:** View a single tea's full profile and history.

**Sections:**

- Tea name, type, origin, producer, description
- Photos gallery with fullscreen viewer
- Purchase date
- Default temperature
- **Saved Brewing Configurations** - List of saved methods for this tea, with ability to:
    - Create new configuration
    - Edit existing configuration
    - Delete configuration
    - Start a session from a configuration
- **Recent sessions** - Sessions brewed with this tea
    - Tap to view session detail
    - Brew again action
    - Delete action
- Favorite toggle
- Edit and Delete actions

### 4.7 Edit Tea

**Purpose:** Create or edit a tea entry.

**Fields:**

- Name (required)
- Tea type (required, selectable from list)
- Origin
- Producer (with autocomplete from existing producers)
- Purchase date (date picker)
- Description
- Photos
- Default temperature (expandable section)

**Validation:** Name and tea type are required. Temperature has range checks.

**Unsaved changes detection** with discard confirmation dialog.

### 4.8 Vessel Management

**Vessel List:**

- Grid of vessel cards showing name, icon, and capacity
- Add new vessel button

**Vessel Detail:**

- Name, icon, capacity
- Session count (how many times this vessel has been used)
- Edit and Delete actions
- Delete is blocked if: it's the only vessel remaining, or it has associated sessions

**Edit Vessel:**

- Name
- Icon selector (curated icon set)
- Capacity (in user's preferred volume unit)

### 4.9 Brewing Configurations

**Purpose:** Save and reuse specific brewing parameter sets for a tea + vessel combination.

**Created from:**

- Tea Detail screen (manual creation)
- Steep Complete "Finish Session" flow (save what you just brewed)
- Quick Timer → Steep Complete flow (same as above when session details are provided)
- Edit Session flow (when editing a steep with a high rating)
- Session Detail screen (retroactive "Save as Method" button — user-initiated, no rating requirement)

**Fields:**

- Vessel (required)
- Label (auto-generated or user-provided)
- Tea type (Loose Leaf with quantity, or Tea Bag with no quantity)
- Water quantity
- Temperature
- Brewing time (required)
- Water type

**Auto-generated labels:** When saving from a completion flow, a label is pre-generated using
the tea name and a style keyword derived from brewing parameters:

| Style | Conditions |
|-------|-----------|
| Gong-fu | Time ≤ 45 s and leaf ratio > 3% |
| Western | Time ≥ 3 min and water ≥ 250 ml |
| Bag Method | No tea quantity specified |
| Grandpa | Time ≥ 2 min and water 150–350 ml |
| Quick Steep | Time ≤ 1 min |
| Short Steep | Time ≤ 2 min |
| Standard | Everything else |

Label format: `"Tea Name · Style"` (e.g. `"Jasmine · Gong-fu"`). The user can edit the label
in the save dialog before confirming.

**Duplicate suppression:** When saving from the Steep Complete/edit flows, the prompt is suppressed
entirely if an identical configuration already exists. When saving retroactively from Session
Detail, the dialog is also suppressed — but the user receives an error snackbar explaining why
("A matching configuration already exists") so the outcome is visible. "Identical" means all
parameters match within tolerance: temperature ±1 °C, water ±10 ml, brewing time ±5 s, tea
quantity ±0.5 g.

**Rating requirement:** The automatic save prompt (shown when finishing a session via Steep
Complete) requires a rating of 3+ stars. The retroactive "Save as Method" action on Session
Detail has no rating requirement — any completed session's parameters can be saved as a method.

**Save dialog:** Displays the session rating, brewing parameters as icon chips (respecting the
user's temperature and volume unit preferences), and an editable label field pre-filled with
the auto-generated label.

**Usage:** When logging a new session for a tea that has saved configurations, a "Choose Method"
dialog presents available configurations for quick pre-fill. The dialog shows each configuration's
label, source rating, and usage count. A "Recommended" badge is shown on the first configuration.
The dialog does **not** display individual brewing parameters (temperature, time, water quantity)
or the associated vessel name inline — the user must recognize configurations by label.

### 4.10 Session History

**Purpose:** Browse and filter all past brewing sessions.

**Display:** Sessions grouped by date, showing tea name, type color, vessel, brewing parameters,
rating, and timestamp.

**Filters (via bottom sheet):**

- Tea type
- Specific tea
- Date range (start/end)
- Minimum rating

**Filter badge** shows active filter count.

**Actions per session:**

- Tap to view Session Detail
- Brew again
- Delete (with confirmation)

**Search:** Text search across session data.

### 4.11 Session Detail

**Purpose:** View full details of a completed session.

**Single-steep view:**

- Tea name, type, vessel
- All brewing parameters (temperature, water qty, tea qty, brewing time, water type)
- Rating display
- Notes
- Photos with fullscreen viewer
- Timestamp

**Multi-steep view:**

- Parent session header
- List of all steeps with individual parameters and ratings
- Average rating across steeps
- Add steep action (with parameter adjustment dialog) — creates a new child steep with
  `status = IN_PROGRESS` and navigates to the Timer screen; the parent session status
  is not changed
- Delete individual steep (with confirmation)

**Actions:** Edit session, Delete session, Brew again, Save as Method.

**Save as Method:** An `OutlinedButton` in the bottom bar (above "Brew Again") lets users save
the parent session's brewing parameters as a reusable configuration at any time after the fact.
Tapping it:
1. Checks for a duplicate configuration — if one exists, shows an error snackbar and stops
2. Otherwise opens the `SaveConfigurationDialog` pre-filled with an auto-generated label
3. User edits the label if desired, then confirms to save
No rating is required for this path. For multi-steep sessions, the parent (first) steep's
parameters define the method.

### 4.12 Edit Session

**Purpose:** Modify a completed session's details.

**Modes:**

- **Edit individual steep** - Modify one steep's parameters
- **Edit full session** - Modify all parameters including tea, vessel, etc.

### 4.13 Analytics

**Purpose:** Visualize brewing patterns and discover insights.

**Period selection:** Last 7 Days, Last 30 Days, Last 90 Days, This Week, This Month, Last
Month, This Year, All Time, Custom Range. All named periods are persisted to user preferences
and restored on next launch. Custom date ranges are **not** persisted — the period reverts to
the last named period on app restart.

**Empty state:** Shown when fewer than 10 completed sessions exist. A progress bar indicates
how many sessions are needed to unlock analytics (`X / 10 sessions completed`).

**Summary card:**

- Total sessions
- Total brewing time (formatted)
- Total water consumed (formatted in preferred unit)
- Unique teas brewed
- Average rating
- Rated sessions count

**Charts and visualizations:**

- **Brewing Trends** - Line chart of sessions over time (trend points)
- **Tea Type Distribution** - Pie/donut chart of session counts by tea type
- **Top Teas** - Bar chart of most-brewed teas
- **Top Rated Teas** - Bar chart of highest-rated teas
- **Vessel Distribution** - Chart of vessel usage frequency
- **Brewing Activity Heatmap** - GitHub-style activity grid showing brewing frequency per day

**Steep Insights:**

- Average steeps per session
- Top re-steeped tea
- New tea discoveries count

**AI-style Insights** (rule-based):

- Most brewed tea
- Average rating trend
- Brewing consistency
- Favorite tea type
- Re-steep habits
- Top rated tea
- Preferred vessel

### 4.14 Settings

**Purpose:** App configuration and data management.

**Sections:**

- **Temperature unit** - Toggle between Celsius and Fahrenheit
- **Volume unit** - Toggle between Milliliters and Fluid Ounces
- **Default water type** - Dropdown to select the water type pre-selected when logging a new session or quick timer session
- **Tea Type management** - Navigate to tea type list
    - View, create, edit, and delete custom tea types
    - System defaults cannot be deleted
    - Each type has: name, default temperature, color
- **Data export** - Export all data (teas, sessions, vessels, configurations, tea types) to a
  file
- **Data import** - Import data from a file, with result summary showing counts of imported items

### 4.15 Deep Links / Notifications

The app supports deep linking via `DeepLinkHandler`, which routes notification taps to the
correct screen. Platform code calls `setRoute()`, and the navigation layer collects and consumes
pending routes.

**Current deep link targets:** Any `NavRoute` can be a deep link target (primarily used for
timer completion notifications).

### 4.16 iOS Live Activity

**Purpose:** Show the active brew timer on the iOS Lock Screen and in the Dynamic Island without
requiring the user to open the app.

**Trigger:** Starts when a timer begins on iOS; updates on every timer tick; ends when the timer
completes, is reset, or the session is discarded.

**Data shown:**

| Surface | Content |
|---------|---------|
| Dynamic Island — compact leading | Leaf icon |
| Dynamic Island — compact trailing | Countdown (MM:SS) or paused indicator |
| Dynamic Island — expanded center | Tea name + steep number |
| Dynamic Island — expanded trailing | Countdown |
| Lock Screen | Tea name, "Steep N · Steeping/Paused", countdown |

**Architecture:** Kotlin `TimerService` emits timer state → `TimerNotificationService` (iosMain)
calls the `LiveActivityService` interface → Swift `LiveActivityManager` calls `ActivityKit` APIs.
Static data (`teaName`, `steepNumber`, `totalSeconds`, `sessionId`) is set at activity start;
dynamic data (`remainingSeconds`, `isPaused`, `endDate`) updates on each tick.

---

## 5. User Flows

### 5.1 First-Time User

1. Open app → Home screen with empty state
2. System defaults are initialized (tea types, default vessel)
3. User taps FAB → "Log Tea"
4. Prompted to create first tea (name + type required)
5. Selects vessel (default available)
6. Sets brewing parameters
7. Starts timer or saves session directly

### 5.2 Log and Brew (Full Flow)

1. Home → FAB → "Log Tea"
2. Select tea (search or create new)
3. Parameters pre-filled from: saved configuration for this tea + vessel > saved configuration
   for this tea type + vessel > no pre-fill
4. Adjust parameters as needed
5. Tap "Start Timer" → Timer screen
6. Timer counts down with visual ring
7. Timer completes → Steep Complete screen
8. User pours tea, drinks at their own pace
9. Rate and add notes when ready (optional, not gated)
10. Optionally start next steep (returns to Timer, then back to Steep Complete)
11. "Finish Session" when done → optionally save configuration
12. Return to Home (updated stats and recent sessions)

### 5.3 Quick Timer Flow

1. Home → FAB → "Quick Timer"
2. Select duration from sheet
3. Timer starts immediately
4. Optionally fill in tea/vessel/parameters via details sheet
5. Timer completes → if session details provided, transitions to Steep Complete screen; otherwise
   timer resets (simple countdown mode)

### 5.4 Brew Again

1. From Home (recent session) or Session Detail → "Brew Again"
2. Navigates to Log Tea, pre-filled with previous session's tea, vessel, and parameters
3. User adjusts and starts timer

### 5.5 Multi-Steep Session

1. Complete first steep via Timer → Steep Complete screen
2. Pour and drink; rate and note this steep's character (e.g., "light, floral opening")
3. Tap "Start Next Steep" → adjust duration/temperature/water
4. New child session created with incremented steep number → Timer starts
5. Steep completes → back to Steep Complete screen for the new steep
6. Previous steeps visible below with their ratings and notes; still editable
7. Rate new steep, note how the tea has evolved (e.g., "fuller body, honey notes")
8. Repeat steps 3–7 as desired — the session builds into a steep-by-steep tasting journal
9. "Finish Session" when leaves are spent → average rating recalculated across all steeps

### 5.6 Configuration Management

1. Tea Detail → "Add Brewing Method"
2. Select vessel, set parameters
3. Save with label
4. Next time logging this tea: "Choose Method" dialog appears
5. Select configuration → parameters pre-filled

### 5.7 Retroactive Save as Method

For users who skipped the save prompt at session completion (or never rated a session):

1. Open any completed session from History or Home
2. Session Detail → tap "Save as Method" (OutlinedButton in bottom bar)
3. If a matching configuration already exists → error snackbar; no dialog shown
4. Otherwise → `SaveConfigurationDialog` opens, pre-filled with auto-generated label and the
   parent session's brewing parameters
5. Edit label if desired → tap "Save Method"
6. Configuration is created and available immediately in Tea Detail → Brewing Configurations

---

## 6. Non-Functional Requirements

### 6.1 Data Storage

- All data stored locally via Room database
- Timer state persisted to database for survival across process death
- User preferences stored via platform preferences (DataStore / NSUserDefaults)
- Photos stored as file paths to local storage

### 6.2 Unit Support

- Temperature: Celsius / Fahrenheit (stored as Celsius internally, converted for display)
- Volume: Milliliters / Fluid Ounces (stored as mL internally, converted for display)
- All conversions are lossless (stored value is the source of truth)

### 6.3 Data Portability

- Full export to file (JSON format covering all entity types)
- Import from exported file with per-entity-type counts

### 6.4 Performance

- Lazy loading for lists (sessions, teas)
- Flow-based reactive data (Room → Repository → ViewModel → UI)
- Timer precision via coroutine-based countdown with ceiling-rounded display

### 6.5 Input Validation Ranges

Validated on submission; no inline range hints are displayed on the fields themselves.

| Field | Rule |
|-------|------|
| Temperature | 0–100 °C (0–212 °F converted to Celsius for validation) |
| Water quantity | Must be > 0 |
| Brewing time | Must be > 0 |
| Tea name | Required (non-empty) |
| Tea type | Required |
| Vessel | Required for session creation |

---

## 7. Future Roadmap

### 7.1 Firebase Sync & Integration

**Priority:** High
**Status:** Not started (data model has `SyncStatus` and `userId` fields prepared)

**Goals:**

- Cloud backup and restore via Firebase
- Multi-device sync (e.g., phone + tablet)
- User authentication (Firebase Auth)
- Real-time sync with conflict resolution (SyncStatus enum already defines SYNCED, PENDING,
  SYNCING, CONFLICT, ERROR, LOCAL_ONLY states)

**Technical notes:**

- Domain models already include `userId`, `syncStatus`, `createdAt`, `updatedAt`, and
  `deletedAt` fields
- Soft-delete pattern (`deletedAt`) is already in place for sync compatibility
- Conflict resolution strategy TBD

### 7.2 Timer Completion Notifications

**Priority:** High
**Status:** Implemented on both platforms. In-app completion (foreground) navigates directly to
Steep Complete with no additional sound or haptic in the shared layer.

**Implemented:**

- Android: notification fires on a HIGH importance channel when the timer completes while
  backgrounded; OS delivers sound/vibration per user settings. Notification tap deep-links to
  Steep Complete.
- iOS: `UNNotificationRequest` fires with default sound when the timer completes while
  backgrounded. Notification tap deep-links to Steep Complete. Live Activity also ends at
  completion (see section 4.16).

**Gap:** When the app is in the foreground at the moment of completion, there is no in-app
sound or haptic — the screen transition to Steep Complete is the only signal. On Android,
the notification may be suppressed by the OS when the app is foregrounded.

### 7.3 Home Screen Widgets

**Priority:** Medium
**Status:** Not started

**Goals:**

- Quick-glance daily brewing stats (session count, water consumed, teas brewed)
- Quick timer launch directly from widget
- Display current in-progress timer status

**Platforms:**

- Android: Jetpack Glance (Compose-based widgets)
- iOS: WidgetKit

### 7.4 Known Issues & Technical Debt

**Active issues:**

- **Quick-add tea inline not rendering** — `LogTeaState.showQuickAddTeaDialog`, the
  `QuickAddTeaClicked` intent, and the ViewModel handler are all wired up, but the dialog
  composable is never rendered in `LogTeaScreen.kt`. Tapping "+ Add New Tea" sets the flag
  but shows nothing.
- **Tea Detail empty state missing** — When a tea has zero logged sessions or zero saved
  brewing configurations, those sections render nothing rather than an empty-state message.
- **WaterType.OTHER has no custom label** — Selecting "Other" for water type stores the enum
  value but provides no field for the user to specify what "Other" means.
- **Photo management has no constraints** — No maximum photo count, file size limit, or
  error handling for picker failures.
- **`purchasePrice` and `stockAmount` not surfaced** — Both fields exist in the domain model
  and database but are not shown in any UI screen. Decision needed: build the feature or
  remove the fields.
- **Choose Method dialog lacks parameter detail** — The dialog shows label, rating, and usage
  count but not the vessel name or brewing parameters. Users with multiple similarly-named
  configurations cannot distinguish them without guessing.

**Previously resolved:**

- ~~`isLoading` race condition in `HomeViewModel.loadData()`~~ — Fixed: coordinated loading state
  across all parallel collectors using `MutableStateFlow` flags
- ~~Dead `recentSessions` field~~ — False alarm: field is `recentSessionsWithTea` and is actively
  used by UI
- ~~`deleteSession()` swallows errors~~ — False alarm: `.onFailure` handler sets error state shown
  to user
- ~~Duplicate flows from `refresh()`~~ — False alarm: `loadJob?.cancel()` correctly cancels child
  coroutines via structured concurrency

### 7.5 Future Feature Backlog

- **Stock amount tracking** — Decrement `stockAmount` when logging a session; surface
  remaining stock on Tea Detail; "running low" indicator.
- **Purchase price / cost analytics** — Display cost per session or cost per gram on Tea
  Detail and Analytics.
- **Undo for destructive actions** — Snackbar with undo action after deleting sessions,
  teas, or configurations (soft-delete infrastructure is already in place).
- **In-progress session constraint messaging** — Explicit explanation when the FAB is hidden
  (e.g., text on the banner: "Finish your current session to start a new one").
- **Analytics custom date range persistence** — Persist the selected custom start/end dates
  to user preferences so the range survives app restarts.
- **Timer completion in-app signal** — Sound or haptic feedback when the timer completes
  while the app is in the foreground.

---

## 8. Glossary

| Term | Definition |
|------|-----------|
| **Steep** | A single infusion of tea leaves in water. One session can have multiple steeps. |
| **Parent session** | The first steep in a multi-steep session (`parentSessionId = null`). |
| **Child steep** | A subsequent steep linked to a parent session via `parentSessionId`. |
| **Brewing configuration** | A saved set of brewing parameters (vessel, temp, time, water qty) for a specific tea. |
| **Quick Timer** | A lightweight timer that can be started without pre-logging a full session. |
| **Steep Complete** | Post-brew companion screen shown after a timer finishes. The user drinks, rates, takes notes, and starts the next steep or finishes — all at their own pace. |
| **Draft / In-progress** | A session with `status = IN_PROGRESS`, typically one with an active timer or on the Steep Complete screen between steeps. |
| **Gongfu** | A Chinese tea brewing method using small vessels, high leaf-to-water ratios, and many short steeps. |
| **Pre-fill** | Automatic population of brewing parameters from a saved configuration, tea defaults, or previous session. |
