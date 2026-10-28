# Leaf Log — Device Checklist (Track B)

> **Last trimmed:** 2026-10-28 · See ROADMAP.md § 3.

The manual pass for what host tests and screenshots can't reach: wall-clock timing, process death,
notifications, Live Activity, permissions, pickers, and Activity recreation. Logic, rules, and
layout belong in Track A and Track S, not here.

**When to run:** before tagging a release, and after any change to `TimerService`, notifications,
the Live Activity, deep links, `FileOperations`, or `MainActivity`. Run the affected run; run
everything before a release.

**Rule:** platform integration ships with a line here. A line that can be tested on the host moves
to Track A, or Track F if it is a flow through the app, and is deleted from this file.

Each platform is its own pass on its own device. Shared behavior is verified once on the host, so a
line appears under both platforms only when each platform does it differently. Across platforms
needs one device of each.

A pass is a few runs done in order, each picking up where the last left off: tick lines as you go
rather than starting over for each one. Brew at 70 °C or cooler, so a reminder comes 5 minutes after
its alert, and keep the steep times short. While waiting on one line, go on to the next.

**Reference backup:** a backup with a few months of sessions and a tea with a long name. The last
pass's export (Photos and backup) serves; import it after the fresh-install run.

---

## Android

### 1. Fresh install

Uninstall first; system in dark mode; an API 34+ device.

- [ ] Cold start: the splash is dark, not white.
- [ ] The notification prompt appears, and denying it crashes nothing.

Allow notifications in system settings, then import the reference backup.

### 2. One steep, watched

- [ ] Start a steep. The foreground service starts without a `specialUse` crash.
- [ ] The ongoing notification's countdown visibly ticks down and its progress bar drains,
      collapsed and expanded.
- [ ] Lock the device past the end time. The completion alert fires on time and sounds.
- [ ] The completion alert survives the ongoing notification being cleared when the foreground
      service stops.
- [ ] Leave it unreviewed and force Doze (`adb shell dumpsys deviceidle force-idle`). The reminder
      arrives late but arrives. Known and accepted: inexact by design.

Then `adb shell dumpsys deviceidle unforce`.

### 3. Alarms with the app gone

- [ ] Next, start the steep → swipe the app from Recents → wait past the end time without opening
      it. The backup alarm's completion alert still fires. Not Settings → Force stop: that cancels
      the app's alarms by design.
- [ ] Revoke "Alarms & reminders" in system settings → run a 1-minute timer with the screen off.
      Completion fires, possibly late, and nothing crashes. Grant it back after.

### 4. Photos and backup

- [ ] Photo picker on Log Tea: cancel, then pick a 48 MP+ photo. On Edit Session: cancel, then
      pick a PNG. Each saves; the stored file is a JPEG no longer than 2048 px on its long edge.
- [ ] Take a portrait photo with the camera app, then add it to a tea, a session and a vessel. It
      shows upright everywhere.
- [ ] Auto Backup: `adb shell bmgr backupnow dev.jketterer.leaflog` with these photos present. It
      succeeds or fails cleanly under the 25 MB quota.

Export a backup and keep it: Across platforms imports it on iOS, and the next pass uses it as the
reference backup.

### 5. Display and navigation

Font scale to max (Settings → Display). It and dark mode recreate the Activity, which is
portrait-locked, so rotation doesn't.

- [ ] Timer ring, Quick Timer duration sheet, and Quick Brew cards with long tea names stay
      readable, with nothing clipped.
- [ ] A recap: every page scrolls rather than clips, and the close button and page dots clear the
      status and navigation bars.
- [ ] Edit Tea, Edit Vessel, Edit Tea Type and Edit Session: with no change, back leaves with the
      predictive-back peek.

Font scale back to default.

- [ ] Settings → Theme: Light and Dark each hold against the opposite system setting, System
      follows it, and the status and navigation bar icons stay legible on each. The choice
      survives a restart with no frame drawn in the other theme.

### Other devices

- [ ] Large screen or tablet on Android 16 (portrait lock ignored): no screen is unusable in
      landscape.

## iOS

### 1. Fresh install

Uninstall first; system in dark mode; region set to Australia.

- [ ] Cold start: the splash is dark, not white.
- [ ] Settings → Hemisphere: South is selected. Switching the region to the US moves it to North
      until a chip is tapped, after which the choice holds.
- [ ] The notification prompt appears. Deny → start a timer: it runs, nothing crashes, and the
      in-app timer completes.
- [ ] Grant from system settings → the next timer posts notifications without a restart.

Import the reference backup.

### 2. A gongfu session, steep by steep

Steep 1, watched:

- [ ] Start it. The Live Activity appears on the lock screen and Dynamic Island with the tea name,
      "Steep 1", and a countdown.
- [ ] Tap the Live Activity on the lock screen, and the expanded Dynamic Island. Each opens the
      Timer for this session.
- [ ] Background 30s → return. Remaining time is correct.
- [ ] Pause. The activity reads "Steep 1 paused" with static time. Background past the original end
      → return: still paused, same remaining time, and nothing fired.
- [ ] Resume: the activity counts down again. ±adjust: its end time moves, and cancelling and
      rescheduling the alarm doesn't freeze it into "Paused".
- [ ] Lock the device past the end time. The alert fires on time at the adjusted end, sounds, and
      reads "<tea> is ready!" and "Steep 1 done, next <time>".
- [ ] The activity's countdown stops at 0:00 and never counts up past it.
- [ ] Tap the alert. Steep Complete opens for this session and the activity ends.

Steep 2, the reminder:

- [ ] Next, start it, and stay in the app past the end. The alert also appears in the foreground.
- [ ] Leave it unreviewed in the background. "How was your <tea>?" arrives 5 minutes later; tap
      it: Steep Complete for this session.

Steep 3, force-quit:

- [ ] Next, start it, force-quit from the app switcher, wait past the end without opening it. The
      scheduled alert still fires.
- [ ] Tap it. The app opens Steep Complete for this session, and the stale Live Activity is
      cleaned up.
- [ ] Force-quit again. The reminder arrives; tap it: Steep Complete for this session.

Steeps 4 and 5, withdrawn:

- [ ] Next, start steep 4, and let it end in the background. Open the app from its icon and tap
      Next: the alert leaves the shade.
- [ ] Let steep 5 end the same way, then Finish: its alert leaves the shade and the activity ends.
      Neither steep's reminder ever arrives.
- [ ] A reminder for a session since finished opens Home.

Then:

- [ ] A western session: the alert reads "Steep 1 done". Leave it in the shade and Discard: the
      alert leaves, the activity ends, and the reminder never arrives.
- [ ] Live Activities disabled in Settings → the timer still works and nothing errors. Enable them
      again.

### 3. Quick Timer

- [ ] The Live Activity appears and its tap opens the Quick Timer.
- [ ] The edge swipe, while running or paused, asks before stopping.

### 4. Photos and backup

- [ ] Settings → Data shows "Photos" with a size. Note it.
- [ ] Photo picker on Log Tea: cancel, then pick a 48 MP+ photo. On Edit Session: cancel, then
      pick a HEIC. Each saves; the stored file is a JPEG no longer than 2048 px on its long edge.
- [ ] Take a portrait photo with the camera app, then add it to a tea, a session and a vessel. It
      shows upright everywhere.
- [ ] Reopen Settings: Photos has grown.
- [ ] Export → close the share sheet. No snackbar, no error, the Export button is usable again, and
      "Last backup" doesn't change.
- [ ] Export again → the share sheet stays open with no "Backup exported" until Save to Files
      completes. Keep the file for Across platforms.

### 5. Display and navigation

Text size to max (Settings → Accessibility → Display & Text Size → Larger Text).

- [ ] Timer ring, Quick Timer duration sheet, and Quick Brew cards with long tea names stay
      readable, with nothing clipped.
- [ ] A recap: every page scrolls rather than clips, and the close button and page dots clear the
      status bar and home indicator.

Text size back to default.

- [ ] Back-swipe on a screen pushed inside a tab returns to the previous screen, not out of the
      tab.
- [ ] Edit Tea, Edit Vessel, Edit Tea Type and Edit Session: with no change, the edge swipe leaves
      at once. With a change made, it asks before discarding, and the question survives rotation.
- [ ] Settings → Theme: Light and Dark each hold against the opposite system setting, System
      follows it, and the status bar stays legible on each. The choice survives a restart with no
      frame drawn in the other theme.
- [ ] Theme set opposite the system: the backup file pickers and share sheet follow it.

### Other devices

- [ ] On an iOS 18 device, start a timer. The Live Activity appears (the widget extension's
      deployment target matches the app's).

## Across platforms

- [ ] Import Android's export on iOS, and iOS's on Android. All teas, sessions, vessels, methods,
      and photos arrive, portrait photos still upright (F4 bug: DEFLATE vs STORE fails today).
