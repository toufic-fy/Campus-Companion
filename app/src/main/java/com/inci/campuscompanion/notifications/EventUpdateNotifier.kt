package com.inci.campuscompanion.notifications

import android.content.Context
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

class EventUpdateNotifier(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationHelper: EventNotificationHelper
) {

    suspend fun onEventsRefreshed(events: List<CampusEvent>) {
        val knownIds = preferencesRepository.knownEventIds.first()
        val newEvents = events.filter { it.id !in knownIds }

        preferencesRepository.setKnownEventIds(events.map { it.id }.toSet())

        val isFirstRefresh = knownIds.isEmpty()
        if (isFirstRefresh || newEvents.isEmpty()) return

        val canNotify = preferencesRepository.eventUpdatesEnabled.first() &&
            NotificationPermissionChecker.isGranted(context)

        if (canNotify) {
            notificationHelper.showNewEvents(newEvents)
        }
    }
}
