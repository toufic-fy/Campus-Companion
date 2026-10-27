package com.inci.campuscompanion.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inci.campuscompanion.ui.EventsViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object EventsModule {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy {
        retrofit.create(CampusApi::class.java)
    }

    private val repository by lazy {
        EventsRepository(EventsRemoteDataSource(api))
    }

    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            require(modelClass.isAssignableFrom(EventsViewModel::class.java))
            return EventsViewModel(repository) as T
        }
    }
}
