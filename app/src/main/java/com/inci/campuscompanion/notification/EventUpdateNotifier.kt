package com.inci.campuscompanion.notification

import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

class EventUpdateNotifier(
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationHelper: EventNotificationHelper
) {

    suspend fun onEventsRefreshed(events: List<CampusEvent>) {
        val knownIds = preferencesRepository.knownEventIds.first()
        val newEvents = if (knownIds.isEmpty()) {
            emptyList()
        } else {
            events.filter { it.id !in knownIds }
        }

        preferencesRepository.setKnownEventIds(events.map { it.id }.toSet())

        val canNotify = preferencesRepository.eventUpdatesEnabled.first() &&
            notificationHelper.isPermissionGranted()

        if (canNotify && newEvents.isNotEmpty()) {
            notificationHelper.showNewEvents(newEvents)
        }
    }
}
