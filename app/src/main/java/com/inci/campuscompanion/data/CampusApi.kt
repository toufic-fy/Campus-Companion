package com.inci.campuscompanion.data

import retrofit2.http.GET

interface CampusApi {
    @GET("posts")
    suspend fun getEvents(): List<EventDto>
}
