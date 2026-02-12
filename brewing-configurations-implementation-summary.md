# Brewing Configurations - Implementation Summary

## Phase 1: Smart Pre-fill from History ✅

### What Was Implemented

1. **Domain Models**
   - `PrefillSource` - Tracks where pre-filled parameters came from
   - `BrewingParametersPrefill` - Contains pre-filled brewing parameters

2. **Use Case**
   - `GetBrewingParametersPrefillUseCase` - Smart fallback logic:
     1. Saved configuration (Phase 2)
     2. This tea + vessel session (rated ≥ 3 stars)
     3. Same tea type + vessel session
     4. Tea defaults
     5. Empty

3. **UI Components**
   - `PrefillBanner` - Displays informative banner showing where parameters came from

4. **ViewModel Integration**
   - Updated `LogTeaViewModel` to trigger pre-fill when both tea and vessel are selected
   - Added `prefillSource` to `LogTeaState`

### How It Works

When a user selects a tea + vessel combination:
- System searches for successful sessions (3+ stars)
- Pre-fills all brewing parameters automatically
- Shows banner explaining the source of parameters
- User can adjust as needed

---

## Phase 2: Explicit Save & Management ✅

### Database Layer

1. **Entity**: `BrewingConfigurationEntity`
   - Stores saved brewing configurations
   - Foreign keys to Tea and BrewingVessel
   - Tracks usage statistics (times used, last used, rating)
   - User can name and disable configurations

2. **DAO**: `BrewingConfigurationDao`
   - CRUD operations
   - Query by tea + vessel
   - Get best configuration (sorted by rating, usage)
   - Increment usage counter

3. **Migration**: `MIGRATION_2_3`
   - Creates `brewing_configurations` table
   - Creates necessary indices for performance
   - Added to both Android and iOS platform modules

### Domain Layer

1. **Model**: `BrewingConfiguration`
   - Domain representation of saved configuration
   - Includes all brewing parameters
   - Metadata (rating, times used, source session)

2. **Repository**: `BrewingConfigurationRepository`
   - Interface for configuration operations
   - Implementations in data layer

### Use Cases

1. **`GenerateConfigurationLabelUseCase`**
   - Auto-generates descriptive labels:
     - "Gong-fu Style" (short steep + high ratio)
     - "Western Style" (long steep + large volume)
     - "Tea Bag Method" (no quantity)
     - "Grandpa Style" (long steep + moderate volume)
     - "Quick Brew" or "Custom Method"

2. **`SaveBrewingConfigurationUseCase`**
   - Saves a configuration from a successful session
   - Validates rating (must be 3+ stars)
   - Accepts custom label or auto-generates

3. **`UpdateBrewingConfigurationUseCase`**
   - Updates existing configuration parameters
   - Change label, parameters, or active status

4. **`DeleteBrewingConfigurationUseCase`**
   - Deletes a configuration permanently

### UI Components

1. **`SaveConfigurationDialog`**
   - Appears after completing a 5⭐ session
   - Shows parameters summary
   - Editable label field
   - Save or Skip options

2. **`ConfigurationCard`**
   - Displays a saved configuration
   - Shows vessel, parameters, and stats
   - Edit and Delete actions

3. **`EditConfigurationDialog`**
   - Edit all configuration parameters
   - Toggle active status
   - Warning that changes affect future sessions

4. **`ChooseMethodDialog`**
   - Appears when multiple configurations exist
   - Radio list of configurations
   - Shows which is recommended
   - "Custom" option to enter manually

5. **`SavedMethodsSection`**
   - Section for Tea Detail Screen
   - Lists all configurations for a tea
   - Grouped by vessel

### Dependency Injection

- All repositories and use cases registered in DI modules
- DAO registered in platform modules (Android + iOS)
- Migration added to database builders

---

## How to Integrate Into Screens

### Timer Completion Screen (Future Work)

After user completes a session with 5⭐ rating:

```kotlin
// In TimerCompletionViewModel or similar
if (session.rating == 5f) {
    // Show save configuration dialog
    showSaveConfigurationDialog(session)
}

fun onSaveConfiguration(session: TeaSession, customLabel: String?) {
    viewModelScope.launch {
        saveBrewingConfigurationUseCase(session, customLabel)
            .onSuccess { /* Show success message */ }
            .onFailure { /* Show error */ }
    }
}
```

### Log Tea Screen (Future Work)

When user selects tea + vessel:

```kotlin
// Check if multiple configurations exist
val configs = brewingConfigurationRepository.getByTeaAndVessel(teaId, vesselId)

if (configs.size > 1) {
    // Show choose method dialog
    showChooseMethodDialog(configs)
} else {
    // Use best config (already handled by GetBrewingParametersPrefillUseCase)
}
```

### Tea Detail Screen (Future Work)

Add a new section showing saved configurations:

```kotlin
// In TeaDetailScreen composable
val configurations by viewModel.configurationsFlow.collectAsState()

SavedMethodsSection(
    configurations = configurations,
    getVesselName = { vesselId ->
        vessels.find { it.id == vesselId }?.name ?: "Unknown"
    },
    onEdit = { configId -> viewModel.onEditConfiguration(configId) },
    onDelete = { configId -> viewModel.onDeleteConfiguration(configId) }
)
```

---

## Database Schema

### brewing_configurations Table

| Column | Type | Description |
|--------|------|-------------|
| id | TEXT | Primary key |
| tea_id | TEXT | Foreign key to teas |
| vessel_id | TEXT | Foreign key to brewing_vessels |
| tea_quantity_grams | REAL | Optional tea quantity |
| water_quantity_ml | INTEGER | Water quantity |
| temperature_celsius | INTEGER | Temperature |
| brewing_time | INTEGER | Duration in milliseconds |
| water_type | TEXT | Water type enum |
| source_session_id | TEXT | Original session ID |
| rating | REAL | Session rating |
| times_used | INTEGER | Usage counter |
| last_used_at | INTEGER | Last used timestamp |
| label | TEXT | User-provided name |
| is_active | INTEGER | Active status (boolean) |
| created_at | INTEGER | Creation timestamp |
| updated_at | INTEGER | Update timestamp |

### Indices

- `tea_id`
- `vessel_id`
- `tea_id, vessel_id` (composite)
- `rating`
- `times_used`
- `last_used_at`

---

## Next Steps

To fully activate Phase 2, you need to:

1. **Update Timer Completion Flow**
   - Show `SaveConfigurationDialog` after 5⭐ sessions
   - Call `SaveBrewingConfigurationUseCase`

2. **Update Log Tea Screen**
   - Show `ChooseMethodDialog` when multiple configs exist
   - Add banner button to choose different method
   - Track which configuration was used for the session

3. **Update Tea Detail Screen**
   - Add `SavedMethodsSection` to display configurations
   - Implement edit/delete handlers
   - Show `EditConfigurationDialog` when editing

4. **Track Configuration Usage**
   - When a session is created using a configuration
   - Call `brewingConfigurationRepository.incrementTimesUsed()`
   - Update `lastUsedAt` timestamp

5. **Configuration Update Prompt** (User Flow 2.4 from plan)
   - Detect when user modifies pre-filled parameters
   - Show dialog: "Update configuration?" or "Save as new?"
   - Implement the update logic

---

## Testing Checklist

- [ ] Database migration runs successfully on fresh install
- [ ] Database migration runs successfully on upgrade from v2
- [ ] Can save a configuration from a 5⭐ session
- [ ] Auto-generated labels are accurate
- [ ] Can edit configuration parameters
- [ ] Can delete a configuration
- [ ] Can toggle configuration active status
- [ ] Pre-fill uses saved configuration first
- [ ] Pre-fill falls back to sessions correctly
- [ ] Multiple configurations show choice dialog
- [ ] Configuration stats (times used, last used) update correctly
- [ ] Tea Detail Screen shows saved methods
- [ ] Deleting a tea cascades to configurations
- [ ] Deleting a vessel cascades to configurations

---

## Files Created

### Database Layer
- `BrewingConfigurationEntity.kt`
- `BrewingConfigurationDao.kt`
- `Migrations.kt`

### Domain Layer
- `BrewingConfiguration.kt`
- `BrewingConfigurationRepository.kt`
- `PrefillSource.kt`
- `BrewingParametersPrefill.kt`

### Data Layer
- `BrewingConfigurationRepositoryImpl.kt`
- `BrewingConfigurationMapper.kt`

### Use Cases
- `GetBrewingParametersPrefillUseCase.kt` (Phase 1)
- `GenerateConfigurationLabelUseCase.kt`
- `SaveBrewingConfigurationUseCase.kt`
- `UpdateBrewingConfigurationUseCase.kt`
- `DeleteBrewingConfigurationUseCase.kt`

### UI Components
- `PrefillBanner.kt` (Phase 1)
- `SaveConfigurationDialog.kt`
- `ConfigurationCard.kt`
- `EditConfigurationDialog.kt`
- `ChooseMethodDialog.kt`
- `SavedMethodsSection.kt`

### DI & Platform
- Updated `RepositoryModule.kt`
- Updated `UseCaseModule.kt`
- Updated `PlatformModule.android.kt`
- Updated `PlatformModule.ios.kt`
- Updated `LeafLogDatabase.kt`

---

## Architecture Compliance

All implementations follow the existing architecture:

✅ Clean Architecture layers (domain, data, presentation)
✅ MVI pattern for UI state management
✅ Use cases for business logic
✅ Repository pattern for data access
✅ Koin for dependency injection
✅ Room for local database
✅ TypeConverters for Duration and Instant
✅ Mappers between entity and domain models

The feature is ready for integration into the UI screens!
