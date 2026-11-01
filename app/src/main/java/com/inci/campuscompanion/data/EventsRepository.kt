package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(
    private val remote: EventsRemoteDataSource
) {
    suspend fun getEvents(page: Int, limit: Int): List<CampusEvent> =
        remote.getEvents(page, limit).map { dto -> dto.toCampusEvent() }

    private fun EventDto.toCampusEvent() = CampusEvent(
        id = id,
        title = name,
        timeLabel = formatTime(startsAt),
        locationLabel = address ?: "TBA",
        description = description.orEmpty()
    )

    private fun formatTime(raw: String): String =
        raw.replace('T', ' ').take(16)
}
