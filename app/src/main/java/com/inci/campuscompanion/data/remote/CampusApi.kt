package com.inci.campuscompanion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CampusApi {
    @GET("events")
    suspend fun getEvents(): List<EventDto>

    companion object {
        // Replace with the workshop's events server. Must end with "/".
        const val BASE_URL = "https://campus-api.example.com/"

        fun create(baseUrl: String = BASE_URL): CampusApi =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CampusApi::class.java)
    }
}
