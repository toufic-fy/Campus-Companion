package com.inci.campuscompanion.data.remote.api

import com.inci.campuscompanion.data.remote.dto.EventDto
import retrofit2.http.GET

interface CampusApi {
    @GET(ApiConstants.Endpoints.EVENTS)
    suspend fun getEvents(): List<EventDto>
}

class EventsRemoteDataSource(
    private val api: CampusApi
) {
    suspend fun getEvents(): List<EventDto> {
        return api.getEvents()
    }
}
