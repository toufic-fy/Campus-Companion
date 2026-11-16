package com.inci.campuscompanion.notification

import com.inci.campuscompanion.model.CampusEvent

data class EventDiff(
    val newEvents: List<CampusEvent>,
    val changedEvents: List<CampusEvent>,
    val snapshot: Set<String>
)

fun eventFingerprint(event: CampusEvent): String =
    listOf(
        event.title,
        event.startsAt,
        event.endsAt.orEmpty(),
        event.locationLabel.orEmpty()
    ).joinToString("|")

fun diffEvents(previous: Set<String>?, events: List<CampusEvent>): EventDiff {
    val snapshot = events.map { "${it.id}|${eventFingerprint(it)}" }.toSet()
    if (previous == null) {
        return EventDiff(emptyList(), emptyList(), snapshot)
    }
    val previousById = previous.associate { entry ->
        entry.substringBefore("|") to entry.substringAfter("|")
    }
    val newEvents = mutableListOf<CampusEvent>()
    val changedEvents = mutableListOf<CampusEvent>()
    for (event in events) {
        val old = previousById[event.id]
        when {
            old == null -> newEvents.add(event)
            old != eventFingerprint(event) -> changedEvents.add(event)
        }
    }
    return EventDiff(newEvents, changedEvents, snapshot)
}
