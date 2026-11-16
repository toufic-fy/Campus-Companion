
package com.inci.campuscompanion.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

interface CampusApi {

    // Class mock API: GET {BASE_URL}events/event
    @GET("events/event")
    suspend fun getEvents(): List<EventDto>

    companion object {
        private const val BASE_URL =
            "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"

        val instance: CampusApi by lazy {
            val json = Json {
                ignoreUnknownKeys = true
            }

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(
                    json.asConverterFactory(
                        "application/json".toMediaType()
                    )
                )
                .build()
                .create(CampusApi::class.java)
        }
    }
}
