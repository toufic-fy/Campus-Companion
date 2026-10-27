package com.inci.campuscompanion.data


import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.model.EventDto

class EventsRemoteDataSource(
    private val api: CampusApi
) {
    suspend fun getEvents(): List<EventDto> =
        api.getEvents()
}

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
