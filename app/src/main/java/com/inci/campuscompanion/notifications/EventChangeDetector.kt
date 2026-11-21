package com.inci.campuscompanion.notifications

import com.inci.campuscompanion.model.CampusEvent

/** The parts of an event we remember between refreshes. */
data class EventSnapshot(
    val id: String,
    val title: String,
    val startsAt: String,
    val endsAt: String?,
    val location: String?
)

fun CampusEvent.toSnapshot() = EventSnapshot(
    id = id,
    title = title,
    startsAt = startsAt,
    endsAt = endsAt,
    location = locationLabel
)

/** One change the user should hear about. */
data class EventChange(
    val eventId: String,
    val title: String,
    val summary: String
)

/**
 * Step 6: compares what we saw last time with the fresh events from the API.
 * Only MEANINGFUL updates count:
 *  - the time changed
 *  - the location changed
 *  - the event disappeared (cancelled / removed)
 * Ignored: unchanged events, description or title edits, and brand-new events.
 */
fun findMeaningfulChanges(
    previous: Map<String, EventSnapshot>,
    fresh: List<CampusEvent>
): List<EventChange> {
    val changes = mutableListOf<EventChange>()
    val freshIds = fresh.map { it.id }.toSet()

    for (event in fresh) {
        val before = previous[event.id] ?: continue // new event: not an "update"

        val timeChanged = before.startsAt != event.startsAt || before.endsAt != event.endsAt
        val locationChanged = before.location != event.locationLabel

        val summary = when {
            timeChanged && locationChanged ->
                "Moved to ${event.locationLabel ?: "a new location"}, now ${event.timeLabel}."
            timeChanged -> "New time: ${event.timeLabel}."
            locationChanged -> "New location: ${event.locationLabel ?: "to be announced"}."
            else -> null
        }
        if (summary != null) changes += EventChange(event.id, event.title, summary)
    }

    for (removed in previous.values) {
        if (removed.id !in freshIds) {
            changes += EventChange(removed.id, removed.title, "This event is no longer scheduled.")
        }
    }
    return changes
}
