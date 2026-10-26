package com.inci.campuscompanion.data.remote

import retrofit2.http.GET

interface CampusApi {

    @GET("event")
    suspend fun getEvents(): List<EventDto>
}