package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventsRepository(private val api: CampusApi) {

    suspend fun getEvents(): Result<List<CampusEvent>> = withContext(Dispatchers.IO) {
        runCatching { api.getEvents().map { it.toCampusEvent() } }
    }
}

/** Maps the network DTO to the model the UI uses. */
fun EventDto.toCampusEvent(): CampusEvent = CampusEvent(
    id = id.orEmpty(),
    title = name ?: "Untitled event",
    description = description.orEmpty(),
    location = address ?: "TBA",
    startsAt = startingDate.orEmpty().take(10),
    endsAt = endingDate.orEmpty().take(10)
)
