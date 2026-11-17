package com.inci.campuscompanion.notification

import android.content.Context
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

/** Called by the background refresh once fresh events are available. */
class EventUpdateNotifier(
    private val context: Context,
    private val preferences: UserPreferencesRepository
) {
    suspend fun onEventsRefreshed(events: List<CampusEvent>) {
        val newEvents = EventUpdatePolicy.newEventsSince(preferences.lastSeenEventIds.first(), events)

        // Remember what was seen even when we do not notify, so turning updates on
        // later does not announce old events as new.
        preferences.setLastSeenEventIds(events.map { it.id }.toSet())

        val shouldNotify = EventUpdatePolicy.shouldNotify(
            preferenceEnabled = preferences.eventUpdatesEnabled.first(),
            permissionGranted = NotificationPermission.isGranted(context),
            newEvents = newEvents
        )
        if (shouldNotify) {
            EventNotificationHelper.postNewEvents(context, newEvents)
        }
    }
}
