# Campus Companion — Kris Andary

## Starting point and attribution

Workshop 1: https://github.com/toufic-fy/Campus-Companion/pull/29 (preserved on its existing branch).
This continuation uses the updated class master (15d31b2) and, as requested, Anthony Kerbage / Kerbagio’s Workshop 2 Room + WorkManager work:
https://github.com/toufic-fy/Campus-Companion/pull/66
Original commits f585f0e and a4cc9f1 remain in this branch’s history with their original author. They supply the Room events cache, migration, durable refresh and status. The current class base already includes the Retrofit feed and an initial navigation graph.

## Workshop 3 — Navigation Compose

MainActivity hosts CampusApp; CampusApp owns one remembered NavController. AppNavHost starts on `tasks`, opens `events`, and scopes the events ViewModel to that destination. The real cached feed is reused.

Tasks exposes `onOpenEvents` so it expresses user intent without depending on navigation machinery; previews/tests can supply a simple callback. The graph calls navigate with launchSingleTop to avoid duplicate Events entries. Both system Back and the visible toolbar Back pop the Events entry, revealing Tasks; we never navigate to another Tasks entry to return.

Manual journey: launch → Tasks → Campus events → Events → toolbar Back → Tasks; repeat using system Back. Tasks and dark-mode preference remain saved.

Validation results are recorded after running the checks, not inferred from another student’s PR.

### Observed navigation verification — 18 November 2026

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` passed on the standalone navigation branch.
- `NavigationWorkshopTest.tasksEventsToolbarAndSystemBack` passed on a fresh API 37 emulator: Tasks → Events → toolbar Back → Tasks → Events → system Back → Tasks (1 device test).
- An earlier attempt on the pre-existing emulator failed because Android System UI stopped responding and took window focus. It is not counted as a successful test; the clean emulator run above passed.

## Workshop 4 — Event update notifications

- `CampusApplication` creates the **Event updates** channel at default importance. Recreating it preserves Android’s user-selected channel settings.
- Tasks includes an Event updates switch backed by DataStore, off by default. Android 13+ permission is requested only when the user enables that choice. Denial preserves the choice, explains the block, and offers system notification settings. Returning to the app refreshes the actual system permission/channel state.
- The helper checks both the product preference and current Android permission/app/channel settings. A monochrome icon, stable event ID, immutable PendingIntent and auto-cancel support a useful notification. A visible Send test notification action uses the same gates.
- The worker refreshes the real repository. A shared mutex serializes refreshes; one Room transaction compares known IDs, replaces cached events and advances the confirmed baseline. The first download is quiet. An empty download still establishes a baseline. Unchanged or previously seen events do not notify again, even after disappearing and returning.
- One notification summarizes a refresh containing several new events. It is sent only after a committed refresh and only when both controls allow it. Known IDs advance even when notifications are off or denied, avoiding a backlog of old alerts later.
- A tap carries `campus://events/{id}` through an explicit NavDeepLinkRequest into the normal Events destination. Tasks remains below it in the back stack; the list scrolls to the event when it is present.
- Database migrations 1→2→3 preserve tasks. Room remains the UI source, including a confirmed empty cache.

### Workshop explanation

A successful API call alone does not deserve a notification. Newly published events are useful because students can discover an activity without keeping the app open. The in-app preference answers whether they want updates; Android permission and channel settings answer whether the platform permits delivery. WorkManager performs deferrable refreshes, not exact event-time reminders.

If a refresh finds three new events while the preference is enabled but Android permission is denied, the app updates Room and the baseline, posts nothing, preserves the preference, and offers system settings. No notification is promised for changes already confirmed while blocked. The design favors avoiding duplicate alerts; it does not claim exactly-once delivery across a process crash between committing the cache and posting.

### Observed notification verification — 18 November 2026

On the final notification implementation, a fresh API 37 emulator completed **8 instrumentation tests, all passed**:

1. Version 1→3 migration retains a previously saved completed task.
2. Initial baseline and unchanged refresh remain quiet; one batch of new IDs is detected; removed/reappearing IDs do not alert again.
3. A confirmed empty baseline works; five concurrent refreshes detect the new batch only once.
4. A simulated network exception preserves Room cache and does not advance the baseline.
5. Baseline application package test.
6. The supplied live MockAPI endpoint refreshes Room through the actual WorkManager worker (nonempty cache asserted; the separate endpoint inspection returned 50 records).
7. Tasks → Events → toolbar Back → Tasks → Events → system Back → Tasks.
8. Contextual Android permission dialog → deny → preference retained and settings guidance; app remains navigable; permission granted with preference off produces no notification; controlled new event produces one real Android notification; unchanged data produces none; disabled preference suppresses a new event while still updating Room; re-enabling does not replay it; Send test notification → actual shade tap → Events → Back → Tasks, with notification auto-cancel asserted.

Build, test APK, JVM test and Android lint checks passed. The included JVM test is the original baseline smoke test; the substantive feature checks are the emulator tests above. Android lint reports warnings but no errors.

The initial notification tap test found a wrong navigation overload (a URI was treated as a serializable route). It was fixed using an explicit NavDeepLinkRequest and the final full suite above passed. Old failing logs are retained separately from final passing evidence.

Not exercised: a physical handset, Android versions below 37, force-stopped/cold-process notification taps, device reboot persistence, or waiting an hour for periodic scheduling. Android 15 setup was attempted but its image download was stopped after the clean API 37 emulator proved reliable. The new-event and network-failure cases use controlled local test fixtures; the shared MockAPI dataset was not modified. Tests do not prove exact-time or exactly-once notification delivery.
