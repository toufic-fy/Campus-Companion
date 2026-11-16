package com.campus.events.notifications

import com.campus.events.data.Event

/**
 * Permission answers "may the app notify?"; product logic answers "should it notify now?".
 * Kept as a pure function so every user state can be unit-tested without a device.
 */
sealed interface NotificationDecision {
    data class NotifyOne(val event: Event) : NotificationDecision
    data class NotifySummary(val events: List<Event>) : NotificationDecision
    data class Skip(val reason: SkipReason) : NotificationDecision
}

enum class SkipReason(val message: String) {
    PREFERENCE_OFF("Event updates are turned off in the app, so nothing was posted."),
    PERMISSION_DENIED("Android notifications are not allowed for this app, so nothing was posted."),
    NO_NEW_EVENTS("No new events since the last refresh, so nothing was posted."),
}

object NotificationPolicy {

    fun decide(
        eventUpdatesEnabled: Boolean,
        permissionGranted: Boolean,
        newEvents: List<Event>,
    ): NotificationDecision = when {
        !eventUpdatesEnabled -> NotificationDecision.Skip(SkipReason.PREFERENCE_OFF)
        !permissionGranted -> NotificationDecision.Skip(SkipReason.PERMISSION_DENIED)
        newEvents.isEmpty() -> NotificationDecision.Skip(SkipReason.NO_NEW_EVENTS)
        newEvents.size == 1 -> NotificationDecision.NotifyOne(newEvents.single())
        // Several new events -> one grouped update rather than a burst of notifications.
        else -> NotificationDecision.NotifySummary(newEvents)
    }
}
