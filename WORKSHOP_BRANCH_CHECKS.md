# Workshop branch checks — 17 November 2026

Checked each corrected implementation separately with the project toolchain (Gradle 9.6.0, JDK 25, SDK 37.0).

| Implementation | Unit tests | Failures/errors | Debug APK | Lint errors |
| --- | --- | --- | --- | --- |
| combined | 17 | 0 | Built | 0 |
| navigation | 9 | 0 | Built | 0 |
| workmanager | 17 | 0 | Built | 0 |

The combined implementation also builds its instrumentation-test APK. Navigation and WorkManager branches were checked with `testDebugUnitTest assembleDebug lintDebug`; their device-test APKs were also built after aligning inherited live-device checks with the new navigation/cache behavior. Test counts include the inherited API tests and example test; they are not a claim of automated Android device coverage.

The combined and WorkManager implementations include two regression tests for successful empty refreshes after an offline error. That failure was reproduced before the correction. The standalone navigation branch now uses toolbar Back, and the standalone WorkManager branch exposes queued refresh and status feedback.

No emulator/device tests were executed in this audit. Permission dialogs, actual notification delivery/taps, system Back and the database migration still require device verification. COMPARISON.md in master records the detailed reference comparison, intentional differences and source revisions.
