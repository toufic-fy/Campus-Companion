package com.inci.campuscompanion.data

class EventsRemoteDataSource(private val api: CampusApi) {
    suspend fun getEvents(): List<EventDto> = api.getEvents()
}
