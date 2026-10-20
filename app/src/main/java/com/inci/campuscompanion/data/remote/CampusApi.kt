package com.inci.campuscompanion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CampusApi {
    @GET("events.json")
    suspend fun getEvents(): List<EventDto>

    companion object {
        private const val BASE_URL =
            "https://raw.githubusercontent.com/toufic-fy/Campus-Companion/master/api/"

        fun create(): CampusApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusApi::class.java)
    }
}
