## Tea Library (Collection, Tea Detail, Add/Edit Tea, Brewing Methods)

Path prefixes: `L/` = `shared/src/commonMain/kotlin/dev/jketterer/leaflog/`, `UI/` = `L/presentation/ui/`. I read every item below in the code. Anything I could not confirm is marked **unverified**. Where a finding touches something ROADMAP.md already lists, I say so.

---

### Tea Collection (`UI/screens/collection/TeaCollectionScreen.kt` + ViewModel)

- **Purpose / verdict:** This is the tea library. It has a Teas/Vessels tab pair, filter chips (All / Favorites / by type), search by name, and five sort options that persist. It works for a small collection, but several pieces break down: search is a one-time snapshot, the error display can never be seen, and search looks at the name only. At hundreds of teas, finding one by producer or region is impossible.
- **Findings:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug~~ | ~~error handling~~ | ~~`UI/screens/collection/TeaCollectionScreen.kt:232-243`~~ | ~~The error Snackbar can never be seen.~~ | ~~It is the last child of a `Column` whose previous child (LazyColumn at :322, `EmptyState`, or the loading `Box`) uses `fillMaxSize()`. A Column measures children in order, so the Snackbar gets 0 height. Every `error` the VM sets (load failures, toggle failures, search failures) disappears silently.~~ |
| bug | correctness | `UI/screens/collection/TeaCollectionViewModel.kt:134-157` | Search results never update after they arrive. Tapping the heart on a search result writes to the DB but the icon does not change, and a second tap writes the same value again instead of toggling back. | `searchTeaUseCase` is a suspend one-time query (`TeaDao.search`, `L/data/local/database/dao/TeaDao.kt:56-64`). `ToggleFavorite(tea)` flips the stale `tea.isFavorite` from that snapshot. |
| bug | concurrency | `TeaCollectionViewModel.kt:142-156` | Typing searches can race each other. | `teaCollectionJob?.cancel()` runs, but the new search `launch` is never assigned to a job. Each keystroke starts an uncancelled query with no debounce, so an older result can arrive last and overwrite the newer one (**unverified in practice**). |
| bug | leak | `TeaCollectionViewModel.kt:68-89` | Each `loadData()` starts another `collectTeaTypes()` collector that never ends. | `loadData` runs on every filter chip tap and every time search is cleared (`:131`, `:138`). The Job returned by `collectTeaTypes()` is discarded, so N filter taps leave N live collectors on `tea_type`. The `try/catch` around the `launch` calls cannot catch anything. |
| friction | search | `L/data/local/database/dao/TeaDao.kt:60` | Search matches `name` only. | `producer`, `origin` and `description` are ignored, so "Yunnan" or "Farmer Leaf" finds nothing. `%` and `_` in the query are not escaped. |
| friction | filter | `TeaCollectionScreen.kt:275`, VM `:143-156` | Search ignores the active filter and hides the chips. Filters and search cannot be combined (Favorites + "oolong", say). | `searchTeaUseCase(query)` always searches all teas while `selectedFilter` stays set without being shown. |
| friction | copy | `L/domain/models/TeaSortOption.kt`, VM `:189-190` | "Most Recent" means last *brewed* but "Oldest" means first *added*. The pair looks like opposites but sorts on different fields, and there is no "Newest added" option. | `MOST_RECENT` sorts by `lastBrewedAt`; `OLDEST` sorts by `createdAt`. |
| friction | loading | VM `:123-131`, `:70` | Every filter-chip tap sets `isLoading = true`, which swaps the list for a full-screen spinner until Room emits. The list flickers on each tap. | `showByFilter` → `loadData` → `isLoading=true`. |
| friction | vessels tab | `TeaCollectionScreen.kt:370-405` | On the Vessels tab, "Add Your First Vessel" appears even when archived vessels exist and only the Active filter is empty. The chip labelled "All" actually means "include archived". ~~`vesselState.error` is never shown.~~ | Empty-state action is unconditional. ~~The screen never reads `vesselState.error`.~~ |
| ~~polish~~ | ~~copy~~ | ~~`UI/components/collection/TeaCard.kt:135`~~ | ~~Shows "1 brews".~~ | ~~No singular form.~~ |
| polish | layout | `TeaCollectionScreen.kt:348-349`, `:247-254` | The FAB is hand-placed with a hardcoded `60.dp` spacer and `16.dp` padding instead of a Scaffold `floatingActionButton`. At large font scale the last card can end up under the FAB. | Hardcoded dimensions. |
| polish | theming | `TeaCard.kt:65-81`, `L/data/repositories/TeaTypeRepositoryImpl.kt` (White = `#FFFDE7`) | ~~The accent stripe uses raw data colours: the White type's stripe is nearly invisible on a light card, and the brown types are low-contrast in dark mode.~~ A type with an empty or invalid hex still draws a transparent 6dp gap, because `hexToColor()` returns `Color.Unspecified`, never null (`UI/components/analytics/ChartUtils.kt:5-15`). | ~~TeaCard also imports a helper from `components/analytics`.~~ |
| polish | layout | iPad (`iosApp/iosApp.xcodeproj/project.pbxproj:380`) | iPad allows landscape, but the list is a single full-width column with no max width or grid. | Only iPhone and Android are portrait-locked (`androidApp/src/main/AndroidManifest.xml:37`). |
| arch | state | `TeaCollectionState.kt` / intents | `TeaClicked` and `AddTeaClicked` intents do nothing ("navigation handled by UI"). The tab and filter live in the VM without `SavedStateHandle`, so they reset after process death. `VesselListState` is referenced by fully-qualified name 5 times. | Dead intents; minor. |
| arch | CLAUDE.md | `L/domain/usecases/SearchTeasUseCase.kt`, `ToggleFavoriteUseCase.kt` | Both are single-repository-call wrappers, which CLAUDE.md forbids. | `search(query.trim())`; `copy()+upsert`. |

- **Opportunities:**
  - **Full-text search across name, producer, origin and description (Room FTS4), combinable with the chips.** At hundreds of teas you search by vendor or region, and `getDistinctProducersFlow` already shows producer is a real grouping.
  - **Facet chips for producer and origin, with counts on each type chip.** The distinct-producers query exists, and `GetTeaTypeDistributionUseCase` already computes counts per type.
  - **Make the list flow-backed so it keeps updating** (combine the query with `getAllFlow`, or use a Room `Flow` search). This removes the whole stale-snapshot class of bugs.
  - **Grid layout on wide screens, with the photo as the hero.** This fits the journal vision, and `Tea.photos` is already there.
  - **"Recently added" sort, and a fast-scroll alphabet index** for name sort at hundreds of teas.

---

### Tea Detail (`UI/screens/collection/TeaDetailScreen.kt` + ViewModel)

- **Purpose / verdict:** Per the roadmap's split of responsibilities, this screen owns per-tea judgement. In practice it is a property sheet: type, origin, producer, default temperature, a session count and an average, plus saved methods (edit/delete only) and up to 5 recent "sessions". Deleting a tea is visibly broken, and the average rating it shows is computed wrongly. ROADMAP 2.2 ("thin") already covers the depth problem. The findings below are the correctness issues underneath it.
- **Findings:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | correctness | `UI/screens/collection/TeaDetailViewModel.kt:235-252` + `L/data/local/database/dao/TeaDao.kt:33-34` | **Delete Tea leaves you on a live page for the deleted tea.** Nothing navigates back. | `onSuccess { /* navigation handled by UI */ }`, but the screen has no such handling. Delete is a soft delete, and `getByIdFlow` has no `deletedAt IS NULL` filter, so the flow re-emits the (deleted) tea and `isLoading` goes back to false. The page stays fully interactive, and "Brew This Tea" still works on the deleted tea. |
| bug | correctness | `L/domain/usecases/session/UpdateTeaStatsUseCase.kt:15-19` | **`Tea.averageRating` averages only the first-steep `rating` of each parent session.** It ignores `TeaSession.averageRating` across steeps. This number is shown on Tea Detail, on TeaCard, and drives the "Rating" sort. | For a gongfu session with steeps rated 3, 5, 5, the tea's average counts only the 3. `GetBrewingParametersPrefillUseCase` gets this right with `effectiveRating() = averageRating ?: rating`, so the prefill and the page disagree about which brews were good. That matters for ROADMAP 2.1/2.2, which say both must use the same "best brew". |
| bug | leak | `TeaDetailViewModel.kt:151-159` | Every emission of the tea flow starts four new collectors (type, sessions, configs, vessels) and never cancels the old ones. | Toggling favourite, a completed brew updating stats, or an edit each add another set. After a few favourite taps the screen runs 4×N flows. Use `flatMapLatest`/`combine`. |
| bug | data | `TeaDetailViewModel.kt:196-205` + `TeaSessionDao.kt:68-76` | "Recent sessions" includes child steeps, so a single 6-steep gongfu session fills all 5 slots. | `getByTeaIdFlow` has no `parentSessionId IS NULL` filter before `take(5)`. ROADMAP 3.1 notes the same thing for History only; this is new evidence that it also affects Tea Detail. |
| bug | copy/correctness | `TeaDetailScreen.kt:512` + `L/data/repositories/TeaSessionRepositoryImpl.kt:102-114` | The delete dialog always says "delete the session and all steeps". On a child-steep card only that one steep is deleted, and `DeleteSessionUseCase` does not recompute the parent's `averageRating`. | Parent-only cascade; no `UpdateAverageRatingUseCase` call. |
| bug | loading | `TeaDetailState.kt:17`, `TeaDetailScreen.kt:110-112,445-456` | "Tea not found" flashes on every open. | `isLoading` defaults to `false` and `LoadTea` only fires in a `LaunchedEffect` after the first frame, so frame 1 renders the `else ->` "Tea not found" branch. |
| friction | dead end | `TeaDetailScreen.kt:461-481` | "Brew This Tea" is always shown, including in the not-found and deleted states. The `BrewThisTeaClicked(vesselId)` parameter is always `null` (`:467`). | Saved methods are listed but cannot be *brewed from*. Only Home Quick Brew can launch a method. |
| friction | data shown | `TeaDetailScreen.kt:295-351` | `purchaseDate` is collected on Edit Tea and never displayed, here or anywhere else. The only readers in `presentation`/`domain` are the edit screen and the create/edit use cases. "Temperature" is really the *default* temperature but is labelled as if it were measured. | grep confirms no other reader. |
| friction | ROADMAP 2.2 correction | `UI/components/configuration/SavedMethodsSection.kt` | ROADMAP 2.2 says zero sessions and zero configs "both currently render nothing". Only half of that is true: zero configs shows "No saved methods yet."; zero sessions renders nothing (`TeaDetailScreen.kt:396`). | Sharper framing of an item the roadmap already lists. |
| polish | layout | `TeaDetailScreen.kt:175,496` | `padding(bottom = 80.dp)` is hardcoded to match the bottom button's height. At large font scale the button grows and covers the last card and the Snackbar. | Hardcoded dimension. |
| ~~polish~~ | ~~formatting~~ | ~~`TeaDetailScreen.kt:599-612` vs `TeaCard.kt:195-198`~~ | ~~The detail page *rounds* the average (4.46 shows as "4.5"); TeaCard *truncates* it ("4.4"). The same tea shows different ratings on two screens.~~ | ~~`Tea.averageRating` is stored untruncated (`UpdateTeaStatsUseCase:18`). CLAUDE.md says averages are truncated.~~ |
| polish | text | `TeaDetailScreen.kt:615-654` | `DetailRow` gives the label and the value 50% width each and ellipsises the value at 2 lines. Long origins like "Menghai, Xishuangbanna, Yunnan" are cut off with no way to read the rest. | `weight(1f)` ×2. |
| a11y | photos | `TeaDetailScreen.kt:280-288` | Every photo is announced as "Tea photo" with no position or count. | |
| arch | CLAUDE.md | `TeaDetailScreen.kt` (719 lines) | The file is well past "a few hundred lines". Four `AlertDialog`s are inline, with no `TeaDetailDialogs` / `TeaDetailSections.kt` split. `onNavigateToEditSession` (`:100`) is an unused parameter. | Composable-organization rule. |
| arch | CLAUDE.md | `L/domain/usecases/DeleteTeaUseCase.kt`, `configuration/DeleteBrewingConfigurationUseCase.kt` | Both are single-repository-call wrappers. | |
| arch | ROADMAP 2.4 correction | `L/data/local/database/dao/BrewingConfigurationDao.kt:38` | Deleting a brewing configuration is a **hard `DELETE`**, and `brewing_configurations` has no `deletedAt` column. ROADMAP 2.4 says the "soft-delete infrastructure (deletedAt) is already in place everywhere … mostly UI". For configurations, undo needs a migration or an in-memory hold. | New evidence against a roadmap statement. |

- **Opportunities:**
  - **Fix the tea average first, then build "best brew" (ROADMAP 2.2) on `effectiveRating`.** Otherwise the page and the prefill will name different best brews.
  - **"Brew with this method" on each ConfigurationCard.** Pass `configurationId` into LogTea. Quick Brew on Home already does this, so it is wiring, not modelling.
  - **Group recent sessions by parent session**, showing each as a card with its steep ratings. This matches the ROADMAP 3.1 rule for History and uses the existing `parentSessionId`/`steepNumber`.
  - **Cellar/age line:** "Bought Mar 2023 · 2y 6m in your cupboard". The data already exists (`purchaseDate`). Pair it with the planned `finishedAt` (2.3), and consider a `harvestYear`/vintage field, which matters more than purchase date for aged pu-erh and white tea. Nothing models that today.
  - **Show parameters against rating over time for this tea** (temperature/time points coloured by rating). Every completed session already stores the parameters and the rating, so no schema change is needed.

---

### Add / Edit Tea (`UI/screens/collection/EditTeaScreen.kt`, `EditTeaSections.kt`, `EditTeaFormLogic.kt`, ViewModel)

- **Purpose / verdict:** This is the best-built screen in the slice. It has a back guard for system back, clear required-field messages, temperature seeded from the tea type (tested), producer autocomplete, and cleanup of orphaned photos on discard. The remaining problems: no guard against saving twice, edit-mode save skips the normalisation that create does, and the flow ends in the wrong place after creating a tea.
- **Findings:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | double submit | `UI/screens/collection/EditTeaViewModel.kt:229-243`, `EditTeaScreen.kt:163-184` | Double-tapping Save in add mode can create **two teas**. | `save()` never checks `isSaving`, and the FAB is never disabled. `CreateTeaUseCase` generates a new UUID on each call. |
| bug | data | `L/domain/usecases/EditTeaUseCase.kt:39-47` vs `CreateTeaUseCase.kt:43-49` | Edit does not trim origin, producer or description; create does. After an edit, `"Farmer Leaf "` and `"Farmer Leaf"` become separate entries in the `DISTINCT producer` autocomplete. | The VM passes `takeIf { isNotBlank() }` without trimming (`:251-255`). |
| ~~bug~~ | ~~state~~ | ~~`EditTeaScreen.kt:65-67` vs `UI/screens/configuration/CreateBrewingConfigurationViewModel.kt:90`~~ | ~~`LaunchedEffect(teaId) { LoadTea }` has no guard, so if the Activity is recreated the form is reloaded from the DB over the user's edits. Newly added photo files are then orphaned, because `photos` is reset while `newlyAddedPhotos` still holds their paths. Android is portrait-locked, but a system dark-mode switch, font-scale change or split-screen still recreates the Activity (manifest has no `configChanges`). **Unverified on device**; the code path is clear.~~ | ~~The configuration VM already guards with `loadedTeaId`. Use the same pattern here.~~ |
| bug | stale write | `EditTeaUseCase.kt:39-52`, `EditTeaViewModel.kt:113-128` | Saving writes back the whole `existingTea` snapshot taken when the screen opened, including `totalSessions`, `averageRating`, `lastBrewedAt` and `isFavorite`. If a brew completes while the form is open (for example via a notification into review, then back), saving reverts the stats. **Edge case, unverified in practice.** | Whole-row `upsert` of a stale copy. |
| bug | crash (edge) | `EditTeaViewModel.kt:110-112`, `:203-209` | `.catchError(...).first()`: if the flow errors, `catch` emits nothing and `first()` throws `NoSuchElementException`, which is uncaught in `viewModelScope`. `addPhoto` does not guard `saveImage` either. On Android `writeBytes` throws. On iOS `writeToFile` returning `false` is ignored (`iosMain/.../ImageStorage.ios.kt`), so the tea stores a path to a file that does not exist. | ROADMAP 2.6 covers picker errors. The iOS silent write failure is new. |
| friction | flow | `EditTeaViewModel.kt:275-279` | After creating a tea you land back in the collection list, not on the new tea's page. With hundreds of teas sorted by name, you then have to find it. | `NavigateBack` in both modes. |
| friction | keyboard | `EditTeaScreen.kt:153-163` | The Save FAB is hidden whenever the IME is open, and there is no Save action in the top bar. To save, you must first dismiss the keyboard. | `AnimatedVisibility(!imeVisible)`. |
| friction | tea types | `EditTeaSections.kt:101-106` | The comment says "this is the screen where [tea types] get extended". It isn't: types are created only in Settings (`UI/screens/settings/teatype/EditTeaTypeViewModel.kt:189`). There is no "+ New type" chip, so a Yellow or Heicha tea means leaving the form. | Misleading comment and a dead end. |
| friction | missing | `EditTeaSections.kt:181-191` | Origin has no autocomplete, although producer does. There is no duplicate-name warning. | The `getDistinctProducersFlow` pattern could be copied for origin. |
| friction | field order | `EditTeaSections.kt:175-232` | The default brewing temperature comes *after* the multi-line description, and IME Next from producer jumps to description, skipping it. | |
| ~~polish~~ | ~~global side effect~~ | ~~`EditTeaViewModel.kt:222-227`~~ | ~~Tapping the °C/°F suffix changes the **app-wide** temperature preference, with no feedback.~~ | ~~See cross-screen patterns.~~ |
| a11y | photos | `UI/components/common/PhotoGrid.kt:67,75-79` | Tea photos are announced as "Session photo". The remove button is 24dp, below the 48dp minimum touch target. | |
| ~~a11y~~ | ~~unit toggle~~ | ~~`UI/components/common/TemperatureInputField.kt:56-65` (same in `VolumeInputField`)~~ | ~~The unit toggle is a `clickable` with `padding(4.dp)` around a short text, a touch target of roughly 24dp.~~ |  |
| polish | dates | `UI/components/common/DatePickerField.kt:99-122,134-137` | Future purchase dates are accepted (no `selectableDates`). ~~The display format is hardcoded English "Mar 5, 2024".~~ | |
| polish | photos | `EditTeaViewModel.kt:205` | Every photo is saved as `.jpg` whatever its real format (PNG or HEIC). The first photo is always the cover, and photos cannot be reordered. Photos come from the picker only, with no camera capture (`PhotoPickerButton.kt:25`). | |

- **Opportunities:**
  - **Capture from the camera (a photo of the bag or label), and let the user pick which photo is the cover.** Under the journal vision, photos carry weight, and packaging photos are the most common tea photo.
  - **After create, open the new tea's page**, with an inline "Save a brewing method" nudge. That closes the gap between adding a tea and brewing it well.
  - **Inline tea-type creation from the chip row** (reuse the Settings EditTeaType route or a small sheet).
  - **Harvest year / vintage field** alongside `purchaseDate`. This is the missing half of cellar tracking.

---

### Add / Edit Brewing Method (`UI/screens/configuration/CreateBrewingConfigurationScreen.kt` + ViewModel; components `ConfigurationCard`, `SavedMethodsSection`, `SaveConfigurationDialog`)

- **Purpose / verdict:** You define a reusable recipe for a tea and vessel pair by hand, or save one from a session through `SaveConfigurationDialog`. The idea is right, but the form is the weakest in the slice. It has no validation at all, fields cannot be cleared on edit, errors are never shown, there is no unsaved-changes guard, and the prefill ignores the vessel you actually pick. The model holds only one brew time, so it cannot represent a gongfu method.
- **Findings:**

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug~~ | ~~error handling~~ | ~~`CreateBrewingConfigurationScreen.kt` (whole file)~~ | ~~`state.error` is never rendered. "Configuration not found" and save failures (`VM:121,234,254`) disappear silently; after a failed save the button simply re-enables.~~ | ~~Nothing reads `state.error`.~~ |
| bug | edit | `L/domain/usecases/configuration/UpdateBrewingConfigurationUseCase.kt:43-51` | **Values cannot be cleared on edit.** Emptying tea quantity (switching to a tea bag) or the name keeps the old value, because every field is `new ?: existing`. | The VM passes `toFloatOrNull()` (null when blank) and `label.takeIf { isNotBlank() }` (`VM:220-222`). |
| bug | edit | `CreateBrewingConfigurationViewModel.kt:97,102` | If the method's vessel is archived, the edit screen opens with no vessel selected and Save disabled, with no explanation. | Loads `getActiveFlow()` only, so `selectedVessel = null`. |
| bug | prefill | `CreateBrewingConfigurationViewModel.kt:124-139,57-60` | Prefill uses the *first* active vessel only. Choosing a different vessel does not re-prefill, so picking a 100ml gaiwan keeps water from a 400ml teapot. | `SelectVessel` just sets the vessel. `GetBrewingParametersPrefillUseCase(tea, vessel)` is vessel-specific. |
| bug | misleading data | `UpdateBrewingConfigurationUseCase.kt:43-53` | Editing a method saved from a session keeps that session's `rating` and `sourceSessionId`. The card still shows ★★★★★ for parameters that were never rated, and prefill still ranks it by that rating (`BrewingConfigurationDao.kt:15`, `ORDER BY rating DESC`). | |
| bug | durations | `UI/components/common/DurationPicker.kt:38` + `CreateBrewingConfigurationScreen.kt:150-157` | Typing "90" in the seconds field silently stores 59s while the field still shows 90. Clearing both fields emits `null`, which this screen drops, so Save keeps the previous time while the field looks empty. | `coerceIn(0, 59)`; `duration?.let { … }`. |
| friction | validation | `CreateBrewingConfigurationViewModel.kt:206-212`, `CreateBrewingConfigurationUseCase.kt:32-75` | There is no range validation. 500°C, negative or zero water, and "abc" grams (silently saved as null, meaning a tea bag) are all accepted. Save is *disabled with no explanation*, which is the exact pattern Edit Tea's comment rejects ("A dead button explains nothing"). | Compare `EditTeaScreen.kt:171-172`. |
| friction | back | `CreateBrewingConfigurationScreen.kt:91`, VM `:83-85` | There is no unsaved-changes guard; the back arrow and system back discard everything. | Edit Tea has `NavigationBackHandler`. |
| friction | context | `CreateBrewingConfigurationScreen.kt:89` | The title "Add Brewing Method" never says which tea it is for. | `teaId` is loaded but the name is not shown. |
| friction | dead end | `UI/components/common/VesselSelector.kt:85-133` | With no active vessels, the dropdown opens empty, Save stays disabled, and there is no link to add a vessel. | |
| friction | defaults | `CreateBrewingConfigurationState.kt:18` vs VM `:153` | The initial water type is `SPRING`, but the loaded fallback is `FILTERED`. | Inconsistent defaults. |
| polish | card | `UI/components/configuration/ConfigurationCard.kt:109-115,150-151,121,128` | Water volume appears twice (in the vessel line and as a chip). Water type is not shown at all. ~~Grams display as "5.0g". Copy reads "Used 1 times" and "2 week(s) ago".~~ The card has no tap action. | |
| polish | icons | `SaveConfigurationDialog.kt:108` vs `ConfigurationCard.kt:150` | Tea quantity uses a Coffee icon in the dialog but `ic_tea_leaf` on the card. ~~`formatBrewingTime` is duplicated (`SaveConfigurationDialog.kt:152` and `BrewingParamChip.kt:49`).~~ | |
| polish | labels | `L/domain/usecases/configuration/GenerateConfigurationLabelUseCase.kt` | Auto-labels ("Gong-fu", "Western", "Standard") ignore the vessel and temperature, so a tea's methods often share a name. This compounds ROADMAP 2.7 (Choose Method dialog can't tell methods apart). Session-saved methods use `config_<millis>_<rand>` IDs while manual ones use UUIDs (`SaveBrewingConfigurationUseCase.kt:67-69`). | Sharpens 2.7: the fix belongs in the label as well as the dialog. |
| a11y | dropdowns | `VesselSelector.kt:80`, `UI/components/common/WaterTypeSelector.kt:54` | Read-only dropdowns use the `PrimaryEditable` anchor. Material intends `PrimaryNotEditable` here, and screen readers will announce an editable field (**unverified with TalkBack**). | |
| arch | dead field | `L/domain/models/BrewingConfiguration.kt:29` | `isActive` ("User can disable configs without deleting") is never set to `false` anywhere. The only writer passes `isActive = null` (`VM:227`). Yet 4 DAO queries filter on it and the card has a dimmed style for it. This is dead code the ROADMAP's dead-code list misses. | grep confirms. |
| arch | duplication | `CreateBrewingConfigurationScreen.kt:134-145,180-185,223-228,265-269` | The preset lists are copied verbatim in `UI/screens/log/LogTeaSections.kt:189,248,290,329` and `UI/components/quicktimer/QuickTimerDetailsSheet.kt:313-326`. ~~Display↔storage unit conversion happens inline in the composable (`:173-177,216-220`), while Edit Tea keeps it as a derived property on its State.~~ | |
| arch | indentation | `CreateBrewingConfigurationScreen.kt:100-316` | The LazyColumn items are indented inconsistently. | Readability for anyone reading the code. |

- **Opportunities:**
  - **Model multi-steep schedules.** `BrewingConfiguration.brewingTime` is a single `Duration`, so a gongfu method is really only its first infusion. Sessions already record per-steep `brewingTime` with `steepNumber`, so a schedule (base + increment, or an explicit list) can be *learned* from the best-rated parent session's child steeps, then fed to the timer to pre-set steep N.
  - **Link a method to where it came from.** `sourceSessionId` is stored but never shown. "From your 12 Mar brew, ★4.5" could open that session. On edit, drop the rating or mark it "modified".
  - **Make saved methods the main way to brew a tea you know:** a "Brew" button on the card, and re-running the prefill when the vessel changes.
  - **Structured tasting vocabulary** (tags such as floral, roasty, astringent, per steep), grouped per tea. Today there is only free-text `description` and `notes`, and no tag model anywhere. This would make ratings explainable and methods comparable.

---

### Shared domain / prefill (`L/domain/usecases/session/GetBrewingParametersPrefillUseCase.kt`)

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | ranking | `GetBrewingParametersPrefillUseCase.kt:56-73` | The same-type fallback returns the config of the *alphabetically first* tea of that type that has one, not the best-rated. It also makes one query per tea of that type (N+1) on every prefill. | `teaRepository.getAll()` is ordered by name, and the loop returns on the first hit. |
| friction | ranking | `BrewingConfigurationDao.kt:15` | Manually created methods have `rating = null`, so under `ORDER BY rating DESC` (NULL sorts lowest) they rank *below* any auto-saved 3★ method. A deliberate manual recipe loses to a passing save. | |
| polish | copy | `UI/components/common/PrefillBanner.kt:38-56` | The SavedConfig banner doesn't name the method. The TypeDefault banner says "your usual temperature for Green tea" when it is a system default (72–100°C seeded in `TeaTypeRepositoryImpl`), not the user's. | |

---

## Cross-screen patterns

1. ~~**Four different ways to show errors, two of them broken.** Collection puts an inline Snackbar in a Column where it can't be seen. Detail uses a Box-aligned Snackbar with a hardcoded 80dp offset. Edit Tea uses `SnackbarHost` (correct). Configuration shows nothing. All of them show raw `"...: ${e.message}"` exception text. This adds concrete evidence to Phase 4 "user-facing error messages": one shared `SnackbarHost` pattern would fix all four.~~
2. **Collectors started per event and never cancelled.** Collection `loadData → collectTeaTypes` and Detail `loadTea → 4× load*` both launch a new `collect` on every trigger. The fix is the same in both: a `combine`/`flatMapLatest` pipeline built once.
3. **Whole-row upserts from stale snapshots.** `ToggleFavoriteUseCase`, `EditTeaUseCase` and search results write back an entire `Tea` read earlier. That overwrites fields other flows changed (stats, favourite). Targeted `UPDATE` queries (`isFavorite = NOT isFavorite`) or a re-read inside the use case would remove the class of bug.
4. **Soft-deleted rows still visible through by-id queries.** `TeaDao.getByIdFlow`/`getById` (and `TeaTypeDao.getById*`) do not filter `deletedAt`. That is the root cause of the Delete Tea bug, and any deep link to a deleted tea will render it too.
5. **Rating semantics diverge.** The tea average uses steep-1 `rating`; prefill uses `averageRating ?: rating`. ~~Display formatting truncates in `TeaCard.formatOneDecimal`, `TopRatedTeasChart` and `TeaPickerSheet`, but rounds in `TeaDetailScreen.Float.format` and `PrefillBanner.formatRating`.~~ One domain `effectiveRating` plus one formatter is needed before ROADMAP 2.1/2.2 "best brew" can be consistent.
6. **Form inconsistency.** Edit Tea keeps Save enabled and explains what is missing, guards back, and validates ranges in the display unit. The Configuration form does none of these. Preset lists and unit conversion are copy-pasted across Configuration, LogTea and QuickTimer.
7. ~~**Unit suffix toggles silently change a global preference** (temperature and volume) from inside a form, with about a 24dp touch target and no feedback. This is the same pattern ROADMAP 2.7 praises for the analytics weight toggle, but inside a form it is a surprising side effect.~~
8. ~~**Missing plurals and hardcoded English:** "1 brews", "Used 1 times", "week(s)", English-only date format.~~ Hardcoded bottom offsets (60dp, 80dp, 88dp) stand in for Scaffold slots.
9. **Read-only dropdowns use the `PrimaryEditable` anchor** (VesselSelector, WaterTypeSelector), and touch targets are small (unit suffixes, photo-remove at 24dp). These are cheap a11y wins even with a11y accepted as a non-gate.
10. **CLAUDE.md violations to delete:** single-call use cases `SearchTeasUseCase`, `ToggleFavoriteUseCase`, `DeleteTeaUseCase`, `DeleteBrewingConfigurationUseCase`. `TeaDetailScreen.kt` at 719 lines has inline dialogs. The dead field `BrewingConfiguration.isActive` belongs in the Phase 2.3 cleanup migration next to `purchasePrice`/`stockAmount`.
11. **Data collected but never read back:** `Tea.purchaseDate`, `BrewingConfiguration.sourceSessionId`, and per-steep brew times on child sessions. All three are the raw material for the ambitious items above (cellar age, method provenance, learned steep schedules) and need no new capture UI.