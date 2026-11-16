# Event update notifications workshop

Implements pages 10–11 of **Android Notifications and Runtime Permissions**, including the sixth (refresh integration) step.

| Step | Implementation |
| --- | --- |
| 1. Declare permission | `POST_NOTIFICATIONS` in `AndroidManifest.xml` |
| 2. Create channel | `CampusApplication.onCreate()` creates **Event updates**, default importance |
| 3. Save preference | DataStore `event_updates_enabled`, off by default, shown on the Events screen |
| 4. Contextual permission | Android 13+ prompt when the user switches Event updates on; denial preserves the preference |
| 5. Test notification | **Send test notification**, monochrome icon, stable ID, auto-cancel, explicit immutable PendingIntent |
| 6. Connect refresh | Foreground and Worker refreshes update Room first, then may post one new-event notification |

## Delivery policy

- Both the saved preference and current Android notification permission/settings must allow posting, including channel settings.
- The first successful refresh establishes a silent baseline. This includes a successful empty feed.
- Later refreshes notify only for newly seen IDs relative to the previous confirmed snapshot. Edited existing events still update the cache silently.
- A refresh containing several new events posts one notification for the first new event.
- An unchanged response produces no notification. A failed refresh leaves the cache/baseline intact.
- The Room cache and refresh marker survive process restarts. A shared repository serializes overlapping UI/Worker refreshes.
- A notification tap enters the normal Events navigation destination through `campus://events/{eventId}`. The workshop has an Events list, so the tap opens that list.
- Denied/disabled notifications never prevent cache updates. Missed updates are not replayed when permission is later granted.

The starting repository contained Room storage only for Tasks. This change adds Events storage and a non-destructive database migration from version 1 to 2, preserving Tasks.

## Run and test

Open this folder in Android Studio, allow Gradle sync, select an Android 13+ emulator/device, and run **app**. Use the existing project toolchain (JDK 25 and SDK 37).

1. Open **Campus Events**. The toggle starts off; no permission prompt appears at startup.
2. Turn **Event updates** on and deny permission. The app stays usable, the toggle stays on, and the explanation links to system settings.
3. Allow notifications, return to Events, and tap **Send test notification**. Open the notification shade and tap it; the Events screen opens and the notification disappears.
4. Turn the toggle off while Android still grants permission. Refreshes do not notify.
5. With updates enabled/allowed, refresh the same API response twice: no repeat notification.
6. Publish a new event in the API after a successful baseline refresh. Refresh again: one notification. Refresh unchanged data or restart the app: no repeat.
7. Disable the Event updates channel in Android settings. Return to the app: test posting is disabled and normal refreshes still work.
8. Disconnect the network: previously cached Events remain available. Existing Tasks remain after the database upgrade.

Automated checks:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Unit tests cover the preference/permission truth table, baseline/empty responses, repeated/changed/new events, overlapping refreshes, repository recreation, and failed network requests. Device-only behaviors (the permission dialog, channel display, notification appearance and navigation) require an emulator/device check.
