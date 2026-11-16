package com.inci.campuscompanion.notification

import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first

class EventNotificationPolicy(
    private val preferences: UserPreferencesRepository,
    private val permissionChecker: NotificationPermissionChecker,
    private val notificationHelper: EventNotificationHelper,
) {
    suspend fun onNewEvents(newEvents: List<CampusEvent>) {
        if (newEvents.isEmpty()) return
        if (!preferences.eventUpdatesEnabled.first()) return
        if (!permissionChecker.isGranted()) return

        if (newEvents.size == 1) {
            notificationHelper.showNewEvent(newEvents.first())
        } else {
            notificationHelper.showNewEventsSummary(newEvents.size)
        }
    }
}
