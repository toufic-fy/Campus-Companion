package com.inci.campuscompanion.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventsRepository(private val api: CampusApi) {
    suspend fun getEvents(): List<CampusEvent> = withContext(Dispatchers.IO) {
        val dtos = api.getEvents()
        dtos.map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.name ?: "No Title",
                description = dto.description ?: "No Description",
                date = dto.startingDate ?: "Unknown Date",
                imageUrl = dto.avatar
            )
        }
    }
}
