package com.inci.campuscompanion.notifications

import com.inci.campuscompanion.data.repository.CampusEvent

object NotificationPolicy {

    // Events in the remote list whose id is not in the cache
    fun findNewEvents(cached: List<CampusEvent>, remote: List<CampusEvent>): List<CampusEvent> {
        val knownIds = cached.map { it.id }.toSet()
        return remote.filter { it.id !in knownIds }
    }

    // Slide 12 rule, plus "there is something to say"
    fun shouldNotify(
        settings: NotificationSettings,
        permissionGranted: Boolean,
        newEvents: List<CampusEvent>
    ): Boolean =
        settings.eventUpdatesEnabled && permissionGranted && newEvents.isNotEmpty()
}