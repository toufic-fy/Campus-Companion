package com.inci.campuscompanion.notifications

import com.inci.campuscompanion.model.CampusEvent

/** What changed between the last seen list of events and the latest refresh. */
data class EventChanges(
    val newEvents: List<CampusEvent>,
    val updatedEvents: List<CampusEvent>
) {
    val isMeaningful: Boolean get() = newEvents.isNotEmpty() || updatedEvents.isNotEmpty()

    fun notificationTitle(): String = when {
        newEvents.size == 1 && updatedEvents.isEmpty() -> "New event: ${newEvents.first().title}"
        updatedEvents.size == 1 && newEvents.isEmpty() -> "Event updated: ${updatedEvents.first().title}"
        else -> "Campus events changed"
    }

    fun notificationText(): String {
        val parts = buildList {
            if (newEvents.isNotEmpty()) add("${newEvents.size} new")
            if (updatedEvents.isNotEmpty()) add("${updatedEvents.size} updated")
        }
        val first = (newEvents + updatedEvents).first()
        return "${parts.joinToString(", ")} • ${first.timeLabel}"
    }
}

/**
 * Pure Kotlin (no Android types) so it can be unit tested.
 *
 * A snapshot is a set of "id=fingerprint" strings. The fingerprint covers the fields a
 * student cares about, so a refresh that returns the same data produces the same snapshot
 * and therefore no notification.
 */
object EventChangeDetector {

    fun snapshotOf(events: List<CampusEvent>): Set<String> =
        events.map { "${it.id}=${fingerprint(it)}" }.toSet()

    /**
     * @param previousSnapshot null when nothing was stored yet (first run). The first run
     * only records a baseline, so installing the app never fires a notification for
     * every existing event.
     */
    fun detect(previousSnapshot: Set<String>?, current: List<CampusEvent>): EventChanges {
        if (previousSnapshot == null) return EventChanges(emptyList(), emptyList())

        val previousById = previousSnapshot.associate { entry ->
            entry.substringBeforeLast('=') to entry.substringAfterLast('=')
        }

        val newEvents = current.filter { it.id !in previousById }
        val updatedEvents = current.filter { event ->
            val oldFingerprint = previousById[event.id]
            oldFingerprint != null && oldFingerprint != fingerprint(event)
        }
        return EventChanges(newEvents, updatedEvents)
    }

    private fun fingerprint(event: CampusEvent): String =
        listOf(
            event.title,
            event.description,
            event.startsAt,
            event.endsAt.orEmpty(),
            event.locationLabel.orEmpty()
        ).joinToString("\u001F").hashCode().toString(16)
}
