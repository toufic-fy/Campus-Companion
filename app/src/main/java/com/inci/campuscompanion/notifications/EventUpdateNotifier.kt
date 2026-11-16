package com.inci.campuscompanion.notifications

import android.content.Context
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

/**
 * Step 6: connects a refreshed Events result to the notification.
 *
 * Gate: the user preference AND the system permission must both allow it,
 * and the data must actually have changed since the last time we looked.
 */
class EventUpdateNotifier(
    private val context: Context,
    private val preferences: UserPreferencesRepository
) {

    /** Called by RefreshEventWorker with the freshly fetched events. */
    suspend fun onEventsRefreshed(events: List<CampusEvent>) {
        val previous = preferences.getSeenEventsSnapshot()
        val changes = EventChangeDetector.detect(previous, events)

        // Always remember what we saw, so the same data never notifies twice.
        preferences.setSeenEventsSnapshot(EventChangeDetector.snapshotOf(events))

        if (!changes.isMeaningful) return
        if (!preferences.eventNotificationsEnabled.first()) return

        EventNotificationHelper.showEventUpdate(
            context = context,
            title = changes.notificationTitle(),
            text = changes.notificationText()
        )
    }

    /** The student is looking at the list right now: mark it as seen, never notify. */
    suspend fun markAsSeen(events: List<CampusEvent>) {
        preferences.setSeenEventsSnapshot(EventChangeDetector.snapshotOf(events))
    }
}
