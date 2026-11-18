# Comparison with Kerbagio — 17 November 2026

This is a source-level comparison of the workshop feature logic, not a claim that the projects are identical or that every instructor requirement is known. The third PDF has not been supplied. Android device execution remains unverified during this task.

Reference branches reviewed:

- [Events API](https://github.com/Kerbagio/Campus-Companion/tree/20947e9c2fd8e473fc2928ed65f1d4b58b9caf2a)
- [Navigation](https://github.com/Kerbagio/Campus-Companion/tree/07650cde2f5909daff6283874599241b32f30a2d)
- [WorkManager](https://github.com/Kerbagio/Campus-Companion/tree/a4cc9f12f68902e9ae9eaacf2e6db3d4742e42f6)
- [Notifications](https://github.com/Kerbagio/Campus-Companion/tree/79e1d1eab8bfc1d4d119536e0badc30d77ba404a)

The reference branches are separate implementations. In particular, his notifications branch uses a Task-only version-1 database and a remote-only Events repository; it does not retain the Events Room cache from his WorkManager branch. Our combined implementation retains the cache and its migration.

## Functional comparison

| Feature | Kerbagio | This project |
| --- | --- | --- |
| Events API | Retrofit, repository mapping, observable loading/content/empty/error states, retry | Present; Kotlin serialization and stricter ID/date validation preserve the existing API workshop |
| Navigation | Tasks start destination, Events route, callback navigation, popBackStack and toolbar Back in navigation branch | Present; toolbar Back also retained in the combined implementation |
| Events Room cache | Event entity, DAO Flow, transactional replace and database migration in WorkManager branch | Present, plus transactional synchronization baseline and seen-ID history |
| Manual background refresh | Main Refresh/Retry actions enqueue unique one-time work | A separate **Refresh when online** action enqueues unique work; foreground Refresh/Retry also remain available |
| Periodic refresh | Unique hourly eligible work, connected network and battery-not-low constraints, KEEP policy | Same core behavior; registered at Application startup |
| Work status | Queued/running/current/failed UI derived from WorkInfo | Equivalent status messages for queued manual work |
| Offline behavior | Cached data wins over refresh status in WorkManager ViewModel | Cached data remains visible and foreground failures add an explanation |
| Retry policy | WorkManager branch retries IO/5xx; notification branch retries every Exception without a limit | IO, HTTP 429 and 5xx receive exponential retries, capped at five attempts; invalid payloads and other HTTP errors fail |
| Channel and permission | Default-importance Event updates channel at startup; POST_NOTIFICATIONS requested after opting in on Android 13+ | Present; also checks app-wide and channel delivery status, and catches permission revocation during posting |
| Product preference | DataStore switch, denied-permission message, settings link, gated test action | Present, including permission/settings recheck on resume and pre-Android-8 settings fallback |
| Automatic notification trigger | Notification branch worker invokes its notifier after refresh | Both foreground and background successful refreshes pass through the shared repository notification policy |
| Notification intent | Immutable/updatable PendingIntent, auto-cancel, opens Events using a registered deep link and a new task | Immutable/updatable PendingIntent, auto-cancel, opens Events and optionally scrolls to its event; handles initial and new Intents while retaining Tasks in the back stack |
| Notification content | Fixed update/test IDs; expandable text listing all new event titles | Stable event-based ID; summary count and first new event title, without expandable all-title text |
| Deduplication | Previous refresh IDs stored in DataStore; an empty set is treated as no baseline | Separate persisted baseline handles a genuinely empty first refresh; Room retains ever-seen IDs so disappearing/reappearing events do not notify again |
| Event ordering | WorkManager cache sorts by raw startsAt and retains raw start/end timestamps | Cache sorts by ID and stores display-ready time labels; chronological ordering is not implemented |
| ViewModel ownership | Events ViewModel created inside the destination in navigation branch | Activity-owned Events ViewModel; shared cache survives navigation and background refresh |

The refresh entry points, ordering, data representation, retry bounds and notification formatting are real differences. They are not evidence of an omitted core feature, but chronological ordering would require additional work if the missing PDF specifically requires it. No change was made merely to reproduce a visual or organizational choice.

## Problems found and corrected in this audit

1. **Empty background refresh did not clear a previous offline error.** The original ViewModel could remain in Error after a worker successfully cached an empty response, because an unchanged empty list does not communicate refresh success. A regression test failed before the fix. Successful refresh completion is now signalled separately after the cache transaction; the ViewModel reads the cache and transitions to Empty even when its list did not change. Added repository and ViewModel regression tests.
2. **The standalone WorkManager branch did not expose its manual work request.** The combined implementation exposed it, but the earlier branch snapshot did not. Added the queued-refresh action and WorkInfo status display to that branch.
3. **The standalone navigation branch had an earlier plain Back button.** Replaced it with the actual toolbar Back implementation already used in the combined code. The WorkManager branch receives the toolbar as well. Inherited live-device tests were aligned with the Campus events entry action; the WorkManager version now expects saved events to remain visible offline.

These changes correct functional behavior and make the workshop branches independently demonstrate their features. They do not copy the reference notification branch's removal of Room caching.

See VALIDATION.md for the combined implementation checks. Separate branch build results are recorded in WORKSHOP_BRANCH_CHECKS.md after verification. Device checks remain necessary for notification permission dialogs, actual delivery/taps, system Back and a real upgrade from version 1.
