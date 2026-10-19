## The Journal: History, Session Detail, Edit Session, Analytics

**Path legend:** `ui/` = `shared/src/commonMain/kotlin/dev/jketterer/leaflog/presentation/ui/`, `dom/` = `…/leaflog/domain/`, `dat/` = `…/leaflog/data/`, `ios/` = `shared/src/iosMain/kotlin/dev/jketterer/leaflog/`. Line numbers marked `~` are approximate to within a line or two. Everything below was checked by reading the code. Items marked *(unverified)* are inferences I didn't confirm at runtime.

---

### History (`ui/screens/history/HistoryScreen.kt`, `HistoryViewModel.kt`, `HistoryState.kt`, `ui/components/history/HistoryFilterSheet.kt`, `ui/components/session/SessionCard.kt`)

- **Purpose / verdict:** A reactive, filterable list of every `tea_session` row, child steeps included. Under the journal vision it is still a parameter list: cards lead with chips (g / temp / time / water), notes are cut to one line, and nothing tells a steep apart from a session. Several state bugs make it unreliable to reread. You leave, come back, and what you see isn't what you asked for.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | correctness (iOS) | `ios/domain/models/TimeFormatter.ios.kt:33-37,45-47` | **iOS relative timestamps are wrong for most past sessions.** "Is today" compares year and day-of-month only, with no month component. On Sep 23, every session from Jan 23 through Aug 23 shows as a bare clock time ("2:30 PM"), and every 22nd shows "Yesterday". | Uses `NSCalendarUnitYear or NSCalendarUnitDay` (day of month). Android correctly uses `DAY_OF_YEAR`. Every History card on iOS goes through this. |
| ~~bug~~ | ~~state~~ | ~~`HistoryScreen.kt:133`, `HistoryState.kt:42-57`~~ | ~~**An invisible search keeps filtering the list.** `showSearchBar` is plain `remember`, but `searchQuery` lives in the ViewModel. Coming back from Session Detail (Nav3 disposes the entry's composition but keeps its ViewModel), or rotating, hides the search bar while the list stays filtered. Nothing shows it: `activeFilterCount` and `hasActiveFilters` both ignore `searchQuery`.~~ | ~~The ViewModel is kept by `rememberViewModelStoreNavEntryDecorator` (`ui/navigation/AppNavigation.kt:104-107`).~~ |
| bug | copy/state | `HistoryScreen.kt:241-251` | A search with no hits says **"No sessions logged yet"** and offers no Clear action. | The empty-state branch keys off `hasActiveFilters`, which excludes the search query. |
| ~~bug~~ | ~~state~~ | ~~`HistoryScreen.kt:86-104`~~ | ~~**Filters from the route come back on every return.** The `LaunchedEffect`s keyed on route args re-fire when the entry re-enters composition. Clear the tea-type filter that came from Analytics, open a session, press back, and the filter is silently re-applied.~~ | ~~Same Nav3 lifecycle as above. The ViewModel survives but the effects run again.~~ |
| bug | correctness | `HistoryViewModel.kt:306-315` → `dom/usecases/session/DeleteSessionUseCase.kt:12-28` | **Deleting a child steep from a History card leaves the parent's `averageRating` stale and leaves gaps in steep numbers.** Session Detail renumbers and recomputes in its own ViewModel (`SessionDetailViewModel.kt:351-370`); the use case does neither. The dialog also says "delete the session and all steeps" (`HistoryScreen.kt:351`), which is false for a steep card. | Root cause: steep-delete logic lives in one caller instead of the shared use case. |
| bug | data integrity | `HistoryViewModel.kt:124-127`, `dat/local/database/dao/TeaDao.kt:15-19` | **Deleting a tea or vessel turns years of sessions into "Unknown Tea" / "Unknown Vessel".** They also lose the tea-photo fallback, the tea-type filter and name search. `DeleteTeaUseCase` soft-deletes only the tea, and `getAllFlow` excludes deleted rows. | Session Detail shows the real name for the same row, because `getById` doesn't filter `deletedAt` (`TeaDao.kt:30-34`). |
| bug | correctness | `HistoryViewModel.kt:209-213` vs `SessionCard.kt:189-193` | **The Minimum Rating filter tests the steep's `rating`, but the card shows `averageRating` first.** A session shown as 4.5★ is hidden by "≥4" when steep 1 was rated 3★, while its 5★ child steep card passes. | The filter and the display read different fields. |
| friction | multi-steep | `SessionCard.kt:69-248` | **Child steeps look exactly like separate brews.** The card never shows `steepNumber`. A steep 3 card carries the parent's copied grams and a full menu (Delete / Brew Again) that acts on that steep only. Roadmap 3.1 notes that child steeps are listed. New here: the card gives no cue, and the per-card actions and rating behave per steep. | No reference to `steepNumber` or `parentSessionId` anywhere in `SessionCard`. |
| friction | multi-steep | `HistoryViewModel.kt:317-336` → `dom/usecases/session/BrewAgainUseCase.kt:15-35` | "Brew Again" on a child card starts a new session using *that steep's* time and temperature. Steep 6's 90 s becomes the new steep 1. | The use case copies whatever row it is given. |
| ~~friction~~ | ~~nav~~ | ~~`ui/navigation/AppNavigation.kt:88-91`~~ | ~~Switching bottom tabs clears the back stack, so HistoryViewModel is rebuilt every time. Scroll position, search and filters are lost, and the full table is re-queried.~~ | ~~`backStack.clear(); backStack.add(route)`~~ |
| ~~friction~~ | ~~nav~~ | ~~`HistoryScreen.kt:202-227`~~ | ~~A filtered History pushed from Analytics, Tea Detail or Home has no back arrow and shows the bottom bar with History selected, so it looks like the root tab. Whether iOS has a back gesture here is *(unverified)*.~~ | ~~`TopAppBar` has no `navigationIcon`.~~ |
| friction | search | `HistoryViewModel.kt:221-228` | Search matches only tea name and notes. `location`, vessel, tea type and producer are ignored, even though `location` is stored and editable. | |
| friction | filter | `HistoryFilterSheet.kt:348+` | The Tea filter is an unsearchable dropdown listing every tea, including teas with no sessions. `TeaPickerSheet` (which has search) already exists. | |
| friction | empty | `HistoryScreen.kt:247-251` | The zero-sessions empty state has no call to action. Analytics' empty state has "Log Tea". | |
| consistency | semantics | `HistoryFilterSheet.kt:72-73` | The filter's "This Week" / "This Month" mean the rolling last 7 / 30 days. Analytics uses the same labels for the calendar week / month. History's "This Week" group means 2–7 days ago. | Three meanings for one label. |
| ~~consistency~~ | ~~nav~~ | ~~`AppNavigation.kt:178-180` vs `336-340`~~ | ~~Brew Again from History pushes Timer, so back returns to History. Brew Again from Session Detail clears the stack, so back lands on Home.~~ |  |
| ~~polish~~ | ~~layout~~ | ~~`SessionCard.kt:185-193`~~ | ~~The " • " separator appears only when `rating != null`, but stars are drawn when `averageRating != null`. The result is "Yesterday★★★★" when steep 1 is unrated.~~ | |
| polish | interaction | `SessionCard.kt:131-137` | "IN PROGRESS" is an `AssistChip` with `onClick = {}`. It swallows taps on that part of the card, and it uses `errorContainer` for a normal state. | |
| polish | journal | `SessionCard.kt:196-203` | Notes are cut to 1 line. The vision calls notes load-bearing. | |
| ~~polish~~ | ~~units~~ | ~~`SessionCard.kt:159`~~ | ~~Leaf is shown as a raw Float ("5.0g") and ignores `weightUnit`. `WeightFormatter` is only used in Analytics.~~ | ~~grep: `WeightFormatter` appears only in `AnalyticsViewModel`.~~ |
| ~~polish~~ | ~~errors~~ | ~~`HistoryScreen.kt:317-328`~~ | ~~Errors appear in a bare `Snackbar` placed in the Column (not a `SnackbarHost`) and show raw `e.message`.~~ |  |
| arch | CLAUDE.md | `HistoryFilterSheet.kt:93,111` | This public component takes the whole `HistoryState` plus `onIntent` and has no `modifier`. That breaks the rule that sections take the specific values they render. | |
| arch | perf | `HistoryViewModel.kt:123-162, 242-251` | Every tea_session / tea / vessel / prefs emission, and every search keystroke, re-runs filtering plus `groupByTimePeriod` (a `toLocalDateTime` per row) over the full table, child steeps included, on the Main thread. Fine today; likely to jank with years of data *(unverified)*. | `combine` is collected on `viewModelScope`, and the repository's `.map` runs in the collector's context. |

**Opportunities:**
- **One card per session:** show one card per parent with the steep count, the first-to-last span and a row of per-steep rating dots. Every field is already stored (`parentSessionId`, child timestamps). This removes the child-card bugs above (delete, min-rating, Brew Again, look-alike cards) as a class.
- **A reading layout, not a spec sheet:** lead with the session's photo strip and 3–4 lines of notes, and push the parameter chips down. Parameters are Analytics and Tea Detail material; the journal row is what you saw and wrote.
- **A calendar as History's navigator:** a month strip at the top (reusing `MonthCalendarHeatmapCanvas`'s layout) that jumps the list to a day. This solves moving through years of data without a fast-scroller.
- **Location as a journal dimension:** it is stored and edited but never shown, searched or filtered.
- **"Previous time with this tea"** link on each card (`getByTeaId` exists). It supports sequence without passing judgement, so it stays within the ownership table.

---

### Session Detail (`ui/screens/history/SessionDetailScreen.kt`, `SessionDetailViewModel.kt`)

- **Purpose / verdict:** The only place a session is shown whole: tea card, a "Session Details" parameter card, one card per steep, and Add Steep / Brew Again / Save as Method. It reads like a form printout. Photos are 72 dp thumbnails inside each steep card, there is no session-level note, and the steep sequence has no times. It also has a collector leak and an Add Steep path with no guards.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| ~~bug~~ | ~~lifecycle/perf~~ | ~~`SessionDetailScreen.kt:105-107` → `SessionDetailViewModel.kt:174-234`~~ | ~~**Every return adds another collector.** `LaunchedEffect(sessionId)` sends `LoadSession`, which launches a new `getByIdFlow().collect` with no job tracking (HistoryViewModel has `loadDataJob`; this has nothing). The ViewModel is kept in the back stack, so each Edit→back or TeaDetail→back adds one. Room invalidates per table, so every tea_session write, including TimerService's persistence, re-runs N × (getById + getChildSteeps + tea + type + vessel) and flashes the full-screen spinner (`isLoading = true`, line 176).~~ | ~~`AppNavigation.kt:104-107` keeps the ViewModel alive.~~ |
| bug | concurrency | `SessionDetailViewModel.kt:118, 391-438` | **Add Steep skips the in-progress guard.** Brew Again goes through `inProgressDelegate.checkAndProceed` (line 114); Add Steep calls `AddSteepUseCase` directly. Adding a steep to an old session while another brew is running creates a second IN_PROGRESS session and clears the stack to its timer. | |
| bug | journal integrity | `dom/usecases/session/AddSteepUseCase.kt:55` | **A steep added to a past session is stamped "now".** Add a steep to last Tuesday's session and it shows up as a separate card under "Today" and counts toward today's Analytics brew time and water. The timestamp can't be edited afterwards. | `timestamp = now` |
| bug | data safety | `SessionDetailViewModel.kt:285-296` | **Photo files are hard-deleted before a soft delete**, and they are deleted even if the row delete then fails. The History list's delete path doesn't touch files at all, so those become orphans. **New framing for roadmap 2.4:** an Undo snackbar would restore a session whose photos are already gone. Photo cleanup has to move to a hard-purge path first. | The row delete is `softDelete`, and `deletedAt` is designed for undo. |
| bug | consistency | `TeaDao.kt:30-34` | A deleted tea is still named here and its name still links to Tea Detail, while History shows "Unknown Tea" for the same row. | `getById`/`getByIdFlow` don't filter `deletedAt`. |
| friction | edit model | `SessionDetailViewModel.kt:98-106, 136-145` | **Two edit entry points with no overlapping fields for the same row.** The top-bar Edit covers vessel, water, leaf and location. Steep 1's pencil covers time, temperature, rating, notes and photos of that same parent row. There is no session-level note, so steep 1's note doubles as one. | |
| friction | nav | `AppNavigation.kt:336-340` | Add Steep sends you to the timer with the stack cleared, and after review SteepComplete goes Home. A steep added from the journal never brings you back to the session. | |
| friction | journal | `SessionDetailScreen.kt:488-631` | Steep cards leave out each steep's time of day, water and the interval between steeps. The per-row `timestamp` exists, so the rhythm of a gongfu session is invisible. | |
| ~~friction~~ | ~~prefs~~ | ~~`SessionDetailViewModel.kt:440-445`~~ | ~~The °C/°F toggle in the Add Steep dialog changes the app-wide preference.~~ |  |
| ~~consistency~~ | ~~format~~ | ~~`SessionDetailScreen.kt:210-224`~~ | ~~The date is hand-built as "September 23, 2026 at 2:30 PM": English month names, forced 12-hour clock, ignores the device's 24h setting. `TimeFormatter.formatClockTime` exists.~~ |  |
| ~~consistency~~ | ~~component reuse~~ | ~~`SessionDetailScreen.kt:562-573`~~ | ~~`SteepCard` re-implements `RatingDisplay` at 14 dp instead of 18 dp. Both floor the value (`rating.toInt()`), so a 3.9 average shows 3 stars.~~ |  |
| consistency | format | `SessionDetailScreen.kt:268, 537` | ~~Leaf is shown as `"${Float}g"`, and~~ duration via `Duration.toString()` instead of `formatBrewingTime`. | |
| polish | clarity | `SessionDetailScreen.kt:232-237` | The header shows `averageRating ?: rating` with no label, so "average of 5 steeps" looks the same as a single rating. It also goes stale after rating edits (Edit Session bug 3). | |
| polish | a11y | `SessionDetailScreen.kt:195-201` | The tea name is a clickable `Text` coloured primary, with no `Role.Button` and a touch target only as tall as the text. | |
| polish | copy | `SessionDetailScreen.kt:145, 252, 217-222+334-339` | "Session Details" is used as both the screen title and the first card's header. "Session not found" appears twice (empty state plus snackbar). | |
| polish | method | `SessionDetailScreen.kt:454` | ~~Save as Method passes `rating = parentSession.rating ?: 0f`, so the dialog shows 0★ when steep 1 is unrated.~~ It also saves only steep 1's parameters for a multi-steep session. | |
| arch | CLAUDE.md | `SessionDetailViewModel.kt:351-370` | Steep renumbering and average recomputation live in the ViewModel, but they are multi-step business logic. Their absence from `DeleteSessionUseCase` is the root cause of History's stale-average bug. | |

**Opportunities:**
- **Steep timeline:** a vertical timeline showing each steep's clock time, the interval since the previous steep, time/temp changes, and the rating and notes inline, with totals (session span, steep count, water). All of these fields are already stored.
- **Photo hero:** show the first photo from any steep at the top, opening the pager with tea / steep / date captions.
- **Previous/next session** navigation by timestamp, plus "previous session of this tea". Rereading as a sequence.
- **A session-level note and an editable date/time.** The data model lacks the note, and the timestamp is never editable.
- **Inline editing** (tap a steep field) instead of a trip to Edit Session.

---

### Edit Session (`ui/screens/history/EditSessionScreen.kt`, `EditSessionViewModel.kt`, `EditSessionState.kt`)

- **Purpose / verdict:** Corrects either the parent's session fields or one steep's parameters, notes and photos. It handles simple number fixes, but the edits that matter most for a journal quietly fail: you can't clear a note, rating or location, or switch to Tea Bag. Errors are never shown, and back gestures skip the discard guard.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | data | `EditSessionViewModel.kt:396-407` + `dom/usecases/session/UpdateSessionUseCase.kt:35,50,59,64` | **You can't clear notes, rating or location, or switch Loose Leaf → Tea Bag.** The ViewModel turns blank into `null`, and the use case treats `null` as "keep the existing value". Save reports success, navigates back, and the old value is still there. | The nullable "null means unchanged" API can't express "cleared". |
| ~~bug~~ | ~~errors~~ | ~~`EditSessionScreen.kt` (whole file)~~ | ~~**Errors are never shown, and neither is loading.** Save failure (`VM:438-445`), photo-save failure (`VM:355-357`) and "Session not found" (`VM:188-193`) are all silent; the Save button just re-enables.~~ | ~~The screen never reads `state.error` or `state.isLoading`.~~ |
| bug | ratings | `EditSessionViewModel.kt` (constructor) | **Rating edits never recompute `averageRating` or tea stats.** The parent's `averageRating`, which History cards, the Detail header, Analytics' average and Top Rated all read first, stays stale after you change any steep's rating. Only SteepComplete recomputes it. | No `UpdateAverageRatingUseCase` or `UpdateTeaStatsUseCase` is injected. |
| ~~bug~~ | ~~back~~ | ~~`EditSessionScreen.kt`~~ | ~~**No `BackHandler`.** Android back or an iOS swipe skips the discard dialog, loses the edits, and orphans newly added photo files (cleanup only runs in `confirmDiscard`, `VM:475-488`).~~ | ~~`BackHandler` exists only in EditTea, EditVessel and TeaPickerSheet.~~ |
| bug | multi-steep | `UpdateSessionUseCase.kt:55-70`, `AddSteepUseCase.kt:51-54` | **Edits to the session don't reach its child steeps.** `AddSteepUseCase` copied vessel, water type, leaf and location onto each child when it was created, and the edit updates one row only. Change the vessel and the child cards in History still show the old one, and still match the old vessel filter. | |
| bug *(likely, timing-dependent)* | race | `EditSessionViewModel.kt:111-131, 148-165` | **Sessions brewed in an archived vessel may not be editable.** `loadVessels` in `init` collects active vessels before `existingSession` is set. If that emits first, `availableVessels` is non-empty, so `loadSession` never adds the archived vessel. The result is `selectedVessel = null`: `isValid` is false and `hasChanges` is true (`EditSessionState.kt:56-64`). The full-session edit can't save, and Back asks to discard when nothing changed. | |
| friction | journal | `EditSessionState.kt` | The timestamp and the tea can't be edited, so a session logged late or under the wrong tea stays that way. | |
| ~~friction~~ | ~~input~~ | ~~`EditSessionViewModel.kt:224, 273`~~ | ~~Temperature and volume accept whole numbers only in the display unit: "6.5" fl oz and "92.5" are rejected. Editing in fl oz rounds the stored mL.~~ | ~~`toIntOrNull()`~~ |
| ~~friction~~ | ~~prefs~~ | ~~`EditSessionViewModel.kt:331-343`~~ | ~~The unit toggles change the app-wide preferences.~~ |  |
| friction | save | `EditSessionScreen.kt:120-128` | Save is only an icon-only check at the top, disabled until something changes. There is no bottom button on a long form. | |
| polish | copy | `EditSessionScreen.kt:104-108, 273` | The title reads "Edit Steep " while `steepNumber` is null~~, and the "g" suffix is hardcoded regardless of weight unit~~. | |
| polish *(edge)* | flow | `EditSessionScreen.kt:400-404` | After saving a 5★ rating, the save-method dialog only renders if `selectedVessel` and `teaName` are non-null. If not (see the archived-vessel race), the dialog flag is set but nothing appears and `NavigateBack` never fires, so you are stuck on the screen. | |
| arch | duplication | `EditSessionScreen.kt:248-279` vs `ui/components/common/EditSessionParametersSheet.kt` | The loose-leaf / tea-bag / quantity form is written twice. | |
| ~~arch~~ | ~~testability~~ | ~~`EditSessionViewModel.kt:219-308`~~ | ~~Unit parsing and validation sit in the ViewModel. They are pure-function candidates, the same pattern as `DurationFields`.~~ |  |

**Opportunities:**
- **Explicit patch type for session updates**, e.g. per-field `Keep / Set / Clear`, replacing the "null means unchanged" parameters. This fixes the whole class behind the clearing bug.
- **One journal-edit surface:** date/time, tea, a session note, and one row per steep. Retire the split between parent edits and steep edits.
- **Notes prompts (roadmap 3.3)** fit here and on SteepComplete.

---

### Analytics (`ui/screens/analytics/*`, `ui/components/analytics/*`, `dom/usecases/session/Get*UseCase.kt`, `GenerateInsightsUseCase.kt`)

- **Purpose / verdict:** Period aggregates: summary cards with period-over-period change, a heatmap, insights, a trend line, two donuts and two bar lists. Multi-steep aggregation is mostly right: leaf and counts use parents only, water and time use all steeps. But it has period-boundary bugs, broken tap maths, one-shot loading, and a streak insight that contradicts a recorded decision. Its links back into History are partly broken, which matters because that link is where Analytics serves the journal.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| bug | date boundary | `AnalyticsViewModel.kt:368-372` | **On Sundays, "This Week" leaves out today.** `startOfWeek = today.minus(dayOfWeek.ordinal + 1)` with Monday = 0 gives, on a Sunday, today−7 up to today 00:00. The whole screen (cards, comparison, heatmap with no today ring) shows last week, and today's brews don't appear. | kotlinx `DayOfWeek` ordinal: MONDAY = 0 … SUNDAY = 6. |
| bug | chart math | `TeaTypeDistributionChart.kt:185-197`, `VesselDistributionChart.kt:184-187` | **Donut taps hit the wrong segment.** The tap angle comes from `atan2` (0° at 3 o'clock), but segment starts are shifted by +90° (0° at 12 o'clock). The hit test is rotated 90°, so tapping a tea type opens History filtered by the segment a quarter-turn counter-clockwise. | `normalizedStart = (startAngle + 90 + 360) % 360` is compared against an unshifted angle. |
| bug | chart math | `BrewingTrendsChart.kt:142-144` | **Tapping a trend point usually picks its left neighbour.** The index is truncated instead of rounded, and the loose `stepX/2 + 20dp` proximity check accepts it. There is no highlight before it navigates. | `.toInt()` |
| bug | chart | `BrewingTrendsChart.kt:165` | **The y-axis labels aren't evenly spaced.** Integer division gives labels 0, 1, 2, 4 at equal heights when `yAxisMax = 4`. | `(yAxisMax * i) / ySteps` |
| bug | date boundary | `dom/usecases/session/GetBrewingTrendsUseCase.kt:39` | **The trend line has a phantom trailing day and plots future days as zero.** The exclusive end instant is turned into an inclusive date. The heatmap use case handles this correctly (`GetBrewingActivityUseCase.kt:~64` `toExclusiveLocalDate`); trends doesn't. This Week / This Month / Last Month gain the next period's first day, and the rest of the current week or month is drawn as zero, so the line always dives at the end. | |
| bug | decision conflict | `GenerateInsightsUseCase.kt:102-114` | **A streak insight exists:** "Your longest brewing streak was N days in a row". The roadmap's Decisions table says **No streaks**. Not listed in the roadmap. | |
| bug | dead feature | `PeriodSelector.kt:45` | **Custom date range can't be reached.** CUSTOM is filtered out of the menu and nothing sends `SelectCustomRange`. New evidence against roadmap 2.7 ("custom range doesn't survive restart"): the range is never selectable, so that item is moot. Its label would also render as "SEP 1 - SEP 30" (`AnalyticsViewModel.kt:174`). | |
| bug | date boundary | `AnalyticsViewModel.kt:353-366` | **"Last N days" periods are off by one:** Last 7 = 7 days, Last 30 = 31 days, Last 90 = 91 days. | |
| bug | data integrity | `GetTopTeasUseCase.kt:~19`, `GetTeaTypeDistributionUseCase.kt:~23`, `GetVesselDistributionUseCase.kt:~23` vs `GetAnalyticsUseCase.kt` | **Totals and breakdowns disagree when teas or vessels are deleted.** Sessions of soft-deleted teas/vessels count in the totals but drop out of every breakdown (`mapNotNull` over a `getAll()` that excludes deleted rows). The donut's centre total ends up below the Sessions card with no explanation. | |
| bug | heatmap | `GetBrewingActivityUseCase.kt:~150`; `dat/repositories/TeaTypeRepositoryImpl.kt:~77` | **Some active days are invisible on the heatmap.** A day whose tea types have a blank colour draws as an empty outline (`dominantNonEmpty` → null). That only happens with legacy or imported data, because the editor requires a colour. Separately, **White tea's seeded colour `#FFFDE7`** fills heatmap cells and its donut segment almost invisibly on light surfaces. `TeaTypeListScreen.kt:184` special-cases that colour; the charts don't. | |
| bug | heatmap | `BrewingActivityHeatmap.kt:373` | **The year-style heatmap orders months ignoring the year.** "Last 90 Days" viewed in Jan–Mar puts January before Oct/Nov/Dec. | `groupBy { it.date.month }.sortedBy { ordinal }` |
| bug | drill-down | `AnalyticsViewModel.kt:104-108` | **Tapping a tea type loses the period.** The donut shows, say, this week's split, but the tap opens History for all time. | Only `filterTeaTypeId` is passed. |
| friction | drill-down | `AnalyticsViewModel.kt:114-115` | Tapping a vessel does nothing, with the comment "no vessel detail screen yet", but `NavRoute.VesselDetailRoute` exists. | |
| friction | loading | `AnalyticsScreen.kt:117-124` vs `232-240` | **Every period change blanks the whole screen.** The loading state replaces everything, including the period selector (which lives inside the data content). | |
| ~~friction~~ | ~~errors~~ | ~~`AnalyticsScreen.kt:126-140`~~ | ~~**The error state is a dead end:** red text, no Retry, and the period selector is hidden.~~ |  |
| friction | freshness | `AnalyticsViewModel.kt:180-341` | **Analytics goes stale.** It loads once with suspend calls and never observes a Flow, so it doesn't update after edits made in History or Detail until the ViewModel is rebuilt. | |
| friction | ranking | `GetTopRatedTeasUseCase.kt:23-37` | **Top Rated has no minimum sample.** One 5★ session outranks a tea with 20 sessions averaging 4.8. The insight requires at least 2 rated sessions; the chart doesn't. | |
| friction | gating | `AnalyticsViewModel.kt:197` | A single all-time gate of 10 sessions hides everything, including the heatmap. Roadmap 3.2 prefers gating each surface separately. | |
| consistency | rounding | `TopRatedTeasChart.kt:170-171` vs `GenerateInsightsUseCase.kt:81` | The chart truncates (4.75 → "4.7") while the insight rounds ("4.8"), on the same screen. | |
| consistency | week start | `GetBrewingTrendsUseCase.kt:79`, `GetBrewingActivityUseCase` `toWeekMonday` | This Week starts on Sunday, but weekly trend buckets, weekly heatmap cells and the weekly tap-to-History range all start on Monday. | |
| ~~polish~~ | ~~copy~~ | ~~`TopRatedTeasChart.kt:132`~~ | ~~"1 sessions".~~ |  |
| polish | framing | `SummaryCard.kt:~97-109` | Fewer sessions or less tea is coloured `error` red: evaluative in a way the journal vision avoids. The arrow's `contentDescription` is null, so a screen reader hears "15%" with no direction. | |
| polish | icons | `AnalyticsScreen.kt:291, 349` | Unique Teas uses a Star (reads as rating); New Teas uses Search. | |
| polish | theming/i18n | `BrewingTrendsChart.kt:96, 237`; `BrewingActivityHeatmap.kt:41-42`; `ChartUtils.kt`; `VesselDistributionChart.kt:82` | ~~Hardcoded English month arrays, US "M/d" labels,~~ and 8–10 sp canvas text in cells capped at 28 dp (overflows at large font scale). Vessels use a fixed Material-2 palette. | |
| a11y | touch targets | `TopTeasChart.kt:129`, `TopRatedTeasChart.kt:123`, donut legends | **Hard to hit:** bar rows are about 24 dp tall, names are clipped at a fixed 100 dp, and legend rows aren't tappable, so a donut segment of about 10° is the only way in. The roadmap already accepts that Canvas charts are invisible to screen readers (Phase 4); note that adding a `Modifier.semantics { contentDescription }` summary is one line per chart. | |
| arch | perf | `AnalyticsViewModel.kt:189-296`, `GetSteepInsightsUseCase.kt:35,50` | **Each load runs about 12 date-range queries, 2 full-table `getAll()` session loads and about 6 `teaRepository.getAll()` calls, one after another.** All Time means 14 full-table loads. The "≥10 sessions" check loads every row just to count (189-195). Aggregation runs on Main, so jank at years of data is likely *(unverified)*. | |
| arch | testability | `AnalyticsViewModel.kt:199-274, 344-407` | The period comparison and `calculateDateRange` sit in the ViewModel with no tests. The Sunday bug lives there, and the function is pure, so it's trivially testable on the host JVM (Track A). | |

**Opportunities:**
- **One query per load, pure aggregators:** fetch `[previousStart, end)` once and pass the list into pure functions. That is about 10× fewer queries and makes every aggregation testable on the host JVM (Track A), including the date-boundary bugs above.
- **Links back into the journal:** tap a heatmap day to open History for that day; the donut *and legend* open History with the current period; tapping a top tea opens History for that tea in that period. That is how Analytics serves "History is the centerpiece".
- **Season / month recap card (roadmap 3.2)** built on the same aggregators with a date window.
- **Parameter distributions** (temperature and steep-time histograms per tea type). These are aggregates, so they fit Analytics' side of the ownership table, and the fields are already stored.
- **Remove the streak insight** per the decision, or replace it with something about tea (e.g. "3 new teas this month").

---

### Shared components (`ui/components/common/PhotoGrid.kt`, `FullscreenImageViewer.kt`, `EditSessionParametersSheet.kt`, `RatingDisplay.kt`, `BrewingParamChip.kt`)

- **Purpose / verdict:** Solid building blocks with a few contract gaps.

| Sev | Category | Location | Finding | Evidence/why |
|---|---|---|---|---|
| arch | CLAUDE.md | ~~`RatingDisplay.kt:24`,~~ `BrewingParamChip.kt:23`, `FullscreenImageViewer.kt:46`, `EditSessionParametersSheet.kt:40` | These public components have no `modifier: Modifier = Modifier` parameter. | |
| ~~consistency~~ | ~~rating~~ | ~~`RatingDisplay.kt:24-50`~~ | ~~It floors the value, so averages show too few stars. Text renders as "(4.0)". Stars have `contentDescription = null` and no merged semantics, so a screen reader hears "(4.5)" with no context.~~ |  |
| a11y | touch | `PhotoGrid.kt:79` | The remove-photo button is 24 dp and removes with no confirmation or undo. Descriptions are a generic "Session photo". | |
| polish *(likely)* | layout | `EditSessionParametersSheet.kt:118` | The sheet's Column has no `verticalScroll`. In landscape or at large font scale, Save can end up off-screen *(unverified on device)*. | |
| polish | viewer | `FullscreenImageViewer.kt` `ZoomableImage` | Double-tap zooms around the centre rather than the tap point. Each page's `scale` isn't reset when you page away, while the pager's `currentScale` is, so an adjacent page that stays composed may be out of sync *(unverified)*. | |
| arch | API | `EditSessionParametersSheet.kt:40-60` | A 19-parameter signature that duplicates Edit Session's form. | |

**Opportunities:** Add tea / steep / date captions to the fullscreen viewer, so a photo can be reread in context rather than as a bare image.

---

### Cross-screen patterns

1. **What counts as a "session" is decided separately in each file.** `rating` vs `averageRating` (History card vs min-rating filter vs `UpdateTeaStatsUseCase.kt:18-19`, which uses steep 1's `rating` for the tea average while `GetTopRatedTeasUseCase.kt:28` uses `averageRating ?: rating`). Steep delete (full logic in one ViewModel, none in the use case). Brew Again from a child steep. Steep number never shown. `AddSteepUseCase` copies parent fields that later edits don't update, and stamps steeps "now". **Recommendation:** a `SessionWithSteeps` domain aggregate, plus use cases that own every steep change (delete, edit rating, add). Recomputing the average and the tea stats then happens in one place, and History and Detail can't drift apart.
2. **Deleted teas/vessels are looked up two different ways.** List queries (`getAllFlow` / `getAll`) exclude soft-deleted rows; `getById` doesn't. The same session is "Unknown Tea" in History, named in Detail, and counted in Analytics totals but not its breakdowns. Decide whether a journal ever hides a deleted tea's name. Roadmap 2.3's `finishedAt` is the natural alternative to deleting.
3. **Loading from a `LaunchedEffect` with ViewModels that outlive the screen.** Session Detail stacks a collector on every return; History re-applies route filters; `remember`-only UI flags drift from ViewModel state. The pattern: pass route args into the ViewModel (e.g. Koin `parametersOf`) and load in `init`, and use `rememberSaveable` or ViewModel state for flags that gate what the ViewModel shows.
4. ~~**Error handling is ad-hoc.** Errors show as bare inline Snackbars (History, Detail), full-screen text with no retry (Analytics), or not at all (Edit Session), and always as raw `${e.message}`. These are concrete cases for the Phase 4 user-facing-errors item.~~
5. ~~**Formatting isn't centralised.** `WeightFormatter` is used only in Analytics ("5.0g" everywhere else). Dates are hand-built in Session Detail, the filter sheet, the Analytics custom label, the trend chart and the heatmap, with three separate English month tables. The iOS relative-date bug. No string resources anywhere in the project. One date/format `expect`/`actual` would cover it.~~
6. **Period words mean different things.** "This Week" means a Sunday-start calendar week (Analytics, with the Sunday bug), Monday-start buckets (trends and heatmap weekly), the rolling last 7 days (History filter), or "2–7 days ago" (History grouping).
7. ~~**Unit toggles in forms rewrite global preferences** (Edit Session temp/volume, the Detail Add Steep dialog, Analytics cards). This may be intended, but it's invisible.~~
8. **Photo file lifecycle is spread across ViewModels** (Session Detail delete, Edit Session, SteepComplete discard), with files hard-deleted on a *soft* delete, while the History list's delete orphans them. This has to be consolidated before roadmap 2.4's Undo, or Undo restores sessions with missing photos.
9. ~~**The back stack is inconsistent.** Tab switches clear it (History and Analytics state lost each time), and flows that start the Timer sometimes clear it and sometimes don't. The journal loses your place.~~
10. **Components break the CLAUDE.md contract:** missing `modifier`, the filter sheet takes the whole State, and duplicated renderers (stars, tea-quantity form).
11. **Touch targets under 48 dp** throughout (photo remove button, chart rows, donut segments, clickable tea-name text).
12. **Time zones (design note, not a bug):** every day bucket uses the device's *current* zone at read time, so sessions logged while travelling move to a different day when you get home. If that matters for a journal, store the UTC offset with each session.
