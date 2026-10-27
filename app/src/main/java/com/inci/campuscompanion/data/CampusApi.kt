package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.EventDto
import retrofit2.http.GET

interface CampusApi {

    @GET("events/event")
    suspend fun getEvents(): List<EventDto>

}
