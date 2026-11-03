package com.inci.campuscompanion

import android.app.Application
import androidx.work.Configuration
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.work.RefreshEventsWorkerFactory

class CampusApplication : Application(), Configuration.Provider {

    val eventsRepository: EventsRepository by lazy {
        EventsRepository(
            EventsRemoteDataSource(RetrofitClient.campusApi),
            AppDatabase.getDatabase(this).eventDao()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(RefreshEventsWorkerFactory(eventsRepository))
            .build()
}
