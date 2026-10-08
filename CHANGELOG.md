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

[Unreleased]: https://github.com/jketterer/leaf-log/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/jketterer/leaf-log/releases/tag/v0.1.0
