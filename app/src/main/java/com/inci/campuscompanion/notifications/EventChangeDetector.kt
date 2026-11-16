package com.inci.campuscompanion.notifications

import com.inci.campuscompanion.model.CampusEvent

object EventChangeDetector {

    // Compares the cache before a refresh with the fresh list from the API.
    // An event counts if its id is new, or if its title/description changed.
    // The very first load (empty cache) returns nothing, so installing the app
    // doesn't fire a notification for every existing event.
    fun findNewOrChanged(
        previous: List<CampusEvent>,
        latest: List<CampusEvent>
    ): List<CampusEvent> {
        if (previous.isEmpty()) return emptyList()
        val previousById = previous.associateBy { it.id }
        return latest.filter { event -> previousById[event.id] != event }
    }
}
