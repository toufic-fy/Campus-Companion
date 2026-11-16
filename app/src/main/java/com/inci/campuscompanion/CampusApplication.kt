package com.inci.campuscompanion

import android.app.Application
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.notifications.EventNotificationHelper
import com.inci.campuscompanion.notifications.createEventUpdatesChannel

class CampusApplication : Application() {
    val preferences by lazy { UserPreferencesRepository(dataStore) }
    val eventsRepository by lazy {
        EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi),
            AppDatabase.getDatabase(this).eventDao())
    }
    val eventNotifications by lazy { EventNotificationHelper(this, preferences) }

    override fun onCreate() {
        super.onCreate()
        createEventUpdatesChannel(this)
    }

    suspend fun refreshEvents() {
        val newEvents = eventsRepository.refreshEvents()
        // One useful notification per refresh, even if several new events arrived.
        newEvents.firstOrNull()?.let { eventNotifications.show(it) }
    }
}
