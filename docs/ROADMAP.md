# Leaf Log — Roadmap

> **Last updated:** 2026-10-23 (Bet 4 designed)
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
| Tea Detail | Per-tea judgment — best brew, steep curve, flavor profile, how a tea has evolved   |
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
- **Not an engagement machine.** No streaks, no nagging, no guilt colors on "fewer sessions".

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
wrong screen, the reminder canceled inside `stop()` — were cross-process, wall-clock,
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
> labeled, 48dp targets, custom Canvas given a semantic summary, controls given value/selected
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
    - Semver, bumped by judgment (not tied to milestones), starting at `0.1.0`.
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
    - Collection: sorted last under every sort option, dimmed, labeled "Finished" (dimming alone is
      color-only meaning), still searchable.
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
  fields added since schema 6) and migrated to V2, the pre-v7 child normalization with it. A newer
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
  it turns to the error color with "Back up now". Nothing on Home: no nagging.
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

**Done, released as 0.4.0.**

**Outcome:** there is a target to polish toward. Today the app is stock Material with a palette:
stock `Typography()`, no `Shapes`, baseline fallbacks for unset color roles, two icon families,
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
    - The color scheme is re-exported from the same seed (`#4C662B`), so the palette stays and
      every role is set. No dynamic color.
    - **Tea-type colors stay user-picked and are toned per theme** by a pure
      `teaTypeColor(hex, dark)` that clamps lightness. Text on them uses `Color.luminance()`,
      replacing the three hand-rolled "is this light?" checks. Stored data doesn't change. The
      picker's Material stock swatches become twelve muted tea colors (Matcha, Maroon, Pu-erh, …),
      spaced so none look alike once toned; new installs seed from them.
    - Dark-mode splash: a `values-night` theme with `windowBackground` on Android (no
      `core-splashscreen`), a launch color asset with a dark appearance on iOS.
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
      tertiary color when it differs from Settings.
    - Precision: whole °C/°F and ml, one decimal for fl oz and g, two for oz. Input accepts that
      many decimals. Presets follow the field's current unit; ounce leaf presets are 0.05 / 0.1 /
      0.15 / 0.2 / 0.25 / 0.35 / 0.5.
    - Validation ranges are in canonical units, in the ViewModel.
    - **The weight unit is honored everywhere** and gets a Settings row. A `LocalUnits`
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
      `NotABackup` and `NewerBackup` — mapped to copy in the UI. They replace the copy
      `DeleteBrewingVesselUseCase` puts in the domain, and the tea, vessel and tea-type delete
      guards throw them. Tea types can no longer lose their last type.
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

---

## Bet 2 — Brewing plans

**Done, released as 0.5.0.**

**Outcome:** the app knows how each tea is brewed, and a gongfu session costs two taps per steep —
Next, then Start — plus whatever you choose to record.

Today: a saved method holds one `brewingTime`; every extra steep costs Next → dialog → Confirm →
Timer → Start (about three taps and two screens), and each steep's time is typed by hand.

### Design

Decided 2026-10-19.

- **A method is a plan.** `BrewingConfiguration` gains `steepIncrement: Duration?` (null = a single
  western steep) and `steepOverrides: Map<Int, Duration>`, a JSON text column like `photos`.
    - Steep 1 is `brewingTime`. Steep N is its override, otherwise steep N−1 + increment, so a plan
      continues from wherever a deliberate push left it. With no overrides this is
      `base + (N−1) × increment`.
    - Time only. Temperature and water are the session's, changed by hand on a steep.
    - No steep count: you stop when the tea is done, so Next is always offered.
    - The style lives on the method, not the tea — a tea can have a gaiwan-gongfu method and a
      mug-western method.
- **A session resolves its plan; it never stores one.** In order: its method
  (`usedConfigurationId`); the session Brew Again copied (`planSessionId`, new, parent row only);
  its own steeps so far. The last gives gongfu by feel: 20s, 25s → next 30s; one steep → the same
  again.
    - **Brew Again** sets `planSessionId`, not the method, and so replays the whole session as
      brewed rather than steep 1.
    - Editing a method changes the remaining steeps of a session using it. That's intended.
- **Plans are learned** by one pure `learnPlan(steeps)`, shared by Save as method, Update method,
  Brew Again and the by-feel fallback.
    - Each steep's `brewedTime` (actual, else planned), rounded to 1s under 15s and to 5s from 15s,
      so a late tap doesn't become an override replayed forever.
    - Base = steep 1. Increment = the most common step between neighbors; none for one steep. An
      override wherever a steep doesn't follow. Lossless: a learned plan replays every steep it was
      learned from.
    - **Save as method** is offered at Finish by a session without a method, at any steep count,
      unless it was rated under 3★ or an identical method exists for the tea and vessel. Unrated
      sessions are offered it. (Today multi-steep sessions are never offered it.)
    - **Update ‹method›** is offered when a session that used a method deviated from it, on the same
      vessel, over the steeps brewed (rounded times; leaf, water and water type exactly; temperature
      beyond 1 °C), unless rated under 3★. Those steeps become today's, with steep 1's water and
      temperature and the session's leaf and water type; the method's overrides past the last one
      stay, and its label and vessel are left alone. A session on a different vessel is offered Save
      as method instead, which moves the session to the new method.
    - The method form edits base and increment, previews the first steeps and marks overrides.
      Overrides are learned only: the form clears them all, never edits one.
- **Two ways out of the timer.** Complete Now becomes **"Poured early"** and keeps recording elapsed
  time as the actual time. **"Already brewed"**, before Start, records the steep at its planned time
  with no actual time and opens the review — for a forgotten timer, or one kept in the kitchen. A
  late start is ±adjust.
- **The timer waits for Start.** Pouring takes time; auto-start was considered and rejected.
- **Rinse** is a yes/no, never timed: a switch on the method that a session copies when it starts
  (`rinsed`), also on Log Tea and Edit Session, shown as "rinsed". No timer state, no steep row.
- **The slim review** between steeps replaces Steep Complete:
    - A question, "How was steep 3?", under the tea and vessel, with the steep's parameters; they
      open the edit sheet, which gains a vessel row, and choosing a vessel fills the water with its
      capacity. Then stars, eight tags ready to tap with More for the rest, and a note. The eight
      are the tags most confirmed on this tea, then on any, then the curated order, ranked once
      as the review opens. Below them, the same steep of the session Brew Again copied, else the tea's last, to
      rate against.
    - A primary **"Next (30s)"** straight to the timer at the planned time. "Adjust" opens a sheet
      with the next steep's time and temperature, then goes to the timer. Water is not adjusted
      between steeps. The review no longer uses the next-steep dialog; Session Detail's add-steep
      still does.
    - "Finish session" is secondary; Discard moves to the overflow. A western plan swaps them:
      Finish is primary, "Another (3:00)" secondary.
    - Earlier steeps and photos leave the loop.
- **The end-of-session review**, its own route after Finish: every steep as a row (time, rating,
  tags; tap to edit), the session note, photos, "vs your best" (this tea's best-rated other
  completed session, when there is one), Save or Update method, and Done. Home's Undo is unchanged.
- **Per-steep tasting tags.**
    - 23 curated tags in one flat list, in a fixed order with aromas first and finishes last:
      floral, fruity, honey, roasted, smoky, grassy, woody, earthy, spicy, sweet, bitter,
      astringent, umami, mineral, malty, nutty, light, smooth, full, lingering, short, drying,
      returning sweetness. No groups.
    - Custom tags are created from the tag sheet's search ("Create ‹word›"), which matches existing
      tags first to prevent near-duplicates, and are appended in creation order. Positions never
      move. Renaming and deleting them is polish-track UI on `deletedAt`.
    - **Carry forward, edit the delta:** steep N+1 starts with every tag of steep N, faded as
      unconfirmed. Tapping a faded tag confirms it; tapping a confirmed one removes it. Zero taps
      when nothing changed, and an untouched carry-over stays unconfirmed — the ratings rule,
      applied to tags.
    - Tables: `tasting_tag(id, key, name, isCustom, displayOrder, deletedAt)`, where `key` names a
      curated tag's string resource and a custom tag stores `name`; `steep_tag(steepId, tagId,
      confirmed)`, keyed on both ids, cascading from the steep row. `Steep.tags` is read with the
      aggregate and written through `EditSteep`.
- **The ready alert shows the plan:** "Steep 3 done, next 30s", no "next" for western. The next
  steep waits until this one is done, so the Live Activity and the ongoing notification say only
  "Steep 3" or "Steep 3 paused". `TimerState` carries the next steep's time.
    - The ready alert's tap is the Next action: it opens the slim review. No separate button.
    - The Live Activity gains a tap target, the Timer.
- **Quick Timer becomes "timer now, tea later."** The user who starts pouring before remembering to
  set a timer must not be forced through adding a tea — but should be invited to log.
    - It runs on `TimerService` as a timer with no session (`TimerState.sessionId` and `teaId` are
      already nullable), persisted in preferences so it survives process death. It inherits the
      foreground service, notifications, Live Activity, and settings toggles.
    - Same Timer screen, with an optional "What are you brewing?" chip opening `TeaPickerSheet`
      (recents, quick-add) during the countdown, or "Log it" at the end.
    - Attaching asks for the tea only. The vessel is the one last used with it, else the most-used;
      parameters come from Log Tea's prefill for the pair, else the timer's duration, the tea or
      type default temperature, the vessel's capacity and the default water type. The review's edit
      sheet fixes a wrong guess.
    - The timer keeps each finished steep's start and length, so attaching after three steeps by
      feel creates the session with all three at their real times, unrated. The session starts when
      its first steep did.
    - No tea at completion: "Log it", "Next (+5s)" for gongfu by feel, or "Just a timer", which
      records nothing.
    - Deletes `QuickTimerViewModel`'s engine, the duplicate screen, and the details sheet.
- **Audit findings cleared** (Rule 1): Steep Complete and Quick Timer, replaced; Timer, in its own
  commit first; the notifications — the ready alert outliving its steep (Working items), shared
  `requestCode`s, taps not checking session status; Home's Finish routing through the Timer and the
  dead Quick Timer handler. Log Tea and Home only gain small additions, so theirs stay in the polish
  track.

**Migrations**, one per feature, all `ADD COLUMN` / `CREATE TABLE`: v8 → v9 the plan
(`steep_increment`, `steep_overrides`, `tea_session.plan_session_id`); v9 → v10 rinse
(`brewing_configurations.rinse`, `tea_session.rinsed`); v10 → v11 the tag tables, the curated
vocabulary seeded at startup alongside the default tea types, so fresh installs get it too. The
backup becomes version 3 in the first of them; the later ones add fields with defaults under it,
since no release ships between them. Version 2 backups import with no plans, rinses or tags.

**Exit:** a 10-steep gongfu session from a saved plan goes review → running timer in two taps per
steep, Next then Start, with rating and tags optional; a western brew from a pinned method takes two
taps to a running timer; Quick Timer and Timer are one engine and one screen; tags carry forward and
round-trip through backup; Track B lines for Live Activity, the timer without a tea across process
death, and an upgrade over the previous dev build at each migration.

---

## Bet 3 — The journal

**Done, released as 0.6.0.**

**Outcome:** looking back is pleasant and useful in the three ways the author actually rereads.
Needs Bet 2's tag data to have something to show.

### Design

Decided 2026-10-21. No middle-dot strings in the UI: each value is its own element (icon and value,
a chip, its own line).

- **A session note of its own.** `tea_session.session_notes`, parent row only. Until now steep 1's
  `notes` doubled as it (the review, Log Tea, Steep Complete all wrote it there).
    - v12 moves every steep 1 note into it and clears steep 1's; reading backup versions 1–3 does
      the same move, so an old backup restores like an upgraded install.
    - The end-of-session review and Log Tea write the session note; the slim review's note is the
      steep's own, steep 1 included. A one-steep log has no separate steep note.
    - Photos stay on steep 1, where the review writes them. Session Detail shows a session's photos
      together. No migration: photos get safety, not features.
- **Dominant tags**, one pure function used by History cards, "last time" and the flavor profile.
  Confirmed tags only, ranked by how many steeps confirmed each, ties in vocabulary order, top 3. An
  untouched carry-over counts for nothing — the ratings rule. The flavor profile counts each tag
  once per session it was confirmed in, so a 10-steep gongfu session doesn't outweigh five western
  brews.
- **"Last time"** is the tea's most recent completed session, on any vessel or method.
    - On Log Tea, a card between the tea selector and `PrefillBanner` once a tea is chosen: date,
      rating, steep 1's leaf, temperature and time, the learned increment and steep count for a
      gongfu session (via `learnPlan`, so it matches what Brew Again replays), dominant tags, and
      the session note's first line (no fallback to steep notes). Tapping opens Session Detail on
      top; the form survives. No "use these" action: the prefill and Choose Method already apply
      parameters. No card for a tea never completed.
    - On a quick-brew card, one line: when and rating. Same session for every method of a tea —
      it's the tea's history. No tags: a tea tastes much the same by any method, so its top tags
      would sit unchanged on every card. Tags belong to the journal and the tea.
    - `PrefillBanner`'s hard-coded strings move to `strings.xml` alongside.
- **Tags on a logged brew.** Save as completed saves the session completed and unrated, then
  replaces Log Tea with the end-of-session review: per-steep rating and tags, the session note,
  photos, "vs your best", Save as method (which Log Tea never offered). Done returns to wherever Log
  Tea was opened from, with no Undo; Back from the review keeps the brew unrated.
  `CompleteSessionDialog` goes.
- **The session card**, one for History and Tea Detail:
    - Tea name and `RatingDisplay`; when it started (clock time in a day group, the date in a flat
      list), steep count and first-to-last span; a one-steep session shows only the time.
    - Rating dots, one per steep, only with 2+ steeps, in front of the steep count rather than
      beside the stars, where two rating marks crowded each other: fill by rating, a ring for
      unrated, one semantic summary ("Steep ratings: 4, 4, 3, unrated").
    - Up to 3 dominant tags as non-interactive tonal chips; the session note, 2 lines.
    - Parameters recede: steep 1's leaf, temperature and water, small and `onSurfaceVariant`. No
      brewing time.
    - In progress: a primary "Brewing, steep 3" label replaces the tap-swallowing `AssistChip`.
      No photo thumbnail: it no longer fit the taller card, and Session Detail shows photos. Menu
      unchanged.
- **History reads by day.**
    - A date header on every day for the last 30 days counting today ("Today", "Yesterday", then
      `formatDate`), per month beyond. Month headings carry no summary (Recap's job).
    - On days with 2+ sessions the header adds distinct teas ("3 teas") and the span from the first
      session's steep 1 to the last session's last steep, as separate elements. No session counts,
      no "standout brew". A session past midnight belongs to the day it started.
    - Filters or search active → a flat list, no headers. No sticky headers.
    - The filter sheet's rolling windows are labeled "Last 7 days" / "Last 30 days".
    - Tea type and vessel take several choices, a session passing on any of them; the other
      filters stay single. Choosing a tea outside the chosen types clears the types.
- **Search**, in memory: both screens already hold every row. One pure `matchesQuery`: case- and
  accent-insensitive, every word must match some field. Debounced, off Main.
    - History: tea name, producer, session note, steep notes, tag labels (carry-overs included —
      search looks for the word, not a judgment), location, vessel. Curated labels resolved with
      `getString`, so it matches what's shown.
    - Collection: name, producer, origin, description. Search combines with the filter instead of
      replacing it. `SearchTeasUseCase` and `TeaDao.search` go.
    - SQL full-text search rejected: an FTS table, migration and triggers for rows already in
      memory. Revisit if History ever pages.
- **Session Detail as a steep timeline.**
    - Header: tea (a button), date and time, the setup (vessel, water type, leaf, rinse, location)
      as icon-and-value, the session note, photos.
    - Each steep on a rail at its clock time, with rating and confirmed tags inline (carry-overs
      appear only in the sheet, faded). "What changed": only deltas from the previous steep (time,
      temperature, water), unchanged values omitted after steep 1. A gap more than 2 min beyond the
      previous steep's brew time is annotated as a break. Total brew time below; Add Steep, Brew
      Again and Save as method as today.
    - **One steep sheet:** the review's `SteepEditSheet` moves to `components/session/` and gains a
      parameters row (time, temperature, water through `EditSessionParametersSheet`). Edits save as
      made, as in the review. Delete steep is a trash icon beside its number.
        - The slim review lays out the same pieces its own way: a question over big stars, tags
          ready to tap and an open note. Both save each edit as it's made, so a finished steep and
          the steep just brewed edit the same way.
        - `EditSessionParametersSheet` has edited steeps as well as sessions since the parameters
          row: renamed `BrewParametersSheet` once Edit Session is session-only.
    - **Edit Session is session-only:** vessel, water type, leaf, rinse, location, plus tea, date
      and time, and the session note. Its steep mode, photo handling and save-method dialog go.
      Changing the date shifts every steep by the same amount. Changing the tea (via
      `TeaPickerSheet`) recomputes both teas' stats and clears `usedConfigurationId` and
      `planSessionId`.
    - Previous/next: up/down chevrons in the top bar (left/right read as back beside the arrow),
      chronological across all sessions, ignoring History's filters. The screen steps in place:
      only the content slides, newer from above as in History, while both bars stay; back still
      returns to the origin. Considered: replacing the route (the whole screen slides in sideways),
      a pager (fights photo rows, a ViewModel per page) and per-tea stepping (Tea Detail's job).
- **Tea Detail tells the tea's story**, in order: info, best brew, flavor profile, steep curve,
  methods, sessions.
    - Best brew: the best `effectiveRating` completed session, with date, parameters, tags, and Brew
      Again; tap opens it. Hidden when nothing is rated.
    - Flavor profile: the top 8 tags with "in 7 of 12 sessions" and a thin bar. No radar.
    - Steep curve: mean rating by steep number as bars, the peak marked ("peaks at steep 4"). A
      steep number shows when 2+ sessions rated it; the curve shows when 2+ sessions rated a steep
      past 1, so western-only teas never see it.
    - Provenance on `ConfigurationCard`: "From your 12 Mar brew, ★4.5", tapping opens it; hidden
      when the source session is gone.
    - Cellar age in the info card from `purchaseDate`, to `finishedAt` for a finished tea.
    - Zero sessions: the judgment sections hide, Sessions shows `EmptyState` with "Log a brew".
    - **Parameters vs rating over time is cut.** The session list, with each row's parameters and
      rating, answers it in reading form. A chart would belong to Recap.
- **Note prompts are deferred.** Tag-seeded prompts need a tag system proven in use first. Freeform
  entries detached from sessions are **rejected** (§ Decisions).
- **Audit findings cleared** (Rule 1), each in the commit that rewrites its screen. History: the
  empty search's Clear, the empty journal's "Log a brew", the Tea filter as `TeaPickerSheet`,
  `HistoryFilterSheet` taking values not State, filtering off Main. Session Detail: the tea name's
  role and target, duplicated titles, `Duration.toString()`. Edit Session: the archived-vessel race
  (load the session before vessels), a bottom Save, one leaf form, strings. Most other report 2 rows
  for these screens were already cleared by F2, F3 and Bet 1.

**Migration:** v11 → v12 adds `tea_session.session_notes` and moves steep 1's notes into it. The
backup becomes version 4; versions 1–3 read with the same move.

**Exit:** each of the three rereading jobs has a surface that answers it in under two taps from
where you'd want it; the journal audit findings (report 2) for History, Session Detail and Edit
Session are cleared; Track B line for an upgrade over the v11 build.

---

## Bet 4 — Recap

**Outcome:** retrospection as an occasion. A dedicated, paged recap — "Your autumn in tea", later
"Your year in tea". The one retrospective surface worth opening, and the portfolio's showpiece
screen.

### Design

Decided 2026-10-23.

- **Analytics reads every session, in memory.** It observes `getAllSessionsFlow()` with the tea,
  tea type and vessel Flows, combines them, and aggregates on `Dispatchers.Default`, so it is live
  and never blocks Main. The roadmap's earlier "one query over `[previousStart, end)`" couldn't
  stand alone: new teas need every tea's first brew, whatever the period.
    - Aggregators are pure functions in `domain/models/Analytics.kt` over a `DateWindow`, like
      `stats()` and `steepCurve()`; `AnalyticsPeriod.window(now, tz, hemisphere)` replaces
      `GetAnalyticsDateRangeUseCase`. Nothing is written and nothing fails, so there are no use
      cases: the ten `Get*UseCase`s go, and `getSessionsStartedBetween` if nothing else reads it.
    - **Insights are deleted**, `GenerateInsightsUseCase`, `TimeInsights` and `InsightItem` with
      them. Sentences about a period are Recap's job; Analytics keeps aggregates.
- **Calendar periods only:** This week, This month, This season, This year, All time. Each is
  compared with the same elapsed part of the one before; All time has no comparison. CUSTOM (never
  reachable), Last month and the rolling windows go, taking two off-by-ones with them. A stored or
  backed-up period that no longer exists reads as This week, so older backups still import.
- **Gated per surface.** `EmptyState` fills the screen only when no session was ever completed; a
  period with none shows it under the selector. Each card hides when it has nothing. Changing the
  period keeps the screen up while it recomputes. The 10-session minimum moves to recaps.
- **What Analytics shows:**
    - All seven summary cards. The comparison is `onSurfaceVariant` both ways, no guilt red, and its
      arrow says "up 15% from last week".
    - **One activity heatmap**, the only chart over time: by day for a week, month or season, by
      week for a year, by month for All time. The trend chart goes, and three of its audit bugs with
      it.
    - The tea type donut. The vessel donut on theme colors: the top 5 from primary, secondary,
      tertiary and their containers, the rest one untappable "Other" slice in outline color.
    - Top teas. Top rated needs 2+ rated sessions per tea and shows ratings through `formatRating`.
    - Every heatmap cell and donut segment has a 1dp `outlineVariant` stroke, so a pale tea type
      color reads on any surface; `TeaTypeListScreen`'s special case for White goes.
- **Links into the journal:** a heatmap cell opens History for its day, week or month; a donut
  segment or legend row opens History by that type over the period; a vessel opens Vessel Detail; a
  top tea opens History by that tea over the period. Legend rows are 48dp targets.
- **Seasons are meteorological, by hemisphere.** Dec–Feb, Mar–May, Jun–Aug, Sep–Nov, named for
  the hemisphere. A Settings choice, Northern or Southern, stored nullable: unset, it follows
  `Locale.current.region` through a pure `Hemisphere.forRegion`. It travels in backups with a
  default. Winter spans two years and is titled so ("Winter 2025–26").
- **Which recaps exist:**
    - A season once it has ended, with 10+ completed sessions. The live season is Analytics' This
      season.
    - Year N from Jan 1 of N+1, when the first completed session is in Q1 of N or earlier, so a
      year begun in June isn't called your year.
    - Computed live from the same Flow, not stored: editing an old session or finishing a tea
      corrects it. "Told once" is the occasion, not frozen numbers.
    - "On this day" waits for year two, outside this bet.
- **Reaching a recap:** at the top of Analytics, above the period selector, a tonal hero card for
  the newest recap that qualifies, then a row of past recaps, newest first. Hidden when none
  qualifies. `RecapRoute` lives on Analytics' stack.
- **The recap is a pager.** `HorizontalPager`, page dots, a close button, no auto-advance. Each page
  fills with a container color, cycling primary, secondary and tertiary, and leads with a
  display-size Figtree number. A tea opens Tea Detail, a session Session Detail, a heatmap day
  History; back lands on the same page. Each page hides when it has nothing:
    1. **Cover:** the title, the months, sessions and distinct teas.
    2. **Rhythm:** the season's heatmap and the busiest month.
    3. **Most brewed:** the top 3 teas with session counts, and a tea type split bar.
    4. **Discoveries:** teas whose first completed session falls in the window.
    5. **Flavors:** `flavorProfile()` over the window, a tag missing from the previous window's top 8
       marked new. Without a previous profile, just the flavors. Needs 3+ tagged sessions; tags
       exist from 0.4.0, so summer 2026 has none.
    6. **How you brewed:** gongfu against western, where a session is gongfu when steep 1 brewed
       under 2 minutes, with the average steeps of a gongfu session; where ratings peak
       (`steepCurve()`); the longest session; the favorite vessel.
    7. **Best session:** `best()` over the window.
    8. **The shelf:** teas finished in the window, and teas waiting, brewed before it, not during
       it and not finished by its end. A closing line.
- **Dormant teas in Collection:** a "Not brewed lately" chip narrows the list to unfinished teas
  last brewed 60+ days ago, oldest first, through a pure `isDormant(now)`. It combines with the type
  filter and search. A recap's shelf uses its own window instead, so a past recap never changes as
  you brew today.
- **Audit findings cleared** (Rule 1), in the commits that rewrite each part: the donut hit-test,
  legend targets, the heatmap ordering months without the year, Canvas text at large font scale,
  the error-red comparison and its silent arrow, the Unique and New teas icons, Top rated's sample
  and rounding, Canvas semantic summaries, strings. Report 2's Analytics section is the last of the
  report.

**Migration:** none. The hemisphere is a preference.

**Exit:** the summer 2026 recap renders from real data; every aggregator and window has host tests
at week, month, season and year boundaries; Track B lines for the hemisphere default on a
southern-region device and the pager across rotation and 2× font.

**Commits.** Each compiles, passes tests and leaves the app working. 2 → 3 → 5 → 7 → 8 → 9 → 10;
4 → 5; 3 → 6; 3 and 4 → 12 → 13–20; 7 → 13.

1. [x] `docs: record the Bet 4 design`
2. [x] `refactor: drop analytics insights`
3. [x] `refactor: analytics from pure aggregators`: `DateWindow`, the aggregators, the ViewModel on
   Flows; the use cases go. No visible change beyond staying live. Tests: every aggregator, steeps
   against sessions, new teas reaching before the window.
4. [x] `feat: a hemisphere setting`. Tests: the region default, a backup without it. Track B: a
   southern-region device.
5. [x] `feat: analytics by calendar period`: This season, the trimmed menu, the fallback, labels in
   `strings.xml`. Tests: week, month, season and year edges, winter across the new year, the
   elapsed-part comparison, importing a backup holding `LAST_30_DAYS`.
6. [x] `feat: analytics gated per surface`
7. [x] `feat: one activity heatmap`: the trend chart goes; All time by month; months ordered by
   year. Test: cells across a year boundary.
8. [x] `fix: analytics chart audit findings`: outlines, the hit-test, legend targets, the neutral
   comparison, icons, Top rated's minimum, semantics. Nothing beyond the fixes.
9. [x] `feat: vessel donut on theme colors`
10. [x] `feat: analytics links into the journal`. Test: a cell's date range.
11. [ ] `feat: not brewed lately`. Test: `isDormant`.
12. [ ] `feat: season recaps`: the windows and gate, `RecapRoute`, the hero card and row, the pager
    with its cover. Tests: finished seasons only, the 10-session gate, the hemisphere flip.
13. [ ] `feat: recap rhythm`
14. [ ] `feat: recap most brewed`
15. [ ] `feat: recap discoveries`
16. [ ] `feat: recap flavors`. Test: a tag new against the previous season.
17. [ ] `feat: recap how you brewed`. Tests: the style rule, the longest session.
18. [ ] `feat: recap best session`
19. [ ] `feat: recap shelf`. Test: finished and waiting at the window's edges.
20. [ ] `feat: year in tea`. Test: the Q1 gate.
21. [ ] `chore: release 0.7.0`: audit rows struck; report 2 deleted.

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
- [ ] **About screen:** version, licenses, privacy policy, support contact.
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

| Decision                                                                       | Reasoning                                                                                                                                                                                                                                                                                 |
|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sync is deferred, not planned**                                              | Backup is covered by export/import (made trustworthy in F4). Multi-device sync matters only if you brew from two devices, which you don't. Sync costs auth, a backend, conflict resolution, a privacy policy, and ongoing money. `SyncStatus` and `userId` can sit unused.                |
| **Purposes stay ranked, public launch stays undated**                          | Revisited 2026-09-23. Personal tool first; a date would let strangers' needs override the author's.                                                                                                                                                                                       |
| **Thin foundation, then bets**                                                 | Only what every bet stands on goes first. Other foundations are paid for by the first bet that touches them. Considered: all foundations first (weeks of invisible work, some shaped better by features) and features first (built on the six-way session logic).                         |
| **Design system before brewing plans**                                         | Bets 2–3 rewrite most screens; building them after the system means building them once.                                                                                                                                                                                                   |
| **Only a user tap stores a rating**                                            | Four code paths wrote ratings the user never gave, and one fed back into itself. Unrated steeps stay null and are excluded from averages and best brew. Cost: less rating data. Worth it: the data means something.                                                                       |
| **Stored derived stats are recomputed, never incremented**                     | Tea and configuration stats are rebuilt from sessions inside each mutation's transaction. Increments drifted: `timesUsed` rose once per steep and never fell on delete. Recomputing makes re-finish, Undo and delete correct with no special cases. Session ratings aren't stored at all. |
| **Discarding steep N ≥ 2 finishes the session**                                | Steeps 1…N−1 happened; only the next one was abandoned. Discarding steep 1 deletes the session. Considered: discarding the whole session from any steep.                                                                                                                                  |
| **Undo is the only way to re-open a finished session**                         | An accidental Finish needs a way back; re-opening old sessions doesn't. Session Detail's Add Steep records a forgotten steep without a timer instead.                                                                                                                                     |
| **A brewing style lives on the method, not the tea**                           | A tea brewed both ways needs two methods; the vessel usually implies the style.                                                                                                                                                                                                           |
| **Plans are base + increment with overrides**                                  | Matches how the author actually brews gongfu. Each steep follows the previous one, so a deliberate push carries forward. Time only: the author rarely changes temperature mid-session and does it by hand. Considered: overrides carrying temperature.                                    |
| **Timer waits for Start**                                                      | Pouring takes time. Auto-start considered and rejected.                                                                                                                                                                                                                                   |
| **Review stays between steeps, slimmed**                                       | Considered: rating during the next countdown (no screen at all) and session-end-only (memory fades by steep 10). A one-screen stars + tags + Next keeps capture high at one tap.                                                                                                          |
| **The ready alert's tap is Next, and opens the review**                        | Considered: advancing from the lock screen with the rating left null, rejected to keep rating capture in the loop; a separate Next button, which would do what the tap already does.                                                                                                      |
| **Tags are per steep, carried forward unconfirmed**                            | Per-steep shows how flavor evolves; carry-forward makes it cost zero taps when nothing changed; "unconfirmed" keeps it honest. Vocabulary is curated plus custom.                                                                                                                         |
| **A session resolves its plan; it never stores one**                           | From its method, the session Brew Again copied, or its own steeps. Considered: copying the plan onto the session, a second copy that drifts from the method. Brew Again points at its source session, not the method, so it replays what was brewed.                                      |
| **Plans learn from actual time, rounded**                                      | "Poured early" is real brew time; "Already brewed" records none, so the planned time stands. Rounding stops a late tap becoming an override replayed forever; 1s under 15s keeps flash steeps.                                                                                            |
| **Overrides are learned, never typed**                                         | Brewing it once and tapping Update does it better than a per-steep editor, which would be the bet's biggest piece of UI. Considered: tap-a-steep editing in the method form.                                                                                                              |
| **Rinse is a flag, not timed**                                                 | The author rinses some teas and never times it. Considered: a timed rinse before steep 1, which the timer, notifications, Live Activity and review would all have to learn.                                                                                                               |
| **Tasting tags are one flat list**                                             | Groups help long lists; 23 tags fit one sheet, and groups make every custom tag ask where it goes. Considered: aroma / taste / body / finish.                                                                                                                                             |
| **A tealess timer keeps its steeps**                                           | Attaching a tea after three steeps by feel logs all three at their real times. Considered: starting the session at the current steep, which loses brews.                                                                                                                                  |
| **One migration per feature**                                                  | Each commit carries the schema it uses, and the author installs dev builds between commits. Considered: one v8 → v9 up front, adding columns nothing uses yet.                                                                                                                            |
| **Quick Timer is "timer now, tea later" on the one engine**                    | Keeps the no-friction entry point the author values, deletes the second timer engine the audit found weak. Considered: nullable-tea draft rows in the database (a migration and nullable tea everywhere) and keeping the separate screen.                                                 |
| **Teas with sessions are finished, never deleted**                             | The journal must never lose a name, and deleting caused "Unknown Tea" and unrestorable backups. Delete remains for zero-session teas.                                                                                                                                                     |
| **`purchasePrice` is deleted, not built**                                      | Cost-per-cup analytics is a feature nobody asked for.                                                                                                                                                                                                                                     |
| **Stock is `finishedAt`, not a quantity**                                      | Marking "I ran out" is the whole need, and it's what feeds the dormant-tea prompt.                                                                                                                                                                                                        |
| **Finished teas stay in the Log Tea picker**                                   | Hiding them makes the auto-clear unreachable.                                                                                                                                                                                                                                             |
| **Host-JVM tests; smoke tests from previews; no interactive Compose UI suite** | Revisited 2026-10-15. Rendering every mandatory preview catches crashes without an emulator. Interaction suites still aren't worth their brittleness. Considered: E2E flows, which need an emulator and duplicate Track B.                                                                |
| **Accessibility basics are definition-of-done, not a launch gate**             | Revisited 2026-09-23. Cheap per touch; the rating control — the loop's main input — is unusable with a screen reader today. One full screen-reader pass before store launch.                                                                                                              |
| **Photos get safety, not features**                                            | The author doesn't reread photos. They matter only because unchecked they break backups and storage.                                                                                                                                                                                      |
| **Freeform journal entries are rejected**                                      | The author rarely writes notes attached to sessions; detached entries would be emptier. Tags and session notes cover the journal.                                                                                                                                                         |
| **A session note is its own column**                                           | Steep 1's note doubling as the session's left steep 1 with no note of its own and every screen deciding which it meant. Existing steep 1 notes move into it. Considered: making the steep 1 rule official, which needs no migration.                                                      |
| **Dominant tags count confirmed tags only**                                    | The ratings rule, applied to tags: an untouched carry-over is not an observation. The flavor profile counts per session so gongfu doesn't outweigh western. Considered: unconfirmed at half weight, and counting anything present.                                                        |
| **"Last time" is the tea's, not the method's**                                 | The reread is about the tea. Considered: the last session with the same method or vessel.                                                                                                                                                                                                 |
| **A logged brew finishes in the end-of-session review**                        | It already has tags, the note, photos and Save as method. Considered: tags in `CompleteSessionDialog`, a sheet over a dialog and a weaker copy of the review.                                                                                                                             |
| **Journal search runs in memory**                                              | Both screens already hold every row. Considered: SQL full-text search, an FTS table, migration and triggers for no gain at this size.                                                                                                                                                     |
| **Parameters vs rating over time is cut**                                      | The session list, each row with its parameters and rating, answers it in reading form. A chart would belong to Recap.                                                                                                                                                                     |
| **Note prompts are deferred**                                                  | Prompts seeded by tags need a tag system proven in use first. Considered: a tag-seeded placeholder question on the review's note.                                                                                                                                                         |
| **Changing a session's tea clears its method link**                            | A method belongs to a tea; a session moved to another tea can't have used it.                                                                                                                                                                                                             |
| **History grouping revived; the rest of the stashed redesign stays dead**      | Three of its five summary fields were Analytics content in a History costume.                                                                                                                                                                                                             |
| **No streaks**                                                                 | An engagement mechanic that says nothing about tea. The shipped streak insight is removed in F1.                                                                                                                                                                                          |
| **The session reminder stays inexact**                                         | `setAndAllowWhileIdle`; Doze spaces these ~9 minutes apart. Not worth the exact-alarm allowance for a soft nudge. The fix if it annoys is `setExactAndAllowWhileIdle`, mirroring the completion alarm.                                                                                    |
| **Notifications get two switches, not one**                                    | Turning off the soft nudge must not kill the alert the app exists to deliver.                                                                                                                                                                                                             |
| **Previews are smoke-tested, not screenshot-tested**                           | Pixel parity isn't the goal, and goldens churn hundreds of PNGs into git on every theme tweak. ComposablePreviewScanner reads `commonMain` previews under the KMP library plugin. Considered: Roborazzi goldens in git, Git LFS, CI-only baselines.                                       |
| **Unit toggles stay, local to the field**                                      | Converting a vendor's instructions in place is worth having. The bugs came from the toggle writing the global preference and living in every ViewModel, not from the toggle. Considered: Settings as the only way to change units.                                                        |
| **The weight unit is honored everywhere**                                      | A setting that one Analytics card obeys is worse than none. Considered: grams only, deleting the preference — leaf is weighed in grams nearly everywhere.                                                                                                                                 |
| **Display units come from a CompositionLocal**                                 | Honoring weight everywhere would otherwise thread preferences through every State that shows a value. Considered: formatting in the screen and passing strings down, the earlier rule.                                                                                                    |
| **Theme mode is a setting**                                                    | Considered: following the system only. It travels in backups like the other preferences.                                                                                                                                                                                                  |
| **Headings are Figtree; body stays system**                                    | A friendly sans gives the app its own voice without formality; system body keeps dense screens scannable. Considered: Literata, tried and dropped as too formal; Nunito, DM Sans.                                                                                                         |
| **The brew loop lives on Home's stack**                                        | Notification taps and Home's banner already open the Timer there. Pushing it onto the origin tab could leave two Timers in two stacks.                                                                                                                                                    |
| **Tablet layouts wait for Store readiness**                                    | The author uses a phone, and nothing ships to strangers before then.                                                                                                                                                                                                                      |
| **Analytics reads every session, in memory**                                   | New teas need every tea's first brew whatever the period, and one Flow keeps the screen live for free. Considered: a range query over `[previousStart, end)` plus a first-brew query.                                                                                                     |
| **Analytics insights are deleted**                                             | Sentences about a period are Recap's job; Analytics keeps aggregates. Considered: moving the eight sentences to strings, and keeping a few as stat tiles.                                                                                                                                 |
| **Analytics periods are calendar windows**                                     | "This week" meant four things across the app. Week, month, season, year and all time, each compared with the same elapsed part of the one before. Considered: keeping the rolling windows and Last month.                                                                                 |
| **One activity heatmap, no trend line**                                        | Both charted sessions over time; the heatmap covers every period. Considered: fixing the trend chart's three bugs and keeping both.                                                                                                                                                       |
| **Seasons follow a hemisphere setting**                                        | Season names are wrong half the world over. The default comes from the locale's region. Considered: Northern only, and naming windows by months.                                                                                                                                          |
| **Recaps are for finished seasons, computed live**                             | A recap is an occasion, told once its season ends; computing it live means edits correct it with no table. Considered: a "so far" recap of the current season, and frozen snapshots.                                                                                                      |
| **A session is gongfu when steep 1 brews under 2 minutes**                     | Pure and works for every session, method or not. Considered: 3+ steeps, which misses an abandoned gongfu session, and the resolved plan, which drags repositories into the aggregators.                                                                                                   |
| **Dormant is 60 days unbrewed, shown on pull**                                 | A recap page and a Collection chip, never a Home card: not an engagement machine. Considered: a cadence-relative rule, and a Home suggestion.                                                                                                                                             |

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
- **Never push a Live Activity update as a side effect of canceling an alarm.** It froze running
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
- [x] "Tea is ready!" outlives its steep — the completion alert (`COMPLETION_NOTIFICATION_ID`)
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
