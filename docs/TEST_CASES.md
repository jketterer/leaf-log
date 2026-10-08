# Leaf Log - Manual E2E Test Cases

> **Last updated:** 2026-02-26
> **Reference:** [PRD.md](PRD.md)
>
> Each test case follows the format:
> **ID** | **Title** | **Preconditions** | **Steps** | **Expected Result**
>
> Severity labels: `P0` = critical flow, `P1` = important, `P2` = edge case / minor

---

## Table of Contents

1. [Home Screen](#1-home-screen)
2. [Log Tea (Session Creation)](#2-log-tea-session-creation)
3. [Timer](#3-timer)
4. [Quick Timer](#4-quick-timer)
5. [Multi-Steep Sessions](#5-multi-steep-sessions)
6. [Tea Collection](#6-tea-collection)
7. [Tea Detail](#7-tea-detail)
8. [Edit Tea](#8-edit-tea)
9. [Vessel Management](#9-vessel-management)
10. [Brewing Configurations](#10-brewing-configurations)
11. [Session History](#11-session-history)
12. [Session Detail](#12-session-detail)
13. [Edit Session](#13-edit-session)
14. [Analytics](#14-analytics)
15. [Settings](#15-settings)
16. [Data Export / Import](#16-data-export--import)
17. [Navigation & Deep Links](#17-navigation--deep-links)
18. [Cross-Cutting Concerns](#18-cross-cutting-concerns)

---

## 1. Home Screen

### Happy Path

#### TC-HOME-001 — Empty state on fresh install `P0`

**Preconditions:** Fresh app install, no data.

**Steps:**

1. Launch the app.

**Expected Result:**

- Home screen displays empty state with welcoming message.
- Daily stats section shows zeroes (0 sessions, 0 water, 0 teas).
- No recent sessions list visible.
- FAB is visible.

---

#### TC-HOME-002 — Daily stats display `P0`

**Preconditions:** At least 2 completed sessions today with different teas.

**Steps:**

1. Navigate to Home screen.

**Expected Result:**

- Session count matches today's completed sessions.
- Water consumed is formatted in the user's preferred unit.
- Unique teas count reflects distinct teas brewed today.

---

#### TC-HOME-003 — Recent sessions list `P0`

**Preconditions:** At least 3 completed sessions exist.

**Steps:**

1. Navigate to Home screen.
2. Observe the recent sessions section.

**Expected Result:**

- Recent sessions are displayed in reverse chronological order.
- Each card shows tea name, tea type color, rating (if set), and timestamp.
- "View All" link is visible.

---

#### TC-HOME-004 — Tap recent session navigates to detail `P1`

**Preconditions:** At least 1 completed session exists.

**Steps:**

1. On Home screen, tap a recent session card.

**Expected Result:**

- Navigates to Session Detail screen for that session.
- Back navigation returns to Home.

---

#### TC-HOME-005 — FAB expands with two options `P0`

**Preconditions:** Home screen is visible.

**Steps:**

1. Tap the FAB.

**Expected Result:**

- FAB expands to show two actions: "Log Tea" and "Quick Timer".
- Tapping outside the expanded FAB collapses it.

---

#### TC-HOME-006 — FAB → Log Tea `P0`

**Preconditions:** Home screen is visible.

**Steps:**

1. Tap the FAB.
2. Tap "Log Tea".

**Expected Result:**

- Navigates to the Log Tea screen.

---

#### TC-HOME-007 — FAB → Quick Timer shows duration sheet `P0`

**Preconditions:** Home screen is visible.

**Steps:**

1. Tap the FAB.
2. Tap "Quick Timer".

**Expected Result:**

- A bottom sheet appears with duration selection options.

---

#### TC-HOME-008 — In-progress sessions banner `P1`

**Preconditions:** At least 1 session with `status = IN_PROGRESS`.

**Steps:**

1. Navigate to Home screen.

**Expected Result:**

- An in-progress sessions banner is displayed showing the count.
- Tapping the banner navigates to the relevant in-progress session / history.

---

#### TC-HOME-009 — Resume in-progress session from Home `P1`

**Preconditions:** 1 in-progress session exists with an active timer.

**Steps:**

1. On Home screen, tap "Resume" on the in-progress banner.

**Expected Result:**

- Navigates to the Timer screen for that session.
- Timer state is restored (remaining time, pause state, etc.).

---

#### TC-HOME-010 — Greeting reflects time of day `P2`

**Preconditions:** None.

**Steps:**

1. Open the app at different times of day (morning, afternoon, evening).

**Expected Result:**

- Greeting text changes appropriately (e.g., "Good morning", "Good afternoon", "Good evening").

---

#### TC-HOME-011 — Daily stats cards are tappable `P1`

**Preconditions:** At least 1 completed session today.

**Steps:**

1. Tap the "sessions" daily stat card.
2. Navigate back to Home.
3. Tap the "water" daily stat card.
4. Navigate back to Home.
5. Tap the "teas" daily stat card.

**Expected Result:**

- Each tap navigates to the appropriate filtered view (History or Analytics).

---

#### TC-HOME-012 — Settings access from Home `P1`

**Preconditions:** Home screen is visible.

**Steps:**

1. Tap the settings icon in the greeting header.

**Expected Result:**

- Navigates to the Settings screen.

---

#### TC-HOME-013 — Pull-to-refresh `P1`

**Preconditions:** Home screen with data.

**Steps:**

1. Pull down on the Home screen to trigger refresh.

**Expected Result:**

- Refresh indicator appears.
- Data reloads and UI updates.

---

### Edge Cases

#### TC-HOME-014 — Delete session from recent sessions `P1`

**Preconditions:** At least 1 recent session displayed.

**Steps:**

1. On a recent session card, trigger the delete action.
2. Confirm deletion in the dialog.

**Expected Result:**

- Session is removed from the recent sessions list.
- Daily stats update accordingly.

---

#### TC-HOME-015 — Brew again from recent session `P1`

**Preconditions:** At least 1 completed session displayed.

**Steps:**

1. On a recent session card, trigger the "Brew Again" action.

**Expected Result:**

- Navigates to Log Tea screen.
- Tea, vessel, and brewing parameters are pre-filled from the original session.

---

#### TC-HOME-016 — Prevent session creation while one is in progress `P0`

**Preconditions:** 1 session is currently IN_PROGRESS.

**Steps:**

1. On Home screen, tap FAB → "Log Tea".

**Expected Result:**

- User is warned or prevented from creating a new session while one is in progress.
  (Verify the specific behavior — the app should not allow duplicate in-progress sessions.)

---

## 2. Log Tea (Session Creation)

### Happy Path

#### TC-LOG-001 — Full session creation with timer `P0`

**Preconditions:** At least 1 tea and 1 vessel exist.

**Steps:**

1. Navigate to Log Tea.
2. Tap tea selector → search and select a tea.
3. Select a vessel.
4. Enter water temperature (e.g., 85°C).
5. Enter water quantity (e.g., 200 mL).
6. Set brewing time (e.g., 3:00).
7. Enter tea quantity (e.g., 5g).
8. Select water type.
9. Tap "Start Timer".

**Expected Result:**

- An IN_PROGRESS session is created in the database.
- Navigates to the Timer screen with the correct duration.
- All parameters match what was entered.

---

#### TC-LOG-002 — Complete session without timer `P0`

**Preconditions:** At least 1 tea and 1 vessel exist.

**Steps:**

1. Navigate to Log Tea.
2. Select tea and vessel.
3. Fill in brewing parameters.
4. Tap "Save as Completed" (or equivalent).
5. In the completion dialog, set a rating (e.g., 4 stars) and add notes.
6. Confirm.

**Expected Result:**

- Session is saved with `status = COMPLETED`.
- Rating and notes are persisted.
- Returns to previous screen.
- Session appears in History and Home's recent sessions.

---

#### TC-LOG-003 — Tea search and selection `P1`

**Preconditions:** At least 5 teas exist with different names.

**Steps:**

1. On Log Tea screen, tap the tea selector.
2. Type a partial name in the search field.

**Expected Result:**

- Tea list filters in real-time to show matching results.
- Selecting a tea closes the dialog and populates the selection.

---

#### TC-LOG-004 — Quick-add tea inline `P1`

**Preconditions:** On the Log Tea screen.

**Steps:**

1. Tap the tea selector.
2. Tap "Quick Add" / create new tea option.
3. Enter a name and select a tea type.
4. Save.

**Expected Result:**

- New tea is created in the collection.
- Tea is automatically selected in the Log Tea form.
- Tea search dialog closes.

---

#### TC-LOG-005 — Pre-fill from brewing configuration `P0`

**Preconditions:** A tea with at least 1 saved brewing configuration exists.

**Steps:**

1. Navigate to Log Tea.
2. Select the tea that has saved configurations.
3. "Choose Method" dialog should appear.
4. Select a configuration.

**Expected Result:**

- All parameters (vessel, temperature, water qty, tea qty, brewing time, water type) are
  pre-filled from the configuration.
- Pre-fill source indicator is displayed.
- `usedConfigurationId` is set on the session.

---

#### TC-LOG-006 — Pre-fill from tea type defaults `P1`

**Preconditions:** A tea exists whose tea type has default temperature and brewing time set.

**Steps:**

1. Navigate to Log Tea.
2. Select the tea.
3. If no configuration exists, dismiss the method dialog or let it auto-fill.

**Expected Result:**

- Temperature and brewing time are pre-filled from the tea type defaults.
- Pre-fill source indicates "Tea Type" or equivalent.

---

#### TC-LOG-007 — Pre-fill from tea-specific defaults `P1`

**Preconditions:** A tea with `defaultTemperatureCelsius` and `defaultBrewingTime` set.

**Steps:**

1. Navigate to Log Tea.
2. Select that tea (no configurations exist for it).

**Expected Result:**

- Temperature and brewing time are pre-filled from the tea's own defaults.
- Tea-level defaults take priority over tea type defaults.

---

#### TC-LOG-008 — Pre-fill via route params (from Tea Detail) `P1`

**Preconditions:** A tea with a saved configuration exists.

**Steps:**

1. From Tea Detail screen, tap "Brew This Tea" with a specific configuration.

**Expected Result:**

- Navigates to Log Tea with `teaId`, `vesselId`, and/or `configurationId` pre-populated.
- Parameters are filled accordingly.

---

#### TC-LOG-009 — Add and remove photos `P2`

**Preconditions:** On Log Tea screen.

**Steps:**

1. Tap "Add Photo".
2. Select a photo from the device.
3. Verify photo appears in the form.
4. Remove the photo.

**Expected Result:**

- Photo is displayed after selection.
- Photo is removed from the form after removal.
- No orphaned files are left.

---

### Edge Cases / Validation

#### TC-LOG-010 — Submit with missing required fields `P0`

**Preconditions:** On Log Tea screen.

**Steps:**

1. Leave tea unselected.
2. Attempt to save or start timer.

**Expected Result:**

- Validation error is shown for the tea field.
- Session is not created.

---

#### TC-LOG-011 — Submit with missing vessel `P0`

**Preconditions:** On Log Tea screen, tea is selected.

**Steps:**

1. Do not select a vessel.
2. Attempt to save or start timer.

**Expected Result:**

- Validation error is shown for the vessel field.

---

#### TC-LOG-012 — Invalid temperature value `P1`

**Preconditions:** On Log Tea screen.

**Steps:**

1. Enter a non-numeric or out-of-range temperature (e.g., "abc" or "999").

**Expected Result:**

- Temperature error message is displayed.
- Save/Start Timer button is disabled while invalid.

---

#### TC-LOG-013 — Invalid water quantity `P1`

**Preconditions:** On Log Tea screen.

**Steps:**

1. Enter "0" or a negative number for water quantity.

**Expected Result:**

- Water quantity error is displayed.

---

#### TC-LOG-014 — Zero or negative brewing time `P1`

**Preconditions:** On Log Tea screen.

**Steps:**

1. Set brewing time to 0:00 or attempt to set a negative value.

**Expected Result:**

- Brewing time error is displayed.
- Save/Start Timer is blocked.

---

#### TC-LOG-015 — Unsaved changes → back navigation `P1`

**Preconditions:** On Log Tea screen with some fields filled.

**Steps:**

1. Fill in some parameters.
2. Press back / navigate away.

**Expected Result:**

- If changes have been made (`hasUnsavedChanges = true`), a discard confirmation dialog appears.
- Confirming discards changes and navigates back.
- Canceling stays on the form.

---

#### TC-LOG-016 — Choose different method `P2`

**Preconditions:** Tea selected, configuration pre-filled.

**Steps:**

1. Tap "Choose Different Method".
2. Select a different configuration (or "None").

**Expected Result:**

- Parameters update to the newly selected configuration.
- Selecting "None" clears the pre-filled values.

---

#### TC-LOG-017 — Toggle temperature unit mid-entry `P2`

**Preconditions:** On Log Tea screen, temperature entered in Celsius.

**Steps:**

1. Enter "85" in the temperature field (Celsius).
2. Toggle temperature unit to Fahrenheit.

**Expected Result:**

- Display value converts to the Fahrenheit equivalent (~185°F).
- Internal stored value remains in Celsius.

---

#### TC-LOG-018 — Toggle volume unit mid-entry `P2`

**Preconditions:** On Log Tea screen, water quantity entered in mL.

**Steps:**

1. Enter "200" in the water quantity field (mL).
2. Toggle volume unit to Fluid Ounces.

**Expected Result:**

- Display value converts to fl oz equivalent (~6-7 fl oz).
- Internal stored value remains in mL.

---

## 3. Timer

### Happy Path

#### TC-TIMER-001 — Timer starts and counts down `P0`

**Preconditions:** An IN_PROGRESS session exists (created via Log Tea → Start Timer).

**Steps:**

1. On the Timer screen, tap "Start".

**Expected Result:**

- Timer starts counting down from the set duration.
- Circular progress ring animates.
- Time displays in MM:SS format, counting down.

---

#### TC-TIMER-002 — Pause and resume timer `P0`

**Preconditions:** Timer is running.

**Steps:**

1. Tap "Pause".
2. Observe the timer.
3. Tap "Resume".

**Expected Result:**

- Timer pauses; remaining time freezes.
- On resume, countdown continues from where it paused.

---

#### TC-TIMER-003 — Timer completes naturally `P0`

**Preconditions:** Timer is running with a short duration (e.g., 5 seconds for testing).

**Steps:**

1. Start the timer and wait for it to reach 0:00.

**Expected Result:**

- Timer shows completion state.
- Rating selector and notes field appear.
- Option to add photos, start next steep, save configuration, or discard.

---

#### TC-TIMER-004 — Rate and save on completion `P0`

**Preconditions:** Timer has completed.

**Steps:**

1. Set a rating (e.g., 4.5).
2. Add notes (e.g., "Great floral aroma").
3. Tap "Save and Finish".

**Expected Result:**

- Session is saved as COMPLETED with the rating and notes.
- Tea's `averageRating` and `totalSessions` are updated.
- Navigates back (Home or previous screen).

---

#### TC-TIMER-005 — Quick adjust time while running `P1`

**Preconditions:** Timer is running.

**Steps:**

1. Tap the "+" quick adjust button to add time.
2. Tap the "-" quick adjust button to subtract time.

**Expected Result:**

- Remaining time increases/decreases by the adjustment increment.
- Timer continues running.

---

#### TC-TIMER-006 — Edit session parameters mid-brew `P1`

**Preconditions:** Timer is running or paused.

**Steps:**

1. Tap "Edit" to open the edit sheet.
2. Change temperature, water quantity, or water type.
3. Confirm edits.

**Expected Result:**

- Session parameters are updated.
- Timer continues uninterrupted.
- Session info display reflects the updated values.

---

#### TC-TIMER-007 — Session info displayed correctly `P1`

**Preconditions:** Timer screen loaded for a session.

**Steps:**

1. Observe the session info section.

**Expected Result:**

- Tea name, vessel name, temperature (in preferred unit), and water quantity (in preferred unit)
  are all displayed correctly.

---

### Edge Cases

#### TC-TIMER-008 — Stop timer early with confirmation `P1`

**Preconditions:** Timer is running.

**Steps:**

1. Tap "Stop".
2. A confirmation dialog appears.
3. Confirm stop.

**Expected Result:**

- Timer stops.
- Completion screen appears (same as natural completion).
- Remaining time at stop is not the final brew time — the elapsed time is used.

---

#### TC-TIMER-009 — Cancel stop confirmation `P2`

**Preconditions:** Timer is running, stop confirmation dialog is displayed.

**Steps:**

1. Tap "Cancel" on the stop confirmation.

**Expected Result:**

- Dialog dismisses.
- Timer continues running.

---

#### TC-TIMER-010 — Reset timer with confirmation `P1`

**Preconditions:** Timer is running or paused.

**Steps:**

1. Tap "Reset".
2. Confirm reset in dialog.

**Expected Result:**

- Timer resets to the original duration.
- Status returns to NOT_STARTED.

---

#### TC-TIMER-011 — Discard session `P1`

**Preconditions:** Timer has completed.

**Steps:**

1. On the completion screen, tap "Discard".
2. Confirm discard.

**Expected Result:**

- Session is deleted (not saved as completed).
- Navigates back to the previous screen.
- Session does not appear in History.

---

#### TC-TIMER-012 — Timer persists across app background `P0`

**Preconditions:** Timer is running.

**Steps:**

1. Background the app (press Home on device).
2. Wait 10-15 seconds.
3. Return to the app.

**Expected Result:**

- Timer screen is restored.
- Remaining time is accurate (accounts for elapsed wall-clock time while backgrounded).
- Timer state is correct (RUNNING if it was running, COMPLETE if time elapsed while
  backgrounded).

---

#### TC-TIMER-013 — Timer persists across process death `P0`

**Preconditions:** Timer is running.

**Steps:**

1. Background the app.
2. Force-kill the app process.
3. Relaunch the app.
4. Navigate to the in-progress session.

**Expected Result:**

- Timer state is restored from the database.
- Remaining time is accurate based on `timerStartedAt` and current time.

---

#### TC-TIMER-014 — Save configuration after completion `P1`

**Preconditions:** Timer has completed, session saved.

**Steps:**

1. On the "Save Configuration?" prompt, enter an optional label.
2. Tap "Save".

**Expected Result:**

- A new `BrewingConfiguration` is created with the session's parameters.
- Configuration appears in the tea's detail screen under "Saved Methods".

---

#### TC-TIMER-015 — Skip save configuration `P2`

**Preconditions:** Timer has completed, save configuration prompt is displayed.

**Steps:**

1. Tap "Skip" or dismiss the prompt.

**Expected Result:**

- No configuration is saved.
- Navigation proceeds normally.

---

## 4. Quick Timer

### Happy Path

#### TC-QUICK-001 — Start quick timer from duration sheet `P0`

**Preconditions:** Home screen visible.

**Steps:**

1. Tap FAB → "Quick Timer".
2. Select a duration (e.g., 3 minutes) from the bottom sheet.

**Expected Result:**

- Navigates to Quick Timer screen.
- Timer initializes with the selected duration.
- Timer auto-starts.

---

#### TC-QUICK-002 — Timer controls (pause, resume, adjust) `P0`

**Preconditions:** Quick Timer is running.

**Steps:**

1. Tap "Pause" → timer pauses.
2. Tap "Resume" → timer resumes.
3. Tap "+" to add time → remaining time increases.
4. Tap "-" to subtract time → remaining time decreases.

**Expected Result:**

- All controls function identically to the full Timer screen.

---

#### TC-QUICK-003 — Add details during timer `P1`

**Preconditions:** Quick Timer is running.

**Steps:**

1. Tap to open the details sheet.
2. Step 1: Select a tea and a vessel.
3. Tap "Next".
4. Step 2: Enter temperature, water quantity, tea quantity, water type.
5. Close the sheet.

**Expected Result:**

- Details are persisted in the Quick Timer state.
- `hasRequiredDetails` becomes true.

---

#### TC-QUICK-004 — Complete quick timer with details → session saved `P0`

**Preconditions:** Quick Timer completed, required details (tea, vessel, temp, water qty) filled
in.

**Steps:**

1. Timer completes.
2. Set a rating and notes in the completion dialog.
3. Tap "Save".

**Expected Result:**

- Session is saved as COMPLETED.
- Session appears in History with all the details entered.
- Tea stats are updated.

---

#### TC-QUICK-005 — Complete quick timer without details `P1`

**Preconditions:** Quick Timer completed, no details filled in.

**Steps:**

1. Timer completes.
2. Observe the completion state.

**Expected Result:**

- Since required details are missing, no session is auto-saved.
- User can still fill in details and save, or discard.

---

#### TC-QUICK-006 — Pre-fill in quick timer details `P2`

**Preconditions:** Quick Timer running, a tea with configurations exists.

**Steps:**

1. Open details sheet.
2. Select a tea that has saved configurations.

**Expected Result:**

- Brewing parameters are pre-filled from the configuration or tea defaults.
- Pre-fill source is displayed.

---

### Edge Cases

#### TC-QUICK-007 — Quick timer stop with confirmation `P1`

**Preconditions:** Quick Timer is running.

**Steps:**

1. Tap "Stop".
2. Confirm in the dialog.

**Expected Result:**

- Timer stops. Completion flow begins.

---

#### TC-QUICK-008 — Quick timer reset with confirmation `P1`

**Preconditions:** Quick Timer is running or paused.

**Steps:**

1. Tap "Reset".
2. Confirm in the dialog.

**Expected Result:**

- Timer resets to original duration.
- Any entered details are preserved.

---

#### TC-QUICK-009 — Quick timer back navigation `P2`

**Preconditions:** Quick Timer is running.

**Steps:**

1. Tap back.

**Expected Result:**

- If timer is running or paused, a discard confirmation appears.
- Confirming navigates back and discards the timer.

---

## 5. Multi-Steep Sessions

### Happy Path

#### TC-STEEP-001 — Start next steep from timer completion `P0`

**Preconditions:** Timer has just completed for a session (first steep).

**Steps:**

1. On completion, tap "Next Steep".
2. Adjust duration, temperature, and/or water quantity in the dialog.
3. Confirm.

**Expected Result:**

- A new child session is created with `parentSessionId` = original session's ID.
- `steepNumber` = 2.
- Timer starts for the new steep with the adjusted parameters.

---

#### TC-STEEP-002 — Multiple steeps accumulate correctly `P0`

**Preconditions:** A session with 2 completed steeps.

**Steps:**

1. Complete steep 3 via the next steep flow.
2. Navigate to Session Detail for the parent session.

**Expected Result:**

- All 3 steeps are listed in order (steep 1, 2, 3).
- Each steep shows its individual parameters and rating.
- Parent session's `averageRating` is the average of all steep ratings (truncated to 1 decimal).

---

#### TC-STEEP-003 — Next steep from Quick Timer `P1`

**Preconditions:** Quick Timer completed with required details filled in.

**Steps:**

1. On completion, tap "Continue to Next Steep".
2. Set parameters for the next steep.
3. Confirm.

**Expected Result:**

- Child session is created.
- New Quick Timer starts with the adjusted duration.

---

#### TC-STEEP-004 — Add steep from Session Detail `P1`

**Preconditions:** A completed parent session exists.

**Steps:**

1. Navigate to Session Detail.
2. Tap "Add Steep".
3. Set duration and temperature in the dialog.
4. Confirm.

**Expected Result:**

- New child steep is added with the correct `steepNumber`.
- Average rating is recalculated.
- Steep appears in the session detail list.

---

### Edge Cases

#### TC-STEEP-005 — Delete individual steep from multi-steep session `P1`

**Preconditions:** A session with 3 steeps exists.

**Steps:**

1. Navigate to Session Detail.
2. Delete steep 2.
3. Confirm deletion.

**Expected Result:**

- Steep 2 is removed.
- Remaining steeps are steep 1 and steep 3 (steep numbers may or may not renumber — verify
  behavior).
- Average rating is recalculated based on remaining steeps.

---

#### TC-STEEP-006 — Delete parent session deletes all steeps `P0`

**Preconditions:** A multi-steep session exists.

**Steps:**

1. Navigate to Session Detail for the parent.
2. Delete the entire session.
3. Confirm.

**Expected Result:**

- Parent session and all child steeps are deleted.
- None appear in History.
- Tea stats are updated.

---

#### TC-STEEP-007 — Average rating with some unrated steeps `P2`

**Preconditions:** A multi-steep session where steep 1 has a rating but steep 2 does not.

**Steps:**

1. Complete steep 2 without setting a rating.
2. Check the parent session's `averageRating`.

**Expected Result:**

- Average rating is calculated only from rated steeps (or verify the specific behavior — does it
  include 0s or exclude unrated?).

---

## 6. Tea Collection

### Happy Path

#### TC-COLL-001 — View teas tab with data `P0`

**Preconditions:** At least 3 teas exist across different types.

**Steps:**

1. Navigate to Collection screen (bottom tab).
2. Observe the Teas tab.

**Expected Result:**

- All teas are displayed.
- Each tea card shows name, type color indicator, origin (if set), rating, and session count.

---

#### TC-COLL-002 — Filter by All / Favorites / By Type `P0`

**Preconditions:** Some teas are marked as favorites; teas of multiple types exist.

**Steps:**

1. Tap "All" filter → all teas shown.
2. Tap "Favorites" filter → only favorited teas shown.
3. Tap "By Type" → select a specific type → only teas of that type shown.

**Expected Result:**

- Each filter correctly narrows the displayed list.
- Counts update to reflect filtered results.

---

#### TC-COLL-003 — Search teas `P1`

**Preconditions:** Multiple teas exist.

**Steps:**

1. Type a partial name in the search bar.

**Expected Result:**

- List filters in real-time to show matching teas.
- Clearing search restores the full (or filtered) list.

---

#### TC-COLL-004 — Sort teas `P1`

**Preconditions:** Multiple teas with different names, ratings, and session counts.

**Steps:**

1. Select sort option "Name (A-Z)" → verify alphabetical order.
2. Select "Rating" → verify highest-rated first.
3. Select "Times Brewed" → verify most-brewed first.
4. Select "Most Recent" → verify most-recently-brewed first.
5. Select "Oldest" → verify oldest-added first.

**Expected Result:**

- Each sort option correctly reorders the list.

---

#### TC-COLL-005 — Toggle favorite from collection `P1`

**Preconditions:** At least 1 tea exists.

**Steps:**

1. Tap the favorite toggle on a tea card.

**Expected Result:**

- Favorite state toggles (filled/unfilled heart or star).
- If "Favorites" filter is active, the tea appears/disappears accordingly.

---

#### TC-COLL-006 — Switch to Vessels tab `P1`

**Preconditions:** At least 1 vessel exists.

**Steps:**

1. Tap the "Vessels" tab in Collection.

**Expected Result:**

- Vessel list is displayed.
- Each card shows vessel name, icon, and capacity.

---

#### TC-COLL-007 — Add tea from collection `P1`

**Preconditions:** On Collection screen.

**Steps:**

1. Tap "Add Tea" button.

**Expected Result:**

- Navigates to Edit Tea screen in create mode.

---

### Edge Cases

#### TC-COLL-008 — Empty collection state `P2`

**Preconditions:** No teas exist (fresh install or all deleted).

**Steps:**

1. Navigate to Collection screen.

**Expected Result:**

- Empty state message is displayed.

---

#### TC-COLL-009 — Search with no results `P2`

**Preconditions:** Teas exist.

**Steps:**

1. Type a search query that matches no teas (e.g., "xyznonexistent").

**Expected Result:**

- Empty state or "No results" message is displayed.

---

#### TC-COLL-010 — Filter + search combined `P2`

**Preconditions:** Multiple tea types and names exist.

**Steps:**

1. Select "By Type" filter for a specific type.
2. Type a search query that only matches some teas of that type.

**Expected Result:**

- Only teas matching both the type filter AND the search query are shown.

---

## 7. Tea Detail

### Happy Path

#### TC-TDET-001 — View tea detail with full data `P0`

**Preconditions:** A tea exists with origin, producer, description, photos, default parameters,
recent sessions, and saved configurations.

**Steps:**

1. Navigate to Tea Detail for that tea.

**Expected Result:**

- All fields are displayed correctly: name, type, origin, producer, description.
- Photos are shown in a gallery.
- Default brewing parameters are visible.
- Saved configurations section lists all configurations.
- Recent sessions section shows recent sessions for this tea.

---

#### TC-TDET-002 — Brew this tea (no configuration) `P1`

**Preconditions:** Tea has no saved configurations.

**Steps:**

1. On Tea Detail, tap "Brew This Tea".

**Expected Result:**

- Navigates to Log Tea with the tea pre-selected.
- Parameters pre-filled from tea or tea type defaults.

---

#### TC-TDET-003 — Brew this tea (with configuration) `P1`

**Preconditions:** Tea has at least 1 saved configuration.

**Steps:**

1. On Tea Detail, tap a configuration's "Brew" action.

**Expected Result:**

- Navigates to Log Tea with tea, vessel, and all parameters pre-filled from the configuration.

---

#### TC-TDET-004 — Brew again from session in tea detail `P1`

**Preconditions:** Tea has at least 1 recent session displayed.

**Steps:**

1. On a session card in Tea Detail, tap "Brew Again".

**Expected Result:**

- Navigates to Log Tea pre-filled with that session's parameters.

---

#### TC-TDET-005 — Toggle favorite from detail `P1`

**Preconditions:** On Tea Detail screen.

**Steps:**

1. Tap the favorite toggle.

**Expected Result:**

- Favorite state toggles.
- Persisted to database.

---

#### TC-TDET-006 — Edit tea from detail `P1`

**Preconditions:** On Tea Detail screen.

**Steps:**

1. Tap "Edit".

**Expected Result:**

- Navigates to Edit Tea screen pre-populated with the tea's current data.

---

#### TC-TDET-007 — Delete tea `P1`

**Preconditions:** On Tea Detail screen.

**Steps:**

1. Tap "Delete".
2. Confirm in the dialog.

**Expected Result:**

- Tea is deleted.
- Navigates back to Collection.
- Tea no longer appears in the list.

---

#### TC-TDET-008 — View all sessions for tea `P2`

**Preconditions:** Tea has more sessions than shown in the "Recent Sessions" section.

**Steps:**

1. Tap "View All Sessions".

**Expected Result:**

- Navigates to History filtered by this tea.

---

### Edge Cases

#### TC-TDET-009 — Tea detail with no sessions `P2`

**Preconditions:** A tea with zero completed sessions.

**Steps:**

1. Navigate to Tea Detail.

**Expected Result:**

- Recent sessions section shows empty state or "No sessions yet" message.
- Rating and session count show appropriate defaults.

---

#### TC-TDET-010 — Delete session from tea detail `P2`

**Preconditions:** Tea has at least 1 recent session.

**Steps:**

1. Tap delete on a session card in Tea Detail.
2. Confirm.

**Expected Result:**

- Session is deleted.
- List updates.
- Tea stats (totalSessions, averageRating) are recalculated.

---

## 8. Edit Tea

### Happy Path

#### TC-ETEA-001 — Create new tea with required fields only `P0`

**Preconditions:** On Edit Tea screen in create mode.

**Steps:**

1. Enter a name (e.g., "Dragon Well").
2. Select a tea type (e.g., "Green").
3. Tap Save.

**Expected Result:**

- Tea is created and appears in the Collection.
- Optional fields are null/empty.

---

#### TC-ETEA-002 — Create tea with all fields `P1`

**Preconditions:** On Edit Tea screen in create mode.

**Steps:**

1. Enter name, select type.
2. Enter origin, producer, description.
3. Set purchase date.
4. Expand brewing params: set default temperature, brewing time, quantity.
5. Add photos.
6. Tap Save.

**Expected Result:**

- All fields are persisted.
- Tea Detail shows all entered data.

---

#### TC-ETEA-003 — Edit existing tea `P0`

**Preconditions:** An existing tea with data.

**Steps:**

1. Navigate to Edit Tea for that tea.
2. Change the name.
3. Change the origin.
4. Save.

**Expected Result:**

- Changes are persisted.
- Tea Detail reflects the updated values.
- Fields not modified retain their original values.

---

#### TC-ETEA-004 — Producer autocomplete `P2`

**Preconditions:** Multiple teas exist with different producers.

**Steps:**

1. On Edit Tea, start typing in the producer field.

**Expected Result:**

- Autocomplete suggestions appear from existing producers in the database.

---

### Edge Cases

#### TC-ETEA-005 — Save with empty name `P0`

**Preconditions:** On Edit Tea screen.

**Steps:**

1. Leave name blank.
2. Select a tea type.
3. Attempt to save.

**Expected Result:**

- Name validation error is shown.
- Save is blocked.

---

#### TC-ETEA-006 — Save with no tea type `P0`

**Preconditions:** On Edit Tea screen.

**Steps:**

1. Enter a name.
2. Do not select a tea type.
3. Attempt to save.

**Expected Result:**

- Tea type validation error is shown.
- Save is blocked.

---

#### TC-ETEA-007 — Unsaved changes discard dialog `P1`

**Preconditions:** On Edit Tea, some fields modified.

**Steps:**

1. Modify a field.
2. Press back.

**Expected Result:**

- Discard confirmation dialog appears.
- "Discard" navigates back without saving.
- "Cancel" stays on the form.

---

#### TC-ETEA-008 — No changes → no discard dialog `P2`

**Preconditions:** On Edit Tea for an existing tea, no modifications made.

**Steps:**

1. Open edit mode.
2. Press back without changing anything.

**Expected Result:**

- Navigates back immediately — no discard dialog.

---

#### TC-ETEA-009 — Invalid default temperature `P2`

**Preconditions:** On Edit Tea, expanding brewing params.

**Steps:**

1. Enter an out-of-range temperature (e.g., "999" or "abc").

**Expected Result:**

- Temperature validation error is displayed.

---

## 9. Vessel Management

### Happy Path

#### TC-VESS-001 — View vessel list `P1`

**Preconditions:** At least 2 vessels exist (including a system default).

**Steps:**

1. Navigate to Collection → Vessels tab.

**Expected Result:**

- All vessels are displayed with name, icon, and capacity.

---

#### TC-VESS-002 — View vessel detail `P1`

**Preconditions:** At least 1 vessel exists.

**Steps:**

1. Tap a vessel card.

**Expected Result:**

- Vessel Detail screen shows name, icon, capacity (in preferred unit), and session count.

---

#### TC-VESS-003 — Create new vessel `P0`

**Preconditions:** On Vessel List or Collection screen.

**Steps:**

1. Tap "Add Vessel".
2. Enter a name (e.g., "Clay Gaiwan").
3. Select an icon.
4. Enter capacity (e.g., "150 mL").
5. Save.

**Expected Result:**

- Vessel is created and appears in the list.
- Vessel is available for selection in Log Tea.

---

#### TC-VESS-004 — Edit existing vessel `P1`

**Preconditions:** A non-system-default vessel exists.

**Steps:**

1. Navigate to Vessel Detail → Edit.
2. Change the name.
3. Save.

**Expected Result:**

- Vessel name is updated.
- Sessions referencing this vessel reflect the new name.

---

#### TC-VESS-005 — Delete vessel `P1`

**Preconditions:** A vessel with 0 sessions and at least 2 total vessels exist.

**Steps:**

1. Navigate to Vessel Detail.
2. Tap Delete.
3. Confirm.

**Expected Result:**

- Vessel is deleted and removed from the list.

---

### Edge Cases

#### TC-VESS-006 — Cannot delete last vessel `P0`

**Preconditions:** Only 1 vessel exists.

**Steps:**

1. Navigate to Vessel Detail.
2. Attempt to delete.

**Expected Result:**

- Delete is disabled (`canDelete = false` when `totalVesselCount <= 1`).
- User cannot delete the only remaining vessel.

---

#### TC-VESS-007 — Cannot delete vessel with sessions `P0`

**Preconditions:** A vessel that has been used in at least 1 session.

**Steps:**

1. Navigate to Vessel Detail.
2. Attempt to delete.

**Expected Result:**

- Delete is disabled (`canDelete = false` when `sessionCount > 0`).

---

#### TC-VESS-008 — Create vessel with empty name `P1`

**Preconditions:** On Edit Vessel in create mode.

**Steps:**

1. Leave name blank.
2. Select an icon.
3. Attempt to save.

**Expected Result:**

- Name validation error is shown.
- Save is blocked.

---

#### TC-VESS-009 — Vessel unsaved changes dialog `P2`

**Preconditions:** On Edit Vessel with changes made.

**Steps:**

1. Change the name.
2. Press back.

**Expected Result:**

- Discard confirmation dialog appears.

---

#### TC-VESS-010 — Capacity in preferred unit `P2`

**Preconditions:** User preferences set to Fluid Ounces.

**Steps:**

1. Create a vessel with capacity "5 fl oz".
2. View vessel detail.

**Expected Result:**

- Capacity is displayed in fl oz.
- Internally stored as mL.

---

## 10. Brewing Configurations

### Happy Path

#### TC-CONF-001 — Create configuration manually from Tea Detail `P0`

**Preconditions:** A tea exists.

**Steps:**

1. Navigate to Tea Detail.
2. Tap "Add Brewing Method" / "Add Configuration".
3. Select a vessel.
4. Enter label, temperature, water quantity, brewing time, water type.
5. Optionally enter tea quantity.
6. Save.

**Expected Result:**

- Configuration is saved and appears in Tea Detail's "Saved Methods" section.
- Label, vessel, and parameters match input.

---

#### TC-CONF-002 — Create configuration from Timer completion `P1`

**Preconditions:** Complete a session via Timer for a tea that has no matching configuration.

**Steps:**

1. On the "Save Configuration?" prompt after completion, enter an optional label.
2. Tap "Save".

**Expected Result:**

- Configuration is created with the session's parameters.
- `sourceSessionId` references the just-completed session.
- `rating` is set from the session's rating.

---

#### TC-CONF-003 — Edit existing configuration `P1`

**Preconditions:** A configuration exists for a tea.

**Steps:**

1. On Tea Detail, tap "Edit" on a configuration.
2. Change the brewing time.
3. Save.

**Expected Result:**

- Configuration is updated.
- Changes reflected in Tea Detail.

---

#### TC-CONF-004 — Delete configuration `P1`

**Preconditions:** A configuration exists for a tea.

**Steps:**

1. On Tea Detail, tap "Delete" on a configuration.

**Expected Result:**

- Configuration is removed.
- Does not affect any past sessions that used it.

---

#### TC-CONF-005 — Use configuration to pre-fill Log Tea `P0`

**Preconditions:** Tea has a saved configuration.

**Steps:**

1. Navigate to Log Tea.
2. Select the tea → "Choose Method" dialog appears.
3. Select the configuration.

**Expected Result:**

- Vessel, temperature, water quantity, tea quantity, brewing time, and water type are all
  pre-filled.
- `usedConfigurationId` is tracked on the session.

---

### Edge Cases

#### TC-CONF-006 — Configuration with deleted vessel `P2`

**Preconditions:** A configuration references a vessel that has since been deleted.

**Steps:**

1. Create a configuration using Vessel A.
2. Delete Vessel A (if allowed).
3. Attempt to use the configuration.

**Expected Result:**

- Graceful handling — configuration should either be invalid/hidden or prompt the user to select
  a different vessel.

---

#### TC-CONF-007 — Save without required fields `P1`

**Preconditions:** On Create Configuration screen.

**Steps:**

1. Leave vessel unselected.
2. Leave brewing time empty.
3. Attempt to save.

**Expected Result:**

- Save button is disabled (`canSave = false`).

---

## 11. Session History

### Happy Path

#### TC-HIST-001 — View history with sessions `P0`

**Preconditions:** At least 5 completed sessions across multiple days.

**Steps:**

1. Navigate to History (bottom tab).

**Expected Result:**

- Sessions are grouped by date.
- Each session shows tea name, type color, vessel, brewing params, rating, and timestamp.
- Sessions within each date group are in reverse chronological order.

---

#### TC-HIST-002 — Filter by tea type `P1`

**Preconditions:** Sessions exist for multiple tea types.

**Steps:**

1. Tap the filter icon → filter sheet opens.
2. Select a specific tea type.
3. Apply.

**Expected Result:**

- Only sessions with the selected tea type are shown.
- Filter badge shows "1" active filter.

---

#### TC-HIST-003 — Filter by specific tea `P1`

**Preconditions:** Multiple teas have sessions.

**Steps:**

1. Open filter sheet.
2. Select a specific tea.
3. Apply.

**Expected Result:**

- Only sessions for that tea are shown.

---

#### TC-HIST-004 — Filter by date range `P1`

**Preconditions:** Sessions span multiple weeks.

**Steps:**

1. Open filter sheet.
2. Set a start and end date.
3. Apply.

**Expected Result:**

- Only sessions within the date range are shown.

---

#### TC-HIST-005 — Filter by minimum rating `P1`

**Preconditions:** Sessions with various ratings exist.

**Steps:**

1. Open filter sheet.
2. Set minimum rating (e.g., 4.0).
3. Apply.

**Expected Result:**

- Only sessions with rating >= 4.0 are shown.
- Unrated sessions are excluded.

---

#### TC-HIST-006 — Clear all filters `P1`

**Preconditions:** One or more filters are active.

**Steps:**

1. Tap "Clear Filters".

**Expected Result:**

- All filters reset.
- Full session list is restored.
- Filter badge disappears.

---

#### TC-HIST-007 — Search sessions `P1`

**Preconditions:** Sessions exist.

**Steps:**

1. Type a query in the search bar (e.g., a tea name).

**Expected Result:**

- Sessions matching the query are shown.
- Non-matching sessions are hidden.

---

#### TC-HIST-008 — Tap session → detail `P0`

**Preconditions:** Sessions exist.

**Steps:**

1. Tap a session card.

**Expected Result:**

- Navigates to Session Detail for that session.

---

#### TC-HIST-009 — Brew again from history `P1`

**Preconditions:** A completed session exists.

**Steps:**

1. Trigger "Brew Again" on a session in History.

**Expected Result:**

- Navigates to Log Tea with parameters pre-filled.

---

#### TC-HIST-010 — Delete session from history `P1`

**Preconditions:** A completed session exists.

**Steps:**

1. Trigger "Delete" on a session.
2. Confirm.

**Expected Result:**

- Session removed from history list.
- Tea stats updated.

---

### Edge Cases

#### TC-HIST-011 — Empty history `P2`

**Preconditions:** No sessions exist.

**Steps:**

1. Navigate to History.

**Expected Result:**

- Empty state message is displayed.

---

#### TC-HIST-012 — Multiple filters combined `P2`

**Preconditions:** Various sessions exist.

**Steps:**

1. Set tea type filter + date range + minimum rating simultaneously.

**Expected Result:**

- Only sessions matching ALL criteria are shown.
- Active filter count reflects all applied filters.

---

#### TC-HIST-013 — Filter results in empty list `P2`

**Preconditions:** Sessions exist but no sessions match a specific filter combination.

**Steps:**

1. Apply filters that match zero sessions.

**Expected Result:**

- Empty state is displayed.
- Filters remain active (not auto-cleared).

---

#### TC-HIST-014 — In-progress filter `P1`

**Preconditions:** At least 1 IN_PROGRESS session exists.

**Steps:**

1. Open filter sheet.
2. Toggle "In-Progress Only".
3. Apply.

**Expected Result:**

- Only IN_PROGRESS sessions are shown.

---

#### TC-HIST-015 — Complete in-progress session from history `P1`

**Preconditions:** IN_PROGRESS session visible in history.

**Steps:**

1. Tap "Complete" (or equivalent) on the in-progress session.

**Expected Result:**

- Navigates to the Timer screen to resume/complete the session.

---

## 12. Session Detail

### Happy Path

#### TC-SDET-001 — View single-steep session detail `P0`

**Preconditions:** A completed session with 1 steep, notes, rating, and photos.

**Steps:**

1. Navigate to Session Detail.

**Expected Result:**

- Tea name, type, and vessel are displayed.
- All brewing parameters shown (temperature, water qty, tea qty, brewing time, water type).
- Rating displayed.
- Notes displayed.
- Photos shown in gallery.
- Timestamp displayed.

---

#### TC-SDET-002 — View multi-steep session detail `P0`

**Preconditions:** A completed session with 3 steeps.

**Steps:**

1. Navigate to Session Detail.

**Expected Result:**

- Parent session header with average rating.
- All 3 steeps listed in order with individual parameters and ratings.
- Each steep is distinguishable (steep 1, 2, 3).

---

#### TC-SDET-003 — Edit session from detail `P1`

**Preconditions:** On Session Detail.

**Steps:**

1. Tap "Edit".

**Expected Result:**

- Navigates to Edit Session screen.

---

#### TC-SDET-004 — Edit individual steep `P1`

**Preconditions:** Multi-steep session, on Session Detail.

**Steps:**

1. Tap "Edit" on a specific steep.

**Expected Result:**

- Navigates to Edit Session for that individual steep (not the full session).

---

#### TC-SDET-005 — Delete session from detail `P1`

**Preconditions:** On Session Detail.

**Steps:**

1. Tap "Delete".
2. Confirm.

**Expected Result:**

- Session (and all child steeps if multi-steep) deleted.
- Navigates back.

---

#### TC-SDET-006 — Brew again from detail `P1`

**Preconditions:** On Session Detail.

**Steps:**

1. Tap "Brew Again".

**Expected Result:**

- Navigates to Log Tea with parameters pre-filled from this session.

---

#### TC-SDET-007 — View tea from session detail `P2`

**Preconditions:** On Session Detail.

**Steps:**

1. Tap the tea name / "View Tea" link.

**Expected Result:**

- Navigates to Tea Detail for that tea.

---

### Edge Cases

#### TC-SDET-008 — Session with no rating or notes `P2`

**Preconditions:** A completed session with no rating and no notes.

**Steps:**

1. Navigate to Session Detail.

**Expected Result:**

- Rating section shows empty/unrated state.
- Notes section is absent or shows "No notes".

---

#### TC-SDET-009 — Fullscreen photo viewer `P2`

**Preconditions:** Session with photos.

**Steps:**

1. Tap a photo in the gallery.

**Expected Result:**

- Fullscreen image viewer opens.
- User can dismiss to return to Session Detail.

---

## 13. Edit Session

### Happy Path

#### TC-ESESS-001 — Edit steep parameters `P0`

**Preconditions:** A completed session with at least 1 steep.

**Steps:**

1. Navigate to Edit Session for a specific steep.
2. Change brewing time, temperature, and rating.
3. Save.

**Expected Result:**

- Changes are persisted.
- Session Detail reflects updated values.
- If rating changed and multi-steep, parent's `averageRating` is recalculated.

---

#### TC-ESESS-002 — Edit full parent session `P1`

**Preconditions:** A completed parent session.

**Steps:**

1. Navigate to Edit Session with `editFullSession = true`.
2. Change vessel, water type, water quantity, tea quantity, location.
3. Save.

**Expected Result:**

- Parent session fields updated.
- Session Detail reflects changes.

---

#### TC-ESESS-003 — Save configuration after edit `P2`

**Preconditions:** Editing a session.

**Steps:**

1. Make changes and save.
2. If "Save Configuration?" prompt appears, save or skip.

**Expected Result:**

- If saved, a configuration is created/updated with the edited parameters.

---

### Edge Cases

#### TC-ESESS-004 — Edit with invalid values `P1`

**Preconditions:** On Edit Session.

**Steps:**

1. Clear the temperature field.
2. Set water quantity to "0".
3. Attempt to save.

**Expected Result:**

- Validation errors shown.
- Save is blocked.

---

#### TC-ESESS-005 — Unsaved changes discard `P1`

**Preconditions:** On Edit Session, changes made.

**Steps:**

1. Modify a field.
2. Press back.

**Expected Result:**

- Discard confirmation dialog appears.

---

#### TC-ESESS-006 — No changes → no discard `P2`

**Preconditions:** On Edit Session, no modifications.

**Steps:**

1. Open edit mode.
2. Press back without changes.

**Expected Result:**

- Navigates back immediately.

---

## 14. Analytics

### Happy Path

#### TC-ANLY-001 — Analytics with sufficient data `P0`

**Preconditions:** At least 10 completed sessions across multiple days, teas, and types.

**Steps:**

1. Navigate to Analytics (bottom tab).

**Expected Result:**

- Summary card displays: total sessions, total brewing time, total water, unique teas, average
  rating.
- All chart sections are visible and populated:
    - Brewing Trends (line chart)
    - Tea Type Distribution (pie/donut)
    - Top Teas (bar chart)
    - Top Rated Teas (bar chart)
    - Vessel Distribution (chart)
    - Brewing Activity Heatmap (grid)
- Steep Insights section shows averages and discoveries.
- Insights section shows rule-based observations.

---

#### TC-ANLY-002 — Change analytics period `P0`

**Preconditions:** Analytics screen visible.

**Steps:**

1. Tap the period selector.
2. Select "This Month".
3. Observe data updates.
4. Select "All Time".
5. Observe data updates.

**Expected Result:**

- Data recalculates for each selected period.
- Summary numbers, charts, and insights all reflect the new period.
- Period label updates.

---

#### TC-ANLY-003 — Custom date range `P1`

**Preconditions:** Analytics screen visible.

**Steps:**

1. Tap period selector.
2. Select "Custom Range".
3. Set start and end dates.

**Expected Result:**

- Analytics filtered to the custom range.
- Period label shows the custom range.

---

#### TC-ANLY-004 — Tap chart element navigates `P1`

**Preconditions:** Analytics with data.

**Steps:**

1. Tap a tea type segment in the distribution chart.
2. Navigate back.
3. Tap a tea in the top teas chart.
4. Navigate back.
5. Tap a trend point in the brewing trends chart.

**Expected Result:**

- Tea type tap → navigates to History filtered by that tea type.
- Top tea tap → navigates to Tea Detail.
- Trend point tap → navigates to History filtered by that date.
- Vessel tap → navigates to Vessel Detail.
- Top rated tea tap → navigates to Tea Detail.

---

#### TC-ANLY-005 — Insights display `P1`

**Preconditions:** Sufficient session data for insights to be generated.

**Steps:**

1. Scroll to the Insights section.

**Expected Result:**

- At least one insight is displayed (e.g., "Most brewed: Dragon Well", "Your average rating is
  4.2").
- Insight types include: MOST_BREWED, AVERAGE_RATING, CONSISTENCY, FAVORITE_TYPE, RE_STEEP,
  TOP_RATED, PREFERRED_VESSEL (where applicable).

---

### Edge Cases

#### TC-ANLY-006 — Analytics with no data `P1`

**Preconditions:** No completed sessions exist.

**Steps:**

1. Navigate to Analytics.

**Expected Result:**

- `hasMinimumData = false` state is shown.
- Empty/placeholder state with encouragement to brew tea.
- Navigate to Log Tea action available.

---

#### TC-ANLY-007 — Analytics with 1 session `P2`

**Preconditions:** Exactly 1 completed session.

**Steps:**

1. Navigate to Analytics.

**Expected Result:**

- Summary card shows data for 1 session.
- Charts display with single data points (no crashes or empty charts).
- Trends chart shows 1 point.

---

#### TC-ANLY-008 — Period with no sessions `P2`

**Preconditions:** Sessions exist in "All Time" but none in "This Week".

**Steps:**

1. Select "This Week" period.

**Expected Result:**

- Summary shows zeroes.
- Charts show empty/no-data state.
- Switching to "All Time" restores the data.

---

#### TC-ANLY-009 — Water quantity in preferred unit `P2`

**Preconditions:** User preference set to Fluid Ounces. Sessions exist.

**Steps:**

1. Navigate to Analytics.

**Expected Result:**

- Total water consumed is displayed in fl oz (not mL).

---

## 15. Settings

### Happy Path

#### TC-SETT-001 — Change temperature unit `P0`

**Preconditions:** Default is Celsius.

**Steps:**

1. Navigate to Settings.
2. Toggle temperature unit to Fahrenheit.
3. Navigate to Log Tea.

**Expected Result:**

- Temperature unit shows "Fahrenheit" in Settings.
- Log Tea screen shows temperature input in °F.
- Existing data displayed in Fahrenheit where applicable.

---

#### TC-SETT-002 — Change volume unit `P0`

**Preconditions:** Default is Milliliters.

**Steps:**

1. Navigate to Settings.
2. Toggle volume unit to Fluid Ounces.
3. Navigate to Log Tea.

**Expected Result:**

- Volume unit shows "Fluid Ounces" in Settings.
- Water quantity input shows fl oz.
- Vessel capacity displayed in fl oz.

---

#### TC-SETT-003 — Navigate to Tea Type management `P1`

**Preconditions:** On Settings screen.

**Steps:**

1. Tap "Manage Tea Types" (or equivalent).

**Expected Result:**

- Navigates to Tea Type List screen.
- All tea types (system + custom) are shown.

---

### Tea Type Management

#### TC-SETT-004 — View tea types `P1`

**Preconditions:** System defaults exist.

**Steps:**

1. Navigate to Settings → Tea Types.

**Expected Result:**

- System default types are listed (e.g., Green, Black, Oolong).
- Custom types (if any) are shown.

---

#### TC-SETT-005 — Create custom tea type `P1`

**Preconditions:** On Tea Type List.

**Steps:**

1. Tap "Add".
2. Enter name (e.g., "Purple Tea").
3. Set default temperature (e.g., 90°C).
4. Set default brewing time (e.g., 2:30).
5. Choose a color.
6. Save.

**Expected Result:**

- New tea type appears in the list.
- Available when creating/editing teas.

---

#### TC-SETT-006 — Edit custom tea type `P1`

**Preconditions:** A custom tea type exists.

**Steps:**

1. Tap edit on the custom type.
2. Change the name and color.
3. Save.

**Expected Result:**

- Type is updated.
- Teas of this type reflect the new color in the UI.

---

#### TC-SETT-007 — Cannot delete system default tea type `P1`

**Preconditions:** On Tea Type List.

**Steps:**

1. Attempt to delete a system default type (e.g., "Green").

**Expected Result:**

- Delete action is disabled or not available for system defaults.

---

#### TC-SETT-008 — Delete custom tea type `P2`

**Preconditions:** A custom tea type with no teas assigned.

**Steps:**

1. Delete the custom type.

**Expected Result:**

- Type is removed from the list.
- No longer available in type selectors.

---

## 16. Data Export / Import

### Happy Path

#### TC-DATA-001 — Export all data `P0`

**Preconditions:** App has teas, sessions, vessels, configurations, and custom tea types.

**Steps:**

1. Navigate to Settings.
2. Tap "Export Data".
3. Wait for export to complete.

**Expected Result:**

- A file is generated containing all data.
- `exportFilePath` is set and the file is accessible.
- No data is lost or corrupted.

---

#### TC-DATA-002 — Import data `P0`

**Preconditions:** An exported data file is available. App may or may not have existing data.

**Steps:**

1. Navigate to Settings.
2. Tap "Import Data".
3. Select the exported file.
4. Wait for import.

**Expected Result:**

- Import result summary is shown with counts:
    - Tea types imported
    - Brewing vessels imported
    - Teas imported
    - Tea sessions imported
    - Configurations imported
- Imported data appears throughout the app.

---

#### TC-DATA-003 — Import into fresh install `P1`

**Preconditions:** Fresh app install, no data. An exported file is available.

**Steps:**

1. Import the file.

**Expected Result:**

- All data is restored.
- Home screen shows sessions, stats, etc.
- Collection shows all teas and vessels.

---

### Edge Cases

#### TC-DATA-004 — Cancel import `P2`

**Preconditions:** On Settings, import file picker is open.

**Steps:**

1. Tap "Import Data".
2. Cancel the file picker without selecting a file.

**Expected Result:**

- Import is cancelled.
- No data is modified.
- No error is shown.

---

#### TC-DATA-005 — Import invalid file `P2`

**Preconditions:** A non-export file (e.g., a random text file).

**Steps:**

1. Tap "Import Data".
2. Select the invalid file.

**Expected Result:**

- Error message is displayed.
- No data is modified or corrupted.

---

#### TC-DATA-006 — Export with no data `P2`

**Preconditions:** Fresh install, no data.

**Steps:**

1. Tap "Export Data".

**Expected Result:**

- Either exports an empty file gracefully or shows a "nothing to export" message.

---

## 17. Navigation & Deep Links

#### TC-NAV-001 — Bottom tab navigation `P0`

**Preconditions:** App has data.

**Steps:**

1. Tap Home tab → verify Home screen.
2. Tap Collection tab → verify Collection screen.
3. Tap History tab → verify History screen.
4. Tap Analytics tab → verify Analytics screen.

**Expected Result:**

- Each tab navigates to the correct screen.
- Selected tab is visually highlighted.
- Data loads correctly on each screen.

---

#### TC-NAV-002 — Tab state preserved on switch `P1`

**Preconditions:** App has data.

**Steps:**

1. Navigate to Collection → apply a search filter.
2. Switch to History tab.
3. Switch back to Collection.

**Expected Result:**

- Collection screen retains the search filter (or reloads to default — verify expected
  behavior).

---

#### TC-NAV-003 — Deep back-stack navigation `P1`

**Preconditions:** App has data.

**Steps:**

1. Home → Log Tea → Timer → complete → Save Configuration.
2. Press back repeatedly.

**Expected Result:**

- Back navigation returns through the correct stack without crashes.
- Eventually returns to Home.

---

#### TC-NAV-004 — Deep link from notification `P1`

**Preconditions:** A timer notification fires (when implemented).

**Steps:**

1. Start a timer.
2. Background the app.
3. Wait for timer to complete.
4. Tap the notification.

**Expected Result:**

- App opens directly to the Timer screen for the completed session.
- DeepLinkHandler routes correctly and is consumed after navigation.

---

#### TC-NAV-005 — History with route filter params `P1`

**Preconditions:** Sessions for multiple tea types exist.

**Steps:**

1. From Analytics, tap a tea type distribution segment.

**Expected Result:**

- Navigates to History with `filterTeaTypeId` pre-set.
- History is pre-filtered to that tea type.

---

## 18. Cross-Cutting Concerns

### Unit Preferences

#### TC-CROSS-001 — Temperature unit consistency `P0`

**Preconditions:** Temperature unit set to Fahrenheit.

**Steps:**

1. Navigate through: Log Tea, Timer, Session Detail, Edit Session, Analytics, Vessel Detail.

**Expected Result:**

- All temperature values throughout the app display in Fahrenheit.
- No screen shows Celsius when preference is Fahrenheit.

---

#### TC-CROSS-002 — Volume unit consistency `P0`

**Preconditions:** Volume unit set to Fluid Ounces.

**Steps:**

1. Navigate through: Log Tea, Timer, Session Detail, Edit Session, Analytics, Vessel Detail.

**Expected Result:**

- All volume values throughout the app display in Fluid Ounces.
- No screen shows mL when preference is fl oz.

---

### Data Integrity

#### TC-CROSS-003 — Tea deletion cascading `P1`

**Preconditions:** A tea with sessions and configurations.

**Steps:**

1. Delete the tea.
2. Check History for its sessions.
3. Check that configurations are cleaned up.

**Expected Result:**

- Tea is deleted.
- Verify cascading behavior (sessions may remain orphaned or be deleted — verify expected
  behavior and document it).

---

#### TC-CROSS-004 — Tea stats update after session changes `P0`

**Preconditions:** A tea with 3 completed sessions, each rated.

**Steps:**

1. Delete 1 session.
2. Check Tea Detail.

**Expected Result:**

- `totalSessions` decremented by 1.
- `averageRating` recalculated.
- `lastBrewedAt` updated if the most recent session was deleted.

---

#### TC-CROSS-005 — Average rating recalculation `P1`

**Preconditions:** A multi-steep session with 3 rated steeps (ratings: 3, 4, 5).

**Steps:**

1. Verify parent session's `averageRating` = 4.0.
2. Edit steep 3's rating from 5 to 2.
3. Verify `averageRating` = 3.0.

**Expected Result:**

- Average rating is (3 + 4 + 2) / 3 = 3.0 (truncated to 1 decimal).

---

### Empty States

#### TC-CROSS-006 — All screens handle empty state `P2`

**Preconditions:** Fresh install, no user data.

**Steps:**

1. Navigate through all tabs and screens:
    - Home, Collection (Teas), Collection (Vessels), History, Analytics.

**Expected Result:**

- Each screen displays an appropriate empty state.
- No crashes or blank screens.
- Calls to action are visible (e.g., "Log your first session").

---

### Error Handling

#### TC-CROSS-007 — Network/database error recovery `P2`

**Preconditions:** Simulate an error condition (e.g., corrupt database state).

**Steps:**

1. Trigger an error.
2. Observe the error state.
3. Dismiss the error and retry.

**Expected Result:**

- Error message is displayed to the user.
- Error can be cleared (`ClearError` intent).
- Retry loads data successfully (if the underlying issue is resolved).

---

### Photos

#### TC-CROSS-008 — Photo persistence across flows `P2`

**Preconditions:** None.

**Steps:**

1. Add a photo during Log Tea.
2. Start Timer → complete → save session.
3. View Session Detail.
4. Edit Session → add another photo → save.
5. View Session Detail again.

**Expected Result:**

- All photos persist through the entire flow.
- Photos are viewable in Session Detail.
- Fullscreen viewer works for all photos.

---

### Orientation / Lifecycle

#### TC-CROSS-009 — Screen rotation during timer `P2`

**Preconditions:** Timer is running.

**Steps:**

1. Rotate the device from portrait to landscape.

**Expected Result:**

- Timer continues running without reset.
- UI rebuilds correctly in the new orientation.
- No data loss.

---

#### TC-CROSS-010 — Form state preserved on config change `P2`

**Preconditions:** On Log Tea with fields filled in.

**Steps:**

1. Fill in all fields.
2. Rotate the device.

**Expected Result:**

- All field values are preserved.
- No validation errors appear spuriously.
