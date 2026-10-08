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

[Unreleased]: https://github.com/jketterer/leaf-log/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/jketterer/leaf-log/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/jketterer/leaf-log/releases/tag/v0.1.0
