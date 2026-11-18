# Observed workshop evidence

Captured from the clean API 37 emulator on 18 November 2026, not mockups.

- 01: Tasks with event updates disabled.
- 02: Android permission dialog triggered by enabling updates.
- 03: Permission denied, preference preserved, settings explanation visible.
- 04: Preference off while Android permission is granted; no notification was posted (asserted in test).
- 05: Actual test notification in Android’s notification shade.
- 06: Events opened by tapping that notification; cached data is visible while another refresh runs.

The test log records all 8 tests passing. Screenshots alone do not prove absence of notifications; the instrumentation assertions cover those cases. New/unchanged-event policy uses controlled fixtures; the live worker test uses the supplied unchanged endpoint. See WORKSHOPS.md for limits and attribution.
