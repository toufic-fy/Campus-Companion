# Three workshop submission

Student: Charbel Ghossoub. Course GitHub account: cghsb.

## Scope

1. Navigation Compose: one Activity, Tasks as the start destination, an Events destination, screen callbacks, system Back and toolbar Back.
2. Background work: a Room Events cache and version 1 to 2 migration preserving Tasks, unique network-constrained WorkManager refresh, periodic refresh, retries for transient failures, and visible queue status.
3. Notifications: startup channel, persisted opt-in, contextual Android 13 permission, denied-permission settings guidance, test notification, meaningful-new-event policy, deduplication and notification entry into Events.

The supplied Navigation and Notifications PDFs are the primary requirements. The missing background-work handout is represented by Kerbagio's public WorkManager workshop PR #66 and the Notifications handout's prerequisites. His Navigation PR #35 and Notifications PR #65 were also reviewed as reference implementations. This project keeps its own existing packages, stronger payload validation and combined Room cache; it does not transplant his code or remove the cache when adding notifications.

## Integration with the teacher base

The teacher master revision 15d31b20b4e868561b6e632110369f5210b6b9cf changed package organization and added another Events/WorkManager implementation. The merge keeps this submission's coherent, tested implementation of those same layers and removes the parallel sample classes. It preserves Tasks, dark mode, navigation and background refresh through the integrated application graph. The teacher revision is recorded as a merge parent so the submission can be compared and merged against that base.

## Refresh explanation

The Events destination observes EventsViewModel state, backed by a Room Flow. Foreground Refresh and Retry call the shared CachedEventsRepository, whose remote repository invokes the Retrofit API and maps DTOs into CampusEvent. A successful transaction replaces the cache and advances the seen-ID baseline. The Flow updates the screen; empty results are deliberate Empty content and failed requests retain saved events. Refresh when online enqueues unique one-time WorkManager work with KEEP. RefreshEventsWorker calls the same repository after connectivity is available, retries transient failures with backoff and updates the same cache without needing the Events screen to remain open. Periodic work is also unique and deferrable.

After the first silent refresh baseline, newly seen event IDs may produce a notification. Delivery requires the DataStore preference, runtime permission, app-wide notifications and the Event updates channel to be enabled. Suppressed or unchanged results do not replay notifications later. A notification PendingIntent enters MainActivity; CampusApp translates it to the Events route, preserving Tasks for Back.

## Device checks

The verification workflow builds the app, runs JVM tests and lint, then executes instrumentation on an Android API 35 emulator. In addition to navigation, migration and live API/offline recovery checks, the workshop device tests exercise actual permission denial/allow, an actual OS notification and its PendingIntent, unchanged-feed suppression, preference-off suppression, and five offline refresh requests producing one queued job that completes after reconnecting.

The notification integration test uses an isolated Room database with controlled remote responses. It never edits the shared class API. Network and notification preferences are restored in finally blocks. Results and evidence are recorded in the workflow run and VALIDATION.md after execution; the existence of a test does not itself claim it passed.
