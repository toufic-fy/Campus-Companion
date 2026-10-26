package com.inci.campuscompanion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"

    val api: CampusApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusApi::class.java)
    }
}