package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventsRepository(private val api: CampusApi = CampusApi.instance) {

    suspend fun getEvents(): List<CampusEvent> = withContext(Dispatchers.IO) {
        api.getEvents().map { it.toCampusEvent() }
    }
}

// DTO -> domain mapping
fun EventDto.toCampusEvent(): CampusEvent = CampusEvent(
    id = id,
    title = name ?: "Untitled event",
    description = description.orEmpty(),
    location = address.orEmpty(),
    startDate = startingDate?.take(10).orEmpty(), // "2026-10-20T12:23:40Z" -> "2026-10-20"
    endDate = endingDate?.take(10).orEmpty(),
    imageUrl = avatar
)
