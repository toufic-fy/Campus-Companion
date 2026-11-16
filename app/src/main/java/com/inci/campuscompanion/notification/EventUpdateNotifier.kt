package com.inci.campuscompanion.notification

import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

/** Compares a fresh event list with the ids we already knew and notifies about new ones. */
class EventUpdateNotifier(
    private val preferences: UserPreferencesRepository,
    private val helper: EventNotificationHelper
) {
    suspend fun onEventsRefreshed(events: List<CampusEvent>) {
        val known = preferences.knownEventIds.first()
        // First sync: just remember the events, do not spam the user.
        val newEvents = if (known.isEmpty()) emptyList() else events.filter { it.id !in known }
        preferences.setKnownEventIds(events.map { it.id }.toSet())

        if (newEvents.isNotEmpty() && preferences.eventUpdatesEnabled.first()) {
            helper.notifyNewEvents(newEvents)
        }
    }
}