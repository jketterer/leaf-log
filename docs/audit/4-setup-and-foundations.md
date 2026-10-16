## Setup screens

Where the roadmap already lists an item, I only repeat it when I have new evidence or a sharper framing, and I label it **[ROADMAP+]**. Anything I couldn't confirm by reading the code is marked *unverified*.

### Vessel list (the Vessels tab inside Collection)
- **Purpose / verdict:** The list isn't its own screen. `VesselListViewModel` is injected into `TeaCollectionScreen` as a second ViewModel and drawn in `VesselsTabContent`. The list works but is minimal: name, capacity and an "Archived" label. About half of the VM is dead ceremony that the screen never calls.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| polish | layout | presentation/ui/screens/collection/TeaCollectionScreen.kt:409-424 | The vessel list has no bottom clearance for the FAB, so the last card sits under the "+" button. | The Teas tab adds `Spacer(60.dp)` (:349). The Vessels `LazyColumn` only has `PaddingValues(16.dp)` (:411). |
| arch | dead code | presentation/ui/screens/vessel/VesselListViewModel.kt:44-45,106-124; VesselListIntent.kt | `VesselClicked`, `AddVesselClicked`, `BackClicked`, `ClearError` and `LoadVessels`, the `_navigationEvent` StateFlow and `VesselListNavigationEvent` are never used. | The screen calls the nav lambdas directly (TeaCollectionScreen.kt:421, :404). Only `ToggleShowArchived` is dispatched. |
| bug | error handling | VesselListState.kt:21 / TeaCollectionScreen.kt:390-426 | `vesselState.error` is set on a load failure but never rendered, so a failure shows as an empty state. | The Vessels tab never reads `vesselState.error`. |
| consistency | code | TeaCollectionScreen.kt:89,91,358,359,374,383,436 | `dev.jketterer...vessel.VesselListState/Intent` is written out fully qualified 7 times instead of imported. | Readability. |
| friction | empty state | TeaCollectionScreen.kt:402 | "No vessels yet" under "All" can't actually happen: defaults are seeded and the last active vessel can't be deleted. "No active vessels" also offers "Add Your First Vessel", which is wrong when archived vessels exist. | InitializeDefaults plus the DeleteBrewingVesselUseCase guard. |
| polish | ripple | presentation/ui/components/vessel/VesselCard.kt:48-52 | `Modifier.clickable` sits on the `Card` rather than using `Card(onClick=…)`, so the ripple isn't clipped to the rounded shape and the card gets no button role. The `alpha(0.6f)` for archived cards also dims the ripple. | Modifier order. |
| a11y | semantics | VesselCard.kt:117-121 | The chevron has `contentDescription = "View details"`, so TalkBack reads that on every card. It's decorative and the card itself has no role. | |
| arch | stringly typed | presentation/ui/components/vessel/VesselIconHelper.kt:14-35 | Icon ids are raw strings (`"ic_french_press"` breaks the naming pattern). Any unknown value quietly falls back to Mug. | `else -> Mug` |

- **Opportunities:**
  - `BrewingVessel.displayOrder` exists and is written, but there's no UI to reorder it. Drag-to-reorder would also set the order in `VesselSelector`.
  - Show "last used" and "N sessions" on each card. The data is already queried in VesselDetail.

### Vessel Detail
- **Purpose / verdict:** A thin read-only page: a big icon, the name repeated from the title, capacity, a session count, and Archive/Delete in an overflow menu. The delete rules in the UI disagree with the use case, and for nearly every vessel you actually use, the page shows a red "Cannot Delete" error card.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | correctness | VesselDetailState.kt:162-163 vs domain/usecases/vessel/DeleteBrewingVesselUseCase.kt:16-17 | The UI's delete rule counts all vessels (`totalVesselCount > 1`, archived included). The use case counts active ones (`countActive() <= 1`). (a) An unused **archived** vessel can't be deleted while you have one active vessel, even though deleting it leaves the active count unchanged. (b) The only active vessel shows as deletable, then fails after you confirm. | `getAll()` (VesselDetailViewModel.kt:63) vs `countActive()`. |
| bug | correctness | VesselDetailViewModel.kt:67 | "Sessions" counts child steeps and in-progress rows, so a 6-steep gongfu session reads as 6. | `TeaSessionDao.getByVesselId` has no `parentSessionId IS NULL` filter. This is the same trap as the roadmap's leaf double-count. |
| friction | polish | VesselDetailScreen.kt:343-379 | A red `errorContainer` "Cannot Delete" card is always shown for any vessel with sessions, i.e. nearly every vessel in daily use. It's alarm styling for a normal state. | `if (!state.canDelete)` |
| polish | loading | VesselDetailViewModel.kt:57 / VesselDetailScreen.kt:72,195 | Coming back from Edit, or unarchiving, re-runs `loadVessel`, which sets `isLoading=true` and replaces the whole page with a spinner for a moment. | One-shot `getById` instead of the existing `getByIdFlow`. |
| ~~arch~~ | ~~dead code~~ | ~~VesselDetailScreen.kt:406-418~~ | ~~The "can't delete" branch of `DeleteConfirmationDialog` can never run, because the Delete menu item is `enabled = state.canDelete` (:188).~~ | |
| consistency | error | VesselDetailScreen.kt:93-97 | The error snackbar never clears `state.error` (there's no ClearError intent), so the same failure twice shows only once. | EditVessel does this correctly with `ErrorShown`. |
| consistency | nav | VesselDetailViewModel.kt:197 | Nav events use a `MutableStateFlow<Event?>` plus a manual "handled" call. The 13 other VMs use `Channel` + `receiveAsFlow`. | See Navigation. |
| polish | code | VesselDetailScreen.kt:256-315 | `state.vessel!!` appears 7 times. | |
| polish | long text | VesselDetailScreen.kt:138 | The TopAppBar title has no `maxLines`/ellipsis. A 50-character name is allowed. | |
| friction | reachability | AnalyticsViewModel.kt:114 | Tapping a vessel slice in Analytics is a no-op, with the comment "no vessel detail screen yet". That screen exists. | Wiring it is a 1-line fix. |

- **Opportunities (all from data already stored):**
  - Parent sessions in this vessel, teas brewed in it, average rating per tea in this vessel ("what this vessel is best for"), and last used.
  - Saved methods that reference it (`BrewingConfiguration.vesselId`).
  - Typical fill vs capacity: the median `waterQuantityMl` of sessions against `capacityMl`.
  - A "View in History" link. `HistoryRoute` needs a `filterVesselId`.
  - Tap the photo for fullscreen (`FullscreenImageViewer` already exists).

### Edit Vessel
- **Purpose / verdict:** This is the most carefully built screen in my slice (recent commit): pure form logic, a system-back guard, photo cleanup on discard, and deliberate save-button behaviour. The remaining problems are in ordering and idempotency.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | state | EditVesselScreen.kt:63-65 / EditVesselViewModel.kt:72 | `LaunchedEffect(vesselId) → LoadVessel` isn't idempotent. Any Activity recreation (dark-mode switch, font-scale or locale change, window resize) builds a new composition while the ViewModel survives, so the load re-runs and overwrites the user's edits with DB values. A freshly picked photo is lost and its file orphaned. | The manifest has no `configChanges`. **The same pattern exists in EditTea, EditSession and EditTeaType.** |
| bug | data | EditVesselViewModel.kt:168-176 | The old photo file is deleted **before** the update is saved. If the save fails, the DB still points at a file that no longer exists. | Cleanup belongs in `onSuccess`. |
| bug | double submit | EditVesselViewModel.kt:154 / EditVesselScreen.kt:157-177 | `saveVessel()` has no `isSaving` guard and the FAB is always enabled on purpose. A double tap in add mode creates two vessels, because each call makes a random UUID. | |
| bug | storage | EditVesselViewModel.kt:150-152 | `RemovePhoto` clears `imagePath` without deleting a photo that was picked but not yet saved, so the file is orphaned. `onPhotoSelected` handles this case (:136-142); remove doesn't. | Adds to the roadmap's photo-storage growth. |
| friction | process death | EditVesselState / VM | Form state isn't saved, so it's lost if the process dies. The Android photo picker is exactly when that tends to happen. | No SavedStateHandle anywhere in the app. |
| consistency | side effect | EditVesselViewModel.kt:122-126 via VolumeInputField suffix | Tapping the unit suffix in one form field changes the app-wide volume unit. The field converts correctly, but nothing tells the user it's a global setting. | Same for temperature in EditTeaType. |
| polish | flicker | EditVesselScreen.kt:132 | The title reads "Add Vessel" and then flips to "Edit Vessel" once loading finishes (isEditMode defaults to false). autoFocus already uses `vesselId`; the title could too. | |
| polish | copy | EditVesselViewModel.kt:145 | Shows `"Failed to save image: ${e.message}"`, i.e. the raw exception text. | |

- **Opportunities:**
  - Make loading idempotent by guarding on the loaded id or loading from VM init with the route key. That one change fixes 4 edit screens.
  - Move photo cleanup into a single `commitPhoto(old, new)` that only runs after a successful save.

### Settings (preferences, notifications, export/import UI)
- **Purpose / verdict:** A clear, honest settings page with thoughtful notification copy. The Data section is where it's weakest: failures are silently thrown away, there's no success confirmation, and import runs without showing what it's about to overwrite.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug~~ | ~~error handling~~ | ~~SettingsScreen.kt:145-148~~ | ~~A save-dialog write failure is thrown away. `onError` calls `ExportCompleted` then `ClearError`. The comment says "Clear any prior, then set new", but nothing new is ever set.~~ | ~~`error` is unused.~~ |
| ~~bug~~ | ~~error handling~~ | ~~SettingsScreen.kt:155~~ | ~~An import picker or copy failure is mapped to `ImportCancelled`, so it's silent.~~ |   |
| ~~bug~~ | ~~main thread~~ | ~~androidMain/.../FileOperations.android.kt:33,72~~ | ~~Export and import copy the whole ZIP with `copyTo` inside the ActivityResult callback, on the main thread. With lots of photos that's an ANR risk.~~ |   |
| ~~bug~~ | ~~recreation~~ | ~~FileOperations.android.kt:24~~ | ~~`pendingFilePath` is plain `remember`. If the Activity is recreated while the system Save dialog is open, the result arrives with it null and falls to `onExported()`: nothing is written, the destination file is created empty, and no error is shown.~~ | ~~*Runtime unverified.*~~ |
| ~~friction~~ | ~~feedback~~ | ~~SettingsViewModel.kt (ExportCompleted)~~ | ~~A successful export gives no confirmation at all on Android: no snackbar, size or counts.~~ |   |
| ~~friction~~ | ~~trust~~ | ~~SettingsScreen.kt:328-338~~ | ~~"Successfully imported: 45 tea sessions" is the number of rows in the file, not new or changed rows. Importing the same backup twice reports the same numbers.~~ | ~~`ImportResult` = `exportData.*.size`~~ |
| ~~friction~~ | ~~safety~~ | ~~SettingsViewModel import~~ | ~~There's no confirmation or preview before import. As the Export/import section below shows, import **reverts edits and un-deletes** rows.~~ |   |
| ~~polish~~ | ~~bug~~ | ~~SettingsScreen.kt:132-143~~ | ~~The export filename is `remember`ed from when the screen **opened**, so two exports in one visit get the same name.~~ |   |
| a11y | semantics | SettingsScreen.kt:205-229 | Each switch sits beside its label rather than inside a `toggleable` row, so TalkBack announces "Switch, on" with no name. The tap target is also only the switch. | |
| friction | missing | SettingsScreen.kt | There's no About section (version, licences, privacy policy, support), and the app version isn't shown anywhere. This is also needed for Phase 4. | grep for version/about/privacy finds nothing. |
| friction | missing | SettingsScreen / TimerViewModel.kt:156 | The "Brew complete" switch shows ON even when OS notification permission is denied. Only the Timer screen checks `hasNotificationPermission()`. | |
| polish | flicker | SettingsState.kt:28 | `isLoading` is never read, so default units flash before DataStore emits. | |
| **[ROADMAP+]** consistency | units | domain `WeightFormatter` used only in AnalyticsViewModel | The roadmap calls the missing weight-unit row "discoverability only". It's worse than that: the weight preference only affects one Analytics card. SessionCard.kt:159, SessionDetailScreen.kt:268, SteepCompleteScreen.kt:409, SteepParameterCard.kt:47, QuickTimerScreen.kt:218 and LogTeaSections.kt:217 all hardcode `"g"`. A Settings row would promise something the app doesn't honour. | |

- **Opportunities:**
  - Record `lastExportedAt` and show "Last backup: 42 days ago" with a nudge. Export is the only backup under the "sync deferred" decision.
  - An import preview dialog: backup date, counts, "N existing records will be overwritten".
  - Include preferences in the export (`PreferencesDataStore` keys are not exported).
  - Show notification permission status with a deep link to OS settings.
  - Use SegmentedButtons for binary unit choices.

### Tea Type List
- **Purpose / verdict:** A plain list of colour dots, names and default temperatures. It's fine, but it tells you nothing about how each type is used, and it's buried in Settings even though tea types are collection data.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| arch | ceremony | TeaTypeListViewModel.kt:22-23,67-82 | Clicks go screen → VM → StateFlow nav event → screen, only to call a lambda. The pattern is copied from VesselList. | |
| consistency | colour | TeaTypeListScreen.kt:184 | "Is this colour light?" is hardcoded to `"#FFFDE7"`. EditTeaTypeScreen.kt:258 hardcodes `#FFFDE7`/`#FFEB3B`, and BrewingActivityHeatmap.kt:51-58 hand-rolls luminance. That's three approaches where Compose's built-in `Color.luminance()` would do. | |
| polish | empty | TeaTypeListScreen.kt:117-121 | The empty state is bare `Text("No tea types found")`, not `EmptyState`, and has no action. | |
| polish | dark mode | TeaTypeListScreen.kt:176-189 | The brown and tan dots (#795548, #8D6E63) have low contrast on the dark surface #1B1C18. Only cream gets an outline. | |
| a11y | semantics | TeaTypeListScreen.kt:161-165 | The card uses `.clickable` with no role and the colour isn't described. | |

- **Opportunities:**
  - Show the tea count per type (already fetched in EditTeaType) and session count.
  - Reorder by `displayOrder` (the field exists; there's no UI).
  - "Merge type into…" as an alternative to the delete block.
  - Default steep time and ratio per type, now that prefill reads the type's temperature.

### Edit Tea Type
- **Purpose / verdict:** It works, but it's the older pattern that EditVessel was rewritten to move away from. The save button is dead with no explanation, system back skips the discard guard, and toggling the unit wipes what you typed.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug~~ | ~~data loss~~ | ~~EditTeaTypeScreen.kt (no BackHandler)~~ | ~~System back or swipe-back throws away edits silently. The discard dialog only triggers from the toolbar arrow. EditVessel added `NavigationBackHandler` for exactly this.~~ | ~~**EditSessionScreen has the same gap.**~~ |
| friction | dead button | EditTeaTypeState.kt:13,42 / EditTeaTypeScreen.kt:151 | In add mode no colour is preselected (`colorHex=""`), and the Save icon stays disabled until you pick one, with nothing explaining why. EditVessel's rule is "a dead button explains nothing". | |
| friction | data loss | EditTeaTypeViewModel.kt:117-121 | The unit toggle sets `temperature = ""`, wiping the input. EditVessel converts the value instead (`convertCapacity`). | |
| bug | validation | EditTeaTypeViewModel.kt:130-138 | Temperature is checked against `>212` whatever the unit. In °C it accepts 150–212; in °F it accepts 1–31. LogTeaViewModel.kt:444-451 already validates per unit correctly. | |
| bug | double submit | EditTeaTypeViewModel.kt:142 | `save()` doesn't check `isSaving`, so it can create duplicate types. The icon is disabled only after a recomposition. | |
| bug | edge | EditTeaTypeViewModel.kt:228 / TeaTypeRepositoryImpl.initializeDefaults | Nothing prevents deleting the **last** type (vessels have that guard). Delete every type and on next launch `count()==0` re-seeds the defaults with fixed ids via `upsertAll`, un-deleting the soft-deleted rows. | |
| polish | precision | EditTeaTypeViewModel.kt:~156-162 | Saving always rewrites the temperature from the rounded display value, so a °F user who only renames the type turns 72.0°C into 72.2°C. EditVessel compares in storage units to avoid this. | |
| polish | data | EditTeaTypeViewModel.kt:169 | The name isn't trimmed and there's no duplicate-name check. | |
| consistency | error | EditTeaTypeScreen.kt:111-115 | The error is never cleared, so a repeated error won't show again. | |
| a11y | semantics | EditTeaTypeScreen.kt:251-274 | Colour swatches are clickable boxes with no label, role or selected state, and 44dp is below the 48dp minimum. | |
| consistency | copy | EditTeaTypeScreen.kt:298-305,323 | ~~"Discard" isn't styled as an error here (it is in EditVessel), and~~ "Delete Tea Type" has no "?" while every other delete dialog does. | |

- **Opportunities:**
  - Port EditVessel's structure: a Sections file, pure form logic plus a test, the back handler, and an always-enabled save with explanation.
  - A custom colour picker, or at least a larger palette.

### "More" route and how setup screens are reached
- **Purpose / verdict:** `NavRoute.MoreRoute` is declared and registered with a serializer, but it has **no `entry<>`** and nothing navigates to it. It's leftover from an earlier design. Settings (and Tea Types through it) can only be reached from the Home top-bar gear icon. Vessels live in the Collection tab. Tea Types, which are collection data, sit in Settings under "Management", a section with one item.
- **Findings table:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| arch | dead code | presentation/ui/navigation/NavRoute.kt:36, NavConfig.kt:14 | The vestigial route would crash if it were ever pushed. | No `entry<NavRoute.MoreRoute>` in AppNavigation. |
| friction | IA | SettingsScreen.kt:234-270 | Tea Types are only reachable via Home → gear → Management. Vessels are only reachable via Collection → Vessels tab. Two similar "setup" entities are found in two different ways. | |

- **Opportunities:** Either delete MoreRoute, or make Collection the home for both vessels and tea types (a third tab or a "Types" chip-row action) and keep Settings for preferences only.

## Cross-cutting foundations

### Navigation — Nav3 is wired correctly and the back stack is restored, but tab behaviour is ad hoc: back from a tab exits the app, tab state isn't kept, and there are crash-prone pop calls.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug (unverified on device)~~ | ~~crash~~ | ~~AppNavigation.kt (18× `backStack.removeLast()`, e.g. :206,:219,:361)~~ | ~~With compileSdk 37, Kotlin `MutableList.removeLast()` binds to the JDK 21 `java.util.List.removeLast()`, which doesn't exist before API 35. That's a likely `NoSuchMethodError` on Android 7–14 (minSdk 24). This is a well-known compileSdk-35 trap.~~ | ~~Use `removeAt(lastIndex)` or `removeLastOrNull()`.~~ |
| bug | deep link | androidApp/.../MainActivity.kt:28 | `handleDeepLink(intent)` runs on **every** `onCreate`, including recreations. After a cold start from a notification, every config change or process-death restore re-pushes TimerRoute/SteepCompleteRoute onto the restored stack, e.g. after you've already finished and gone Home. | Needs a `savedInstanceState == null` guard and clearing the extras. |
| friction | back behaviour | AppNavigation.kt:89-92 | Tapping a tab does `clear()` + `add(tab)`, so system back from Collection/History/Analytics **exits the app** instead of returning to Home. | |
| consistency | back behaviour | AppNavigation.kt:137,145 vs :89 | The same tab screen behaves differently depending on how you got there. Home's shortcuts *push* Collection/Analytics on top of Home (back returns Home); the tab bar *replaces* the stack (back exits). | |
| friction | state loss | AppNavigation.kt:89-92 + `rememberViewModelStoreNavEntryDecorator` | Switching tabs destroys the other tab's ViewModel, scroll position, search and filters (e.g. History search, Collection Vessels tab). | |
| friction | iOS | HistoryScreen.kt, AnalyticsScreen.kt, TeaCollectionScreen.kt | Tab screens reached as drill-downs (TeaDetail → History, Analytics → History, Home → Collection) show no Up arrow. On iOS the only way back is the edge swipe (*support in CMP 1.12 + Nav3 unverified*) or a tab tap that clears the stack. | |
| bug (unverified) | double pop | all `onNavigateBack = { backStack.removeLast() }` | Pops aren't guarded by "am I still on top". A fast double-tap on Up during the exit animation can pop twice, or empty the stack, which Nav3 rejects. | |
| polish | transitions | AppNavigation.kt:108-119 | Every change, tab switches included, uses a horizontal slide. Tab switches slide "forward" even when going back to Home. Material recommends fade-through for top-level changes. | |
| a11y | nav bar | AppNavigation.kt:420,432,444,456 | Nav icons use `contentDescription = ""` where it should be `null`. Harmless because there's a label, but it's the wrong idiom. | |
| consistency | events | VesselList/VesselDetail/TeaTypeList/EditTeaType VMs | 4 VMs (all in this slice) use `MutableStateFlow<NavEvent?>` plus `onNavigationEventHandled()`. 13 use `Channel.receiveAsFlow`. | |
| verified | restoration | AppNavigation.kt:58-61, NavConfig.kt | The back stack survives process death (serializable NavKeys, all 19 routes registered). ViewModel form state does not: there's no SavedStateHandle anywhere. | |

**Opportunities:**
- Keep `[Home, tab]` so back returns Home, or keep a back stack per tab.
- Add Up icons on pushed tab screens.
- Use fade-through for tab changes.
- Use one `navigateUp()` helper that pops only when `size > 1`.

### Theme and design identity — a Theme Builder palette on stock Material. No dynamic colour (a defensible brand choice), no typography or shape identity.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| polish | identity | theme/Typography.kt:4 | `val Typography = Typography()`: a stock type scale with no brand font. There's no `Shapes` either; 9 ad-hoc `RoundedCornerShape(4/6/8/12.dp)`. | |
| polish | palette gaps | theme/Theme.kt | `surfaceBright`, `surfaceDim` and the `*Fixed` roles aren't set, so they fall back to the M3 baseline (purple-tinted) values. *Which components render them is unverified.* | |
| consistency | tea colours | EditTeaTypeScreen.kt:57-71, ChartUtils.kt:19-30 | Tea-type colours are stock Material 500 hex values stored as strings and never adjusted for dark mode. | |
| arch | placement | components/analytics/ChartUtils.kt:5 | `String.hexToColor()` lives in the analytics package but is imported by 7 non-analytics files. | |
| bug | dark mode | androidApp/src/main/AndroidManifest.xml:15 | The window theme is `@android:style/Theme.Material.Light.NoActionBar` with no `values-night` and no SplashScreen API, so dark-mode users get a white splash and a white flash on launch. | |
| verified | hardcoded colours | 21 `Color(...)`/`Color.X` literals outside theme, in 6 files | These are mostly legitimate (fullscreen viewer black, scrims, the chart fallback palette). `VesselIcons.kt:19` uses a black stroke, but every call site tints it via `Icon`, so that's fine. | |
| polish | spacing | presentation/ | 732 `.dp` literals. They mostly follow a 4dp grid (16×193, 8×147, 4×86, 12×73, 24×46), but FAB clearance is done three different ways (60dp spacer, 88dp padding, none). | |

**Opportunity:** A tea-appropriate display font plus Shapes would give real identity cheaply. Tone tea-type colours per theme (e.g. derive a container colour from the hex).

### Strings and localization — not localization-ready at all.
- **0** `stringResource` calls. There's no `composeResources/values/strings.xml`; composeResources has only `drawable/`.
- **246** `Text("…")` literals and **303** `text=/label=/title=/message=/contentDescription="…"` literals across **71 files**. Representative: SettingsScreen.kt:207, VesselDetailScreen.kt:106-107, EditTeaTypeScreen.kt:343-347.
- Plurals are done by hand with "(s)" (VesselDetailScreen.kt:367,413, DeleteBrewingVesselUseCase.kt:28) or inline `if (n==1)` (EditTeaTypeScreen.kt:345).
- **User-facing copy lives in the domain layer**: DeleteBrewingVesselUseCase.kt:19,28 messages are shown verbatim in snackbars.
- Dates use hardcoded US-order patterns `"MMM d"` / `"MMM d, yyyy"` (TimeFormatter.android.kt, .ios.kt), and "Yesterday" is hardcoded.
- **Opportunity:** Given Phase 4, move strings as screens are touched. At minimum, errors should become typed domain errors mapped to copy in the UI.

### components/common reuse — decent shared kit, but several near-duplicates and inconsistent empty states.

| Sev | Category | Location | Finding |
|---|---|---|---|
| consistency | duplication | EditTeaSections.kt:43 & EditVesselSections.kt:52 | Byte-identical `internal fun SectionHeader`. There are 3 more private header variants (SettingsScreen.kt:379, TeaPickerSheet.kt:367, HistoryFilterSheet.kt:338). |
| consistency | duplication | TemperatureInputField.kt / VolumeInputField.kt | 123 lines each; they differ by only 44 lines after swapping names. `EditCapacityField.kt` is a label-only wrapper. One `UnitInputField` would cover all three. |
| ~~consistency~~ | ~~duplication~~ | ~~EditTea/EditSession/EditVessel/EditTeaType screens~~ | ~~4 copies of the "Discard Changes?" dialog, with inconsistent Discard styling.~~ |
| ~~consistency~~ | ~~duplication~~ | ~~HomeScreen.kt:374, TeaDetailScreen.kt:511, SessionDetailScreen.kt:409, HistoryScreen.kt:350~~ | ~~4 copies of the "Delete Session?" dialog. 11 inline delete dialogs total.~~ |
| consistency | duplication | EditTeaVM:95, TeaCollectionVM:117, TeaDetailVM:180 | A private `Flow.catchError` helper copy-pasted 3 times. |
| consistency | empty states | EmptyState used in 5 places | `AnalyticsEmptyState` and `EmptyHomeState` are separate. TeaTypeList (bare Text), VesselDetail ("Vessel not found" bare Text) and QuickTimerDetailsSheet:221 don't use it. `EmptyState` has no icon slot and always shows a FontAwesome PlusSquare. |
| consistency | icons | 49 files use Feather, 8 use FontAwesome, plus custom vectors | Two icon families are mixed, and FontAwesome ships whole because R8 is off. |
| a11y | touch target | TemperatureInputField.kt:56-66 (and Volume) | The unit-toggle suffix is text plus 4dp padding, about 28dp tall, and it changes a **global** preference. |

### Error handling — five different patterns, and several screens drop failures silently.
Counts: 41 `.onFailure`, 14 `catchError`, 85 sites that put `e.message` or `error.message` into user-facing text, and 7 `showSnackbar` calls in 5 files.

| Sev | Pattern | Where | Finding |
|---|---|---|---|
| bug | **invisible inline Snackbar** | TeaCollectionScreen.kt:232, HistoryScreen.kt:317 | The `Snackbar` sits in a `Column` **after** a `fillMaxSize()` LazyColumn with no `weight`. It gets no height and is laid out off-screen, so these errors never appear. (SessionDetail/Home/TeaDetail/LogTea use `align` in a Box and do show.) |
| bug | **error set, never rendered** | TimerScreen (TimerViewModel.kt:148,215,244,266,285), CreateBrewingConfigurationScreen (VM :121,234,254), VesselList | Timer start/pause/resume failures and brewing-method save failures are completely silent. `QuickTimerState.error` is never even written. |
| bug | **swallowed** | SettingsScreen.kt:145-148,155 | Export and import picker failures are mapped to "completed" or "cancelled". |
| friction | raw exceptions | 85 sites, e.g. CreateBrewingConfigurationVM:234 `"Failed to save: ${e.message}"` | SQLite and IO messages reach the user verbatim. |
| consistency | 3 UI mechanisms | SnackbarHost (5 files), static inline `Snackbar` + Dismiss (7 files), nothing (4 States) | Some clear the error after showing it (EditVessel `ErrorShown`), others never do (VesselDetail, EditTeaType), so a repeated identical error isn't shown again. |

**Opportunity [ROADMAP+ for Phase 4 "user-facing errors"]:** One `UiMessage` channel per VM rendered by a single `SnackbarHost` pattern. Map a small set of typed domain failures (`NotFound`, `InUse(count)`, `LastRemaining`, `Io`) to copy. Treat the invisible and unrendered cases above as bugs now, not Phase 4.

### Export/import (data layer) — works for the happy path (same platform, fresh device, few photos). There are several verified data-integrity bugs beyond the roadmap's "version never read".

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| **bug** | restore fails | DataExportRepositoryImpl.kt:43-47,208-217 + TeaSessionEntity.kt:18-29 | Export uses `getAll()` (soft-deleted rows excluded). Deleting a tea soft-deletes **only the tea** (TeaRepositoryImpl.kt:59-62), so its sessions are still exported with a `teaId` that isn't in the file. On a **fresh device**, inserting them breaks the `teaId` foreign key. Configurations pointing at a deleted vessel hit the same problem (CASCADE FK). If you've ever deleted a tea that had sessions, your backup can't be fully restored onto a new phone. | FK enforcement relies on Room enabling foreign keys (*runtime unverified*). |
| **bug** | partial writes | DataExportRepositoryImpl.kt:191-218 | There's no transaction; nothing in the codebase uses `withTransaction`. A failure midway leaves a half-imported DB, with images already written (:144-154). | |
| **bug** | cross-platform | iosMain/.../ZipArchiver.ios.kt:222-234 vs ZipArchiver.android.kt:17-23 | Android's `ZipOutputStream` writes DEFLATE (with data descriptors, so sizes are 0 in local headers). The hand-written iOS parser handles STORE only and throws "Only STORE method is supported". **An Android backup can't be imported on iOS.** | |
| **bug** | OOM | DataExportRepositoryImpl.kt:106-115; ZipArchiver.android.kt:25-43 | Every image is loaded into a `List<ZipEntry(ByteArray)>` on both export and import. iOS also builds the whole archive as one ByteArray plus an NSData copy. With no photo size or count cap, hundreds of full-size photos will run out of memory. **[ROADMAP+ 2.6: photo safety becomes a backup-integrity issue, not just storage.]** | |
| **bug** | semantics **[ROADMAP+]** | DataExportRepositoryImpl.kt:192-218 | The roadmap says "merges rather than clobbers". More precisely, it clobbers whole rows: it upserts without comparing `updatedAt`, so importing an older backup **reverts every edit** since then and **un-deletes** anything deleted since (the backup row has `deletedAt=null`). | |
| bug | photos | DataExportRepositoryImpl.kt:145-153 | Imported images are named `<index>.jpg` from a per-export counter and written with `writeBytes`, which overwrites. A second import of a *different* backup overwrites the first import's `session_images/0.jpg…`. Importing your own backup repoints rows to the copies and orphans the originals, doubling photo storage. | |
| bug | legacy path | DataExportRepositoryImpl.kt:170-172,184-185 | A legacy JSON import sets `photos = emptyList()` / `imagePath = null` and upserts. For sessions that already exist locally, that **detaches their existing photos**. | |
| **[ROADMAP+]** arch | versioning | data/models/LeafLogExportData.kt:12-18 | The wire format *is* the Room `@Serializable` entities. Each of the 5 migrations silently changed the backup format while `version` stayed at 1. The roadmap notes version is never read; the root issue is that the backup format is coupled to the schema. | |
| ~~polish~~ | ~~temp files~~ | ~~DataExportRepositoryImpl.kt:118-119; FileOperations.android.kt:88~~ | ~~`leaflog_export.zip` (a full copy of every photo) and `leaflog_import_temp` are never deleted from cache.~~ |   |
| polish | dirs | DataExportRepositoryImpl.kt:148 vs EditTeaViewModel.kt:206 | Imported tea photos go to `tea_photos/`; photos taken in the app go to `tea_images/`. | |
| verified | safety | DataExportRepositoryImpl.kt:145 | Zip-slip is neutralised by `substringAfterLast('/')`. | |

**Opportunities:**
- Stream ZIP entries file-to-file.
- Use DEFLATE on both platforms, or STORE on both (photos don't compress anyway).
- Import inside one write transaction.
- Use a DTO layer with an explicit version.
- Last-writer-wins on `updatedAt`, and carry `deletedAt` through the export.
- Content-hash image names (dedup and no collisions).
- A host-JVM round-trip test (roadmap 1.2) should include "tea deleted with sessions → restore to empty DB" and "Android zip → iOS parser".

### DI modules — clean and complete. No unused registrations; every use case and VM has a consumer (InitializeDefaults is used from the platform entry points).

| Sev | Category | Location | Finding |
|---|---|---|---|
| arch | CLAUDE.md violation | domain/usecases/DeleteTeaUseCase.kt, configuration/DeleteBrewingConfigurationUseCase.kt | Single repo call plus try/catch. EditTeaType deletes its type directly in the VM, which is the correct pattern, so the two approaches now sit side by side. |
| arch | CLAUDE.md violation (borderline) | ToggleFavoriteUseCase.kt (copy + upsert), SearchTeasUseCase.kt (blank guard + trim + `search`) | Near-wrappers. |
| arch | pointless | timer/CancelTimerUseCase.kt | A pure `copy()` wrapped in a try/catch that can't throw, returning Result. It could be a `TimerState` method. |
| arch | suspicious | timer/CompleteTimerUseCase.kt:17-20 | Upserts the session with `status = IN_PROGRESS`, which it already is. *Likely a redundant write; unverified.* |
| consistency | docs drift | di/RepositoryModule.kt | Repositories are `singleOf` (correct), but CLAUDE.md says `factoryOf` for repositories. Fix the doc. |
| arch | duplication | di/PlatformModule.android.kt / .ios.kt | The Room builder, migrations list and 5 DAO registrations are duplicated. Only the path and driver context differ. |
| verified **[ROADMAP+]** | migrations | both PlatformModules | All five migrations (`MIGRATION_1_2…5_6`) **are** registered on both builders, which answers the roadmap 1.1 question. |

### Build/config and store readiness — builds for personal use; several things block a store release or look unfinished.

| Sev | Category | Location | Finding |
|---|---|---|---|
| bug | iOS | iosApp.xcodeproj/project.pbxproj:312,345 | The widget extension's `IPHONEOS_DEPLOYMENT_TARGET = 26.2`; the app is 18.2. On iOS 18.2–26.1 the Live Activity extension won't install while the app still requests activities. |
| store blocker | iOS | pbxproj:318,351 (+ CURRENT_PROJECT_VERSION) | The widget's `MARKETING_VERSION = 1.0` / build number are hardcoded, while the app's come from `Config.xcconfig`. The first version bump makes App Store validation fail (extension version must match the app). |
| store blocker | iOS | (missing) PrivacyInfo.xcprivacy | iOS preferences use `NSUserDefaults` (PreferencesDataStore.ios.kt), a required-reason API. Without a privacy manifest, App Store Connect rejects the upload (ITMS-91053). |
| polish | iOS | pbxproj:310,343 | The widget's display name is "LeafLogWidgets" (visible in Live Activity settings). `ITSAppUsesNonExemptEncryption` isn't set. |
| store risk | Android | AndroidManifest.xml | `FOREGROUND_SERVICE_SPECIAL_USE` needs a Play Console declaration and review. `SCHEDULE_EXACT_ALARM` is denied by default on 14+. *Policy outcome unverified.* |
| polish | Android | androidApp/build.gradle.kts:21 | `isMinifyEnabled = false`: the full FontAwesome and Feather icon libraries and Ktor ship unshrunk. |
| arch | deps | shared/build.gradle.kts | `coil-network-ktor3` is declared, but the app has no network features and no INTERNET permission, so it's dead weight. `room-sqlite-wrapper` and `lifecycle-process` in `shared` androidMain are unused. The catalog also declares unused `androidx-core-ktx`, `androidx-appcompat` and `kotlin-testJunit`, beyond the roadmap's list. |
| polish | assets | composeResources/drawable | `compose-multiplatform.xml` (template leftover) and `french-press.svg`, `tea-leaf.svg`, `teapot.svg` are unreferenced. |
| friction | first run **[ROADMAP+]** | MainActivity.kt:27,31-42 | Android asks for notification permission in `onCreate` on the very first launch, with no context, and asks again on every recreation until granted or permanently denied. iOS asks lazily at first timer use. |
| polish | orientation | Manifest:37, iOSApp.swift | Portrait is locked. With targetSdk 36, Android 16 ignores the lock on large screens (sw≥600dp), and iPad declares all orientations, so landscape layouts will appear on tablets, foldables and iPad and have never been tested. |
| risk | backup | Manifest:10 | `allowBackup="true"` with no `dataExtractionRules`. Photos in `filesDir` count toward Android Auto Backup's 25MB quota, and going over it silently fails the whole backup. *Unverified.* |
| trap | git | index: screens/vessel/EditVesselFormLogic.kt (status `AD`) | This file is staged as *added* but deleted in the working tree. It defines the same 5 `internal` functions as EditVesselState.kt in the same package, so a `git commit` without `-a` produces a commit that fails to compile. |

### Platform parity

| Sev | Category | Location | Finding |
|---|---|---|---|
| **bug** | iOS | iosMain/.../TimeFormatter.ios.kt:33-40 | `isToday` and `isYesterday` compare `NSCalendarUnitDay`, which is **day-of-month**, not day-of-year. In the same year, a session from Aug 23 shows as "today" (time only) on Sept 23. Android correctly uses `DAY_OF_YEAR`. This affects every `SessionCard` (History). |
| **bug** | iOS | ZipArchiver.ios.kt:233 | iOS can't import Android backups (DEFLATE, see Export/import). |
| bug | iOS | pbxproj:312 | The Live Activity extension requires iOS 26.2 (see Build). |
| ~~parity~~ | ~~export UX~~ | ~~FileOperations.android/ios~~ | ~~Android uses a "Save as" document; iOS uses a share sheet, with `onExported()` fired on *presentation*, and `copyItemAtPath(error = null)` ignores copy failure. iOS uses the deprecated `keyWindow` (2 sites).~~ |
| parity | recreation | Android only | The config-change bugs (edit forms re-seeding, deep-link replay, export `pendingFilePath`) are Android-only, since iOS doesn't recreate the view controller. |
| by design | lifecycle | iosMain TimerLifecycleHandler.kt:5,9 | A no-op on iOS; scheduled notifications cover it. There are no other TODO stubs in platform code. The only TODOs are HomeScreen.kt:215 (`userName`) and LeafLogApplication.kt:35. |
| naming | iOS | PreferencesDataStore.ios.kt | Named "DataStore" but it's `NSUserDefaults` plus an in-memory StateFlow. Fine, but that's what triggers the privacy-manifest requirement. |