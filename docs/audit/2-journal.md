## The Journal: Analytics (History, Session Detail and Edit Session cleared in 0.6.0)

**Path legend:** `ui/` = `shared/src/commonMain/kotlin/dev/jketterer/leaflog/presentation/ui/`, `dom/` = `…/leaflog/domain/`, `dat/` = `…/leaflog/data/`, `ios/` = `shared/src/iosMain/kotlin/dev/jketterer/leaflog/`. Line numbers marked `~` are approximate to within a line or two. Everything below was checked by reading the code. Items marked *(unverified)* are inferences I didn't confirm at runtime.

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
3. ~~**Loading from a `LaunchedEffect` with ViewModels that outlive the screen.** Session Detail stacks a collector on every return; History re-applies route filters; `remember`-only UI flags drift from ViewModel state. The pattern: pass route args into the ViewModel (e.g. Koin `parametersOf`) and load in `init`, and use `rememberSaveable` or ViewModel state for flags that gate what the ViewModel shows.~~
4. ~~**Error handling is ad-hoc.** Errors show as bare inline Snackbars (History, Detail), full-screen text with no retry (Analytics), or not at all (Edit Session), and always as raw `${e.message}`. These are concrete cases for the Phase 4 user-facing-errors item.~~
5. ~~**Formatting isn't centralised.** `WeightFormatter` is used only in Analytics ("5.0g" everywhere else). Dates are hand-built in Session Detail, the filter sheet, the Analytics custom label, the trend chart and the heatmap, with three separate English month tables. The iOS relative-date bug. No string resources anywhere in the project. One date/format `expect`/`actual` would cover it.~~
6. **Period words mean different things.** "This Week" means a Sunday-start calendar week (Analytics, with the Sunday bug), Monday-start buckets (trends and heatmap weekly), the rolling last 7 days (History filter), or "2–7 days ago" (History grouping).
7. ~~**Unit toggles in forms rewrite global preferences** (Edit Session temp/volume, the Detail Add Steep dialog, Analytics cards). This may be intended, but it's invisible.~~
8. **Photo file lifecycle is spread across ViewModels** (Session Detail delete, Edit Session, SteepComplete discard), with files hard-deleted on a *soft* delete, while the History list's delete orphans them. This has to be consolidated before roadmap 2.4's Undo, or Undo restores sessions with missing photos.
9. ~~**The back stack is inconsistent.** Tab switches clear it (History and Analytics state lost each time), and flows that start the Timer sometimes clear it and sometimes don't. The journal loses your place.~~
10. **Components break the CLAUDE.md contract:** missing `modifier`, ~~the filter sheet takes the whole State,~~ and duplicated renderers (stars, tea-quantity form).
11. **Touch targets under 48 dp** throughout (photo remove button, chart rows, donut segments, clickable tea-name text).
12. **Time zones (design note, not a bug):** every day bucket uses the device's *current* zone at read time, so sessions logged while travelling move to a different day when you get home. If that matters for a journal, store the UTC offset with each session.
