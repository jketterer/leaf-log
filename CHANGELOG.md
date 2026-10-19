# Changelog

All notable changes to Leaf Log are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

<!--
Releasing a version:
1. Set VERSION_NAME in version.properties.
2. Rename "Unreleased" below to "[x.y.z] - YYYY-MM-DD", fill it from `git log vPREV..HEAD --oneline`
   in user-facing language, and open a new empty "Unreleased".
3. Commit as `chore: release x.y.z`.
4. `git tag -a vx.y.z -m "x.y.z"`, then `git push --follow-tags`.
Build numbers are automatic: Xcode Cloud on iOS, a UTC timestamp on Android.
-->

## [Unreleased]

## [0.4.0] - 2026-10-19

One look across the app, one way of telling you something went wrong, and units that stay the way
you set them.

### Added

- Choose a Light, Dark or System theme in Settings → Appearance. Backups carry the choice.
- A weight unit in Settings, honoured everywhere a leaf weight is shown.
- Tap a unit in a form to switch that field for as long as the form is open. Settings doesn't
  change. Home's water tile and the Analytics totals switch the same way, only while you're there.
- Each tab keeps its own screens. Switching tabs and back returns you to where you were, with
  search, filters and scroll position intact. Tapping the current tab returns to its top.
- A screen opened inside a tab, such as History for one tea, has an Up arrow.

### Changed

- New headings typeface, consistent corners, and a full colour scheme in light and dark.
- Tea-type colours are toned for light and dark, so every type reads on both. The colour picker
  offers twelve tea colours.
- One icon set throughout.
- Ratings show to the nearest half star with one decimal ("4.3"). An unrated session says
  "Unrated" instead of showing empty stars, and a screen reader can read and set a rating.
- Dates follow your device's language and region.
- Counts read correctly: "1 brew", "used 1 time".
- Problems appear as a short message at the bottom of the screen, never as technical error text.
  A message cut short by leaving the screen shows again when you come back.
- Delete and discard confirmations look and read the same everywhere.
- Tab switches fade; screens opened inside a tab slide.

### Fixed

- In dark mode, the app no longer flashes white at launch.
- Back from Collection, History or Analytics returns Home instead of leaving the app.
- Switching a unit back and forth no longer changes the saved value, and saving a form without
  touching a field leaves it exactly as it was (no more 150 ml becoming 147.9 ml).
- Temperatures and volumes accept decimals, such as 92.5 °C or 6.5 fl oz.
- Edit Tea Type checks the temperature in the unit shown, and switching the unit no longer clears
  it.
- The last tea type can no longer be deleted.
- System back or swipe on Edit Tea Type and Edit Session asks before discarding your changes.
- Forms no longer reload over what you typed after a dark-mode or font-size change, and Quick Timer
  no longer restarts.
- History no longer re-applies a filter you cleared when you return to it, and Session Detail no
  longer slows down each time you come back to it.
- History's search bar stays open when you come back to it, so a search can't keep filtering the
  list out of sight.
- Failures that were never shown now are: on the Timer, Steep Complete, Edit Session, the brewing
  method form, Collection, History and Vessels.
- Steep Complete no longer hides its form behind an error, and the next-steep dialog no longer
  accepts water above 100 °C.

## [0.3.0] - 2026-10-15

Backups restore on either platform, onto a fresh device, without losing or reverting anything.

### Added

- Importing a backup shows what it will do first: when the backup was made, and how many records
  are new, updated or removed. Import is turned off when the device already has everything in it.
- Backups carry your settings. They're restored only onto a device with no teas yet, so a device
  you already use keeps its own.
- Settings shows how long ago you last backed up. After 14 days, or if you never have, it turns red
  and the Export button reads "Back up now".
- Settings shows how much space your photos take on this device.
- "Backup exported" confirms an export once it has been saved.

### Changed

- Import keeps whichever copy of each record changed last. An older backup no longer reverts what
  you've changed since, a delete made after the backup stays deleted, and importing the same backup
  twice changes nothing.
- Backups include deleted teas, vessels, tea types and brewing methods with their photos, so the
  sessions of a deleted tea restore with their tea.
- An import happens all at once: if it fails, nothing changes and no photos are left behind.
- A session that was in progress when the backup was made arrives finished. A session in progress
  on the device you import into is never touched.
- An older backup can hold sessions of a tea or vessel deleted before it was made. Those are left
  out, and the import says how many; a fresh export from this version brings everything.
- Edits to the built-in tea types and vessels survive a restore onto a fresh install.
- Export files are named with the date and time they were made.
- Photos added from now on are saved as JPEGs no longer than 2048 pixels on their longest side, so
  they take less space and backups are smaller. Photos already in the app are left as they are.

### Fixed

- A backup made on Android now imports on iOS.
- A photo that can't be saved shows an error. On iOS, a photo that failed to save is no longer kept
  as a missing image.
- Exporting on Android no longer freezes the screen while the file is copied.
- Rotating or switching dark mode while the export or import picker is open no longer opens it a
  second time.
- On iOS, an export counts as done only once the share sheet saves or sends it, and a failed copy
  is reported.

## [0.2.0] - 2026-10-14

Teas are marked finished instead of deleted, so the journal keeps the name of every tea it mentions.

### Added

- Mark a tea finished from the menu on Tea Detail, and bring it back with "Back in Stock". Tea Detail
  shows when it was finished, and brewing a finished tea puts it back in stock.
- Finished teas are faded and listed last in Collection, carry a "Finished" chip in Collection and
  the Log Tea picker, and their Quick Brew cards are faded.
- A tea type that teas still use can be merged into another type: its teas move to the type you
  choose and keep their own temperatures.

### Changed

- A tea with sessions can no longer be deleted. Tea Detail offers Delete only for a tea that was
  never brewed.
- Deleting a tea or a vessel also deletes its saved brewing methods.

### Fixed

- Teas deleted in earlier versions that still have sessions come back as finished, so those sessions
  show their tea again. Vessels deleted while sessions used them come back archived, and tea types
  still used by a tea come back.
- A deleted tea, vessel, tea type or brewing method no longer opens from a screen that still pointed
  at it. Tea Detail closes when its tea is deleted.

## [0.1.2] - 2026-10-13

Sessions and their steeps now follow one set of rules everywhere in the app.

### Added

- Finishing a session from Steep Complete offers Undo on Home, which reopens the session and returns
  to Steep Complete.
- The timer records how long each steep actually brewed, counting adjustments and Complete Now.
  Brew Again and the Log Tea prefill start from that time instead of the planned one.

### Changed

- A session stays in progress until you finish it. Starting a second steep no longer marks it
  complete, and tea stats count finished sessions only.
- A session's rating is the average of its rated steeps, and a tea's rating is the average of its
  sessions' ratings, each session counting once. Unrated sessions are left out.
- Tea and brewing method stats are recalculated from sessions whenever one changes, so editing,
  reopening or deleting a session keeps them right. A method's "times used" no longer goes up once
  per steep.
- History, Tea Detail and Home show one card per session, and Vessel Detail counts sessions rather
  than steeps.
- Add Steep in Session Detail records a forgotten steep on a finished session, and only shows there.

### Fixed

- Log Session's rating starts unrated instead of filled in.
- A session in progress on its second or later steep is now recognised as in progress.
- A failed save no longer leaves a session half-updated.

## [0.1.1] - 2026-10-08

Bug fixes from the first full audit of the app.

### Changed

- History's "This Week" filter is now called "Last 7 Days", which is what it always showed.
- Weeks start on Monday everywhere, including the Analytics heatmap.
- Quick Brew now counts toward a method's "times used" and "last used".

### Removed

- The "longest brewing streak" insight in Analytics.

### Fixed

- The app no longer saves ratings you didn't give. Opening Steep Complete stored a guessed rating,
  Quick Brew copied the method's rating onto the new session, and saving a method from a session
  without a rating filled in 5 stars.
- On iOS, a session from the same day of an earlier month no longer shows as "Today".
- Analytics' "This Week" now includes today when today is Sunday.
- Tapping a segment of the tea type chart opens the right tea type in History, and tapping the trend
  chart selects the nearest point instead of the one to its left. Both charts now describe
  themselves to screen readers.
- Editing a brew from the timer, or resetting a paused timer, no longer loses the timer's saved
  state.
- Discarding a session from Steep Complete cancels its reminder, so the reminder no longer reopens a
  deleted session.
- The Live Activity works on iOS 18. It previously required iOS 26.2.
- Errors now show on the Collection, History, Timer, Brewing Method, Edit Session and Vessels
  screens, where they were set but never displayed. A failed export or import in Settings now says
  so, rather than reporting success or cancellation.
- Going back no longer risks a crash on Android 14 and earlier.

## [0.1.0] - 2026-10-08

The first versioned build of the Kotlin Multiplatform rewrite, for Android and iOS.

Leaf Log keeps a collection of teas, vessels and tea types, and saves brewing methods per tea.
A brewing timer runs multi-steep sessions with a rating for each steep, survives the app being
backgrounded, and shows as a notification on Android and a Live Activity on iOS. A quick timer is
there for brewing without logging. History and per-tea detail show past sessions, Analytics
summarises them by period, and export/import provides backup.

### Before 0.1.0

The app was first written in Flutter and published on Google Play as `com.jketterer.leaf_log`.
That code is tagged `flutter-1.1.1`. An abandoned 2022 Flutter rewrite is tagged `flutter-2.0-wip`.

[Unreleased]: https://github.com/jketterer/leaf-log/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/jketterer/leaf-log/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/jketterer/leaf-log/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/jketterer/leaf-log/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/jketterer/leaf-log/releases/tag/v0.1.0
