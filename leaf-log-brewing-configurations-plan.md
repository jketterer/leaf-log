# Brewing Configurations - Implementation Plan

## Problem Statement

The current model assumes a single "default" brewing method per tea, but reality is more nuanced:
- Same tea can be brewed multiple ways (Gong-fu vs Western vs Grandpa style)
- Brewing parameters depend on vessel size and type
- Users develop personal preferences through experimentation

**Current flawed approach**:
```kotlin
data class Tea(
    val defaultBrewingTime: Duration?,      // ❌ Ignores brewing method
    val defaultTemperatureCelsius: Int?,    // ❌ Ignores vessel context
    val defaultQuantity: Int?,              // ❌ Ignores vessel size
)
```

**Solution**: Smart brewing configurations that learn from successful sessions and are tied to tea + vessel combinations.

---

## Solution Overview

### Core Concept: Learn from Success

Instead of asking users to configure brewing methods upfront, **learn organically**:
1. User brews tea naturally with their preferred parameters
2. System observes which sessions are rated highly (3+ stars)
3. Next time user selects same tea + vessel → auto-fill from successful session
4. Over time, user builds a library of proven brewing methods
5. Optional: Explicitly save and name favorite configurations

### Key Principles

1. **Zero Configuration Required** - Works out of the box, improves with use
2. **Context-Aware** - Tea + Vessel combination (Gong-fu vs Western)
3. **Quality-Filtered** - Only learn from rated sessions (3+ stars)
4. **Progressive Enhancement** - Starts simple, adds features over time
5. **Non-Intrusive** - Suggestions, not requirements

---

## Data Model

### BrewingConfiguration Entity

**Location**: `data/local/database/entities/BrewingConfigurationEntity.kt`

**Schema**:
```kotlin
@Entity(
    tableName = "brewing_configurations",
    foreignKeys = [
        ForeignKey(
            entity = TeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tea_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BrewingVesselEntity::class,
            parentColumns = ["id"],
            childColumns = ["vessel_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tea_id"),
        Index("vessel_id"),
        Index(value = ["tea_id", "vessel_id"]),  // Composite index for lookups
        Index("rating"),
        Index("times_used"),
        Index("last_used_at"),
    ],
)
data class BrewingConfigurationEntity(
    @PrimaryKey val id: String,
    val teaId: String,
    val vesselId: String,

    // Brewing parameters (from a successful session)
    val teaQuantityGrams: Int?,
    val waterQuantityMl: Int,
    val temperatureCelsius: Int,
    val brewingTime: Duration,          // TypeConverter: Duration ↔ Long
    val waterType: WaterType,           // TypeConverter: WaterType ↔ String

    // Metadata
    val sourceSessionId: String,        // Session this was learned from
    val rating: Float,                  // Rating of that session
    val timesUsed: Int,                 // How often user has used this config
    val lastUsedAt: Instant?,           // TypeConverter: Instant ↔ Long

    // User customization
    val label: String?,                 // e.g., "My Gong-fu Method", "Quick Western"
    val isActive: Boolean,              // User can disable configs without deleting

    val createdAt: Instant,             // TypeConverter: Instant ↔ Long
    val updatedAt: Instant,             // TypeConverter: Instant ↔ Long
)
```

**Design decisions**:
- **Composite index** on (tea_id, vessel_id) for efficient lookups
- **timesUsed** tracks popularity for recommendations
- **label** is optional - auto-generated, user can customize
- **isActive** allows soft disable (hide from suggestions)
- **sourceSessionId** preserves lineage (user can see which session it came from)

---

## Phase 1: Smart Pre-fill from History (MVP)

**Goal**: Automatically pre-fill brewing parameters from user's successful sessions.

**Scope**: No explicit configuration management - purely automatic learning.

---

### User Flow 1.1: First Time Brewing (No History)

```
LogTeaScreen
├─ User selects tea: "Dragon Well"
├─ User selects vessel: "Gaiwan"
│
├─ System checks: brewing_configurations WHERE tea_id = dragon_well AND vessel_id = gaiwan
├─ Result: None found
│
├─ Fallback: Check sessions WHERE vessel_id = gaiwan AND tea.tea_type = green AND rating >= 3
│   ├─ Purpose: Find how user brews OTHER green teas in Gaiwan
│   └─ Found: Sencha in Gaiwan (3g, 150ml, 75°C, 2m)
│
├─ Pre-fill vessel-typical parameters:
│   ├─ Water quantity: 150ml (from Sencha session)
│   ├─ Brewing time: 2m (from Sencha session)
│   ├─ Temperature: 80°C (from tea.defaultTemperatureCelsius or tea type default)
│   └─ Tea quantity: empty (user-specific)
│
├─ Show banner: "💡 Suggested parameters based on how you brew green tea"
│
└─ User adjusts as needed and brews
```

**Intelligent fallback logic**:
1. Try: This tea + this vessel
2. Try: Same tea type + this vessel (what you asked for in Q3)
3. Try: Tea defaults (if set)
4. Default: Empty (user enters from scratch)

---

### User Flow 1.2: Repeat Brewing (History Exists)

```
LogTeaScreen
├─ User selects tea: "Dragon Well"
├─ User selects vessel: "Gaiwan"
│
├─ System queries:
│   SELECT * FROM tea_sessions
│   WHERE tea_id = dragon_well
│     AND vessel_id = gaiwan
│     AND rating >= 3
│     AND status = COMPLETED
│   ORDER BY rating DESC, timestamp DESC
│   LIMIT 1
│
├─ Found: Session from Jan 15 (5⭐, 5g, 100ml, 85°C, 30s)
│
├─ Pre-fill all parameters:
│   ├─ Tea quantity: 5g
│   ├─ Water quantity: 100ml
│   ├─ Temperature: 85°C
│   ├─ Brewing time: 30s
│   ├─ Water type: Filtered
│   └─ Location: (empty or last used)
│
├─ Show banner: "💡 Using your 5⭐ method from Jan 15"
│   └─ Tap banner: Shows session detail that config came from
│
└─ User can adjust or brew as-is
```

**Benefits**:
- Instant value on second use
- No configuration required
- Learns from actual brewing, not hypotheticals

---

### User Flow 1.3: Adjusted Parameters (Update Flow)

```
Timer Completion Screen
├─ User just brewed Dragon Well in Gaiwan
├─ Used pre-filled config but adjusted temperature 85°C → 87°C
├─ Rates session: 5⭐
│
├─ System detects:
│   ├─ Config was used (pre-filled from session XYZ)
│   ├─ User made adjustment (temperature changed)
│   ├─ High rating (5 stars)
│   └─ Should update configuration
│
├─ Dialog: "Update Configuration?"
│   ├─ "You adjusted the temperature to 87°C and rated it 5⭐"
│   ├─ "Update your saved method with these new parameters?"
│   ├─ [Update Configuration] - Updates existing session as source
│   └─ [Keep Original] - Doesn't change anything
│
└─ If [Update]: Next time will pre-fill with 87°C
```

**Q4 Clarification**: Suggest updating when parameters change AND rating >= 4 stars.

---

### Technical Implementation (Phase 1)

**Since we're NOT using separate table yet**, implement with repository queries:

```kotlin
// In LogTeaViewModel
suspend fun prefillFromBestSession(tea: Tea, vessel: BrewingVessel) {
    // 1. Try: This tea + this vessel, rated >= 3 stars
    var bestSession = teaSessionRepository.getByTeaId(tea.id)
        .filter { it.vesselId == vessel.id }
        .filter { it.rating != null && it.rating >= 3f }
        .maxByOrNull { it.rating!! * 100 + it.timestamp.epochSeconds }

    // 2. Fallback: Same tea TYPE + this vessel
    if (bestSession == null) {
        val teaType = teaRepository.getById(tea.id)?.teaTypeId
        val sameTeas = teaRepository.getAll().filter { it.teaTypeId == teaType }
        val sameTeasIds = sameTeas.map { it.id }

        bestSession = teaSessionRepository.getAll()
            .filter { it.teaId in sameTeasIds }
            .filter { it.vesselId == vessel.id }
            .filter { it.rating != null && it.rating >= 3f }
            .maxByOrNull { it.rating!! * 100 + it.timestamp.epochSeconds }
    }

    // 3. Apply pre-fill
    if (bestSession != null) {
        // Pre-fill from session
        // Show appropriate banner based on source
    } else {
        // Use tea defaults or empty
    }
}
```

**Performance**: This is acceptable for Phase 1 since:
- Queries are on indexed fields
- Filter operations are in-memory (small datasets)
- Only runs once when tea+vessel selected
- No separate table needed yet

---

## Phase 2: Explicit Save & Management

**Goal**: Allow users to explicitly save, name, and manage brewing configurations.

**When to implement**: After Phase 1 has been used for a while and users have natural configurations emerging.

---

### User Flow 2.1: Explicit Save After Great Session

```
Timer Completion / Session Save
├─ User completes session
├─ Rates: 5⭐
│
├─ Dialog: "💡 Save This as a Brewing Method?"
│   ├─ "Dragon Well in Gaiwan"
│   ├─ "5g • 100ml • 85°C • 30s"
│   ├─ Input field: [Auto-generated name] ← Can edit
│   │   └─ Default: "Gong-fu Style" (detected from parameters)
│   │   └─ User can rename: "My Perfect Gong-fu"
│   ├─ [Save Method] [Skip]
│   └─ Note: "This will be suggested next time you brew this combination"
│
└─ If saved: Creates brewing_configurations record
```

**Auto-naming logic** (Q5 - auto-name but allow editing):
```
If brewingTime <= 45s AND teaQuantity/waterQuantity ratio > 0.03:
    → "Gong-fu Style"
Else if brewingTime >= 3m AND waterQuantity >= 250ml:
    → "Western Style"
Else if teaQuantity == null (tea bag):
    → "Tea Bag Method"
Else:
    → "Custom Method"
```

User can tap name field and edit before saving.

---

### User Flow 2.2: Multiple Configurations (Selection)

```
LogTeaScreen
├─ User selects: Dragon Well + Gaiwan
│
├─ System finds 3 saved configurations:
│   1. "My Gong-fu" (5⭐, 12 uses, last: 2 days ago)
│   2. "Quick Morning" (4⭐, 5 uses, last: 1 week ago)
│   3. "Experimental" (3⭐, 1 use, last: 2 months ago)
│
├─ Auto-select #1 (highest rated, most used) - Q6 answer
├─ Pre-fill with "My Gong-fu" parameters
│
├─ Show banner: "💡 My Gong-fu (5⭐, used 12 times)"
│   └─ With button: [Choose Different Method]
│
├─ If user taps [Choose Different Method]:
│   ├─ Dialog: "Choose Brewing Method"
│   │   ├─ ○ My Gong-fu (5⭐, 12 uses) [Recommended]
│   │   ├─ ○ Quick Morning (4⭐, 5 uses)
│   │   ├─ ○ Experimental (3⭐, 1 use)
│   │   └─ ○ Custom (enter manually)
│   │
│   └─ User selects → Pre-fills with that config
│
└─ User brews and completes
```

**Recommendation algorithm** (Q1 - highest rating first, then most recent):
```
Sort by:
1. Rating (DESC) - 5 stars before 4 stars
2. Times used (DESC) - More popular first
3. Last used (DESC) - Recent first
Take first
```

---

### User Flow 2.3: Managing Configurations

**Location**: Tea Detail Screen (Q7 answer)

```
Tea Detail Screen
├─ [All existing sections...]
│
├─ Section: "SAVED BREWING METHODS"
│   ├─ Card: "My Gong-fu" (Gaiwan)
│   │   ├─ 5g • 100ml • 85°C • 30s
│   │   ├─ 5⭐ • Used 12 times • Last: 2 days ago
│   │   └─ Actions: [✏️ Edit] [🗑️ Delete]
│   │
│   ├─ Card: "Western Style" (Teapot)
│   │   ├─ 3g • 300ml • 80°C • 3m
│   │   ├─ 4⭐ • Used 5 times • Last: 1 week ago
│   │   └─ Actions: [✏️ Edit] [🗑️ Delete]
│   │
│   └─ [+ Add Method] ← Opens LogTeaScreen with this tea pre-selected
│
└─ Tap [✏️ Edit]:
    └─ Edit Configuration Dialog
        ├─ Name: [My Gong-fu]
        ├─ Tea quantity: [5] g
        ├─ Water quantity: [100] ml
        ├─ Temperature: [85] °C
        ├─ Brewing time: [0:30]
        ├─ Water type: [Filtered ▼]
        ├─ [Save Changes] [Cancel]
        └─ Note: Changes will apply to future sessions
```

**Edit behavior**:
- Can change parameters
- Can rename
- Can disable (toggle "Active" switch)
- Doesn't affect past sessions (only future pre-fills)

---

### User Flow 2.4: Configuration Update Prompt

**Scenario**: User uses saved config but makes adjustments

```
Timer Completion
├─ Session completed with 5⭐ rating
├─ System detects:
│   ├─ Pre-filled from config "My Gong-fu"
│   ├─ User changed temperature: 85°C → 87°C
│   └─ High rating suggests improvement
│
├─ Dialog: "Update 'My Gong-fu' Configuration?"
│   ├─ "You increased temperature to 87°C and rated it 5⭐"
│   │
│   ├─ Changes:
│   │   └─ Temperature: 85°C → 87°C
│   │
│   ├─ Options:
│   │   ├─ [Update Configuration] ← Updates existing config (Q4 answer)
│   │   ├─ [Save as New Method] ← Creates new config
│   │   └─ [No Thanks] ← One-time adjustment, don't save
│   │
│   └─ Note: "Updating will change future pre-fills for this method"
│
└─ If [Update]: brewing_configurations record updated
└─ If [Save as New]: New record created (prompts for name)
└─ If [No Thanks]: Session saved, config unchanged
```

**When to show update prompt** (Q4):
- Config was used (session has `used_configuration_id` metadata)
- Parameters were changed from config
- Rating >= 4 stars
- Changes are meaningful (not just 1°C or 5 seconds)

---

## Phase 3: Advanced Features (Future)

**Note**: Based on Q9 and Q10 answers, we're removing community sharing and templates from the plan.

### Removed Features:
- ❌ Community sharing (Q9 - keep private)
- ❌ Generic templates (Q10 - configurations are tea-specific)

### Remaining Advanced Features:

#### 3.1: Configuration Statistics

Show analytics for each configuration:
- Success rate (% of sessions rated 4+ stars when using this config)
- Average rating when using this config
- Temperature/time adjustments users make
- "This config produces consistently great results!"

#### 3.2: Configuration Comparison

Side-by-side comparison of methods:
```
Compare: My Gong-fu vs Quick Morning

Parameter       | My Gong-fu | Quick Morning
----------------|------------|---------------
Tea Quantity    | 5g         | 3g
Water           | 100ml      | 150ml
Temperature     | 85°C       | 80°C
Time            | 30s        | 2m
Avg Rating      | 4.8⭐      | 4.2⭐
Times Used      | 12         | 5
```

#### 3.3: Smart Recommendations

System suggests configurations:
- "💡 Try a longer steep time? Users of similar green teas average 45s in Gaiwan"
- "💡 Your Gong-fu method gets higher ratings than Quick Morning. Use it more often?"
- Based on aggregate patterns (privacy-preserving)

#### 3.4: Seasonal Adjustments

Track if parameters drift by season:
- "You tend to brew hotter in winter (+3°C average)"
- Suggest seasonal adjustments

---

## Database Schema Design

### brewing_configurations Table

**Relationships**:
```
Tea (1) ──< (M) BrewingConfiguration (M) >── (1) BrewingVessel
                         │
                         │ (optional reference)
                         ▼
                   TeaSession (source)
```

**Indices** (critical for performance):
```sql
CREATE INDEX idx_config_lookup ON brewing_configurations(tea_id, vessel_id);
CREATE INDEX idx_config_ranking ON brewing_configurations(rating DESC, times_used DESC);
CREATE INDEX idx_config_source ON brewing_configurations(source_session_id);
```

**Cascade behavior**:
- Tea deleted → Configurations deleted
- Vessel deleted → Configurations deleted
- Source session deleted → Configuration remains (session is just lineage, config is independent)

---

## Pre-fill Algorithm (Detailed)

### Priority Order (Implemented in Use Case)

**When**: User selects tea + vessel

**Steps**:
1. **Check saved configurations** (Phase 2 only):
   ```
   SELECT * FROM brewing_configurations
   WHERE tea_id = ? AND vessel_id = ? AND is_active = 1
   ORDER BY rating DESC, times_used DESC, last_used_at DESC
   LIMIT 1
   ```
   If found → Use this (highest priority)

2. **Check tea + vessel sessions** (Phase 1):
   ```
   SELECT * FROM tea_sessions
   WHERE tea_id = ? AND vessel_id = ? AND rating >= 3 AND status = COMPLETED
   ORDER BY rating DESC, timestamp DESC
   LIMIT 1
   ```
   If found → Pre-fill from this session

3. **Check tea type + vessel sessions** (Fallback - Q3 answer):
   ```
   SELECT ts.* FROM tea_sessions ts
   JOIN teas t ON ts.tea_id = t.id
   WHERE t.tea_type_id = ? AND ts.vessel_id = ? AND ts.rating >= 3
   ORDER BY ts.rating DESC, ts.timestamp DESC
   LIMIT 1
   ```
   If found → Pre-fill water + time, suggest temperature from tea defaults

4. **Check tea defaults** (Last resort):
   ```
   Use tea.defaultTemperatureCelsius, tea.defaultBrewingTime
   ```

5. **Empty** (First time ever):
   ```
   All fields empty except water type (default: FILTERED)
   ```

**Banner text varies by source**:
- Config: "💡 My Gong-fu (5⭐, used 12 times)"
- Direct session: "💡 Using your 5⭐ method from Jan 15"
- Type fallback: "💡 Suggested parameters based on how you brew green tea"
- Tea defaults: "💡 Suggested parameters for Dragon Well"
- Empty: No banner

---

## UI Components Needed

### Phase 1:
1. **Pre-fill banner** (LogTeaScreen)
   - Shows source of pre-filled parameters
   - Tap to see source session (Phase 1.5 enhancement)

2. **Update suggestion logic** (not UI)
   - Detects parameter changes
   - No prompt yet (just saves session normally)

### Phase 2:
3. **Save Configuration Dialog** (after 5⭐ sessions)
   - Shows parameters
   - Editable name field
   - Save/Skip buttons

4. **Choose Method Dialog** (when multiple configs exist)
   - Radio list of configurations
   - Shows stats (rating, usage, last used)
   - "Custom" option

5. **Update Configuration Dialog** (when params changed)
   - Shows changes
   - Update/Save as New/Skip buttons

6. **Saved Methods Section** (Tea Detail Screen)
   - List of configurations for this tea
   - Edit/Delete actions per config

7. **Edit Configuration Dialog**
   - Edit all parameters
   - Rename
   - Active toggle
   - Save/Cancel

---

## Metrics to Track

### Usage Analytics (for refinement):
- % of sessions that use pre-filled configs (success metric)
- % of config save prompts accepted (engagement)
- Average parameters per tea (understand diversity)
- Most common vessel+tea combinations (optimize for these)

### Performance Metrics:
- Query time for configuration lookup
- Database size impact
- Memory usage of in-memory filtering

---

## Migration Path

### Current State → Phase 1:
1. Keep `Tea.defaultTemperatureCelsius` and `defaultBrewingTime`
2. Add pre-fill logic to LogTeaViewModel
3. No schema changes (queries existing tea_sessions)
4. Banner UI component
5. Test with existing data

### Phase 1 → Phase 2:
1. Create `brewing_configurations` table
2. **One-time migration**: Create configs from all 4+ star sessions
   ```sql
   INSERT INTO brewing_configurations
   SELECT generate_id(), tea_id, vessel_id, ...
   FROM tea_sessions
   WHERE rating >= 4 AND status = COMPLETED
   GROUP BY tea_id, vessel_id, <parameters>
   ```
3. Add save dialog UI
4. Add management UI in Tea Detail
5. Update pre-fill logic to check configs table first

---

## Open Questions for You

Before I finalize the plan:

**Q12**: Should configurations be synced via Firebase (Phase 8) or kept local only?

**Q13**: Should we track which configuration was used for each session? (Adds `used_configuration_id` to TeaSession)
- Enables: "This method produces 4.8⭐ average rating"
- Enables: Update prompts (Q4)
- Cost: Extra field + tracking logic

**Q14**: Maximum configurations per tea+vessel?
- Unlimited
- Cap at 5 (prevents clutter)
- Cap at 3 (forces quality over quantity)

**Q15**: Should we show configuration stats in Tea Detail?
- "Your Gong-fu method averages 4.8⭐ across 12 sessions"
- "Quick Morning is 15% faster but rates 0.6 stars lower"

Let me know your thoughts on these, and I'll finalize the complete implementation plan!