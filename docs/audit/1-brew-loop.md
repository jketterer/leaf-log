## Brew loop: Home, Log Tea, Timer, Quick Timer, Steep Complete (plus the navigation between them)

**Path legend:** `ui/` = `shared/src/commonMain/kotlin/dev/jketterer/leaflog/presentation/ui/` · `dom/` = `…/leaflog/domain/` · `dat/` = `…/leaflog/data/` · `and/` = `shared/src/androidMain/kotlin/dev/jketterer/leaflog/` · `ios/` = `shared/src/iosMain/kotlin/dev/jketterer/leaflog/` · `app/` = `androidApp/src/main/`.

I checked every finding by reading the code. Anything that also needs a device or a layout pass to confirm is marked "(unverified)". Nothing was edited or built.

---

### Home

- **Purpose / verdict:** Home is the launchpad. It has the resume banner, today's stats, the quick-brew row, recent sessions and the FAB. The layout is right for daily use, but the quick-brew row doesn't record usage, the screen fully reloads on every tab switch, and several of its tiles do things you wouldn't guess. Two ROADMAP 2.7 items about this area no longer match the code.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | correctness | `dom/usecases/session/CreateSessionUseCase.kt:101-117` (called from `ui/screens/home/HomeViewModel.kt:488-500`) | Quick Brew creates the session **without `usedConfigurationId`** and **with `rating = config.rating`**. | Because of this, neither `CompleteSessionUseCase` nor `SteepCompleteViewModel.finishSession` ever raises `timesUsed`/`lastUsedAt` for the one feature built around saved methods. The new, untasted session is also pre-rated. `SteepCompleteViewModel.initialize` only suggests a rating when `rating == null`, so the method's rating shows up as if the user gave it, and it is kept if the review is skipped. |
| bug | correctness | `ui/viewmodel/InProgressSessionDelegate.kt:96` → `dom/usecases/session/CompleteSessionUseCase.kt` (`rating = rating`) | In the conflict dialog, "Complete & Start New" passes `rating = null`, which **erases any rating already on the session**. | Two ways this bites: a rating typed on Steep Complete before pressing Back is wiped, and so is the rating copied in by Quick Brew. This path also skips `UpdateAverageRatingUseCase` for child steeps and never clears the `timer*` fields. |
| bug | correctness | `ui/components/session/SessionInProgressDialog.kt` (Discard button) + delegate `:123` | "Discard & Start New" deletes the session in one tap with no confirmation and no undo. | Every other destructive action in the slice asks first. |
| friction | state | `ui/navigation/AppNavigation.kt:167-170`, `HomeViewModel.kt:~262`, `ui/screens/home/HomeScreen.kt:188-195` | Every bottom-tab switch calls `backStack.clear()`, which destroys Home's entry and its ViewModel. Coming back rebuilds everything: `isLoading` shows a full-screen spinner and scroll position is lost. | `rememberViewModelStoreNavEntryDecorator` clears the store when an entry leaves the back stack. |
| bug | polish | `ui/components/home/ExpandableFAB.kt:45,128-130` | When the menu opens, the icon switches from Plus to X *and* rotates 45°. An X turned 45° is a "+", so the open state looks the same as the closed one. | `rotate(rotation)` is applied to the already-swapped icon. |
| bug | state | `ui/components/home/QuickBrewCard.kt:57` | The card uses `widthIn(min = cardWidth)` inside a `LazyRow`, where the main-axis width is unbounded. Long tea names or labels therefore stretch the card, the `maxLines=1` ellipsis never kicks in, and the "about 2.3 cards visible" sizing in `QuickBrewSection` stops working. | Reasoned from layout constraints (unverified on device). |
| bug | state | `ui/components/home/DailyStatsCard.kt:64-76` | The auto-shrink `scaleFactor` is a `remember` with no key and can only shrink. After a long value (e.g. "1,250 ml") shrinks the text, short values like "3" stay small. | It only multiplies by 0.9 and never resets. |
| bug | state | `dom/usecases/session/GetDailyStatsUseCase.kt:32`, `ui/components/home/GreetingHeader.kt:28`, `HomeViewModel.kt:265` | "Today" is only recalculated when a flow emits, when the composition is new, or on `loadData`. If Home stays open past midnight, stats, date and greeting stay on yesterday until something writes to the DB. | `today` is computed inside the combine lambda, and the header uses `remember { todayIn(...) }`. |
| friction | purpose | `ui/components/home/DraftSessionsBanner.kt:42-139` | With more than one in-progress session, the banner shows only the newest. `totalInProgressCount` is only displayed in the no-details fallback, so the other sessions can't be reached from Home. | — |
| friction | purpose | `ui/components/home/DailyStatsSection.kt:64-90`, `HomeViewModel.kt:219,511` | The three stat tiles do unrelated things: Sessions opens History filtered to today, Water silently toggles the global volume unit, and Teas opens the whole Collection rather than today's teas. Nothing hints at any of this. | Tapping a stats tile should not change a setting. |
| friction | flow | `HomeScreen.kt:120-123`, `HomeViewModel.kt:238` | "Finish" goes to `TimerRoute`, which immediately redirects to Steep Complete. That's an extra screen and spinner on the way to the review. | Navigate straight to `SteepCompleteRoute`. |
| friction | flow | `QuickBrewCard.kt:58-61`, `HomeViewModel.kt:438` | Editing a quick-brew card is long-press only, with no visual hint and no `onLongClickLabel`. Any method used even once (`timesUsed > 0`) appears on Home permanently, and Manage can only pin or reorder, not hide. | — |
| polish | state | `ui/components/home/EmptyHomeState.kt:35-39,65,77` | ~~The empty state doesn't scroll (`fillMaxSize` plus `Arrangement.Center`), so it clips at large font sizes. The button label contains a literal "+". The tip "Add teas to your collection first" is misleading because Log Tea has quick-add.~~ The empty state also replaces the quick-brew row and stats whenever there are zero parent sessions, even if methods are pinned. | ~~Stale KDoc "Uses emoji icons" here and at `HomeScreen.kt:73`.~~ |
| polish | theming | `HomeScreen.kt:337`, `ExpandableFAB.kt:71-78` | The scrim is hard-coded `Color.Black.copy(0.4f)`. The FAB menu labels are plain `onSurface` text on that scrim, which is low-contrast in light theme, and only the mini-FAB is tappable, not the label. There is no BackHandler to close the menu. | — |
| polish | state | `ui/components/session/SessionCard.kt:131-137,147-151,185-193` | (1) The " • " separator depends on `session.rating`, but the value shown prefers `averageRating`. A parent with steep 1 unrated renders "2h ago4.5". (2) `"$teaTypeName • $vesselName"` has no `maxLines`. (3) The "IN PROGRESS" chip uses `errorContainer` for a normal state and has `onClick = {}`. (4) Grams render as "5.0g". | — |
| a11y | a11y | `AppNavigation.kt:494-540`, `QuickBrewCard.kt:58`, `DailyStatsCard.kt` | Nav icons use `contentDescription = ""` (the labels still read, but `null` is the idiom). The long-press edit has no semantic label, and the stat tiles have no `onClickLabel`, so their hidden actions can't be discovered. | — |
| consistency | errors | `HomeScreen.kt:353-367` | Errors use a hand-made `Snackbar` showing the raw `e.message`, and it stays until dismissed. Screens outside this slice (EditTea, EditVessel, TeaTypeList) use `SnackbarHost`. | — |
| arch | dead code | ~~`ui/components/home/RecentSessionsCard.kt` (168 lines, no references);~~ `HomeIntent.Refresh` / `HomeState.isRefreshing` (never dispatched or set by the UI); `HomeScreen.kt:215` `userName = null // TODO` | Dead weight. The file `DraftSessionsBanner.kt` actually holds `InProgressSessionsBanner`. | — |
| consistency | roadmap | `HomeScreen.kt:347` (`visible = true`, since commit 558d194) | **ROADMAP 2.7 is stale:** it says the FAB is hidden during an in-progress session, but it is always visible now and the conflict dialog replaced that behaviour. | New evidence. |

**Opportunities**
- **One-tap brew from a pinned method.** A config already carries every parameter, yet today it takes Quick Brew, then the Timer screen, then "Start Brewing". Create the session and start the timer in one step.
- **Brew Again that replays the whole steep curve.** Child steeps already store each steep's time and temperature, so a gongfu session is a replayable schedule today. `BrewAgainUseCase` only copies steep 1.
- **Time-of-day suggestion instead of a static greeting.** Session timestamps and `GetTimeInsightsUseCase` exist, so Home could say "your usual 3pm oolong".
- **Replace the Water tile's unit toggle with a "today's cups" strip** that links into a History day view. This fits the Phase 3 journal direction.
- **Show every in-progress session and a live countdown in the banner.** `TimerState.remainingDuration` is already collected.

---

### Log Tea (with TeaPickerSheet, QuickAddTeaForm, ChooseMethodDialog)

- **Purpose / verdict:** This is the full-form path: tea (the picker, with recents, favorites, type chips and quick-add, is the best component in the slice), vessel, prefilled parameters, then Begin (timer) or Log (already brewed). The prefill order is solid domain logic. The problems are silent gating, a water-type reset bug, a destructive path hidden in the method dialog, and leftover scaffolding.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | correctness | `ui/screens/log/LogTeaViewModel.kt:186-196` | The load collector sets `selectedWaterType = prefs.defaultWaterType` on **every** emission of a five-way `combine`. Toggling °C/°F or ml/oz (a prefs write), or quick-adding a tea (the teas flow emits), resets whatever water type the user or the prefill chose (`:277`). | — |
| friction | state | `ui/screens/log/LogTeaScreen.kt:284-298`, `LogTeaViewModel.kt:577-589` | The action buttons only appear once the form is valid (`canSave`), and nothing says what's missing. The "X is required" messages in `saveSession` can never show, because the buttons that would trigger them aren't on screen. For a tea with no history, brewing time is null and the only signal is that there are no buttons. | — |
| friction | flow | loop | "Begin Session" opens the Timer, which then needs "Start Brewing"; the timer never auto-starts (`TimerViewModel.initialize` leaves it NOT_STARTED). Quick Brew and Brew Again work the same way. That's three taps and a screen before any countdown. | — |
| bug | data | ~~`LogTeaViewModel.kt:281`~~ | ~~`completionRating = prefill.rating` pre-selects stars in the "Log Session" dialog from a saved method or a past brew. Confirming without touching them records a rating the user didn't give.~~ | See Cross-screen #5. |
| friction | correctness | `ui/components/configuration/ChooseMethodDialog.kt:90,99` + `LogTeaViewModel.kt:295-312` | Tapping the selected method deselects it. "Select" then sends `null`, which means "Custom" and **wipes every parameter**. There is no visible Custom option and no undo. | Also "Used 1 times" (`:216`) and the rating shown as a raw float ("4.0"). |
| consistency | roadmap | `ChooseMethodDialog.kt:~180-210` | **ROADMAP 2.7 is partly stale:** the dialog now shows tea, temperature, time and water chips (since March 2026). The vessel is left out, but the list is already filtered to the chosen tea and vessel, so that's fine. | New evidence. |
| arch | CLAUDE.md | `LogTeaScreen.kt:110-117,120` | `LaunchedEffect(hasOptionalData) { showOptionalFields = true }` is one of the warning signs CLAUDE.md lists. The collapsed summary compares against `WaterType.FILTERED`, while `hasOptionalData` compares against the user's default, so they disagree. | — |
| arch | dead code | `LogTeaIntent` AddPhotoClicked / PhotoSelected / PhotoRemoved and `state.photos` (there's no photo UI); `hasUnsavedChanges` (set about 12 times, never read, so Back has no discard guard); `LogTeaRoute.configurationId` (no caller passes it) | If a caller ever did pass `configurationId`, `MethodSelected` at `LogTeaScreen.kt:75-77` would race the async `loadConfigurationsAndPrefill` and be overwritten. | — |
| consistency | copy | `LogTeaSections.kt:246-256` vs `QuickTimerDurationSheet.kt:126-167` vs `QuickTimerDetailsSheet.kt:311-327` | Brew-time presets run 0:30–5:00 here but 10s–5m in Quick Timer. The full form has no preset for the 10–20s gongfu flash steep. The temperature, water and tea presets are copy-pasted between the two screens. | — |
| polish | state | `LogTeaViewModel.kt:270` | A prefilled leaf weight shows as "5.0" in the field (`Float.toString`). | — |
| polish | layout | `LogTeaScreen.kt:300-313` | The error Snackbar is aligned `BottomCenter`, on top of the Begin/Log buttons, and shows the raw exception message. | — |
| friction | copy | FAB → `LogTeaSections.kt:455,467` → `TimerControlButtons` | The FAB's "Log Session" opens the form. Inside the form, "Log Session" means "save as already brewed", and "Begin Session" starts the timer, which then says "Start Brewing". That's four wordings for one flow, and one of them means two different things. | — |
| friction | nav | `AppNavigation.kt:395-398` | Begin clears the whole back stack. Starting from Tea Detail → Log → Timer, Back lands on Home, not the tea you came from. | Possibly intentional. |
| a11y | a11y | `ui/components/collection/TeaPickerSheet.kt:391-412` | The selected picker row is only shown by its background; it has no `selected` semantics. | Minor; otherwise the picker and quick-add are well built (back handling inside the sheet, keyed `remember`s). |

**Opportunities**
- **Default brewing time (and leaf ratio) per tea type.** `TeaType` only has `defaultTemperatureCelsius`, so the first brew of any new tea needs the time typed by hand. With a type-level time and ratio, every field could be prefilled.
- **Scale the prefill to the vessel.** Configs and sessions store grams and ml, and vessels store `capacityMl`. Prefill step 3 ("same type, same vessel") copies absolute grams; scaling to the vessel's volume would be more accurate.
- **Inline "Last time: 5g · 95° · 20s · ★4" under the tea card.** The prefill already queries `getByTeaId`.
- **One primary "Brew" button that auto-starts the timer,** with "Log a past brew" as the secondary action, since logging after the fact is the rare case.

---

### Timer

- **Purpose / verdict:** The countdown screen: ±adjust, pause/reset, complete now, edit parameters. The underlying engine (`TimerService` plus clock-based `resolvedAt`) is sound, and the ROADMAP's hard-won fixes are in place. At the screen level, errors are never displayed (so several paths dead-end silently), the time you actually brewed is discarded, and editing parameters mid-brew can overwrite the saved timer state.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | errors | `ui/screens/timer/TimerScreen.kt` (never reads `state.error`); set at `TimerViewModel.kt:145,243,265,284,306,368,394` | The Timer screen **never shows errors**. For example, "Session not found" (such as after tapping a stale notification for a discarded session) leaves a 0:00 ring and a Start button that does nothing, because `startTimer` returns early when there is no session. | — |
| bug | correctness | `dom/usecases/timer/AdjustTimeUseCase.kt:17-18`, `ui/components/timer/QuickAdjustButtons.kt:70` | Before starting, −1m or −30s on a short timer makes `totalDuration` **negative**; that branch has no lower limit. The ring shows 0:00, and Start fails with "Duration must be greater than 0", but since errors aren't shown, Start just does nothing until the user taps +. | — |
| bug | data | `TimerViewModel.kt:291-311`, `dom/usecases/timer/CompleteTimerUseCase.kt:26`, `SaveTimerStateUseCase.kt:33` | **The time actually brewed is never recorded.** ±adjust only changes `timerTotalMs`, "Complete Now" records nothing, and `session.brewingTime` keeps the planned value. That planned value is what Steep Complete displays and what "best brew" prefill replays. `timerTotalMs` is only read by restore and by the banner's progress bar. | — |
| bug | correctness | `TimerViewModel.kt:454-490` (also `StartTimerUseCase.kt:31`, `CompleteTimerUseCase.kt:26`) | `confirmEditSession` saves a **stale copy of the whole session row**, `_state.value.session.copy(...)`. That copy's `timer*` fields date from `initialize`, because Start and Pause save through `SaveTimerStateUseCase` and never update `state.session`. Editing parameters mid-brew therefore writes `timerStatus = null` (fresh session): the Home banner loses its progress bar, and a restore after process death restarts from full duration. It stays wrong until the next pause or completion re-saves. | — |
| bug | state | `TimerViewModel.kt:321-334` | Reset (offered only while paused) updates `TimerService` but isn't saved. The DB still says PAUSED with the old remaining time, and `initialize` then prefers the DB because the live state is NOT_STARTED. Leave and come back, or lose the process, and the reset is undone. | — |
| bug | data | `TimerViewModel.kt:437-466` (same pattern in `SteepCompleteViewModel.kt:519-563`) | The edit sheet converts through **whole-number display units**. For fl-oz users, opening it and pressing Save without changes turns 150 ml into 147.9 ml (5 oz) and 250 ml into 236.6 ml. For °F users, 83 °C becomes 181 °F and then 82.8 °C. | `fromCelsius` / `fromMilliliters` return `Int`, and confirm always rebuilds the values from the display strings. |
| bug | notifications | `and/domain/services/TimerNotificationService.kt:200-218,302-304`; iOS never removes the delivered completion notification | The "X is ready!" notification is only cleared when tapped. After finishing in the app it stays; tapping it later opens Steep Complete for a session that is already COMPLETED. | Steep Complete has no status guard (see Steep Complete). |
| bug | notifications (unverified) | `and/domain/services/TimerNotificationService.kt:72-77` | The running, complete and reminder notifications all build their tap PendingIntent with `requestCode = 0` and the same Intent. Extras don't count toward PendingIntent identity, so with `FLAG_UPDATE_CURRENT` whichever was built last decides where *every* live notification opens. For example, a leftover "ready" notification for steep A opens `Timer(A2)` once the next steep's notification is rebuilt. | Same shape as the ROADMAP's earlier "tap lands on the wrong screen" bug; needs a device check. |
| friction | roadmap framing | `dom/services/TimerService.kt:137-139`; `ios/domain/services/TimerNotificationService.kt` (`willPresent` → Banner\|Sound) | **Sharper framing for ROADMAP 2.5:** in the foreground, completion *is* signalled, but only through the system notification. So it has the notification's weak tone, and it disappears completely when the in-app "timer completion" toggle is off. The Timer's warning banner (`showNotificationWarning`) only checks OS permission, not that toggle. | New framing. |
| polish | layout | `TimerScreen.kt:248-303` (duplicated in `QuickTimerScreen.kt:205-260`) | The ring size is `maxHeight − 420.dp`, a hard-coded guess, and the column doesn't scroll. With the permission banner showing, a large font, or on iOS/tablet landscape, the controls fall off screen. Android phones are portrait-locked (`app/AndroidManifest.xml:37`), which hides this there. | Unverified on device. |
| polish | copy | `TimerScreen.kt:213`, `QuickTimerScreen.kt:134` | The reset dialog formats the duration with `Duration.toString()` ("2m 30s"); everywhere else uses "2:30". | — |
| consistency | copy | `TimerScreen.kt:149-207` | The overflow item says "Cancel Session", the dialog title says "Cancel Session?", and the confirm button says "Delete". | — |
| arch | structure | `TimerViewModel.kt:65-76,344-373` | `onCleared` does a DB write inside `runBlocking` on the main thread. Discard bypasses `DeleteSessionUseCase` (so tea stats aren't updated), does a pointless `saveTimerStateUseCase.clear` right before deleting, and an exception from `delete` escapes the coroutine. `CompleteTimerUseCase` only re-saves the stale snapshot with IN_PROGRESS: it adds nothing and can overwrite newer data. | — |
| a11y | a11y | `ui/components/timer/CircularTimerRing.kt` | The Canvas arc has no semantics, and the time isn't a live region, so screen readers don't announce completion. The ring's font scales with the ring and again with the system font scale, so a 100dp ring can overflow. | — |

**Opportunities**
- **Record the actual brewed time** (elapsed at completion) as `brewingTime` or a new `actualBrewingTimeMs`. History becomes accurate, and the prefill replays what you really did.
- **Model a gongfu steep schedule.** `BrewingConfiguration` has one `brewingTime`, and `AddSteepUseCase` takes each steep's time from the caller. A base + increment (or an explicit list) would let the Timer show "Steep 3 · next 25s" and advance automatically.
- **Rinse/preheat as "steep 0".** It isn't modelled today; `steepNumber` starts at 1.
- **An auto-start preference** would remove one tap from every brew.
- **In-app completion moment:** haptic plus an in-app chime and a "Pour now" state for a few seconds before jumping to Steep Complete, independent of notification settings.

---

### Quick Timer

- **Purpose / verdict:** A no-form timer from the FAB that you can upgrade into a session by adding tea, vessel and parameters. If those are complete when it hits zero, it auto-creates an IN_PROGRESS session and hands off to Steep Complete. The idea is good for "just brewing", but it is a **second, weaker timer engine** that bypasses `TimerService`, so it misses every lifecycle guarantee the main timer has earned.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | lifecycle | `ui/screens/quicktimer/QuickTimerViewModel.kt:151-166,295-342` | The countdown runs in `viewModelScope`, not `TimerService`: no foreground service (`lifecycleHandler` is never called) and nothing is persisted. If Android kills the process in the background, the timer is lost; the back stack then restores `QuickTimerRoute`, and `Initialize` **auto-starts a brand-new full-length countdown**. | — |
| bug | lifecycle (unverified) | `ui/screens/quicktimer/QuickTimerScreen.kt:55-57`, `QuickTimerViewModel.kt:138-149,297` | `Initialize`, which auto-starts, runs on every new composition. A config change other than rotation (dark-mode switch, font scale, locale) recreates the Activity but keeps the ViewModel, so the effect runs again: the timer resets to full length, and `startCountdown` starts a second loop without cancelling the first (`timerJob` is simply overwritten). | The manifest only locks orientation. |
| bug | notifications | `QuickTimerViewModel.kt:314-318`, `and/domain/services/TimerNotificationService.kt:170-173,302-304` | `showTimerRunning` posts the **ongoing** (can't be swiped) notification with `notify()`. With no foreground service, nothing ever cancels it; `onTimerStopped` only cancels the alarm. The loop also posts it on the last tick with 0 remaining and status RUNNING, so the chronometer counts into negative numbers. Result on Android: after a Quick Timer finishes or is stopped, an undismissable "Your tea · Steep 1" notification stays until the next real Timer run. | Confirm on device. |
| bug | notifications (likely) | `QuickTimerViewModel.kt:321-331` | On completion it doesn't cancel the backup alarm (TimerService does, `TimerService.kt:140`). The AlarmManager backup fires at about the same moment and re-posts the completion notification, likely alerting twice. | Same notification id, no `setOnlyAlertOnce`. |
| bug | prefs | `QuickTimerViewModel.kt:160-164,189-193,217-221,328-331` | It schedules the alarm and posts the completion notification unconditionally, **ignoring the `timerCompletionNotificationsEnabled` setting**. It also never schedules the finish-your-session reminder, so an auto-saved quick-timer session gets no nudge. | — |
| bug | flow | `ui/components/quicktimer/QuickTimerDetailsSheet.kt:201` | The **"Change" button on the selected tea does nothing**. It calls `onTeaSearchQueryChanged("")`, which only clears the search text, so `selectedTea` stays and the card never switches back to search. | — |
| bug | errors | `QuickTimerViewModel.kt:540-580` | `autoSaveCompleted` silently gives up on a non-integer temperature or water value (`toIntOrNull … return@launch`) and ignores `createSessionUseCase` failures; there's no `onFailure`, and the catch comment says "user can save from the complete screen", which they never reach. The user sees COMPLETE with no prompt and no session, and Back then asks "Discard session?". | — |
| bug | layout (unverified) | `QuickTimerDetailsSheet.kt:284-482` | Step 2 of the details sheet has no `verticalScroll` inside the bottom sheet. Tea-type toggle, three fields, three preset rows, water type and buttons won't fit on small phones or with a large font, and "Done" becomes unreachable. | — |
| friction | purpose | `QuickTimerDetailsSheet.kt:95,426` | The water-quantity field is hidden whenever the vessel has a capacity, so a half-filled pot can't be recorded. A prefilled config volume that differs from capacity is saved without ever being shown. | — |
| friction | copy | `QuickTimerScreen.kt:141-151` | In the completion prompt, dismissing (tapping outside or Back) means "Add Details", while the primary button is the destructive "Discard". | — |
| friction | flow | `ui/components/quicktimer/QuickTimerDurationSheet.kt:48,90-94` | It always defaults to 2:00 and doesn't remember the last duration. A preset only fills the field, so "Start Timer" is still needed (two taps). The subtitle "without logging session details" contradicts the later auto-save. | — |
| friction | purpose | `QuickTimerDetailsSheet.kt:226-241` | The tea list is bare buttons in a 150dp scrolling list, with none of TeaPickerSheet's recents, favorites, type chips or quick-add. A tea that isn't in the collection yet can't be logged from Quick Timer. | — |
| consistency | data | `QuickTimerViewModel.kt:563,568` | The session is created at the end of the brew, so its `timestamp` is the finish time; Log Tea, Quick Brew and Brew Again stamp the start. It saves `brewingTime = originalDuration`, dropping adjustments (the same loss as Timer). | — |
| arch | dead code | `QuickTimerState` (`showNextSteepDialog`, `nextSteep*`, `rating`, `notes`, `isLoading`, `error`), `QuickTimerIntent.RatingChanged` / `NotesChanged`, `QuickTimerNavEvent.NavigateToTimer` (never sent) plus its handler at `AppNavigation.kt:458-462` | The class KDoc ("simple timer without a database session") is out of date. | — |

**Opportunities**
- **Run Quick Timer on `TimerService`**, one engine for both. It would inherit the foreground service, persistence, the settings toggles, notification cleanup and Live Activity handling. The blocker is that `TeaSession.teaId` is required: either allow a nullable-tea draft session, or persist a session-less `TimerState` in preferences.
- **Reuse `TeaPickerSheet`** in the details sheet (recents, favorites, quick-add).
- **"Recent durations" presets** stored in preferences, plus long-press on the FAB to start the last duration directly.
- **"Same again / +5s" after completion** for gongfu-by-feel users who never log anything.

---

### Steep Complete

- **Purpose / verdict:** The review after each steep: parameters (editable), photos, rating, notes and previous steeps, then Next Steep or Finish (optionally saving the brew as a method). The screen has the right shape. The ROADMAP says the author skips it for lack of payoff (2.1), and the code has other problems too. It writes a guessed rating the moment it opens, its per-field auto-saves overwrite each other, one transient error hides the whole form, and every session-completion rule lives in the ViewModel.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | data | `ui/screens/steepcomplete/SteepCompleteViewModel.kt:179-190` | **On open, an unrated steep gets `round(tea.averageRating)` written to the DB as its rating.** Skip the review and that guess is kept. It then feeds `UpdateTeaStatsUseCase` (the tea average, `:18-19`), which drives the next suggestion, so it reinforces itself. It also feeds the "best-rated session" step of `GetBrewingParametersPrefillUseCase`. | **Sharper framing for ROADMAP 2.1:** part of what the rating "does" today is echo the tea's own average back. |
| bug | data | `SteepCompleteViewModel.kt:228-249,252-282,537-563` | `updateRating`, `updateNotes`, `addPhoto`, `removePhoto` and `confirmEditParameters` each save `_state.value.session.copy(<one field>)`. `state.session` is only refreshed at initialize and after an edit, so **each save reverts the other fields**. Example: rate 4, then type a note, and the DB rating goes back to the suggested value or null. You see this after any re-initialize (a config change re-runs `LaunchedEffect(sessionId)`) or after leaving with Back. | Every keystroke also saves the whole row, which invalidates every `tea_session` Flow; Home's `GetDailyStats` rescans all sessions on each keystroke. |
| bug | errors | `ui/screens/steepcomplete/SteepCompleteScreen.kt:155-165`, VM `:264,351-354` | **Any error replaces the whole form** with a centered message. There is no ClearError intent and the bottom bar stays. Rating, notes and photos can't be reached until you leave. How to trigger it: `NextSteepParameterDialog` accepts over 100 °C (its check is only `temperature > 0.0`, `ui/components/timer/NextSteepParameterDialog.kt:48`), `AddSteepUseCase` rejects it, and the screen is stuck. | — |
| bug | notifications | `SteepCompleteViewModel.kt:434-474` | **Discard never calls `timerService.onSessionResolved`** (`DeleteSessionUseCase` and the Timer's discard both do). The "How was your X?" reminder therefore still fires for the deleted session. Tapping it reopens Steep Complete, and because `TeaSessionDao.getById` (`dat/local/database/dao/TeaSessionDao.kt:52`) doesn't filter out soft-deleted rows, the deleted session loads as reviewable. | — |
| bug | correctness | `SteepCompleteViewModel.kt:160-216,377-382` | `initialize` doesn't check the session's status. Reopening an already-COMPLETED session (for example via the leftover completion notification, see Timer) and pressing Finish **increments `timesUsed` a second time**, and Next Steep adds a steep to a finished session. | — |
| bug | purpose | `SteepCompleteViewModel.kt:400-402` | The "save as method" prompt requires `steepNumber == 1`, but steep 1 is completed through `confirmNextSteep`, which never prompts. **Multi-steep (gongfu) sessions are never offered "save method"**, even though they're the most tuned brews. | — |
| arch | CLAUDE.md | `SteepCompleteViewModel.kt:300-474` | `confirmNextSteep`, `finishSession` and `confirmDiscardSession` are multi-step business flows sitting in the ViewModel: complete, clear timer fields, count usage, recompute the average, complete the parent, stop the timer, withdraw the reminder, update tea stats, decide the method prompt. CLAUDE.md names "session completion flows, steep management" as use-case territory. `CompleteSessionUseCase` exists but isn't used here, and the two paths differ. None of these coroutines catches exceptions, so a Room failure in `finishSession` leaves the spinner up forever or crashes. | — |
| friction | state | `NextSteepParameterDialog.kt:112-147` | The temperature field is driven from a stored Double and ignores unparsable input (`:124`), so you can't clear it to retype; this is the pattern CLAUDE.md warns against. There's no water control, even though the ViewModel supports one (`NextSteepWaterQuantityChanged` is never dispatched). The next steep's time defaults to the previous steep's (VM `:201`), so every gongfu steep needs a manual +5s. | — |
| friction | flow | loop | Each extra steep costs Next Steep → dialog → Confirm → Timer → Start Brewing: about three taps and two screens, for 8–12 steeps per gongfu session. | — |
| friction | state | `SteepCompleteScreen.kt:177-183` | While Next or Finish is working, `isLoading` hides both the form and the bottom bar behind a full-screen spinner, so the bar's `enabled = !isLoading` never matters. `isLoading` also starts out `true`. | — |
| polish | copy | screen + `ui/components/session/RatingSelector.kt:42` | Nothing marks the pre-filled stars as a suggestion, and tapping the current star to clear it isn't discoverable. | — |
| a11y | a11y | `RatingSelector.kt:51` | All five stars have `contentDescription = "Star"`, with no selected state or value semantics. A screen-reader user can't tell or set the rating, and rating is the loop's central input. | — |
| polish | previews | `SteepCompleteScreen.kt:555,579` | The gongfu previews use 30, 40 and 45 **minutes**. The previous-steep chips show "5.0g". | — |
| ~~arch~~ | ~~dead code~~ | ~~`ui/components/session/CompleteDraftDialog.kt` (`CompleteInProgressDialog`, 201 lines)~~ | ~~Unused; only its own previews reference it.~~ | — |

**Opportunities** (beyond ROADMAP 2.1's best-brew comparison)
- **Stop guessing ratings.** Store null and show "Suggested ★4, tap to confirm". A skipped review then stays honest, and 2.1's comparison works on clean data.
- **A steep curve for the session.** Child steeps already hold each steep's time, temperature and rating, so Finish could show a small time-vs-rating chart. "Save as method" for multi-steep sessions could then save the whole schedule (this needs the schedule model from the Timer section).
- **One-tap "Same +5s" next steep** that skips the dialog. The increment can be derived from the last two steeps' times, which are already in the DB.
- **An "actual vs planned time" row,** once the Timer records elapsed time.
- **Tasting prompts or chips** (aroma, body, astringency) to feed Phase 3.3's richer notes.

---

### Navigation (AppNavigation.kt / NavRoute.kt / DeepLinkHandler.kt)

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| friction | state | `ui/navigation/AppNavigation.kt:167-170` | Switching tabs clears the stack, so each tab loses its ViewModel, scroll position and filters every time. There are no per-tab back stacks. | — |
| bug | flow | `AppNavigation.kt:143-154` | Notification taps push the route without checking the session's status, and only skip it if it's identical to the current top. Taps on notifications for finished or deleted sessions therefore open a live review or timer for them. | Connects Timer's leftover-notification finding and Steep Complete's discard finding. |
| arch | dead code | `NavRoute.kt:35-36`, `NavConfig.kt` | `MoreRoute` is serialized but has no screen, so it would crash if it were ever pushed or restored. `NavRoute.kt:44` is missing a trailing comma (CLAUDE.md). The QuickTimer → Timer handler (`:458-462`) is dead. | — |

---

## Cross-screen patterns

1. **Four different ways of handling errors, and none uses `SnackbarHost`** (screens outside the slice, such as EditTea, EditVessel and TeaTypeList, do):
   - Home and Log Tea show a hand-made `Snackbar` with the raw `e.message`.
   - Steep Complete replaces its whole content, with no way to recover.
   - Timer and Quick Timer set an error but never show it.
   - Some failures are silently swallowed: Quick Timer's auto-save, `HomeViewModel.quickBrew` when `getById` returns null, `UpdateTeaStatsUseCase`.

   Decide once which failures the user can act on (this is ROADMAP Phase 4's "user-facing error messages").
2. **Whole-row saves from stale ViewModel copies.** Timer (`StartTimerUseCase`, `CompleteTimerUseCase`, `confirmEditSession`) and Steep Complete (rating, notes, photos, edit) save `snapshot.copy(one field)`, which silently reverts other fields. Field-level DAO updates (`UPDATE … SET rating = ?`), or read-modify-write inside a use case, would remove the whole class of bug.
3. **The session-lifecycle rules live in six places that have drifted apart:** `CompleteSessionUseCase`, `SteepCompleteViewModel` (finish, next steep, discard), `TimerViewModel.confirmDiscardSession`, `DeleteSessionUseCase`, and `InProgressSessionDelegate`. Symptoms found:
   - the reminder isn't withdrawn after discarding from Steep Complete;
   - usage is counted twice when a finished session is re-finished;
   - "Complete & Start New" leaves timer fields in place, skips the average recompute, and erases the rating.

   A `FinishSteepUseCase` / `DiscardSessionUseCase` pair would absorb all of it.
4. **Two timer engines.** `TimerService` and `QuickTimerViewModel` differ on foreground service, persistence, settings toggles, the reminder, cancelling the alarm on completion, and cleaning up the ongoing notification.
5. **Ratings the user didn't give.** Four sources:
   - Steep Complete writes a guess from the tea's average as soon as it opens.
   - Quick Brew copies `config.rating` onto the new session.
   - Log Tea's "Log Session" dialog is pre-filled from `prefill.rating`.
   - `SaveConfigurationDialog` falls back to `session.rating ?: 5f` (`SteepCompleteScreen.kt:263`).

   Separately, `UpdateTeaStatsUseCase` averages the parent's `rating` while the prefill uses `averageRating ?: rating`, so a tea's average and its "best brew" disagree for multi-steep sessions. This matters directly to ROADMAP 2.1 and 2.2.
6. **Planned time vs brewed time.** No flow records how long the tea actually brewed (Timer adjust and Complete Now, Quick Timer's `originalDuration`).
7. **Timestamps mean different things.** Log Tea, Quick Brew and Brew Again stamp the start of the brew; Quick Timer's auto-save stamps the end.
8. **Units and formatting.**
   - Whole-number display conversions lose precision in every edit sheet (Timer, Steep Complete, Next Steep).
   - Leaf weight is rendered as `"${grams}g"`, giving "5.0g", in SessionCard, ChooseMethodDialog, PreviousSteepItem and `SteepParameterCard.kt:47`.
   - The reset dialogs use `Duration.toString()`.
9. **Copy.**
   - Actions: "Log Session" (with two different meanings), "Begin Session", "Start Brewing", "Start Timer", "Complete", "Finish Session", "Finish".
   - Destructive actions: "Cancel Session", "Delete", "Discard", "Discard & Start New".
   - ~~Section headers are typed as ALL-CAPS strings ("TODAY'S STATS", "QUICK BREW", "RECENT SESSIONS", "BREWING PARAMETERS", "OPTIONAL DETAILS", "PINNED", "BY USAGE"), while TeaPickerSheet uses `.uppercase()` and other screens use sentence case ("Previous Steeps", "Rate this steep").~~
   - "Used 1 times". Every string is hard-coded rather than in string resources.
10. **Hard-coded layout numbers and containers that don't scroll.** The 420.dp ring allowance is duplicated in Timer and Quick Timer; there's an 80.dp FAB padding and a 150.dp tea list. ~~EmptyHomeState,~~ Timer, Quick Timer, Quick Timer's step 2 and the long AlertDialogs (Next Steep, Choose Method) don't scroll. Android's portrait lock (`AndroidManifest.xml:37`) hides landscape problems on phones, but not on iOS or tablets.
11. **Duplicated preset tables** between `LogTeaSections` and `QuickTimerDetailsSheet`, with different brew-time presets in each.
12. **CLAUDE.md "public component has a `modifier` param" is missed** by ChooseMethodDialog, ManageQuickBrewSheet, QuickTimerDurationSheet and QuickTimerDetailsSheet.
13. **Dead code in this slice:**
    - ~~`RecentSessionsCard.kt` and `CompleteDraftDialog.kt` (about 370 lines together)~~
    - `HomeIntent.Refresh` and `isRefreshing`
    - Log Tea's photo intents, `hasUnsavedChanges`, and the route's `configurationId`
    - Quick Timer's dead fields and `NavigateToTimer` event
    - `NextSteepWaterQuantityChanged`
    - `NavRoute.MoreRoute`
    - `CompleteTimerUseCase`, which does almost nothing
14. **Accessibility.** RatingSelector, the Canvas ring, long-press-only actions, the chip whose tap does nothing, and the FAB labels. This is consistent with the ROADMAP's accepted risk, but RatingSelector semantics is a tiny fix with outsized value because rating is the loop's main input.
15. **ROADMAP corrections from this slice:**
    - 2.7 "FAB hidden" is stale; the FAB has been always visible since 558d194.
    - 2.7 "Choose Method lacks parameters" is stale; it shows parameter chips, and the vessel is implied because the list is already vessel-specific.
    - 2.5 needs the reframing above: the foreground signal exists, but only through notifications and their settings toggle.
