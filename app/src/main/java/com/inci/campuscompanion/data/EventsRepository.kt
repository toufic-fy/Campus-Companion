package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(private val remote: EventsRemoteDataSource) {
    suspend fun getEvents(): List<CampusEvent> = remote.getEvents().map { dto ->
        require(dto.id.isNotBlank() && dto.name.isNotBlank() && dto.startingDate.isNotBlank()) {
            "Invalid event data"
        }
        CampusEvent(
            id = dto.id,
            title = dto.name,
            timeLabel = dto.startingDate,
            locationLabel = dto.address ?: "TBA"
        )
    }
}
