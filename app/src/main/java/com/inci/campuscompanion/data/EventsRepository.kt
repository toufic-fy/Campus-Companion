package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(private val remote: EventsRemoteDataSource) {
    suspend fun getEvents(): List<CampusEvent> = remote.getEvents().map { dto ->
        CampusEvent(
            id = requireNotNull(dto.id?.takeIf { it.isNotBlank() }) { "Missing event id" },
            title = requireNotNull(dto.name?.takeIf { it.isNotBlank() }) { "Missing event name" },
            timeLabel = requireNotNull(dto.startingDate?.takeIf { it.isNotBlank() }) { "Missing event startingDate" },
            locationLabel = dto.address?.takeIf { it.isNotBlank() } ?: "TBA",
            description = dto.description.orEmpty()
        )
    }
}
