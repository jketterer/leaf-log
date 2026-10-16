# Leaf Log — Roadmap

> **Last updated:** 2026-10-15 (Bet 1 designed)
> **Inputs:** the code audit in `docs/audit/` (2026-09-23) and a vision interview the same day.
> **Supersedes:** PRD.md, FEATURES.md, ARCHITECTURE.md, DATA_MODEL.md, DOMAIN_LOGIC.md,
> DESIGN_SYSTEM.md, architecture-audit.md, punch-list.md — to be deleted in F0. CLAUDE.md carries
> the architecture rules. TEST_CASES.md survives only as the Track B device checklist.

This document is the single forward-looking record for Leaf Log. Nothing in it describes what the
code already says — the code says that better and doesn't go stale. It holds intent, ordering,
rationale, and the decisions that shouldn't be re-argued.

---

## 1. What Leaf Log is

A tea brewing tracker for Android and iOS, shared Compose Multiplatform, used daily by its author.

It serves three purposes at once, and they're ranked. When they conflict, higher wins:

1. **A personal daily tool.** The author is the user. Friction he feels is the highest-priority
   signal in the project. This is why the working-items list at the bottom exists.
2. **A public product.** A real target with no date. Store-readiness is its own gated phase (§ Store
   readiness); nothing ships until it completes, but it blocks nothing before then.
3. **A portfolio piece.** Strangers read the code. This justifies work with no user-facing payoff —
   widgets, test infrastructure, architectural discipline — but never at the cost of 1 or 2.

### Vision: the tea knows how you brew it, and remembers how it went

**Leaf Log at its best is a record of tea you enjoy rereading — kept by an app that makes recording
it nearly free.**

The previous vision stopped at the first half. The interview added the second, because it surfaced
an uncomfortable fact: **the author rarely writes notes or takes photos.** A journal nobody writes
in has nothing to reread. So the order of the work is: make brewing fast, make capture cost almost
nothing, *then* build the reading surfaces on data that now exists.

The author brews **both styles, depending on the tea** — gongfu (many short steeps) and western (one
or two long ones). The app should know which a given tea wants, and make either path fast.

What the author actually rereads, in their own ranking — and therefore what the journal is built
for:

- **This tea's history, right before brewing it.** "Last time: 5g · 95° · 20s · ★4, went bitter at
  steep 3."
- **A day or week in sequence.** What was drunk, and when.
- **Seasonal and yearly recaps.**

Not on the list: **photos.** They get safety work (they threaten backups) and nothing more.

Division of responsibility between screens:

| Screen     | Owns                                                                               |
|------------|------------------------------------------------------------------------------------|
| Analytics  | Period aggregates and counts                                                       |
| Tea Detail | Per-tea judgement — best brew, steep curve, flavour profile, how a tea has evolved |
| History    | The readable record. Sequence, rhythm, rereading                                   |
| Recap      | Retrospection as an occasion — a season or a year, told once                       |

### What Leaf Log is NOT

A roadmap without a "no" is a wishlist. These are decided, not open:

- **Not a social app.** No sharing, no feeds, no friends, no public profiles.
- **Not a shop.** No vendor catalogs, no affiliate links, no purchasing.
- **Not a cost tracker.** `purchasePrice` is being deleted, not built.
- **Not a stock inventory.** You mark a tea finished. You do not count grams remaining.
- **Not synced.** See § Decisions.
- **Not a photo journal.** Photos are kept safe, not featured.
- **Not an engagement machine.** No streaks, no nagging, no guilt colours on "fewer sessions".

---

## 2. Current state (verified 2026-09-23)

|                                               |                                                                                                                       |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Kotlin source files                           | ~304                                                                                                                  |
| Screens (Screen + ViewModel + State + Intent) | 16                                                                                                                    |
| Use cases                                     | 47 — 4 are single-call wrappers CLAUDE.md forbids                                                                     |
| **Test files**                                | **4** — `GetBrewingParametersPrefillUseCaseTest`, `DurationFieldsTest`, `EditTeaFormLogicTest`, `EditVesselStateTest` |
| Room schema                                   | version 6, 5 migrations, all registered on both platforms, schemas exported                                           |
| Migration tests                               | **zero**                                                                                                              |
| CI — Android / shared                         | GitHub Actions: `:shared:allTests :androidApp:assembleDebug` (F0)                                                     |
| CI — iOS                                      | Xcode Cloud, via `iosApp/ci_scripts/ci_post_clone.sh`                                                                 |
| Crash reporting                               | none                                                                                                                  |
| String resources                              | **none** — ~550 hardcoded literals across 71 files                                                                    |
| Version                                       | 0.1.0 from `version.properties` (F0). Before that: 1.0 / build 1 everywhere, no tags, no changelog                    |
| `docs/`                                       | Tracked; superseded docs deleted (F0)                                                                                 |

**The audit's headline:** the surface is further along than the foundations. ~355 findings, ~117
bugs, most of them produced by a handful of root causes repeated on every screen (audit §3). The
worst: **"what a session is" is decided in about six places** that have drifted apart, and **the app
records things that didn't happen** — four code paths write ratings the user never gave.

**Dead code confirmed:** `Tea.purchasePrice`, `Tea.stockAmount`, `BrewingConfiguration.isActive`,
`NavRoute.MoreRoute`, `RecentSessionsCard`, `CompleteDraftDialog`, Log Tea's photo intents, Quick
Timer's unused state, `CompleteTimerUseCase`, unused catalog/Gradle deps (`junit`,
`espresso`, `testExt`, `core-ktx`, `appcompat`, `kotlin-testJunit`, `coil-network-ktor3`,
`room-sqlite-wrapper`, `lifecycle-process`), 4 unreferenced drawables.

---

## 3. Verification strategy

The project's own bug history is the evidence. The earlier bugs — the Live Activity counting up past
0:00, the banner reading "Finish" while routing to the timer, the notification tap landing on the
wrong screen, the reminder cancelled inside `stop()` — were cross-process, wall-clock,
platform-integration bugs. The audit added a second class: **state and data-integrity bugs**
(stale-snapshot writes, date boundaries, aggregation, backup round-trips) that pure host tests catch
cheaply. Three tracks, each catching a class the others can't.

### Track A — logic, host JVM, runs in CI

Room migrations (`sqlite-bundled` runs on the host), export/import round-trips, the session
aggregate, use cases, ViewModel state machines, pure date/format/parse functions.

### Track S — preview smoke tests, host JVM, runs in CI

Every `@Preview` — already mandatory per CLAUDE.md — is rendered on Robolectric light, dark and at
2× font scale, and the test fails if one crashes. Nothing is compared against a golden: the author
isn't after pixel parity, and goldens would churn hundreds of PNGs into git on every theme tweak.
Components with user text add a long-text preview, for eyeballing in the IDE. It is **not** the
rejected Compose UI test suite: nothing is tapped, nothing is flaky. Goes live in Bet 1.

### Track B — manual, real device, run rarely and deliberately

Timer across backgrounding and process death, notification delivery and tap routing, Live Activity,
deep links, permission prompts, file pickers, Activity recreation (dark-mode switch, font scale),
Doze-delayed reminders, and the audit's §7 list. Lives in a trimmed `TEST_CASES.md`.

### The rules, going forward

> **Non-trivial logic ships with one host-JVM test. A new or changed component ships with its
> preview variants. Platform integration ships with a line on the Track B checklist.**

> **Definition of done includes accessibility basics** for anything touched: meaningful icons
> labelled, 48dp targets, custom Canvas given a semantic summary, controls given value/selected
> semantics. Not a launch gate — a habit.

---

## 4. How this roadmap is shaped

- **One thin foundation, then five bets in order.** The foundation is only what every bet stands on.
  Everything else is paid for by the first bet that touches it — no multi-week invisible phase.
- **Each bet has an outcome, exit criteria, and a design section.** Exit criteria are how you know
  it's done; the design section holds the decisions a later session would otherwise re-derive.
- **The polish track runs continuously** alongside the bets (§ Polish track). Rule: *a bet that
  rewrites a screen also clears that screen's audit findings.* Nothing gets rebuilt twice.
- `docs/audit/` is the polish backlog. Delete a report once its findings are cleared or absorbed.

```
F0 Ground truth ─► F1 Fix now ─► F2 Session aggregate ─► F3 Tea lifecycle ─► F4 Backup integrity
                                                                     │
          ┌──────────────────────────────────────────────────────────┘
          ▼
Bet 1 Design system & app shell ─► Bet 2 Brewing plans ─► Bet 3 Journal ─► Bet 4 Recap ─► Bet 5 Widgets
          ▲                                                                                   │
          └─────────────────────── Polish track (continuous) ─────────────────────────────────┘
```

---

## Foundation

### F0 — Ground truth

**Done.** Small; everything after it is worth more once it's done.

- **GitHub Actions CI**: `./gradlew :shared:allTests :androidApp:assembleDebug` on push and PR.
  Without it, every test written later is a thing you *remember* to run.
- **Test dependencies:** `koin-test` wired for DI graph verification; the Android-instrumented
  scaffolding and the unused deps listed in § 2 deleted.
- **`TEST_CASES.md`** trimmed to the Track B checklist. The superseded docs were committed, then
  deleted, so they're recoverable from history.
- **Release hygiene.** "Which build is on my phone" must be answerable.
    - Semver, bumped by judgement (not tied to milestones), starting at `0.1.0`.
    - `version.properties` at the repo root holds `VERSION_NAME` and nothing else. Android reads it
      in Gradle; `Config.xcconfig` `#include`s it (a comment-free properties file is valid xcconfig)
      and maps it to `MARKETING_VERSION`. The widget extension's hardcoded
      `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` are removed so it inherits.
    - Build numbers are never edited by hand. iOS: Xcode Cloud sets `CFBundleVersion` per archive;
      TestFlight shows `0.1.0 (47)`. Android: `versionCode` = UTC `yyMMddHH` via a `ValueSource`
      (survives non-linear branch work; plain `now()` would be frozen by the configuration cache);
      `versionName` = `0.1.0+<sha>[.dirty]` on every variant, visible in App info.
    - No in-app version display until the About screen (Store readiness).
    - `CHANGELOG.md` in Keep a Changelog format, written at bump time from `git log`, in user
      language. Bump steps live at its top.
    - Tags: `v0.1.0` style, annotated. History: `flutter-1.1.1` (the published 2019 Flutter app,
      `com.jketterer.leaf_log`) and `flutter-2.0-wip` (the 2022 `two-point-oh` attempt), kept out of
      the `v*` namespace.
    - `main` is the default branch; CI triggers on `[main, kmp]` until `kmp` merges.

**Exit:** both platforms and the widget read `version.properties`; `CHANGELOG.md` has a `0.1.0`
entry; the three tags are pushed; CI and Xcode Cloud trigger from `main`.

### F1 — Fix now

**Done, released as 0.1.1.** The verified bugs in `docs/audit/README.md` §2 that no later item
rewrites. No design needed; all small.

- **Ratings the user didn't give stopped being written** (#2, #3): Steep Complete's guess on open,
  Quick Brew's, and `SaveConfigurationDialog`'s `?: 5f`. F2's migration couldn't tell a guess from a
  real rating, so this went first.
- **iOS relative dates** (#9): a pure commonMain classifier (Today / Yesterday / This year / Older,
  given `now` and `tz`); the actuals only format.
- **Weeks start on Monday, everywhere** (#13): one `LocalDate.startOfWeek()`. History's "This Week"
  filter became "Last 7 Days" — that's what it does.
- **Chart taps** (#14): hit-testing as pure functions; each Canvas has a semantic summary.
- **Timer edit sheet** (#15), **Steep Complete discard withdraws the reminder** (#17), **Live
  Activity deployment target** 18.2 (#18).
- **Errors you can see** (#19): minimal visibility only. Bet 1 replaces the source.
- **Streak insight removed** (#20) and **`removeLast()` → `removeAt(lastIndex)`** (see Hard-won
  facts).

**Moved, not fixed here:** #2–7 → F2 (except the rating writes), #8 → F3, #10–12 → F4, #16 Quick
Timer notification → Bet 2 (which deletes that engine). #1 closed in F0.

**Exit:** each fix that involved a branch, loop, or date calculation has a Track A test.

### F2 — The session aggregate

**Done, released as 0.1.2.**

**Outcome:** one definition of a session and its steeps, owned in one place. The prerequisite for
brewing plans, best brew, the journal, and recap — all of which would otherwise be built on six
disagreeing copies of the rules.

**Design** (decided 2026-10-13):

- **Storage stays one table; the domain splits.** A parent row is the session *and* steep 1;
  children are steeps 2…n. The domain gets `Session` (tea, status, start time, vessel, water type,
  leaf, location, `usedConfigurationId`), `Steep` (number, planned and actual time, temperature,
  water, rating, notes, photos, timer fields) and `SessionWithSteeps(session, steeps)`. The mapper
  splits a parent row into `Session` + `Steep(1)`, so `Session.id` is steep 1's id. Domain
  `TeaSession` is deleted. The timer works on a `Steep`.
- **Status lives on the parent and means the session.** It stays `IN_PROGRESS` until Finish. Child
  `status` is ignored, not maintained. The current steep is the highest `steepNumber` of an
  in-progress session. (Today the parent goes `COMPLETED` when steep 2 starts, so tea stats count
  unfinished sessions and `hasInProgressSession()` misses steeps 2+.)
- **Session-level fields live on the parent only.** Children's vessel, water type, leaf, location
  and `usedConfigurationId` are NULL. `teaId` stays on every row (FK cascade, by-tea queries).
- **Ratings:**
    - `effectiveRating` = mean of rated steeps, `null` if none. **Computed on the aggregate, never
      stored:** `averageRating` and `UpdateAverageRatingUseCase` are deleted. Full precision in the
      domain; rounding belongs to Bet 1's rating component.
    - A tea's rating = mean of its completed sessions' `effectiveRating`s. Each session counts once;
      unrated sessions are skipped.
    - Only a user tap stores a rating. The Log Session dialog starts unrated. Ghost suggestions are
      Bet 1 and 2's; F2 renders none.
- **Stored derived stats are recomputed, never incremented.** Tea `totalSessions`, `averageRating`
  and `lastBrewedAt` (completed sessions only; `lastBrewedAt` is the start time) and configuration
  `timesUsed` and `lastUsedAt` are recomputed from sessions by one function inside every mutation's
  transaction. `incrementTimesUsed` is deleted: it fired once per steep and never decremented.
  Re-finish, reopen and delete are then correct with no special cases.
- **Transactions.** A domain `Transactor`; Room implements it with
  `useWriterConnection { it.immediateTransaction { } }`, the test fake just runs the block. Database
  work runs inside it. Side effects (timer stop, reminder withdrawal, file deletion) run after the
  commit, so a rollback never withdraws a reminder. A stats failure rolls the mutation back.
- **Writes, one use case per operation:** `StartSession` (Brew Again folds in), `AddSteep`,
  `EditSteep`, `EditSession`, `DeleteSteep` (renumbers), `FinishSession` (no-op when already
  completed), `ReopenSession`, `DeleteSession`, plus timer state save and clear. Repository writes
  are called only from these. The six current paths — `CompleteSessionUseCase`,
  `SteepCompleteViewModel` (×3), `TimerViewModel` discard, `InProgressSessionDelegate`,
  `SessionDetailViewModel` steep delete — collapse into them.
    - Edits take a transform applied to the row re-read inside the transaction:
      `editSteep(id) { it.copy(notes = null) }`. Callers name only what they changed, so stale
      snapshots can't happen and `null` means cleared. No patch type. Changes to identity or timer
      fields are rejected.
    - Discard is `DeleteSession` on steep 1, and `DeleteSteep(N)` + `FinishSession` on steep N ≥ 2.
    - Soft deletes stop deleting photo files (F3's rule, pulled forward). Removing a photo in Edit
      Session still deletes the file.
    - A unit field goes into an edit only if its display text changed. One pure helper, used by the
      Timer, Steep Complete and Edit Session sheets.
- **Reads return aggregates.** One query per read: the matching parents plus their children via
  `COALESCE(parentSessionId, id) IN (…)`, grouped by one `toAggregates()`. A session belongs to its
  start time for ranges and days. Callers keep filtering by status. Routes carry the session id;
  Session Detail's child-to-parent walk is deleted. History and Tea Detail get one card per session
  without a layout change, and Vessel Detail stops counting steeps as sessions. Analytics ports
  mechanically; Bet 4 restructures it.
- **Actual brew time.** `Steep.actualTime` = `totalDuration − remainingDuration` at timer
  completion:
  includes ±adjust, excludes paused time, and is the elapsed time for Complete Now. The planned
  `brewingTime` is never rewritten. `null` means no timer record. Prefill and Brew Again replay
  `actualTime ?: plannedTime`; Quick Timer passes it too. Nothing displays it until Bet 3.
- **Session Detail's Add Steep records a forgotten steep:** no timer, stamped
  `last.timestamp + (actual ?: planned)`, and the session stays completed.
- **Undo an accidental finish.** Home's route takes `finishedSessionId`; a long snackbar offers
  Undo, which runs `ReopenSession` and returns to Steep Complete. This is not a general way to
  re-open old sessions, and it doesn't re-arm the reminder.
- **Backups stay importable** (the minimum; F4 does the rest). Import writes rows through the DAO,
  NULLs children's session fields and recomputes all stats. The new column defaults to null, so v6
  backups still parse.

**Migration:** v6 → v7 rebuilds `tea_session`. Session columns become nullable and are NULLed on
children; `averageRating` is dropped; `actualBrewingTimeMs` is added with no backfill (an old total
can't tell Complete Now from a natural finish). A parent with an `IN_PROGRESS` child becomes
`IN_PROGRESS`, the rest `COMPLETED`. Tea and configuration stats are recomputed in SQL; a test
checks the SQL against the Kotlin rules on the same fixture.

**Verification:**

- Track A: every use case against a fake repository, the rating and stats rules, aggregate grouping,
  the migration on a synthetic fixture (host JVM, bundled SQLite), and an F2 export → import round
  trip.
- Track B: install `v0.1.1` on an emulator and seed by hand a single-steep session, a gongfu session
  with a deleted middle steep, a parent edited after steeps were added, an unrated session, a
  session left mid-steep, and a saved method used across several steeps. Install the F2 build over
  it and check the screens and stats.

**Exit:** the six paths are gone; Track A tests cover every steep mutation, the rating rule and the
migration; the emulator upgrade passes; History, Tea Detail and prefill agree on every tea's rating.

### F3 — Tea lifecycle: finished, not deleted

**Done, released as 0.2.0.**

**Outcome:** the journal never loses a name, and soft-delete stops leaking.

**Design** (decided 2026-10-14):

- **`Tea.finishedAt: Instant?`.** Null = in stock. Timestamp, not boolean — tells you *when*, and
  matches the `deletedAt` idiom.
    - Excluded from dormant-tea prompts (Bet 4) — the reason the field exists.
    - Set and cleared only from Tea Detail ("Mark finished" / "Back in stock", plus a "Finished
      <date>" line). A plain repository write from the ViewModel; it bumps `updatedAt`.
    - `StartSessionUseCase` clears it inside its transaction, so Log Tea, Quick Brew, Brew Again and
      Quick Timer all clear it. Add Steep, Reopen and import don't: they don't prove you still have
      the tea.
    - Collection: sorted last under every sort option, dimmed, labelled "Finished" (dimming alone is
      colour-only meaning), still searchable.
    - Log Tea picker: stays visible with the same label. No "Still have this?" dialog: starting
      clears the flag anyway, and a "last of it" choice wasn't worth threading through every start
      path. Quick Brew dims finished teas' methods.
- **A tea with sessions cannot be deleted** — only finished. "Has sessions" means any live parent
  row of any status: `totalSessions` counts completed ones only and would let an in-progress tea go.
  Delete is offered only at zero (typos, duplicates).
    - Vessels already follow the rule (`DeleteBrewingVesselUseCase`, `canDelete`).
    - Tea types already refuse delete when used. F3 adds **merge**: the blocked-delete dialog lists
      every other live type, system defaults included; `MergeTeaTypeUseCase` moves every tea,
      deleted ones too, and soft-deletes the source in one transaction. Teas keep their own
      temperatures.
- **Deletes take their methods.** Soft-deleting a tea or a vessel soft-deletes its configurations in
  the same transaction, so method lists need only `deleted_at IS NULL` and Undo restores both.
- **By-id queries filter soft-deleted rows** like list queries do. Tea Detail closes when its tea
  goes null after loading (#8). The "Unknown Tea" fallbacks stay as defensive code; the migration's
  repair and the delete rule are what keep them unreachable.
- **Photo files are only removed on hard purge**, never on soft delete. Already true: F2 stopped the
  session deletes, the others never did, and hard purge (`deleteSyncedOldItems`) never runs.
- **Brewing configurations get `deleted_at`**, replacing `is_active` (no UI ever set it false).

**Migration:** v7 → v8 with `ALTER TABLE ADD/DROP COLUMN` (bundled SQLite on both platforms, no
rebuild). Adds `tea.finishedAt` and `brewing_configurations.deleted_at`; drops `purchasePrice`,
`stockAmount` and `is_active`, mapping `is_active = 0` to `deleted_at = updated_at`. It repairs rows
the old delete paths left behind: a deleted tea with live sessions comes back finished
(`finishedAt = deletedAt`), a deleted vessel a live session uses comes back archived, a deleted type
a live tea uses comes back, and methods whose tea or vessel is still deleted are soft-deleted.

**Exit:** no screen can show "Unknown Tea"; Undo is purely UI work; Track A tests for each rule.

### F4 — Backup integrity

**Done, released as 0.3.0.**

**Outcome:** a backup restores on either platform, onto a fresh device, without losing or reverting
anything. Export is the only backup under the no-sync decision, so this is data safety, not polish.

**Design** (decided 2026-10-15):

- **One ZIP implementation, on okio, in commonMain.** Both `ZipArchiver` actuals are deleted.
  Reading is okio's `openZip`, which handles STORE and DEFLATE, so existing Android backups import
  on iOS. Writing is a small streaming STORE writer: photos don't compress, and STORE needs each
  entry's CRC up front, so it reads each photo twice (CRC, then copy). okio 3.17 already arrives
  through DataStore; it is declared in the catalog. Entries stream file-to-file: no whole-archive
  `ByteArray`. Entry names are checked against zip-slip.
    - Java's `ZipInputStream` can't read STORED entries that use data descriptors, so the writer
      never uses them.
- **Versioned DTOs, decoupled from Room.** `version = 2`. The DTOs carry facts only: no derived
  stats (tea `totalSessions`, `averageRating`, `lastBrewedAt`; method `timesUsed`, `lastUsedAt`), no
  `userId` or `syncStatus`. Today's entity JSON is frozen as `BackupV1` (lenient, defaults for
  fields added since schema 6) and migrated to V2, the pre-v7 child normalisation with it. A newer
  version is refused with "update the app". The plain-JSON import path is deleted: every export
  since Feb 2026 is a ZIP.
- **Export everything, deleted rows included**, and their photos. A deleted tea's sessions then
  restore with their tea (#11), and a later Undo finds its photos. Export reads its snapshot inside
  one transaction. The file is named with the export date. Photo paths whose file is missing are
  dropped.
- **Orphans are skipped and counted.** Version 1 never carried deleted rows, so it can hold sessions
  of a tea or vessel deleted before it was made. A row whose tea, type or vessel is in neither the
  backup nor the app is left out with what hangs off it, and the result says how many. The recovery
  is a fresh export from 0.3.0.
- **Last-writer-wins per row on `updatedAt`; a tie is skipped**, so importing twice changes nothing.
  Devices aren't synced, so each one's timestamps only move forward and per-row is consistent.
    - **Every write stamps `updatedAt`, soft deletes included.** Today `softDelete` sets only
      `deletedAt`, so a newer backup's delete would tie and be skipped.
    - **Seeded tea types and vessels are created at epoch 0**, so any real row beats an untouched
      seed and edits to defaults survive a fresh-device restore. Existing installs are unaffected.
- **Import runs in order:** plan → copy the photos winning rows need → one transaction (re-plan,
  upsert, repair, recompute stats) → preferences → temp cleanup. A failure leaves the database
  untouched and deletes the photo files that import created.
    - The plan is a pure `planMerge(incoming, local)`: insert, update or skip per row.
    - **Repair** is F3's v7 → v8 repair SQL, moved into `TeaSessionDao` like the stats SQL and
      shared with the migration: a deleted tea with live sessions comes back finished, a used vessel
      archived, a used type restored, orphaned methods soft-deleted. Like stats it is derived, so it
      doesn't stamp `updatedAt`.
    - A backup's `IN_PROGRESS` session lands `COMPLETED` with its timer fields cleared: its timer is
      gone, and two in-progress sessions would break the current-steep rule. A local in-progress
      session is never touched.
- **Photos are content-addressed on the device.** Import streams each needed entry into a temp file
  while hashing it (okio `HashingSink`), then moves it to `photos/<sha256>.<ext>`, or drops it if
  that file exists. V1 and V2 archives share the path; export doesn't hash. Imports stop writing
  `tea_photos/`. Existing photos keep their names.
- **Files through an injected okio `FileSystem`**, with paths from `ImageStorage`, which already
  resolves stored paths (iOS's legacy absolute ones included) and the cache dir. Round trips run on
  the host JVM: the Android `ImageStorage` over a temp dir, and the in-memory Room database.
  `DataExportRepository` becomes `BackupRepository`; Settings calls it directly.
- **Preferences travel in the backup and apply only when the database has no tea rows** (a
  fresh-device restore). They carry no timestamp, so importing into a device with data leaves its
  settings alone.
- **Import preview:** pick → plan (read-only) → dialog → Confirm. It shows the backup date,
  "N new · M updated · K removed · rest unchanged" across all records, and whether settings will be
  restored. Rows that arrive already deleted don't count as new. The result reports the same
  numbers, not rows in the file.
- **"Last backup: 42 days ago"** (or "Never") in Settings' Data section, recorded only when the file
  is delivered: Android's SAF write succeeded, iOS's share sheet completed. Past **14 days**
  it turns to the error colour with "Back up now". Nothing on Home: no nagging.
- **Photo safety** (previous 2.6):
    - `ImageStorage.saveImage` downscales every photo added to the app to a 2048px long edge, JPEG
      quality 85, so `.jpg` is finally true for HEIC and PNG picks. Existing photos aren't
      re-encoded.
    - Android decodes at a power-of-two `inSampleSize`, then scales and applies EXIF orientation in
      one matrix, read with androidx `ExifInterface`: `BitmapFactory` ignores orientation,
      `ImageDecoder` needs API 28 (minSdk is 24), and lint flags the framework `ExifInterface` below
      API 25. iOS uses FileKit's `compressImage`, which draws through `UIImage` and comes out
      upright.
    - Save failures reach the user: `EditTeaViewModel.addPhoto` is unguarded, and iOS ignores
      `writeToFile` returning false, storing a path to a missing file.
    - Settings shows the storage photos take.
- **Settings' data section clears its audit findings** (Rule 1): the whole-file copy on the main
  thread, `pendingFilePath` lost on recreation, the filename remembered at screen open, iOS
  `onExported` firing on presentation and ignoring copy errors, deprecated `keyWindow`, and no
  success confirmation.

**Migration:** none. No schema change.

**Exit:** Track A round-trips for: full export → wipe → import; the old Android archive read by the
common reader; a finished tea with sessions restored to an empty DB, still finished with its
sessions named; a version 1 backup onto a device that still has the tea deleted brings it back
finished, and sessions whose tea is in neither are skipped and counted; import twice = no change;
older backup over newer data = newer data wins. The F2 plan promised an export → import round trip
that was never written; these are the first.

---

## Bet 1 — Design system & app shell

**Outcome:** there is a target to polish toward. Today the app is stock Material with a palette:
stock `Typography()`, no `Shapes`, baseline fallbacks for unset colour roles, two icon families,
five error-display patterns, four copies of the same dialogs. Bets 2–3 rewrite most screens; doing
this first means they're built once, in the final style.

Scope is **the system, not a restyle of every screen.**

**Design** (decided 2026-10-15):

- **Track S goes live first**, so every later commit proves its previews still render.
    - ComposablePreviewScanner reads the `@Preview`s in `commonMain` and a parameterized test
      renders them on Robolectric in `androidHostTest`.
    - Every preview renders three ways — light, dark, 2× font scale — so no preview is edited and
      new ones are covered for free. Long-text cases are hand-written previews.
    - No goldens; a preview fails only by crashing. Considered: Roborazzi goldens in git, dropped
      2026-10-15 after the first cut committed 531 PNGs.
    - Previews are deterministic: the ten that call `Clock.System.now()` take a fixed instant, so
      the IDE shows the same "3 days ago" every day.
- **Identity:**
    - **Figtree** (OFL, bundled as compose resources, static weights) for display, headline and
      title styles. Body and labels stay system, for density. Timer and parameter digits use tabular
      figures.
    - A `Shapes` scale (4/8/12/16/28dp) replaces the ad-hoc `RoundedCornerShape`s.
    - The colour scheme is re-exported from the same seed (`#4C662B`), so the palette stays and
      every role is set. No dynamic colour.
    - **Tea-type colours stay user-picked and are toned per theme** by a pure
      `teaTypeColor(hex, dark)` that clamps lightness. Text on them uses `Color.luminance()`,
      replacing the three hand-rolled "is this light?" checks. Stored data doesn't change.
      The picker's Material stock swatches become twelve muted tea colours (Matcha, Maroon,
      Pu-erh, …), spaced so none look alike once toned; new installs seed from them.
    - Dark-mode splash: a `values-night` theme with `windowBackground` on Android (no
      `core-splashscreen`), a launch colour asset with a dark appearance on iOS.
    - **One icon family: Feather**, plus filled star, half star and filled heart drawn in its style.
      FontAwesome existed only because Feather is outline-only; it's removed.
- **Theme mode: Light / Dark / System**, in a Settings Appearance section with the units. It travels
  in the backup's preferences block as a nullable field (null = System). The root renders a plain
  background until preferences first load, so the app never flashes the wrong theme; the OS splash
  still follows the system. System bars follow the chosen mode on both platforms.
- **Units: Settings owns the default; forms can toggle locally.** The toggle is a feature. Its bugs
  came from where its state lived: forms wrote the global preference, cleared typed text to
  "reconvert from storage", converted from rounded display text, and validated in whichever unit was
  showing — once per ViewModel.
    - **One `UnitInputField`** for temperature, volume and weight, replacing the Temperature /
      Volume / Capacity variants. It owns its text and toggled unit — CLAUDE.md's `DurationPicker`
      rule, "extract the logic, not the state". ViewModels hold `Double?` in °C / ml / g and lose
      their toggle intents and string fields.
    - A form reads units once when it opens. Per-tab back stacks let a form stay open while Settings
      changes in another tab; typed text must not change meaning.
    - The field keeps the exact canonical value and re-renders from it on a toggle, so flipping
      never drifts (85 °C → 185 °F → 85 °C). **Toggling alone never changes stored data:** the field
      reports a value only when typed, and only if it differs at display precision from the value it
      opened with. This absorbs F2's `editedUnitValue`.
    - A toggle lasts until the form closes (saveable), then resets to Settings. Nothing remembers
      it — that would be a hidden second preference.
    - The suffix is a chip with a swap glyph, a 48dp target and a spoken label; it takes the
      tertiary colour when it differs from Settings.
    - Precision: whole °C/°F and ml, one decimal for fl oz and g, two for oz. Input accepts that
      many decimals. Presets follow the field's current unit; ounce leaf presets are 0.05 / 0.1 /
      0.15 / 0.2 / 0.25 / 0.35 / 0.5.
    - Validation ranges are in canonical units, in the ViewModel.
    - **The weight unit is honoured everywhere** and gets a Settings row. A `LocalUnits`
      CompositionLocal, provided at the root, feeds the formatted-value components, so display-only
      screens stop carrying `userPreferences` in State. This replaces CLAUDE.md's "derivations that
      depend on `userPreferences` stay in the screen".
    - Display toggles (Analytics totals, Home's stat tile) become the same local chip: they last
      while on the screen and never write the preference.
- **One message surface.**
    - A small `UiMessages` class per ViewModel holds its pending messages as state, as Android's
      UI-events guidance has it: a message stays until the screen reports it shown, so one cut short
      by rotation or navigation is shown again. One `UiMessageEffect` per screen feeds the
      `Scaffold`'s `SnackbarHost`. No base ViewModel.
    - A `UiMessage` carries a string or plural resource plus args, resolved by the screen, so
      ViewModels stay locale-free and tests assert on the resource. It may carry an action label and
      an intent to send back; Home's Undo moves onto it, and the polish track's Undo-for-deletes is
      then purely UI.
    - Typed domain failures — `NotFound`, `InUse(n)`, `LastRemaining`, `Io`, and the backup's
      `NotABackup` and `NewerBackup` — mapped to copy in the UI. They replace the copy `DeleteBrewingVesselUseCase` puts in the domain, and the tea, vessel
      and tea-type delete guards throw them. Tea types can no longer lose their last type.
    - Anything else reads "Couldn't <action>", and the exception goes to Kermit's log.
    - A failed load shows the snackbar over the screen's empty state; there's no retry, because a
      local database failing is a bug, not a flaky network. Success messages ("Backup exported")
      use the same queue. Field validation stays inline as `supportingText`.
- **Strings and dates.** Compose resources' `strings.xml` and plurals. What moves in Bet 1 is what
  Bet 1 writes or edits, plus every plural bug ("1 brews", "Used 1 times", "(s)"); screens migrate
  as later bets rewrite them. One `expect`/`actual` formats a `LocalDate` by skeleton (`MONTH_DAY`,
  `MONTH_DAY_YEAR`, `MONTH_YEAR`, …): Android's `DateFormat.getBestDateTimePattern`, iOS's
  `setLocalizedDateFormatFromTemplate`. It replaces the hardcoded `"MMM d"` patterns and the three
  month-name arrays, with no java.time (minSdk 24). `GenerateInsightsUseCase`'s sentences wait for
  Bet 4.
- **Components**, each once, with a modifier and previews:
    - `ConfirmDestructiveDialog`, and `DiscardChangesGuard(hasChanges, onDiscard)`, which owns the
      back handler as well as the dialog, so adopting it fixes system back silently discarding Edit
      Tea Type and Edit Session.
    - `SectionHeader`, and `EmptyState` with an icon slot.
    - `RatingDisplay`: stars to the nearest half, one trimmed decimal ("4.3", "4"), "Rated 4.3 out
      of 5" to a screen reader, a muted "Unrated" for null — never zero stars. `RatingSelector` gets
      value semantics; Bet 2's review is built on it.
    - Formatted temperature, volume and weight values, reading `LocalUnits`.
- **App shell**, on the Nav3 multiple-back-stacks recipe: one `NavBackStack` per tab, each with its
  own decorators, shown as `[Home stack] + [current tab stack]`, so hidden tabs keep their
  ViewModels (see Hard-won facts).
    - Back from a tab's root returns Home; back from Home exits. Re-tapping the current tab pops it
      to its root.
    - The bottom bar shows across a tab's stack and hides on editors and the brew loop.
    - The brew loop and notification deep links always use Home's stack; the origin tab keeps its
      own.
    - A destination with no args (Home → Collection) switches tabs. One with args (Tea Detail →
      History for this tea) pushes onto the current stack with an Up arrow, leaving that tab's own
      search and filters alone.
    - One `navigateUp()` that pops only when the calling entry is on top and isn't a root.
      Fade-through for tab switches, slides for push and pop. `MoreRoute` is deleted.
    - Route args reach ViewModels through `parametersOf(route)` (`viewModelOf` resolves a
      route-typed constructor param) and load in `init`, killing the `LaunchedEffect`-reload bug
      class: edit forms re-seeding over user input, stacking collectors, re-applied filters.
- **Not here:** tablet layouts (Store readiness).

**Exit:** every shared component exists once, with previews Track S renders; no screen renders an
operation failure any other way; no raw exception text; a unit flipped any number of times saves
what was typed, and an untouched one saves nothing; tab navigation behaves per the shell rules;
Track B lines for theme mode and the dark splash on both platforms, the iOS edge swipe, and tab
state across rotation and process death.

**Commits.** Each compiles, passes tests and leaves the app working. 7 → 9–13; 6 → 13; 14 → 15 → 16.

- [x] `docs: record the Bet 1 design`
- [x] `test: render every preview`: the preview scanner, the three-way render test, fixed-clock
  previews. Proves compose resources render under Robolectric before anything depends on it.
- [x] `feat: Figtree type scale and shapes`
- [x] `feat: set every colour role and tone tea-type colours`, with the dark splash. Test:
  `teaTypeColor` on both themes.
- [x] `feat: choose light, dark or system theme`. Test: the backup round trip.
- [x] `refactor: one icon family`
- [x] `feat: string resources and plurals`
- [x] `feat: locale-aware dates`
- [x] `feat: one message surface`: `UiMessages`, `UiMessageEffect`, the typed failures and the
  guards that throw them, Home's Undo, and the tea library and Settings screens. Test: failure →
  message mapping.
- [ ] `refactor: messages on the brew loop and journal`: the remaining screens; every
  `error: String?` goes.
- [ ] `feat: shared confirm and discard dialogs`
- [ ] `feat: section header and empty state`
- [ ] `feat: rating display and selector`. Test: half-star rounding.
- [ ] `feat: one unit input field with local toggles`: the component and its pure functions, then
  Edit Tea, Edit Vessel, Edit Tea Type and Brewing Method. Tests: flips round-trip, 93 °C, decimal
  input, an untouched field reports nothing.
- [ ] `refactor: brew and journal forms on the unit field`: Log Tea, Edit Session, the parameter
  sheets, the next-steep dialog, Quick Timer. `editedUnitValue` and the old variants go.
- [ ] `feat: honour the weight unit everywhere`: `LocalUnits`, the Settings row, every hardcoded
  `"g"`, the local display toggles, the CLAUDE.md rule rewritten.
- [ ] `refactor: load route args in ViewModel init`. Test: History applies its route filter once.
- [ ] `feat: per-tab back stacks`. Test: the navigator — back from a tab root, re-tap, the
  double-pop guard, switch vs push.
- [ ] `chore: release 0.4.0`: audit rows struck.

---

## Bet 2 — Brewing plans

**Outcome:** the app knows how each tea is brewed, and a gongfu session costs one tap per steep plus
whatever you choose to record.

Today: a saved method holds one `brewingTime`; every extra steep costs Next → dialog → Confirm →
Timer → Start (about three taps and two screens), and each steep's time is typed by hand.

### Design

- **A method is a plan.** `BrewingConfiguration` gains `steepIncrement: Duration?` (null = a single
  western steep) and optional per-steep overrides. Steep N's time is
  `base + (N−1) × increment` unless overridden. The style lives on the method, not the tea — a tea
  can have a gaiwan-gongfu method and a mug-western method.
- **Plans are learned.** Finishing a multi-steep session offers "Save as method", which captures the
  observed base and increment, with overrides where you deviated. (Today multi-steep sessions are
  *never* offered this — the prompt only fires on steep 1.) Manual create/edit still exists.
- **The timer waits for Start.** Pouring takes time; auto-start was considered and rejected.
- **Rinse** is recorded, excluded from ratings. See open questions for how.
- **The slim review** between steeps: stars, carried-forward tags, a collapsed note affordance, and
  a primary **"Next · 25s"** that goes straight to the timer at the planned time — no parameter
  dialog; a small "adjust" for when you deviate. "Finish session" is secondary. The full review,
  including best-brew comparison (previous 2.1), appears once, at session end.
- **Per-steep tasting tags.**
    - A curated vocabulary of ~20–25 tags grouped as aroma / taste / body / finish, plus the user's
      own custom tags (typeahead against existing tags to prevent near-duplicates).
    - **Carry forward, edit the delta:** steep N+1 starts with steep N's tags pre-selected but
      *unconfirmed*. Touching a tag confirms it. Unconfirmed tags are stored as such and read as
      weaker evidence. Zero taps when nothing changed; the record shows how flavour evolved.
    - Tables: `tasting_tag(id, name, group, isCustom, displayOrder, deletedAt)` and
      `session_tag(sessionId, tagId, confirmed)`. Included in the F4 export DTOs.
- **Live Activity and the ongoing notification show the plan** — "Steep 3 · 25s · next 30s". Their
  Next action opens the app to the slim review (rating capture stays in the loop).
- **Quick Timer becomes "timer now, tea later."** The user who starts pouring before remembering to
  set a timer must not be forced through adding a tea — but should be invited to log.
    - It runs on `TimerService` as a timer with no session (`TimerState.sessionId` and `teaId` are
      already nullable), persisted in preferences so it survives process death. It inherits the
      foreground service, notifications, Live Activity, and settings toggles.
    - Same Timer screen, with an optional "What are you brewing?" chip opening `TeaPickerSheet`
      (recents, quick-add) during the countdown.
    - On completion: tea attached → it becomes a real session and goes to the slim review. No tea →
      one prompt: pick a tea to log it, or "Just a timer".
    - "Next · +5s" works without a tea, for gongfu by feel.
    - Deletes `QuickTimerViewModel`'s engine, the duplicate screen, and the details sheet.
- **Brew Again** replays the whole plan, not steep 1.
- The between-steep flow clears the brew-loop audit findings (report 1) for every screen it
  rewrites.

**Migration:** v8 → v9 — plan columns on `brewing_configurations`, the two tag tables, seeded
curated vocabulary.

**Exit:** a 10-steep gongfu session from a saved plan takes one tap per steep to advance; a western
brew from a pinned method takes two taps to a running timer; Quick Timer and Timer are one engine
and one screen; tags carry forward and round-trip through backup; Track B lines for Live Activity
and the timer-without-tea across process death.

### Open questions

- **Rinse representation.** The parent row is both the `Session` and steep 1. A rinse as a
  `steepNumber = 0` row breaks that invariant. Recommended: a `rinseDuration: Duration?` on
  `Session` instead of a row.
- **Per-steep temperature** in plans (some gongfu raises temperature across steeps). Recommended:
  overrides may carry temperature; base plan does not.
- **Curated vocabulary contents** — draft the list before the migration.
- **Learning from sessions where the timer was adjusted** — use actual brew time (F2) as the
  observed value.

---

## Bet 3 — The journal

**Outcome:** looking back is pleasant and useful in the three ways the author actually rereads.
Needs Bet 2's tag data to have something to show.

- **"Last time" on Log Tea and quick-brew cards.** After choosing a tea: date, parameters, rating,
  dominant tags, first line of note — at the moment parameters are being chosen.
- **Tea Detail tells the tea's story** (previous 2.2, expanded): sessions listed as sessions (not
  steeps), best brew under the F2 rating rule, the steep curve (where this tea peaks), a flavour
  profile from tags, parameters-vs-rating over time, methods with provenance ("From your 12 Mar
  brew, ★4.5" — `sourceSessionId` is stored and never shown), and cellar age from
  `purchaseDate` (collected, never displayed). Empty states for zero sessions.
- **History, as a reading layout** (previous 3.1, revised):
    - **One card per session**: steep count, first-to-last span, per-steep rating dots, dominant
      tags. Parameters recede. Child steeps stop masquerading as separate brews.
    - Group per day for the last 30 days, per month beyond. Keep "Today"/"Yesterday".
    - Day header only on days with 2+ sessions: teas drunk, and first-to-last cup span. No session
      counts, no "standout brew" — judgement lives on Tea Detail.
    - Month headings carry no summary (that's Recap's job).
    - Filters or search active → flat list, no summaries.
    - Search covers tea, notes, tags, location, vessel, producer. Filters combine with search.
- **Session Detail as a steep timeline:** each steep's clock time, interval since the previous, what
  changed, rating and tags inline. Session-level note. Editable date/time and tea. Previous/next
  session navigation.
- **Collection search** gets full-text over name, producer, origin, description, combinable with
  filters.
- **Richer notes** (previous 3.3): prompts on the end-of-session review, seeded by the tags you
  chose. Freeform entries detached from sessions are **rejected** (§ Decisions).

**Exit:** each of the three rereading jobs has a surface that answers it in under two taps from
where you'd want it; the journal audit findings (report 2) for History, Session Detail and Edit
Session are cleared.

---

## Bet 4 — Recap

**Outcome:** retrospection as an occasion. A dedicated, paged recap — "Your autumn in tea", later
"Your year in tea". The one retrospective surface worth opening, and the portfolio's showpiece
screen.

- **Prerequisite: Analytics refactor** — one query per load over `[previousStart, end)`, then pure
  aggregator functions. Kills the ~14 sequential full-table queries on All Time, moves work off
  Main, and makes every date-boundary bug a Track A test. Analytics becomes a Flow so it stops going
  stale.
- **Content:** most-brewed teas, new discoveries, how the flavour profile shifted (tags),
  steep-curve habits, the best session, finished teas.
- **Gating:** build the mechanism once (a date window); gate each surface on data. With 6–12 months
  logged: season recaps ship now; Year in Tea ships gated to the first full year; "on this day"
  waits for year two.
- **Dormant teas** ("you haven't brewed X in a while") land here, excluding finished teas (F3).
- **Analytics ↔ journal links:** heatmap day → History for that day; donut segments *and legends*
  → History with the current period; vessel slice → Vessel Detail.

**Exit:** a season recap renders from real data; every aggregator has host tests including week,
month and year boundaries.

---

## Bet 5 — Widgets

Android Glance + iOS WidgetKit. Portfolio grounds: two native implementations demonstrate platform
depth that shared Compose code can't. Neither shares code.

Scope: today at a glance, start a pinned method, in-progress status. iOS's Live Activity already
covers the in-brew case, so the iOS widget's value is the glance and the launch.

**Open question:** whether "start a pinned method" from a widget can start the timer directly (App
Intents on iOS; a broadcast into `TimerService` on Android) given the timer waits for Start.

---

## Polish track (continuous)

Fed by `docs/audit/`. Runs alongside the bets, never as a phase of its own.

- **Rule 1:** a bet that rewrites a screen clears that screen's audit findings.
- **Rule 2:** findings on screens no bet touches (Vessels, Tea Types, Settings, Edit Tea, Brewing
  Method form) are worked in small batches between bets. Priority: bug > friction > consistency >
  polish.
- **Rule 3:** anything accessibility-related follows the definition of done (§3).
- **Undo for destructive actions** (previous 2.4) lives here once F3 lands — it is purely UI then.
- **Completion signal** (previous 2.5, reframed): the foreground signal exists but only as the
  system notification, so it inherits the weak tone and vanishes when the in-app toggle is off. Add
  an in-app haptic and chime independent of notification settings, and a new notification channel id
  with vibration for brew-complete only (channels are immutable — see Hard-won facts). Blocked on an
  audio asset.
- Smaller carried items: `WaterType.OTHER` has no description field.

---

## Store readiness

Gated. Nothing ships to strangers until these are done. Blocks nothing before then.

- [ ] **Crash reporting.** Sentry's KMP SDK (Crashlytics would drag in the Firebase dependency the
  sync decision rejected).
- [ ] **First-run experience.** A stranger opens to an empty Home. Also: notification permission is
  requested in the first `onCreate` with no context — ask at first timer use, as iOS does.
- [ ] **Store blockers from the audit:** `PrivacyInfo.xcprivacy` (NSUserDefaults is a
  required-reason API), `FOREGROUND_SERVICE_SPECIAL_USE` Play declaration, `isMinifyEnabled = true`,
  `ITSAppUsesNonExemptEncryption`, widget display name, `dataExtractionRules`.
- [ ] **About screen:** version, licences, privacy policy, support contact.
- [ ] **One TalkBack and VoiceOver pass** over the whole app.
- [ ] Store listings and screenshots.
- [ ] **Large-screen layouts.** Android 16 ignores the portrait lock on large screens, so tablets
  see landscape regardless.

---

## Later, and not doing

| Item                                                 | Status                                                                           |
|------------------------------------------------------|----------------------------------------------------------------------------------|
| Firebase / multi-device sync                         | Deferred — § Decisions                                                           |
| Watch app (Wear OS / watchOS)                        | Not doing for now — the Live Activity and notification cover the wrist-free case |
| Label scan (OCR) for Quick Add                       | Not doing for now                                                                |
| Localization (translations)                          | Later — Bet 1 makes it possible; no languages planned                            |
| Freeform journal entries                             | Rejected — § Decisions                                                           |
| Photo features (hero, gallery, camera, cover choice) | Rejected — § Decisions                                                           |

---

## Decisions

Recorded so future sessions don't re-argue them. Each was decided against a real alternative.

| Decision                                                                            | Reasoning                                                                                                                                                                                                                                                                                 |
|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sync is deferred, not planned**                                                   | Backup is covered by export/import (made trustworthy in F4). Multi-device sync matters only if you brew from two devices, which you don't. Sync costs auth, a backend, conflict resolution, a privacy policy, and ongoing money. `SyncStatus` and `userId` can sit unused.                |
| **Purposes stay ranked, public launch stays undated**                               | Revisited 2026-09-23. Personal tool first; a date would let strangers' needs override the author's.                                                                                                                                                                                       |
| **Thin foundation, then bets**                                                      | Only what every bet stands on goes first. Other foundations are paid for by the first bet that touches them. Considered: all foundations first (weeks of invisible work, some shaped better by features) and features first (built on the six-way session logic).                         |
| **Design system before brewing plans**                                              | Bets 2–3 rewrite most screens; building them after the system means building them once.                                                                                                                                                                                                   |
| **Only a user tap stores a rating**                                                 | Four code paths wrote ratings the user never gave, and one fed back into itself. Unrated steeps stay null and are excluded from averages and best brew. Cost: less rating data. Worth it: the data means something.                                                                       |
| **Stored derived stats are recomputed, never incremented**                          | Tea and configuration stats are rebuilt from sessions inside each mutation's transaction. Increments drifted: `timesUsed` rose once per steep and never fell on delete. Recomputing makes re-finish, Undo and delete correct with no special cases. Session ratings aren't stored at all. |
| **Discarding steep N ≥ 2 finishes the session**                                     | Steeps 1…N−1 happened; only the next one was abandoned. Discarding steep 1 deletes the session. Considered: discarding the whole session from any steep.                                                                                                                                  |
| **Undo is the only way to re-open a finished session**                              | An accidental Finish needs a way back; re-opening old sessions doesn't. Session Detail's Add Steep records a forgotten steep without a timer instead.                                                                                                                                     |
| **A brewing style lives on the method, not the tea**                                | A tea brewed both ways needs two methods; the vessel usually implies the style.                                                                                                                                                                                                           |
| **Plans are base + increment with overrides**                                       | Matches how the author actually brews gongfu.                                                                                                                                                                                                                                             |
| **Timer waits for Start**                                                           | Pouring takes time. Auto-start considered and rejected.                                                                                                                                                                                                                                   |
| **Review stays between steeps, slimmed**                                            | Considered: rating during the next countdown (no screen at all) and session-end-only (memory fades by steep 10). A one-screen stars + tags + Next keeps capture high at one tap.                                                                                                          |
| **Lock-screen Next opens the review**                                               | Considered: advancing from the lock screen with the rating left null. Rejected to keep rating capture in the loop.                                                                                                                                                                        |
| **Tags are per steep, carried forward unconfirmed**                                 | Per-steep shows how flavour evolves; carry-forward makes it cost zero taps when nothing changed; "unconfirmed" keeps it honest. Vocabulary is curated plus custom.                                                                                                                        |
| **Quick Timer is "timer now, tea later" on the one engine**                         | Keeps the no-friction entry point the author values, deletes the second timer engine the audit found weak. Considered: nullable-tea draft rows in the database (a migration and nullable tea everywhere) and keeping the separate screen.                                                 |
| **Teas with sessions are finished, never deleted**                                  | The journal must never lose a name, and deleting caused "Unknown Tea" and unrestorable backups. Delete remains for zero-session teas.                                                                                                                                                     |
| **`purchasePrice` is deleted, not built**                                           | Cost-per-cup analytics is a feature nobody asked for.                                                                                                                                                                                                                                     |
| **Stock is `finishedAt`, not a quantity**                                           | Marking "I ran out" is the whole need, and it's what feeds the dormant-tea prompt.                                                                                                                                                                                                        |
| **Finished teas stay in the Log Tea picker**                                        | Hiding them makes the auto-clear unreachable.                                                                                                                                                                                                                                             |
| **Host-JVM tests; smoke tests from previews; no interactive Compose UI suite**      | Revisited 2026-10-15. Rendering every mandatory preview catches crashes without an emulator. Interaction suites still aren't worth their brittleness. Considered: E2E flows, which need an emulator and duplicate Track B.                                                                |
| **Accessibility basics are definition-of-done, not a launch gate**                  | Revisited 2026-09-23. Cheap per touch; the rating control — the loop's main input — is unusable with a screen reader today. One full screen-reader pass before store launch.                                                                                                              |
| **Photos get safety, not features**                                                 | The author doesn't reread photos. They matter only because unchecked they break backups and storage.                                                                                                                                                                                      |
| **Freeform journal entries are rejected**                                           | The author rarely writes notes attached to sessions; detached entries would be emptier. Tags and session notes cover the journal.                                                                                                                                                         |
| **History grouping revived; the rest of the stashed redesign stays dead**           | Three of its five summary fields were Analytics content in a History costume.                                                                                                                                                                                                             |
| **No streaks**                                                                      | An engagement mechanic that says nothing about tea. The shipped streak insight is removed in F1.                                                                                                                                                                                          |
| **The session reminder stays inexact**                                              | `setAndAllowWhileIdle`; Doze spaces these ~9 minutes apart. Not worth the exact-alarm allowance for a soft nudge. The fix if it annoys is `setExactAndAllowWhileIdle`, mirroring the completion alarm.                                                                                    |
| **Notifications get two switches, not one**                                         | Turning off the soft nudge must not kill the alert the app exists to deliver.                                                                                                                                                                                                             |
| **Previews are smoke-tested, not screenshot-tested**                                | Pixel parity isn't the goal, and goldens churn hundreds of PNGs into git on every theme tweak. ComposablePreviewScanner reads `commonMain` previews under the KMP library plugin. Considered: Roborazzi goldens in git, Git LFS, CI-only baselines.                                       |
| **Unit toggles stay, local to the field**                                           | Converting a vendor's instructions in place is worth having. The bugs came from the toggle writing the global preference and living in every ViewModel, not from the toggle. Considered: Settings as the only way to change units.                                                        |
| **The weight unit is honoured everywhere**                                          | A setting that one Analytics card obeys is worse than none. Considered: grams only, deleting the preference — leaf is weighed in grams nearly everywhere.                                                                                                                                 |
| **Display units come from a CompositionLocal**                                      | Honouring weight everywhere would otherwise thread preferences through every State that shows a value. Considered: formatting in the screen and passing strings down, the earlier rule.                                                                                                   |
| **Theme mode is a setting**                                                         | Considered: following the system only. It travels in backups like the other preferences.                                                                                                                                                                                                  |
| **Headings are Figtree; body stays system**                                         | A friendly sans gives the app its own voice without formality; system body keeps dense screens scannable. Considered: Literata, tried and dropped as too formal; Nunito, DM Sans.                                                                                                         |
| **The brew loop lives on Home's stack**                                             | Notification taps and Home's banner already open the Timer there. Pushing it onto the origin tab could leave two Timers in two stacks.                                                                                                                                                    |
| **Tablet layouts wait for Store readiness**                                         | The author uses a phone, and nothing ships to strangers before then.                                                                                                                                                                                                                      |

---

## Hard-won facts

Things that cost real debugging time and would cost it again. Not history — traps.

- **`NotificationChannel` sound, importance and vibration are immutable after creation.**
  `timer_complete` exists on installed devices; changing it in code is silently ignored. Any tone or
  vibration change needs a **new channel id**.
- **`TimerService.stop()` must not cancel the session reminder.** `stop()` means "timer torn down";
  a steep finishing is exactly when review becomes pending. Withdrawal goes through
  `TimerService.onSessionResolved(sessionId)`, guarded so resolving an old session can't cancel a
  live brew's reminder.
- **iOS Live Activity: use `Text(timerInterval:countsDown:)`, never `Text(_:style:.timer)`.** The
  latter keeps ticking upward once its date passes.
- **Never push a Live Activity update as a side effect of cancelling an alarm.** It froze running
  timers into "Paused". Pausing is explicit via `TimerNotificationService.showTimerPaused(state)`.
- **Derive in-progress status from wall-clock in exactly one place** —
  `Steep.resolvedTimerStatus(now)`, on the session's current steep. Gate live progress on matching
  session id. Known residual:
  a Home screen left open with no live timer won't re-resolve from time passing alone.
- **Room KMP does not support enum parameters in raw `@Query` SQL.** The `'COMPLETED'` /
  `'IN_PROGRESS'` string literals in DAOs are correct.
- **Process-kill edge case, unfixed:** if the process is killed and the session is completed via the
  conflict dialog, the reminder fires once for a finished session. Android could check status in the
  receiver; iOS can't.
- **Nav3 keeps an entry's ViewModel while the entry is on the back stack, but disposes its
  composition.** Any `LaunchedEffect` that loads data re-runs on return and on Activity recreation,
  against a ViewModel that already has state. Load from `init` with the route key.
- **Tab switching with `backStack.clear()` destroys every other tab's ViewModel.**
- **`NSCalendarUnitDay` is day-of-month.** Comparing year + day says Aug 23 is "today" on Sep 23.
  Compare full dates.
- **Kotlin `DayOfWeek.ordinal` is Monday = 0.** `today − (ordinal + 1)` on a Sunday lands on the
  previous Sunday and excludes today.
- **`PendingIntent` identity ignores extras.** Notifications sharing requestCode 0 and the same
  Intent all route to whichever was built last (suspected; confirm on device).
- **A `Snackbar` placed after a `fillMaxSize()` sibling in a `Column` gets zero height** and is
  never seen. Use `SnackbarHost` in a `Scaffold`.
- **Room `@Upsert` onto an existing row fails in host tests** with a bare `SQLException`: the
  mockable android.jar drops the message Room reads to spot the key conflict. The
  `androidHostTest` copy of `android.database.SQLException` fixes it; don't delete it.
- **Rebuilding a table in a migration: rename the old one away before creating and copying into the
  new one**, so dropping the old one can't cascade into the new one (v6 → v7).
- **okio's `FileSystem.SYSTEM` isn't visible from commonMain** (`openZip` is). The platform modules
  provide it.
- **Robolectric SDK 36 needs JVM module flags on JDK 21**; `PreviewRenderTest` runs on 35.
  Roborazzi's `generateComposePreviewRobolectricTests` also breaks the host tests' classpath: its
  `afterEvaluate` check reads dependencies before KMP wires commonMain into `androidHostTest`.
- **Never call `MutableList.removeLast()` / `removeFirst()`.** With compileSdk ≥ 35 they may bind to
  JDK 21's `List.removeLast()`, absent below API 35. Use `removeAt(lastIndex)` / `removeAt(0)`.

---

## Working items

The daily-friction inbox. Low ceremony by design — dump the annoyance while you feel it, and promote
it into a bet or the polish track when it's understood. One line per issue: what's annoying, then
what it should do instead. Sub-bullets only for decisions a later session needs.

Working an item: take the first unchecked one, check it off when it's done.

**Format:**

```
- [ ] Short title — the annoyance. **Should:** what it does instead.
    - decision/context worth carrying forward (optional)
```

### Open

- [ ] Device pass notes from the 2026-09-23 audit — **Should:** be dumped here as they're found,
  then triaged into F1, a bet, or the polish track.
- [ ] "Tea is ready!" outlives its steep — the completion alert (`COMPLETION_NOTIFICATION_ID`)
  stays in the shade after Next, Finish or Discard; nothing in code ever cancels it, only a tap
  (`AUTO_CANCEL`) clears it. **Should:** be withdrawn once the steep it announced is resolved.
    - found on the emulator 2026-10-13; predates F2, present at `5cb87a1`
- [ ] Home banner bar frozen at the last save — with no live timer (app relaunched mid-brew), the
  bar falls back to the stored `timerRemainingMs`, so it shows progress as of the last save, not
  now. **Should:** derive elapsed from `timerStartedAt` the way `Steep.resolvedTimerStatus` does.
    - same family as the Hard-won facts residual (Home doesn't re-resolve as time passes); fix both
      together
    - found on the emulator 2026-10-13; predates F2
- [ ] Test files split per function — `SortTeasTest` tests one function pulled out of
  `TeaCollectionViewModel.kt`, and others may be the same. **Should:** a source file's tests all
  live in one test file named after it (`TeaCollectionViewModelTest`), not one file per function.
    - audit every test file; noted during F3 on 2026-10-14
