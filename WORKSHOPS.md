# Campus Companion workshops

This branch builds on `feature/charbel-events-api` and implements Navigation Compose, Events caching/background work, and notifications.

## Requirements and reference

- Navigation_Compose_From_Intent_to_NavHost.pdf: two destinations, Tasks first, callbacks, system Back and toolbar Back, reuse the Events feed.
- Android_Notifications_and_Runtime_Permissions.pdf: channel at application startup, opt-in preference, Android 13+ permission requested after opt-in, test notification and tap to Events; meaningful new-event integration.
- The third PDF was not supplied. The background-work scope is inferred from the notifications PDF prerequisites and Kerbagio's public `feature/anthonykerbage-workmanager` branch: Room Events cache, connected-network constraints, unique manual and periodic work, and transient-error retries. Reference: https://github.com/Kerbagio/Campus-Companion/tree/feature/anthonykerbage-workmanager . This implementation adapts the concepts to this project's existing packages and Events API.

## What to demonstrate

1. Open the app: Tasks appears first. Add a task and toggle dark mode.
2. Tap **Campus events**. Confirm content, empty, loading, error and retry behavior. Use toolbar Back and system Back; both return to Tasks.
3. Load Events once, then disconnect. Saved Events remain visible after reopening the app. A failed foreground refresh explains that saved data is being shown.
4. Tap **Refresh when online** while disconnected. Work is queued; reconnect and observe it complete. Unique periodic refresh is also registered once, with hourly eligibility, network and battery constraints. WorkManager does not promise an exact execution time.
5. Return to Tasks. Event updates start off. Turn them on: Android 13+ asks for permission at that moment. Deny permission: Tasks and Events remain usable, the preference remains on, and a settings link explains how to allow notifications later.
6. Allow permission and channel delivery, then tap **Send test**. Check the monochrome icon and auto-dismiss; tapping opens Events. Verify both a closed app and an already-open app, followed by Back to Tasks.
7. A first successful refresh establishes a silent baseline, even when empty. After that, only never-seen event IDs generate an update. Repeated refreshes, changed titles for known IDs, and a removed ID returning do not notify. The cache and seen-ID history advance transactionally. Preference off or permission/channel blocked always suppresses delivery; enabling later does not replay suppressed updates.
8. With updates enabled, add a new event to the course API and refresh. One summary notification opens the first new event and scrolls to it. Avoid changing shared course data unless your instructor allows it.
9. Upgrade from database version 1: existing Tasks remain intact. The migration adds only Events tables.

## Navigation explanation

CampusApp owns the NavController. Tasks receives onOpenEvents because it expresses user intent while the graph decides the route. Events receives onBack, which pops the stack instead of adding another Tasks entry. Notification Intents are translated at the same navigation boundary into an Events route and optional event ID.

## Notification explanation

The product preference belongs in DataStore. Android permission and channel status are queried from the system. If a refresh finds three new events but permission is denied, update the cache and deduplication history, preserve the preference, and post nothing. Offer system settings so the user can change permission later. A successful refresh without new events does not deserve a notification.

## Validation commands

Use the project's JDK 25/Gradle toolchain and Android SDK 37.0:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

Unit tests cover remote mapping, existing ViewModel behavior, silent baselines, an initially empty feed, persistence of deduplication across repository recreation, failed refresh preservation, concurrent refreshes, and notification-delivery failure isolation. Device checks are necessary for Android permission dialogs, notifications, navigation entry points and migration behavior. See VALIDATION.md for results from this environment.
