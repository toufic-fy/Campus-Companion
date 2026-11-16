# Session 2 workshop — Event update notifications

## Steps

| # | Step | Where | Checkpoint |
|---|------|-------|------------|
| 1 | Declare `POST_NOTIFICATIONS` | `AndroidManifest.xml` | Manifest compiles |
| 2 | Create the **Event updates** channel | `EventNotificationHelper.createChannel()`, called in `MainActivity.onCreate` | Settings → Apps → Campus Companion → Notifications shows "Event updates" |
| 3 | Add the notification preference | `UserPreferencesRepository.eventNotificationsEnabled` (DataStore, default **off**) + switch on the Events screen | The switch shows the saved choice after restarting the app |
| 4 | Request permission contextually | `EventNotificationsSection`: the system dialog appears only when the student turns the switch on (Android 13+) | Allow → switch on. Deny → switch stays off, app keeps working |
| 5 | Build one test notification | "Send test notification" button → `EventNotificationHelper.showTestNotification()` | Title, bell icon, and tap → opens Events (Back → Tasks) |
| 6 | Connect the changed Events result | `RefreshEventWorker` → `EventUpdateNotifier.onEventsRefreshed()` → `EventChangeDetector` | Only new or changed events notify |

## Checkpoint: both gates

A notification is posted only when **both** are true:

1. **User preference**: `eventNotificationsEnabled` is on (checked in `EventUpdateNotifier`).
2. **System permission**: `POST_NOTIFICATIONS` is granted (Android 13+) and notifications aren't blocked
   in settings (checked in `EventNotificationHelper.canPostNotifications()` right before posting).

## Avoiding repeated notifications

Master has no Room table for events yet, so the app keeps a small snapshot in DataStore
(`seen_events_snapshot`): one `id=fingerprint` entry per event. The fingerprint covers
title, description, start/end time and location.

- **First refresh**: only records the snapshot. No notification, so installing the app doesn't announce every event.
- **Same data again**: same snapshot, no notification.
- **New id**: "New event: …". **Same id, changed fields**: "Event updated: …".
- **Removed events**: no notification.
- **Opening the Events screen** marks the list as seen, so the worker never announces events the student already saw.
- All updates use one notification id, so a newer update replaces the old one instead of stacking.

The rules are unit tested in `EventChangeDetectorTest`.

## Tap action

The notification's `PendingIntent` opens `campuscompanion://events`, which is registered as a
`navDeepLink` on the `events` destination. Navigation builds the back stack Tasks → Events,
so Back from a tapped notification returns to Tasks.

## How to test on a device

1. Run the app on an Android 13+ emulator and open **Events**.
2. Turn on **Event update notifications** → the system dialog appears → **Allow**.
3. Tap **Send test notification** → check the title and icon, tap it → Events opens.
4. Turn the switch off and on again, and this time **Don't allow** → the switch stays off and the app still works.
5. Change or add an event in the mockapi.io project, then run the worker (App Inspection →
   Background Task Inspector, or wait for the periodic run) → one "New event / Event updated" notification.
   Run it again without changing data → no new notification.
