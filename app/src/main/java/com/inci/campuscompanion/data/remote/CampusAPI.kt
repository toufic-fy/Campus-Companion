package com.inci.campuscompanion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CampusApi {

    @GET("events/event")
    suspend fun getEvents(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 5
    ): List<EventDto>

    companion object {
        fun create(): CampusApi {
            return Retrofit.Builder()
                .baseUrl("https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/")
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(CampusApi::class.java)
        }
    }
}
