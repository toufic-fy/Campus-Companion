
package com.inci.campuscompanion.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

interface CampusApi {

    @GET("posts")
    suspend fun getEvents(): List<EventDto>

    companion object {
        private const val BASE_URL =
            "https://jsonplaceholder.typicode.com/"

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
