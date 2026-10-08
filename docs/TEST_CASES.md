# Leaf Log — Device Checklist (Track B)

> **Last trimmed:** 2026-10-08 · See ROADMAP.md § 3.

The manual pass for what host tests and screenshots can't reach: wall-clock timing, process death,
notifications, Live Activity, permissions, pickers, and Activity recreation. Logic, rules, and
layout belong in Track A and Track S, not here.

**When to run:** before tagging a release, and after any change to `TimerService`, notifications,
the Live Activity, deep links, `FileOperations`, or `MainActivity`. Run the affected section; run
everything before a release.

**Rule:** platform integration ships with a line here. A line that can be tested on the host moves
to Track A and is deleted from this file.

Tags: **[A]** Android only · **[iOS]** iOS only · untagged = both.

---

## 1. Timer lifetime

- [ ] Running timer → background 30s → return. Remaining time is correct; if it elapsed while
      backgrounded, the timer shows complete.
- [ ] Running timer → lock the device past the end time. Completion alert fires on time.
- [ ] Running timer → force-stop from Recents → relaunch. The session is in progress and the
      remaining time is correct.
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
- [ ] Pause → Reset → leave the timer and come back, then force-stop and relaunch. Both times the
      timer is at full duration and not started.

## 2. Notifications

- [ ] **[A]** Running timer: ongoing notification shows tea name, "Steep N", a countdown that counts
      down, and a draining progress bar. Expanded view matches.
- [ ] **[A]** Pause: the notification switches to "Paused · m:ss" and stops ticking. Resume restarts
      it.
- [ ] Completion alert shows "<tea> is ready!" with sound. On [iOS] it also appears while the app
      is in the foreground.
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

## 3. Tap routing

- [ ] Tap the ongoing notification (app backgrounded) → Timer for that session. **[A]**
- [ ] Tap the completion alert (app backgrounded) → Steep Complete for that session.
- [ ] Tap the completion alert after the app was force-stopped (cold start) → Steep Complete.
- [ ] Tap the reminder → Steep Complete for the session it names.
- [ ] Tap a notification, then rotate or switch dark mode. The route is not replayed a second
      time.
- [ ] Two sessions with posted notifications → tap the older one. It opens the older session,
      not the newer (requestCode-0 routing suspected broken on [A]; see audit §7).

## 4. Live Activity [iOS]

- [ ] Start a timer. Live Activity appears on the lock screen and Dynamic Island with tea name,
      steep, and a countdown.
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

- [ ] Export → system save dialog → file written. Reopen it from the Files app; size is non-zero.
- [ ] Export from [A], import on [iOS], and the reverse. All teas, sessions, vessels, methods,
      and photos arrive (F4 bug: DEFLATE vs STORE fails today).
- [ ] Import picker → cancel. Nothing changes, no error shown.
- [ ] Import a non-backup file. A readable error; nothing changes.
- [ ] Open the import picker → rotate or switch dark mode while it's open → pick a file. The
      import still runs (`pendingFilePath` null after recreation suspected; see audit §7).
- [ ] Photo picker on Log Tea and Edit Session: pick, cancel, and pick a very large photo.
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
- [ ] **[A]** Large screen or tablet on Android 16 (portrait lock ignored): no screen is unusable
      in landscape.

## 8. Navigation

- [ ] **[iOS]** Back-swipe on a screen pushed inside a tab returns to the previous screen, not out
      of the tab.
- [ ] **[A]** System back from a tab's root returns Home, then exits.

## 9. Quick Timer

Until Bet 2 merges it into the Timer engine.

- [ ] Start → background → complete. Exactly one completion alert, not two.
- [ ] **[A]** Ongoing notification appears, counts down, and clears on completion or stop.
- [ ] Complete with details → the session is saved; tap routing lands on the right screen.
