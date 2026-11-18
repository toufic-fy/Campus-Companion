# Three workshop verification 18 November 2026

Student: Charbel Ghossoub. Course account: cghsb.

Verified code commit: a9c4c8904431c3dfb5ac16b79ca7335612af6195.
Successful run: https://github.com/cghsb/Campus-Companion/actions/runs/44281864413

## Results

- Debug APK built successfully with Gradle 9.6.0, JDK 25 and Android SDK 37.0.
- All 17 JVM unit tests passed: 5 remote repository, 5 ViewModel, 6 cache/deduplication and the existing example test. Zero failures and zero skipped tests.
- Android lint completed successfully. Dependency/version and resource warnings remain; the run did not suppress lint errors.
- All 13 instrumentation tests passed on an Android API 35 emulator. Zero failures, errors or skipped tests.
- The workflow artifact contains the APK, HTML/XML reports, screenshots and relevant logs. No shared API records were modified.

## Device coverage

- Navigation: Tasks opens first; the Events action, toolbar Back and actual Android system Back return to Tasks. An incoming notification URI while the app is open reaches Events and preserves the Tasks Back stack.
- Room migration: an actual version-1 database retains its saved Task and gains a usable Events cache, baseline and seen-ID history.
- Background work: five offline refresh requests produce one unique queued WorkManager request. Restoring real emulator connectivity makes that same request succeed and write live Events into Room.
- Notifications: no startup permission prompt; opt-in triggers the Android permission dialog; denial preserves the preference and leaves the app usable without posting. After permission is granted, the test notification has a title, small icon, auto-cancel flag and working PendingIntent into Events.
- Notification policy: a first refresh is silent; a genuinely new ID posts once; unchanged refreshes do not duplicate it; preference off suppresses delivery while the cache still updates; enabling later does not replay unchanged events. Controlled new-event responses use an isolated Room cache.
- Events API: all four failure-exercise scenarios pass. The production MainActivity loads live data, retains saved cards offline and recovers after connectivity returns.

## Corrections verified

The first device runs exposed a Back handling issue and a test method with a non-void JVM signature. Events now handles system Back at the navigation boundary; MainActivity uses singleTop for notification entry; the WorkManager test returns Unit explicitly.

Notification entry also exposed an ActivityScenario cleanup limitation: the runner filters lifecycle events by the original launch Intent, while the app correctly calls setIntent for an incoming notification. The tests retain and restore the launch Intent only for cleanup, after all real entry and Back assertions have run. No app assertions were removed. The complete rerun above passes.

## Scope and practical limits

The Navigation and Notifications PDFs supplied for the assignment are the primary requirements. The missing third handout's WorkManager/Room scope was inferred from the Notifications prerequisites and Kerbagio's public WorkManager PR #66, as requested. His Navigation and Notifications PRs were reviewed as references; this implementation preserves its own integrated cache and architecture.

Runtime verification covers Android API 35. Notification PendingIntent delivery is tested with an already-open app; cold notification entry is implemented but is not a separate automated case in this run. WorkManager timing is deferrable, not an exact reminder guarantee.

WORKSHOP_COMPLETION.md explains the integrated requirements, teacher-base merge and Refresh layers. WORKSHOPS.md provides the demonstration sequence. COMPARISON.md and WORKSHOP_BRANCH_CHECKS.md retain the dated 17 November source-audit history; their earlier device-testing limitations are superseded by the successful run above.
