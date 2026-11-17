# Workshop validation — 17 November 2026

The final source was checked with Gradle 9.6.0, JDK 25, and Android SDK 37.0.

## Passed

- `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL.
- 17 unit tests, zero failures or errors: 5 remote repository tests, 5 ViewModel tests, 6 cache/notification-deduplication tests, and the existing example test.
- Debug application APK and instrumentation-test APK both built successfully.
- Android lint: zero errors, 23 warnings. Warnings cover dependency/version recommendations, a redundant manifest label, and unused resources; none report missing runtime-permission checks.
- Device-test sources compile, including `WorkshopNavigationTest` and `EventsMigrationTest`.
- Read-only HTTPS check of the production Events API: 50 records with the expected fields, valid date ranges and unique IDs. No API records were modified.
- `git diff --check` passes. The published implementation tree was checked against the local tree.

## Still requires an emulator or device

No connected Android tests were executed during this task. Compilation does not establish runtime behavior of system permission dialogs, notification delivery, notification taps, system Back or a real database upgrade. Run the navigation and migration tests and demonstrate the permission-denied, preference-off, unchanged-feed and allowed-new-event cases on Android 13 or newer. Test notification entry with the app both closed and already open.

The earlier API-only report in EVENTS_API_README.md contains historical device-test results; those results are not validation of these new workshops.

WORKSHOPS.md contains the demonstration sequence and design explanations. The third PDF was not supplied; its background-work scope was inferred from the notification PDF prerequisites and Kerbagio's reference implementation.

The source comparison in COMPARISON.md identified an empty-background-refresh recovery bug. The regression failed before the fix; the corrected code passes the full 17-test suite, lint, application build and instrumentation-test APK build. Standalone workshop branch corrections are also described there.
