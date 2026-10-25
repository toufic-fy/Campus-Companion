package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(private val remoteDataSource: EventsRemoteDataSource) {
    suspend fun getEvents(): List<CampusEvent> =
        remoteDataSource.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                timeLabel = dto.startsAt,
                locationLabel = dto.location
            )
        }
}
