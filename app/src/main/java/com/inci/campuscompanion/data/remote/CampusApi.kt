package com.inci.campuscompanion.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CampusApi {
    @GET("events/event")
    suspend fun getEvents(): List<EventDto>

    companion object {
        const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"

        fun create(baseUrl: String = BASE_URL): CampusApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CampusApi::class.java)
        }
    }
}
