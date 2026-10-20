package com.inci.campuscompanion.data.remote

import com.inci.campuscompanion.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CampusApi {
    @GET("events")
    suspend fun getEvents(): List<EventDto>

    companion object {
        // Set per build type in app/build.gradle.kts.
        const val BASE_URL = BuildConfig.EVENTS_BASE_URL

        fun create(baseUrl: String = BASE_URL): CampusApi =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CampusApi::class.java)
    }
}
