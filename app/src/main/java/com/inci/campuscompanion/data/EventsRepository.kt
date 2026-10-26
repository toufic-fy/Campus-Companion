package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(
    private val api: CampusApi
) {
    suspend fun getEvents(page: Int, limit: Int = 5): List<CampusEvent> {
        return api.getEvents(page = page, limit = limit).mapIndexed { index, dto ->
            val formattedDate = dto.date?.let {
                if (it.contains("T")) it.substringBefore("T") else it
            } ?: "Date TBA"

            CampusEvent(
                id = dto.id ?: "${page}_$index",
                title = dto.title ?: "Untitled Event",
                description = dto.description,
                date = formattedDate,
                location = dto.location
            )
        }
    }
}