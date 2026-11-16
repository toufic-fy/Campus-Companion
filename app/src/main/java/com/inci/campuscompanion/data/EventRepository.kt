package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventsRepository(
    private val eventDao: EventDao,
    private val api: CampusApi = CampusApi.instance
) {
    // The screen reads events from Room, not straight from the network.
    val cachedEvents: Flow<List<CampusEvent>> =
        eventDao.observeEvents().map { rows ->
            rows.map { it.toCampusEvent() }
        }

    // Called by RefreshEventsWorker: fetch from the API, then save in Room.
    // Network/HTTP errors are thrown so the worker can decide retry vs failure.
    suspend fun refreshEvents() {
        val events = api.getEvents().map { dto -> dto.toCampusEvent() }
        eventDao.replaceAll(events.map { it.toEntity() })
    }
}
