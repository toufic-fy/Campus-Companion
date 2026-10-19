package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(private val api: CampusApi) {

    suspend fun getEvents(): List<CampusEvent> {
        val response = api.getEvents()
        return response.map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.title,
                description = dto.body
            )
        }
    }
}
