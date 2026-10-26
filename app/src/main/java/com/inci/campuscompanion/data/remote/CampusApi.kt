package com.inci.campuscompanion.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

interface CampusApi {
    @GET("event")
    suspend fun getEvents(): List<EventDto>
}

object CampusApiFactory {
    const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"

    fun create(baseUrl: String = BASE_URL): CampusApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }
            .asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CampusApi::class.java)
}

class EventsRemoteDataSource(private val api: CampusApi) {
    suspend fun getEvents(): List<EventDto> = api.getEvents()
}
