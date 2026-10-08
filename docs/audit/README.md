# Leaf Log — Code Audit (2026-09-23)

Input to the ROADMAP rewrite. Not a forward-looking document; fold what matters into ROADMAP.md
and delete this directory once it has been absorbed.

**Method.** Four parallel read-only audits, one per slice. Each traced every screen end to end
(entry → state → intents → use cases → DAO → exit) and cited `path:line` for every finding.

| Report | Covers | Findings | Of which bugs |
|---|---|---|---|
| [1-brew-loop.md](1-brew-loop.md) | Home, Log Tea, Timer, Quick Timer, Steep Complete, brew navigation | 78 | 32 |
| [2-journal.md](2-journal.md) | History, Session Detail, Edit Session, Analytics, shared photo/rating components | 87 | 30 |
| [3-tea-library.md](3-tea-library.md) | Collection, Tea Detail, Edit Tea, Brewing Methods, prefill | 67 | 22 |
| [4-setup-and-foundations.md](4-setup-and-foundations.md) | Vessels, Settings, Tea Types, navigation, theme, strings, errors, export/import, DI, build, platform parity | 123 | 33 |

~355 rows, ~117 marked bug, with overlap between slices (several agents independently found the
same root causes, which is itself signal). Items marked ✔ below were re-checked by hand after the
audit. Items marked *(device)* are clear in code but need a device to confirm the symptom.

---

## 1. The headline

The app's **surface is further along than its foundations.** Screens look finished; underneath,
the same half-dozen root causes produce most of the bugs on every screen. That changes what the
roadmap should be: not a list of screen tweaks, but a short run of foundation work that deletes
whole classes of bugs, followed by the ambitious work, which the foundations are prerequisites for.

The single most important finding: **"what is a session" is decided separately in ~6 places.**
Rating semantics, steep deletion, completion, discard, and reminder withdrawal have drifted apart
between `CompleteSessionUseCase`, `SteepCompleteViewModel`, `TimerViewModel`,
`DeleteSessionUseCase`, `InProgressSessionDelegate` and `SessionDetailViewModel`. Roadmap 2.1/2.2
("best brew") cannot be built correctly on top of this.

---

## 2. Fix now — verified, corrupts data or breaks a core flow

Ordered by damage. None of these needs design; all are small.

| # | Bug | Where | Report |
|---|---|---|---|
| 1 | **Staged file breaks the next commit.** `EditVesselFormLogic.kt` is staged (`AD`) and redefines the 5 `internal` functions that now live in `EditVesselState.kt`. `git commit` without `-a` produces a non-compiling commit. ✔ | git index | 4 |
| 2 | ~~**Steep Complete writes a guessed rating on open.** An unrated steep gets `round(tea.averageRating)` persisted immediately; skip the review and the guess stays, then feeds the tea average and the prefill — self-reinforcing. ✔~~ | `SteepCompleteViewModel.kt:179-190` | 1 |
| 3 | ~~**Quick Brew pre-rates sessions and never counts usage.** Session is created with `rating = config.rating` and no `usedConfigurationId`, so `timesUsed`/`lastUsedAt` never rise for the feature built on them. ✔~~ | `CreateSessionUseCase.kt:101-117` | 1 |
| 4 | **Steep Complete auto-saves revert each other.** Each field saves `staleSnapshot.copy(oneField)`; rate then type a note → rating reverts. | `SteepCompleteViewModel.kt:228-282` | 1 |
| 5 | **Edit Session can't clear anything.** Blank notes/rating/location or Loose→Bag is a silent no-op, because `null` means "unchanged". | `UpdateSessionUseCase.kt:35-64` | 2 |
| 6 | **Rating edits never recompute averages** (session `averageRating`, tea stats). Only Steep Complete does. | `EditSessionViewModel` | 2 |
| 7 | **Tea average uses steep 1 only**; prefill uses `averageRating ?: rating`. Tea page and prefill disagree on what was good. | `UpdateTeaStatsUseCase.kt:15-19` | 1, 3 |
| 8 | **Delete Tea leaves you on the deleted tea's live page** (can still brew it). `getByIdFlow` doesn't filter `deletedAt`. | `TeaDetailViewModel.kt:235`, `TeaDao.kt:33` | 3 |
| 9 | ~~**iOS relative dates wrong for most sessions** — compares year + day-of-*month*. Sep 23 shows every earlier 23rd as "today". ✔~~ | `TimeFormatter.ios.kt:33-47` | 2, 4 |
| 10 | **Android backups can't be imported on iOS** — Android writes DEFLATE, iOS parser accepts STORE only. ✔ | `ZipArchiver.ios.kt:233` | 4 |
| 11 | **Backups can't be fully restored after deleting a tea** — its sessions export with a dangling `teaId` → FK failure on a fresh device. Import is non-transactional, so it half-applies. | `DataExportRepositoryImpl.kt:43-47,191-218` | 4 |
| 12 | **Importing an older backup reverts edits and un-deletes rows** (blind upsert, `deletedAt` not carried). Roadmap calls this "merge". | `DataExportRepositoryImpl.kt:192-218` | 4 |
| 13 | ~~**Analytics "This Week" excludes today on Sundays.** ✔~~ | `AnalyticsViewModel.kt:369-370` | 2 |
| 14 | ~~**Donut taps open the wrong segment** (90° offset); trend taps pick the left neighbour.~~ | `TeaTypeDistributionChart.kt:185`, `BrewingTrendsChart.kt:142` | 2 |
| 15 | ~~**Timer edit sheet wipes persisted timer state** (saves stale row with `timerStatus = null`); reset while paused is never persisted.~~ | `TimerViewModel.kt:321-334,454-490` | 1 |
| 16 | **Quick Timer leaves an undismissable ongoing notification** and ignores the completion-notification setting. *(device)* | `QuickTimerViewModel.kt:160-221,314-331` | 1 |
| 17 | ~~**Steep Complete discard doesn't withdraw the reminder**; the reminder then reopens the deleted session as reviewable.~~ | `SteepCompleteViewModel.kt:434-474` | 1 |
| 18 | ~~**iOS Live Activity extension requires iOS 26.2**, app requires 18.2. ✔~~ | `project.pbxproj:312,345` | 4 |
| 19 | ~~**Errors that can never be seen**: inline `Snackbar` after a `fillMaxSize` sibling (Collection, History); `state.error` never rendered (Timer, Brewing Method, Edit Session, Vessels); export/import failures mapped to "completed"/"cancelled".~~ | see §3E | 3, 4 |
| 20 | ~~**A streak insight ships** despite "No streaks" in Decisions. ✔~~ | `GenerateInsightsUseCase.kt:102` | 2 |

Also worth a device check soon: `backStack.removeLast()` (18 sites) with compileSdk 37 / minSdk 24
may bind to the JDK 21 `List.removeLast()` and throw `NoSuchMethodError` below API 35 —
*unverified*, trivially testable on an API 34 emulator.

---

## 3. Root causes — each is a foundation workstream

These are where the roadmap should spend its first real effort. Each deletes a *class* of bug.

### A. Session aggregate — one owner for session + steeps
- Symptoms: steep delete leaves stale averages and numbering gaps from History but not from Detail
  (logic lives in one ViewModel); "Complete & Start New" erases the rating and skips recompute;
  re-finishing a completed session double-counts `timesUsed`; parent edits don't propagate to
  child steeps (copied vessel/water/leaf/location); Brew Again from a child card starts at steep
  N's parameters; child steeps render as look-alike separate brews in History, Tea Detail recents,
  and Vessel session counts; min-rating filter reads `rating` while cards show `averageRating`.
- Fix shape: a `SessionWithSteeps` domain aggregate and a small set of use cases that own every
  steep mutation (add, delete, rate, finish, discard). One `effectiveRating` definition. Averages
  and tea stats recompute in exactly one place.

### B. Honest data — the record only holds what actually happened
- **Ratings the user didn't give** — four sources: Steep Complete guess, Quick Brew copy, Log
  Session dialog prefill, `SaveConfigurationDialog` `?: 5f`.
- **Planned vs actual time** — no flow records how long tea actually brewed (±adjust, Complete Now,
  Quick Timer). The prefill replays the plan, not the brew.
- **Timestamps** — steeps added to past sessions are stamped "now"; Quick Timer stamps end, others
  stamp start; session time is never editable.
- **Unit round-trips lose precision** — every edit sheet converts through whole display units
  (150 ml → 147.9 ml for fl-oz users on a no-op save).
- **`null` means "unchanged"** in update use cases, so "cleared" is inexpressible.
- **Whole-row upserts from stale snapshots** — Timer, Steep Complete, Edit Tea, Toggle Favorite,
  search results. Field-level updates or read-modify-write inside use cases.

### C. Soft-delete semantics
- By-id queries return deleted rows; list queries don't. Result: History says "Unknown Tea",
  Session Detail names it and links to it, Analytics counts it in totals but not breakdowns,
  export writes orphaned children.
- Photos are hard-deleted on a soft delete (Session Detail, Edit Vessel before save) — **Undo (2.4)
  would restore sessions with missing photos.** Brewing configurations are hard-deleted with no
  `deletedAt` column at all.
- Needs one decision: does the journal ever hide a deleted tea's name? `finishedAt` (2.3) is the
  natural alternative to deleting a tea you've drunk.

### D. Screen lifecycle and navigation
- Loading from `LaunchedEffect` with ViewModels that outlive the composition: edit forms re-seed
  over user edits on any Activity recreation (4 screens), collectors stack on every return
  (Session Detail, Tea Detail, Collection), route filters re-apply on back (History), Quick Timer
  restarts on recreation. Fix once: route args into the ViewModel, load in `init`.
- Tabs `clear()` the back stack: back from any tab exits the app, every tab rebuilds on return
  (spinner, lost scroll/search/filters), pushed tab screens have no Up arrow.
- Deep link replays on every `onCreate`; notification taps don't check session status;
  `PendingIntent` requestCode 0 shared across notifications *(device)*.
- No `SavedStateHandle` anywhere; form state dies with the process (photo picker is the classic
  trigger).

### E. Error handling
- Five patterns: `SnackbarHost` (5 files), inline `Snackbar` (7), full-screen replace with no
  recovery (Steep Complete, Analytics), set-but-never-rendered (Timer, Quick Timer, Brewing
  Method, Edit Session, Vessels), swallowed (export/import, Quick Timer auto-save).
- 85 sites show raw `e.message`. User-facing copy lives in domain use cases.
- Fix shape: one `UiMessage` channel + `SnackbarHost` per screen; a handful of typed domain
  failures (`NotFound`, `InUse(n)`, `LastRemaining`, `Io`) mapped to copy in the UI. This pulls
  Phase 4's "user-facing errors" forward: the invisible/unrendered cases are bugs now.

### F. Two timer engines
Quick Timer runs its own countdown in `viewModelScope`: no foreground service, no persistence, no
settings toggles, no reminder, no notification cleanup, no alarm cancel. One engine
(`TimerService`) for both, via a nullable-tea draft or a session-less persisted `TimerState`.

### G. Backup integrity
Beyond "version never read" (roadmap 1.2): wire format *is* the Room entities (5 migrations changed
it silently), DEFLATE/STORE mismatch, no transaction, orphaned FKs, blind clobber, all images held
in memory (OOM with photo growth), `<index>.jpg` names collide across imports, legacy path detaches
existing photos, temp files never cleaned, preferences not exported, no "last backup" signal even
though export is the only backup under the no-sync decision.

### H. Presentation system
- **Strings:** 0 `stringResource` calls; ~550 hardcoded literals in 71 files; hand-rolled plurals
  ("1 brews", "Used 1 times", "week(s)"); US-only date patterns in 5 places.
- **Formatting:** `WeightFormatter` used only in Analytics — "5.0g" everywhere else. A weight
  setting row (2.7) would promise something the app doesn't honour. Rating display truncates in
  some places and rounds in others; stars floor.
- **Design identity:** stock `Typography()`, no `Shapes`, M3 baseline fallbacks for unset roles,
  tea-type colours never adjusted for dark mode (White `#FFFDE7` invisible on light surfaces), white
  splash in dark mode, two icon families (Feather + FontAwesome).
- **Duplication:** 4 discard dialogs, 4 delete-session dialogs (11 inline delete dialogs total),
  5 section-header variants, Temperature/Volume/Capacity inputs, preset tables (with *different*
  brew-time presets in Log Tea vs Quick Timer), `catchError` ×3, `formatBrewingTime` ×2.
- **Unit suffix toggles in forms silently change a global preference** (6+ places, ~24–28 dp
  touch targets).
- **Hardcoded layout offsets** (60/80/88 dp FAB clearance, `maxHeight − 420.dp` timer ring) and
  non-scrolling containers; hidden on phones by the portrait lock, exposed on iPad/tablets —
  which Android 16 no longer lets you lock.

### I. Release blockers (Phase 4 inputs)
Widget extension deployment target and hardcoded version, missing `PrivacyInfo.xcprivacy`
(NSUserDefaults), `FOREGROUND_SERVICE_SPECIAL_USE` Play declaration, notification permission
requested on first `onCreate` with no context, `isMinifyEnabled = false`, unused deps
(`coil-network-ktor3`, `room-sqlite-wrapper`, `lifecycle-process`, `core-ktx`, `appcompat`), no
About/version anywhere in the UI, `allowBackup` without extraction rules.

---

## 4. ROADMAP corrections

| Roadmap says | Actually |
|---|---|
| Header: superseded docs "all deleted" | All eight still in `docs/`, none committed |
| 1.1 "verify all five migrations are registered" | They are, on both platforms — answered |
| 1.2 import "merges rather than clobbers. Correct default; keep it" | It clobbers whole rows, reverts newer edits, un-deletes. Not a safe default |
| 2.2 zero sessions and zero configs "both render nothing" | Zero configs shows "No saved methods yet"; zero sessions renders nothing |
| 2.4 "soft-delete infrastructure in place everywhere … mostly UI" | Configurations hard-delete (no column); photos are hard-deleted on soft delete |
| 2.5 "nothing signals completion when the app is open" | The system notification does, in foreground — weakly, and not at all when the in-app toggle is off |
| 2.7 FAB hidden during in-progress session | Always visible since 558d194; conflict dialog replaced it |
| 2.7 Choose Method lacks vessel/parameters | Shows parameter chips; list is already vessel-scoped. Real problem is duplicate auto-labels |
| 2.7 custom date range doesn't survive restart | Custom range is unreachable — filtered out of the menu |
| 2.7 weight-unit row is "discoverability only" | Weight unit is honoured by one Analytics card only |
| Decisions: No streaks | A streak insight ships |
| CLAUDE.md: `factoryOf` for repositories | Repositories are `singleOf` (correctly) — fix the doc |
| Dead-code list | Also: `BrewingConfiguration.isActive`, `RecentSessionsCard`, `CompleteDraftDialog`, `NavRoute.MoreRoute`, Log Tea photo intents, Quick Timer dead state, `CompleteTimerUseCase`, 4 single-call use cases, 3 unreferenced drawables |

---

## 5. Screen verdicts

One line each; detail in the linked reports.

| Screen | Verdict |
|---|---|
| Home | Right layout; quick-brew doesn't count usage, full reload on every tab switch, stat tiles do three unrelated things (one silently changes a setting) |
| Log Tea | Picker is the best component in the app; buttons appear only when valid with no reason shown; water type resets on any prefs emit |
| Timer | Engine sound; screen never shows errors, discards actual brew time, edit sheet corrupts timer state |
| Quick Timer | Good idea on a second, weaker engine — loses every lifecycle guarantee the main timer earned |
| Steep Complete | Right shape; guesses ratings, auto-saves clobber each other, one error hides the form, business flows live in the VM, gongfu sessions never offered "save method" |
| History | Still a parameter list, not a journal; child steeps look like brews; invisible search keeps filtering; route filters re-apply |
| Session Detail | Reads like a form printout; stacking collectors; Add Steep skips in-progress guard and stamps "now" |
| Edit Session | Can't clear fields; no errors shown; no back guard; edits don't reach child steeps |
| Analytics | Mostly-correct multi-steep aggregation; date-boundary bugs, broken chart taps, loads once and goes stale, ~14 sequential full-table queries on All Time |
| Collection | Works small; search is name-only, a stale snapshot, and can't combine with filters; error display invisible |
| Tea Detail | A property sheet (roadmap 2.2); delete broken, average wrong, collectors leak, recents filled by child steeps |
| Edit Tea | Best form in the app; double-submit creates duplicates, edit skips trimming, lands on list not the new tea |
| Brewing Method | Weakest form: no validation, no errors, can't clear, prefill ignores chosen vessel, can't represent gongfu |
| Vessels / Vessel Detail | Thin; delete rules in UI disagree with use case; red "Cannot Delete" card on every used vessel |
| Edit Vessel | Most carefully built; photo deleted before save, double-submit, re-seed on recreation |
| Settings | Honest and clear; data section swallows failures, no confirmation, no import preview, no About |
| Tea Types | Old pattern EditVessel moved away from; unit toggle wipes input, °C/°F validation wrong, can delete the last type |

---

## 6. Opportunity catalog

Deduplicated across all four reports. Grounded in what the model already stores or clearly lacks.
This is the raw material for choosing big bets in the vision interview — not a commitment.

**Brewing depth**
- **Steep schedules.** `BrewingConfiguration.brewingTime` is one `Duration`; child steeps already
  store per-steep time/temp/rating. Learn a schedule (base + increment or explicit list) from the
  best-rated session, show "Steep 3 · next 25s", auto-advance. Save-as-method saves the schedule.
  Brew Again replays the whole curve. Surfaced independently by three of four audits.
- **One-tap brewing.** Pinned method → session + running timer in one tap; "Same +5s" next steep
  that skips the dialog; auto-start preference. Today a gongfu steep costs ~3 taps and 2 screens.
- **Actual brew time** recorded alongside planned; "actual vs planned" on review.
- **Rinse/preheat as steep 0.**
- **Per-type default time and leaf ratio**, and prefill scaled to vessel capacity.
- **In-app completion moment** (haptic, chime, "Pour now") independent of notification settings.

**The journal**
- **One card per session** with steep count, span, per-steep rating dots. Kills the child-card bug
  class as a side effect.
- **Reading layout**: photo strip and notes lead, parameter chips recede.
- **Steep timeline** in Session Detail: clock time, interval since previous steep, changes,
  inline rating/notes.
- **Calendar navigator** atop History (reuse heatmap canvas); heatmap day → History day.
- **Session-level note**, editable date/time, previous/next session navigation, "previous time
  with this tea".
- **Location** as a journal dimension (stored and edited, never shown or searched).
- **Photo hero** and captions in the fullscreen viewer; camera capture; choose cover photo.
- **Structured tasting vocabulary** (aroma/body/astringency tags per steep) — makes ratings
  explainable, the journal searchable, and "teas like this" possible. No tag model exists.

**The library**
- **Cellar/aging**: `purchaseDate` is collected and never displayed; add harvest year/vintage.
  "Bought Mar 2023 · 2y 6m in your cupboard".
- **Method provenance**: `sourceSessionId` stored, never shown — "From your 12 Mar brew ★4.5".
- **Per-tea evolution**: parameters vs rating over time, steep curve ("peaks at steep 4").
- **FTS search** across name/producer/origin/description, combinable with filters; producer and
  origin facets.
- **Vessel as a lens**: "what this vessel is best for", typical fill vs capacity, methods using it.
- Drag-reorder for vessels and tea types (`displayOrder` exists, no UI); merge tea types.

**Retrospection** (roadmap 3.2)
- Analytics refactor to one query + pure aggregators — prerequisite for recaps and makes every
  date-boundary bug host-testable.
- Season/month recaps, parameter distributions per tea type, time-of-day suggestion on Home
  ("your usual 3pm oolong").

**Platform and craft**
- Tea-appropriate display font + Shapes; dark-mode-toned tea colours; fade-through tab transitions.
- Per-tab back stacks; tablet/iPad layouts (can't be avoided on Android 16 anyway).
- "Last backup 42 days ago" nudge; import preview; preferences in export.
- String resources + typed errors — the prerequisite for localization and for Phase 4.
- Canvas chart semantics (one line per chart) and RatingSelector semantics — the rating control is
  the loop's central input and is currently unusable with a screen reader.

---

## 7. Needs a device to confirm

`removeLast()` below API 35 · shared `PendingIntent` requestCode routing · Quick Timer ongoing
notification and double alert · Quick Timer and edit-form behaviour on dark-mode/font-scale
recreation · deep-link replay after process death · Quick Brew card width with long names · Timer
ring and Quick Timer step 2 at large font scale · iOS back-swipe on pushed tab screens ·
`FileOperations` `pendingFilePath` null after recreation · Auto Backup quota with photos.
