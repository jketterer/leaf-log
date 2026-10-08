# Features

Complete feature guide for Leaf Log — every screen, user flow, state shape, and intent.

## Navigation Routes

20 serializable routes defined in `NavRoute.kt`:

| Route | Parameters | Description |
|---|---|---|
| `HomeRoute` | — | Main dashboard |
| `CollectionRoute` | — | Tea & vessel collection |
| `HistoryRoute` | `filterTeaTypeId?`, `filterTeaId?`, `filterDateStart?`, `filterDateEnd?` | Session history |
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

Bottom navigation tabs: Home, Collection, History, Analytics.

---

## Screens

### Home Screen

**Purpose**: Dashboard with daily stats, in-progress sessions, and recent activity.

**State** (`HomeState`):
- `greeting: String` — Time-based greeting ("Good morning", "Good afternoon", etc.)
- `dailyStats: DailyStats` — Today's session count, water consumed, unique teas
- `recentSessionsWithTea: List<SessionWithTeaData>` — Last 5 sessions with tea/type metadata
- `inProgressSessionsCount: Int` — Active sessions count
- `mostRecentInProgress: InProgressSessionInfo?` — Current brewing session info
- `liveTimerState: TimerState?` — Real-time timer state from TimerService
- `userPreferences: UserPreferences`
- `isFabExpanded: Boolean` — Expandable FAB menu state
- `showDurationSheet: Boolean` — Quick timer duration picker
- `sessionPendingDelete: String?` — Delete confirmation state
- `isLoading, isRefreshing, error, isEmpty`

**Intents** (`HomeIntent`):
- `LoadData`, `Refresh`
- `LogTeaClicked`, `QuickTimerClicked`, `StartQuickTimer(durationSeconds)`, `DismissDurationSheet`
- `FabExpandedChanged(expanded)`
- `SessionClicked(sessionId)`, `BrewAgainClicked(sessionId)`
- `DeleteSessionClicked(sessionId)`, `ConfirmDeleteSession`, `CancelDeleteSession`
- `ViewAllSessionsClicked`, `ViewAllStatsClicked`
- `DailyStatsTodaySessionsClicked`, `DailyStatsWaterCardClicked`, `DailyStatsTeasCardClicked`
- `ResumeInProgressClicked`
- `SettingsClicked`, `ClearError`

**UI Layout**:
1. Greeting header with current date
2. Daily stats cards (sessions, water, unique teas) — each tappable
3. In-progress session banner with resume button (if active)
4. Recent sessions list — session cards with brew again, delete, and view actions
5. Expandable FAB: "Log Session" + "Quick Timer" sub-buttons

---

### Log Tea Screen (Session Wizard)

**Purpose**: Multi-step wizard to create a new brewing session.

**State** (`LogTeaState`):
- *Tea Selection*: `selectedTea`, `selectedTeaType`, `availableTeas`, `teaSearchQuery`, `showTeaSearchDialog`
- *Vessel*: `selectedVessel`, `availableVessels`
- *Brewing Parameters*: `teaQuantityGrams`, `isTeaBag`, `waterQuantityMl/Display`, `temperatureCelsius/Display`, `brewingTime`, `selectedWaterType`, `location`
- *Prefill*: `prefillSource`, `usedConfigurationId`, `availableConfigurations`, `showChooseMethodDialog`
- *Completion Dialog*: `showCompleteSessionDialog`, `completionDialogNotes`, `completionRating`
- *Validation Errors*: `teaError`, `vesselError`, `teaQuantityError`, `waterQuantityError`, `temperatureError`, `brewingTimeError`
- `isLoading`, `isSaving`, `error`, `userPreferences`

**Intents** (`LogTeaIntent`):
- Tea: `ShowTeaSearchDialog`, `HideTeaSearchDialog`, `TeaSearchQueryChanged(query)`, `TeaSelected(teaId)`, `QuickAddTeaClicked`
- Vessel: `VesselSelected(vesselId)`
- Parameters: `TeaBagModeChanged(isTeaBag)`, `TeaQuantityChanged(qty)`, `WaterQuantityChanged(qty)`, `TemperatureChanged(temp)`, `BrewingTimeChanged(duration)`, `WaterTypeSelected(waterType)`, `LocationChanged(location)`
- Units: `ToggleTemperatureUnit`, `ToggleVolumeUnit`
- Methods: `ChooseDifferentMethodClicked`, `MethodSelected(configId)`, `DismissChooseMethodDialog`
- Actions: `StartTimerClicked`, `SaveAsCompleted`
- Completion: `CompletionDialogNotesChanged(notes)`, `CompletionRatingChanged(rating)`, `ConfirmCompleteSession`, `DismissCompleteSessionDialog`
- `BackClicked`, `ClearError`

**UI Layout**:
1. Tea selection (card display or search button)
2. Vessel selector (dropdown)
3. Brewing parameters (gated on tea + vessel selection):
   - Tea bag vs loose leaf toggle
   - Tea quantity with preset chips (1g, 2g, 3g, ...)
   - Brewing time picker with presets (0:30, 1:00, 2:00, ...)
   - Temperature with unit toggle and presets
   - Water quantity with unit toggle and presets
4. Optional details (collapsible): water type, location
5. Prefill banner (if using saved method or same-type suggestion)
6. Action buttons: "Start Session" FAB, "Complete without timer" menu option

**Prefill Logic**: When tea + vessel are selected, `GetBrewingParametersPrefillUseCase` checks for saved configurations (exact match → same type match → none).

---

### Tea Collection Screen

**Purpose**: Browse and manage tea collection.

**State** (`TeaCollectionState`):
- `teas: List<Tea>`, `teaTypes: List<TeaType>`
- `selectedFilter: FilterType` — ALL, FAVORITES, BY_TYPE
- `selectedTypeId: String?` — For BY_TYPE filter
- `selectedTab: CollectionTab` — TEAS or VESSELS
- `searchQuery: String`
- `selectedSortOption: TeaSortOption`
- `isLoading`, `isEmpty`, `error`

**Intents** (`TeaCollectionIntent`):
- `LoadData`
- `ShowAllTeas`, `ShowFavorites`, `FilterByType(typeId)`
- `SearchQueryChanged(query)`, `TeaClicked(teaId)`
- `ToggleFavorite(tea)`, `TabSelected(tab)`, `SortSelected(option)`
- `AddTeaClicked`, `ClearError`

**UI Layout**:
- Tab row: "Teas" | "Vessels"
- Search bar (toggle visibility)
- Filter chips: All, Favorites, per-type
- Sort menu
- Tea cards with accent border, photo, stats, favorite toggle
- Vessel cards with icon, capacity
- FAB: Add tea or vessel based on active tab

---

### Tea Detail Screen

**Purpose**: View tea info, stats, saved methods, and session history.

**State** (`TeaDetailState`):
- `tea: Tea?`, `teaType: TeaType?`
- `recentSessions: List<TeaSession>` — Last brews
- `configurations: List<BrewingConfiguration>` — Saved methods
- `vessels: List<BrewingVessel>`
- `showDeleteConfirmation: Boolean`
- `userPreferences: UserPreferences`
- `isLoading`, `error`

**Intents** (`TeaDetailIntent`):
- `LoadTea(teaId)`, `EditTeaClicked`, `DeleteTeaClicked`, `ConfirmDelete`, `CancelDelete`
- `ToggleFavorite`
- `SessionClicked(sessionId)`, `BrewAgainClicked(sessionId)`, `ViewAllSessionsClicked`
- `BrewThisTeaClicked(vesselId?)`
- `AddConfigurationClicked`, `EditConfigurationClicked(configId)`, `DeleteConfigurationClicked(configId)`
- `BackClicked`, `ClearError`

**UI Layout**:
1. Photo carousel (if photos exist)
2. Stats: total brews, average rating, producer, origin, purchase date
3. Saved brewing methods section — configuration cards with "Brew Using..." buttons
4. Recent sessions list
5. Action buttons: Edit, Delete, "Brew This Tea"

---

### Edit Tea Screen

**Purpose**: Create or edit a tea entry.

**State** (`EditTeaState`):
- `isEditMode: Boolean`
- Form: `name`, `selectedTeaTypeId`, `origin`, `producer`, `purchaseDate`, `defaultTemperatureCelsius`, `description`, `photos`
- `availableTeaTypes: List<TeaType>`, `availableProducers: List<String>`
- Validation: `nameError`, `teaTypeError`, `temperatureError`
- `showDiscardConfirmation: Boolean`
- `userPreferences: UserPreferences`

**Intents** (`EditTeaIntent`):
- `LoadTea(teaId?)`
- `NameChanged(name)`, `TeaTypeSelected(typeId)`, `OriginChanged(origin)`, `ProducerChanged(producer)`, `PurchaseDateChanged(date)`, `DefaultTemperatureChanged(temp)`, `DescriptionChanged(desc)`
- `PhotoSelected(imageBytes)`, `PhotoRemoved(photoPath)`
- `ToggleTemperatureUnit`
- `SaveClicked`, `BackClicked`, `ConfirmDiscard`, `CancelDiscard`

---

### History Screen

**Purpose**: View and filter all past brewing sessions.

**State** (`HistoryState`):
- `allSessions: List<TeaSession>` — Unfiltered
- `groupedSessions: Map<String, List<TeaSession>>` — Grouped by time period (Today, Yesterday, This Week, etc.)
- `teas`, `teaTypes`, `vessels` — Maps for display
- *Filters*: `searchQuery`, `selectedTeaTypeId`, `selectedTeaId`, `dateRangeStart/End`, `minRating`
- `hasActiveFilters: Boolean`, `activeFilterCount: Int`
- `showFilterSheet: Boolean`
- `userPreferences: UserPreferences`

**Intents** (`HistoryIntent`):
- `LoadData`
- `SearchQueryChanged(query)`
- `ShowFilterSheet`, `HideFilterSheet`
- Filters: `FilterByTeaType(typeId?)`, `FilterByTea(teaId?)`, `FilterByDateRange(start?, end?)`, `FilterByMinRating(rating?)`, `ClearFilters`
- `SessionClicked(sessionId)`, `BrewAgain(sessionId)`, `DeleteSession(sessionId)`, `ConfirmDeleteSession`, `CancelDeleteSession`
- `CompleteInProgress(sessionId)`, `ClearError`

**Filter Sheet**:
- Tea type filter chips
- Tea dropdown (filtered by selected type)
- Date range presets: Today, This Week, This Month
- Minimum rating selector
- Clear All button

Accepts filter parameters from route and applies them via `LaunchedEffect`.

---

### Session Detail Screen

**Purpose**: View a complete brewing session with all steeps.

**State** (`SessionDetailState`):
- `parentSession: TeaSession?`, `childSteeps: List<TeaSession>`
- `tea`, `teaType`, `vessel` — Related models
- `allSteeps: List<TeaSession>` — Computed (parent + sorted children)
- `isSingleSteep/isMultiSteep: Boolean`
- Multi-steep: `showAddSteepDialog`, `nextSteepDuration/Temperature`
- Config save: `showSaveConfigurationDialog`, `suggestedConfigurationLabel`
- `userPreferences: UserPreferences`

**Intents** (`SessionDetailIntent`):
- `LoadSession(sessionId)`
- `EditSessionClicked`, `EditSteepClicked(steepId)`
- `DeleteSessionClicked`, `ConfirmDelete`, `CancelDelete`
- `DeleteSteepClicked(steepId)`, `ConfirmDeleteSteep`
- `BrewAgainClicked`
- `AddSteepClicked`, `CancelAddSteep`, `UpdateNextSteepDuration(duration?)`, `UpdateNextSteepTemperature(temp)`, `ConfirmAddSteep`
- `SaveAsConfigurationClicked`, `ConfirmSaveConfiguration(label)`, `DismissSaveConfiguration`
- `ViewTeaClicked(teaId)`, `BackClicked`, `ClearError`

**UI Layout**:
1. Session header: tea name, vessel, date, overall rating
2. Steep cards with individual parameters, rating, notes
3. If multi-steep: "Add Steep" button
4. Action buttons row: Edit, Delete, Brew Again, Save Configuration

---

### Edit Session Screen

**Purpose**: Modify session parameters or individual steep details.

**State** (`EditSessionState`):
- `existingSession: TeaSession?`
- `isParentSession: Boolean` — Determines editable fields
- `steepNumber: Int?`, `teaName: String?`
- Common fields: `brewingTime`, `temperatureCelsius/Display`, `notes`, `photos`, `rating`
- Parent-only fields: `selectedVessel`, `selectedWaterType`, `waterQuantityMl/Display`, `teaQuantityGrams`, `isTeaBag`, `location`
- Validation errors
- `showDiscardConfirmation`, `userPreferences`

**Intents** (`EditSessionIntent`):
- `LoadSession(sessionId, editFullSession)`
- Common: `BrewingTimeChanged(duration)`, `TemperatureChanged(temp)`, `RatingChanged(rating)`, `NotesChanged(notes)`, `PhotoSelected(bytes)`, `PhotoRemoved(path)`, `ToggleTemperatureUnit`
- Parent-only: `VesselSelected(vessel)`, `WaterTypeSelected(waterType)`, `WaterQuantityChanged(qty)`, `TeaQuantityChanged(qty)`, `TeaBagModeChanged(isTeaBag)`, `LocationChanged(location)`, `ToggleVolumeUnit`
- `SaveClicked`, `BackClicked`, `ConfirmDiscard`, `CancelDiscard`
- `SaveConfigurationClicked(label?)`, `SkipSaveConfiguration`

---

### Timer Screen

**Purpose**: Countdown timer for active brewing session.

**State** (`TimerScreenState`):
- `timerState: TimerState` — From TimerService
- `session: TeaSession?`, `tea: Tea?`, `vessel: BrewingVessel?`
- `formattedTime: String` — "MM:SS" display
- Confirmations: `showCompleteNowConfirmation`, `showDiscardSessionConfirmation`, `showResetConfirmation`
- Edit sheet: `showEditSheet`, `editTemperatureCelsius`, `editWaterQuantityMl`, `editTeaQuantityGrams`, `editIsTeaBag`, `editWaterType`
- `userPreferences: UserPreferences`

**Intents** (`TimerIntent`):
- `Initialize(sessionId)`
- `StartTimer`, `PauseTimer`, `ResumeTimer`
- `ResetTimer`, `ConfirmReset`, `CancelReset`
- `AdjustTime(adjustment)` — ±Duration
- `CompleteNow`, `ConfirmCompleteNow`, `CancelCompleteNow`
- `DiscardSession`, `ConfirmStop`, `CancelStop`, `BackClicked`
- Edit: `EditSession`, field changes, `ConfirmEditSession`, `CancelEditSession`, unit toggles

**UI Layout**:
1. Top bar: tea name, vessel name
2. Brewing parameters card (editable via icon)
3. Circular timer ring with MM:SS center text
4. Quick adjust buttons: ±5s, ±10s, ±30s, ±1m
5. Control buttons: Start / Pause / Resume
6. Overflow menu: Complete Now, Cancel Session

**Auto-completion**: When timer reaches zero, auto-navigates to `SteepCompleteRoute`.

---

### Quick Timer Screen

**Purpose**: Timer-first interface — optionally add session details while brewing.

**State** (`QuickTimerState`):
- Timer: `totalDuration`, `remainingDuration`, `status`, `progress`, `formattedTime`
- Optional details: `selectedTea`, `selectedVessel`, `teaQuantityGrams`, `isTeaBag`, `temperatureDisplay`, `waterQuantityDisplay`, `waterType`, `prefillSource`
- Notes: `rating`, `notes`
- Options: `availableTeas`, `availableVessels`, `teaSearchQuery`
- Sheet: `showDetailsSheet`, `detailsSheetStep` (1=tea/vessel, 2=params)
- Dialogs: `showNextSteepDialog`, `showStopConfirmation`, `showResetConfirmation`
- `inProgressSession: TeaSession?`
- `userPreferences: UserPreferences`

**Intents** (`QuickTimerIntent`):
- `Initialize(durationSeconds)`
- Timer: `StartTimer`, `PauseTimer`, `ResumeTimer`, `ResetTimer`, `ConfirmReset`, `CancelReset`, `StopTimer`, `ConfirmStop`, `CancelStop`, `AdjustTime(adjustment)`
- Sheet: `ShowDetailsSheet`, `HideDetailsSheet`, `NextDetailsStep`, `PreviousDetailsStep`
- Tea/vessel: `TeaSearchQueryChanged(query)`, `TeaSelected(teaId)`, `VesselSelected(vesselId)`
- Parameters: `TeaQuantityChanged(qty)`, `TeaBagModeChanged(isTeaBag)`, `TemperatureChanged(temp)`, `WaterQuantityChanged(qty)`, `WaterTypeSelected(waterType)`, unit toggles
- Notes: `RatingChanged(rating?)`, `NotesChanged(notes)`
- `BackClicked`

**UI Layout**:
1. Circular timer with countdown
2. Quick adjust buttons
3. Control buttons: Start/Pause/Resume/Reset/Stop
4. Optional details sheet (collapsible):
   - Step 1: Tea & vessel selection
   - Step 2: Brewing parameters

---

### Steep Complete Screen

**Purpose**: Post-steep completion — rate, add notes, continue steeping, or finish.

**State** (`SteepCompleteState`):
- `session`, `tea`, `vessel` — Current steep
- `rating: Float`, `notes: String`, `photos: List<String>`
- `previousSteeps: List<TeaSession>` — Prior steeps in session
- Multi-steep dialog: `showNextSteepDialog`, `nextSteepDuration/Temperature/WaterQuantity`
- Config save: `showSaveConfigurationDialog`, `suggestedConfigurationLabel`
- Edit sheet: `showEditParametersSheet`, edit field values
- `userPreferences: UserPreferences`

**Intents** (`SteepCompleteIntent`):
- `Initialize(sessionId)`
- `RatingChanged(rating)`, `NotesChanged(notes)`
- Photos: `PhotoSelected(imageBytes)`, `PhotoRemoved(path)`
- Next steep: `ShowNextSteepDialog`, `DismissNextSteepDialog`, `NextSteepDurationChanged(duration?)`, `NextSteepTemperatureChanged(temp)`, `NextSteepWaterQuantityChanged(qty)`, `ConfirmNextSteep`
- `FinishSession`
- Discard: `ShowDiscardConfirmation`, `ConfirmDiscard`, `DismissDiscardConfirmation`
- Config: `SaveConfigurationClicked(label?)`, `SkipSaveConfiguration`
- Previous steeps: `PreviousSteepRatingChanged(steepId, rating)`, `PreviousSteepNotesChanged(steepId, notes)`
- Edit: `ShowEditParametersSheet`, `DismissEditParametersSheet`, field changes, `ConfirmEditParameters`, `CancelEditParameters`
- Unit toggles, `BackClicked`

**UI Layout**:
1. Steep info indicator ("Steep 2 of 3")
2. Rating selector (5 stars)
3. Notes textarea
4. Photo gallery
5. Previous steeps summary (collapsible, editable ratings/notes)
6. Edit parameters button
7. Action buttons: "Next Steep", "Finish Session", "Discard Session", "Save as Brewing Method"

---

### Analytics Screen

**Purpose**: Statistics, trends, and insights on brewing habits.

**State** (`AnalyticsState`):
- `selectedPeriod: AnalyticsPeriod`
- `customDateRange: Pair<LocalDate, LocalDate>?`
- `analytics: AnalyticsData?`, `comparison: PeriodComparison?`
- `insights: List<Insight>`
- `formattedWaterQuantity/formattedBrewingTime: String`
- `periodLabel: String`
- Charts: `trendPoints`, `trendGranularity`, `teaTypeDistribution`, `topTeas`, `topRatedTeas`, `vesselDistribution`, `activityCells`, `steepInsights`
- `hasMinimumData: Boolean`, `totalCompletedSessions: Int`
- `showPeriodSelector: Boolean`

**Intents** (`AnalyticsIntent`):
- `LoadAnalytics`
- `ShowPeriodSelector`, `HidePeriodSelector`
- `SelectPeriod(period)`, `SelectCustomRange(start, end)`
- `NavigateToLogTea`
- Chart taps: `TapTrendPoint(date)`, `TapTeaType(typeId)`, `TapTopTea(teaId)`, `TapTopRatedTea(teaId)`, `TapVessel(vesselId)` — all navigate to History with filters
- `ToggleWaterUnit`

**UI Layout**:
1. Period selector: preset chips + custom range
2. KPI summary cards: Sessions, total water, avg time, avg rating (with % change vs previous period)
3. Brewing trends line chart
4. Tea type distribution donut chart
5. Top teas bar chart
6. Top rated teas with star ratings
7. Vessel usage donut chart
8. Activity heatmap (day/week grid with tea type colors)
9. Steep insights (avg steeps, top re-steeped, new discoveries)
10. Generated insights cards

---

### Settings Screen

**Purpose**: App preferences and data management.

**State** (`SettingsState`):
- `preferences: UserPreferences`
- `isExporting`, `isImporting: Boolean`
- `exportFilePath: String?`
- `showImportPicker: Boolean`
- `importResult: ImportResult?`

**Intents** (`SettingsIntent`):
- `UpdateTemperatureUnit(unit)`, `UpdateVolumeUnit(unit)`, `UpdateDefaultWaterType(waterType)`
- `ExportData`, `ExportCompleted`
- `ImportData`, `ImportFileSelected(filePath)`, `ImportCancelled`
- `DismissImportResult`, `ClearError`

**UI Layout**:
1. Preferences section: temperature unit, volume unit, default water type
2. Management navigation: Tea Types, Vessels
3. Data section: Export, Import (with result dialog)

---

### Tea Type Management

**Tea Type List** (`TeaTypeListScreen`):
- Shows all tea types with color indicator and display order
- FAB to add new type
- Intents: `LoadTeaTypes`, `TeaTypeClicked(typeId)`, `AddTeaTypeClicked`

**Edit Tea Type** (`EditTeaTypeScreen`):
- Name input, color hex picker, default temperature
- Intents: `LoadTeaType(typeId?)`, `NameChanged(name)`, `ColorSelected(hex)`, `SaveClicked`, `BackClicked`, `ConfirmDiscard`

---

### Vessel Management

**Vessel List** (`VesselListScreen`):
- Shows all vessels with icon, capacity
- FAB to add new vessel
- Intents: `LoadVessels`, `VesselClicked(vesselId)`, `AddVesselClicked`

**Vessel Detail** (`VesselDetailScreen`):
- Vessel info, usage count, capacity
- `canDelete` computed (must have >1 vessel, no sessions referencing it)
- Intents: `LoadVessel(vesselId)`, `EditVesselClicked`, `DeleteVesselClicked`

**Edit Vessel** (`EditVesselScreen`):
- Name, icon selector, capacity, photo
- Intents: `LoadVessel(vesselId?)`, `NameChanged(name)`, `IconSelected(iconName)`, `CapacityChanged(capacity)`, `ToggleVolumeUnit`, `PhotoSelected(bytes)`, `RemovePhoto`, `SaveClicked`, `BackClicked`, `ConfirmDiscard`

---

### Create Brewing Configuration Screen

**Purpose**: Save a tea + vessel + parameters combination for reuse.

**State** (`CreateBrewingConfigurationState`):
- `vessels: List<BrewingVessel>`, `selectedVessel: BrewingVessel?`
- `label: String` — Custom name
- Form: `teaQuantityGrams`, `waterQuantityMl/Display`, `temperatureCelsius/Display`, `brewingTime`, `waterType`
- `isEditMode: Boolean`, `editingConfigId: String?`
- `canSave: Boolean`, `userPreferences: UserPreferences`

**Intents** (`CreateBrewingConfigurationIntent`):
- `LoadData(teaId, configurationId?)`
- `SelectVessel(vesselId)`, `UpdateLabel(label)`, `UpdateTeaQuantity(qty)`, `UpdateWaterQuantity(qty)`, `UpdateTemperature(temp)`, `UpdateBrewingTime(duration)`, `SelectWaterType(waterType)`
- Unit toggles
- `Save`, `NavigateBack`

---

## Key User Flows

### Log & Brew Session

1. Home → "Log Tea" FAB
2. LogTea: Select tea → Select vessel → Fill parameters
3. "Start Session" → Timer screen (creates IN_PROGRESS session)
4. Timer: Countdown with pause/resume/adjust
5. Timer completes → SteepComplete screen
6. Rate, add notes → "Finish Session" (marks COMPLETED)

### Multi-Steep Session

1. First steep completes normally via Timer → SteepComplete
2. SteepComplete: Tap "Next Steep" → dialog pre-fills duration/temp/water from the just-completed steep; user adjusts as needed
3. Confirm → creates child session (parentSessionId = parent.id, steepNumber = N+1)
4. Timer starts for new steep
5. Repeat until user selects "Finish Session"
6. Parent session gets averageRating calculated across all steeps

### Brew Again

1. Home or History: "Brew Again" on a past session
2. Creates a new IN_PROGRESS session cloning the previous session's parameters (tea, vessel, all brewing params)
3. Navigates directly to the Timer screen

### Quick Timer

1. Home: "Quick Timer" FAB → Duration picker sheet
2. Select duration → QuickTimer screen
3. Timer runs immediately
4. Optionally: expand details sheet to add tea, vessel, parameters
5. On completion: if all details filled → option to save as session; otherwise just dismiss

### Save Brewing Configuration

1. After completing a session (SteepComplete or SessionDetail)
2. "Save as Brewing Method" → dialog with auto-generated label
3. Confirm → saves configuration linked to tea + vessel
4. Later: LogTea screen checks for saved configs and auto-populates

### Analytics Exploration

1. Analytics tab → period selector
2. View KPI cards, charts, heatmap
3. Tap any chart element (tea type, top tea, vessel, trend point)
4. Navigates to History with pre-applied filters

### Data Export/Import

1. Settings → "Export Data" → creates ZIP file
2. Share/save ZIP file externally
3. Later: Settings → "Import Data" → file picker
4. Merges imported data, shows count dialog
