package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.network.EventsRemoteDataSource
import com.inci.campuscompanion.notifications.NotificationPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val _events = MutableStateFlow<List<CampusEvent>>(emptyList())
    val events: StateFlow<List<CampusEvent>> = _events.asStateFlow()

    /**
     * Fetches the latest events from the remote data source, updates the in-memory cache, and returns the list.
     */
    suspend fun getEvents(): List<CampusEvent> {
        val domainEvents = remote.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.title,
                timeLabel = dto.startsAt,
                locationLabel = dto.location ?: "TBA"
            )
        }
        _events.value = domainEvents
        return domainEvents
    }

    /**
     * Refreshes campus events from the remote data source.
     * Called by WorkManager background workers and manual refresh triggers.
     */
    suspend fun refreshEvents(): List<CampusEvent> {
        return getEvents()
    }


}