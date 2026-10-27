package com.inci.campuscompanion.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface CampusApi {
    @GET("event")
    suspend fun getEvents(): List<EventDto>

    companion object {
        const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"

        fun create(
            baseUrl: String = BASE_URL,
            client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
        ): CampusApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusApi::class.java)
    }
}

class EventsRemoteDataSource(private val api: CampusApi) {
    suspend fun getEvents(): List<EventDto> = api.getEvents()
}
