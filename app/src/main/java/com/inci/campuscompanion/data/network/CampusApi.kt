package com.inci.campuscompanion.data.network
import retrofit2.http.GET



interface CampusApi {
    @GET("events")
    suspend fun getEvents(): List<EventDto>
}