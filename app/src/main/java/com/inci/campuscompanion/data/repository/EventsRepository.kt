package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.network.EventsRemoteDataSource

// Domain model (UI-ready)
data class CampusEvent(
    val id: String,
    val title: String,
    val timeLabel: String,
    val locationLabel: String
)

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