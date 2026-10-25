
package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource

class EventsRepository(
    private val remote: EventsRemoteDataSource
) {
    suspend fun getEvents(): List<CampusEvent> =
        remote.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.title,
                timeLabel = dto.startsAt,
                locationLabel = dto.location ?: "TBA"
            )
        }
}
