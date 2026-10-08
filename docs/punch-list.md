# Temp punch list (delete when done)

Issues found from daily use. One line per issue: what's annoying, then what it
should do instead. Add sub-bullets only for decisions a later session needs.

Working an item: take the first unchecked one, check it off when it's done.

---

## Format

- [ ] Short title — the annoyance. **Should:** what it does instead.
    - decision/context worth carrying forward (optional)

## Items

- [x] iOS live notification is flaky
    - timer sometimes starts counting backwards after hitting 0:00, sometimes doesn't update
      automatically, etc.
    - Counting up past 0:00: the widget used `Text(_:style:.timer)`, which keeps ticking upward
      once its date passes. The `isComplete` guard couldn't catch it because the activity body
      isn't guaranteed to re-render at the moment the timer elapses. Now uses
      `Text(timerInterval:countsDown:)`, which is bounded by the range and holds at 0:00.
    - Stuck/stale updates: `cancelCompletionAlarm()` pushed an `isPaused = true` Live Activity
      update, and every alarm reschedule cancels first, so restarting the countdown or adjusting
      the time froze a running timer into "Paused". Pausing is now explicit via a new
      `TimerNotificationService.showTimerPaused(state)`; `cancelCompletionAlarm` only withdraws
      the pending notification. The spurious pause also cancelled the Live Activity's auto-end
      task, which is why activities sometimes outlived their brew.
    - Android gained a real paused notification from the same change (it previously relied on the
      same side effect and just left the Chronometer ticking).
    - NOT VERIFIED: the Swift widget change is unbuilt, no macOS toolchain here. Kotlin side
      compiles for both targets. Check the countdown on a real brew before trusting it.
- [x] In-progress session indicators are flaky
    - if i leave the app and open it later, i will see inconsistent UI for the in-progress session
    - Narrowed to the Home banner, symptom "wrong button". The button text and the navigation
      answered the same question from different sources: the label used live TimerService progress
      while `handleResumeSession` branched on the persisted `session.timerStatus`. So it could read
      "Finish" and still open the timer screen.
    - Fixed by deriving from wall-clock in one place: `TeaSession.resolvedTimerStatus(now)`. Label,
      progress bar and routing all read it now. Display-only, no writes on the read path.
    - Also fixed: the banner rendered `liveTimerState.progress` without checking the timer belonged
      to the session being shown, so one session's countdown could appear on another's card. Now
      gated on matching session id via `HomeState.inProgressTimerProgress`.
    - Known gap: the resolved status is computed when the in-progress flow emits, so a Home screen
      left open with no live timer won't re-resolve purely from time passing. Covered in practice
      because TimerService persists COMPLETE while the app is alive, which re-triggers the flow.
- [x] Opening the app after a session timer finishes sometimes brings you back to the timer screen
    - It should bring you to the review screen, but the timer screen fires instead. Going back
      brings you back to the correct screen
    - Two causes, both fixed. (1) iOS notification taps hardcoded `NavRoute.TimerRoute`; now the
      scheduled completion alarm carries a `destination` in userInfo and routes to
      `SteepCompleteRoute`, mirroring Android's intent extras. (2) `TimerViewModel` prefers the
      live `TimerService` state, which stops advancing while backgrounded, so the
      already-complete redirect didn't fire; it now re-derives via `TimerState.resolvedAt(now)`
      before deciding.
    - Left alone deliberately: when the timer resolves to COMPLETE on open we still return early
      without persisting, so the DB can say RUNNING while the UI says done. That's the item above.
- [x] In-progress sessions have no reminder to finish them
    - I don't know if this is solved with a scheduled notification, or if we should rethink the
      actual workflow
    - Went with a scheduled local notification that opens the review screen. Delay is
      `SessionReminderDelay.forBrewingTemperature`: 5 min at 70C rising to 15 min at 95C, on the
      theory that a near-boiling black tea is still too hot when a cooler green is drinkable.
      Unknown temperature (quick timers) gets the 10 min midpoint.
    - Scheduled when the steep *starts* (delay = remaining brew + cooldown), same as the completion
      alarm, so it survives the process being killed mid-brew. Cancelled on pause, stop, discard,
      and on completing the session; rescheduled on time adjust and on each new steep, so only the
      final steep's reminder actually survives to fire.
    - `CompleteSessionUseCase` now takes `TimerService` and stops it when the completed session is
      the one being timed. Without this, finishing via the conflict dialog left the reminder
      scheduled and it fired for an already-finished session.
    - NOT covered: quick timers. Their session doesn't exist yet when the timer starts, so there is
      nothing to schedule against. Left alone rather than half-wired.
    - Known residual: if the process is killed and you then complete the session via the conflict
      dialog, TimerService has no state to match on, so the reminder isn't cancelled and will fire
      once for a finished session. Android could check status in the receiver; iOS can't, since the
      notification fires without running app code.
    - BUG FOUND IN USE, FIXED: the reminder never fired. `TimerViewModel.completeTimer` calls
      `timerService.stop()` when a steep finishes (to reset the singleton for the next steep), and
      `stop()` was cancelling the reminder. So it died at the exact moment it became relevant.
      Confirmed via `adb shell dumpsys alarm`: `Reason=alarm_cancelled`, with the completion alarm
      cancelled 7ms earlier, matching the call order inside `stop()`.
    - Conceptual mistake behind it: `stop()` meant "timer torn down", and I loaded it with "session
      no longer needs review". A steep finishing is when review becomes PENDING. Now split, `stop()`
      leaves the reminder alone, and `TimerService.onSessionResolved(sessionId)` withdraws it from
      the finish/discard/delete paths, guarded so resolving an old session can't cancel a live
      brew's reminder.
    - Same root cause had silently killed the `CompleteSessionUseCase` guard added with this item:
      by the time a session completes, `stop()` has already cleared `_timerState`, so
      `getCurrentState().sessionId == session.id` never matched. That path now uses
      `onSessionResolved` too.
    - VERIFIED END TO END on the Android emulator: reminder scheduled, survived steep completion,
      fired, and the notification posted.
    - EXPECTED, NOT A BUG: the reminder arrives late, by minutes. Android schedules it with
      `setAndAllowWhileIdle`, which is inexact; observed on the emulator with a 6 minute delivery
      window (`window=+6m7s`, `count=0` while overdue), and Doze spaces these alarms ~9 minutes
      apart on a real device. Decision made deliberately to keep it inexact rather than spend the
      exact-alarm allowance on a soft nudge. Consequence to accept: the temperature curve sets a
      target, not a guarantee, so treat the 5-15 min cooldown as "roughly when" rather than "at".
      If a late reminder ever becomes annoying, the fix is `setExactAndAllowWhileIdle`, mirroring
      the completion alarm, which already has the `canScheduleExactAlarms()` fallback.
- [x] Let notifications be turned off in-app
    - Two switches in a new "Notifications" section in Settings: brew complete, and finish-your-
      session. Both default on. Chose separate switches over one master toggle so turning off the
      soft nudge doesn't also kill the alert the app exists to deliver.
    - Android already exposed per-channel control in system settings (the reminder has its own
      channel); iOS has no equivalent, so the in-app toggle is what gives parity there.
    - `TimerService` mirrors the preference flow into a StateFlow so the countdown loop can check
      it without suspending, and re-applies on change: flipping a switch mid-brew withdraws or
      restores the scheduled alert for the brew already running, not just the next one.
- [ ] Make the steep review easier or more valuable  (IN PROGRESS: loop closed, screen not touched)
    - Split out of the reminder item. I skip through rating/review to get to the timer, which says
      the review isn't earning its place. A reminder to do a thing I don't value just becomes a
      notification I dismiss. Deliberately did NOT solve this by letting the notification skip
      review, since that hollows out the screen instead of fixing it.
    - Root cause found: rating a steep was a dead end. It fed `averageRating`, which was only ever
      displayed. Prefill came exclusively from saved BrewingConfigurations (a separate explicit
      dialog), so a session rating never influenced any future brew. The app asked for a grade that
      changed nothing.
    - DONE: `GetBrewingParametersPrefillUseCase` gained step 2, prefill from the highest-rated past
      session of this tea + vessel, with `PrefillSource.BestRatedSession(rating)` and a banner
      reading "From your best brew of X, rated 4.5". Explicit saved configs still win (step 1),
      same-type configs drop to step 3.
    - Only parent sessions count (their params describe steep 1, which is what a new session
      starts) and a 3.0 rating floor applies, since reproducing a cup you disliked is worse than a
      generic same-type starting point. Rating used is `averageRating ?: rating`.
    - STILL TO DO, agreed but deliberately deferred to see if the loop alone is enough:
      show the payoff on the review screen itself ("best brew of this tea so far" + how this steep
      compares), keeping the existing rating/notes/photo inputs.
    - NOT runtime-tested. Worth checking against a tea you have brewed several times.
- [x] We should track quantity/weight of tea as a card on the analytics/insight screen
    - Two cards in a new row: "Tea Used" (period total) and "Avg per Session".
    - Counts PARENT sessions only. `AddSteepUseCase` copies `teaQuantityGrams` onto every child
      steep, so summing all completed sessions would report the same leaf once per steep: a 5g
      gongfu session with 6 steeps would have read 30g. Water is still summed across all steeps,
      correctly, since each steep uses fresh water.
    - `teaQuantityGrams` is optional, so the total carries "from N of M sessions" when some
      sessions lack a weight, and the whole row is hidden when none have one (a bare "0.0 g" reads
      as a measurement rather than as missing data).
    - Tap either card to toggle g/oz, mirroring the Water Used card. Needed a new `WeightUnit`
      preference + `WeightFormatter` (auto-upgrades to kg/lb on large totals).
    - Not done: no weight-unit row in Settings, though temperature and volume both have one. The
      tap toggle persists the preference, so a Settings row would only add discoverability.
- [ ] History screen is basic and boring, with little purpose  (SHELVED)
    - Built it, didn't like the result, stashed the diff. No current ideas for a better direction.
      The design notes below are kept in case something surfaces later; treat them as a record of
      what was tried and rejected rather than a plan to resume.
    - should brainstorm ways to add functionality or make it more interesting
    - Reframed: per-tea progression belongs on Tea Detail, not History. Tea Detail already owns the
      per-tea view (sessions, saved methods) but shows only `totalSessions` and `averageRating`, so
      "best brew" and param/rating evolution go there. Analytics owns period aggregates. That
      leaves History as the chronological view, and its real problem is that nothing invites
      browsing: the cards are already rich (photo, params, rating, Brew Again).
    - The "readable journal" idea was cut back hard after checking it against Analytics. Analytics
      already reports per-day session counts (`ActivityCell` in the heatmap), daily/weekly/monthly
      trend points, month totals via THIS_MONTH/LAST_MONTH, and most-brewed teas via TopTeas. Of
      the five summary fields originally agreed, three were Analytics content in a History costume.
      `SessionCard` already shows photo, params, rating, notes and Brew Again, so the per-session
      detail is not untapped either.
    - Agreed (narrowed) design:
      - Group per-day for the last 30 days, per-month beyond that, replacing the current
        Today/Yesterday/This Week/This Month/<year> buckets, which lump a whole week together and
        dump everything older into one bucket per year. Keep "Today"/"Yesterday" wording. This
        grouping fix is the part that stands on its own merit and duplicates nothing.
      - Day header line only on days with 2+ sessions: teas drunk and the first-to-last cup span.
        These two are the only agreed fields Analytics does not already report (the heatmap has a
        dominant tea *type colour*, not tea names; TimeInsights is aggregate, not per-day).
      - NO session counts anywhere: that is the heatmap's job.
      - Month headings carry no summary at all.
      - Deliberately NOT evaluative: no "standout brew of the day". Judgement lives on Tea Detail.
      - Filters active => drop back to today's flat list with no summaries. A filtered journal is a
        search result, and a summary describing only matching sessions misreports the day.
    - What was built, then stashed: `groupByTimePeriod()` replaced with `groupIntoSections()`, `HistoryState.groupedSessions`
      (a Map) replaced with an ordered `List<HistorySection>` carrying an optional `headerLine`.
    - History lists child steeps as their own cards (`getAllFlow` has no parent filter, pre-existing
      and left alone), so the "2+ brews" gate counts parent sessions only: a 6-steep gongfu session
      must not read as six separate brews. The time span still covers every steep, since a later
      steep is still a cup that was drunk.
    - Search counts as narrowing too, not just the filter sheet: `HistoryState.isNarrowed`.
    - NOT runtime-tested beyond compiling and installing. Worth checking a day with 2+ brews shows
      the header, a single multi-steep day does not, and that filtering makes headers disappear.
    - Retrospective hook dropped for now. "On this day" is empty until a year of data exists;
      streaks are an engagement mechanic that says nothing about tea; dormant-teas is the good one
      but must not suggest brewing tea you have run out of. See the stock item below.
- [ ] Track remaining stock per tea
    - Prerequisite for a "you haven't brewed X in a while" prompt, which otherwise risks suggesting
      tea that's gone. `Tea.stockAmount` already exists in the entity, model and migrations, is set
      to null by `CreateTeaUseCase`, and is never read, written or shown: a dead field.
    - Sessions already record `teaQuantityGrams`, the same value the Tea Used analytics card sums,
      so the decrement input exists. Open questions: units (stockAmount is an Int today), whether
      to decrement automatically per session or track manually, and what happens at zero.
- [ ] Notification sound is stock and easy to miss  (SHELVED, needs an audio asset)
    - Confirmed on device: the `timer_complete` channel is `mSound=.../notification_sound`,
      `usage=USAGE_NOTIFICATION`, `mVibrationEnabled=false`, `FLAG_MUTE_HAPTIC`. Default tone, at
      notification volume, no vibration.
    - TRAP for whoever picks this up: NotificationChannel sound, importance and vibration are
      immutable after creation. `timer_complete` already exists on installed devices, so changing
      it in `createNotificationChannels()` is silently ignored. It needs a NEW channel id.
    - Decisions taken before shelving: keep the notification tone rather than moving to alarm
      volume, add vibration, and apply it to brew-complete only. The finish-your-session reminder
      stays quiet, since making a soft nudge loud is how it earns being switched off.
    - Shelved because the real fix wants a custom sound file, which does not exist in the project
      (no `res/raw`, no audio anywhere) and cannot be authored here. iOS especially: without a
      bundled file its only louder option is a Critical Alert, which needs an Apple-approved
      entitlement a tea timer is unlikely to get.
- [ ] Tea Detail: show best brew and how a tea has evolved
    - Came out of the History grilling: Tea Detail is the natural home for per-tea judgement, and
      it currently shows only `totalSessions` and `averageRating` despite already holding the
      tea's sessions and saved methods.
    - Reuses the "best brew" concept the prefill loop introduced, so the tea's page and the
      pre-filled parameters would agree on which brew was best.
    - The division of responsibility that came out of that grilling, worth keeping: Analytics owns
      counts and aggregates, Tea Detail owns per-tea judgement, History owns sequence and
      retrieval. It is what ruled several ideas out of History.
