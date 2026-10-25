package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(
    private val remote: EventsRemoteDataSource
) {
    suspend fun getEvents(): List<CampusEvent> =
        remote.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.name,
                timeLabel = dto.startingDate,
                locationLabel = dto.address ?: "TBA"
            )
        }
}