package com.inci.campuscompanion.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

/**
 * Retrofit service: only knows endpoint paths and HTTP methods.
 */
interface CampusApi {
    @GET("events/event")
    suspend fun getEvents(): List<EventDto>

    companion object {
        // HTTPS only — cleartext HTTP is disabled by default on API 28+.
        private const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"

        // The server sends extra fields (avatar, createdAt) that the app does not use.
        private val json = Json { ignoreUnknownKeys = true }

        fun create(): CampusApi =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CampusApi::class.java)
    }
}
