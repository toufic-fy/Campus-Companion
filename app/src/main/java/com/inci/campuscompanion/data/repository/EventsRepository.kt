package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.model.CampusEvent
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource

class EventsRepository(
    private val remote: EventsRemoteDataSource
) {
    suspend fun getEvents(): List<CampusEvent> {
        return remote.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.name,
                timeLabel = dto.startingDate,
                locationLabel = dto.address
                    ?.takeIf { it.isNotBlank() }
                    ?: "Location to be announced",
                description = dto.description
            )
        }
    }
}