package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent

/**
 * Makes remote data usable by the rest of the app:
 * maps EventDto (wire format) into CampusEvent (UI-ready data).
 * Caching / Room sync is intentionally left for a later module.
 */
class EventsRepository(
    private val remote: EventsRemoteDataSource
) {
    suspend fun getEvents(): List<CampusEvent> =
        remote.getEvents().map { dto -> dto.toCampusEvent() }

    private fun EventDto.toCampusEvent() = CampusEvent(
        id = id,
        title = name,
        timeLabel = formatTime(startsAt),
        locationLabel = address ?: "TBA",
        description = description.orEmpty()
    )

    // "2026-03-14T09:30:00.000Z" -> "2026-03-14 09:30"
    // Formatting stays here, out of the composable.
    private fun formatTime(raw: String): String =
        raw.replace('T', ' ').take(16)
}
