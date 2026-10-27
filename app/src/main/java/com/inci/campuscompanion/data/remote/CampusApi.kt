package com.inci.campuscompanion.data.remote

import retrofit2.http.GET

interface CampusApi {
    @GET("event")
    suspend fun getEvents(): List<EventDto>
}

class EventsRemoteDataSource(private val api: CampusApi) {
    suspend fun getEvents(): List<EventDto> = api.getEvents()
}
