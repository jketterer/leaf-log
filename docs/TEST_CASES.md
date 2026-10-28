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
- [ ] Running timer → edit parameters → force-stop → relaunch. The Home banner keeps its progress
      bar throughout.

## 2. Notifications

- [ ] **[A]** Running timer: the ongoing notification's countdown visibly ticks down and its
      progress bar drains, collapsed and expanded.
- [ ] The completion alert sounds. On **[iOS]** it reads "<tea> is ready!" and "Steep 3 done, next
      30s" (a western method: "Steep 1 done"), and also appears while the app is in the foreground.
- [ ] **[A]** Completion alert survives the ongoing notification being cleared when the foreground
      service stops.
- [ ] Leave a finished steep unreviewed. The reminder "How was your <tea>?" arrives after the
      configured delay.
- [ ] **[A]** With the device in Doze (`adb shell dumpsys deviceidle force-idle`), the reminder
      arrives late but arrives. Known and accepted: inexact by design.
- [ ] **[iOS]** Next, Finish or Discard from the review withdraws a completion alert or reminder
      left in the shade, and a reminder not yet due.
- [ ] **[A]** Delete a running session from History's card menu. Its ongoing notification goes.

## 3. Tap routing

- [ ] **[iOS]** Tap the completion alert and the reminder, backgrounded and after a force-quit. Each
      opens Steep Complete for its session; a reminder for a session since finished opens Home.

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

- [ ] **[iOS]** Export → close the share sheet. No snackbar, no error, the Export button is usable
      again, and "Last backup" doesn't change.
- [ ] **[iOS]** Export → share sheet stays open with no "Backup exported" until a destination
      (Save to Files, AirDrop) completes.
- [ ] Export from [A], import on [iOS], and the reverse. All teas, sessions, vessels, methods,
      and photos arrive (F4 bug: DEFLATE vs STORE fails today).
- [ ] Photo picker on Log Tea and Edit Session: pick, cancel, and pick a very large photo.
- [ ] Take a portrait photo with the camera app, then add it to a tea, a session and a vessel. It
      shows upright everywhere, and again after export → import on the other platform.
- [ ] Settings → Data shows "Photos" with a size. Add a photo, reopen Settings: it grows.
- [ ] Add a HEIC **[iOS]** or PNG photo and a 48 MP+ photo. Each saves; the stored file is a JPEG no
      longer than 2048 px on its long edge.
- [ ] **[A]** Auto Backup: `adb shell bmgr backupnow dev.jketterer.leaflog` with photos present.
      It succeeds or fails cleanly under the 25 MB quota.

## 7. Recreation and display

Recreate with a dark-mode switch and with a font-scale change (Settings → Display), not rotation:
the Activity is portrait-locked.

- [ ] Max font scale: Timer ring, Quick Timer duration sheet, and Quick Brew cards with long tea
      names stay readable, with nothing clipped.
- [ ] Settings → Theme: Light and Dark each hold against the opposite system setting, System
      follows it, and the status and navigation bar icons stay legible on each. The choice
      survives a restart with no frame drawn in the other theme.
- [ ] Cold start with the system in dark mode: the splash is dark, not white.
- [ ] Settings → Hemisphere on a fresh install with the device region set to Australia (both
      platforms): South is selected. Switching the region to the US moves it to North until a
      chip is tapped, after which the choice holds.
- [ ] A recap at max font scale: every page scrolls rather than clips, and the close button and
      page dots clear the status and navigation bars.
- [ ] **[iOS]** Theme set opposite the system: the backup file pickers and share sheet follow it.
- [ ] **[A]** Large screen or tablet on Android 16 (portrait lock ignored): no screen is unusable
      in landscape.

## 8. Navigation

- [ ] **[iOS]** Back-swipe on a screen pushed inside a tab returns to the previous screen, not out
      of the tab.
- [ ] Edit Tea, Edit Vessel, Edit Tea Type and Edit Session: with no change, back leaves with the
      predictive-back peek. On **[iOS]**, with a change made, the edge swipe asks before
      discarding, and the question survives rotation.

## 9. Quick Timer

- [ ] **[iOS]** The Live Activity appears and its tap opens the Quick Timer.
- [ ] **[iOS]** The edge swipe, while running or paused, asks before stopping.
