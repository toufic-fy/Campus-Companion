package com.inci.campuscompanion.notification

import com.inci.campuscompanion.model.CampusEvent

/** Notify about a meaningful change, not about a successful refresh. */
object EventUpdatePolicy {

    /**
     * Events published since the last confirmed refresh. With no previous refresh there is
     * nothing to compare against, so the first result never counts as "new".
     */
    fun newEventsSince(previousIds: Set<String>?, current: List<CampusEvent>): List<CampusEvent> {
        if (previousIds == null) return emptyList()
        return current.filter { it.id !in previousIds }
    }

    /** A notification needs the student's choice, the system permission and something new. */
    fun shouldNotify(
        preferenceEnabled: Boolean,
        permissionGranted: Boolean,
        newEvents: List<CampusEvent>
    ): Boolean = preferenceEnabled && permissionGranted && newEvents.isNotEmpty()
}
