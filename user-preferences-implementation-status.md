# User Preferences Implementation Status

## Completed Components

### 1. Formatter Utilities ✓
- **TemperatureFormatter** - Formats temperatures according to user preference
  - `format(celsius, unit)` - Returns formatted string (e.g., "80°C" or "176°F")
  - `getUnitSymbol(unit)` - Returns just the symbol
  - `getInputLabel(unit)` - Returns label for input fields

- **VolumeFormatter** - Formats volumes according to user preference
  - `format(milliliters, unit)` - Returns formatted string (e.g., "200mL" or "6fl oz")
  - `getUnitSymbol(unit)` - Returns just the symbol
  - `getInputLabel(unit)` - Returns label for input fields

- **UnitConverter** - Converts between storage and display units
  - `inputTemperatureToCelsius()` - Converts user input to storage format
  - `celsiusToDisplayTemperature()` - Converts storage to display format
  - `inputVolumeToMilliliters()` - Converts user input to storage format
  - `millilitersToDisplayVolume()` - Converts storage to display format

### 2. Updated Screens (Partially)

#### LogTeaScreen ✓ (Partial)
- State includes `userPreferences`
- ViewModel loads preferences via `GetPreferencesUseCase`
- Input labels updated to use `TemperatureFormatter.getInputLabel()` and `VolumeFormatter.getInputLabel()`
- Input suffixes updated to use formatters

**Still needed**: Convert input values when saving/loading

#### SessionDetailScreen ✓ (Complete for display)
- State includes `userPreferences`
- ViewModel loads preferences
- Display values updated:
  - Water amount uses `VolumeFormatter.format()`
  - Temperature uses `TemperatureFormatter.format()`

## Remaining Work

### Screens That Need Updates

All screen files identified by the Explore agent need updates. Here's the pattern for each type:

### Pattern A: Display-Only Screens (Easier)

For screens that only **display** temperature/volume values (no input):

1. **Update State** - Add `userPreferences: UserPreferences = UserPreferences()`
2. **Update ViewModel** - Add `GetPreferencesUseCase` and load preferences
3. **Update UI** - Replace hardcoded displays with formatters

**Example (SessionDetailScreen pattern):**
```kotlin
// Before
value = "${session.temperatureCelsius}°C"

// After
value = TemperatureFormatter.format(
    session.temperatureCelsius,
    state.userPreferences.temperatureUnit
)
```

**Files needing this pattern:**
- ✓ SessionDetailScreen.kt (done)
- TimerScreen.kt
- TeaDetailScreen.kt
- SessionCard.kt
- RecentSessionCard.kt
- ConfigurationCard.kt
- SaveConfigurationDialog.kt

### Pattern B: Input Screens (More Complex)

For screens with **input fields** for temperature/volume:

1. **Update State** - Add `userPreferences: UserPreferences = UserPreferences()`
2. **Update ViewModel**:
   - Add `GetPreferencesUseCase` and load preferences
   - When loading prefill values, convert from storage to display:
     ```kotlin
     val displayTemp = UnitConverter.celsiusToDisplayTemperature(
         storedCelsius,
         state.userPreferences.temperatureUnit
     )
     ```
   - When saving, convert from display to storage:
     ```kotlin
     val storageCelsius = UnitConverter.inputTemperatureToCelsius(
         userInputValue.toInt(),
         state.userPreferences.temperatureUnit
     )
     ```
3. **Update UI** - Use formatters for labels/suffixes

**Files needing this pattern:**
- ✓ LogTeaScreen.kt (labels done, conversion logic needed)
- EditSessionScreen.kt
- EditConfigurationDialog.kt

### Detailed File List

#### High Priority (User-facing displays)
1. **TimerScreen.kt** (Pattern A)
   - Line 317: `"${state.session.temperatureCelsius}°C • ${state.session.waterQuantityMl}ml"`
   - Line 400: Session summary with temp and volume
   - Line 579: Next steep temperature input suffix

2. **TeaDetailScreen.kt** (Pattern A)
   - Line 275: `"${state.tea.defaultTemperatureCelsius}°C"`

3. **EditSessionScreen.kt** (Pattern B - Input)
   - Line 153: Temperature input label
   - Line 158: Temperature suffix
   - Line 206: Water quantity label

#### Medium Priority (Cards and components)
4. **SessionCard.kt** (Pattern A)
   - Line 126: `"$teaTypeName • ${session.brewingTime} • ${session.temperatureCelsius}°C"`

5. **RecentSessionCard.kt** (Pattern A)
   - Line 94: `"$teaTypeName • ${session.brewingTime} • ${session.temperatureCelsius}°C"`

6. **ConfigurationCard.kt** (Pattern A)
   - Line 94: `"${configuration.waterQuantityMl}ml"`
   - Line 95: `"${configuration.temperatureCelsius}°C"`

7. **SaveConfigurationDialog.kt** (Pattern A)
   - Line 69: `"${waterQuantityMl}ml"`
   - Line 70: `"${temperatureCelsius}°C"`

8. **EditConfigurationDialog.kt** (Pattern B - Input)
   - Line 85: Water quantity input label
   - Line 93: Temperature input label
   - Line 96: Temperature suffix

## Implementation Steps

### For Each Screen

1. **Update State file** - Add userPreferences field
2. **Update ViewModel file**:
   ```kotlin
   import dev.jketterer.leaflog.domain.usecases.preferences.GetPreferencesUseCase

   class MyViewModel(
       // ... existing parameters
       private val getPreferencesUseCase: GetPreferencesUseCase,
   ) : ViewModel() {

       init {
           loadPreferences()
       }

       private fun loadPreferences() {
           viewModelScope.launch {
               getPreferencesUseCase().collect { preferences ->
                   _state.update { it.copy(userPreferences = preferences) }
               }
           }
       }
   }
   ```

3. **Update UI file** - Add formatter imports and use them:
   ```kotlin
   import dev.jketterer.leaflog.domain.models.TemperatureFormatter
   import dev.jketterer.leaflog.domain.models.VolumeFormatter
   import dev.jketterer.leaflog.domain.models.UnitConverter // For input screens
   ```

4. **For input screens only** - Add conversion logic in ViewModel when:
   - Loading prefill values (storage → display)
   - Saving user input (display → storage)

## Testing Checklist

After updating all screens:

1. Change unit preferences in Settings
2. Verify each screen shows correct units:
   - Temperature displays in °C or °F
   - Volume displays in mL or fl oz
3. Test input screens:
   - Enter value in preferred unit
   - Save and verify it's stored correctly
   - Load it back and verify it displays in current preference
4. Test unit switching:
   - Create session in Celsius
   - Switch to Fahrenheit
   - Verify displays update
   - Create new session in Fahrenheit
   - Switch back to Celsius
   - Verify both sessions display correctly

## Notes

- **Storage format** is always Celsius and Milliliters (this never changes)
- **Display format** changes based on user preferences
- Input validation ranges may need adjustment for Fahrenheit (0-100°C = 32-212°F)
