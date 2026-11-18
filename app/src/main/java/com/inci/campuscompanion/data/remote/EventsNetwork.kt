package com.inci.campuscompanion.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object EventsNetwork {
    private const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"

    val api: CampusApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(CampusApi::class.java)
    }
}
