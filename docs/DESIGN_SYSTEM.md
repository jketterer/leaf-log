# Design System

Design system and component library for Leaf Log.

## Theme

### Material 3

Uses Compose Material 3 with custom color scheme. Theme composable: `LeafLogTheme(useDarkTheme)`.

### Color Scheme

**Light Theme**:

| Role | Hex | Description |
|---|---|---|
| Primary | `#4C662B` | Olive green (tea/nature) |
| onPrimary | `#FFFFFF` | |
| PrimaryContainer | `#CCEDA3` | Light sage |
| onPrimaryContainer | `#0F2000` | |
| Secondary | `#596248` | Muted brown-green |
| onSecondary | `#FFFFFF` | |
| SecondaryContainer | `#DDE7C7` | Pale sage |
| onSecondaryContainer | `#171E0A` | |
| Tertiary | `#7D5635` | Warm brown |
| onTertiary | `#FFFFFF` | |
| TertiaryContainer | `#FFDCC2` | Peach |
| onTertiaryContainer | `#2E1500` | |
| Error | `#BA1A1A` | |
| ErrorContainer | `#FFDAD6` | |
| Background | `#FEFCF5` | Off-white cream |
| Surface | `#FEFCF5` | |
| onSurface | `#1B1C18` | Near-black |
| SurfaceVariant | `#E1E4D4` | |
| Outline | `#75796C` | |
| OutlineVariant | `#C5C8B9` | |
| InverseSurface | `#30312C` | |
| InversePrimary | `#B0D18A` | |
| SurfaceContainer | `#EEEFE4` | |
| SurfaceContainerHigh | `#E8E9DE` | |
| SurfaceContainerHighest | `#E2E3D9` | |
| SurfaceContainerLow | `#F4F4EA` | |
| SurfaceContainerLowest | `#FFFFFF` | |

**Dark Theme**:

| Role | Hex |
|---|---|
| Primary | `#B0D18A` (light sage) |
| onPrimary | `#203600` |
| PrimaryContainer | `#364E16` |
| Secondary | `#C1CAAB` |
| onSecondary | `#2B331D` |
| SecondaryContainer | `#414A32` |
| Tertiary | `#EFBD94` (peach) |
| onTertiary | `#48290D` |
| TertiaryContainer | `#623F20` |
| Error | `#FFB4AB` |
| Background/Surface | `#1B1C18` |
| onSurface | `#E4E3DB` |
| SurfaceVariant | `#44483D` |
| Outline | `#8E9285` |
| InverseSurface | `#E4E3DB` |
| InversePrimary | `#4C662B` |
| SurfaceContainer | `#2F302B` |
| SurfaceContainerHigh | `#3A3B35` |
| SurfaceContainerHighest | `#454640` |
| SurfaceContainerLow | `#242520` |
| SurfaceContainerLowest | `#0E0F0B` |

**Aesthetic**: Warm, earthy, minimalist — tea-focused with olive green primary and warm brown accent.

### Typography

Uses Material 3 default typography scale (no custom fonts):
- Display: Large, Medium, Small
- Headline: Large, Medium, Small
- Title: Large, Medium, Small
- Body: Large, Medium, Small
- Label: Large, Medium, Small

### Shapes

Material 3 default rounded corner shapes:
- Medium: 12dp (cards, dialogs)
- Small: 8dp (photo thumbnails, small cards)
- Large: Material 3 defaults (buttons, FABs)

---

## Component Library

### Input Components

#### TemperatureInputField
**File**: `components/common/TemperatureInputField.kt`

OutlinedTextField with tappable unit suffix that toggles between °C and °F.

| Prop | Type | Description |
|---|---|---|
| `value` | `String` | Current temperature value |
| `onValueChange` | `(String) -> Unit` | Value change callback |
| `currentUnit` | `TemperatureUnit` | Current display unit |
| `onToggleUnit` | `() -> Unit` | Toggle °C/°F |
| `label` | `String` | Field label |
| `modifier` | `Modifier` | |
| `isError` | `Boolean` | Error state |
| `supportingText` | `String?` | Error/helper text |
| `enabled` | `Boolean` | |

#### VolumeInputField
**File**: `components/common/VolumeInputField.kt`

OutlinedTextField with tappable unit suffix that toggles between mL and fl oz.

| Prop | Type | Description |
|---|---|---|
| `value` | `String` | Current volume value |
| `onValueChange` | `(String) -> Unit` | |
| `currentUnit` | `VolumeUnit` | Current display unit |
| `onToggleUnit` | `() -> Unit` | Toggle mL/fl oz |
| `label` | `String` | |
| `modifier`, `isError`, `supportingText`, `enabled` | | Same as above |

#### DurationPicker
**File**: `components/common/DurationPicker.kt`

Two-field picker (minutes/seconds) that syncs from external Duration prop.

| Prop | Type | Description |
|---|---|---|
| `duration` | `Duration?` | External duration value |
| `onDurationChange` | `(Duration) -> Unit` | Change callback |
| `label` | `String` | |
| `modifier` | `Modifier` | |
| `isError` | `Boolean` | |
| `errorMessage` | `String?` | |
| `syncKey` | `Any?` | Triggers re-sync from external value when changed |

Validates seconds (0-59). Auto-selects text on focus.

#### EditCapacityField
**File**: `components/common/EditCapacityField.kt`

Wrapper around VolumeInputField for vessel capacity editing.

#### WaterTypeSelector
**File**: `components/common/WaterTypeSelector.kt`

ExposedDropdownMenu with WaterType enum values.

| Prop | Type |
|---|---|
| `selectedWaterType` | `WaterType` |
| `onWaterTypeSelected` | `(WaterType) -> Unit` |
| `label` | `String` |
| `modifier` | `Modifier` |

#### VesselSelector
**File**: `components/common/VesselSelector.kt`

ExposedDropdownMenu showing vessel icon/image + name + capacity per item.

| Prop | Type |
|---|---|
| `vessels` | `List<BrewingVessel>` |
| `selectedVessel` | `BrewingVessel?` |
| `onVesselSelected` | `(BrewingVessel) -> Unit` |
| `label` | `String` |
| `volumeUnit` | `VolumeUnit` |
| `modifier` | `Modifier` |
| `isError` | `Boolean` |
| `errorMessage` | `String?` |
| `imageStorage` | `ImageStorage` |

#### PresetChips
**File**: `components/common/PresetChips.kt`

Generic horizontal scrollable FilterChip list.

| Prop | Type |
|---|---|
| `presets` | `List<Preset<T>>` |
| `currentValue` | `T?` |
| `onSelect` | `(T) -> Unit` |
| `modifier` | `Modifier` |
| `isSelected` | `(Preset<T>, T?) -> Boolean` |

Uses LazyRow with FilterChip. Supports custom selection comparison logic.

#### RatingSelector
**File**: `components/session/RatingSelector.kt`

Interactive 5-star rating selector.

| Prop | Type |
|---|---|
| `rating` | `Float` |
| `onRatingChange` | `(Float) -> Unit` |
| `modifier` | `Modifier` |

Tap a star to select (1-5). Tap current rating to clear (set to 0).

---

### Display Components

#### RatingDisplay
**File**: `components/common/RatingDisplay.kt`

Read-only star rating with optional number.

| Prop | Type | Description |
|---|---|---|
| `rating` | `Float` | Rating value (0-5) |
| `iconsOnly` | `Boolean` | If true, hide numeric label |

Shows filled stars (FontAwesomeIcons.Solid.Star) and outlined stars (FontAwesomeIcons.Regular.Star). Icon size: 18dp, spacing: 2dp.

#### TempDisplay
**File**: `components/common/TempDisplay.kt`

Formatted temperature display.

| Prop | Type |
|---|---|
| `tempCelsius` | `Double` |
| `tempUnit` | `TemperatureUnit` |
| `style` | `TextStyle` |
| `color` | `Color` |
| `modifier` | `Modifier` |

#### BrewingParamChip
**File**: `components/common/BrewingParamChip.kt`

Compact chip showing an icon + text for a brewing parameter.

| Prop | Type |
|---|---|
| `icon` | `ImageVector` |
| `text` | `String` |

RoundedCornerShape(6dp), secondaryContainer background.

Includes helper function: `formatBrewingTime(seconds): String`.

#### EmptyState
**File**: `components/common/EmptyState.kt`

Centered message with optional action button.

| Prop | Type |
|---|---|
| `message` | `String` |
| `actionText` | `String?` |
| `onActionClick` | `(() -> Unit)?` |
| `modifier` | `Modifier` |

---

### Card Components

#### TeaCard
**File**: `components/collection/TeaCard.kt`

| Prop | Type |
|---|---|
| `tea` | `Tea` |
| `teaTypeName` | `String` |
| `onTeaClick` | `() -> Unit` |
| `modifier` | `Modifier` |
| `teaTypeColorHex` | `String?` |
| `onFavoriteClick` | `(() -> Unit)?` |
| `trailingContent` | `@Composable (() -> Unit)?` |
| `imageStorage` | `ImageStorage` |

Features:
- 6dp left accent border colored by tea type
- Tea photo thumbnail (64x64dp, rounded)
- Name, type, origin
- Stats: brew count, average rating
- Favorite toggle or custom trailing content

#### SessionCard
**File**: `components/session/SessionCard.kt`

| Prop | Type |
|---|---|
| `session` | `TeaSession` |
| `teaName`, `teaTypeName`, `vesselName` | `String` |
| `teaPhotoUrl` | `String?` |
| `userPrefs` | `UserPreferences` |
| `onSessionClick` | `() -> Unit` |
| `onBrewAgainClick` | `(() -> Unit)?` |
| `onDeleteClick` | `(() -> Unit)?` |
| `canBrewAgain` | `Boolean` |
| `modifier` | `Modifier` |
| `imageStorage` | `ImageStorage` |

Features:
- Session/tea photo (48x48dp)
- Tea name, type, vessel
- Brewing parameters as chips (FlowRow)
- Relative timestamp + rating
- Notes (truncated)
- IN_PROGRESS badge (errorContainer background)
- More menu: Brew Again + Delete

#### VesselCard
**File**: `components/vessel/VesselCard.kt`

| Prop | Type |
|---|---|
| `vessel` | `BrewingVessel` |
| `volumeUnit` | `VolumeUnit` |
| `onClick` | `() -> Unit` |
| `modifier` | `Modifier` |
| `imageStorage` | `ImageStorage` |

Vessel image/icon (40x40dp) + name + capacity. ChevronRight trailing icon.

#### ConfigurationCard
**File**: `components/configuration/ConfigurationCard.kt`

| Prop | Type |
|---|---|
| `configuration` | `BrewingConfiguration` |
| `vesselName` | `String` |
| `userPreferences` | `UserPreferences` |
| `onEdit` | `() -> Unit` |
| `onDelete` | `() -> Unit` |
| `modifier` | `Modifier` |

Features:
- Label + optional rating
- Vessel name + water quantity subtitle
- Stats: times used, last used
- Parameter chips (tea qty, temp, time, water)
- Edit + Delete icon buttons
- Dimmed if `!isActive`

#### RecentSessionCard
**File**: `components/home/RecentSessionCard.kt`

Compact session card for home screen. 48x48dp photo, single-line info.

#### DailyStatsCard
**File**: `components/home/DailyStatsCard.kt`

| Prop | Type |
|---|---|
| `value` | `String` |
| `label` | `String` |
| `icon` | `ImageVector` |
| `onClick` | `(() -> Unit)?` |
| `modifier` | `Modifier` |
| `containerColor` | `Color` |
| `contentColor` | `Color` |

Large stat value (auto-scaled if overflow), label, icon.

#### SummaryCard
**File**: `components/analytics/SummaryCard.kt`

| Prop | Type |
|---|---|
| `value` | `String` |
| `label` | `String` |
| `modifier` | `Modifier` |
| `percentageChange` | `Float?` |
| `icon` | `ImageVector?` |
| `onClick` | `(() -> Unit)?` |

Headline-size stat + label + optional trend indicator (up/down arrow with %).

---

### Photo & Media Components

#### PhotoGrid
**File**: `components/common/PhotoGrid.kt`

| Prop | Type |
|---|---|
| `photos` | `List<String>` |
| `onAddPhoto` | `(ByteArray) -> Unit` |
| `onRemovePhoto` | `(String) -> Unit` |
| `modifier` | `Modifier` |
| `enabled` | `Boolean` |
| `imageStorage` | `ImageStorage` |

Horizontal LazyRow of 80x80dp photo thumbnails. Delete button overlay on each (when enabled). Tap to open fullscreen viewer. PhotoPickerButton below for adding.

#### PhotoPickerButton
**File**: `components/common/PhotoPickerButton.kt`

Launches FileKit file picker (image type). Camera icon.

| Prop | Type |
|---|---|
| `onPhotoPicked` | `(PlatformFile) -> Unit` |
| `modifier` | `Modifier` |
| `text` | `String` |

#### FullscreenImageViewer
**File**: `components/common/FullscreenImageViewer.kt`

| Prop | Type |
|---|---|
| `photos` | `List<String>` |
| `initialIndex` | `Int` |
| `onDismiss` | `() -> Unit` |

Features:
- Dialog with fullscreen zoomable images
- HorizontalPager for multi-image navigation
- Pinch-to-zoom (1-5x), pan with bounds checking
- Double-tap to zoom 2x or reset
- Photo counter badge (if > 1)
- Close button (top-left)
- Black background (95% alpha)

---

### Dialogs & Sheets

#### CompleteInProgressDialog
**File**: `components/session/CompleteDraftDialog.kt`

Complete an in-progress session inline. Shows session summary, rating selector, notes field. Buttons: "Complete Session" | "Save for Later".

#### SaveConfigurationDialog
**File**: `components/configuration/SaveConfigurationDialog.kt`

Save brewing parameters as a reusable method. Shows tea + vessel info, parameter chips, editable label field. Buttons: "Save Method" | "Not Now".

#### HistoryFilterSheet
**File**: `components/history/HistoryFilterSheet.kt`

ModalBottomSheet with filter options: tea type chips, tea dropdown, date range presets (Today/This Week/This Month), minimum rating selector, Clear All button.

#### PrefillBanner
**File**: `components/common/PrefillBanner.kt`

Info banner showing the source of pre-filled parameters.

| Prop | Type |
|---|---|
| `source` | `PrefillSource` |
| `teaName` | `String` |
| `modifier` | `Modifier` |
| `hasMultipleMethods` | `Boolean` |
| `onChooseDifferentMethod` | `(() -> Unit)?` |

Messages:
- SavedConfig: "Using your saved brewing method for {teaName}"
- SameTypeConfig: "Suggested parameters based on how you brew {sourceTeaName}"

SecondaryContainer background, RoundedCornerShape(8dp).

---

### Chart Components

#### CircularTimerRing
**File**: `components/timer/CircularTimerRing.kt`

| Prop | Type | Default |
|---|---|---|
| `progress` | `Float` | 0-1 |
| `timeText` | `String` | Center text |
| `modifier` | `Modifier` | 240dp size |
| `ringColor` | `Color` | primary |
| `backgroundColor` | `Color` | surfaceVariant |

Canvas-based circular progress ring. 16dp stroke width, rounded caps. 300ms tween animation. 360° arc from top (-90°). Auto-scaled center text (displayLarge, bold).

#### BrewingTrendsChart
**File**: `components/analytics/BrewingTrendsChart.kt`

Line chart of sessions over time. Canvas-drawn with tap interaction via `detectTapGestures`. Supports daily/weekly/monthly granularity.

#### TeaTypeDistributionChart
**File**: `components/analytics/TeaTypeDistributionChart.kt`

Donut chart showing tea type breakdown with percentages. Canvas-drawn with tap gestures.

#### TopTeasChart
**File**: `components/analytics/TopTeasChart.kt`

Horizontal bar chart showing top 5 teas by brew count. Canvas-drawn.

#### TopRatedTeasChart
**File**: `components/analytics/TopRatedTeasChart.kt`

List of top-rated teas with star ratings.

#### VesselDistributionChart
**File**: `components/analytics/VesselDistributionChart.kt`

Donut chart showing vessel usage breakdown. Canvas-drawn with tap gestures.

#### BrewingActivityHeatmap
**File**: `components/analytics/BrewingActivityHeatmap.kt`

Activity heatmap grid (similar to GitHub contribution graph). Cells colored by dominant tea type color. Daily or weekly granularity based on period.

---

### Layout Components

#### SteepParameterCard
**File**: `components/common/SteepParameterCard.kt`

Displays brewing parameters for a single steep. Has two overloads:
1. Takes `TeaSession` + `UserPreferences` — extracts and formats values
2. Takes individual params: `steepNumber`, `teaQuantity`, `waterQuantity`, `temperature`, `brewingTime`

ParameterRow layout: icon + label + value. Icons: tea leaf (resource), Droplet, Thermometer, Clock.

#### BrewingParameterDisplay
**File**: `components/session/BrewingParameterDisplay.kt`

Simple row: label (left) + value (right, bold). bodyMedium text.

#### ExpandableFAB
**File**: `components/home/ExpandableFAB.kt`

| Prop | Type |
|---|---|
| `expanded` | `Boolean` |
| `onExpandedChange` | `(Boolean) -> Unit` |
| `onLogSessionClick` | `() -> Unit` |
| `onQuickTimerClick` | `() -> Unit` |
| `visible` | `Boolean` |
| `modifier` | `Modifier` |

Main FAB with Plus icon (rotates 45° to X when expanded). Two sub-FABs:
- "Quick Timer" (Clock icon) — secondaryContainer
- "Log Session" (Edit icon) — secondaryContainer

Labels appear to the left. Animated: vertical expand + fade.

#### GreetingHeader
**File**: `components/home/GreetingHeader.kt`

| Prop | Type |
|---|---|
| `greeting` | `String` |
| `userName` | `String?` |
| `modifier` | `Modifier` |

Greeting text (headlineMedium, bold) + current date subtitle (bodyMedium).

#### TeaFilterChips
**File**: `components/collection/TeaFilterChips.kt`

Horizontal scrollable FilterChips: "All", "Favorites" (with Heart icon), per-TeaType filters.

#### QuickAdjustButtons
**File**: `components/timer/QuickAdjustButtons.kt`

| Prop | Type |
|---|---|
| `onAdjust` | `(Duration) -> Unit` |
| `enabled` | `Boolean` |
| `modifier` | `Modifier` |

Two rows of time adjustment buttons:
- Row 1 (fine): -10s, -5s, +5s, +10s (OutlinedButton)
- Row 2 (coarse): -1m, -30s, +30s, +1m (FilledTonalButton)

---

### Vessel Components

#### VesselIcons
**File**: `components/vessel/VesselIcons.kt`

Custom SVG vessel icons built with `ImageVector.Builder`:
- **Mug**: Rectangular body + D-shaped handle
- **Gaiwan**: Saucer + trapezoid bowl + arched lid with knob
- **Kyusu**: Rounded rect body + front spout + yokode arc handle
- **TeaCup**: Tapered cup on saucer (no handle)

All: Black stroke (2px), round caps/joins, 24x24dp default.

#### VesselIconHelper
**File**: `components/vessel/VesselIconHelper.kt`

Maps `vessel.iconName` string → `ImageVector` via VesselIcons object.

---

## Icon System

### Primary: Feather Icons
Package: `br.com.devsrsouza.compose.icons:feather`

Common icons used: Clock, Droplet, Thermometer, Edit, Edit2, Trash2, MoreVertical, ChevronRight, Plus, X, Camera, Coffee, Layers, Bookmark, Info, Download, BarChart2, TrendingUp, PieChart, ArrowUp, ArrowDown, Search, Filter, Heart, Star, Settings, Home.

### Secondary: Font Awesome
Package: `br.com.devsrsouza.compose.icons:font-awesome`

Used primarily for ratings:
- `FontAwesomeIcons.Solid.Star` — filled star
- `FontAwesomeIcons.Regular.Star` — outlined star
- `FontAwesomeIcons.Regular.PlusSquare` — empty state action icon

### Custom Icons
- **Vessel icons**: Mug, Gaiwan, Kyusu, TeaCup (ImageVector paths)
- **Tea leaf**: Drawable resource (`Res.drawable.ic_tea_leaf`)

### Icon Sizing Convention

| Context | Size |
|---|---|
| Menu/close/nav icons | 24dp |
| Parameter chips, ratings | 18-20dp |
| Indicators, helpers | 12-16dp |
| Inside chips/badges | 10-14dp |

---

## Common Patterns

### Unit Toggle

Temperature and volume inputs have tappable suffixes that toggle units. The toggle persists the preference immediately via PreferencesRepository. All values are stored in Celsius/mL and converted for display.

### Photo Handling

- **Storage**: ImageStorage (platform-specific) manages files in subdirectories
- **Display**: Coil AsyncImage for async loading with caching
- **Selection**: FileKit library for cross-platform file picker
- **Removal**: Delete icon overlay on photo grid items
- **Fullscreen**: Zoomable viewer with paging support

### Preset Chips

Generic `PresetChips<T>` component used for:
- Tea quantity presets (1g, 2g, 3g, ...)
- Brewing time presets (0:30, 1:00, 2:00, ...)
- Temperature presets (60°C, 70°C, 80°C, ...)
- Water volume presets (100mL, 200mL, 250mL, ...)

### Duration Input

DurationPicker with minutes and seconds fields. External Duration prop syncs internal state via `syncKey` to prevent overwriting user edits.

### Rating Display vs Selector

- **Display** (RatingDisplay): Read-only stars with optional numeric label
- **Selector** (RatingSelector): Interactive 5-star tap input, tap current to clear

### Empty States

Centered message + optional action button using EmptyState component.

---

## Spacing & Layout Conventions

| Spacing | Usage |
|---|---|
| 4dp | Minor text adjustments |
| 6-8dp | Gaps between chip items |
| 12dp | Standard internal padding |
| 16dp | Standard screen horizontal padding |
| 24-32dp | Section spacing |
| 48dp | Photo thumbnails (session cards) |
| 64dp | Tea card photos |
| 80dp | Photo grid items |
| 240dp | Timer ring diameter |

### Elevation

- Cards: 2dp default
- Dialogs: Material 3 defaults

---

## Animation & Interaction

### Transitions
- CircularTimerRing: 300ms tween for progress changes
- ExpandableFAB: Fade + vertical expand/shrink
- FAB icon rotation: 45° animated float

### Gestures
- FullscreenImageViewer: Pinch-to-zoom (1-5x), pan, double-tap zoom
- Chart components: `pointerInput` + `detectTapGestures` for data point selection

### Focus Behavior
- DurationPicker: Auto-selects text on field focus
- ImeAction.Next/Done for field progression
