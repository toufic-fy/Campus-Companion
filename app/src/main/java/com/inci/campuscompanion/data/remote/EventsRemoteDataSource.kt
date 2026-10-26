package com.inci.campuscompanion.data.remote

class EventsRemoteDataSource(
    private val api: CampusApi
) {
    suspend fun getEvents(): List<EventDto> {
        return api.getEvents()
    }
}