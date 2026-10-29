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

## [0.8.0] - 2026-10-29

Brew again: repeat a past brew by picking it from your history and seeing exactly what it was
before the timer starts. Saved methods are gone.

### Added

- A Brew again row on Home: one card for each tea and vessel you've brewed together, most recent
  first, showing the brew it repeats. It replays your last brew, or your best when the last was
  rated under 3★.
- A Brew again sheet, from Home, History, Tea Detail's best brew and Session Detail. It shows the
  brew's leaf, water, temperature, rinse, each steep, tags and note, with Last time and Best brew to
  switch between when they differ. Start Brewing begins the session; Adjust opens it in Log Tea
  first.
- Log Tea fills from the brew Brew again would repeat for the tea and vessel you pick. Start from
  switches it to your last time, your best brew, or Scratch.
- A Flavors tab in Collection: the tags you've tasted, most used first, and the ones you haven't
  yet. A flavor opens to how often you've tasted it and the teas you taste it in most, from there
  or from Tea Detail's top flavors. Your own tags can be renamed, merged into another or deleted.

### Changed

- Nothing starts a session on one tap: Brew again always opens the sheet first.
- A repeated brew's later steeps follow the brew it repeats, moved by however much you changed
  steep 1.
- Steep Complete leads with Finish when the brew you repeated was a single steep.
- Home's recent sessions no longer have a Brew again button; the row replaces it.
- Session complete is redesigned, and the tea and vessel sit beside a leaf in the tea's color on
  the brew screens.
- Log Tea picks the tea and vessel together at the top, the vessel from a row of chips, and only
  shows the brew parameters once both are chosen.
- New vessel icons.
- Backups carry tag renames, merges and deletes, and importing an older backup doesn't undo them.

### Removed

- Saved methods, Quick Brew and the offers to save or update a method. A session started from a
  method keeps replaying the session the method was saved from, and older backups import the same
  way. Method names are not kept.

### Fixed

- Home's stats for today update when you come back to the app.
- The Timer no longer opens over Steep Complete once a steep has finished.
- The Timer's ring resizes smoothly when its buttons change instead of jumping.
- Bottom sheets open fully instead of stopping halfway, where the keyboard could hide their
  bottom.

## [0.7.0] - 2026-10-26

Recaps: once a season ends, "Your autumn in tea" pages through what you brewed, and from January,
"Your year in tea". Analytics follows the calendar and links into your journal.

### Added

- Season recaps, once a season with 10 or more sessions has ended, and a year recap from January.
  Each is a set of pages: the season at a glance, its rhythm, your most brewed teas, the teas you
  discovered, your flavors, how you brewed, your best session and your shelf. Tap a tea, session or
  day to open it. A new recap leads Analytics for two weeks; the book icon opens every recap.
- A Hemisphere setting, so seasons match where you live. It starts from your region.
- "Not brewed lately" in the collection: unfinished teas last brewed 60 or more days ago, oldest
  first. It works with the type filter and search.
- Analytics links into your journal: a heatmap cell opens History for that day, week or month, a
  tea type opens History by that type over the period, a vessel opens its page, and a top tea opens
  History by that tea.

### Changed

- Analytics periods follow the calendar: This week, This month, This season, This year and All
  time, each compared with the same part of the period before. A saved period that no longer exists
  reads as This week.
- Analytics stays up to date as you brew and edit, and keeps the screen up while a period loads.
  Each card hides when it has nothing to show, rather than the whole screen waiting for 10 sessions.
- One activity heatmap replaces the trend chart: by day for a week, month or season, by week for a
  year, by month for All time.
- The vessel chart uses the app's colors, with the smaller vessels grouped as "Other".
- Comparisons read "up 15% from last week" in a neutral color, never red.
- Top rated needs at least 2 rated sessions of a tea.
- Analytics no longer shows insights.

### Fixed

- This week includes today on a Sunday.
- Tapping a chart segment opens the segment you tapped, and legend rows can be tapped too.
- Every heatmap cell and chart segment has an outline, so a pale tea type such as White shows on
  any background.
- The heatmap orders months by year, and its text fits at large font sizes.
- Screen readers describe each chart and the direction of each comparison.
- Typing a note no longer saves on every keystroke; it saves once you pause.

## [0.6.0] - 2026-10-22

The journal reads like one: History by day, a session as a timeline of its steeps, and a tea page
that tells you how it brews best.

### Added

- A note for the whole session, apart from each steep's own. Notes written on a first steep move
  into it, in the app and in older backups.
- "Last time" on Log Tea: once you pick a tea, how its last session went, with the date, rating,
  parameters, top tags and your note. Tap it to open the session. Quick-brew cards on Home say when
  you last brewed the tea and how you rated it.
- Logging a brew you already made opens the session review, so it gets ratings, tags, a note and
  photos like any other, and can be saved as a method.
- History groups sessions by day: "Today", "Yesterday", then dates, and months beyond 30 days. A
  busy day shows how many teas and how long you were brewing.
- Filter History by several tea types or vessels at once.
- Search History by tea, producer, notes, tags, location or vessel, and the collection by name,
  producer, origin or description. Case and accents don't matter, and the collection's search works
  together with its filter.
- Session Detail shows the session as a timeline: each steep at its clock time with its rating and
  tags, only what changed from the steep before, and any break between steeps.
- Edit a session's tea, date, time and note. Changing the date moves every steep with it.
- Up and down arrows on Session Detail step to the next or previous session.
- Tea Detail tells the tea's story: your best brew with Brew Again, a flavor profile of the tags you
  confirm most, and a steep curve showing which steep you rate highest.
- A saved method shows the brew it came from ("From your 12 Mar brew, ★4.5"). Tap it to open that
  session.
- A tea's details show how long you've had it, from its purchase date.

### Changed

- The session card leads with the rating, a dot per steep's rating, the top tags and the note.
  Parameters sit below, smaller.
- One sheet edits a steep everywhere: rating, tags, note, time, temperature and water, each saved as
  you change it.
- The review after each steep asks how it was, with big stars, tags ready to tap and an open note.
- Edit Session edits the session only, with Save at the bottom. Steeps are edited from Session
  Detail.
- History's Tea filter is a searchable list. "This Week" and "This Month" read "Last 7 days" and
  "Last 30 days", which is what they meant.
- A tea with no sessions offers "Log a brew".

### Fixed

- A History search with no results says so and offers to clear it, and an empty journal offers "Log
  a brew".
- A session brewed in an archived vessel can be edited.
- Session Detail shows brew times like the rest of the app, and a screen reader announces its tea
  name as a button.

## [0.5.0] - 2026-10-21

A saved method knows every steep, so a gongfu session is Next, then Start, for as long as the tea
lasts.

### Added

- A method plans its steeps: the first steep's time and how much to add each steep after. The
  method form previews the first steeps and marks any adjusted by a brew.
- Brew Again replays the whole session as brewed, every steep at its time, not just the first.
- A session with no method plans by feel from its own steeps: 20s then 25s leads to 30s.
- After each steep, a short review: stars, tasting tags, a note, and "Next (30s)" straight to the
  timer at the planned time. "Adjust" changes the next steep's time and temperature first. A
  single-steep method leads with Finish and offers "Another".
- After Finish, a review of the whole session: every steep, the note, photos, and how it compares
  with your best session of that tea.
- "Save as method" for a session of any number of steeps, and "Update method" when you brewed a
  method differently and liked it.
- Tasting tags on each steep: 23 to choose from, or create your own. The next steep starts with
  the last one's tags, faded until you confirm them.
- Record a rinse on a method, a session, Log Tea and Edit Session.
- "Already brewed", before Start, records the steep at its planned time and opens the review.
- Change the vessel from a steep's parameters; the water fills to its capacity.
- The ready alert names the next steep: "Steep 3 done, next 30s". Tapping it opens the review.
- Tap the Live Activity to open the Timer.
- Quick Timer can become a session. "What are you brewing?" attaches a tea while it runs, and "Log
  it" when it's done. Steeps timed before then are kept at their real times.
- When a Quick Timer finishes with no tea, "Next (+5s)" starts another, and "Just a timer" leaves
  without recording anything.

### Changed

- Quick Timer is the same timer and screen as a session's. It keeps running in the background,
  survives the app being closed, and follows the notification settings.
- "Complete Now" is "Finish early", and records how long the tea actually brewed.
- Steep Complete is replaced by the two reviews above.
- Finish on Home's in-progress banner goes straight to the review.

### Fixed

- The ready alert no longer stays in the notification shade after you've moved on.
- A notification for a session already finished or discarded opens the app on Home instead of
  that session.
- Notifications on Android open the screen they announce, not whichever was posted last.
- Quick Timer no longer leaves a notification that can't be dismissed, or alerts twice.
- Taking time off a timer before starting it no longer takes it below one second.
- Reset on a paused timer stays reset after leaving the screen or closing the app.
- Editing parameters mid-steep no longer loses the timer's progress.
- The timer ring fits large font sizes and small screens, and a screen reader reads its time and
  progress.

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
