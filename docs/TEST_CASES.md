# Leaf Log — Device Checklist (Track B)

> **Last trimmed:** 2026-10-08 · See ROADMAP.md § 3.

The manual pass for what host tests and screenshots can't reach: wall-clock timing, process death,
notifications, Live Activity, permissions, pickers, and Activity recreation. Logic, rules, and
layout belong in Track A and Track S, not here.

**When to run:** before tagging a release, and after any change to `TimerService`, notifications,
the Live Activity, deep links, `FileOperations`, or `MainActivity`. Run the affected section; run
everything before a release.

**Rule:** platform integration ships with a line here. A line that can be tested on the host moves
to Track A, or Track F if it is a flow through the app, and is deleted from this file.

Tags: **[A]** Android only · **[iOS]** iOS only · untagged = both.

---

## 1. Timer lifetime

- [ ] Running timer → background 30s → return. Remaining time is correct; if it elapsed while
      backgrounded, the timer shows complete.
- [ ] Running timer → lock the device past the end time. Completion alert fires on time.
- [ ] Running timer → force-stop → wait past the end time without opening the app. Completion
      alert still fires (backup alarm on [A], scheduled notification on [iOS]).
- [ ] **[A]** Revoke "Alarms & reminders" in system settings → run a 1-minute timer with the screen
      off. Completion fires, possibly late, and nothing crashes.
- [ ] Pause → background → return. Still paused, same remaining time, and nothing fires at the
      original end time.
- [ ] ±adjust during a running timer. The ongoing notification and Live Activity show the new end
      time, and the alert fires at the new time.
- [ ] Running timer → edit parameters → force-stop → relaunch. Still running with the correct
      remaining time, and the edits kept. The Home banner keeps its progress bar throughout.

## 2. Notifications

- [ ] **[A]** Running timer: ongoing notification shows tea name, "Steep 3", a countdown that
      counts down, and a draining progress bar. Expanded view matches. No next steep while brewing.
- [ ] **[A]** Pause: the text reads "Steep 3 paused", the header shows a static m:ss, and nothing
      ticks. Resume restarts it.
- [ ] Completion alert from a gongfu method shows "<tea> is ready!" and "Steep 3 done, next 30s"
      with sound. A western method shows "Steep 1 done". On [iOS] it also appears while the app is in the foreground.
- [ ] **[A]** Force-stop mid-steep and let the backup alarm post the alert. Same text.
- [ ] Leave the completion alert in the shade, then Next, Finish or Discard from the review. The
      alert is withdrawn each time.
- [ ] **[A]** Completion alert survives the ongoing notification being cleared when the foreground
      service stops.
- [ ] Settings → turn off completion notifications. No completion alert; the timer still completes
      in-app.
- [ ] Settings → turn off session reminders. Completion alerts still fire.
- [ ] Leave a finished steep unreviewed. The reminder "How was your <tea>?" arrives after the
      configured delay.
- [ ] **[A]** With the device in Doze (`adb shell dumpsys deviceidle force-idle`), the reminder
      arrives late but arrives. Known and accepted: inexact by design.
- [ ] Rate or discard the steep before the reminder is due. The reminder never arrives.
- [ ] Start a new steep while an old session's reminder is pending. Resolving the old session does
      not cancel the new one's alerts.
- [ ] Discard from Steep Complete. The reminder is withdrawn.
- [ ] Delete a running session from History's card menu. Its timer notification and reminder go.
- [ ] Brew three steeps, leave the third unreviewed, then Finish from Steep Complete. The reminder
      is withdrawn.

## 3. Tap routing

- [ ] Tap the ongoing notification (app backgrounded) → Timer for that session. **[A]**
- [ ] Tap the completion alert (app backgrounded) → Steep Complete for that session.
- [ ] Tap the completion alert after the app was force-stopped (cold start) → Steep Complete.
- [ ] Tap the reminder → Steep Complete for the session it names.
- [ ] Tap the completion alert for steep 2 or later → Steep Complete for that steep, with the
      earlier ones under Previous Steeps.
- [ ] Tap a notification, then rotate or switch dark mode. The route is not replayed a second
      time.
- [ ] **[A]** Steep 2 running with a reminder for steep 1 still posted → tap each. The ongoing
      notification opens the Timer, the reminder the review; neither opens the other's screen.
- [ ] Tap a reminder for a session already finished or discarded. The app opens on Home, not a
      review or timer for it.

## 4. Live Activity [iOS]

- [ ] Start a timer. Live Activity appears on the lock screen and Dynamic Island with tea name,
      "Steep 3", and a countdown; paused, it reads "Steep 3 paused".
- [ ] Tap the Live Activity on the lock screen, and the expanded Dynamic Island → the Timer for that
      session.
- [ ] Countdown stops at 0:00 and never counts up past it.
- [ ] Pause → the activity shows paused with static time. Resume → it counts down again.
- [ ] ±adjust → the activity's end time moves. Cancelling and rescheduling the alarm does not
      freeze it into "Paused".
- [ ] Complete, discard, or tap the completion alert → the activity ends.
- [ ] Force-quit with an activity showing → relaunch. Stale activities are cleaned up.
- [ ] Live Activities disabled in Settings → the timer still works and nothing errors.
- [ ] On an iOS 18 device, start a timer. The Live Activity appears (the widget extension's
      deployment target matches the app's).

## 5. Permissions

- [ ] **[A]** Fresh install on API 33+: the notification prompt appears. Deny → timers still run;
      no crash; the in-app timer still completes.
- [ ] **[iOS]** Fresh install: the notification prompt appears. Deny → same as above.
- [ ] Grant later from system settings → the next timer posts notifications without a restart.
- [ ] **[A]** On API 34+, the foreground service starts without a `specialUse` crash.

## 6. Files and backup

- [ ] Export → system save dialog → file written under a name with today's date and time; a
      "Backup exported" snackbar shows. Reopen it from the Files app; size is non-zero. Export
      again in the same visit: the second file gets a later name.
- [ ] Export → cancel the save dialog **[A]** or close the share sheet **[iOS]**. No snackbar,
      no error, the Export button is usable again, and "Last backup" doesn't change.
- [ ] Fresh install: Settings shows "Last backup: never" in red with "Back up now". Export →
      "Last backup: today" in grey, the button reads "Export", and it survives an app restart.
- [ ] **[iOS]** Export → share sheet stays open with no "Backup exported" until a destination
      (Save to Files, AirDrop) completes.
- [ ] **[A]** Export → rotate or switch dark mode while the save dialog is open → save. The file
      is written in full, the dialog opens only once, and the snackbar shows.
- [ ] Export from [A], import on [iOS], and the reverse. All teas, sessions, vessels, methods,
      and photos arrive (F4 bug: DEFLATE vs STORE fails today).
- [ ] Upgrade from `v0.1.1` (schema 6). Before installing over it, seed a single-steep session, a
      gongfu session with a deleted middle steep, a parent edited after steeps were added, an
      unrated session, a session left mid-steep and a saved method used across several steeps.
      After: the app opens, the mid-steep session is still in progress, and History, Tea Detail,
      Collection and the method agree on every count and rating.
- [ ] Upgrade from `v0.1.2` (schema 7). Before installing over it, delete a tea that has sessions.
      After: it is back in Collection, faded, last and marked Finished, and its sessions in History
      show its name.
- [ ] Upgrade from `v0.4.0` (schema 8). Before installing over it, save a method, brew a gongfu
      session from it and leave another mid-steep. After: the app opens, the mid-steep session is
      still in progress, the method plans a single steep, Brew Again on the gongfu session replays
      every steep, and the tag sheet lists the 23 tags.
- [ ] Upgrade from `v0.5.0` (schema 11). Before installing over it, finish a gongfu session with
      a review note and a note on steep 2. After: History's card and Session Detail show the review
      note as the session's, steep 1 has no note, steep 2 keeps its own, and the review note edits
      from Edit Session.
- [ ] Import a backup exported by `v0.5.0`. Steep 1's notes arrive as the sessions' notes.
- [ ] Import a backup exported by `v0.4.0`. It imports with no plans, rinses or tags.
- [ ] Import a backup exported by `v0.1.1`. It imports, and tea and method stats match the
      sessions that arrived.
- [ ] Import picker → cancel. Nothing changes, no error shown.
- [ ] Import a non-backup file. A readable error; nothing changes.
- [ ] Open the import picker → rotate or switch dark mode while it's open → pick a file. The
      picker opens only once, and the preview dialog shows.
- [ ] Photo picker on Log Tea and Edit Session: pick, cancel, and pick a very large photo.
- [ ] Take a portrait photo with the camera app, then add it to a tea, a session and a vessel. It
      shows upright everywhere, and again after export → import on the other platform.
- [ ] Settings → Data shows "Photos" with a size. Add a photo, reopen Settings: it grows. Import a
      backup with photos: it grows without reopening.
- [ ] Add a HEIC **[iOS]** or PNG photo and a 48 MP+ photo. Each saves; the stored file is a JPEG no
      longer than 2048 px on its long edge.
- [ ] **[A]** Auto Backup: `adb shell bmgr backupnow dev.jketterer.leaflog` with photos present.
      It succeeds or fails cleanly under the 25 MB quota.

## 7. Recreation and display

Recreate with a dark-mode switch and with a font-scale change (Settings → Display), not rotation:
the Activity is portrait-locked.

- [ ] Running timer → recreate. The timer keeps running, with no reset and no second alert.
- [ ] Quick Timer running → recreate. Same as above.
- [ ] Edit Tea, Edit Vessel, Edit Session, and Log Tea half-filled → recreate. Typed values
      survive and nothing re-seeds from the database.
- [ ] Max font scale: Timer ring, Quick Timer duration sheet, and Quick Brew cards with long tea
      names stay readable, with nothing clipped.
- [ ] Settings → Theme: Light and Dark each hold against the opposite system setting, System
      follows it, and the status and navigation bar icons stay legible on each. The choice
      survives a restart with no frame drawn in the other theme.
- [ ] Cold start with the system in dark mode: the splash is dark, not white.
- [ ] Settings → Hemisphere on a fresh install with the device region set to Australia (both
      platforms): South is selected. Switching the region to the US moves it to North until a
      chip is tapped, after which the choice holds.
- [ ] A recap paged past its cover → recreate, and separately open a tea from it and come back:
      the same page shows. At max font scale every page scrolls rather than clips, and the close
      button and page dots clear the status and navigation bars.
- [ ] **[iOS]** Theme set opposite the system: the backup file pickers and share sheet follow it.
- [ ] **[A]** Large screen or tablet on Android 16 (portrait lock ignored): no screen is unusable
      in landscape.

## 8. Navigation

- [ ] **[iOS]** Back-swipe on a screen pushed inside a tab returns to the previous screen, not out
      of the tab.
- [ ] **[A]** System back from a tab's root returns Home, then exits.
- [ ] Edit Tea, Edit Vessel, Edit Tea Type and Edit Session, each with a change made: system back
      (Android) or the edge swipe (iOS) asks before discarding, as the toolbar arrow does. With no
      change it leaves at once, with the predictive-back peek. The question survives rotation.

## 9. Quick Timer

- [ ] Start → background → complete. Exactly one ready alert; its tap opens the Quick Timer, done,
      offering "Just a timer".
- [ ] **[A]** Ongoing notification appears, counts down, opens the Quick Timer on tap, and clears on
      completion, Poured early or stop.
- [ ] **[iOS]** The Live Activity appears and its tap opens the Quick Timer.
- [ ] Back, by the arrow, system back or the edge swipe, while running or paused asks before
      stopping. Stopping withdraws its notifications.
- [ ] "Just a timer" returns Home with nothing recorded.
- [ ] Three steeps by "Next", then "Log it" with a tea → the review of steep 3, with steeps 1 and 2
      at their real times, unrated. The session starts when steep 1 did.
- [ ] "What are you brewing?" while running → the session's Timer, still counting, its ongoing
      notification and ready alert now naming the tea. Quick-add from the picker does the same.
- [ ] Two steeps by "Next" → let the process die → reopen → attach. Both steeps are logged.
