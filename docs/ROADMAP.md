# Leaf Log — Roadmap

> **Last updated:** 2026-09-23
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

### Track S — screenshots, host JVM, runs in CI

**New.** Every `@Preview` — already mandatory per CLAUDE.md — is rendered as a screenshot test
(Roborazzi or Compose Preview Screenshot Testing; choose in Bet 1). Previews gain dark and
max-font-scale variants and a long-text case. This catches the polish regressions the author cares
about most, with no emulator and no interaction scripts. It is **not** the rejected Compose UI test
suite: nothing is tapped, nothing is flaky.

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

Small; everything after it is worth more once it's done.

- [x] **Unstage `EditVesselFormLogic.kt`** (`git rm --cached`). It will break the next commit.
- [x] **Commit `docs/` as-is, then delete the superseded eight** in a second commit, so the old
  material is recoverable from history.
- [x] **GitHub Actions CI**: `./gradlew :shared:allTests :androidApp:assembleDebug` on push and PR.
  Without it, every test written later is a thing you *remember* to run.
- [x] **Test dependencies:** keep and wire `koin-test` (DI graph verification); delete the
  Android-instrumented scaffolding and the other unused deps listed in § 2.
- [x] **Trim `TEST_CASES.md`** to the Track B checklist (~150 lines).
- [x] **Release hygiene.** "Which build is on my phone" must be answerable.
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
    - `master` → `main` now; CI triggers on `[main, kmp]` until `kmp` merges. Check the Xcode Cloud
      workflow's branch start condition after the rename.

      Exit: both platforms and the widget read `version.properties` (the `#include` confirmed by a
      local Xcode build and an Xcode Cloud run); `CHANGELOG.md` has a `0.1.0` entry; the three tags
      are pushed; `main` exists and CI and Xcode Cloud trigger from it.

### F1 — Fix now

The verified bugs in `docs/audit/README.md` §2 that no later item rewrites. No design needed; all
small. One conventional commit per item, its test in the same commit. Strike the audit row as it
closes.

- [x] **Stop writing ratings the user didn't give** (#2, #3). F2's migration can't tell a guess from
  a real rating, so every day these run is unrecoverable. Deletions only:
    - Steep Complete's guess-and-upsert on open (`SteepCompleteViewModel` `suggestedRating`).
    - Quick Brew: `rating = null`, and pass `usedConfigurationId = config.id` so `timesUsed` /
      `lastUsedAt` finally rise (increments only fire on `COMPLETED`, so no double count).
    - `SaveConfigurationDialog` gets `session.rating`, not `?: 5f`; its `rating` becomes nullable.
    - The Log Session dialog prefill stays for F2 — the user sees it and submits it.
- [x] **iOS relative dates** (#9). A pure commonMain function classifies an instant as Today /
  Yesterday / This year / Older given `now` and `tz`; the actuals only format. Test: year boundary,
  Aug 23 vs Sep 23.
- [x] **Weeks start on Monday, everywhere** (#13). One `LocalDate.startOfWeek()` replaces the three
  `ordinal` copies (Analytics `THIS_WEEK`, trend buckets, activity). Heatmap grid and labels go
  Monday-first. History's "This Week" filter is renamed "Last 7 Days" — that's what it does.
  `calculateDateRange` takes `now` and `tz`. Test: Sunday, Monday, month and year edges.
- [x] **Chart taps** (#14). Donut segment and trend point hit-testing become pure functions. Each
  Canvas gets a `contentDescription` summary (definition of done). Test: angle offset, nearest-point
  rounding.
- [ ] **Timer edit sheet** (#15). Re-read the session inside the launch, copy the edited fields onto
  it, then upsert. Reset while paused persists via `saveTimerStateUseCase`.
- [ ] **Steep Complete discard withdraws the reminder** (#17). `timerService.onSessionResolved`.
- [ ] **Live Activity deployment target** 26.2 → 18.2 (#18). Exit: an Xcode Cloud build; a Track B
  line for the Live Activity on an iOS 18 device.
- [ ] **Errors you can see** (#19). Minimal visibility only — Bet 1 replaces the source, not the
  placement:
    - Collection, History: inline `Snackbar` → `Scaffold` `snackbarHost`.
    - Timer, Brewing Method, Edit Session, Vessels: render `state.error` in a `SnackbarHost`.
    - Settings: export and import picker failures get their own intents instead of
      `ExportCompleted` / `ImportCancelled`.
- [ ] **Remove the streak insight** (#20): the block, `computeMaxStreak`,
  `InsightType.CONSISTENCY` if otherwise unused, the preview string.
- [ ] **`removeLast()` → `removeAt(lastIndex)`** in `AppNavigation.kt` (18 sites). Safe whether or
  not the API < 35 crash is real; move the Hard-won facts entry from "unverified" to a rule.
- [ ] **Release `0.1.1`**: CHANGELOG entry, tag `v0.1.1`.

**Moved, not fixed here:** #2–7 → F2 (except the rating writes stopped above), #8 → F3, #10–12 → F4,
#16 Quick Timer notification → Bet 2 (which deletes that engine). #1 closed in F0.

Exit: every item above checked; each fix that involved a branch, loop, or date calculation has a
Track A test.

### F2 — The session aggregate

**Outcome:** one definition of a session and its steeps, owned in one place. The prerequisite for
brewing plans, best brew, the journal, and recap — all of which would otherwise be built on six
disagreeing copies of the rules.

**Design:**

- A `SessionWithSteeps` domain aggregate (parent + ordered children). Reads that present sessions —
  History, Tea Detail, Vessel Detail, Analytics, recap — go through it.
- **One rating rule:** `effectiveRating` = mean of *rated* steeps, `null` if none. Replaces the
  current split (tea stats read steep 1's `rating`; prefill reads `averageRating ?: rating`).
  Unrated steeps are excluded from every average and from best brew.
- **Every steep mutation goes through a use case** that also recomputes the session's average and
  the tea's stats: `AddSteep`, `RateSteep`, `EditSteep`, `DeleteSteep` (renumbers),
  `FinishSession`, `DiscardSession` (withdraws the reminder). The six current code paths —
  `CompleteSessionUseCase`, `SteepCompleteViewModel` (×3), `TimerViewModel` discard,
  `InProgressSessionDelegate`, `SessionDetailViewModel` steep delete — collapse into these.
- **Honest data rules**, enforced in the aggregate's use cases, not screens:
    - Only a user tap stores a rating. Suggestions render as ghost values. The four sources of
      unrequested ratings (Steep Complete guess, Quick Brew copy, Log Session dialog prefill,
      `SaveConfigurationDialog ?: 5f`) are removed.
    - Session-level fields (vessel, water type, leaf, location) live on the parent only; children
      stop copying them. Reads take them from the parent. (Fixes edits not propagating, and the leaf
      double-count trap becomes structurally impossible.)
    - A steep added to a past session is stamped relative to that session, not "now".
    - **Record actual brew time** alongside the planned time. ±adjust and Complete Now stop being
      lost; prefill and plans replay what actually happened.
    - Updates use field-level writes or read-modify-write *inside* the use case. No more
      `staleSnapshot.copy(oneField)` from ViewModels.
    - Update APIs can express "cleared" (explicit patch type, not `null` = unchanged).
    - Unit edits round-trip in storage units; display conversion never writes back unless the user
      changed the value.
- Re-finishing a completed session is a no-op (fixes the `timesUsed` double count).

**Migration:** v6 → v7 — move session-level fields off children (keep columns nullable for one
version if simpler), add `actualBrewingTimeMs`, recompute every parent's `averageRating` and every
tea's stats under the new rule.

**Exit:** the six paths are gone; Track A tests cover every steep mutation, the rating rule, and the
migration against real v6 data; History, Tea Detail and prefill agree on every tea's rating.

### F3 — Tea lifecycle: finished, not deleted

**Outcome:** the journal never loses a name, and soft-delete stops leaking.

- **`Tea.finishedAt: Instant?`.** Null = in stock. Timestamp, not boolean — tells you *when*, and
  matches the `deletedAt` idiom.
    - Excluded from dormant-tea prompts (Bet 4) — the reason the field exists.
    - Auto-clears when a session is logged with that tea.
    - Collection: sorted last, dimmed, still searchable.
    - Log Tea picker: stays visible and marked; tapping asks "Still have this?" and clears the flag.
      Hiding it would make the auto-clear unreachable.
- **A tea with sessions cannot be deleted** — only finished. Delete is offered only for teas with
  zero sessions (typos, duplicates). Same rule for vessels (archive exists; forbid delete when used)
  and tea types (merge instead of delete when used).
- **By-id queries filter soft-deleted rows** like list queries do; a deleted tea's page closes.
- **Photo files are only removed on hard purge**, never on soft delete. Prerequisite for Undo.
- **Brewing configurations get `deletedAt`** (they hard-delete today).
- Same migration drops `purchasePrice`, `stockAmount`, and `BrewingConfiguration.isActive`.

**Exit:** no screen can show "Unknown Tea"; Undo is purely UI work; Track A tests for each rule.

### F4 — Backup integrity

**Outcome:** a backup restores on either platform, onto a fresh device, without losing or reverting
anything. Export is the only backup under the no-sync decision, so this is data safety, not polish.

- **Explicit export DTOs with a version**, decoupled from Room entities. Unknown future version →
  refuse with a clear message. Older versions → migrate the DTO.
- **One ZIP format on both platforms.** Android writes DEFLATE today and iOS reads STORE only.
  Photos don't compress; STORE on both is the lazy correct answer.
- **Import in one transaction.** A failure leaves the database untouched.
- **Last-writer-wins on `updatedAt`**, and `deletedAt` travels in the backup. An older backup no
  longer reverts edits or un-deletes rows.
- **Stream entries file-to-file.** No whole-archive `ByteArray`; no OOM at hundreds of photos.
- **Content-hash image names.** No collisions across imports, no duplicate storage on re-import.
- Export preferences too. Clean up temp files. Settings shows **"Last backup: 42 days ago"**
  and a gentle nudge past a threshold.
- **Import preview:** backup date, counts, and "N existing records will be updated".
- **Photo safety** (previous 2.6): size cap and compression on import to the app, picker error
  handling, storage shown in Settings.

**Exit:** Track A round-trips for: full export → wipe → import; Android archive read by the iOS
parser path; tea finished-with-sessions restored to an empty DB; import twice = no change; older
backup over newer data = newer data wins.

---

## Bet 1 — Design system & app shell

**Outcome:** there is a target to polish toward. Today the app is stock Material with a palette:
stock `Typography()`, no `Shapes`, baseline fallbacks for unset colour roles, two icon families,
five error-display patterns, four copies of the same dialogs. Bets 2–3 rewrite most screens; doing
this first means they're built once, in the final style.

Scope is **the system, not a restyle of every screen.**

- **Identity:** a display typeface suited to a journal, a type scale, `Shapes`, all colour roles
  set, tea-type colours toned per theme (White `#FFFDE7` is invisible on light surfaces today),
  dark-mode splash, one icon family.
- **One component per pattern:** section header, unit input field (replaces Temperature / Volume /
  Capacity variants), confirm-destructive dialog, discard-changes dialog, empty state (with an icon
  slot), rating display with correct rounding and semantics, formatted weight/time/date.
- **One error surface:** a `UiMessage` channel per ViewModel, one `SnackbarHost` per screen, and a
  small set of typed domain failures (`NotFound`, `InUse(n)`, `LastRemaining`, `Io`) mapped to copy
  in the UI. No raw `e.message` reaches a user.
- **Strings:** string-resource infrastructure and plurals in place ("1 brews", "Used 1 times" die
  here). Screens migrate as later bets touch them. Locale-aware date formatting behind one
  `expect`/`actual`.
- **Unit toggles stop changing global preferences from inside forms.** Units are a Settings concern,
  with a weight row that is actually honoured everywhere.
- **App shell:** per-tab back stacks (back from a tab returns Home, tabs keep their state), Up
  arrows on pushed tab screens, fade-through for tab changes, one safe `navigateUp()`. Route args go
  into ViewModels and load in `init`, killing the `LaunchedEffect`-reload bug class (edit forms
  re-seeding over user input, stacking collectors, re-applied filters).
- **Track S goes live** here, locking the system in.

**Exit:** every shared component exists once, with preview variants in Track S; no screen renders
errors any other way; no raw exception text; tab navigation behaves per the shell rules.

**Open questions:** typeface choice; Roborazzi vs Compose Preview Screenshot Testing (the latter is
Android-only but reads existing previews directly); whether adaptive large-screen layouts land here
or in the polish track (Android 16 ignores the portrait lock on large screens, so tablets will see
landscape regardless).

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

**Migration:** v7 → v8 — plan columns on `brewing_configurations`, the two tag tables, seeded
curated vocabulary.

**Exit:** a 10-steep gongfu session from a saved plan takes one tap per steep to advance; a western
brew from a pinned method takes two taps to a running timer; Quick Timer and Timer are one engine
and one screen; tags carry forward and round-trip through backup; Track B lines for Live Activity
and the timer-without-tea across process death.

### Open questions

- **Rinse representation.** The aggregate treats the parent as steep 1. A rinse as a
  `steepNumber = 0` row breaks that invariant. Recommended: a `rinseDuration: Duration?` on the
  parent session instead of a row.
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
- [ ] Store listings and screenshots (Track S makes these cheap).

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

| Decision                                                                            | Reasoning                                                                                                                                                                                                                                                                  |
|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sync is deferred, not planned**                                                   | Backup is covered by export/import (made trustworthy in F4). Multi-device sync matters only if you brew from two devices, which you don't. Sync costs auth, a backend, conflict resolution, a privacy policy, and ongoing money. `SyncStatus` and `userId` can sit unused. |
| **Purposes stay ranked, public launch stays undated**                               | Revisited 2026-09-23. Personal tool first; a date would let strangers' needs override the author's.                                                                                                                                                                        |
| **Thin foundation, then bets**                                                      | Only what every bet stands on goes first. Other foundations are paid for by the first bet that touches them. Considered: all foundations first (weeks of invisible work, some shaped better by features) and features first (built on the six-way session logic).          |
| **Design system before brewing plans**                                              | Bets 2–3 rewrite most screens; building them after the system means building them once.                                                                                                                                                                                    |
| **Only a user tap stores a rating**                                                 | Four code paths wrote ratings the user never gave, and one fed back into itself. Unrated steeps stay null and are excluded from averages and best brew. Cost: less rating data. Worth it: the data means something.                                                        |
| **A brewing style lives on the method, not the tea**                                | A tea brewed both ways needs two methods; the vessel usually implies the style.                                                                                                                                                                                            |
| **Plans are base + increment with overrides**                                       | Matches how the author actually brews gongfu.                                                                                                                                                                                                                              |
| **Timer waits for Start**                                                           | Pouring takes time. Auto-start considered and rejected.                                                                                                                                                                                                                    |
| **Review stays between steeps, slimmed**                                            | Considered: rating during the next countdown (no screen at all) and session-end-only (memory fades by steep 10). A one-screen stars + tags + Next keeps capture high at one tap.                                                                                           |
| **Lock-screen Next opens the review**                                               | Considered: advancing from the lock screen with the rating left null. Rejected to keep rating capture in the loop.                                                                                                                                                         |
| **Tags are per steep, carried forward unconfirmed**                                 | Per-steep shows how flavour evolves; carry-forward makes it cost zero taps when nothing changed; "unconfirmed" keeps it honest. Vocabulary is curated plus custom.                                                                                                         |
| **Quick Timer is "timer now, tea later" on the one engine**                         | Keeps the no-friction entry point the author values, deletes the second timer engine the audit found weak. Considered: nullable-tea draft rows in the database (a migration and nullable tea everywhere) and keeping the separate screen.                                  |
| **Teas with sessions are finished, never deleted**                                  | The journal must never lose a name, and deleting caused "Unknown Tea" and unrestorable backups. Delete remains for zero-session teas.                                                                                                                                      |
| **`purchasePrice` is deleted, not built**                                           | Cost-per-cup analytics is a feature nobody asked for.                                                                                                                                                                                                                      |
| **Stock is `finishedAt`, not a quantity**                                           | Marking "I ran out" is the whole need, and it's what feeds the dormant-tea prompt.                                                                                                                                                                                         |
| **Finished teas stay in the Log Tea picker**                                        | Hiding them makes the auto-clear unreachable.                                                                                                                                                                                                                              |
| **Host-JVM tests; screenshot tests from previews; no interactive Compose UI suite** | Revisited 2026-09-23. Screenshot tests reuse mandatory previews, run without an emulator, and catch the polish regressions that matter. Interaction suites still aren't worth their brittleness.                                                                           |
| **Accessibility basics are definition-of-done, not a launch gate**                  | Revisited 2026-09-23. Cheap per touch; the rating control — the loop's main input — is unusable with a screen reader today. One full screen-reader pass before store launch.                                                                                               |
| **Photos get safety, not features**                                                 | The author doesn't reread photos. They matter only because unchecked they break backups and storage.                                                                                                                                                                       |
| **Freeform journal entries are rejected**                                           | The author rarely writes notes attached to sessions; detached entries would be emptier. Tags and session notes cover the journal.                                                                                                                                          |
| **History grouping revived; the rest of the stashed redesign stays dead**           | Three of its five summary fields were Analytics content in a History costume.                                                                                                                                                                                              |
| **No streaks**                                                                      | An engagement mechanic that says nothing about tea. The shipped streak insight is removed in F1.                                                                                                                                                                           |
| **The session reminder stays inexact**                                              | `setAndAllowWhileIdle`; Doze spaces these ~9 minutes apart. Not worth the exact-alarm allowance for a soft nudge. The fix if it annoys is `setExactAndAllowWhileIdle`, mirroring the completion alarm.                                                                     |
| **Notifications get two switches, not one**                                         | Turning off the soft nudge must not kill the alert the app exists to deliver.                                                                                                                                                                                              |

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
  `TeaSession.resolvedTimerStatus(now)`. Gate live progress on matching session id. Known residual:
  a Home screen left open with no live timer won't re-resolve from time passing alone.
- **`AddSteepUseCase` copies `teaQuantityGrams` onto every child steep** (until F2 moves
  session-level fields to the parent). Summing leaf across all rows double-counts; leaf totals count
  parents only. Water is correctly summed across steeps.
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
- **`ZipOutputStream` defaults to DEFLATE with data descriptors** (sizes 0 in local headers). The
  hand-written iOS ZIP reader handles STORE only.
- **Kotlin `DayOfWeek.ordinal` is Monday = 0.** `today − (ordinal + 1)` on a Sunday lands on the
  previous Sunday and excludes today.
- **`PendingIntent` identity ignores extras.** Notifications sharing requestCode 0 and the same
  Intent all route to whichever was built last (suspected; confirm on device).
- **A `Snackbar` placed after a `fillMaxSize()` sibling in a `Column` gets zero height** and is
  never seen. Use `SnackbarHost` in a `Scaffold`.
- **`backStack.removeLast()` with compileSdk ≥ 35** may bind to JDK 21's `List.removeLast()`, absent
  below API 35 (unverified — check on an API 34 emulator; `removeAt(lastIndex)` is safe).

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
