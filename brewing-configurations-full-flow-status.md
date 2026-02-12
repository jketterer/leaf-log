# Brewing Configurations - Full User Flow Status

## ✅ Completed Features

### 1. Save Configuration After 5⭐ Session

**When**: User edits a session steep and rates it 5⭐
**What Happens**:
- After saving, `SaveConfigurationDialog` automatically appears
- Shows brewing parameters with auto-generated label (e.g., "Gong-fu Style", "Western Style")
- User can edit label or skip
- Saves configuration to database

**Files Modified**:
- `EditSessionViewModel.kt` - Added logic to show dialog after 5⭐ save
- `EditSessionState.kt` - Added `showSaveConfigurationDialog` and `savedSession`
- `EditSessionIntent.kt` - Added `SaveConfigurationClicked` and `SkipSaveConfiguration`
- `EditSessionScreen.kt` - Integrated `SaveConfigurationDialog` component

**Status**: ✅ **Fully Implemented**

---

### 2. Smart Pre-fill with Multiple Configurations

**When**: User selects tea + vessel in Log Tea Screen
**What Happens**:
- System loads all saved configurations for this combination
- Auto-selects best configuration (highest rated, most used)
- Pre-fills all brewing parameters
- Shows banner with "Change" button if multiple methods exist
- Clicking "Change" shows `ChooseMethodDialog` to select different method
- Selecting "Custom" clears pre-filled parameters

**Files Modified**:
- `LogTeaViewModel.kt` - Added configuration loading and selection logic
- `LogTeaState.kt` - Added `availableConfigurations`, `usedConfigurationId`, `showChooseMethodDialog`
- `LogTeaIntent.kt` - Added `ChooseDifferentMethodClicked`, `MethodSelected`, `DismissChooseMethodDialog`
- `LogTeaScreen.kt` - Integrated `ChooseMethodDialog`
- `PrefillBanner.kt` - Added "Change" button for multiple methods
- `GetBrewingParametersPrefillUseCase.kt` - Updated to check saved configurations first

**Status**: ✅ **Fully Implemented**

---

### 3. Configuration Usage Tracking

**When**: User selects a configuration from the method selector
**What Happens**:
- `usedConfigurationId` is tracked in state
- When configuration is selected, `incrementTimesUsed()` is called
- Updates `timesUsed` counter and `lastUsedAt` timestamp

**Files Modified**:
- `LogTeaViewModel.kt` - Calls `incrementTimesUsed()` when method selected

**Status**: ✅ **Fully Implemented**

---

### 4. UI Components

All Phase 2 UI components have been created:

✅ `SaveConfigurationDialog` - Save configuration after great session
✅ `ConfigurationCard` - Display saved configuration with stats
✅ `EditConfigurationDialog` - Edit configuration parameters
✅ `ChooseMethodDialog` - Select between multiple configurations
✅ `SavedMethodsSection` - Section for Tea Detail Screen
✅ `PrefillBanner` - Shows pre-fill source with "Change" button

**Status**: ✅ **All Components Created**

---

## 🚧 Remaining Work

### 1. Tea Detail Screen Integration

**What's Needed**:
Add saved configurations section to Tea Detail Screen showing all methods for this tea.

**Implementation**:

1. Update `TeaDetailViewModel`:
```kotlin
// Add to constructor
private val brewingConfigurationRepository: BrewingConfigurationRepository,
private val brewingVesselRepository: BrewingVesselRepository,
private val deleteBrewingConfigurationUseCase: DeleteBrewingConfigurationUseCase,
private val updateBrewingConfigurationUseCase: UpdateBrewingConfigurationUseCase,

// Add to state
val configurations: List<BrewingConfiguration> = emptyList(),
val vessels: List<BrewingVessel> = emptyList(),
val showEditConfigDialog: Boolean = false,
val editingConfig: BrewingConfiguration? = null,

// Add to loadTea()
viewModelScope.launch {
    brewingConfigurationRepository.getByTeaIdFlow(teaId)
        .collect { configs ->
            _state.update { it.copy(configurations = configs) }
        }

    brewingVesselRepository.getAllFlow()
        .collect { vessels ->
            _state.update { it.copy(vessels = vessels) }
        }
}

// Add intents
is TeaDetailIntent.EditConfigurationClicked -> {
    val config = brewingConfigurationRepository.getById(intent.configId)
    _state.update { it.copy(editingConfig = config, showEditConfigDialog = true) }
}

is TeaDetailIntent.DeleteConfigurationClicked -> {
    deleteBrewingConfigurationUseCase(intent.configId)
}

is TeaDetailIntent.SaveConfigurationChanges -> {
    val config = _state.value.editingConfig ?: return
    updateBrewingConfigurationUseCase(...)
    _state.update { it.copy(showEditConfigDialog = false, editingConfig = null) }
}
```

2. Update `TeaDetailScreen`:
```kotlin
// In LazyColumn, after recent sessions section
if (state.configurations.isNotEmpty()) {
    item(key = "saved_methods") {
        SavedMethodsSection(
            configurations = state.configurations,
            getVesselName = { vesselId ->
                state.vessels.find { it.id == vesselId }?.name ?: "Unknown"
            },
            onEdit = { configId ->
                viewModel.onIntent(TeaDetailIntent.EditConfigurationClicked(configId))
            },
            onDelete = { configId ->
                viewModel.onIntent(TeaDetailIntent.DeleteConfigurationClicked(configId))
            }
        )
    }
}

// Show EditConfigurationDialog when needed
if (state.showEditConfigDialog && state.editingConfig != null) {
    EditConfigurationDialog(
        configuration = state.editingConfig,
        onSave = { label, teaQty, waterQty, temp, time, waterType, isActive ->
            viewModel.onIntent(TeaDetailIntent.SaveConfigurationChanges(...))
        },
        onDismiss = { viewModel.onIntent(TeaDetailIntent.DismissEditConfig) }
    )
}
```

**Estimated Effort**: 30-45 minutes

---

### 2. Track Used Configuration in Sessions

**What's Needed**:
Store which configuration was used when creating a session, so we can show stats like "This method produces 4.8⭐ average rating".

**Implementation**:

1. Add field to `TeaSessionEntity`:
```kotlin
@ColumnInfo(name = "used_configuration_id")
val usedConfigurationId: String? = null,
```

2. Create migration to add column

3. Update `CreateSessionUseCase` to accept `usedConfigurationId` parameter

4. Pass `usedConfigurationId` from `LogTeaViewModel` when saving session:
```kotlin
createSessionUseCase(
    // ...existing parameters...
    usedConfigurationId = currentState.usedConfigurationId,
)
```

**Estimated Effort**: 20-30 minutes

---

### 3. Configuration Update Prompt (Optional Enhancement)

**What's Needed**:
When user modifies pre-filled parameters and rates session 4-5⭐, ask if they want to update the configuration.

**Implementation**:

1. Track original pre-filled values in state
2. After saving session, compare with current values
3. If changed and rating >= 4, show dialog:
   - "Update 'Gong-fu Style' Configuration?"
   - "You changed temperature 85°C → 87°C and rated it 5⭐"
   - [Update Configuration] [Save as New Method] [No Thanks]

**Estimated Effort**: 45-60 minutes

---

### 4. Better Tea Name in SaveConfigurationDialog

**What's Needed**:
Currently shows "Tea" placeholder - need to pass actual tea name.

**Implementation**:

In `EditSessionViewModel`, load tea name:
```kotlin
// When showing dialog
viewModelScope.launch {
    val tea = teaRepository.getById(session.teaId)
    _state.update { it.copy(teaName = tea?.name) }
}
```

Add `teaName` to `EditSessionState` and pass to dialog.

**Estimated Effort**: 10 minutes

---

## 📊 Current Implementation Status

| Feature | Status | Notes |
|---------|--------|-------|
| Database Layer | ✅ Complete | Table, DAO, migration, repository all done |
| Domain Models | ✅ Complete | All models and use cases created |
| Save After 5⭐ | ✅ Complete | Fully functional in EditSessionScreen |
| Smart Pre-fill | ✅ Complete | Loads configurations, pre-fills parameters |
| Multiple Method Selection | ✅ Complete | ChooseMethodDialog working |
| Usage Tracking | ✅ Complete | incrementTimesUsed() called on selection |
| UI Components | ✅ Complete | All dialogs and components created |
| Tea Detail Integration | ⚠️ Pending | Need to add SavedMethodsSection to screen |
| Session Tracking | ⚠️ Pending | Need to store usedConfigurationId in sessions |
| Update Prompt | ⚠️ Optional | Enhancement for future |
| Tea Name in Dialog | ⚠️ Minor | Small improvement needed |

---

## 🎯 Next Steps

### Immediate (Required for Full Flow)
1. **Integrate into Tea Detail Screen** (30-45 min)
   - Show saved methods section
   - Wire up edit/delete handlers
   - Add EditConfigurationDialog

2. **Track Configuration Usage in Sessions** (20-30 min)
   - Add field to session entity
   - Update create session flow
   - Create migration

### Nice to Have (Enhancements)
3. **Configuration Update Prompt** (45-60 min)
   - Detect parameter changes
   - Show update dialog
   - Handle update vs save-as-new

4. **Polish** (10 min)
   - Fix tea name in SaveConfigurationDialog
   - Add any missing error handling

---

## 🧪 Testing Checklist

Once remaining work is complete:

### Happy Path
- [ ] Create 5⭐ session steep → SaveConfigurationDialog appears
- [ ] Save configuration with custom label
- [ ] Log new session with same tea + vessel → Parameters pre-fill from saved config
- [ ] Multiple configurations exist → "Change" button appears in banner
- [ ] Click "Change" → ChooseMethodDialog shows all methods
- [ ] Select different method → Parameters update correctly
- [ ] Tea Detail Screen shows saved methods section
- [ ] Edit configuration → Changes apply to future sessions
- [ ] Delete configuration → Removed from list

### Edge Cases
- [ ] Save configuration without custom label → Uses auto-generated label
- [ ] Skip save configuration → Session saved, no config created
- [ ] Create configuration for tea bag (no quantity) → Label = "Tea Bag Method"
- [ ] Configuration usage counter increments correctly
- [ ] lastUsedAt timestamp updates
- [ ] Delete tea → Cascades to configurations
- [ ] Delete vessel → Cascades to configurations

---

## 💡 Usage Flow Example

### Scenario: User discovers their perfect brewing method

1. **First brew** (no history):
   - User selects Dragon Well + Gaiwan
   - No configurations exist → fields are empty
   - User enters: 5g, 100ml, 85°C, 30s
   - Completes session, rates 5⭐

2. **SaveConfigurationDialog appears**:
   - Shows: "5g • 100ml • 85°C • 30s"
   - Suggested label: "Gong-fu Style"
   - User edits to: "My Perfect Gong-fu"
   - Clicks "Save Method"

3. **Next time**:
   - User selects Dragon Well + Gaiwan
   - Banner: "Using your 5⭐ method from 2 days ago"
   - All parameters pre-filled automatically
   - User brews, rates 5⭐ again

4. **Configuration tracking**:
   - `timesUsed` increments to 1
   - `lastUsedAt` updates to now
   - Configuration builds history

5. **Multiple methods**:
   - User creates "Quick Western" method (different parameters, 4⭐)
   - Now two configurations exist
   - Banner shows "Change" button
   - User can switch between methods easily

6. **Tea Detail Screen**:
   - Shows both configurations
   - "My Perfect Gong-fu" - 5⭐, used 12 times
   - "Quick Western" - 4⭐, used 3 times
   - User can edit or delete

---

## 🚀 What's Already Working

You can test right now:
1. Edit a session steep and rate it 5⭐
2. SaveConfigurationDialog will appear
3. Save it with a label
4. Go to Log Tea Screen
5. Select same tea + vessel
6. Parameters will auto-fill from your saved configuration!

The core loop is **fully functional** - only Tea Detail Screen integration and session tracking remain!
