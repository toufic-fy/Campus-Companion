package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(private val api: CampusApi) {

    suspend fun getEvents(): List<CampusEvent> =
        api.getEvents().mapNotNull { it.toCampusEvent() }
}

// Drops events without an id or title; fills the remaining gaps with readable defaults.
fun EventDto.toCampusEvent(): CampusEvent? {
    val id = id ?: return null
    val title = title?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    return CampusEvent(
        id = id,
        title = title,
        location = location?.trim().orEmpty().ifEmpty { "Location TBA" },
        startsAt = startsAt?.trim().orEmpty().ifEmpty { "Time TBA" },
        description = description?.trim().orEmpty()
    )
}
