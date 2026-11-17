package com.inci.campuscompanion

import android.app.Application
import android.content.Context
import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.data.remote.EventsNetwork
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.worker.RefreshEventsWorker

class CampusApplication : Application() {
    val graph by lazy { AppGraph(this) }
    override fun onCreate() {
        super.onCreate()
        RefreshEventsWorker.schedule(this)
    }
}

class AppGraph(context: Context) {
    val preferences = UserPreferencesRepository(context.dataStore)
    val events = CachedEventsRepository(
        EventsRepository(EventsRemoteDataSource(EventsNetwork.api)),
        RoomEventsCache(AppDatabase.getDatabase(context).eventDao())
    )
}
